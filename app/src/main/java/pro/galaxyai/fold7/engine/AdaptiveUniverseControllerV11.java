package pro.galaxyai.fold7.engine;

/**
 * v11 adaptive layer over the deterministic v10 Autonomous Universe.
 * Keeps the same persistent universe identity while adding slow session-aware
 * modulation and conservative long-session frame pacing without network calls.
 */
public class AdaptiveUniverseControllerV11 {
    private final AutonomousUniverseControllerV10 autonomous;
    private final float phaseOffset;
    private float sessionTime;
    private float mainBlend;

    public AdaptiveUniverseControllerV11(long seed, long evolutionEpoch) {
        autonomous = new AutonomousUniverseControllerV10(seed, evolutionEpoch);
        long mixed = seed ^ (evolutionEpoch * 0x9E3779B97F4A7C15L);
        phaseOffset = (float) ((mixed & 0xffffL) / 65535.0 * Math.PI * 2.0);
    }

    public void update(float deltaSeconds, boolean mainDisplay) {
        float dt = Math.max(0f, Math.min(deltaSeconds, 0.25f));
        autonomous.update(dt, mainDisplay);
        sessionTime += dt;

        float target = mainDisplay ? 1f : 0f;
        mainBlend += (target - mainBlend) * Math.min(1f, dt * 2.4f);
    }

    public float getSceneScale() {
        float adaptiveBreath = 1f + 0.0035f * (float) Math.sin(sessionTime * 0.045f + phaseOffset);
        return autonomous.getSceneScale() * adaptiveBreath;
    }

    public float getAvatarHorizontalBias() {
        float drift = 0.0025f * (float) Math.sin(sessionTime * 0.08f + phaseOffset * 0.5f);
        return autonomous.getAvatarHorizontalBias() + drift;
    }

    public float getAvatarVerticalBias() {
        float drift = 0.0020f * (float) Math.cos(sessionTime * 0.065f + phaseOffset * 0.7f);
        return autonomous.getAvatarVerticalBias() + drift;
    }

    public float getPulseMultiplier() {
        float adaptation = 0.985f + 0.025f * mainBlend;
        return autonomous.getPulseMultiplier() * adaptation;
    }

    public float getParticleMultiplier() {
        float longSessionSaver = sessionTime > 900f ? 0.94f : 1f;
        return autonomous.getParticleMultiplier() * longSessionSaver;
    }

    /**
     * Retains v10 pacing and gradually reduces long-session load while keeping
     * the main display responsive after Fold transitions.
     */
    public long getFrameDelayMillis(boolean mainDisplay) {
        long base = autonomous.getFrameDelayMillis(mainDisplay);
        if (sessionTime > 1800f) {
            return base + (mainDisplay ? 3L : 5L);
        }
        if (sessionTime > 900f) {
            return base + (mainDisplay ? 2L : 3L);
        }
        return base;
    }
}
