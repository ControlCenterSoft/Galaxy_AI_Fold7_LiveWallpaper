package pro.galaxyai.fold7.engine;

/**
 * v53 Natural Expression Inertia.
 *
 * Critically damped local filter for facial-expression coefficients. It prevents abrupt
 * reversals when AI state changes quickly, while still allowing happy/thinking reactions
 * to arrive faster than calm recovery. No sensors or media are consumed.
 */
public final class ExpressionInertiaV53 {
    private float smile;
    private float browLift;
    private float browPinch;
    private float cheekLift;
    private float smileVelocity;
    private float browLiftVelocity;
    private float browPinchVelocity;
    private float cheekVelocity;

    public void update(float targetSmile, float targetBrowLift, float targetBrowPinch,
                       float targetCheekLift, float serenity, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        float calm = clamp(serenity, 0f, 1f);
        float response = 5.0f - calm * 1.25f;
        float damping = 2.0f * (float) Math.sqrt(response);

        smileVelocity = springVelocity(smile, smileVelocity, targetSmile, response, damping, dt);
        smile += smileVelocity * dt;
        browLiftVelocity = springVelocity(browLift, browLiftVelocity, targetBrowLift, response * 1.08f, damping, dt);
        browLift += browLiftVelocity * dt;
        browPinchVelocity = springVelocity(browPinch, browPinchVelocity, targetBrowPinch, response * 1.12f, damping, dt);
        browPinch += browPinchVelocity * dt;
        cheekVelocity = springVelocity(cheekLift, cheekVelocity, targetCheekLift, response * 0.96f, damping, dt);
        cheekLift += cheekVelocity * dt;

        smile = clamp(smile, -0.22f, 0.82f);
        browLift = clamp(browLift, -0.30f, 0.48f);
        browPinch = clamp(browPinch, 0f, 0.48f);
        cheekLift = clamp(cheekLift, 0f, 0.58f);
    }

    private static float springVelocity(float value, float velocity, float target,
                                        float stiffness, float damping, float dt) {
        float acceleration = (target - value) * stiffness - velocity * damping;
        return velocity + acceleration * dt;
    }

    public float getSmile() { return smile; }
    public float getBrowLift() { return browLift; }
    public float getBrowPinch() { return browPinch; }
    public float getCheekLift() { return cheekLift; }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
