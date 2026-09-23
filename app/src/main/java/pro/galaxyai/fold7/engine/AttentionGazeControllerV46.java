package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v46 Attentive Gaze controller, upgraded in v48 with Natural Fixation Gaze.
 *
 * v48 replaces the continuously wandering sinusoidal look with short fixation periods,
 * bounded saccades and critically damped settling. Touch temporarily becomes the fixation
 * target, then gaze returns to a local privacy-safe idle pattern. No camera, face tracking,
 * microphone, precise location or sensor stream is read.
 */
public final class AttentionGazeControllerV46 {
    private float time;
    private float targetX;
    private float targetY;
    private float gazeX;
    private float gazeY;
    private float gazeVelocityX;
    private float gazeVelocityY;
    private float fixationX;
    private float fixationY;
    private float fixationClock;
    private float fixationDuration = 1.8f;
    private int fixationIndex;
    private float touchX;
    private float touchY;
    private float touchWeight;
    private float focus = 0.42f;
    private float curiosity = 0.38f;
    private float serenity = 0.72f;
    private String emotion = "calm";

    public void update(SceneDecision decision, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;
        if (decision != null) {
            float k = Math.min(1f, dt * 3.0f);
            focus += (clamp(decision.focus, 0f, 1f) - focus) * k;
            curiosity += (clamp(decision.curiosity, 0f, 1f) - curiosity) * k;
            serenity += (clamp(decision.serenity, 0f, 1f) - serenity) * k;
            emotion = normalizeEmotion(decision.avatarState);
        }

        touchWeight *= (float) Math.pow(0.055f, dt);
        fixationClock += dt;
        if (fixationClock >= fixationDuration) {
            chooseFixation();
            fixationClock = 0f;
        }

        float microScale = "focused".equals(emotion) ? 0.18f
                : ("thinking".equals(emotion) ? 0.46f
                : ("sleep".equals(emotion) ? 0.03f : 0.30f));
        float microX = ((float) Math.sin(time * 2.13f + 0.4f)
                + (float) Math.sin(time * 3.71f + 1.8f) * 0.31f)
                * 0.028f * microScale;
        float microY = ((float) Math.sin(time * 1.87f + 1.1f)
                + (float) Math.sin(time * 3.19f + 2.2f) * 0.25f)
                * 0.020f * microScale;

        float touchGain = 0.92f * touchWeight;
        targetX = clamp(fixationX + microX + touchX * touchGain, -0.92f, 0.92f);
        targetY = clamp(fixationY + microY + touchY * touchGain * 0.72f, -0.78f, 0.78f);

        // v48 Natural Fixation: spring-damper motion gives a quick saccade followed by a
        // soft settle instead of a robotic linear interpolation.
        float spring = "sleep".equals(emotion) ? 6.0f
                : ("focused".equals(emotion) ? 24.0f : 31.0f);
        float damping = "sleep".equals(emotion) ? 5.4f : 8.8f;
        gazeVelocityX += (targetX - gazeX) * spring * dt;
        gazeVelocityY += (targetY - gazeY) * spring * dt;
        float decay = (float) Math.exp(-damping * dt);
        gazeVelocityX *= decay;
        gazeVelocityY *= decay;
        gazeX += gazeVelocityX * dt;
        gazeY += gazeVelocityY * dt;

        float boundX = "sleep".equals(emotion) ? 0.12f : 0.78f;
        float boundY = "sleep".equals(emotion) ? 0.08f : 0.58f;
        gazeX = clamp(gazeX, -boundX, boundX);
        gazeY = clamp(gazeY, -boundY, boundY);
    }

    private void chooseFixation() {
        fixationIndex++;
        float emotionGain = "focused".equals(emotion) ? 0.30f
                : ("thinking".equals(emotion) ? 0.86f
                : ("happy".equals(emotion) ? 0.66f
                : ("sleep".equals(emotion) ? 0.02f : 0.52f)));
        float curiosityGain = 0.62f + curiosity * 0.38f;
        float calmGain = 0.78f + (1f - serenity) * 0.22f;
        float phase = fixationIndex * 1.6180339f;

        fixationX = (float) Math.sin(phase * 1.73f + 0.31f)
                * 0.34f * emotionGain * curiosityGain;
        fixationY = (float) Math.sin(phase * 1.11f + 1.27f)
                * 0.19f * emotionGain * calmGain;

        // Occasionally look near the viewer/centre for a more attentive presence.
        if ((fixationIndex % 4) == 0 || focus > 0.78f) {
            fixationX *= 0.24f;
            fixationY *= 0.22f;
        }

        float base = "focused".equals(emotion) ? 2.35f
                : ("thinking".equals(emotion) ? 1.22f
                : ("sleep".equals(emotion) ? 4.8f : 1.75f));
        float variation = 0.45f + 0.55f * Math.abs((float) Math.sin(phase * 0.73f));
        fixationDuration = base + variation * ("sleep".equals(emotion) ? 1.4f : 0.95f);
    }

    public void onTouch(float normalizedX, float normalizedY, boolean pressed) {
        touchX = clamp(normalizedX, -1f, 1f);
        touchY = clamp(normalizedY, -1f, 1f);
        if (pressed) {
            touchWeight = 1f;
            // Lock the current fixation briefly so touch attention does not fight idle gaze.
            fixationClock = 0f;
            fixationDuration = Math.max(fixationDuration, 1.15f);
        } else {
            touchWeight = Math.max(touchWeight, 0.48f);
        }
    }

    public float getGazeX() {
        return clamp(gazeX, -1f, 1f);
    }

    public float getGazeY() {
        return clamp(gazeY, -1f, 1f);
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
