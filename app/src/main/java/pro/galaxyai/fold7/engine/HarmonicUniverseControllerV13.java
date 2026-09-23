package pro.galaxyai.fold7.engine;

/**
 * v13 harmonic layer over the v12 Resonant Universe.
 * Adds a slow bounded modulation that keeps Fold-transition energy expressive
 * while preserving the adaptive long-session pacing and offline behavior.
 */
public class HarmonicUniverseControllerV13 {
    private final ResonantUniverseControllerV12 resonant;
    private float phase;

    public HarmonicUniverseControllerV13(long seed, long evolutionEpoch) {
        resonant = new ResonantUniverseControllerV12(seed, evolutionEpoch);
    }

    public void update(float deltaSeconds, boolean mainDisplay) {
        float dt = Math.max(0f, Math.min(deltaSeconds, 0.25f));
        resonant.update(dt, mainDisplay);
        phase += dt * (mainDisplay ? 0.55f : 0.42f);
        if (phase > 10000f) phase -= 10000f;
    }

    private float wave() {
        return (float) Math.sin(phase);
    }

    public float getSceneScale() {
        return resonant.getSceneScale() * (1f + wave() * 0.0020f);
    }

    public float getAvatarHorizontalBias() {
        return resonant.getAvatarHorizontalBias() + wave() * 0.0015f;
    }

    public float getAvatarVerticalBias() {
        return resonant.getAvatarVerticalBias()
                + (float) Math.cos(phase * 0.83f) * 0.0012f;
    }

    public float getPulseMultiplier() {
        return resonant.getPulseMultiplier() * (1f + wave() * 0.025f);
    }

    public float getParticleMultiplier() {
        return resonant.getParticleMultiplier()
                * (1f + (float) Math.cos(phase * 0.71f) * 0.018f);
    }

    public long getFrameDelayMillis(boolean mainDisplay) {
        return resonant.getFrameDelayMillis(mainDisplay);
    }
}
