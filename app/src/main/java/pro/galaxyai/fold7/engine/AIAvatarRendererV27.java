package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v27 living AI face renderer.
 *
 * The Gateway controls high-level emotional state only. Geometry and animation stay local so the
 * wallpaper remains smooth and fully functional when AIDI is unavailable.
 */
public final class AIAvatarRendererV27 {
    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint face = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint contour = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyeWhite = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iris = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pupil = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detail = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lip = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blush = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float time;
    private float blink = 1f;
    private float blinkClock;
    private float smile;
    private float browLift;
    private float eyeSoftness;
    private float targetSmile;
    private float targetBrowLift;
    private float targetEyeSoftness;
    private float targetGlow = 0.55f;
    private float glow = 0.55f;
    private float presence = 0.6f;
    private float targetPresence = 0.6f;
    private float gazeX;
    private float gazeY;
    private boolean mainDisplay;
    private String emotion = "calm";

    public AIAvatarRendererV27() {
        contour.setStyle(Paint.Style.STROKE);
        contour.setStrokeCap(Paint.Cap.ROUND);
        contour.setStrokeJoin(Paint.Join.ROUND);
        eyeWhite.setStyle(Paint.Style.FILL);
        iris.setStyle(Paint.Style.FILL);
        pupil.setStyle(Paint.Style.FILL);
        detail.setStyle(Paint.Style.STROKE);
        detail.setStrokeCap(Paint.Cap.ROUND);
        detail.setStrokeJoin(Paint.Join.ROUND);
        lip.setStyle(Paint.Style.STROKE);
        lip.setStrokeCap(Paint.Cap.ROUND);
        blush.setStyle(Paint.Style.FILL);
    }

    public void update(SceneDecision decision, float deltaSeconds, boolean main) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;
        mainDisplay = main;

        String state = decision == null ? "calm" : normalizeEmotion(decision.avatarState);
        emotion = state;
        targetPresence = decision == null ? 0.60f : clamp(decision.avatarPresence, 0.25f, 1f);
        configureEmotion(state);

        float k = Math.min(1f, dt * 3.8f);
        smile += (targetSmile - smile) * k;
        browLift += (targetBrowLift - browLift) * k;
        eyeSoftness += (targetEyeSoftness - eyeSoftness) * k;
        glow += (targetGlow - glow) * k;
        presence += (targetPresence - presence) * k;

        // Calm deterministic gaze movement: no RNG jumps and no dependence on network timing.
        gazeX = (float) Math.sin(time * 0.31f) * 0.45f;
        gazeY = (float) Math.sin(time * 0.19f + 1.3f) * 0.24f;

