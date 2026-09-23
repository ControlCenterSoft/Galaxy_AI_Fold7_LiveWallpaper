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
    public final float avatarPresence;
    public final float serenity;
    public final float curiosity;
    public final float focus;
    public final long ttlSeconds;
    public final String source;

    /** Backward-compatible constructor for v20-v22 callers. */
    public SceneDecision(String sceneName, String avatarState, float energy,
                         float particleMultiplier, float pulseMultiplier,
                         float sceneScaleMultiplier, float horizontalBias,
                         float verticalBias, long ttlSeconds, String source) {
        this(sceneName, avatarState, energy, particleMultiplier, pulseMultiplier,
                sceneScaleMultiplier, horizontalBias, verticalBias,
                defaultPresence(avatarState), defaultSerenity(avatarState),
                defaultCuriosity(avatarState), defaultFocus(avatarState),
                ttlSeconds, source);
    }

    public SceneDecision(String sceneName, String avatarState, float energy,
                         float particleMultiplier, float pulseMultiplier,
                         float sceneScaleMultiplier, float horizontalBias,
                         float verticalBias, float avatarPresence, float serenity,
                         float curiosity, float focus, long ttlSeconds, String source) {
        this.sceneName = sceneName == null ? "continuum" : sceneName;
        this.avatarState = normalizeAvatarState(avatarState);
        this.energy = clamp(energy, 0f, 1f);
        this.particleMultiplier = clamp(particleMultiplier, 0.45f, 1.8f);
        this.pulseMultiplier = clamp(pulseMultiplier, 0.6f, 1.5f);
        this.sceneScaleMultiplier = clamp(sceneScaleMultiplier, 0.97f, 1.03f);
        this.horizontalBias = clamp(horizontalBias, -0.02f, 0.02f);
        this.verticalBias = clamp(verticalBias, -0.02f, 0.02f);
        this.avatarPresence = clamp(avatarPresence, 0f, 1f);
        this.serenity = clamp(serenity, 0f, 1f);
        this.curiosity = clamp(curiosity, 0f, 1f);
        this.focus = clamp(focus, 0f, 1f);
        this.ttlSeconds = Math.max(60L, Math.min(3600L, ttlSeconds));
        this.source = source == null ? "unknown" : source;
    }

    public static SceneDecision neutral(String source) {
        return new SceneDecision("continuum", "calm", 0.45f,
                1f, 1f, 1f, 0f, 0f,
                0.55f, 0.72f, 0.38f, 0.42f,
                300L, source);
    }

    public static SceneDecision fromGateway(JSONObject root) {
        JSONObject scene = root.optJSONObject("scene");
        JSONObject avatar = root.optJSONObject("avatar");
        JSONObject traits = avatar != null ? avatar.optJSONObject("traits") : null;
        String sceneName = scene != null
                ? scene.optString("name", "continuum")
                : root.optString("scene", "continuum");
        String avatarState = avatar != null
                ? avatar.optString("emotion", avatar.optString("state", "calm"))
                : root.optString("avatar", "calm");

        double energy = scene != null ? scene.optDouble("energy", 0.45) : root.optDouble("energy", 0.45);
        double particles = scene != null ? scene.optDouble("particle_multiplier", 1.0) : root.optDouble("particle_multiplier", 1.0);
        double pulse = scene != null ? scene.optDouble("pulse_multiplier", 1.0) : root.optDouble("pulse_multiplier", 1.0);
        double scale = scene != null ? scene.optDouble("scene_scale", 1.0) : root.optDouble("scene_scale", 1.0);
        double x = scene != null ? scene.optDouble("avatar_x_bias", 0.0) : 0.0;
        double y = scene != null ? scene.optDouble("avatar_y_bias", 0.0) : 0.0;
        double presence = traits != null ? traits.optDouble("presence", defaultPresence(avatarState)) : defaultPresence(avatarState);
        double serenity = traits != null ? traits.optDouble("serenity", defaultSerenity(avatarState)) : defaultSerenity(avatarState);
        double curiosity = traits != null ? traits.optDouble("curiosity", defaultCuriosity(avatarState)) : defaultCuriosity(avatarState);
        double focus = traits != null ? traits.optDouble("focus", defaultFocus(avatarState)) : defaultFocus(avatarState);
        long ttl = root.optLong("ttl", 900L);

        return new SceneDecision(sceneName, avatarState, (float) energy,
                (float) particles, (float) pulse, (float) scale,
                (float) x, (float) y, (float) presence, (float) serenity,
                (float) curiosity, (float) focus, ttl, "aidi-gateway");
    }

    private static String normalizeAvatarState(String state) {
        if (state == null) return "calm";
        String value = state.trim().toLowerCase();
        if ("aware".equals(value)) return "thinking";
        if ("resting".equals(value)) return "sleep";
        if ("calm".equals(value) || "focused".equals(value) || "thinking".equals(value)
                || "happy".equals(value) || "sleep".equals(value)) return value;
        return "calm";
    }

    private static float defaultPresence(String state) {
        state = normalizeAvatarState(state);
        if ("focused".equals(state)) return 0.82f;
        if ("thinking".equals(state)) return 0.72f;
        if ("happy".equals(state)) return 0.86f;
        if ("sleep".equals(state)) return 0.38f;
        return 0.56f;
    }

    private static float defaultSerenity(String state) {
        state = normalizeAvatarState(state);
        if ("sleep".equals(state)) return 0.92f;
        if ("calm".equals(state)) return 0.80f;
        if ("focused".equals(state)) return 0.42f;
        if ("happy".equals(state)) return 0.72f;
        return 0.58f;
    }

    private static float defaultCuriosity(String state) {
        state = normalizeAvatarState(state);
        if ("thinking".equals(state)) return 0.78f;
        if ("focused".equals(state)) return 0.62f;
        if ("happy".equals(state)) return 0.58f;
        if ("sleep".equals(state)) return 0.18f;
        return 0.38f;
    }

    private static float defaultFocus(String state) {
        state = normalizeAvatarState(state);
        if ("focused".equals(state)) return 0.92f;
        if ("thinking".equals(state)) return 0.66f;
        if ("happy".equals(state)) return 0.46f;
        if ("sleep".equals(state)) return 0.20f;
        return 0.44f;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
