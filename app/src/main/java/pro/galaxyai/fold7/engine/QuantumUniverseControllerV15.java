package pro.galaxyai.fold7.engine;

/**
 * v15 Quantum Universe orchestration layer over the v14 Coherent Universe.
 * Preserves the personal-universe lineage while adding deterministic phase
 * continuity, Fold-state synchronization and lightweight predictive pacing.
 * All modulation is bounded and fully offline.
 */
public class QuantumUniverseControllerV15 {
    private final CoherentUniverseControllerV14 coherent;
    private float quantumPhase;
    private float foldSync = 1f;
    private float predictiveLoad = 0.5f;

    public QuantumUniverseControllerV15(long seed, long evolutionEpoch) {
        coherent = new CoherentUniverseControllerV14(seed, evolutionEpoch);
        long mixed = seed ^ (evolutionEpoch * 0x9E3779B97F4A7C15L);
        quantumPhase = ((mixed >>> 8) & 0xFFFFL) / 65535f * (float) (Math.PI * 2.0);
    }

    public void update(float deltaSeconds, boolean mainDisplay) {
        float dt = Math.max(0f, Math.min(deltaSeconds, 0.25f));
        coherent.update(dt, mainDisplay);

        float targetSync = mainDisplay ? 1f : 0.90f;
        float syncBlend = Math.min(1f, dt * 0.9f);
        foldSync += (targetSync - foldSync) * syncBlend;

        float targetLoad = mainDisplay ? 0.58f : 0.42f;
        float loadBlend = Math.min(1f, dt * 0.45f);
        predictiveLoad += (targetLoad - predictiveLoad) * loadBlend;

        quantumPhase += dt * (0.23f + predictiveLoad * 0.05f);
        if (quantumPhase > 10000f) quantumPhase -= 10000f;
    }

    private float quantumWave() {
        return (float) Math.sin(quantumPhase) * foldSync;
    }

    public float getSceneScale() {
        return coherent.getSceneScale() * (1f + quantumWave() * 0.0010f);
    }

    public float getAvatarHorizontalBias() {
        return coherent.getAvatarHorizontalBias()
                + (float) Math.sin(quantumPhase * 0.73f) * foldSync * 0.0008f;
    }

    public float getAvatarVerticalBias() {
        return coherent.getAvatarVerticalBias()
                + (float) Math.cos(quantumPhase * 0.61f) * foldSync * 0.0007f;
    }

    public float getPulseMultiplier() {
        return coherent.getPulseMultiplier() * (1f + quantumWave() * 0.012f);
    }

    public float getParticleMultiplier() {
        return coherent.getParticleMultiplier()
                * (1f + (float) Math.cos(quantumPhase * 0.49f) * foldSync * 0.010f);
    }

    public long getFrameDelayMillis(boolean mainDisplay) {
        long base = coherent.getFrameDelayMillis(mainDisplay);
        long predictiveAdjustment = predictiveLoad > 0.55f ? -1L : 1L;
        return Math.max(30L, Math.min(60L, base + predictiveAdjustment));
    }
}
