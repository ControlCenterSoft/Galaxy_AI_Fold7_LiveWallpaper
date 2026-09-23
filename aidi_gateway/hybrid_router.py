#!/usr/bin/env python3
"""Local-first AIDI provider router with optional cloud escalation and circuit breakers."""

import json
import os
import threading
import time
import urllib.request


def _bool_env(name, default=False):
    raw = os.environ.get(name)
    if raw is None:
        return bool(default)
    return raw.strip().lower() in ("1", "true", "yes", "on")


class CircuitBreaker:
    def __init__(self, name, failure_threshold=3, cooldown_seconds=120):
        self.name = name
        self.failure_threshold = max(1, int(failure_threshold))
        self.cooldown_seconds = max(5, int(cooldown_seconds))
        self.failures = 0
        self.opened_at = 0.0
        self.last_error = ""
        self.last_success_at = 0.0
        self._lock = threading.Lock()

    def allow(self):
        with self._lock:
            if self.opened_at <= 0:
                return True
            if time.time() - self.opened_at >= self.cooldown_seconds:
                self.opened_at = 0.0
                self.failures = 0
                return True
            return False

    def success(self):
        with self._lock:
            self.failures = 0
            self.opened_at = 0.0
            self.last_error = ""
            self.last_success_at = time.time()

    def failure(self, error):
        with self._lock:
            self.failures += 1
            self.last_error = str(error)[:160]
            if self.failures >= self.failure_threshold:
                self.opened_at = time.time()

    def snapshot(self):
        with self._lock:
            opened = self.opened_at > 0 and time.time() - self.opened_at < self.cooldown_seconds
            remaining = 0
            if opened:
                remaining = max(0, int(self.cooldown_seconds - (time.time() - self.opened_at)))
            return {
                "name": self.name,
                "state": "open" if opened else "closed",
                "failures": self.failures,
                "cooldown_remaining_seconds": remaining,
                "last_error": self.last_error,
                "last_success_at": int(self.last_success_at),
            }


class ProviderError(RuntimeError):
    pass


class OllamaProvider:
    def __init__(self):
        self.name = "local_ollama"
        self.url = os.environ.get("AIDI_OLLAMA_URL", "http://192.168.10.218:11434/api/chat")
        self.model = os.environ.get("AIDI_OLLAMA_MODEL", "qwen3-coder:30b")
        self.timeout = float(os.environ.get("AIDI_LOCAL_TIMEOUT_SECONDS", "90"))

    def complete_json(self, system_prompt, payload):
        body = {
            "model": self.model,
            "stream": False,
            "format": "json",
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": json.dumps(payload, separators=(",", ":"))},
            ],
            "options": {"temperature": 0.15, "num_predict": 260},
        }
        request = urllib.request.Request(
            self.url,
            data=json.dumps(body, separators=(",", ":")).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        with urllib.request.urlopen(request, timeout=self.timeout) as response:
            result = json.loads(response.read().decode("utf-8"))
        content = result.get("message", {}).get("content", "{}")
        candidate = json.loads(content)
        if not isinstance(candidate, dict):
            raise ProviderError("local provider returned non-object JSON")
        return candidate


class OpenAICompatibleProvider:
    """Optional cloud escalation target. No API key is committed; configuration is environment-only."""

    def __init__(self):
        self.name = "cloud_openai_compatible"
        self.url = os.environ.get("AIDI_CLOUD_URL", "").strip()
        self.model = os.environ.get("AIDI_CLOUD_MODEL", "").strip()
        self.api_key = os.environ.get("AIDI_CLOUD_API_KEY", "").strip()
        self.timeout = float(os.environ.get("AIDI_CLOUD_TIMEOUT_SECONDS", "45"))
        self.enabled = _bool_env("AIDI_CLOUD_ENABLED", False) and bool(self.url and self.model)

    def complete_json(self, system_prompt, payload):
        if not self.enabled:
            raise ProviderError("cloud provider disabled")
        body = {
            "model": self.model,
            "temperature": 0.15,
            "response_format": {"type": "json_object"},
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": json.dumps(payload, separators=(",", ":"))},
            ],
        }
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["Authorization"] = "Bearer " + self.api_key
        request = urllib.request.Request(
            self.url,
            data=json.dumps(body, separators=(",", ":")).encode("utf-8"),
            headers=headers,
            method="POST",
        )
        with urllib.request.urlopen(request, timeout=self.timeout) as response:
            result = json.loads(response.read().decode("utf-8"))
        choices = result.get("choices") or []
        if not choices:
            raise ProviderError("cloud provider returned no choices")
        content = choices[0].get("message", {}).get("content", "{}")
        candidate = json.loads(content)
        if not isinstance(candidate, dict):
            raise ProviderError("cloud provider returned non-object JSON")
        return candidate


