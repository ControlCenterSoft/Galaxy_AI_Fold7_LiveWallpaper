package pro.galaxyai.fold7.engine;

/**
 * v56 Natural Posture Inertia.
 *
 * Bounded spring-damper channels for whole-portrait idle posture. It removes phase-edge
 * velocity snaps when the contextual idle controller changes lean, lift, roll or scale.
 * This is entirely local math and consumes no sensors or media.
 */
public final class PostureInertiaV56 {
    private float x;
    private float y;
    private float roll;
    private float scale = 1f;
    private float vx;
    private float vy;
    private float vRoll;
    private float vScale;

    public void update(float targetX, float targetY, float targetRoll, float targetScale,
                       float intensity, boolean speaking, String emotion, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        float i = clamp(intensity, 0f, 1f);
        float tx = targetX * i;
        float ty = targetY * i;
        float tr = targetRoll * i;
        float ts = 1f + (targetScale - 1f) * i;

        float stiffness = "sleep".equals(emotion) ? 2.1f
                : (speaking ? 8.4f : ("thinking".equals(emotion) ? 6.8f : 4.9f));
        float damping = 2.0f * (float) Math.sqrt(stiffness) * 1.06f;

        vx = stepVelocity(x, vx, tx, stiffness, damping, dt);
        vy = stepVelocity(y, vy, ty, stiffness, damping, dt);
        vRoll = stepVelocity(roll, vRoll, tr, stiffness * 0.92f, damping, dt);
        vScale = stepVelocity(scale, vScale, ts, stiffness * 0.78f, damping, dt);

        x += vx * dt;
        y += vy * dt;
        roll += vRoll * dt;
        scale += vScale * dt;

        x = clamp(x, -0.018f, 0.018f);
        y = clamp(y, -0.014f, 0.014f);
        roll = clamp(roll, -0.012f, 0.012f);
        scale = clamp(scale, 0.992f, 1.010f);
    }

    private static float stepVelocity(float value, float velocity, float target,
                                      float stiffness, float damping, float dt) {
        return velocity + ((target - value) * stiffness - velocity * damping) * dt;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getRoll() { return roll; }
    public float getScale() { return scale; }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
