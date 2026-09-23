package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v42 autonomous gesture sequencer.
 *
 * Adds sparse, human-like nods, leans and acknowledgement motions on top of the
 * continuous v40 motion field. All scheduling is local and driven only by the
 * already-approved SceneDecision state and local TTS speaking state. No camera,
 * microphone, sensors, raw media, identifiers or precise location are used.
 */
public final class AutonomousGestureV42 {
    private static final int NONE = 0;
    private static final int NOD = 1;
    private static final int LEAN_LEFT = 2;
    private static final int LEAN_RIGHT = 3;
    private static final int GLANCE = 4;
    private static final int ACKNOWLEDGE = 5;

    private long rng = System.nanoTime() ^ 0x5DEECE66DL;
    private float time;
    private float nextGestureIn = 4.8f;
    private float gestureElapsed;
    private float gestureDuration = 1f;
    private int gesture = NONE;
    private boolean wasSpeaking;
    private String emotion = "calm";
    private float energy = 0.45f;
    private float curiosity = 0.38f;
    private float serenity = 0.72f;

    private float x;
    private float y;
    private float roll;
    private float scale = 1f;
    private float intensity;

    public void update(SceneDecision decision, boolean speaking, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;

        if (decision != null) {
            float k = Math.min(1f, dt * 2.8f);
            energy += (clamp(decision.energy, 0f, 1f) - energy) * k;
            curiosity += (clamp(decision.curiosity, 0f, 1f) - curiosity) * k;
            serenity += (clamp(decision.serenity, 0f, 1f) - serenity) * k;
            emotion = normalizeEmotion(decision.avatarState);
        }

        if (speaking && !wasSpeaking && !"sleep".equals(emotion)) {
            beginGesture(ACKNOWLEDGE, 0.78f);
        }
        wasSpeaking = speaking;

        if (gesture == NONE) {
            nextGestureIn -= dt;
            if (nextGestureIn <= 0f) {
                beginGesture(chooseGesture(), chooseDuration());
            }
        } else {
            gestureElapsed += dt;
            if (gestureElapsed >= gestureDuration) {
                gesture = NONE;
                gestureElapsed = 0f;
                scheduleNext();
            }
        }

        computePose();
    }

    public void applyGestureTransform(Canvas canvas, int width, int height) {
        float px = width * 0.5f;
        float py = height * 0.47f;
        canvas.translate(width * x, height * y);
        canvas.translate(px, py);
        canvas.rotate(roll);
        canvas.scale(scale, scale);
        canvas.translate(-px, -py);
    }

    public float getPixelOffsetX(int width) {
        return width * x;
    }

    public float getPixelOffsetY(int height) {
        return height * y;
    }

    public float getScale() {
        return scale;
    }

    public float getIntensity() {
        return intensity;
    }

    public boolean isGestureActive() {
        return gesture != NONE;
    }

    private void computePose() {
        if (gesture == NONE) {
            x += (0f - x) * 0.18f;
            y += (0f - y) * 0.18f;
            roll += (0f - roll) * 0.18f;
            scale += (1f - scale) * 0.18f;
            intensity += (0f - intensity) * 0.20f;
            return;
        }

        float p = clamp(gestureElapsed / Math.max(0.20f, gestureDuration), 0f, 1f);
        float envelope = (float) Math.sin(Math.PI * p);
        float secondary = (float) Math.sin(Math.PI * 2f * p);
        float activity = "sleep".equals(emotion) ? 0.18f
                : ("focused".equals(emotion) ? 0.66f : 1f);

        float targetX = 0f;
        float targetY = 0f;
        float targetRoll = 0f;
        float targetScale = 1f;

        if (gesture == NOD) {
            targetY = envelope * 0.0065f * activity;
            targetScale = 1f - envelope * 0.0025f;
        } else if (gesture == LEAN_LEFT) {
            targetX = -envelope * 0.0095f * activity;
            targetRoll = -envelope * 1.05f * activity;
        } else if (gesture == LEAN_RIGHT) {
            targetX = envelope * 0.0095f * activity;
            targetRoll = envelope * 1.05f * activity;
        } else if (gesture == GLANCE) {
            float direction = ((rng >>> 8) & 1L) == 0L ? -1f : 1f;
            targetX = direction * envelope * 0.0068f * (0.55f + curiosity * 0.45f) * activity;
            targetRoll = direction * secondary * 0.38f * activity;
        } else if (gesture == ACKNOWLEDGE) {
            targetY = envelope * 0.0048f;
            targetScale = 1f + envelope * 0.0038f;
            targetRoll = secondary * 0.18f;
        }

        float smooth = 0.34f;
        x += (targetX - x) * smooth;
        y += (targetY - y) * smooth;
        roll += (targetRoll - roll) * smooth;
        scale += (targetScale - scale) * smooth;
        intensity += (envelope - intensity) * 0.32f;
    }

    private int chooseGesture() {
        int r = nextInt(100);
        if ("sleep".equals(emotion)) return r < 82 ? NONE : NOD;
        if ("focused".equals(emotion)) return r < 58 ? NOD : (r < 80 ? GLANCE : LEAN_LEFT);
        if ("thinking".equals(emotion)) return r < 36 ? GLANCE : (r < 68 ? LEAN_RIGHT : NOD);
        if ("happy".equals(emotion)) return r < 45 ? NOD : (r < 72 ? LEAN_LEFT : LEAN_RIGHT);
        if (r < 34) return NOD;
        if (r < 56) return GLANCE;
        if (r < 78) return LEAN_LEFT;
        return LEAN_RIGHT;
    }

    private float chooseDuration() {
        float base = "focused".equals(emotion) ? 1.25f
                : ("thinking".equals(emotion) ? 1.55f
                : ("happy".equals(emotion) ? 1.05f : 1.35f));
        return base + nextFloat() * 0.55f;
    }

    private void beginGesture(int next, float duration) {
        if (next == NONE) {
            scheduleNext();
            return;
        }
        gesture = next;
        gestureElapsed = 0f;
        gestureDuration = Math.max(0.45f, duration);
    }

    private void scheduleNext() {
        float min;
        float range;
        if ("sleep".equals(emotion)) {
            min = 14f; range = 10f;
        } else if ("focused".equals(emotion)) {
            min = 7.5f; range = 7f;
        } else if ("thinking".equals(emotion)) {
            min = 4.8f; range = 5.5f;
        } else if ("happy".equals(emotion)) {
            min = 3.8f; range = 4.5f;
        } else {
            min = 5.5f; range = 6.5f;
        }
        float liveliness = 0.72f + energy * 0.28f + (1f - serenity) * 0.12f;
        nextGestureIn = (min + nextFloat() * range) / Math.max(0.65f, liveliness);
    }

    private int nextInt(int bound) {
        return (int) (nextFloat() * Math.max(1, bound));
    }

    private float nextFloat() {
        rng = rng * 6364136223846793005L + 1442695040888963407L;
        long bits = (rng >>> 40) & 0xFFFFFFL;
        return bits / 16777216f;
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
