package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v49 Head-Eye Coordination.
 *
 * Human gaze normally leads and the head follows a fraction later. This local controller
 * reproduces that relationship using only the already generated privacy-safe gaze vector
 * and AI state. It never reads camera, microphone, location, biometrics or raw media.
 */
public final class HeadEyeCoordinationV49 {
    private float followX;
    private float followY;
    private float roll;
    private float targetX;
    private float targetY;
    private float targetRoll;
    private float focus = 0.42f;
    private float serenity = 0.72f;
    private String emotion = "calm";

    public void update(SceneDecision decision, float gazeX, float gazeY, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        if (decision != null) {
            float k = Math.min(1f, dt * 2.8f);
            focus += (clamp(decision.focus, 0f, 1f) - focus) * k;
            serenity += (clamp(decision.serenity, 0f, 1f) - serenity) * k;
            emotion = normalizeEmotion(decision.avatarState);
        }

        float gain = "focused".equals(emotion) ? 0.26f
                : ("thinking".equals(emotion) ? 0.48f
                : ("happy".equals(emotion) ? 0.42f
                : ("sleep".equals(emotion) ? 0.06f : 0.34f)));
        gain *= 0.82f + (1f - serenity) * 0.18f;

        // Eyes lead. The head follows only the larger portion of gaze displacement,
        // preventing a robotic one-to-one lock between eye and head movement.
        float deadX = deadZone(gazeX, 0.12f);
        float deadY = deadZone(gazeY, 0.10f);
        targetX = deadX * gain;
        targetY = deadY * gain * 0.62f;
        targetRoll = -deadX * gain * 0.045f;

        float speed = "sleep".equals(emotion) ? 0.75f
                : ("focused".equals(emotion) ? 1.65f : 2.15f);
        float smooth = 1f - (float) Math.exp(-speed * dt);
        followX += (targetX - followX) * smooth;
        followY += (targetY - followY) * smooth;
        roll += (targetRoll - roll) * smooth;

        // High focus biases the head back toward centre while the eyes may remain offset.
        float centreBias = clamp((focus - 0.72f) * 0.18f, 0f, 0.05f);
        followX *= 1f - centreBias;
        followY *= 1f - centreBias;
    }

    public float getFollowX() {
        return clamp(followX, -0.42f, 0.42f);
    }

    public float getFollowY() {
        return clamp(followY, -0.28f, 0.28f);
    }

    public float getRoll() {
        return clamp(roll, -0.025f, 0.025f);
    }

    private static float deadZone(float value, float threshold) {
        float a = Math.abs(value);
        if (a <= threshold) return 0f;
        float signed = value < 0f ? -1f : 1f;
        return signed * (a - threshold) / (1f - threshold);
    }

    private static String normalizeEmotion(String value) {
        String v = value == null ? "calm" : value.trim().toLowerCase();
        if ("aware".equals(v)) return "thinking";
        if ("resting".equals(v)) return "sleep";
        if ("calm".equals(v) || "focused".equals(v) || "thinking".equals(v)
                || "happy".equals(v) || "sleep".equals(v)) return v;
        return "calm";
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
