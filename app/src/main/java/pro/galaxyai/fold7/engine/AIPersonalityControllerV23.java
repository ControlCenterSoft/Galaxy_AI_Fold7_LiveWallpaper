package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * Converts bounded v23 personality traits into smooth visual behavior.
 * Traits come from the AIDI decision (or deterministic local fallback defaults).
 */
public final class AIPersonalityControllerV23 {
    private SceneDecision target = SceneDecision.neutral("personality-bootstrap");
    private float presence = 0.55f;
    private float serenity = 0.70f;
    private float curiosity = 0.40f;
    private float focus = 0.45f;
    private float phase = 0f;

    public void setDecision(SceneDecision decision) {
        if (decision != null) target = decision;
    }

    public void update(float deltaSeconds) {
        float dt = Math.max(0f, Math.min(deltaSeconds, 0.25f));
        float blend = Math.min(1f, dt * 0.42f);
        presence = lerp(presence, target.avatarPresence, blend);
        serenity = lerp(serenity, target.serenity, blend);
        curiosity = lerp(curiosity, target.curiosity, blend);
        focus = lerp(focus, target.focus, blend);
        phase += dt * (0.18f + curiosity * 0.18f);
        if (phase > 10000f) phase -= 10000f;
    }

    public float getAvatarScale() {
        float breath = (float) Math.sin(phase) * (0.006f + serenity * 0.007f);
        return 0.955f + presence * 0.085f + breath;
    }

    public float getGlowMultiplier() {
        return 0.86f + serenity * 0.18f + presence * 0.08f;
    }

    public float getHologramMultiplier() {
        return 0.82f + curiosity * 0.22f + focus * 0.12f;
    }

    public float getAuraParticleMultiplier() {
        return 0.84f + curiosity * 0.24f + presence * 0.10f;
    }

    public float getHorizontalDrift() {
        return (float) Math.sin(phase * 0.63f) * curiosity * 0.0025f;
    }

    public float getVerticalDrift() {
        return (float) Math.cos(phase * 0.49f) * (1f - focus) * 0.0018f;
    }

    public String getState() {
        return target.avatarState;
    }

    private static float lerp(float current, float desired, float amount) {
        return current + (desired - current) * amount;
    }
}
