package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import pro.galaxyai.fold7.ai.AIPersonalizationProfileV30;
import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v31 Adaptive Portrait Presence renderer.
 *
 * Builds on v28 style/emotion semantics, but replaces the flat mask look with a layered
 * portrait: dimensional skin shading, eyelids, iris rings and highlights, cheek light,
 * nose bridge, upper/lower lips, chin light and subtle head/gaze motion. No camera or
 * biometric input is used; all motion is generated locally from bounded state.
 */
public final class AIAvatarRendererV31 {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint soft = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyeWhite = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iris = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pupil = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lip = new Paint(Paint.ANTI_ALIAS_FLAG);

    private AvatarStyleV28 style = AvatarStyleV28.SCI_FI;
    private float time;
    private float blink = 1f;
    private float blinkClock;
    private float smile = 0.16f;
    private float targetSmile = 0.16f;
    private float browLift;
    private float targetBrow;
    private float glow = 0.62f;
    private float targetGlow = 0.62f;
    private float presence = 0.70f;
    private float targetPresence = 0.70f;
    private float warmth = 0.50f;
    private float eyeGlow = 0.62f;
    private float expression = 0.60f;
    private boolean mainDisplay;
    private String emotion = "calm";

    public AIAvatarRendererV31() {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        lip.setStyle(Paint.Style.FILL);
    }

    public void update(SceneDecision decision, AvatarStyleV28 newStyle,
                       AIPersonalizationProfileV30.Snapshot personal,
                       float deltaSeconds, boolean main) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;
        mainDisplay = main;
        if (newStyle != null) style = newStyle;
        if (personal != null) {
            warmth = clamp(personal.appearanceWarmth, 0f, 1f);
            eyeGlow = clamp(personal.eyeGlow, 0f, 1f);
            expression = clamp(personal.expressionIntensity, 0.35f, 1f);
        }

        emotion = normalizeEmotion(decision == null ? "calm" : decision.avatarState);
        targetPresence = decision == null ? 0.70f : clamp(decision.avatarPresence, 0.30f, 1f);
        configureEmotion(emotion);

        float k = Math.min(1f, dt * 4.0f);
        smile += (targetSmile - smile) * k;
        browLift += (targetBrow - browLift) * k;
        glow += (targetGlow - glow) * k;
        presence += (targetPresence - presence) * k;

