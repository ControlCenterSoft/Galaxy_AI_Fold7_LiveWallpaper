package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v46 Attentive Gaze controller, upgraded by v48 Natural Fixation Gaze and v54 Unified Gaze Intent.
 *
 * v54 moves fixation planning into a deterministic shared contract. Every controller instance
 * receives the same bounded fixation sequence from the same AI state, so eye-mesh motion and
 * head/eye coordination remain behaviourally aligned. Touch temporarily becomes the fixation
 * target, then gaze returns to the local privacy-safe plan. No camera, face tracking,
 * microphone, precise location or sensor stream is read.
 */
public final class AttentionGazeControllerV46 {
    private final UnifiedGazeIntentV54 gazeIntent = new UnifiedGazeIntentV54();
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
        UnifiedGazeIntentV54.Target next = gazeIntent.next(
                fixationIndex, emotion, focus, curiosity, serenity);
        fixationX = next.x;
        fixationY = next.y;
        fixationDuration = next.durationSeconds;
    }

    public void onTouch(float normalizedX, float normalizedY, boolean pressed) {
        touchX = clamp(normalizedX, -1f, 1f);
        touchY = clamp(normalizedY, -1f, 1f);
        if (pressed) {
            touchWeight = 1f;
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

    public float getGazeSpeed() {
        return clamp((float) Math.sqrt(gazeVelocityX * gazeVelocityX + gazeVelocityY * gazeVelocityY), 0f, 2f);
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
