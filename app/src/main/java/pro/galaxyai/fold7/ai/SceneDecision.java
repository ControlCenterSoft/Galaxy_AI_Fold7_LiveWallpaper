package pro.galaxyai.fold7.ai;

import org.json.JSONObject;

public final class SceneDecision {
    public final String sceneName;
    public final String avatarState;
    public final float energy;
    public final float particleMultiplier;
    public final float pulseMultiplier;
    public final float sceneScaleMultiplier;
    public final float horizontalBias;
    public final float verticalBias;
    public final long ttlSeconds;
    public final String source;

    public SceneDecision(String sceneName, String avatarState, float energy,
                         float particleMultiplier, float pulseMultiplier,
                         float sceneScaleMultiplier, float horizontalBias,
                         float verticalBias, long ttlSeconds, String source) {
        this.sceneName = sceneName == null ? "continuum" : sceneName;
        this.avatarState = avatarState == null ? "calm" : avatarState;
        this.energy = clamp(energy, 0f, 1f);
        this.particleMultiplier = clamp(particleMultiplier, 0.45f, 1.8f);
        this.pulseMultiplier = clamp(pulseMultiplier, 0.6f, 1.5f);
        this.sceneScaleMultiplier = clamp(sceneScaleMultiplier, 0.97f, 1.03f);
        this.horizontalBias = clamp(horizontalBias, -0.02f, 0.02f);
        this.verticalBias = clamp(verticalBias, -0.02f, 0.02f);
        this.ttlSeconds = Math.max(60L, Math.min(3600L, ttlSeconds));
        this.source = source == null ? "unknown" : source;
    }

    public static SceneDecision neutral(String source) {
        return new SceneDecision("continuum", "calm", 0.45f,
                1f, 1f, 1f, 0f, 0f, 300L, source);
    }

    public static SceneDecision fromGateway(JSONObject root) {
        JSONObject scene = root.optJSONObject("scene");
        JSONObject avatar = root.optJSONObject("avatar");
        String sceneName = scene != null
                ? scene.optString("name", "continuum")
                : root.optString("scene", "continuum");
        String avatarState = avatar != null
                ? avatar.optString("state", "calm")
                : root.optString("avatar", "calm");

        double energy = scene != null ? scene.optDouble("energy", 0.45) : root.optDouble("energy", 0.45);
        double particles = scene != null ? scene.optDouble("particle_multiplier", 1.0) : root.optDouble("particle_multiplier", 1.0);
        double pulse = scene != null ? scene.optDouble("pulse_multiplier", 1.0) : root.optDouble("pulse_multiplier", 1.0);
        double scale = scene != null ? scene.optDouble("scene_scale", 1.0) : root.optDouble("scene_scale", 1.0);
        double x = scene != null ? scene.optDouble("avatar_x_bias", 0.0) : 0.0;
        double y = scene != null ? scene.optDouble("avatar_y_bias", 0.0) : 0.0;
        long ttl = root.optLong("ttl", 900L);

        return new SceneDecision(sceneName, avatarState, (float) energy,
                (float) particles, (float) pulse, (float) scale,
                (float) x, (float) y, ttl, "aidi-gateway");
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
