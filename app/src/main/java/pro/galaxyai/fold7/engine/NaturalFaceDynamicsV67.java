package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v67 Natural Face Dynamics.
 *
 * Coordinates fixation gaze, irregular microsaccades, head follow and a critically damped
 * speech envelope. This controller consumes only local AI state and TTS lifecycle values.
 * It does not read camera, microphone, precise location, biometrics or raw media.
 */
public final class NaturalFaceDynamicsV67 {
    private final AttentionGazeControllerV46 gaze = new AttentionGazeControllerV46();
    private final HeadEyeCoordinationV49 headEye = new HeadEyeCoordinationV49();

    private float time;
    private float microClock;
    private float microDuration = 0.78f;
    private int microIndex;
    private float microX;
    private float microY;
    private float gazeX;
    private float gazeY;
    private float mouth;
    private float mouthVelocity;
    private float speech;
    private float speechVelocity;
    private float focus = 0.42f;
    private float serenity = 0.72f;
    private String emotion = "calm";

    public void update(SceneDecision decision,
                       float rawMouth,
                       float rawSpeech,
                       float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;

        if (decision != null) {
            float k = Math.min(1f, dt * 2.8f);
            focus += (clamp(decision.focus, 0f, 1f) - focus) * k;
            serenity += (clamp(decision.serenity, 0f, 1f) - serenity) * k;
            emotion = normalizeEmotion(decision.avatarState);
        }

        gaze.update(decision, dt);
        updateMicrosaccade(dt);

        float microGain = "focused".equals(emotion) ? 0.38f
                : ("thinking".equals(emotion) ? 0.92f
                : ("sleep".equals(emotion) ? 0.04f : 0.64f));
        gazeX = clamp(gaze.getGazeX() + microX * microGain, -1f, 1f);
        gazeY = clamp(gaze.getGazeY() + microY * microGain, -1f, 1f);
        headEye.update(decision, gazeX, gazeY, dt);

        // Human lips accelerate into opening faster than they snap shut. The damped envelope
        // keeps Russian TTS articulation soft and avoids the rubber-mouth effect.
        float targetMouth = clamp(rawMouth, 0f, 1f);
        float mouthStiffness = targetMouth > mouth ? 34f : 20f;
        float mouthDamping = targetMouth > mouth ? 10.5f : 8.8f;
        mouthVelocity += ((targetMouth - mouth) * mouthStiffness
                - mouthVelocity * mouthDamping) * dt;
        mouth += mouthVelocity * dt;
        mouth = clamp(mouth, 0f, 1f);

        float targetSpeech = clamp(rawSpeech, 0f, 1f);
        float speechStiffness = targetSpeech > speech ? 26f : 14f;
        float speechDamping = targetSpeech > speech ? 9.2f : 7.4f;
        speechVelocity += ((targetSpeech - speech) * speechStiffness
                - speechVelocity * speechDamping) * dt;
        speech += speechVelocity * dt;
        speech = clamp(speech, 0f, 1f);
    }

    public void onTouch(float normalizedX, float normalizedY, boolean pressed) {
        gaze.onTouch(normalizedX, normalizedY, pressed);
    }

    public float getGazeX() { return gazeX; }
    public float getGazeY() { return gazeY; }
    public float getHeadFollowX() { return headEye.getFollowX(); }
    public float getHeadFollowY() { return headEye.getFollowY(); }
    public float getHeadRoll() { return headEye.getRoll(); }
    public float getMouthOpen() { return mouth; }
    public float getSpeechEnergy() { return speech; }

    private void updateMicrosaccade(float dt) {
        microClock += dt;
        if (microClock >= microDuration) {
            microClock = 0f;
            microIndex++;

            // Deterministic irregular sequence: no sensor input and no random-state drift.
            float a = hashUnit(microIndex * 17 + 3) * 2f - 1f;
            float b = hashUnit(microIndex * 29 + 11) * 2f - 1f;
            float c = hashUnit(microIndex * 43 + 19);

            float amplitude = 0.012f + c * 0.024f;
            if ("focused".equals(emotion)) amplitude *= 0.45f;
            if ("thinking".equals(emotion)) amplitude *= 1.18f;
            if ("sleep".equals(emotion)) amplitude *= 0.05f;

            microX = a * amplitude;
            microY = b * amplitude * 0.62f;

            float stateScale = "thinking".equals(emotion) ? 0.76f
                    : ("focused".equals(emotion) ? 1.20f
                    : ("sleep".equals(emotion) ? 2.4f : 1f));
            microDuration = (0.46f + hashUnit(microIndex * 61 + 7) * 1.12f) * stateScale;
        }

        // A real microsaccade is brief, followed by a fixation plateau.
        float pulse = 1f - smoothStep(0.065f, 0.19f, microClock);
        microX *= pulse;
        microY *= pulse;
    }

    private static float hashUnit(int x) {
        int n = x;
        n = (n << 13) ^ n;
        int v = n * (n * n * 15731 + 789221) + 1376312589;
        return ((v & 0x7fffffff) % 10000) / 9999f;
    }

    private static float smoothStep(float edge0, float edge1, float x) {
        if (edge1 <= edge0) return x >= edge1 ? 1f : 0f;
        float t = clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
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
