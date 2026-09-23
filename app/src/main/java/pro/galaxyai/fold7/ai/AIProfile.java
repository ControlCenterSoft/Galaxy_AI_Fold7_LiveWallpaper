package pro.galaxyai.fold7.ai;

import org.json.JSONException;
import org.json.JSONObject;

/** Privacy-minimal durable summary of the wallpaper's learned behavior. */
public final class AIProfile {
    public final long decisionCount;
    public final long remoteDecisionCount;
    public final long fallbackDecisionCount;
    public final String lastScene;
    public final String lastAvatarState;
    public final String favoriteScene;
    public final long favoriteSceneCount;
    public final long updatedAtMs;

    public AIProfile(long decisionCount, long remoteDecisionCount,
                     long fallbackDecisionCount, String lastScene,
                     String lastAvatarState, String favoriteScene,
                     long favoriteSceneCount, long updatedAtMs) {
        this.decisionCount = Math.max(0L, decisionCount);
        this.remoteDecisionCount = Math.max(0L, remoteDecisionCount);
        this.fallbackDecisionCount = Math.max(0L, fallbackDecisionCount);
        this.lastScene = safe(lastScene, "continuum");
        this.lastAvatarState = safe(lastAvatarState, "calm");
        this.favoriteScene = safe(favoriteScene, this.lastScene);
        this.favoriteSceneCount = Math.max(0L, favoriteSceneCount);
        this.updatedAtMs = Math.max(0L, updatedAtMs);
    }

    public static AIProfile empty() {
        return new AIProfile(0L, 0L, 0L, "continuum", "calm",
                "continuum", 0L, 0L);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("decision_count", decisionCount);
        object.put("remote_decision_count", remoteDecisionCount);
        object.put("fallback_decision_count", fallbackDecisionCount);
        object.put("last_scene", lastScene);
        object.put("last_avatar", lastAvatarState);
        object.put("favorite_scene", favoriteScene);
        object.put("favorite_scene_count", favoriteSceneCount);
        // Only coarse age is sent; no raw event history or exact timestamps leave the device.
        long ageMinutes = updatedAtMs == 0L
                ? -1L
                : Math.max(0L, (System.currentTimeMillis() - updatedAtMs) / 60_000L);
        object.put("age_minutes", ageMinutes);
        return object;
    }

    private static String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        String trimmed = value.trim();
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }
}
