package pro.galaxyai.fold7.engine;

/**
 * v18 Singularity Universe layer over v17.
 * Adds a slow deterministic horizon phase with bounded focus, pulse and
 * particle modulation while preserving offline rendering and Fold continuity.
 */
public class SingularityUniverseControllerV18 {
    private final ContinuumUniverseControllerV17 continuum;
    private float horizonPhase;
    private float displayContinuity = 1f;
    private float gravityBias;

    public SingularityUniverseControllerV18(long seed, long evolutionEpoch) {
        continuum = new ContinuumUniverseControllerV17(seed, evolutionEpoch);
        long mixed = seed + evolutionEpoch * 0xD1B54A32D192ED03L;
        horizonPhase = ((mixed >>> 10) & 0xFFFFL) / 65535f * (float) (Math.PI * 2.0);
        gravityBias = ((mixed >>> 30) & 0xFFFFL) / 65535f;
    }

    public void update(float deltaSeconds, boolean mainDisplay) {
        float dt = Math.max(0f, Math.min(deltaSeconds, 0.25f));
        continuum.update(dt, mainDisplay);

        float target = mainDisplay ? 1f : 0.95f;
        float blend = Math.min(1f, dt * 0.48f);
        displayContinuity += (target - displayContinuity) * blend;

        horizonPhase += dt * (0.078f + gravityBias * 0.028f);
        if (horizonPhase > 10000f) horizonPhase -= 10000f;

        gravityBias += dt * 0.00009f;
        if (gravityBias > 1f) gravityBias -= 1f;
    }

    private float horizonWave() {
        return (float) Math.sin(horizonPhase) * displayContinuity;
    }

    private float lensWave() {
        return (float) Math.cos(horizonPhase * 0.31f + gravityBias * 6.2831855f);
    }

    public float getSceneScale() {
        return continuum.getSceneScale()
                * (1f + horizonWave() * 0.0018f + lensWave() * 0.0007f);
    }

    public float getAvatarHorizontalBias() {
        return continuum.getAvatarHorizontalBias()
                + (float) Math.sin(horizonPhase * 0.52f) * displayContinuity * 0.0015f;
    }

    public float getAvatarVerticalBias() {
        return continuum.getAvatarVerticalBias()
                + (float) Math.cos(horizonPhase * 0.41f) * displayContinuity * 0.0013f;
    }

    public float getPulseMultiplier() {
        return continuum.getPulseMultiplier()
                * (1f + horizonWave() * 0.018f + lensWave() * 0.005f);
    }

    public float getParticleMultiplier() {
        return continuum.getParticleMultiplier()
                * (1f + lensWave() * 0.016f + horizonWave() * 0.004f);
    }

    public long getFrameDelayMillis(boolean mainDisplay) {
        long base = continuum.getFrameDelayMillis(mainDisplay);
        long adjustment = Math.abs(horizonWave()) < 0.18f ? 1L : 0L;
        return Math.max(16L, Math.min(50L, base + adjustment));
    }
}
