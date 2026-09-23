package pro.galaxyai.fold7.engine;

/**
 * v17 Continuum Universe layer over v16.
 * Adds long-horizon deterministic phase evolution and smooth continuity
 * across Fold display changes while remaining offline and battery-aware.
 */
public class ContinuumUniverseControllerV17 {
    private final EntangledUniverseControllerV16 entangled;
    private float continuumPhase;
    private float foldContinuity = 1f;
    private float longHorizonDrift;

    public ContinuumUniverseControllerV17(long seed, long evolutionEpoch) {
        entangled = new EntangledUniverseControllerV16(seed, evolutionEpoch);
        long mixed = seed ^ (evolutionEpoch * 0x9E3779B97F4A7C15L);
        continuumPhase = ((mixed >>> 16) & 0xFFFFL) / 65535f * (float) (Math.PI * 2.0);
        longHorizonDrift = ((mixed >>> 32) & 0xFFFFL) / 65535f;
    }

    public void update(float deltaSeconds, boolean mainDisplay) {
        float dt = Math.max(0f, Math.min(deltaSeconds, 0.25f));
        entangled.update(dt, mainDisplay);

        float target = mainDisplay ? 1f : 0.94f;
        float blend = Math.min(1f, dt * 0.55f);
        foldContinuity += (target - foldContinuity) * blend;

        continuumPhase += dt * (0.105f + longHorizonDrift * 0.035f);
        if (continuumPhase > 10000f) continuumPhase -= 10000f;

        longHorizonDrift += dt * 0.00012f;
        if (longHorizonDrift > 1f) longHorizonDrift -= 1f;
    }

    private float primaryWave() {
        return (float) Math.sin(continuumPhase) * foldContinuity;
    }

    private float secondaryWave() {
        return (float) Math.cos(continuumPhase * 0.43f + longHorizonDrift * 6.2831855f);
    }

    public float getSceneScale() {
        return entangled.getSceneScale()
                * (1f + primaryWave() * 0.0014f + secondaryWave() * 0.0005f);
    }

    public float getAvatarHorizontalBias() {
        return entangled.getAvatarHorizontalBias()
                + (float) Math.sin(continuumPhase * 0.61f) * foldContinuity * 0.0012f;
    }

    public float getAvatarVerticalBias() {
        return entangled.getAvatarVerticalBias()
                + (float) Math.cos(continuumPhase * 0.37f) * foldContinuity * 0.0010f;
    }

    public float getPulseMultiplier() {
        return entangled.getPulseMultiplier()
                * (1f + primaryWave() * 0.014f + secondaryWave() * 0.004f);
    }

    public float getParticleMultiplier() {
        return entangled.getParticleMultiplier()
                * (1f + secondaryWave() * 0.013f);
    }

    public long getFrameDelayMillis(boolean mainDisplay) {
        long base = entangled.getFrameDelayMillis(mainDisplay);
        // Preserve v16 battery pacing; only allow a tiny bounded continuum adjustment.
        long adjustment = secondaryWave() > 0.65f ? 1L : 0L;
        return Math.max(16L, Math.min(50L, base + adjustment));
    }
}
