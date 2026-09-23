#!/usr/bin/env python3
import json
import threading
import unittest
import urllib.error
import urllib.request

from aidi_gateway_simulator import PROTOCOL, create_server


class SceneProtocolIntegrationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = create_server("127.0.0.1", 0)
        cls.port = cls.server.server_address[1]
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join(timeout=2)

    def request(self, method, path, payload=None, headers=None):
        body = None
        actual_headers = dict(headers or {})
        if payload is not None:
            if isinstance(payload, bytes):
                body = payload
            else:
                body = json.dumps(payload, separators=(",", ":")).encode("utf-8")
            actual_headers.setdefault("Content-Type", "application/json")
        request = urllib.request.Request(
            "http://127.0.0.1:%d%s" % (self.port, path),
            data=body,
            headers=actual_headers,
            method=method,
        )
        try:
            with urllib.request.urlopen(request, timeout=2) as response:
                raw = response.read().decode("utf-8")
                return response.status, dict(response.headers.items()), json.loads(raw)
        except urllib.error.HTTPError as exc:
            raw = exc.read().decode("utf-8")
            return exc.code, dict(exc.headers.items()), json.loads(raw)

    def analyze(self, state, request_id):
        payload = {
            "device": "GalaxyFold7",
            "app_version": "20.0",
            "request_id": request_id,
            "captured_at_ms": 1770000000000,
            "state_sequence": 42,
            "state": state,
        }
        status, headers, body = self.request(
            "POST",
            "/api/v1/scene/analyze",
            payload,
            {
                "X-AIDI-Protocol": PROTOCOL,
                "X-AIDI-Request-Id": request_id,
            },
        )
        self.assertEqual(200, status)
        self.assertEqual(PROTOCOL, headers.get("X-AIDI-Protocol"))
        self.assertEqual(request_id, headers.get("X-AIDI-Request-Id"))
        self.assertEqual(request_id, body["request_id"])
        self.assertEqual(42, body["state_sequence"])
        self.assertTrue(body["decision_id"].startswith("sim-v20-"))
        self.assertEqual("aidi-gateway-simulator", body["source"])
        self.assert_scene_bounds(body)
        return body

    def assert_scene_bounds(self, body):
        scene = body["scene"]
        self.assertLessEqual(0.0, float(scene["energy"]))
        self.assertLessEqual(float(scene["energy"]), 1.0)
        self.assertLessEqual(0.45, float(scene["particle_multiplier"]))
        self.assertLessEqual(float(scene["particle_multiplier"]), 1.8)
        self.assertLessEqual(0.6, float(scene["pulse_multiplier"]))
        self.assertLessEqual(float(scene["pulse_multiplier"]), 1.5)
        self.assertLessEqual(0.97, float(scene["scene_scale"]))
        self.assertLessEqual(float(scene["scene_scale"]), 1.03)
        self.assertLessEqual(-0.02, float(scene["avatar_x_bias"]))
        self.assertLessEqual(float(scene["avatar_x_bias"]), 0.02)
        self.assertLessEqual(-0.02, float(scene["avatar_y_bias"]))
        self.assertLessEqual(float(scene["avatar_y_bias"]), 0.02)
        self.assertLessEqual(60, int(body["ttl"]))
        self.assertLessEqual(int(body["ttl"]), 3600)
        self.assertTrue(body["avatar"]["state"])

    def test_health_contract(self):
        status, headers, body = self.request("GET", "/health")
        self.assertEqual(200, status)
        self.assertEqual(PROTOCOL, headers.get("X-AIDI-Protocol"))
        self.assertEqual("ok", body["status"])
        self.assertEqual(PROTOCOL, body["protocol"])

    def test_scene_decision_matrix(self):
        scenarios = [
            (
                "low-battery",
                {"battery": 10, "charging": False, "hour": 14, "motion_level": 0.8, "fold_state": "main"},
                "deep_black",
                "resting",
            ),
            (
                "night-idle",
                {"battery": 82, "charging": False, "hour": 23, "motion_level": 0.18, "fold_state": "main"},
                "deep_nebula",
                "calm",
            ),
            (
                "charging-active",
                {"battery": 82, "charging": True, "hour": 14, "motion_level": 0.8, "fold_state": "main"},
                "energy_core",
                "focused",
            ),
            (
                "main-display",
                {"battery": 82, "charging": False, "hour": 14, "motion_level": 0.2, "fold_state": "main"},
                "living_continuum",
                "aware",
            ),
            (
                "cover-display",
                {"battery": 82, "charging": False, "hour": 14, "motion_level": 0.2, "fold_state": "cover"},
                "compact_continuum",
                "calm",
            ),
        ]

        for name, state, expected_scene, expected_avatar in scenarios:
            with self.subTest(name=name):
                body = self.analyze(state, "ci-" + name)
                self.assertEqual(expected_scene, body["scene"]["name"])
                self.assertEqual(expected_avatar, body["avatar"]["state"])

    def test_decision_is_deterministic(self):
        state = {
            "battery": 72,
            "charging": False,
            "hour": 18,
            "motion_level": 0.25,
            "fold_state": "main",
        }
        first = self.analyze(state, "ci-deterministic")
        second = self.analyze(state, "ci-deterministic")
        self.assertEqual(first["decision_id"], second["decision_id"])
        self.assertEqual(first["scene"], second["scene"])
        self.assertEqual(first["avatar"], second["avatar"])

    def test_rejects_wrong_protocol(self):
        status, headers, body = self.request(
            "POST",
            "/api/v1/scene/analyze",
            {"request_id": "ci-proto", "state": {}},
            {"X-AIDI-Protocol": "scene-v0", "X-AIDI-Request-Id": "ci-proto"},
        )
        self.assertEqual(400, status)
        self.assertEqual(PROTOCOL, headers.get("X-AIDI-Protocol"))
        self.assertEqual("unsupported_protocol", body["error"])

    def test_rejects_request_id_mismatch(self):
        status, _, body = self.request(
            "POST",
            "/api/v1/scene/analyze",
            {"request_id": "body-id", "state": {}},
            {"X-AIDI-Protocol": PROTOCOL, "X-AIDI-Request-Id": "header-id"},
        )
        self.assertEqual(400, status)
        self.assertEqual("bad_request", body["error"])
        self.assertIn("request id mismatch", body["detail"])

    def test_rejects_malformed_json(self):
        status, _, body = self.request(
            "POST",
            "/api/v1/scene/analyze",
            b'{"state":',
            {"Content-Type": "application/json", "X-AIDI-Protocol": PROTOCOL},
        )
        self.assertEqual(400, status)
        self.assertEqual("bad_request", body["error"])

    def test_rejects_non_json_content_type(self):
        status, _, body = self.request(
            "POST",
            "/api/v1/scene/analyze",
            b"state=main",
            {"Content-Type": "text/plain", "X-AIDI-Protocol": PROTOCOL},
        )
        self.assertEqual(415, status)
        self.assertEqual("unsupported_media_type", body["error"])

    def test_unknown_endpoint(self):
        status, _, body = self.request("GET", "/missing")
        self.assertEqual(404, status)
        self.assertEqual("not_found", body["error"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
