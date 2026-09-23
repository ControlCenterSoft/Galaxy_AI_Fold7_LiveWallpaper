package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v39 adaptive render-quality policy.
 *
 * Keeps portrait animation smooth while reducing unnecessary work during calm,
 * cover-display and sleep states. The policy is deterministic and uses only
 * already-available scene values; it requests no new sensors or permissions.
 */
public final class AdaptiveRenderQualityV39 {
    private float energy = 0.45f;
    private float presence = 0.75f;
    private float focus = 0.42f;
    private float quality = 0.92f;
    private String emotion = "calm";

    public void update(SceneDecision decision, float deltaSeconds, boolean mainDisplay) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        float k = Math.min(1f, dt * 2.0f);
        if (decision != null) {
            energy += (clamp(decision.energy, 0f, 1f) - energy) * k;
            presence += (clamp(decision.avatarPresence, 0.35f, 1f) - presence) * k;
            focus += (clamp(decision.focus, 0f, 1f) - focus) * k;
            emotion = normalizeEmotion(decision.avatarState);
        }

        float target = mainDisplay ? 1.0f : 0.90f;
        target += energy * 0.05f + presence * 0.03f;
        if ("focused".equals(emotion)) target += 0.02f;
        if ("happy".equals(emotion)) target += 0.03f;
        if ("sleep".equals(emotion)) target -= 0.18f;
        target = clamp(target, 0.72f, 1.08f);
        quality += (target - quality) * k;
    }

    public long adjustFrameDelay(long requestedMillis, boolean mainDisplay) {
        long requested = Math.max(24L, Math.min(80L, requestedMillis));
        long floor;
        long ceiling;

        if ("sleep".equals(emotion)) {
            floor = mainDisplay ? 48L : 55L;
            ceiling = 72L;
        } else if ("focused".equals(emotion)) {
            floor = mainDisplay ? 30L : 34L;
            ceiling = 46L;
        } else if (energy > 0.72f || "happy".equals(emotion)) {
            floor = mainDisplay ? 28L : 32L;
            ceiling = 44L;
        } else {
            floor = mainDisplay ? 32L : 37L;
            ceiling = 52L;
        }

        long adaptive = Math.max(floor, Math.min(ceiling, requested));
        if (focus < 0.20f && energy < 0.25f && !"sleep".equals(emotion)) {
            adaptive = Math.min(58L, adaptive + 5L);
        }
        return adaptive;
    }

    public float getEffectScale() {
        return clamp(0.82f + quality * 0.18f, 0.84f, 1.02f);
    }

    public float getParticleScale() {
        float activity = "sleep".equals(emotion) ? 0.72f : (0.88f + energy * 0.14f);
        return clamp(activity * getEffectScale(), 0.68f, 1.04f);
    }

    public float getHologramScale() {
        float activity = "sleep".equals(emotion) ? 0.76f : (0.90f + focus * 0.10f);
        return clamp(activity * getEffectScale(), 0.72f, 1.03f);
    }

    private static String normalizeEmotion(String value) {
        String v = value == null ? "calm" : value.trim().toLowerCase();
        if ("aware".equals(v)) return "thinking";
        if ("resting".equals(v)) return "sleep";
        if ("calm".equals(v) || "focused".equals(v) || "thinking".equals(v)
                || "happy".equals(v) || "sleep".equals(v)) return v;
        return "calm";
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
