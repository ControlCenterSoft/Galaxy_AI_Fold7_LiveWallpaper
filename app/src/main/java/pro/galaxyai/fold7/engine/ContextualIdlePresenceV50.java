package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v50 Contextual Idle Presence, extended by v51 Upper Body Dynamics.
 *
 * Alternates between stillness, slight lean, nod and recovery while v51 adds chest
 * breathing, asymmetric shoulder settling and neck counter-motion. The combined controller
 * uses only local AI state and TTS speaking state. It does not access camera, microphone,
 * precise location, biometrics or raw media.
 */
public final class ContextualIdlePresenceV50 {
    private final UpperBodyDynamicsV51 upperBody = new UpperBodyDynamicsV51();
    private float time;
    private float phaseClock;
    private float phaseDuration = 2.4f;
    private int phaseIndex;
    private float targetX;
    private float targetY;
    private float targetRoll;
    private float targetScale = 1f;
    private float offsetX;
    private float offsetY;
    private float roll;
    private float scale = 1f;
    private float energy = 0.45f;
    private float serenity = 0.72f;
    private float intensity;
    private String emotion = "calm";

    public void update(SceneDecision decision, boolean speaking, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;
        phaseClock += dt;
        upperBody.update(decision, speaking, dt);

        if (decision != null) {
            float k = Math.min(1f, dt * 2.6f);
            energy += (clamp(decision.energy, 0f, 1f) - energy) * k;
            serenity += (clamp(decision.serenity, 0f, 1f) - serenity) * k;
            emotion = normalizeEmotion(decision.avatarState);
        }

        if (phaseClock >= phaseDuration) {
            phaseClock = 0f;
            choosePhase(speaking);
        }

        float localIntensity = "sleep".equals(emotion) ? 0.10f
                : ("focused".equals(emotion) ? 0.34f
                : ("thinking".equals(emotion) ? 0.78f
                : ("happy".equals(emotion) ? 0.92f : 0.58f)));
        localIntensity *= 0.82f + energy * 0.18f;
        if (speaking) localIntensity = Math.max(localIntensity, 0.72f);
        intensity += (localIntensity - intensity) * Math.min(1f, dt * 2.3f);

        float smooth = 1f - (float) Math.exp(-(speaking ? 3.1f : 1.85f) * dt);
        offsetX += (targetX * intensity - offsetX) * smooth;
        offsetY += (targetY * intensity - offsetY) * smooth;
        roll += (targetRoll * intensity - roll) * smooth;
        scale += (1f + (targetScale - 1f) * intensity - scale) * smooth;
    }

    private void choosePhase(boolean speaking) {
        phaseIndex++;
        float p = phaseIndex * 1.41421356f;

        if ("sleep".equals(emotion)) {
            targetX = 0f;
            targetY = 0.004f;
            targetRoll = 0f;
            targetScale = 0.998f;
            phaseDuration = 4.8f;
            return;
        }

        int mode = phaseIndex % 5;
        if (speaking || mode == 3) {
            targetX = (float) Math.sin(p * 0.7f) * 0.004f;
            targetY = 0.008f + (float) Math.sin(p * 1.3f) * 0.003f;
            targetRoll = (float) Math.sin(p) * 0.0035f;
            targetScale = 1.0025f;
            phaseDuration = speaking ? 1.15f : 1.55f;
        } else if (mode == 1 || mode == 4) {
            targetX = (float) Math.sin(p * 1.17f) * 0.010f;
            targetY = (float) Math.sin(p * 0.83f) * 0.004f;
            targetRoll = -targetX * 0.42f;
            targetScale = 1.001f;
            phaseDuration = 2.2f + Math.abs((float) Math.sin(p)) * 1.25f;
        } else {
            targetX = 0f;
            targetY = 0f;
            targetRoll = 0f;
            targetScale = 1f;
            float calm = 0.8f + serenity * 0.9f;
            phaseDuration = 2.0f + calm + Math.abs((float) Math.sin(p * 0.61f));
        }
    }

    public void applyTransform(Canvas canvas, int width, int height) {
        float px = offsetX * width;
        float py = offsetY * height;
        canvas.translate(px, py);
        canvas.rotate(roll * 57.29578f, width * 0.53f, height * 0.40f);
        canvas.scale(scale, scale, width * 0.53f, height * 0.43f);

        // v51: posture is pivoted around the chest so the shoulders and head do not move
        // as a rigid card. The amplitudes are deliberately tiny to preserve realism.
        float shoulderDelta = upperBody.getShoulderLiftRight() - upperBody.getShoulderLiftLeft();
        float chestScaleX = 1f + upperBody.getShoulderSpread() * 0.0038f;
        float chestScaleY = 1f + upperBody.getBreath() * 0.0028f;
        canvas.rotate(shoulderDelta * 0.42f, width * 0.53f, height * 0.72f);
        canvas.scale(chestScaleX, chestScaleY, width * 0.53f, height * 0.72f);
        canvas.translate(
                upperBody.getNeckCounterX() * width * 0.0018f,
                upperBody.getNeckCounterY() * height * 0.0012f
        );
    }

    public float getPixelOffsetX(int width) {
        return offsetX * width;
    }

    public float getPixelOffsetY(int height) {
        return offsetY * height;
    }

    public float getScale() {
        return clamp(scale, 0.992f, 1.010f);
    }

    public float getIntensity() {
        return clamp(intensity, 0f, 1f);
    }

    public UpperBodyDynamicsV51 getUpperBody() {
        return upperBody;
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
