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

/** v28 style-aware living avatar renderer. Emotion and style are orthogonal. */
public final class AIAvatarRendererV28 {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyeWhite = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iris = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pupil = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lip = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private AvatarStyleV28 style = AvatarStyleV28.SCI_FI;
    private float time;
    private float blink = 1f;
    private float blinkClock;
    private float smile = 0.15f;
    private float browLift;
    private float targetSmile = 0.15f;
    private float targetBrow;
    private float glow = 0.6f;
    private float targetGlow = 0.6f;
    private float presence = 0.6f;
    private float targetPresence = 0.6f;
    private boolean mainDisplay;

    public AIAvatarRendererV28() {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        lip.setStyle(Paint.Style.STROKE);
        lip.setStrokeCap(Paint.Cap.ROUND);
    }

    public void update(SceneDecision decision, AvatarStyleV28 newStyle,
                       float deltaSeconds, boolean main) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;
        mainDisplay = main;
        if (newStyle != null) style = newStyle;

        String emotion = normalizeEmotion(decision == null ? "calm" : decision.avatarState);
        targetPresence = decision == null ? 0.60f : clamp(decision.avatarPresence, 0.25f, 1f);
        configureEmotion(emotion);

        float k = Math.min(1f, dt * 3.8f);
        smile += (targetSmile - smile) * k;
        browLift += (targetBrow - browLift) * k;
        glow += (targetGlow - glow) * k;
        presence += (targetPresence - presence) * k;