class HybridRouter:
    def __init__(self):
        threshold = int(os.environ.get("AIDI_BREAKER_FAILURES", "3"))
        cooldown = int(os.environ.get("AIDI_BREAKER_COOLDOWN_SECONDS", "120"))
        self.local = OllamaProvider()
        self.cloud = OpenAICompatibleProvider()
        self.local_breaker = CircuitBreaker("local_ollama", threshold, cooldown)
        self.cloud_breaker = CircuitBreaker("cloud_openai_compatible", threshold, cooldown)
        self.allow_cloud_escalation = _bool_env("AIDI_CLOUD_ESCALATE_ON_LOCAL_FAILURE", True)
        self._lock = threading.Lock()
        self._stats = {
            "local_success": 0,
            "local_error": 0,
            "cloud_success": 0,
            "cloud_error": 0,
            "all_providers_failed": 0,
        }

    def _attempt(self, provider, breaker, system_prompt, payload, tier):
        if not breaker.allow():
            raise ProviderError("%s circuit open" % provider.name)
        started = time.time()
        try:
            candidate = provider.complete_json(system_prompt, payload)
            breaker.success()
            with self._lock:
                self._stats[tier + "_success"] += 1
            return candidate, {
                "schema": "provenance-v1",
                "tier": tier,
                "provider": provider.name,
                "model": provider.model,
                "latency_ms": max(0, int((time.time() - started) * 1000)),
                "cloud": tier == "cloud",
                "escalated": tier == "cloud",
            }
        except Exception as exc:
            breaker.failure(exc)
            with self._lock:
                self._stats[tier + "_error"] += 1
            raise

    def complete_json(self, system_prompt, payload):
        errors = []
        try:
            return self._attempt(
                self.local, self.local_breaker, system_prompt, payload, "local"
            )
        except Exception as exc:
            errors.append("local:%s" % str(exc)[:120])

        if self.allow_cloud_escalation and self.cloud.enabled:
            try:
                return self._attempt(
                    self.cloud, self.cloud_breaker, system_prompt, payload, "cloud"
                )
            except Exception as exc:
                errors.append("cloud:%s" % str(exc)[:120])

        with self._lock:
            self._stats["all_providers_failed"] += 1
        raise ProviderError("; ".join(errors) if errors else "no provider available")

    def fast_path_provenance(self):
        return {
            "schema": "provenance-v1",
            "tier": "deterministic",
            "provider": "aidi_fast_path",
            "model": "rules-v24",
            "latency_ms": 0,
            "cloud": False,
            "escalated": False,
        }

    def health(self):
        with self._lock:
            stats = dict(self._stats)
        return {
            "local": {
                "provider": self.local.name,
                "model": self.local.model,
                "url": self.local.url,
                "breaker": self.local_breaker.snapshot(),
            },
            "cloud": {
                "provider": self.cloud.name,
                "enabled": self.cloud.enabled,
                "configured": bool(self.cloud.url and self.cloud.model),
                "model": self.cloud.model,
                "breaker": self.cloud_breaker.snapshot(),
            },
            "cloud_escalation": self.allow_cloud_escalation,
            "stats": stats,
        }
