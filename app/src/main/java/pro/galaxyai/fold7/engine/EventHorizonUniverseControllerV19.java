package pro.galaxyai.fold7.engine;

/**
 * v19 Event Horizon Universe layer over v18.
 * Adds slow deterministic horizon precession and bounded visual modulation
 * while preserving offline rendering, Fold continuity and battery-aware pacing.
 */
public class EventHorizonUniverseControllerV19 {
    private final SingularityUniverseControllerV18 singularity;
    private float precessionPhase;
    private float displayContinuity = 1f;
    private float horizonMemory;

    public EventHorizonUniverseControllerV19(long seed, long evolutionEpoch) {
        singularity = new SingularityUniverseControllerV18(seed, evolutionEpoch);
        long mixed = seed ^ (evolutionEpoch * 0x94D049BB133111EBL);
        precessionPhase = ((mixed >>> 14) & 0xFFFFL) / 65535f * (float) (Math.PI * 2.0);
        horizonMemory = ((mixed >>> 34) & 0xFFFFL) / 65535f;
    }

    public void update(float deltaSeconds, boolean mainDisplay) {
        float dt = Math.max(0f, Math.min(deltaSeconds, 0.25f));
        singularity.update(dt, mainDisplay);

        float target = mainDisplay ? 1f : 0.96f;
        float blend = Math.min(1f, dt * 0.44f);
        displayContinuity += (target - displayContinuity) * blend;

        precessionPhase += dt * (0.061f + horizonMemory * 0.022f);
        if (precessionPhase > 10000f) precessionPhase -= 10000f;

        horizonMemory += dt * 0.00007f;
        if (horizonMemory > 1f) horizonMemory -= 1f;
    }

    private float precessionWave() {
        return (float) Math.sin(precessionPhase) * displayContinuity;
    }

    private float horizonWave() {
        return (float) Math.cos(precessionPhase * 0.27f + horizonMemory * 6.2831855f);
    }

    public float getSceneScale() {
        return singularity.getSceneScale()
                * (1f + precessionWave() * 0.0020f + horizonWave() * 0.0008f);
    }

    public float getAvatarHorizontalBias() {
        return singularity.getAvatarHorizontalBias()
                + (float) Math.sin(precessionPhase * 0.48f) * displayContinuity * 0.0017f;
    }

    public float getAvatarVerticalBias() {
        return singularity.getAvatarVerticalBias()
                + (float) Math.cos(precessionPhase * 0.36f) * displayContinuity * 0.0015f;
    }

    public float getPulseMultiplier() {
        return singularity.getPulseMultiplier()
                * (1f + precessionWave() * 0.020f + horizonWave() * 0.006f);
    }

    public float getParticleMultiplier() {
        return singularity.getParticleMultiplier()
                * (1f + horizonWave() * 0.018f + precessionWave() * 0.005f);
    }

    public long getFrameDelayMillis(boolean mainDisplay) {
        long base = singularity.getFrameDelayMillis(mainDisplay);
        long adjustment = Math.abs(precessionWave()) < 0.15f ? 1L : 0L;
        return Math.max(16L, Math.min(50L, base + adjustment));
    }
}