        blinkClock += dt;
        float interval = "focused".equals(emotion) ? 4.6f : ("sleep".equals(emotion) ? 2.0f : 3.6f);
        if (blinkClock >= interval) {
            float p = (blinkClock - interval) / 0.32f;
            blink = p < 0.5f ? 1f - p * 2f : p < 1f ? (p - 0.5f) * 2f : 1f;
            if (p >= 1f) blinkClock = 0f;
        } else {
            blink = "sleep".equals(emotion) ? 0.12f : 1f;
        }
    }

    public void draw(Canvas canvas, float cx, float cy, float size) {
        float breathe = 1f + (float) Math.sin(time * 1.05f) * 0.010f;
        float driftX = (float) Math.sin(time * 0.23f) * size * 0.020f;
        float driftY = (float) Math.sin(time * 0.17f + 0.8f) * size * 0.016f;
        float alpha = 0.66f + presence * 0.32f;

        canvas.save();
        canvas.translate(cx + driftX, cy + driftY);
        canvas.scale(breathe, breathe);
        drawHalo(canvas, size, alpha);
        drawHead(canvas, size, alpha);
        drawEyes(canvas, size, alpha);
        drawBrows(canvas, size, alpha);
        drawNose(canvas, size, alpha);
        drawMouth(canvas, size, alpha);
        drawStyleAccent(canvas, size, alpha);
        canvas.restore();
    }

    private void drawHalo(Canvas canvas, float s, float alpha) {
        int accent = style.irisColor;
        glowPaint.setShader(new RadialGradient(0f, -s * 0.03f, s * 0.76f,
                withAlpha(accent, (int) ((44 + glow * 45) * alpha)),
                Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(0f, -s * 0.03f, s * 0.76f, glowPaint);
        glowPaint.setShader(null);
    }

    private void drawHead(Canvas canvas, float s, float alpha) {
        float w = s * (mainDisplay ? 0.39f : 0.40f);
        float h = s * 0.53f;
        Path head = new Path();
        head.moveTo(0f, -h);
        head.cubicTo(w * 0.80f, -h * 0.94f, w, -h * 0.36f, w * 0.88f, h * 0.18f);
        head.cubicTo(w * 0.80f, h * 0.58f, w * 0.46f, h * 0.92f, 0f, h);
        head.cubicTo(-w * 0.46f, h * 0.92f, -w * 0.80f, h * 0.58f, -w * 0.88f, h * 0.18f);
        head.cubicTo(-w, -h * 0.36f, -w * 0.80f, -h * 0.94f, 0f, -h);
        head.close();

        int mid = blend(style.faceLight, style.faceShade, style == AvatarStyleV28.HUMAN ? 0.36f : 0.48f);
        fill.setShader(new LinearGradient(-w, -h, w, h,
                new int[]{withAlpha(style.faceLight, (int) (154 * alpha)),
                        withAlpha(mid, (int) (112 * alpha)),
                        withAlpha(style.faceShade, (int) (76 * alpha))},
                null, Shader.TileMode.CLAMP));
        canvas.drawPath(head, fill);
        fill.setShader(null);

        stroke.setStrokeWidth(Math.max(1.7f, s * (style == AvatarStyleV28.HUMAN ? 0.004f : 0.006f)));
        stroke.setColor(withAlpha(style.irisColor, (int) ((125 + style.hologramStrength * 85) * alpha)));
        canvas.drawPath(head, stroke);

        if (style == AvatarStyleV28.HUMAN) {
            fill.setColor(Color.argb((int) (24 * alpha), 255, 145, 182));
            canvas.drawCircle(-w * 0.48f, h * 0.22f, s * 0.09f, fill);
            canvas.drawCircle(w * 0.48f, h * 0.22f, s * 0.09f, fill);
        }
    }

    private void drawEyes(Canvas canvas, float s, float alpha) {
        float y = -s * 0.105f;
        float dx = s * 0.145f;
        float ew = s * 0.092f;
        float eh = s * 0.038f * Math.max(0.06f, blink);
        float gazeX = (float) Math.sin(time * 0.31f) * ew * 0.11f;
        float gazeY = (float) Math.sin(time * 0.19f + 1.3f) * eh * 0.08f;

        eyeWhite.setColor(style == AvatarStyleV28.HUMAN
                ? Color.argb((int) (228 * alpha), 247, 247, 250)
                : Color.argb((int) (220 * alpha), 225, 244, 255));
        iris.setColor(withAlpha(style.irisColor, (int) (245 * alpha)));
        pupil.setColor(Color.argb((int) (248 * alpha), 8, 20, 42));

        drawEye(canvas, -dx, y, ew, eh, gazeX, gazeY, s);
        drawEye(canvas, dx, y, ew, eh, gazeX, gazeY, s);
    }

    private void drawEye(Canvas canvas, float x, float y, float ew, float eh,
                         float gazeX, float gazeY, float s) {
        RectF eye = new RectF(x - ew, y - eh, x + ew, y + eh);
        canvas.drawOval(eye, eyeWhite);
        if (eh <= s * 0.006f) return;
        float irisR = Math.min(ew * 0.33f, Math.max(s * 0.010f, eh * 0.82f));
        canvas.drawCircle(x + gazeX, y + gazeY, irisR, iris);
        canvas.drawCircle(x + gazeX, y + gazeY, irisR * 0.45f, pupil);
    }

    private void drawBrows(Canvas canvas, float s, float alpha) {
        float y = -s * 0.205f - browLift * s * 0.055f;
        float dx = s * 0.145f;
        stroke.setStrokeWidth(Math.max(2f, s * 0.009f));
        stroke.setColor(withAlpha(style.faceShade, (int) (190 * alpha)));
        canvas.drawLine(-dx - s * 0.074f, y + s * 0.010f, -dx + s * 0.070f, y - s * 0.006f, stroke);
        canvas.drawLine(dx - s * 0.070f, y - s * 0.006f, dx + s * 0.074f, y + s * 0.010f, stroke);
    }

    private void drawNose(Canvas canvas, float s, float alpha) {
        stroke.setStrokeWidth(Math.max(1.2f, s * 0.0038f));
        stroke.setColor(withAlpha(blend(style.faceLight, style.irisColor, 0.32f), (int) (112 * alpha)));
        Path nose = new Path();
        nose.moveTo(s * 0.010f, -s * 0.080f);
        nose.cubicTo(s * 0.026f, s * 0.005f, -s * 0.020f, s * 0.074f, -s * 0.035f, s * 0.105f);
        nose.cubicTo(-s * 0.010f, s * 0.126f, s * 0.025f, s * 0.126f, s * 0.048f, s * 0.106f);
        canvas.drawPath(nose, stroke);
    }

    private void drawMouth(Canvas canvas, float s, float alpha) {
        float y = s * 0.205f;
        float half = s * 0.105f;
        float curve = s * (0.018f + smile * 0.060f);
        lip.setStrokeWidth(Math.max(2f, s * (style == AvatarStyleV28.HUMAN ? 0.008f : 0.009f)));
        lip.setColor(withAlpha(style.lipColor, (int) (210 * alpha)));
        Path mouth = new Path();
        mouth.moveTo(-half, y);
        mouth.cubicTo(-half * 0.42f, y + curve, half * 0.42f, y + curve, half, y);
        canvas.drawPath(mouth, lip);
    }

    private void drawStyleAccent(Canvas canvas, float s, float alpha) {
        if (style == AvatarStyleV28.HUMAN) return;
        stroke.setStrokeWidth(Math.max(1.2f, s * 0.004f));
        stroke.setColor(withAlpha(style.irisColor,
                (int) ((80 + style.hologramStrength * 120) * alpha)));
        float y = -s * 0.335f;
        canvas.drawCircle(0f, y, s * 0.021f, stroke);
        if (style == AvatarStyleV28.SCI_FI) {
            canvas.drawLine(-s * 0.052f, y, -s * 0.027f, y, stroke);
            canvas.drawLine(s * 0.027f, y, s * 0.052f, y, stroke);
            canvas.drawArc(new RectF(-s * 0.30f, -s * 0.46f, s * 0.30f, s * 0.24f), 205f, 130f, false, stroke);
        } else {
            canvas.drawArc(new RectF(-s * 0.24f, -s * 0.43f, s * 0.24f, s * 0.19f), 200f, 140f, false, stroke);
        }
    }

    private void configureEmotion(String state) {
        if ("focused".equals(state)) {
            targetSmile = 0.05f; targetBrow = -0.16f; targetGlow = 0.72f;
        } else if ("thinking".equals(state)) {
            targetSmile = 0.10f; targetBrow = 0.28f; targetGlow = 0.66f;
        } else if ("happy".equals(state)) {
            targetSmile = 0.78f; targetBrow = 0.20f; targetGlow = 0.88f;
        } else if ("sleep".equals(state)) {
            targetSmile = 0.04f; targetBrow = -0.04f; targetGlow = 0.30f;
        } else {
            targetSmile = 0.20f; targetBrow = 0.06f; targetGlow = 0.58f;
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

    private static int withAlpha(int color, int alpha) {
        return Color.argb(Math.max(0, Math.min(255, alpha)), Color.red(color), Color.green(color), Color.blue(color));
    }

    private static int blend(int a, int b, float t) {
        float k = clamp(t, 0f, 1f);
        return Color.rgb((int) (Color.red(a) * (1f - k) + Color.red(b) * k),
                (int) (Color.green(a) * (1f - k) + Color.green(b) * k),
                (int) (Color.blue(a) * (1f - k) + Color.blue(b) * k));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
