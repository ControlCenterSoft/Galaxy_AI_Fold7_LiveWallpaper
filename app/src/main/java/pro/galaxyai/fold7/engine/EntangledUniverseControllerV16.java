package pro.galaxyai.fold7.engine;

/**
 * v16 Entangled Universe layer over the v15 Quantum Universe.
 * Carries phase continuity across Fold display states while keeping all
 * modulation deterministic, bounded and offline.
 */
public class EntangledUniverseControllerV16 {
    private final QuantumUniverseControllerV15 quantum;
    private float entangledPhase;
    private float crossDisplayMemory = 1f;

    public EntangledUniverseControllerV16(long seed, long evolutionEpoch) {
        quantum = new QuantumUniverseControllerV15(seed, evolutionEpoch);
        long mixed = seed + evolutionEpoch * 0x632BE59BD9B4E019L;
        entangledPhase = ((mixed >>> 12) & 0xFFFFL) / 65535f * (float) (Math.PI * 2.0);
    }

    public void update(float deltaSeconds, boolean mainDisplay) {
        float dt = Math.max(0f, Math.min(deltaSeconds, 0.25f));
        quantum.update(dt, mainDisplay);

        float targetMemory = mainDisplay ? 1f : 0.92f;
        float blend = Math.min(1f, dt * 0.75f);
        crossDisplayMemory += (targetMemory - crossDisplayMemory) * blend;

        entangledPhase += dt * (0.19f + crossDisplayMemory * 0.04f);
        if (entangledPhase > 10000f) entangledPhase -= 10000f;
    }

    private float wave() {
        return (float) Math.sin(entangledPhase) * crossDisplayMemory;
    }

    public float getSceneScale() {
        return quantum.getSceneScale() * (1f + wave() * 0.0009f);
    }

    public float getAvatarHorizontalBias() {
        return quantum.getAvatarHorizontalBias()
                + (float) Math.sin(entangledPhase * 0.71f) * crossDisplayMemory * 0.0007f;
    }

    public float getAvatarVerticalBias() {
        return quantum.getAvatarVerticalBias()
                + (float) Math.cos(entangledPhase * 0.59f) * crossDisplayMemory * 0.0006f;
    }

    public float getPulseMultiplier() {
        return quantum.getPulseMultiplier() * (1f + wave() * 0.010f);
    }

    public float getParticleMultiplier() {
        return quantum.getParticleMultiplier()
                * (1f + (float) Math.cos(entangledPhase * 0.47f) * crossDisplayMemory * 0.009f);
    }

    public long getFrameDelayMillis(boolean mainDisplay) {
        return quantum.getFrameDelayMillis(mainDisplay);
    }
}
