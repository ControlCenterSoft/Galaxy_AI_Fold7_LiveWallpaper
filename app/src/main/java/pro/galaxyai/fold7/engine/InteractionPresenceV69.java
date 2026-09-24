package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v69 Touch Presence controller.
 *
 * Produces a short, bounded body response to explicit user interaction: a subtle lean toward
 * the touch point, tiny scale-in and soft roll that settle back with critical damping.
 * It consumes no sensors, media, microphone, camera or precise location.
 */
public final class InteractionPresenceV69 {
    private float pulse;
    private float pulseVelocity;
    private float leanX;
    private float leanY;
    private float leanXVelocity;
    private float leanYVelocity;
    private float targetLeanX;
    private float targetLeanY;
    private float speech;

    public void update(SceneDecision decision, boolean speaking, float speechEnergy, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        speech += (clamp(speechEnergy, 0f, 1f) - speech) * Math.min(1f, dt * 7.5f);

        float targetPulse = 0f;
        pulseVelocity += ((targetPulse - pulse) * 22f - pulseVelocity * 8.2f) * dt;
        pulse += pulseVelocity * dt;
        pulse = clamp(pulse, 0f, 1f);

        float relax = speaking ? 0.82f : 1f;
        leanXVelocity += ((targetLeanX * relax - leanX) * 18f - leanXVelocity * 8.5f) * dt;
        leanYVelocity += ((targetLeanY * relax - leanY) * 18f - leanYVelocity * 8.5f) * dt;
        leanX += leanXVelocity * dt;
        leanY += leanYVelocity * dt;

        float returnRate = speaking ? 0.80f : 1.45f;
        targetLeanX += (0f - targetLeanX) * Math.min(1f, dt * returnRate);
        targetLeanY += (0f - targetLeanY) * Math.min(1f, dt * returnRate);
    }

    public void triggerAttention(float normalizedX, float normalizedY) {
        targetLeanX = clamp(normalizedX, -1f, 1f) * 0.72f;
        targetLeanY = clamp(normalizedY, -1f, 1f) * 0.46f;
        pulse = Math.max(pulse, 0.72f);
        pulseVelocity = Math.max(pulseVelocity, 1.3f);
    }

    public float getTranslateX() {
        return leanX * 0.014f;
    }

    public float getTranslateY() {
        return leanY * 0.008f - speech * 0.0025f;
    }

    public float getScale() {
        return 1f + pulse * 0.013f + speech * 0.0035f;
    }

    public float getRollDegrees() {
        return leanX * 1.9f;
    }

    public float getPresenceEnergy() {
        return clamp(pulse * 0.55f + speech * 0.45f, 0f, 1f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
