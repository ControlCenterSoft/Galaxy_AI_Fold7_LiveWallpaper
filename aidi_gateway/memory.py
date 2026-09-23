#!/usr/bin/env python3
"""Bounded AIDI scene memory with an optional Qdrant persistence backend."""

import json
import math
import os
import threading
import time
import urllib.error
import urllib.request
from collections import defaultdict, deque


class MemoryStore:
    def __init__(self):
        self.qdrant_url = os.environ.get("AIDI_QDRANT_URL", "").rstrip("/")
        self.collection = os.environ.get("AIDI_QDRANT_COLLECTION", "galaxy_ai_scene_memory_v21")
        self.api_key = os.environ.get("AIDI_QDRANT_API_KEY", "")
        self.timeout = float(os.environ.get("AIDI_QDRANT_TIMEOUT_SECONDS", "2.5"))
        self.max_local = max(8, min(256, int(os.environ.get("AIDI_MEMORY_LOCAL_ENTRIES", "48"))))
        self.score_threshold = float(os.environ.get("AIDI_MEMORY_SCORE_THRESHOLD", "0.86"))
        self._local = defaultdict(lambda: deque(maxlen=self.max_local))
        self._lock = threading.Lock()
        self._collection_ready = False
        self._stats = {
            "local_writes": 0,
            "local_hits": 0,
            "qdrant_writes": 0,
            "qdrant_hits": 0,
            "qdrant_errors": 0,
        }

    @staticmethod
    def _state(payload):
        state = payload.get("state") or {}
        fold = 1.0 if state.get("fold_state") == "main" else 0.0
        hour = max(0.0, min(23.0, float(state.get("hour", 12)))) / 23.0
        battery = max(0.0, min(100.0, float(state.get("battery", 50)))) / 100.0
        motion = max(0.0, min(1.0, float(state.get("motion_level", 0.0))))
        charging = 1.0 if bool(state.get("charging", False)) else 0.0
        return [hour, battery, motion, fold, charging]

    @classmethod
    def _vector(cls, payload):
        raw = cls._state(payload)
        norm = math.sqrt(sum(value * value for value in raw)) or 1.0
        return [round(value / norm, 7) for value in raw]

    @staticmethod
    def _device(payload):
        value = str(payload.get("device", "GalaxyFold7"))
        return value[:80] or "GalaxyFold7"

    @staticmethod
    def _cosine(a, b):
        dot = sum(x * y for x, y in zip(a, b))
        na = math.sqrt(sum(x * x for x in a)) or 1.0
        nb = math.sqrt(sum(x * x for x in b)) or 1.0
        return dot / (na * nb)

    def _headers(self):
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["api-key"] = self.api_key
        return headers

    def _request(self, method, path, body=None):
        raw = None if body is None else json.dumps(body, separators=(",", ":")).encode("utf-8")
        request = urllib.request.Request(
            self.qdrant_url + path,
            data=raw,
            headers=self._headers(),
            method=method,
        )
        with urllib.request.urlopen(request, timeout=self.timeout) as response:
            data = response.read()
        return json.loads(data.decode("utf-8")) if data else {}

    def _ensure_collection(self):
        if not self.qdrant_url or self._collection_ready:
            return
        try:
            self._request("GET", "/collections/%s" % self.collection)
            self._collection_ready = True
            return
        except urllib.error.HTTPError as exc:
            if exc.code != 404:
                raise
        self._request(
            "PUT",
            "/collections/%s" % self.collection,
            {"vectors": {"size": 5, "distance": "Cosine"}},
        )
        self._collection_ready = True

    def remember(self, payload, decision, source):
        vector = self._vector(payload)
        device = self._device(payload)
        record = {
            "vector": vector,
            "decision": decision,
            "source": str(source)[:80],
            "created_at": int(time.time()),
        }
        with self._lock:
            self._local[device].append(record)
            self._stats["local_writes"] += 1

        if not self.qdrant_url:
            return
        point_id = int(time.time_ns() & 0x7FFFFFFFFFFFFFFF)
        point_payload = {
            "device": device,
            "decision": decision,
            "source": str(source)[:80],
            "created_at": int(time.time()),
        }
        try:
            self._ensure_collection()
            self._request(
                "PUT",
                "/collections/%s/points?wait=true" % self.collection,
                {"points": [{"id": point_id, "vector": vector, "payload": point_payload}]},
            )
            with self._lock:
                self._stats["qdrant_writes"] += 1
        except Exception:
            with self._lock:
                self._stats["qdrant_errors"] += 1

    def _recall_local(self, payload):
        vector = self._vector(payload)
        device = self._device(payload)
        with self._lock:
            records = list(self._local.get(device, ()))
        best = None
        best_score = -1.0
        for record in records:
            score = self._cosine(vector, record["vector"])
            if score > best_score:
                best = record
                best_score = score
        if best is None or best_score < self.score_threshold:
            return None
        result = dict(best["decision"])
        result["_memory_score"] = round(best_score, 4)
        result["_memory_backend"] = "local"
        with self._lock:
            self._stats["local_hits"] += 1
        return result

    def _recall_qdrant(self, payload):
        if not self.qdrant_url:
            return None
        try:
            self._ensure_collection()
            result = self._request(
                "POST",
                "/collections/%s/points/search" % self.collection,
                {
                    "vector": self._vector(payload),
                    "limit": 1,
                    "with_payload": True,
                    "score_threshold": self.score_threshold,
                    "filter": {
                        "must": [{"key": "device", "match": {"value": self._device(payload)}}]
                    },
                },
            )
            points = result.get("result") or []
            if not points:
                return None
            payload_out = points[0].get("payload") or {}
            decision = payload_out.get("decision")
            if not isinstance(decision, dict):
                return None
            remembered = dict(decision)
            remembered["_memory_score"] = round(float(points[0].get("score", 0.0)), 4)
            remembered["_memory_backend"] = "qdrant"
            with self._lock:
                self._stats["qdrant_hits"] += 1
            return remembered
        except Exception:
            with self._lock:
                self._stats["qdrant_errors"] += 1
            return None

    def recall(self, payload):
        return self._recall_qdrant(payload) or self._recall_local(payload)

    def stats(self):
        with self._lock:
            result = dict(self._stats)
            result["devices"] = len(self._local)
            result["local_entries"] = sum(len(values) for values in self._local.values())
        result["qdrant_enabled"] = bool(self.qdrant_url)
        result["collection"] = self.collection
        return result
