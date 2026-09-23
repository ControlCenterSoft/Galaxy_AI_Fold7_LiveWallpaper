package pro.galaxyai.fold7.ai;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Small durable memory for the personal universe. Stores only aggregate scene
 * preferences and the last AI state; no raw sensor/event history is retained.
 */
public final class AIProfileStore {
    private static final String PREFS = "aidi_profile_v21";
    private static final String KEY_DECISIONS = "decision_count";
    private static final String KEY_REMOTE = "remote_decision_count";
    private static final String KEY_FALLBACK = "fallback_decision_count";
    private static final String KEY_LAST_SCENE = "last_scene";
    private static final String KEY_LAST_AVATAR = "last_avatar";
    private static final String KEY_SCENE_COUNTS = "scene_counts";
    private static final String KEY_UPDATED = "updated_at";

    private final SharedPreferences prefs;

    public AIProfileStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized AIProfile snapshot() {
        JSONObject counts = readCounts();
        String favorite = prefs.getString(KEY_LAST_SCENE, "continuum");
        long favoriteCount = 0L;
        Iterator<String> keys = counts.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            long count = counts.optLong(key, 0L);
            if (count > favoriteCount) {
                favorite = key;
                favoriteCount = count;
            }
        }
        return new AIProfile(
                prefs.getLong(KEY_DECISIONS, 0L),
                prefs.getLong(KEY_REMOTE, 0L),
                prefs.getLong(KEY_FALLBACK, 0L),
                prefs.getString(KEY_LAST_SCENE, "continuum"),
                prefs.getString(KEY_LAST_AVATAR, "calm"),
                favorite,
                favoriteCount,
                prefs.getLong(KEY_UPDATED, 0L));
    }

    public synchronized void recordDecision(SceneDecision decision, boolean remote) {
        if (decision == null) return;
        String scene = safeToken(decision.sceneName, "continuum");
        String avatar = safeToken(decision.avatarState, "calm");
        JSONObject counts = readCounts();
        try {
            counts.put(scene, Math.min(1_000_000L, counts.optLong(scene, 0L) + 1L));
        } catch (JSONException ignored) {
            // JSONObject backed by strings/longs should not fail; keep the last valid aggregate if it does.
        }

        long total = Math.min(1_000_000L, prefs.getLong(KEY_DECISIONS, 0L) + 1L);
        long remoteCount = prefs.getLong(KEY_REMOTE, 0L);
        long fallbackCount = prefs.getLong(KEY_FALLBACK, 0L);
        if (remote) remoteCount = Math.min(1_000_000L, remoteCount + 1L);
        else fallbackCount = Math.min(1_000_000L, fallbackCount + 1L);

        prefs.edit()
                .putLong(KEY_DECISIONS, total)
                .putLong(KEY_REMOTE, remoteCount)
                .putLong(KEY_FALLBACK, fallbackCount)
                .putString(KEY_LAST_SCENE, scene)
                .putString(KEY_LAST_AVATAR, avatar)
                .putString(KEY_SCENE_COUNTS, counts.toString())
                .putLong(KEY_UPDATED, System.currentTimeMillis())
                .apply();
    }

    private JSONObject readCounts() {
        String raw = prefs.getString(KEY_SCENE_COUNTS, "{}");
        try {
            return new JSONObject(raw == null ? "{}" : raw);
        } catch (JSONException ignored) {
            return new JSONObject();
        }
    }

    private static String safeToken(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        String normalized = value.trim().replaceAll("[^A-Za-z0-9_.-]", "_");
        if (normalized.isEmpty()) return fallback;
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }
}
