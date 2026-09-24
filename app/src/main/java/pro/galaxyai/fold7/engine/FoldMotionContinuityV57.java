package pro.galaxyai.fold7.engine;

/**
 * v57 Fold Motion Continuity.
 *
 * Smooths the Fold transition signal before it reaches portrait translation/rotation/scale.
 * A critically damped progress channel and bounded velocity impulse avoid visible pose snaps
 * when Android switches between cover and main surfaces. No sensors or media are consumed.
 */
public final class FoldMotionContinuityV57 {
    private float progress;
    private float progressVelocity;
    private float impulse;

    public void update(float targetProgress, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        float target = clamp(targetProgress, 0f, 1f);

        float stiffness = 14.0f;
        float damping = 7.6f;
        float acceleration = (target - progress) * stiffness - progressVelocity * damping;
        progressVelocity += acceleration * dt;
        progress += progressVelocity * dt;
        progress = clamp(progress, 0f, 1f);

        float targetImpulse = clamp(progressVelocity * 0.028f, -0.060f, 0.060f);
        float blend = 1f - (float) Math.exp(-5.2f * dt);
        impulse += (targetImpulse - impulse) * blend;
        if (Math.abs(target - progress) < 0.002f && Math.abs(progressVelocity) < 0.01f) {
            impulse *= (float) Math.exp(-4.0f * dt);
        }
    }

    public float getProgress() { return progress; }
    public float getImpulse() { return clamp(impulse, -0.060f, 0.060f); }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
