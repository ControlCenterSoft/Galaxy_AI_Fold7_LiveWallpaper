package pro.galaxyai.fold7.engine;

/**
 * v14 coherence layer over the v13 Harmonic Universe.
 * Keeps the harmonic motion phase-coherent across Fold states while preserving
 * the personal-universe lineage, bounded modulation and offline behavior.
 */
public class CoherentUniverseControllerV14 {
    private final HarmonicUniverseControllerV13 harmonic;
    private float phase;
    private float envelope = 1f;

    public CoherentUniverseControllerV14(long seed, long evolutionEpoch) {
        harmonic = new HarmonicUniverseControllerV13(seed, evolutionEpoch);
    }

    public void update(float deltaSeconds, boolean mainDisplay) {
        float dt = Math.max(0f, Math.min(deltaSeconds, 0.25f));
        harmonic.update(dt, mainDisplay);

        float targetEnvelope = mainDisplay ? 1f : 0.86f;
        float blend = Math.min(1f, dt * 0.65f);
        envelope += (targetEnvelope - envelope) * blend;

        phase += dt * 0.31f;
        if (phase > 10000f) phase -= 10000f;
    }

    private float wave() {
        return (float) Math.sin(phase) * envelope;
    }

    public float getSceneScale() {
        return harmonic.getSceneScale() * (1f + wave() * 0.0012f);
    }

    public float getAvatarHorizontalBias() {
        return harmonic.getAvatarHorizontalBias()
                + (float) Math.sin(phase * 0.79f) * envelope * 0.0009f;
    }

    public float getAvatarVerticalBias() {
        return harmonic.getAvatarVerticalBias()
                + (float) Math.cos(phase * 0.67f) * envelope * 0.0008f;
    }

    public float getPulseMultiplier() {
        return harmonic.getPulseMultiplier() * (1f + wave() * 0.015f);
    }

    public float getParticleMultiplier() {
        return harmonic.getParticleMultiplier()
                * (1f + (float) Math.cos(phase * 0.53f) * envelope * 0.012f);
    }

    public long getFrameDelayMillis(boolean mainDisplay) {
        return harmonic.getFrameDelayMillis(mainDisplay);
    }
}
