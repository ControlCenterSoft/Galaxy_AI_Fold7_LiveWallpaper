package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v40 Living Avatar Motion controller.
 *
 * Generates fully local body/head presence motion from AI scene state and optional
 * wallpaper touch coordinates. No sensor, camera, microphone or raw media input is
 * required. Touch positions are kept in memory only and are never persisted or sent
 * to the gateway.
 */
public final class LivingAvatarMotionV40 {
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
    private float foldProgress;
    private float previousFoldProgress;
    private float foldImpulse;
    private boolean mainDisplay;

    private float offsetX;
    private float offsetY;
    private float rollDegrees;
    private float yawSkew;
    private float pitchSkew;
    private float scale = 1f;

    public void update(SceneDecision decision, float deltaSeconds,
                       boolean main, float currentFoldProgress) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;
        mainDisplay = main;

        if (decision != null) {
            float k = Math.min(1f, dt * 3.2f);
            energy += (clamp(decision.energy, 0f, 1f) - energy) * k;
            curiosity += (clamp(decision.curiosity, 0f, 1f) - curiosity) * k;
            focus += (clamp(decision.focus, 0f, 1f) - focus) * k;
            serenity += (clamp(decision.serenity, 0f, 1f) - serenity) * k;
            presence += (clamp(decision.avatarPresence, 0f, 1f) - presence) * k;
            emotion = normalizeEmotion(decision.avatarState);
        }

        previousFoldProgress = foldProgress;
        foldProgress = clamp(currentFoldProgress, 0f, 1f);
        float foldVelocity = (foldProgress - previousFoldProgress) / Math.max(0.016f, dt);
        foldImpulse += clamp(foldVelocity * 0.012f, -0.045f, 0.045f);
        foldImpulse *= (float) Math.pow(0.22f, dt);

        touchWeight *= (float) Math.pow(0.11f, dt);
        touchPulse *= (float) Math.pow(0.055f, dt);

        float activity = activityForEmotion();
        float idleX = ((float) Math.sin(time * 0.31f)
                + (float) Math.sin(time * 0.77f + 1.1f) * 0.37f)
                * (0.42f + curiosity * 0.58f) * activity;
        float idleY = ((float) Math.sin(time * 0.23f + 0.6f)
                + (float) Math.sin(time * 0.51f + 2.0f) * 0.28f)
                * (0.50f + (1f - serenity) * 0.50f) * activity;

        float desiredX = idleX * (mainDisplay ? 0.012f : 0.008f);
        float desiredY = idleY * 0.0065f;
        desiredX += touchX * touchWeight * 0.022f;
        desiredY += touchY * touchWeight * 0.012f;

        if ("thinking".equals(emotion)) {
            desiredX += 0.0045f * (float) Math.sin(time * 0.47f + 0.4f);
            desiredY -= 0.0015f;
        } else if ("happy".equals(emotion)) {
            desiredY -= 0.0025f + 0.0015f * (float) Math.sin(time * 0.94f);
        } else if ("focused".equals(emotion)) {
            desiredX *= 0.58f;
            desiredY *= 0.58f;
        } else if ("sleep".equals(emotion)) {
            desiredX *= 0.22f;
            desiredY *= 0.28f;
        }

        float smooth = Math.min(1f, dt * ("sleep".equals(emotion) ? 1.2f : 2.6f));
        offsetX += (desiredX - offsetX) * smooth;
        offsetY += (desiredY - offsetY) * smooth;

        float desiredRoll = idleX * 0.72f * activity
                + touchX * touchWeight * 1.35f
                + foldImpulse * 17f;
        if ("thinking".equals(emotion)) desiredRoll += 0.55f;
        if ("focused".equals(emotion)) desiredRoll *= 0.45f;
        if ("sleep".equals(emotion)) desiredRoll *= 0.25f;
        rollDegrees += (desiredRoll - rollDegrees) * Math.min(1f, dt * 2.4f);
        rollDegrees = clamp(rollDegrees, -2.2f, 2.2f);

        float desiredYaw = (idleX * 0.0068f + touchX * touchWeight * 0.011f) * activity;
        float desiredPitch = (idleY * 0.0032f + touchY * touchWeight * 0.006f) * activity;
        yawSkew += (desiredYaw - yawSkew) * Math.min(1f, dt * 2.8f);
        pitchSkew += (desiredPitch - pitchSkew) * Math.min(1f, dt * 2.8f);
        yawSkew = clamp(yawSkew, -0.014f, 0.014f);
        pitchSkew = clamp(pitchSkew, -0.009f, 0.009f);

        float breath = (float) Math.sin(time * ("sleep".equals(emotion) ? 0.24f : 0.39f));
        float desiredScale = 1f
                + breath * (0.0030f + energy * 0.0025f) * activity
                + touchPulse * 0.010f
                + foldImpulse * 0.025f;
        if ("happy".equals(emotion)) {
            desiredScale += (float) Math.sin(time * 0.83f) * 0.0016f;
        }
        scale += (desiredScale - scale) * Math.min(1f, dt * 3.3f);
        scale = clamp(scale, 0.985f, 1.025f);
    }

    public void onTouch(float normalizedX, float normalizedY, boolean pressed) {
        touchX = clamp(normalizedX, -1f, 1f);
        touchY = clamp(normalizedY, -1f, 1f);
        if (pressed) {
            touchWeight = 1f;
            touchPulse = Math.min(1f, touchPulse + 0.72f);
        } else {
            touchWeight = Math.max(touchWeight, 0.52f);
        }
    }

    public void applyPortraitTransform(android.graphics.Canvas canvas, int width, int height) {
        float px = width * 0.5f;
        float py = height * (mainDisplay ? 0.46f : 0.45f);
        canvas.translate(width * offsetX, height * offsetY);
        canvas.translate(px, py);
        canvas.rotate(rollDegrees);
        canvas.skew(yawSkew, pitchSkew);
        canvas.scale(scale, scale);
        canvas.translate(-px, -py);
    }

    public float getPixelOffsetX(int width) {
        return width * offsetX;
    }

    public float getPixelOffsetY(int height) {
        return height * offsetY;
    }

    public float getScale() {
        return scale;
    }

    public float getMotionIntensity() {
        return clamp(Math.abs(offsetX) * 26f
                + Math.abs(offsetY) * 34f
                + Math.abs(rollDegrees) * 0.11f
                + touchWeight * 0.22f, 0f, 1f);
    }

    private float activityForEmotion() {
        if ("sleep".equals(emotion)) return 0.18f;
        if ("focused".equals(emotion)) return 0.56f;
        if ("thinking".equals(emotion)) return 1.04f;
        if ("happy".equals(emotion)) return 1.10f;
        return 0.82f + (1f - serenity) * 0.18f + presence * 0.06f;
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
