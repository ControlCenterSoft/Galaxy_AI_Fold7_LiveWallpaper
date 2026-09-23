package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v47 Micro Expression controller.
 *
 * Produces subtle bounded expression coefficients from existing AI state only. The values
 * are consumed by the portrait bitmap mesh, so the original face pixels are deformed
 * rather than covered with synthetic geometry. No camera, microphone or biometric input.
 */
public final class MicroExpressionControllerV47 {
    private float time;
    private float smile;
    private float browLift;
    private float browPinch;
    private float cheekLift;
    private float targetSmile;
    private float targetBrowLift;
    private float targetBrowPinch;
    private float targetCheekLift;
    private float expression = 0.58f;
    private float serenity = 0.72f;
    private String emotion = "calm";

    public void update(SceneDecision decision, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;
        if (decision != null) {
            float k = Math.min(1f, dt * 3.0f);
            expression += (clamp(decision.avatarPresence, 0.25f, 1f) - expression) * k;
            serenity += (clamp(decision.serenity, 0f, 1f) - serenity) * k;
            emotion = normalizeEmotion(decision.avatarState);
        }

        configureTargets();
        float idle = (float) Math.sin(time * 0.29f + 0.7f) * 0.035f
                + (float) Math.sin(time * 0.73f + 1.6f) * 0.014f;
        targetSmile += idle * (1f - serenity) * 0.42f;
        targetBrowLift += (float) Math.sin(time * 0.19f + 2.1f) * 0.018f;

        float smooth = Math.min(1f, dt * 3.8f);
        smile += (targetSmile - smile) * smooth;
        browLift += (targetBrowLift - browLift) * smooth;
        browPinch += (targetBrowPinch - browPinch) * smooth;
        cheekLift += (targetCheekLift - cheekLift) * smooth;

        smile = clamp(smile, -0.22f, 0.82f);
        browLift = clamp(browLift, -0.30f, 0.48f);
        browPinch = clamp(browPinch, 0f, 0.48f);
        cheekLift = clamp(cheekLift, 0f, 0.58f);
    }

    private void configureTargets() {
        float e = clamp(0.55f + expression * 0.45f, 0.55f, 1f);
        if ("happy".equals(emotion)) {
            targetSmile = 0.68f * e;
            targetBrowLift = 0.15f * e;
            targetBrowPinch = 0.02f;
            targetCheekLift = 0.50f * e;
        } else if ("thinking".equals(emotion)) {
            targetSmile = 0.08f;
            targetBrowLift = 0.30f * e;
            targetBrowPinch = 0.10f * e;
            targetCheekLift = 0.07f;
        } else if ("focused".equals(emotion)) {
            targetSmile = 0.01f;
            targetBrowLift = -0.14f * e;
            targetBrowPinch = 0.31f * e;
            targetCheekLift = 0.02f;
        } else if ("sleep".equals(emotion)) {
            targetSmile = 0.02f;
            targetBrowLift = -0.04f;
            targetBrowPinch = 0f;
            targetCheekLift = 0f;
        } else {
            targetSmile = 0.16f * e;
            targetBrowLift = 0.05f * e;
            targetBrowPinch = 0.02f;
            targetCheekLift = 0.09f * e;
        }
    }

    public float getSmile() { return smile; }
    public float getBrowLift() { return browLift; }
    public float getBrowPinch() { return browPinch; }
    public float getCheekLift() { return cheekLift; }

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