        blinkClock += dt;
        float interval = "focused".equals(state) ? 4.7f : ("sleep".equals(state) ? 2.0f : 3.6f);
        if (blinkClock >= interval) {
            float p = (blinkClock - interval) / 0.34f;
            if (p < 0.5f) blink = 1f - p * 2f;
            else if (p < 1f) blink = (p - 0.5f) * 2f;
            else {
                blink = 1f;
                blinkClock = 0f;
            }
        } else {
            blink = "sleep".equals(state) ? 0.12f : 1f;
        }
    }

    public void draw(Canvas canvas, float cx, float cy, float size) {
        float breathe = 1f + (float) Math.sin(time * 1.05f) * 0.010f;
        float headTurn = (float) Math.sin(time * 0.23f) * 0.022f;
        float headTilt = (float) Math.sin(time * 0.17f + 0.8f) * 0.018f;
        float alpha = 0.68f + presence * 0.30f;

        canvas.save();
        canvas.translate(cx + headTurn * size, cy + headTilt * size);
        canvas.scale(breathe, breathe);

        drawHalo(canvas, size, alpha);
        drawFace(canvas, size, alpha);
        drawEyes(canvas, size, alpha);
        drawBrows(canvas, size, alpha);
        drawNose(canvas, size, alpha);
        drawMouth(canvas, size, alpha);
        drawAccent(canvas, size, alpha);

        canvas.restore();
    }

    public String getEmotion() {
        return emotion;
    }

    private void drawHalo(Canvas canvas, float s, float alpha) {
        float radius = s * (0.68f + glow * 0.10f);
        halo.setShader(new RadialGradient(
                0f, -s * 0.03f, radius,
                new int[]{
                        Color.argb((int) (52 * alpha), 120, 205, 255),
                        Color.argb((int) (24 * alpha), 105, 90, 255),
                        Color.TRANSPARENT
                },
                new float[]{0f, 0.52f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(0f, -s * 0.03f, radius, halo);
        halo.setShader(null);
    }

    private void drawFace(Canvas canvas, float s, float alpha) {
        float w = s * (mainDisplay ? 0.39f : 0.40f);
        float h = s * 0.53f;

        Path head = new Path();
        head.moveTo(0f, -h);
        head.cubicTo(w * 0.80f, -h * 0.94f, w, -h * 0.36f, w * 0.88f, h * 0.18f);
        head.cubicTo(w * 0.80f, h * 0.58f, w * 0.46f, h * 0.92f, 0f, h);
        head.cubicTo(-w * 0.46f, h * 0.92f, -w * 0.80f, h * 0.58f, -w * 0.88f, h * 0.18f);
        head.cubicTo(-w, -h * 0.36f, -w * 0.80f, -h * 0.94f, 0f, -h);
        head.close();

        face.setShader(new LinearGradient(
                -w, -h, w, h,
                new int[]{
                        Color.argb((int) (120 * alpha), 198, 228, 255),
                        Color.argb((int) (78 * alpha), 125, 164, 244),
                        Color.argb((int) (52 * alpha), 76, 74, 170)
                }, null, Shader.TileMode.CLAMP));
        canvas.drawPath(head, face);
        face.setShader(null);

        contour.setStrokeWidth(Math.max(1.8f, s * 0.006f));
        contour.setColor(Color.argb((int) (185 * alpha), 182, 227, 255));
        canvas.drawPath(head, contour);

        // Cheek highlights make the portrait read as a face rather than a wireframe mask.
        blush.setShader(new RadialGradient(-w * 0.48f, h * 0.22f, s * 0.13f,
                Color.argb((int) (36 * alpha), 255, 154, 211), Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawCircle(-w * 0.48f, h * 0.22f, s * 0.13f, blush);
        blush.setShader(new RadialGradient(w * 0.48f, h * 0.22f, s * 0.13f,
                Color.argb((int) (36 * alpha), 255, 154, 211), Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawCircle(w * 0.48f, h * 0.22f, s * 0.13f, blush);
        blush.setShader(null);
    }

    private void drawEyes(Canvas canvas, float s, float alpha) {
        float y = -s * 0.105f;
        float dx = s * 0.145f;
        float ew = s * 0.092f;
        float eh = s * 0.038f * Math.max(0.06f, blink * (1f - eyeSoftness * 0.22f));

        eyeWhite.setColor(Color.argb((int) (218 * alpha), 225, 244, 255));
        iris.setColor(Color.argb((int) (245 * alpha), 91, 211, 255));
        pupil.setColor(Color.argb((int) (248 * alpha), 8, 28, 52));

        drawEye(canvas, -dx, y, ew, eh, s, alpha);
        drawEye(canvas, dx, y, ew, eh, s, alpha);
    }

    private void drawEye(Canvas canvas, float x, float y, float ew, float eh, float s, float alpha) {
        RectF eye = new RectF(x - ew, y - eh, x + ew, y + eh);
        canvas.drawOval(eye, eyeWhite);
        if (eh <= s * 0.006f) return;

        float gx = gazeX * ew * 0.24f;
        float gy = gazeY * eh * 0.28f;
        float irisR = Math.min(ew * 0.33f, Math.max(s * 0.010f, eh * 0.82f));
        canvas.drawCircle(x + gx, y + gy, irisR, iris);
        canvas.drawCircle(x + gx, y + gy, irisR * 0.46f, pupil);

        detail.setStrokeWidth(Math.max(1.2f, s * 0.003f));
        detail.setColor(Color.argb((int) (170 * alpha), 168, 226, 255));
        canvas.drawArc(eye, 188f, 164f, false, detail);
    }

    private void drawBrows(Canvas canvas, float s, float alpha) {
        float y = -s * 0.205f - browLift * s * 0.055f;
        float dx = s * 0.145f;
        detail.setStrokeWidth(Math.max(2f, s * 0.010f));
        detail.setColor(Color.argb((int) (180 * alpha), 142, 196, 255));
        canvas.drawLine(-dx - s * 0.074f, y + s * 0.010f, -dx + s * 0.070f, y - s * 0.006f, detail);
        canvas.drawLine(dx - s * 0.070f, y - s * 0.006f, dx + s * 0.074f, y + s * 0.010f, detail);
    }

    private void drawNose(Canvas canvas, float s, float alpha) {
        detail.setStrokeWidth(Math.max(1.3f, s * 0.004f));
        detail.setColor(Color.argb((int) (112 * alpha), 193, 225, 255));
        Path nose = new Path();
        nose.moveTo(s * 0.010f, -s * 0.080f);
        nose.cubicTo(s * 0.026f, s * 0.005f, -s * 0.020f, s * 0.074f, -s * 0.035f, s * 0.105f);
        nose.cubicTo(-s * 0.010f, s * 0.126f, s * 0.025f, s * 0.126f, s * 0.048f, s * 0.106f);
        canvas.drawPath(nose, detail);
    }

    private void drawMouth(Canvas canvas, float s, float alpha) {
        float y = s * 0.205f;
        float half = s * 0.105f;
        float curve = s * (0.018f + smile * 0.060f);
        lip.setStrokeWidth(Math.max(2f, s * 0.009f));
        lip.setColor(Color.argb((int) (205 * alpha), 255, 151, 215));

        Path mouth = new Path();
        mouth.moveTo(-half, y);
        mouth.cubicTo(-half * 0.42f, y + curve, half * 0.42f, y + curve, half, y);
        canvas.drawPath(mouth, lip);

        lip.setStrokeWidth(Math.max(1.2f, s * 0.004f));
        lip.setColor(Color.argb((int) (112 * alpha), 210, 226, 255));
        Path lower = new Path();
        lower.moveTo(-half * 0.72f, y + s * 0.018f);
        lower.cubicTo(-half * 0.30f, y + s * 0.055f, half * 0.30f, y + s * 0.055f,
                half * 0.72f, y + s * 0.018f);
        canvas.drawPath(lower, lip);
    }

    private void drawAccent(Canvas canvas, float s, float alpha) {
        // A subtle living-universe glyph keeps the portrait recognisably AI without hiding the face.
        float y = -s * 0.335f;
        detail.setStrokeWidth(Math.max(1.2f, s * 0.004f));
        detail.setColor(Color.argb((int) ((90 + glow * 90) * alpha), 128, 225, 255));
        canvas.drawCircle(0f, y, s * 0.021f, detail);
        canvas.drawLine(-s * 0.050f, y, -s * 0.027f, y, detail);
        canvas.drawLine(s * 0.027f, y, s * 0.050f, y, detail);
    }

    private void configureEmotion(String state) {
        if ("focused".equals(state)) {
            targetSmile = 0.05f;
            targetBrowLift = -0.16f;
            targetEyeSoftness = 0.02f;
            targetGlow = 0.72f;
        } else if ("thinking".equals(state)) {
            targetSmile = 0.10f;
            targetBrowLift = 0.28f;
            targetEyeSoftness = 0.08f;
            targetGlow = 0.66f;
        } else if ("happy".equals(state)) {
            targetSmile = 0.78f;
            targetBrowLift = 0.20f;
            targetEyeSoftness = 0.24f;
            targetGlow = 0.88f;
        } else if ("sleep".equals(state)) {
            targetSmile = 0.04f;
            targetBrowLift = -0.04f;
            targetEyeSoftness = 0.90f;
            targetGlow = 0.30f;
        } else {
            targetSmile = 0.20f;
            targetBrowLift = 0.06f;
            targetEyeSoftness = 0.12f;
            targetGlow = 0.58f;
        }
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
