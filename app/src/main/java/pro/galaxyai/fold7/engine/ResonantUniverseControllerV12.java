package pro.galaxyai.fold7.engine;

/**
 * v12 resonance layer over the v11 Adaptive Universe.
 * Fold display changes briefly increase visual energy, then decay smoothly,
 * preserving offline deterministic behavior and long-session pacing.
 */
public class ResonantUniverseControllerV12 {
    private final AdaptiveUniverseControllerV11 adaptive;
    private boolean initialized;
    private boolean lastMainDisplay;
    private float resonance;
    private float time;

    public ResonantUniverseControllerV12(long seed, long evolutionEpoch) {
        adaptive = new AdaptiveUniverseControllerV11(seed, evolutionEpoch);
    }

    public void update(float deltaSeconds, boolean mainDisplay) {
        float dt = Math.max(0f, Math.min(deltaSeconds, 0.25f));
        adaptive.update(dt, mainDisplay);
        time += dt;

        if (!initialized) {
            initialized = true;
            lastMainDisplay = mainDisplay;
        } else if (mainDisplay != lastMainDisplay) {
            lastMainDisplay = mainDisplay;
            resonance = 1f;
        }

        resonance = Math.max(0f, resonance - dt * 0.30f);
    }

    public float getSceneScale() {
        float ring = resonance * 0.004f * (float) Math.sin(time * 8.0f);
        return adaptive.getSceneScale() * (1f + ring);
    }

    public float getAvatarHorizontalBias() {
        return adaptive.getAvatarHorizontalBias()
                + resonance * 0.0025f * (float) Math.sin(time * 6.5f);
    }

    public float getAvatarVerticalBias() {
        return adaptive.getAvatarVerticalBias()
                + resonance * 0.0018f * (float) Math.cos(time * 7.0f);
    }

    public float getPulseMultiplier() {
        return adaptive.getPulseMultiplier() * (1f + resonance * 0.12f);
    }

    public float getParticleMultiplier() {
        return adaptive.getParticleMultiplier() * (1f + resonance * 0.08f);
    }

    /**
     * Temporarily raises render cadence around Fold transitions, then returns
     * to the adaptive long-session pacing inherited from v11.
     */
    public long getFrameDelayMillis(boolean mainDisplay) {
        long base = adaptive.getFrameDelayMillis(mainDisplay);
        if (resonance > 0.12f) {
            return Math.min(base, mainDisplay ? 33L : 40L);
        }
        return base;
    }
}
