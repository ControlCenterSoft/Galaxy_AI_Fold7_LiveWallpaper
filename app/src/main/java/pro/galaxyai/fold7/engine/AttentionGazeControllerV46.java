package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v46 Attentive Gaze controller.
 *
 * Generates privacy-safe local eye-target motion from AI state plus touch coordinates.
 * No camera, face tracking, microphone or sensor stream is read. The controller produces
 * only two normalized gaze offsets consumed by the portrait mesh.
 */
public final class AttentionGazeControllerV46 {
    private float time;
    private float targetX;
    private float targetY;
    private float gazeX;
    private float gazeY;
    private float touchX;
    private float touchY;
    private float touchWeight;
    private float focus = 0.42f;
    private float curiosity = 0.38f;
    private float serenity = 0.72f;
    private String emotion = "calm";

    public void update(SceneDecision decision, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;
        if (decision != null) {
            float k = Math.min(1f, dt * 3.0f);
            focus += (clamp(decision.focus, 0f, 1f) - focus) * k;
            curiosity += (clamp(decision.curiosity, 0f, 1f) - curiosity) * k;
            serenity += (clamp(decision.serenity, 0f, 1f) - serenity) * k;
            emotion = normalizeEmotion(decision.avatarState);
        }

        touchWeight *= (float) Math.pow(0.08f, dt);

        float microX = ((float) Math.sin(time * 0.91f + 0.4f)
                + (float) Math.sin(time * 1.73f + 1.8f) * 0.36f)
                * (0.18f + curiosity * 0.34f);
        float microY = ((float) Math.sin(time * 0.67f + 1.1f)
                + (float) Math.sin(time * 1.31f + 2.2f) * 0.28f)
                * (0.11f + (1f - serenity) * 0.18f);

        if ("focused".equals(emotion)) {
            microX *= 0.45f;
            microY *= 0.45f;
        } else if ("thinking".equals(emotion)) {
            microX *= 1.20f;
            microY *= 1.12f;
        } else if ("sleep".equals(emotion)) {
            microX *= 0.10f;
            microY *= 0.08f;
        }

        float touchGain = 0.84f * touchWeight;
        targetX = clamp(microX * 0.34f + touchX * touchGain, -1f, 1f);
        targetY = clamp(microY * 0.32f + touchY * touchGain * 0.70f, -1f, 1f);

        float speed = "focused".equals(emotion) ? 5.0f : 6.5f;
        if ("sleep".equals(emotion)) speed = 1.4f;
        float smooth = Math.min(1f, dt * speed);
        gazeX += (targetX - gazeX) * smooth;
        gazeY += (targetY - gazeY) * smooth;

        float focusDamping = 1f - focus * 0.12f;
        gazeX *= focusDamping;
        gazeY *= focusDamping;
    }

    public void onTouch(float normalizedX, float normalizedY, boolean pressed) {
        touchX = clamp(normalizedX, -1f, 1f);
        touchY = clamp(normalizedY, -1f, 1f);
        if (pressed) touchWeight = 1f;
        else touchWeight = Math.max(touchWeight, 0.45f);
    }

    public float getGazeX() {
        return clamp(gazeX, -1f, 1f);
    }

    public float getGazeY() {
        return clamp(gazeY, -1f, 1f);
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
