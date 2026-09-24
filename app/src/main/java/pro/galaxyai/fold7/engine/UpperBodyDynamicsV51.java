package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v51 Upper Body Dynamics.
 *
 * Generates bounded local body motion for the deformable portrait: chest breathing,
 * asymmetric shoulder settling and a small neck counter-motion. The controller consumes
 * only SceneDecision plus local TTS speaking state; it never accesses camera, microphone,
 * precise location, biometrics or raw media.
 */
public final class UpperBodyDynamicsV51 {
    private float time;
    private float energy = 0.45f;
    private float serenity = 0.72f;
    private float presence = 0.75f;
    private float breath;
    private float shoulderLiftLeft;
    private float shoulderLiftRight;
    private float shoulderSpread;
    private float neckCounterX;
    private float neckCounterY;
    private String emotion = "calm";

    public void update(SceneDecision decision, boolean speaking, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;

        if (decision != null) {
            float k = Math.min(1f, dt * 2.6f);
            energy += (clamp(decision.energy, 0f, 1f) - energy) * k;
            serenity += (clamp(decision.serenity, 0f, 1f) - serenity) * k;
            presence += (clamp(decision.avatarPresence, 0.35f, 1f) - presence) * k;
            emotion = normalizeEmotion(decision.avatarState);
        }

        float sleep = "sleep".equals(emotion) ? 0.22f : 1f;
        float focus = "focused".equals(emotion) ? 0.68f : 1f;
        float speechBoost = speaking ? 1.12f : 1f;
        float intensity = clamp((0.62f + energy * 0.24f + presence * 0.14f)
                * sleep * focus * speechBoost, 0.12f, 1.12f);

        float breathRate = "sleep".equals(emotion) ? 0.22f : 0.34f + energy * 0.06f;
        float targetBreath = (float) Math.sin(time * breathRate * 6.2831853f) * intensity;

        float settleA = (float) Math.sin(time * 0.17f + 0.4f);
        float settleB = (float) Math.sin(time * 0.23f + 2.1f);
        float asymmetry = (settleA * 0.64f + settleB * 0.36f)
                * (0.40f + (1f - serenity) * 0.35f) * intensity;

        float targetLeft = targetBreath * 0.78f + asymmetry * 0.34f;
        float targetRight = targetBreath * 0.78f - asymmetry * 0.34f;
        float targetSpread = targetBreath * 0.72f + Math.abs(asymmetry) * 0.12f;
        float targetNeckX = -asymmetry * 0.26f;
        float targetNeckY = -targetBreath * 0.18f;

        float smooth = 1f - (float) Math.exp(-2.7f * dt);
        breath += (targetBreath - breath) * smooth;
        shoulderLiftLeft += (targetLeft - shoulderLiftLeft) * smooth;
        shoulderLiftRight += (targetRight - shoulderLiftRight) * smooth;
        shoulderSpread += (targetSpread - shoulderSpread) * smooth;
        neckCounterX += (targetNeckX - neckCounterX) * smooth;
        neckCounterY += (targetNeckY - neckCounterY) * smooth;
    }

    public float getBreath() {
        return clamp(breath, -1f, 1f);
    }

    public float getShoulderLiftLeft() {
        return clamp(shoulderLiftLeft, -1f, 1f);
    }

    public float getShoulderLiftRight() {
        return clamp(shoulderLiftRight, -1f, 1f);
    }

    public float getShoulderSpread() {
        return clamp(shoulderSpread, -1f, 1f);
    }

    public float getNeckCounterX() {
        return clamp(neckCounterX, -1f, 1f);
    }

    public float getNeckCounterY() {
        return clamp(neckCounterY, -1f, 1f);
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
