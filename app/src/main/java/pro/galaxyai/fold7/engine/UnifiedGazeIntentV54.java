package pro.galaxyai.fold7.engine;

/**
 * v54 Unified Gaze Intent.
 *
 * Deterministic privacy-safe fixation planner shared by every gaze controller instance.
 * It converts AI state into the same bounded fixation target and dwell time without
 * randomness, sensors, camera input or biometric tracking. This keeps the mesh eye motion
 * and head/eye coordination on one stable behavioural contract.
 */
public final class UnifiedGazeIntentV54 {
    public static final class Target {
        public final float x;
        public final float y;
        public final float durationSeconds;

        Target(float x, float y, float durationSeconds) {
            this.x = x;
            this.y = y;
            this.durationSeconds = durationSeconds;
        }
    }

    public Target next(int sequence, String emotion, float focus,
                       float curiosity, float serenity) {
        String state = normalizeEmotion(emotion);
        float emotionGain = "focused".equals(state) ? 0.30f
                : ("thinking".equals(state) ? 0.86f
                : ("happy".equals(state) ? 0.66f
                : ("sleep".equals(state) ? 0.02f : 0.52f)));
        float curiosityGain = 0.62f + clamp(curiosity, 0f, 1f) * 0.38f;
        float calmGain = 0.78f + (1f - clamp(serenity, 0f, 1f)) * 0.22f;
        float phase = Math.max(1, sequence) * 1.6180339f;

        float x = (float) Math.sin(phase * 1.73f + 0.31f)
                * 0.34f * emotionGain * curiosityGain;
        float y = (float) Math.sin(phase * 1.11f + 1.27f)
                * 0.19f * emotionGain * calmGain;

        // Regularly return near the viewer/centre. Strong focus also recentres attention.
        if ((sequence % 4) == 0 || focus > 0.78f) {
            x *= 0.24f;
            y *= 0.22f;
        }

        float base = "focused".equals(state) ? 2.35f
                : ("thinking".equals(state) ? 1.22f
                : ("sleep".equals(state) ? 4.8f : 1.75f));
        float variation = 0.45f + 0.55f * Math.abs((float) Math.sin(phase * 0.73f));
        float duration = base + variation * ("sleep".equals(state) ? 1.4f : 0.95f);

        return new Target(clamp(x, -0.92f, 0.92f),
                clamp(y, -0.78f, 0.78f), duration);
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
