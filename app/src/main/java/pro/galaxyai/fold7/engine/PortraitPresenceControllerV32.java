package pro.galaxyai.fold7.engine;

/**
 * v32 Expression Presence Engine.
 *
 * Converts bounded AIDI portrait traits into smooth composition multipliers. The controller
 * is deliberately platform-free so its state transitions can be tested on the JVM. It does
 * not consume camera, microphone, biometric or location input.
 */
public final class PortraitPresenceControllerV32 {
    private float phase;
    private float scaleMultiplier = 1f;
    private float verticalShift;
    private float horizontalShift;
    private float glowMultiplier = 1f;
    private float particleMultiplier = 1f;
    private float attention = 0.55f;
    private long suggestedFrameDelayMillis = 37L;

    public void update(String emotion,
                       float presence,
                       float focus,
                       float curiosity,
                       float energy,
                       float expressionIntensity,
                       boolean mainDisplay,
                       float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        phase += dt;

        String state = normalizeEmotion(emotion);
        presence = clamp(presence, 0f, 1f);
        focus = clamp(focus, 0f, 1f);
        curiosity = clamp(curiosity, 0f, 1f);
        energy = clamp(energy, 0f, 1f);
        expressionIntensity = clamp(expressionIntensity, 0.35f, 1f);

        float targetAttention = clamp(
                0.30f + presence * 0.28f + focus * 0.30f + curiosity * 0.12f,
                0f, 1f);
        float targetScale = 0.955f
                + presence * 0.075f
                + expressionIntensity * 0.035f
                + focus * 0.018f
                + (mainDisplay ? 0f : 0.020f);
        float targetVertical = -0.004f
                - targetAttention * (mainDisplay ? 0.010f : 0.014f);
        float targetGlow = 0.86f + energy * 0.14f + presence * 0.10f;
        float targetParticles = 0.82f + curiosity * 0.18f + energy * 0.12f;
        long targetFrameDelay = targetAttention > 0.78f ? 32L : 37L;

        if ("focused".equals(state)) {
            targetScale += 0.020f;
            targetVertical -= 0.006f;
            targetGlow += 0.055f;
            targetParticles *= 0.90f;
            targetFrameDelay = 32L;
        } else if ("thinking".equals(state)) {
            targetScale += 0.010f;
            targetGlow += 0.025f;
            targetParticles += 0.055f;
        } else if ("happy".equals(state)) {
            targetScale += 0.024f;
            targetVertical -= 0.003f;
            targetGlow += 0.070f;
            targetParticles += 0.090f;
            targetFrameDelay = 32L;
        } else if ("sleep".equals(state)) {
            targetScale -= 0.045f;
            targetVertical += 0.012f;
            targetGlow = 0.74f + energy * 0.05f;
            targetParticles = 0.58f;
            targetAttention = 0.16f;
            targetFrameDelay = 50L;
        }

        float microAmplitude = "sleep".equals(state) ? 0.0008f : (0.0015f + curiosity * 0.0020f);
        float targetHorizontal = (float) Math.sin(phase * 0.31f) * microAmplitude;

        float blend = 1f - (float) Math.exp(-dt * 4.2f);
        scaleMultiplier = approachLimited(
                scaleMultiplier,
                clamp(targetScale, 0.90f, 1.12f),
                blend,
                Math.max(0.002f, dt * 0.26f));
        verticalShift = approach(verticalShift, clamp(targetVertical, -0.030f, 0.020f), blend);
        horizontalShift = approach(horizontalShift, clamp(targetHorizontal, -0.006f, 0.006f), blend);
        glowMultiplier = approach(glowMultiplier, clamp(targetGlow, 0.72f, 1.18f), blend);
        particleMultiplier = approach(particleMultiplier, clamp(targetParticles, 0.55f, 1.16f), blend);
        attention = approach(attention, targetAttention, blend);
        suggestedFrameDelayMillis = targetFrameDelay;
    }

    public float getScaleMultiplier() {
        return scaleMultiplier;
    }

    public float getVerticalShift() {
        return verticalShift;
    }

    public float getHorizontalShift() {
        return horizontalShift;
    }

    public float getGlowMultiplier() {
        return glowMultiplier;
    }

    public float getParticleMultiplier() {
        return particleMultiplier;
    }

    public float getAttention() {
        return attention;
    }

    public long getSuggestedFrameDelayMillis(long universeDelayMillis) {
        long boundedUniverse = Math.max(16L, Math.min(50L, universeDelayMillis));
        if (suggestedFrameDelayMillis >= 50L) return Math.max(boundedUniverse, 50L);
        return Math.min(boundedUniverse, suggestedFrameDelayMillis);
    }

    private static String normalizeEmotion(String emotion) {
        if (emotion == null) return "calm";
        String value = emotion.trim().toLowerCase();
        if ("focused".equals(value) || "thinking".equals(value) || "happy".equals(value)
                || "sleep".equals(value) || "calm".equals(value)) return value;
        if ("aware".equals(value)) return "thinking";
        if ("resting".equals(value)) return "sleep";
        return "calm";
    }

    private static float approach(float current, float target, float blend) {
        return current + (target - current) * clamp(blend, 0f, 1f);
    }

    private static float approachLimited(float current, float target, float blend, float maxDelta) {
        float candidate = approach(current, target, blend);
        float delta = clamp(candidate - current, -Math.abs(maxDelta), Math.abs(maxDelta));
        return current + delta;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
