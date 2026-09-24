package pro.galaxyai.fold7.engine;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import pro.galaxyai.fold7.ai.RussianAIPersonalityV33;
import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * Privacy-first ambient personality voice layer.
 *
 * Uses Android TextToSpeech only. No microphone, speech recognition or raw audio capture is used.
 * Voice is opt-in and defaults to disabled. All timing and enablement state are stored locally.
 * Since v33 the communication language on the phone is Russian (ru-RU) by default.
 * Since v41 a local synthetic speech-activity envelope is exposed for mouth animation; it is
 * derived only from TTS lifecycle callbacks and local time, never from microphone/audio samples.
 * v69 adds explicit user-initiated touch reactions while preserving the same privacy contract.
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
    private long lastInteractionAt;
    private int interactionSequence;
    private float desiredPitch = 1.02f;
    private float desiredRate = 0.92f;
    private volatile boolean speaking;
    private volatile long speechStartedNanos;

    public AmbientPersonalityControllerV29(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        tts = new TextToSpeech(context.getApplicationContext(), this);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                speaking = true;
                speechStartedNanos = System.nanoTime();
            }

            @Override
            public void onDone(String utteranceId) {
                speaking = false;
            }

            @Override
            public void onError(String utteranceId) {
                speaking = false;
            }

            @Override
            public void onStop(String utteranceId, boolean interrupted) {
                speaking = false;
            }
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(RussianAIPersonalityV33.locale());
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

        String emotion = RussianAIPersonalityV33.normalizeEmotion(decision.avatarState);
        long now = System.currentTimeMillis();
        long minInterval = minIntervalMillis(getReactionLevel());

        boolean emotionChanged = !emotion.equals(lastEmotion);
        if (!emotionChanged && now - lastSpokenAt < minInterval) return;
        if (now - lastSpokenAt < Math.min(30_000L, minInterval)) return;

        String phrase = RussianAIPersonalityV33.phraseFor(emotion, getReactionLevel());
        if (phrase.isEmpty()) return;

        tts.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "galaxy-ai-v41-" + now);
        lastEmotion = emotion;
        lastSpokenAt = now;
    }

    /**
     * v69: explicit long-press reaction. This never starts recognition and never accesses
     * microphone/audio input. It only speaks a short Russian phrase through Android TTS when
     * voice reactions are enabled by the user.
     */
    public void reactToInteraction(SceneDecision decision) {
        if (!isVoiceEnabled() || !ttsReady) return;

        long now = System.currentTimeMillis();
        if (now - lastInteractionAt < 2_500L) return;

        String emotion = decision == null ? "calm" : decision.avatarState;
        String phrase = RussianAIPersonalityV33.interactionPhraseFor(
                emotion,
                interactionSequence++
        );
        if (phrase.isEmpty()) return;

        tts.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "galaxy-ai-v69-touch-" + now);
        lastInteractionAt = now;
        lastSpokenAt = now;
        lastEmotion = RussianAIPersonalityV33.normalizeEmotion(emotion);
    }

    /** True only while Android TTS reports an active utterance. */
    public boolean isSpeaking() {
        return speaking;
    }

    /**
     * Returns a bounded local pseudo-viseme envelope in [0..1]. It is intentionally
     * independent of captured audio; Russian TTS lifecycle + local clock are sufficient.
     */
    public float getSpeechActivity() {
        if (!speaking) return 0f;
        float seconds = (System.nanoTime() - speechStartedNanos) / 1_000_000_000f;
        float syllable = 0.5f + 0.5f * (float) Math.sin(seconds * 15.7f);
        float consonant = 0.5f + 0.5f * (float) Math.sin(seconds * 27.4f + 0.8f);
        float phrase = 0.5f + 0.5f * (float) Math.sin(seconds * 5.2f + 0.3f);
        return clamp(0.12f + syllable * 0.50f + consonant * 0.23f + phrase * 0.15f, 0f, 1f);
    }

    public boolean isVoiceEnabled() {
        return prefs.getBoolean(KEY_VOICE_ENABLED, false);
    }

    public int getReactionLevel() {
        return clampLevel(prefs.getInt(KEY_REACTION_LEVEL, LEVEL_QUIET));
    }

    public void shutdown() {
        speaking = false;
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

    private static int clampLevel(int level) {
        return Math.max(LEVEL_QUIET, Math.min(LEVEL_EXPRESSIVE, level));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
