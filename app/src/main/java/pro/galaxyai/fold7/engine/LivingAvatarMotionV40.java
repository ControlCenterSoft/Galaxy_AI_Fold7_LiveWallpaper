package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v44 Kinetic Presence motion controller, extended by v57 Fold Motion Continuity.
 *
 * Keeps the privacy-safe local motion model from v40 but increases visible, smooth
 * whole-portrait movement so the assistant reads as alive rather than as a static
 * wallpaper. v57 routes Fold progress through a damped continuity controller so portrait
 * translation, roll, skew and scale do not snap when Android switches surfaces.
 */
public final class LivingAvatarMotionV40 {
    private final FoldMotionContinuityV57 foldContinuity = new FoldMotionContinuityV57();
    private float time;
    private float energy = 0.45f;
    private float curiosity = 0.38f;
    private float focus = 0.42f;
    private float serenity = 0.72f;
    private float presence = 0.58f;
    private String emotion = "calm";

    private float touchX;
    private float touchY;
    private float touchWeight;
    private float touchPulse;
    private boolean mainDisplay;

    private float offsetX;
    private float offsetY;
    private float rollDegrees;
    private float yawSkew;
    private float pitchSkew;
    private float scale = 1f;
    private float kineticPhase;

    public void update(SceneDecision decision, float deltaSeconds,
                       boolean main, float currentFoldProgress) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;
        mainDisplay = main;

        if (decision != null) {
            float k = Math.min(1f, dt * 3.0f);
            energy += (clamp(decision.energy, 0f, 1f) - energy) * k;
            curiosity += (clamp(decision.curiosity, 0f, 1f) - curiosity) * k;
            focus += (clamp(decision.focus, 0f, 1f) - focus) * k;
            serenity += (clamp(decision.serenity, 0f, 1f) - serenity) * k;
            presence += (clamp(decision.avatarPresence, 0f, 1f) - presence) * k;
            emotion = normalizeEmotion(decision.avatarState);
        }

        foldContinuity.update(currentFoldProgress, dt);
        float foldBlend = foldContinuity.getProgress();
        float foldImpulse = foldContinuity.getImpulse();

        touchWeight *= (float) Math.pow(0.10f, dt);
        touchPulse *= (float) Math.pow(0.05f, dt);

        float activity = activityForEmotion();

        // Three slow harmonics avoid obvious looping. Their sum drives a visible but
        // still cinematic sway of the whole portrait.
        float idleX = ((float) Math.sin(time * 0.27f)
                + (float) Math.sin(time * 0.63f + 1.1f) * 0.42f
                + (float) Math.sin(time * 0.11f + 2.4f) * 0.24f)
                * (0.40f + curiosity * 0.60f) * activity;
        float idleY = ((float) Math.sin(time * 0.21f + 0.6f)
                + (float) Math.sin(time * 0.47f + 2.0f) * 0.31f
                + (float) Math.sin(time * 0.09f + 1.4f) * 0.18f)
                * (0.48f + (1f - serenity) * 0.52f) * activity;

        kineticPhase += dt * (0.18f + energy * 0.12f);
        float approach = (float) Math.sin(kineticPhase)
                * (0.35f + presence * 0.65f) * activity;

        // v57 blends cover/main motion amplitude continuously instead of switching it.
        float horizontalAmplitude = 0.011f + foldBlend * 0.005f;
        float desiredX = idleX * horizontalAmplitude;
        float desiredY = idleY * 0.0080f - approach * 0.0018f;
        desiredX += touchX * touchWeight * 0.028f;
        desiredY += touchY * touchWeight * 0.015f;
        desiredX += foldImpulse * 0.16f;
        desiredY -= Math.abs(foldImpulse) * 0.055f;

        if ("thinking".equals(emotion)) {
            desiredX += 0.0065f * (float) Math.sin(time * 0.43f + 0.4f);
            desiredY -= 0.0022f;
        } else if ("happy".equals(emotion)) {
            desiredY -= 0.0038f + 0.0020f * (float) Math.sin(time * 0.82f);
        } else if ("focused".equals(emotion)) {
            desiredX *= 0.50f;
            desiredY *= 0.50f;
        } else if ("sleep".equals(emotion)) {
            desiredX *= 0.18f;
            desiredY *= 0.22f;
        }

