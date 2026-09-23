package pro.galaxyai.fold7.engine;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * Privacy-first ambient personality voice layer.
 *
 * Uses Android TextToSpeech only. No microphone, speech recognition or raw audio capture is used.
 * Voice is opt-in and defaults to disabled. All timing and enablement state are stored locally.
 */
public final class AmbientPersonalityControllerV29 implements TextToSpeech.OnInitListener {
    public static final String PREFS = "ambient_personality_v29";
    public static final String KEY_VOICE_ENABLED = "voice_enabled";
    public static final String KEY_REACTION_LEVEL = "reaction_level";

    public static final int LEVEL_QUIET = 0;
    public static final int LEVEL_NORMAL = 1;
    public static final int LEVEL_EXPRESSIVE = 2;

    private final SharedPreferences prefs;
    private final TextToSpeech tts;
    private boolean ttsReady;
    private String lastEmotion = "";
    private long lastSpokenAt;
    private float desiredPitch = 1.02f;
    private float desiredRate = 0.92f;

    public AmbientPersonalityControllerV29(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        tts = new TextToSpeech(context.getApplicationContext(), this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.getDefault());
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                    && result != TextToSpeech.LANG_NOT_SUPPORTED;
            if (ttsReady) applyVoiceManner(desiredPitch, desiredRate);
        } else {
            ttsReady = false;
        }
    }

    /** v30 extension: bounded local voice manner, still without microphone or cloud audio. */
    public void applyVoiceManner(float pitch, float rate) {
        desiredPitch = clamp(pitch, 0.75f, 1.30f);
        desiredRate = clamp(rate, 0.70f, 1.25f);
        if (ttsReady) {
            tts.setPitch(desiredPitch);
            tts.setSpeechRate(desiredRate);
        }
    }

    public void maybeReact(SceneDecision decision) {
        if (!isVoiceEnabled() || !ttsReady || decision == null) return;

        String emotion = normalizeEmotion(decision.avatarState);
        long now = System.currentTimeMillis();
        long minInterval = minIntervalMillis(getReactionLevel());

        boolean emotionChanged = !emotion.equals(lastEmotion);
        if (!emotionChanged && now - lastSpokenAt < minInterval) return;
        if (now - lastSpokenAt < Math.min(30_000L, minInterval)) return;

        String phrase = phraseFor(emotion, getReactionLevel());
        if (phrase.isEmpty()) return;

        tts.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "galaxy-ai-v29-" + now);
        lastEmotion = emotion;
        lastSpokenAt = now;
    }

    public boolean isVoiceEnabled() {
        return prefs.getBoolean(KEY_VOICE_ENABLED, false);
    }

    public int getReactionLevel() {
        return clampLevel(prefs.getInt(KEY_REACTION_LEVEL, LEVEL_QUIET));
    }

    public void shutdown() {
        tts.stop();
        tts.shutdown();
    }

    public static void setVoiceEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_VOICE_ENABLED, enabled).apply();
    }

    public static boolean readVoiceEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_VOICE_ENABLED, false);
    }

    public static void setReactionLevel(Context context, int level) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_REACTION_LEVEL, clampLevel(level)).apply();
    }

    public static int readReactionLevel(Context context) {
        return clampLevel(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_REACTION_LEVEL, LEVEL_QUIET));
    }

    private static long minIntervalMillis(int level) {
        if (level == LEVEL_EXPRESSIVE) return 90_000L;
        if (level == LEVEL_NORMAL) return 240_000L;
        return 900_000L;
    }

    private static String phraseFor(String emotion, int level) {
        if ("focused".equals(emotion)) return level == LEVEL_QUIET ? "Focused." : "Focusing with you.";
        if ("thinking".equals(emotion)) return level == LEVEL_EXPRESSIVE ? "The universe is thinking with you." : "Thinking.";
        if ("happy".equals(emotion)) return level == LEVEL_EXPRESSIVE ? "Energy is bright today." : "Bright energy.";
        if ("sleep".equals(emotion)) return level == LEVEL_EXPRESSIVE ? "I will stay quiet while the universe rests." : "Resting quietly.";
        return level == LEVEL_EXPRESSIVE ? "I am here with you." : "I am here.";
    }

    private static String normalizeEmotion(String value) {
        String v = value == null ? "calm" : value.trim().toLowerCase();
        if ("aware".equals(v)) return "thinking";
        if ("resting".equals(v)) return "sleep";
        if ("calm".equals(v) || "focused".equals(v) || "thinking".equals(v)
                || "happy".equals(v) || "sleep".equals(v)) return v;
        return "calm";
    }

    private static int clampLevel(int level) {
        return Math.max(LEVEL_QUIET, Math.min(LEVEL_EXPRESSIVE, level));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
