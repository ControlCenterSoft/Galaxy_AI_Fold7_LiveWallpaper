package pro.galaxyai.fold7.ai;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Local, privacy-first personalization profile for v30.
 *
 * Stores only bounded rendering/voice preferences. The local profile id never leaves the device;
 * Gateway serialization contains only coarse non-sensitive preference values.
 */
public final class AIPersonalizationProfileV30 {
    public static final String PREFS = "personalized_avatar_v30";
    private static final String KEY_PROFILE_ID = "profile_id";
    private static final String KEY_PRESET = "preset";
    private static final String KEY_WARMTH = "appearance_warmth";
    private static final String KEY_EYE_GLOW = "eye_glow";
    private static final String KEY_EXPRESSION = "expression_intensity";
    private static final String KEY_VOICE_PITCH = "voice_pitch";
    private static final String KEY_VOICE_RATE = "voice_rate";

    public static final String PRESET_BALANCED = "balanced";
    public static final String PRESET_SOFT = "soft";
    public static final String PRESET_VIVID = "vivid";

    private final SharedPreferences prefs;

    public AIPersonalizationProfileV30(Context context) {
        if (context == null) throw new IllegalArgumentException("context == null");
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        ensureProfileId();
    }

    public Snapshot snapshot() {
        return new Snapshot(
                safePreset(prefs.getString(KEY_PRESET, PRESET_BALANCED)),
                clamp(prefs.getFloat(KEY_WARMTH, 0.50f), 0f, 1f),
                clamp(prefs.getFloat(KEY_EYE_GLOW, 0.62f), 0f, 1f),
                clamp(prefs.getFloat(KEY_EXPRESSION, 0.60f), 0.35f, 1f),
                clamp(prefs.getFloat(KEY_VOICE_PITCH, 1.02f), 0.75f, 1.30f),
                clamp(prefs.getFloat(KEY_VOICE_RATE, 0.92f), 0.70f, 1.25f)
        );
    }

    public String getLocalProfileId() {
        return ensureProfileId();
    }

    public void applyPreset(String preset) {
        String safe = safePreset(preset);
        SharedPreferences.Editor e = prefs.edit().putString(KEY_PRESET, safe);
        if (PRESET_SOFT.equals(safe)) {
            e.putFloat(KEY_WARMTH, 0.72f)
                    .putFloat(KEY_EYE_GLOW, 0.46f)
                    .putFloat(KEY_EXPRESSION, 0.48f)
                    .putFloat(KEY_VOICE_PITCH, 0.96f)
                    .putFloat(KEY_VOICE_RATE, 0.84f);
        } else if (PRESET_VIVID.equals(safe)) {
            e.putFloat(KEY_WARMTH, 0.42f)
                    .putFloat(KEY_EYE_GLOW, 0.92f)
                    .putFloat(KEY_EXPRESSION, 0.92f)
                    .putFloat(KEY_VOICE_PITCH, 1.10f)
                    .putFloat(KEY_VOICE_RATE, 1.05f);
        } else {
            e.putFloat(KEY_WARMTH, 0.50f)
                    .putFloat(KEY_EYE_GLOW, 0.62f)
                    .putFloat(KEY_EXPRESSION, 0.60f)
                    .putFloat(KEY_VOICE_PITCH, 1.02f)
                    .putFloat(KEY_VOICE_RATE, 0.92f);
        }
        e.apply();
    }

    public void setAppearance(float warmth, float eyeGlow, float expressionIntensity) {
        prefs.edit()
                .putFloat(KEY_WARMTH, clamp(warmth, 0f, 1f))
                .putFloat(KEY_EYE_GLOW, clamp(eyeGlow, 0f, 1f))
                .putFloat(KEY_EXPRESSION, clamp(expressionIntensity, 0.35f, 1f))
                .putString(KEY_PRESET, "custom")
                .apply();
    }

    public void setVoiceManner(float pitch, float rate) {
        prefs.edit()
                .putFloat(KEY_VOICE_PITCH, clamp(pitch, 0.75f, 1.30f))
                .putFloat(KEY_VOICE_RATE, clamp(rate, 0.70f, 1.25f))
                .putString(KEY_PRESET, "custom")
                .apply();
    }

    /** Clears personalization only. AIDI learned memory is deliberately kept separate. */
    public void resetProfile() {
        prefs.edit().clear().apply();
        ensureProfileId();
        applyPreset(PRESET_BALANCED);
    }

    private String ensureProfileId() {
        String id = prefs.getString(KEY_PROFILE_ID, "");
        if (id == null || id.length() < 8) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_PROFILE_ID, id).apply();
        }
        return id;
    }

    private static String safePreset(String value) {
        if (PRESET_SOFT.equals(value) || PRESET_VIVID.equals(value)
                || PRESET_BALANCED.equals(value) || "custom".equals(value)) return value;
        return PRESET_BALANCED;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class Snapshot {
        public final String preset;
        public final float appearanceWarmth;
        public final float eyeGlow;
        public final float expressionIntensity;
        public final float voicePitch;
        public final float voiceRate;

        Snapshot(String preset, float appearanceWarmth, float eyeGlow,
                 float expressionIntensity, float voicePitch, float voiceRate) {
            this.preset = preset;
            this.appearanceWarmth = appearanceWarmth;
            this.eyeGlow = eyeGlow;
            this.expressionIntensity = expressionIntensity;
            this.voicePitch = voicePitch;
            this.voiceRate = voiceRate;
        }

        /** No local profile id, names, account data, raw media or sensor history are serialized. */
        public JSONObject toGatewayJson() throws JSONException {
            JSONObject root = new JSONObject();
            root.put("schema", "avatar-personalization-v1");
            root.put("preset", preset);
            root.put("appearance_warmth", appearanceWarmth);
            root.put("eye_glow", eyeGlow);
            root.put("expression_intensity", expressionIntensity);
            root.put("voice_pitch", voicePitch);
            root.put("voice_rate", voiceRate);
            root.put("contains_sensitive_data", false);
            return root;
        }
    }
}
