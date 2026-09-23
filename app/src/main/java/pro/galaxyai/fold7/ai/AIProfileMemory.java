package pro.galaxyai.fold7.ai;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Small on-device memory for scene and avatar personalization.
 * Stores only bounded rendering preferences derived from prior decisions.
 * No raw sensor history, text prompts, account data or precise location are persisted.
 */
public final class AIProfileMemory {
    private static final String PREFS = "galaxy_ai_profile_v21";
    private static final String KEY_SAMPLES = "samples";
    private static final String KEY_AVG_ENERGY = "avg_energy";
    private static final String KEY_AVG_PARTICLES = "avg_particles";
    private static final String KEY_AVG_PRESENCE = "avg_presence";
    private static final String KEY_AVG_SERENITY = "avg_serenity";
    private static final String KEY_AVG_CURIOSITY = "avg_curiosity";
    private static final String KEY_AVG_FOCUS = "avg_focus";
    private static final String KEY_LAST_SCENE = "last_scene";
    private static final String KEY_LAST_AVATAR = "last_avatar";
    private static final String KEY_GATEWAY_SUCCESS = "gateway_success";
    private static final String KEY_FALLBACK_COUNT = "fallback_count";

    private static final float ALPHA = 0.16f;

    private final SharedPreferences prefs;

    public AIProfileMemory(Context context) {
        if (context == null) throw new IllegalArgumentException("context == null");
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized void record(SceneDecision decision, boolean gatewaySuccess) {
        if (decision == null) return;
        int samples = Math.max(0, prefs.getInt(KEY_SAMPLES, 0));
        float previousEnergy = prefs.getFloat(KEY_AVG_ENERGY, decision.energy);
        float previousParticles = prefs.getFloat(KEY_AVG_PARTICLES, decision.particleMultiplier);
        float previousPresence = prefs.getFloat(KEY_AVG_PRESENCE, decision.avatarPresence);
        float previousSerenity = prefs.getFloat(KEY_AVG_SERENITY, decision.serenity);
        float previousCuriosity = prefs.getFloat(KEY_AVG_CURIOSITY, decision.curiosity);
        float previousFocus = prefs.getFloat(KEY_AVG_FOCUS, decision.focus);

        SharedPreferences.Editor editor = prefs.edit()
                .putInt(KEY_SAMPLES, Math.min(1000000, samples + 1))
                .putFloat(KEY_AVG_ENERGY, smooth(previousEnergy, decision.energy, samples, 0f, 1f))
                .putFloat(KEY_AVG_PARTICLES, smooth(previousParticles, decision.particleMultiplier, samples, 0.45f, 1.8f))
                .putFloat(KEY_AVG_PRESENCE, smooth(previousPresence, decision.avatarPresence, samples, 0f, 1f))
                .putFloat(KEY_AVG_SERENITY, smooth(previousSerenity, decision.serenity, samples, 0f, 1f))
                .putFloat(KEY_AVG_CURIOSITY, smooth(previousCuriosity, decision.curiosity, samples, 0f, 1f))
                .putFloat(KEY_AVG_FOCUS, smooth(previousFocus, decision.focus, samples, 0f, 1f))
                .putString(KEY_LAST_SCENE, safe(decision.sceneName, "continuum", 64))
                .putString(KEY_LAST_AVATAR, safe(decision.avatarState, "calm", 32));

        if (gatewaySuccess) {
            editor.putInt(KEY_GATEWAY_SUCCESS,
                    Math.min(1000000, Math.max(0, prefs.getInt(KEY_GATEWAY_SUCCESS, 0)) + 1));
        } else {
            editor.putInt(KEY_FALLBACK_COUNT,
                    Math.min(1000000, Math.max(0, prefs.getInt(KEY_FALLBACK_COUNT, 0)) + 1));
        }
        editor.apply();
    }

    public synchronized ProfileSnapshot snapshot() {
        return new ProfileSnapshot(
                Math.max(0, prefs.getInt(KEY_SAMPLES, 0)),
                clamp(prefs.getFloat(KEY_AVG_ENERGY, 0.45f), 0f, 1f),
                clamp(prefs.getFloat(KEY_AVG_PARTICLES, 1.0f), 0.45f, 1.8f),
                clamp(prefs.getFloat(KEY_AVG_PRESENCE, 0.55f), 0f, 1f),
                clamp(prefs.getFloat(KEY_AVG_SERENITY, 0.70f), 0f, 1f),
                clamp(prefs.getFloat(KEY_AVG_CURIOSITY, 0.40f), 0f, 1f),
                clamp(prefs.getFloat(KEY_AVG_FOCUS, 0.45f), 0f, 1f),
                safe(prefs.getString(KEY_LAST_SCENE, "continuum"), "continuum", 64),
                safe(prefs.getString(KEY_LAST_AVATAR, "calm"), "calm", 32),
                Math.max(0, prefs.getInt(KEY_GATEWAY_SUCCESS, 0)),
                Math.max(0, prefs.getInt(KEY_FALLBACK_COUNT, 0))
        );
    }

    private static float smooth(float previous, float next, int samples, float min, float max) {
        float value = samples == 0 ? next : previous + ALPHA * (next - previous);
        return clamp(value, min, max);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String safe(String value, String fallback, int maxLength) {
        if (value == null || value.length() == 0) return fallback;
        return value.substring(0, Math.min(maxLength, value.length()));
    }

    public static final class ProfileSnapshot {
        public final int samples;
        public final float averageEnergy;
        public final float averageParticles;
        public final float averagePresence;
        public final float averageSerenity;
        public final float averageCuriosity;
        public final float averageFocus;
        public final String lastScene;
        public final String lastAvatar;
        public final int gatewaySuccesses;
        public final int fallbackCount;

        ProfileSnapshot(int samples, float averageEnergy, float averageParticles,
                        float averagePresence, float averageSerenity,
                        float averageCuriosity, float averageFocus,
                        String lastScene, String lastAvatar,
                        int gatewaySuccesses, int fallbackCount) {
            this.samples = samples;
            this.averageEnergy = averageEnergy;
            this.averageParticles = averageParticles;
            this.averagePresence = averagePresence;
            this.averageSerenity = averageSerenity;
            this.averageCuriosity = averageCuriosity;
            this.averageFocus = averageFocus;
            this.lastScene = lastScene;
            this.lastAvatar = lastAvatar;
            this.gatewaySuccesses = gatewaySuccesses;
            this.fallbackCount = fallbackCount;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject personality = new JSONObject();
            personality.put("presence", averagePresence);
            personality.put("serenity", averageSerenity);
            personality.put("curiosity", averageCuriosity);
            personality.put("focus", averageFocus);

            JSONObject root = new JSONObject();
            root.put("schema", "profile-v2");
            root.put("samples", samples);
            root.put("average_energy", averageEnergy);
            root.put("average_particles", averageParticles);
            root.put("personality", personality);
            root.put("last_scene", lastScene);
            root.put("last_avatar", lastAvatar);
            root.put("gateway_successes", gatewaySuccesses);
            root.put("fallback_count", fallbackCount);
            return root;
        }
    }
}
