package pro.galaxyai.fold7.engine;

/**
 * Deterministic, persistent scene personality for the v9 Personal Universe track.
 * The caller owns persistence of the seed; this controller turns it into subtle
 * motion/placement differences while preserving continuity across Fold modes.
 */
public class PersonalUniverseControllerV9 {
    private final float phaseA;
    private final float phaseB;
    private final float horizontalBias;
    private final float verticalBias;
    private float time;
    private float foldBlend;

    public PersonalUniverseControllerV9(long seed) {
        long mixedA = mix64(seed ^ 0x9E3779B97F4A7C15L);
        long mixedB = mix64(seed ^ 0xC2B2AE3D27D4EB4FL);
        phaseA = unit(mixedA) * (float) (Math.PI * 2.0);
        phaseB = unit(mixedB) * (float) (Math.PI * 2.0);
        horizontalBias = (unit(mixedA >>> 11) - 0.5f) * 0.05f;
        verticalBias = (unit(mixedB >>> 13) - 0.5f) * 0.035f;
    }

    public void update(float deltaSeconds, boolean mainDisplay) {
        time += Math.max(0f, deltaSeconds);
        float target = mainDisplay ? 1f : 0f;
        float response = Math.min(1f, deltaSeconds * 3.5f);
        foldBlend += (target - foldBlend) * response;
    }

    public float getSceneScale() {
        return 1f
                + 0.012f * (float) Math.sin(time * 0.17f + phaseA)
                + 0.008f * foldBlend;
    }

    public float getAvatarHorizontalBias() {
        return horizontalBias
                + 0.012f * (float) Math.sin(time * 0.11f + phaseB);
    }

    public float getAvatarVerticalBias() {
        return verticalBias
                + 0.009f * (float) Math.cos(time * 0.09f + phaseA);
    }

    public float getPulseMultiplier() {
        return 0.96f
                + 0.06f * (float) Math.sin(time * 0.23f + phaseB)
                + 0.03f * foldBlend;
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

    private static float unit(long value) {
        long positive = value & 0x7fffffffffffffffL;
        return (positive % 1_000_000L) / 999_999f;
    }
}
