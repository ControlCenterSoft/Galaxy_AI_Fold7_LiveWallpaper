package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v71 Russian pseudo-viseme dynamics.
 *
 * Produces privacy-safe mouth-shape parameters from the existing Russian TTS lifecycle only.
 * It does not inspect audio buffers or microphone input. The output is a smooth sequence of
 * broad Russian vowel-like shapes (open / round / wide) that the portrait mesh applies to the
 * original lip pixels, avoiding a single repetitive open-close motion.
 */
public final class RussianVisemeDynamicsV71 {
    private float time;
    private float visemeClock;
    private float visemeDuration = 0.12f;
    private int visemeIndex;

    private float targetRound;
    private float targetWide;
    private float targetOpenScale = 1f;

    private float round;
    private float wide;
    private float openScale = 1f;
    private float mouthOpen;
    private float speechEnergy;
    private String emotion = "calm";

    public void update(boolean speaking,
                       float ttsActivity,
                       float baseMouthOpen,
                       float baseSpeechEnergy,
                       SceneDecision decision,
                       float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;
        if (decision != null) emotion = normalizeEmotion(decision.avatarState);

        float activity = clamp(ttsActivity, 0f, 1f);
        if (speaking && activity > 0.01f) {
            visemeClock -= dt;
            if (visemeClock <= 0f) {
                visemeIndex++;
                chooseNextViseme(activity);
                float cadence = 0.5f + 0.5f
                        * (float) Math.sin(visemeIndex * 1.732f + 0.41f);
                visemeDuration = 0.080f + cadence * 0.090f;
                if ("thinking".equals(emotion)) visemeDuration *= 1.06f;
                if ("happy".equals(emotion)) visemeDuration *= 0.94f;
                visemeClock = visemeDuration;
            }
        } else {
            targetRound = 0f;
            targetWide = 0f;
            targetOpenScale = 1f;
            visemeClock = 0f;
        }

        float shapeSpeed = speaking ? 10.8f : 6.0f;
        float k = Math.min(1f, dt * shapeSpeed);
        round += (targetRound - round) * k;
        wide += (targetWide - wide) * k;
        openScale += (targetOpenScale - openScale) * Math.min(1f, dt * 9.0f);

        float openTarget = clamp(baseMouthOpen * openScale, 0f, 0.94f);
        float openSpeed = openTarget > mouthOpen ? 12.5f : 8.6f;
        mouthOpen += (openTarget - mouthOpen) * Math.min(1f, dt * openSpeed);
        speechEnergy += (clamp(baseSpeechEnergy, 0f, 1f) - speechEnergy)
                * Math.min(1f, dt * 8.0f);

        if (!speaking && mouthOpen < 0.008f) mouthOpen = 0f;
    }

    public float getMouthOpen() {
        return mouthOpen;
    }

    public float getSpeechEnergy() {
        return speechEnergy;
    }

    public float getRoundness() {
        return clamp(round, 0f, 1f);
    }

    public float getWidthBias() {
        return clamp(wide, -1f, 1f);
    }

    private void chooseNextViseme(float activity) {
        // Five broad Russian vowel families. This is intentionally text/audio independent:
        // it gives the face human variety while retaining the privacy-first contract.
        int family = Math.floorMod(visemeIndex * 3 + 1, 5);
        switch (family) {
            case 0: // А / Я-like: open and moderately wide
                targetRound = 0.08f;
                targetWide = 0.55f;
                targetOpenScale = 1.08f;
                break;
            case 1: // О / Ё-like: rounded, medium-open
                targetRound = 0.82f;
                targetWide = -0.32f;
                targetOpenScale = 0.90f;
                break;
            case 2: // У / Ю-like: strongly rounded and narrower
                targetRound = 1.0f;
                targetWide = -0.62f;
                targetOpenScale = 0.72f;
                break;
            case 3: // И-like: wider, shallower opening
                targetRound = 0.02f;
                targetWide = 0.92f;
                targetOpenScale = 0.64f;
                break;
            default: // Э / Ы-like: neutral-mid articulation
                targetRound = 0.18f;
                targetWide = 0.28f;
                targetOpenScale = 0.82f;
                break;
        }

        float gain = 0.52f + activity * 0.48f;
        targetRound *= gain;
        targetWide *= gain;
        targetOpenScale = 1f + (targetOpenScale - 1f) * gain;

        if ("focused".equals(emotion)) {
            targetRound *= 0.88f;
            targetWide *= 0.86f;
        } else if ("happy".equals(emotion)) {
            targetWide = clamp(targetWide + 0.10f, -1f, 1f);
        }
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
