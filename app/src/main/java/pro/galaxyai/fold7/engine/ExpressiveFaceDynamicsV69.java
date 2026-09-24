package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v69 Expressive Face Coupling, refined by v72 conversational blink timing.
 *
 * Keeps the privacy-safe v67 gaze/head controller, adds deterministic human-like attention
 * pulses and couples natural blinks to attention shifts and Russian TTS phrase boundaries.
 * Speech articulation remains local and damped. No camera, microphone, biometrics, precise
 * location or raw media are read or transmitted.
 */
public final class ExpressiveFaceDynamicsV69 {
    private final NaturalFaceDynamicsV67 base = new NaturalFaceDynamicsV67();

    private float time;
    private float attentionClock;
    private float nextAttentionPulse = 3.6f;
    private float pulseClock;
    private float pulseX;
    private float pulseY;
    private int pulseIndex;
    private float blinkCue;
    private float blinkRefractory;

    private float mouth;
    private float mouthVelocity;
    private float speech;
    private float speechVelocity;
    private float previousRawSpeech;
    private float pauseAccent;
    private String emotion = "calm";

    public void update(SceneDecision decision,
                       float rawMouth,
                       float rawSpeech,
                       float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;

        if (decision != null) {
            emotion = normalizeEmotion(decision.avatarState);
        }

        base.update(decision, rawMouth, rawSpeech, dt);
        updateAttentionPulse(rawSpeech, dt);
        updateSpeechArticulation(rawMouth, rawSpeech, dt);
        previousRawSpeech = rawSpeech;
    }

    public void onTouch(float normalizedX, float normalizedY, boolean pressed) {
        base.onTouch(normalizedX, normalizedY, pressed);
        if (pressed) {
            // Direct user attention should feel intentional, not twitchy.
            attentionClock = 0f;
            pulseClock = 0f;
            pulseX = 0f;
            pulseY = 0f;
            blinkCue = 0f;
            blinkRefractory = Math.max(blinkRefractory, 0.45f);
        }
    }

    public float getGazeX() {
        return clamp(base.getGazeX() + pulseX, -1f, 1f);
    }

    public float getGazeY() {
        return clamp(base.getGazeY() + pulseY, -1f, 1f);
    }

    public float getHeadFollowX() {
        return base.getHeadFollowX();
    }

    public float getHeadFollowY() {
        return base.getHeadFollowY();
    }

    public float getHeadRoll() {
        return base.getHeadRoll();
    }

    public float getMouthOpen() {
        return mouth;
    }

    public float getSpeechEnergy() {
        return speech;
    }

    public float getBlinkCueStrength() {
        float magnitude = (float) Math.sqrt(pulseX * pulseX + pulseY * pulseY);
        float saccadeCue = clamp(magnitude / 0.040f, 0f, 1f);
        return Math.max(saccadeCue, clamp(blinkCue, 0f, 1f));
    }