        blinkClock += dt;
        float interval = "focused".equals(emotion) ? 4.8f : ("sleep".equals(emotion) ? 2.2f : 3.7f);
        if (blinkClock >= interval) {
            float p = (blinkClock - interval) / 0.30f;
            blink = p < 0.5f ? 1f - p * 2f : p < 1f ? (p - 0.5f) * 2f : 1f;
            if (p >= 1f) blinkClock = 0f;
        } else {
            blink = "sleep".equals(emotion) ? 0.10f : 1f;
        }
    }

    public void draw(Canvas canvas, float cx, float cy, float size) {
        float breathe = 1f + (float) Math.sin(time * 0.95f) * 0.009f;
        float yaw = (float) Math.sin(time * 0.19f) * 0.035f;
        float driftY = (float) Math.sin(time * 0.15f + 0.7f) * size * 0.010f;
        float alpha = clamp(0.70f + presence * 0.30f, 0f, 1f);

        canvas.save();
        canvas.translate(cx + yaw * size * 0.45f, cy + driftY);
        canvas.scale(breathe * (1f - Math.abs(yaw) * 0.35f), breathe);
        drawHalo(canvas, size, alpha);
        drawNeckAndEars(canvas, size, alpha);
        drawHead(canvas, size, alpha, yaw);
        drawCheekAndTempleLight(canvas, size, alpha, yaw);
        drawEyes(canvas, size, alpha, yaw);
        drawBrows(canvas, size, alpha, yaw);
        drawNose(canvas, size, alpha, yaw);
        drawMouth(canvas, size, alpha);
        drawChinLight(canvas, size, alpha);
        drawStyleAccent(canvas, size, alpha);
        canvas.restore();
    }

    private void drawHalo(Canvas canvas, float s, float alpha) {
        int accent = style.irisColor;
        int haloAlpha = (int) ((30f + glow * 54f + eyeGlow * 18f) * alpha);
        soft.setShader(new RadialGradient(0f, -s * 0.04f, s * 0.73f,
                withAlpha(accent, haloAlpha), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(0f, -s * 0.04f, s * 0.73f, soft);
        soft.setShader(null);
    }

    private void drawNeckAndEars(Canvas canvas, float s, float alpha) {
        int skin = warmed(style.faceLight, warmth, 0.18f);
        int shade = warmed(style.faceShade, warmth, 0.10f);
        fill.setShader(new LinearGradient(0f, s * 0.26f, 0f, s * 0.65f,
                withAlpha(skin, (int) (132 * alpha)), withAlpha(shade, (int) (78 * alpha)),
                Shader.TileMode.CLAMP));
        RectF neck = new RectF(-s * 0.115f, s * 0.26f, s * 0.115f, s * 0.65f);
        canvas.drawRoundRect(neck, s * 0.08f, s * 0.08f, fill);
        fill.setShader(null);

        fill.setColor(withAlpha(blend(skin, shade, 0.32f), (int) (132 * alpha)));
        canvas.drawOval(new RectF(-s * 0.455f, -s * 0.10f, -s * 0.345f, s * 0.12f), fill);
        canvas.drawOval(new RectF(s * 0.345f, -s * 0.10f, s * 0.455f, s * 0.12f), fill);
    }

    private void drawHead(Canvas canvas, float s, float alpha, float yaw) {
        float w = s * (mainDisplay ? 0.405f : 0.420f);
        float h = s * 0.535f;
        Path head = new Path();
        head.moveTo(0f, -h);
        head.cubicTo(w * 0.76f, -h * 0.98f, w * 1.02f, -h * 0.44f, w * 0.90f, h * 0.14f);
        head.cubicTo(w * 0.82f, h * 0.58f, w * 0.48f, h * 0.93f, 0f, h);
        head.cubicTo(-w * 0.48f, h * 0.93f, -w * 0.82f, h * 0.58f, -w * 0.90f, h * 0.14f);
        head.cubicTo(-w * 1.02f, -h * 0.44f, -w * 0.76f, -h * 0.98f, 0f, -h);
        head.close();

        int light = warmed(style.faceLight, warmth, style == AvatarStyleV28.HUMAN ? 0.30f : 0.13f);
        int shade = warmed(style.faceShade, warmth, style == AvatarStyleV28.HUMAN ? 0.20f : 0.08f);
        int mid = blend(light, shade, 0.46f + yaw * 0.8f);
        fill.setShader(new LinearGradient(-w, -h * 0.65f, w, h * 0.75f,
                new int[]{withAlpha(light, (int) (190 * alpha)),
                        withAlpha(mid, (int) (158 * alpha)),
                        withAlpha(shade, (int) (112 * alpha))},
                new float[]{0f, 0.50f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawPath(head, fill);
        fill.setShader(null);

        stroke.setStrokeWidth(Math.max(1.2f, s * 0.0035f));
        stroke.setColor(withAlpha(blend(style.irisColor, light, 0.55f),
                (int) ((72 + style.hologramStrength * 60) * alpha)));
        canvas.drawPath(head, stroke);
    }

    private void drawCheekAndTempleLight(Canvas canvas, float s, float alpha, float yaw) {
        int warmPink = Color.rgb(255, 145, 174);
        int accent = blend(warmPink, style.irisColor, style == AvatarStyleV28.HUMAN ? 0.15f : 0.55f);
        float cheekAlpha = (style == AvatarStyleV28.HUMAN ? 38f : 23f) * alpha * (0.55f + warmth * 0.45f);
        soft.setShader(new RadialGradient(-s * 0.205f, s * 0.105f, s * 0.155f,
                withAlpha(accent, (int) cheekAlpha), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(-s * 0.205f, s * 0.105f, s * 0.155f, soft);
        soft.setShader(new RadialGradient(s * 0.205f, s * 0.105f, s * 0.155f,
                withAlpha(accent, (int) cheekAlpha), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(s * 0.205f, s * 0.105f, s * 0.155f, soft);
        soft.setShader(null);

        int temple = blend(style.faceLight, style.irisColor, 0.20f);
        stroke.setStrokeWidth(Math.max(1f, s * 0.0025f));
        stroke.setColor(withAlpha(temple, (int) (56 * alpha)));
        canvas.drawArc(new RectF(-s * 0.31f, -s * 0.39f, s * 0.31f, s * 0.12f), 205f, 130f, false, stroke);
    }

    private void drawEyes(Canvas canvas, float s, float alpha, float yaw) {
        float y = -s * 0.115f;
        float dx = s * 0.148f;
        float ew = s * 0.096f;
        float eh = s * 0.043f * Math.max(0.055f, blink);
        float gazeX = ((float) Math.sin(time * 0.27f) * 0.11f + yaw * 0.8f) * ew;
        float gazeY = (float) Math.sin(time * 0.17f + 1.1f) * eh * 0.08f;

        eyeWhite.setColor(style == AvatarStyleV28.HUMAN
                ? Color.argb((int) (238 * alpha), 250, 248, 249)
                : Color.argb((int) (230 * alpha), 229, 244, 255));
        iris.setColor(withAlpha(blend(style.irisColor, Color.WHITE, eyeGlow * 0.12f), (int) (248 * alpha)));
        pupil.setColor(Color.argb((int) (250 * alpha), 5, 12, 28));

        drawEye(canvas, -dx, y, ew, eh, gazeX, gazeY, s, alpha);
        drawEye(canvas, dx, y, ew, eh, gazeX, gazeY, s, alpha);
    }

    private void drawEye(Canvas canvas, float x, float y, float ew, float eh,
                         float gazeX, float gazeY, float s, float alpha) {
        RectF eye = new RectF(x - ew, y - eh, x + ew, y + eh);
        canvas.drawOval(eye, eyeWhite);
        if (eh <= s * 0.006f) {
            stroke.setStrokeWidth(Math.max(1.8f, s * 0.005f));
            stroke.setColor(withAlpha(style.faceShade, (int) (190 * alpha)));
            canvas.drawLine(x - ew, y, x + ew, y, stroke);
            return;
        }

        float irisR = Math.min(ew * 0.36f, Math.max(s * 0.012f, eh * 0.86f));
        canvas.drawCircle(x + gazeX, y + gazeY, irisR, iris);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Math.max(1f, s * 0.0025f));
        stroke.setColor(withAlpha(blend(style.irisColor, Color.BLACK, 0.35f), (int) (180 * alpha)));
        canvas.drawCircle(x + gazeX, y + gazeY, irisR * 0.78f, stroke);
        canvas.drawCircle(x + gazeX, y + gazeY, irisR * 0.45f, pupil);

        fill.setColor(Color.argb((int) ((160 + eyeGlow * 90) * alpha), 255, 255, 255));
        canvas.drawCircle(x + gazeX - irisR * 0.24f, y + gazeY - irisR * 0.28f,
                irisR * 0.18f, fill);
        fill.setColor(Color.argb((int) (95 * alpha), 255, 255, 255));
        canvas.drawCircle(x + gazeX + irisR * 0.24f, y + gazeY + irisR * 0.20f,
                irisR * 0.08f, fill);

        stroke.setStrokeWidth(Math.max(1.4f, s * 0.004f));
        stroke.setColor(withAlpha(style.faceShade, (int) (165 * alpha)));
        canvas.drawArc(eye, 195f, 150f, false, stroke);
    }

    private void drawBrows(Canvas canvas, float s, float alpha, float yaw) {
        float y = -s * 0.216f - browLift * s * 0.050f;
        float dx = s * 0.148f;
        float expressive = 0.004f + expression * 0.006f;
        stroke.setStrokeWidth(Math.max(2f, s * expressive));
        stroke.setColor(withAlpha(blend(style.faceShade, Color.rgb(60, 42, 60), 0.28f),
                (int) (205 * alpha)));
        canvas.drawLine(-dx - s * 0.078f, y + s * (0.012f + yaw * 0.03f),
                -dx + s * 0.070f, y - s * 0.006f, stroke);
        canvas.drawLine(dx - s * 0.070f, y - s * 0.006f,
                dx + s * 0.078f, y + s * (0.012f - yaw * 0.03f), stroke);
    }

    private void drawNose(Canvas canvas, float s, float alpha, float yaw) {
        int noseColor = blend(warmed(style.faceShade, warmth, 0.12f), style.faceLight, 0.48f);
        stroke.setStrokeWidth(Math.max(1.1f, s * 0.0032f));
        stroke.setColor(withAlpha(noseColor, (int) (120 * alpha)));
        Path bridge = new Path();
        bridge.moveTo(s * (0.006f + yaw * 0.12f), -s * 0.102f);
        bridge.cubicTo(s * (0.020f + yaw * 0.10f), -s * 0.025f,
                -s * 0.018f + yaw * s * 0.05f, s * 0.070f,
                -s * 0.030f + yaw * s * 0.05f, s * 0.112f);
        canvas.drawPath(bridge, stroke);

        Path base = new Path();
        base.moveTo(-s * 0.050f, s * 0.115f);
        base.cubicTo(-s * 0.018f, s * 0.139f, s * 0.018f, s * 0.139f, s * 0.050f, s * 0.115f);
        canvas.drawPath(base, stroke);
    }

    private void drawMouth(Canvas canvas, float s, float alpha) {
        float y = s * 0.222f;
        float half = s * (0.104f + expression * 0.012f);
        float smileCurve = s * (0.010f + smile * 0.050f);
        int lipColor = warmed(style.lipColor, warmth, style == AvatarStyleV28.HUMAN ? 0.28f : 0.10f);

        Path upper = new Path();
        upper.moveTo(-half, y);
        upper.cubicTo(-half * 0.48f, y - s * 0.030f, -half * 0.22f, y - s * 0.015f, 0f, y - s * 0.024f);
        upper.cubicTo(half * 0.22f, y - s * 0.015f, half * 0.48f, y - s * 0.030f, half, y);
        upper.cubicTo(half * 0.52f, y + smileCurve * 0.70f, -half * 0.52f, y + smileCurve * 0.70f, -half, y);
        upper.close();
        lip.setColor(withAlpha(blend(lipColor, style.faceShade, 0.18f), (int) (186 * alpha)));
        canvas.drawPath(upper, lip);

        Path lower = new Path();
        lower.moveTo(-half, y + s * 0.003f);
        lower.cubicTo(-half * 0.42f, y + smileCurve + s * 0.032f,
                half * 0.42f, y + smileCurve + s * 0.032f, half, y + s * 0.003f);
        lower.cubicTo(half * 0.38f, y + smileCurve * 0.45f,
                -half * 0.38f, y + smileCurve * 0.45f, -half, y + s * 0.003f);
        lower.close();
        lip.setColor(withAlpha(blend(lipColor, Color.WHITE, 0.10f), (int) (165 * alpha)));
        canvas.drawPath(lower, lip);

        stroke.setStrokeWidth(Math.max(1f, s * 0.0024f));
        stroke.setColor(withAlpha(blend(lipColor, Color.BLACK, 0.38f), (int) (165 * alpha)));
        canvas.drawLine(-half * 0.76f, y + s * 0.002f, half * 0.76f, y + s * 0.002f, stroke);
    }

    private void drawChinLight(Canvas canvas, float s, float alpha) {
        int light = warmed(style.faceLight, warmth, 0.24f);
        soft.setShader(new RadialGradient(0f, s * 0.365f, s * 0.115f,
                withAlpha(light, (int) (44 * alpha)), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(0f, s * 0.365f, s * 0.115f, soft);
        soft.setShader(null);
    }

    private void drawStyleAccent(Canvas canvas, float s, float alpha) {
        if (style == AvatarStyleV28.HUMAN) return;
        stroke.setStrokeWidth(Math.max(1.1f, s * 0.0035f));
        stroke.setColor(withAlpha(style.irisColor,
                (int) ((58 + style.hologramStrength * 92) * alpha)));
        float y = -s * 0.355f;
        canvas.drawCircle(0f, y, s * 0.017f, stroke);
        if (style == AvatarStyleV28.SCI_FI) {
            canvas.drawArc(new RectF(-s * 0.305f, -s * 0.46f, s * 0.305f, s * 0.18f),
                    208f, 124f, false, stroke);
        }
    }

    private void configureEmotion(String state) {
        float e = expression;
        if ("focused".equals(state)) {
            targetSmile = 0.03f; targetBrow = -0.13f * e; targetGlow = 0.70f;
        } else if ("thinking".equals(state)) {
            targetSmile = 0.10f; targetBrow = 0.24f * e; targetGlow = 0.65f;
        } else if ("happy".equals(state)) {
            targetSmile = 0.72f * e; targetBrow = 0.18f * e; targetGlow = 0.86f;
        } else if ("sleep".equals(state)) {
            targetSmile = 0.04f; targetBrow = -0.03f; targetGlow = 0.28f;
        } else {
            targetSmile = 0.18f * e; targetBrow = 0.05f * e; targetGlow = 0.57f;
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

    private static int warmed(int color, float warmth, float amount) {
        float k = clamp(warmth * amount, 0f, 0.45f);
        int warm = Color.rgb(255, 178, 154);
        return blend(color, warm, k);
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(Math.max(0, Math.min(255, alpha)),
                Color.red(color), Color.green(color), Color.blue(color));
    }

    private static int blend(int a, int b, float t) {
        float k = clamp(t, 0f, 1f);
        return Color.rgb(
                (int) (Color.red(a) * (1f - k) + Color.red(b) * k),
                (int) (Color.green(a) * (1f - k) + Color.green(b) * k),
                (int) (Color.blue(a) * (1f - k) + Color.blue(b) * k));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
