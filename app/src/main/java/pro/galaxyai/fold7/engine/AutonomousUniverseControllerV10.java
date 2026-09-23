package pro.galaxyai.fold7.engine;

/**
 * v10 autonomous evolution layer built on top of the persistent v9 Personal Universe.
 * It keeps the user's existing universe seed, evolves presentation deterministically
 * between sessions and exposes battery-aware frame pacing without external AI calls.
 */
public class AutonomousUniverseControllerV10 {
    private enum Phase { QUIET, BALANCED, VIVID }

    private final PersonalUniverseControllerV9 personal;
    private long rngState;
    private float time;
    private float foldBlend;
    private float phaseTimer;
    private Phase phase;

    public AutonomousUniverseControllerV10(long seed, long evolutionEpoch) {
        personal = new PersonalUniverseControllerV9(seed);
        rngState = mix64(seed ^ (evolutionEpoch * 0x9E3779B97F4A7C15L));
        phase = Phase.values()[(int) (positive(rngState) % Phase.values().length)];
        phaseTimer = nextPhaseDuration();
    }

    public void update(float deltaSeconds, boolean mainDisplay) {
        float dt = Math.max(0f, Math.min(deltaSeconds, 0.25f));
        time += dt;
        personal.update(dt, mainDisplay);

        float target = mainDisplay ? 1f : 0f;
        foldBlend += (target - foldBlend) * Math.min(1f, dt * 3.5f);

        phaseTimer -= dt;
        if (phaseTimer <= 0f) {
            advancePhase();
            phaseTimer = nextPhaseDuration();
        }
    }

    public float getSceneScale() {
        float phaseScale;
        switch (phase) {
            case QUIET:
                phaseScale = 0.995f;
                break;
            case VIVID:
                phaseScale = 1.015f;
                break;
            default:
                phaseScale = 1.005f;
        }
        return personal.getSceneScale()
                * phaseScale
                * (1f + 0.004f * (float) Math.sin(time * 0.07f));
    }

    public float getAvatarHorizontalBias() {
        float drift = phase == Phase.VIVID ? 0.008f : 0.004f;
        return personal.getAvatarHorizontalBias()
                + drift * (float) Math.sin(time * 0.13f + 1.1f);
    }

    public float getAvatarVerticalBias() {
        float drift = phase == Phase.QUIET ? 0.003f : 0.006f;
        return personal.getAvatarVerticalBias()
                + drift * (float) Math.cos(time * 0.10f + 0.6f);
    }

    public float getPulseMultiplier() {
        float phasePulse;
        switch (phase) {
            case QUIET:
                phasePulse = 0.92f;
                break;
            case VIVID:
                phasePulse = 1.12f;
                break;
            default:
                phasePulse = 1.0f;
        }
        return personal.getPulseMultiplier() * phasePulse;
    }

    public float getParticleMultiplier() {
        float base = phase == Phase.VIVID ? 1.10f : (phase == Phase.QUIET ? 0.90f : 1.0f);
        return base + 0.04f * foldBlend;
    }

    /**
     * Conservative frame pacing for an always-on visual surface.
     * Vivid main-screen scenes target ~30 FPS while quiet/cover scenes reduce work.
     */
    public long getFrameDelayMillis(boolean mainDisplay) {
        if (!mainDisplay) {
            return phase == Phase.VIVID ? 40L : 50L;
        }
        switch (phase) {
            case QUIET:
                return 42L;
            case VIVID:
                return 33L;
            default:
                return 37L;
        }
    }

    private void advancePhase() {
        int next = (phase.ordinal() + 1 + (nextUnit() > 0.72f ? 1 : 0)) % Phase.values().length;
        phase = Phase.values()[next];
    }

    private float nextPhaseDuration() {
        return 45f + nextUnit() * 75f;
    }

    private float nextUnit() {
        long x = rngState;
        x ^= x << 13;
        x ^= x >>> 7;
        x ^= x << 17;
        rngState = x;
        return (positive(x) % 1_000_000L) / 999_999f;
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

    private static long positive(long value) {
        return value & 0x7fffffffffffffffL;
    }
}
