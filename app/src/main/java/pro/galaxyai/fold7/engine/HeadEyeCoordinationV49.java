package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v49 Head-Eye Coordination, upgraded by v55 Anticipatory Head Follow.
 *
 * Eyes lead a new fixation first. The head joins only after a short, state-aware lead
 * interval and follows through a bounded spring-damper trajectory. Small microsaccades stay
 * eye-only, reducing robotic whole-head jitter. The controller consumes only the already
 * generated privacy-safe gaze vector and AI state; it reads no camera, microphone,
 * precise location, biometrics or raw media.
 */
public final class HeadEyeCoordinationV49 {
    private float followX;
    private float followY;
    private float roll;
    private float velocityX;
    private float velocityY;
    private float rollVelocity;
    private float targetX;
    private float targetY;
    private float targetRoll;
    private float lastGazeX;
    private float lastGazeY;
    private float gazeLeadClock;
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

        float gazeMotion = (float) Math.sqrt(
                (gazeX - lastGazeX) * (gazeX - lastGazeX)
                        + (gazeY - lastGazeY) * (gazeY - lastGazeY));
        lastGazeX = gazeX;
        lastGazeY = gazeY;

        float significant = Math.max(Math.abs(gazeX), Math.abs(gazeY));
        if (significant > 0.13f || gazeMotion > 0.035f) {
            gazeLeadClock += dt;
        } else {
            gazeLeadClock = Math.max(0f, gazeLeadClock - dt * 1.7f);
        }

        float leadDelay = "thinking".equals(emotion) ? 0.075f
                : ("happy".equals(emotion) ? 0.085f
                : ("focused".equals(emotion) ? 0.125f
                : ("sleep".equals(emotion) ? 0.28f : 0.105f)));
        float followGate = smoothStep(leadDelay * 0.55f, leadDelay, gazeLeadClock);

        float gain = "focused".equals(emotion) ? 0.25f
                : ("thinking".equals(emotion) ? 0.50f
                : ("happy".equals(emotion) ? 0.44f
                : ("sleep".equals(emotion) ? 0.05f : 0.35f)));
        gain *= 0.82f + (1f - serenity) * 0.18f;

        // v55: microsaccades remain eye-only; the head responds to displacement that survives
        // the dead zone and gaze-lead interval.
        float deadX = deadZone(gazeX, 0.13f);
        float deadY = deadZone(gazeY, 0.11f);
        targetX = deadX * gain * followGate;
        targetY = deadY * gain * 0.62f * followGate;
        targetRoll = -deadX * gain * 0.043f * followGate;

        float stiffness = "sleep".equals(emotion) ? 2.4f
                : ("focused".equals(emotion) ? 6.2f : 8.4f);
        float damping = "sleep".equals(emotion) ? 3.4f
                : ("focused".equals(emotion) ? 4.9f : 5.6f);

        velocityX += ((targetX - followX) * stiffness - velocityX * damping) * dt;
        velocityY += ((targetY - followY) * stiffness - velocityY * damping) * dt;
        rollVelocity += ((targetRoll - roll) * stiffness - rollVelocity * damping) * dt;
        followX += velocityX * dt;
        followY += velocityY * dt;
        roll += rollVelocity * dt;

        // High focus gently recentres the head while the eyes can continue the fixation.
        float centreBias = clamp((focus - 0.72f) * 0.20f, 0f, 0.055f);
        float returnGain = 1f - centreBias * Math.min(1f, dt * 30f);
        followX *= returnGain;
        followY *= returnGain;

        followX = clamp(followX, -0.42f, 0.42f);
        followY = clamp(followY, -0.28f, 0.28f);
        roll = clamp(roll, -0.025f, 0.025f);
    }

    public float getFollowX() { return followX; }
    public float getFollowY() { return followY; }
    public float getRoll() { return roll; }

    private static float smoothStep(float edge0, float edge1, float x) {
        if (edge1 <= edge0) return x >= edge1 ? 1f : 0f;
        float t = clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
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