    private void updateAttentionPulse(float rawSpeech, float dt) {
        attentionClock += dt;
        pulseClock = Math.max(0f, pulseClock - dt);
        blinkRefractory = Math.max(0f, blinkRefractory - dt);
        blinkCue *= (float) Math.pow(0.020f, dt);
        if (blinkCue < 0.005f) blinkCue = 0f;

        boolean phraseBoundary = previousRawSpeech > 0.34f && rawSpeech < 0.13f;
        boolean timed = attentionClock >= nextAttentionPulse;
        boolean allowed = !"sleep".equals(emotion);

        if (!allowed) {
            blinkCue = 0f;
            pulseX *= (float) Math.pow(0.02f, dt);
            pulseY *= (float) Math.pow(0.02f, dt);
        }

        if (allowed && (timed || phraseBoundary)) {
            pulseIndex++;
            attentionClock = 0f;

            float direction = ((pulseIndex & 1) == 0) ? 1f : -1f;
            float jitter = hashUnit(pulseIndex * 37 + 11) * 2f - 1f;
            float vertical = hashUnit(pulseIndex * 53 + 7) * 2f - 1f;

            float amplitude = "focused".equals(emotion) ? 0.021f
                    : ("thinking".equals(emotion) ? 0.038f
                    : ("happy".equals(emotion) ? 0.034f : 0.029f));
            if (phraseBoundary) amplitude *= 0.82f;

            pulseX = direction * amplitude * (0.76f + Math.abs(jitter) * 0.24f);
            pulseY = vertical * amplitude * 0.34f;
            pulseClock = 0.115f;

            // v72: conversational blinks are explicit rather than relying only on eye velocity.
            // Phrase endings are the strongest cue; autonomous attention shifts blink less often.
            if (blinkRefractory <= 0f) {
                if (phraseBoundary) {
                    blinkCue = "focused".equals(emotion) ? 0.66f : 0.92f;
                    blinkRefractory = 1.55f;
                } else {
                    float chance = hashUnit(pulseIndex * 97 + 23);
                    float threshold = "thinking".equals(emotion) ? 0.48f
                            : ("happy".equals(emotion) ? 0.58f : 0.64f);
                    if (chance > threshold) {
                        blinkCue = 0.58f + (chance - threshold) * 0.72f;
                        blinkRefractory = 1.85f;
                    }
                }
            }

            float intervalBase = "focused".equals(emotion) ? 4.8f
                    : ("thinking".equals(emotion) ? 2.9f
                    : ("happy".equals(emotion) ? 3.2f : 3.7f));
            nextAttentionPulse = intervalBase
                    + hashUnit(pulseIndex * 71 + 5) * 1.75f;
        }

        if (pulseClock > 0f) {
            float normalized = 1f - pulseClock / 0.115f;
            float envelope = normalized < 0.28f
                    ? smoothStep(normalized / 0.28f)
                    : 1f - smoothStep((normalized - 0.28f) / 0.72f);
            pulseX *= 0.72f + envelope * 0.28f;
            pulseY *= 0.72f + envelope * 0.28f;
        } else {
            pulseX *= (float) Math.pow(0.035f, dt);
            pulseY *= (float) Math.pow(0.035f, dt);
            if (Math.abs(pulseX) < 0.0001f) pulseX = 0f;
            if (Math.abs(pulseY) < 0.0001f) pulseY = 0f;
        }
    }

    private void updateSpeechArticulation(float rawMouth, float rawSpeech, float dt) {
        float baseMouth = base.getMouthOpen();
        float baseSpeech = base.getSpeechEnergy();

        float speechDrop = clamp(previousRawSpeech - rawSpeech, 0f, 1f);
        pauseAccent += ((speechDrop > 0.15f ? 1f : 0f) - pauseAccent)
                * Math.min(1f, dt * (speechDrop > 0.15f ? 16f : 6f));

        float syllableTexture = 0.5f + 0.5f
                * (float) Math.sin(time * ("happy".equals(emotion) ? 12.8f : 10.7f) + 0.65f);
        float textureGain = clamp(rawSpeech, 0f, 1f) * 0.075f;
        float targetMouth = clamp(baseMouth
                + syllableTexture * textureGain
                - pauseAccent * 0.12f,
                0f,
                0.93f);

        float stiffness = targetMouth > mouth ? 46f : 24f;
        float damping = targetMouth > mouth ? 13.2f : 9.8f;
        mouthVelocity += ((targetMouth - mouth) * stiffness - mouthVelocity * damping) * dt;
        mouth += mouthVelocity * dt;
        mouth = clamp(mouth, 0f, 1f);

        float targetSpeech = clamp(baseSpeech * 0.92f + rawSpeech * 0.08f, 0f, 1f);
        float speechStiffness = targetSpeech > speech ? 28f : 15f;
        float speechDamping = targetSpeech > speech ? 9.8f : 7.2f;
        speechVelocity += ((targetSpeech - speech) * speechStiffness
                - speechVelocity * speechDamping) * dt;
        speech += speechVelocity * dt;
        speech = clamp(speech, 0f, 1f);

        if (rawMouth <= 0.01f && rawSpeech <= 0.01f && mouth < 0.008f) {
            mouth = 0f;
            mouthVelocity *= 0.4f;
        }
    }

    private static float hashUnit(int x) {
        int n = x;
        n = (n << 13) ^ n;
        int v = n * (n * n * 15731 + 789221) + 1376312589;
        return ((v & 0x7fffffff) % 10000) / 9999f;
    }

    private static float smoothStep(float value) {
        float v = clamp(value, 0f, 1f);
        return v * v * (3f - 2f * v);
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