        float smooth = Math.min(1f, dt * ("sleep".equals(emotion) ? 1.0f : 2.25f));
        offsetX += (desiredX - offsetX) * smooth;
        offsetY += (desiredY - offsetY) * smooth;

        float desiredRoll = idleX * 1.05f * activity
                + touchX * touchWeight * 1.75f
                + foldImpulse * 18f;
        if ("thinking".equals(emotion)) desiredRoll += 0.85f;
        if ("happy".equals(emotion)) desiredRoll += (float) Math.sin(time * 0.72f) * 0.35f;
        if ("focused".equals(emotion)) desiredRoll *= 0.42f;
        if ("sleep".equals(emotion)) desiredRoll *= 0.20f;
        rollDegrees += (desiredRoll - rollDegrees) * Math.min(1f, dt * 2.1f);
        rollDegrees = clamp(rollDegrees, -3.2f, 3.2f);

        float desiredYaw = (idleX * 0.010f + touchX * touchWeight * 0.016f
                + foldImpulse * 0.045f) * activity;
        float desiredPitch = (idleY * 0.0048f + touchY * touchWeight * 0.008f
                - Math.abs(foldImpulse) * 0.018f) * activity;
        yawSkew += (desiredYaw - yawSkew) * Math.min(1f, dt * 2.35f);
        pitchSkew += (desiredPitch - pitchSkew) * Math.min(1f, dt * 2.35f);
        yawSkew = clamp(yawSkew, -0.020f, 0.020f);
        pitchSkew = clamp(pitchSkew, -0.013f, 0.013f);

        float breath = (float) Math.sin(time * ("sleep".equals(emotion) ? 0.22f : 0.36f));
        float desiredScale = 1f
                + breath * (0.0048f + energy * 0.0033f) * activity
                + approach * 0.0035f
                + touchPulse * 0.013f
                + foldImpulse * 0.026f;
        if ("happy".equals(emotion)) {
            desiredScale += (float) Math.sin(time * 0.76f) * 0.0024f;
        }
        scale += (desiredScale - scale) * Math.min(1f, dt * 2.8f);
        scale = clamp(scale, 0.978f, 1.034f);
    }

    public void onTouch(float normalizedX, float normalizedY, boolean pressed) {
        touchX = clamp(normalizedX, -1f, 1f);
        touchY = clamp(normalizedY, -1f, 1f);
        if (pressed) {
            touchWeight = 1f;
            touchPulse = Math.min(1f, touchPulse + 0.82f);
        } else {
            touchWeight = Math.max(touchWeight, 0.56f);
        }
    }

    public void applyPortraitTransform(android.graphics.Canvas canvas, int width, int height) {
        float foldBlend = foldContinuity.getProgress();
        float px = width * 0.5f;
        float py = height * (0.45f + foldBlend * 0.01f);
        canvas.translate(width * offsetX, height * offsetY);
        canvas.translate(px, py);
        canvas.rotate(rollDegrees);
        canvas.skew(yawSkew, pitchSkew);
        canvas.scale(scale, scale);
        canvas.translate(-px, -py);
    }

    public float getPixelOffsetX(int width) { return width * offsetX; }
    public float getPixelOffsetY(int height) { return height * offsetY; }
    public float getScale() { return scale; }
    public float getFoldBlend() { return foldContinuity.getProgress(); }

    public float getMotionIntensity() {
        return clamp(Math.abs(offsetX) * 30f
                + Math.abs(offsetY) * 38f
                + Math.abs(rollDegrees) * 0.13f
                + Math.abs(scale - 1f) * 16f
                + touchWeight * 0.22f
                + Math.abs(foldContinuity.getImpulse()) * 3.0f, 0f, 1f);
    }

    private float activityForEmotion() {
        if ("sleep".equals(emotion)) return 0.16f;
        if ("focused".equals(emotion)) return 0.52f;
        if ("thinking".equals(emotion)) return 1.08f;
        if ("happy".equals(emotion)) return 1.14f;
        return clamp(0.84f + (1f - serenity) * 0.16f + presence * 0.08f, 0.72f, 1.08f);
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
