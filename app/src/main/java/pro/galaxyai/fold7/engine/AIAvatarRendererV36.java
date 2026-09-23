package pro.galaxyai.fold7.engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Base64;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import pro.galaxyai.fold7.R;
import pro.galaxyai.fold7.ai.AIPersonalizationProfileV30;
import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v36 photoreal human portrait renderer.
 *
 * HUMAN style uses a bundled, local-only portrait asset with subtle breathing,
 * head drift, natural blink cover, micro-saccade glints and emotion-dependent
 * presence. SCI_FI and CUSTOM keep the proven v31 procedural renderer.
 * No camera, microphone, biometric model or network media upload is involved.
 */
public final class AIAvatarRendererV36 {
    private final AIAvatarRendererV31 procedural = new AIAvatarRendererV31();
    private final Paint photoPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final Paint softPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyelidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap humanPortrait;

    private AvatarStyleV28 style = AvatarStyleV28.SCI_FI;
    private String emotion = "calm";
    private boolean mainDisplay;
    private float time;
    private float blinkClock;
    private float blink = 1f;
    private float expression = 0.60f;
    private float eyeGlow = 0.62f;
    private float presence = 0.70f;

    public AIAvatarRendererV36(Context context) {
        humanPortrait = loadPortrait(context);
        lashPaint.setStyle(Paint.Style.STROKE);
        lashPaint.setStrokeCap(Paint.Cap.ROUND);
        glintPaint.setStyle(Paint.Style.FILL);
    }

    public void update(SceneDecision decision, AvatarStyleV28 newStyle,
                       AIPersonalizationProfileV30.Snapshot personal,
                       float deltaSeconds, boolean main) {
        procedural.update(decision, newStyle, personal, deltaSeconds, main);
        if (newStyle != null) style = newStyle;
        mainDisplay = main;

        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;
        emotion = normalizeEmotion(decision == null ? "calm" : decision.avatarState);
        presence = decision == null ? 0.70f : clamp(decision.avatarPresence, 0.30f, 1f);
        if (personal != null) {
            expression = clamp(personal.expressionIntensity, 0.35f, 1f);
            eyeGlow = clamp(personal.eyeGlow, 0f, 1f);
        }

        updateBlink(dt);
    }

    public void draw(Canvas canvas, float cx, float cy, float size) {
        if (style != AvatarStyleV28.HUMAN || humanPortrait == null) {
            procedural.draw(canvas, cx, cy, size);
            return;
        }

        float calmFactor = "focused".equals(emotion) ? 0.45f : ("sleep".equals(emotion) ? 0.25f : 1f);
        float breathe = 1f + (float) Math.sin(time * 0.88f) * 0.0075f * calmFactor;
        float headX = (float) Math.sin(time * 0.19f) * size * 0.010f * calmFactor;
        float headY = (float) Math.sin(time * 0.13f + 0.8f) * size * 0.006f * calmFactor;
        float tilt = (float) Math.sin(time * 0.11f + 0.3f) * 0.42f * calmFactor;
        if ("thinking".equals(emotion)) tilt += 0.35f;

        float portraitWidth = size * (mainDisplay ? 1.18f : 1.05f);
        float aspect = humanPortrait.getHeight() / (float) humanPortrait.getWidth();
        float portraitHeight = portraitWidth * aspect;
        RectF dst = new RectF(
                -portraitWidth * 0.5f,
                -portraitHeight * 0.34f,
                portraitWidth * 0.5f,
                portraitHeight * 0.66f);

        canvas.save();
        canvas.translate(cx + headX, cy + headY);
        canvas.rotate(tilt);
        canvas.scale(breathe, breathe);

        drawAura(canvas, size);

        int alpha = "sleep".equals(emotion) ? 214 : Math.round(232f + presence * 23f);
        photoPaint.setAlpha(Math.max(0, Math.min(255, alpha)));
        canvas.drawBitmap(humanPortrait, null, dst, photoPaint);

        drawBlink(canvas, dst);
        drawMicroSaccadeGlints(canvas, dst);
        drawEmotionLight(canvas, dst);
        canvas.restore();
    }

    private void updateBlink(float dt) {
        if ("sleep".equals(emotion)) {
            blink = 0.06f;
            blinkClock += dt;
            return;
        }

        blinkClock += dt;
        float interval = "focused".equals(emotion) ? 4.7f : ("thinking".equals(emotion) ? 3.2f : 3.8f);
        float blinkDuration = 0.28f;
        if (blinkClock >= interval) {
            float p = (blinkClock - interval) / blinkDuration;
            if (p < 0.5f) blink = 1f - p * 2f;
            else if (p < 1f) blink = (p - 0.5f) * 2f;
            else {
                blink = 1f;
                blinkClock = 0f;
            }
        } else {
            blink = 1f;
        }
    }

    private void drawAura(Canvas canvas, float size) {
        int base = "happy".equals(emotion) ? Color.rgb(172, 122, 255) : Color.rgb(96, 156, 255);
        int alpha = Math.round((30f + eyeGlow * 28f) * (0.65f + presence * 0.35f));
        softPaint.setShader(new RadialGradient(0f, -size * 0.05f, size * 0.78f,
                Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base)),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawCircle(0f, -size * 0.05f, size * 0.78f, softPaint);
        softPaint.setShader(null);
    }

    private void drawBlink(Canvas canvas, RectF dst) {
        float closed = 1f - clamp(blink, 0f, 1f);
        if (closed < 0.05f) return;

        float y = dst.top + dst.height() * 0.345f;
        float leftX = dst.left + dst.width() * 0.365f;
        float rightX = dst.left + dst.width() * 0.635f;
        float eyeW = dst.width() * 0.112f;
        float eyeH = dst.height() * 0.026f;

        int lidAlpha = Math.round(238f * closed);
        eyelidPaint.setColor(Color.argb(lidAlpha, 188, 143, 139));
        float coverH = eyeH * (0.30f + closed * 1.35f);
        canvas.drawOval(new RectF(leftX - eyeW * 0.5f, y - coverH * 0.5f,
                leftX + eyeW * 0.5f, y + coverH * 0.5f), eyelidPaint);
        canvas.drawOval(new RectF(rightX - eyeW * 0.5f, y - coverH * 0.5f,
                rightX + eyeW * 0.5f, y + coverH * 0.5f), eyelidPaint);

        lashPaint.setStrokeWidth(Math.max(1.4f, dst.width() * 0.0042f));
        lashPaint.setColor(Color.argb(Math.round(210f * closed), 58, 43, 57));
        drawLash(canvas, leftX, y, eyeW);
        drawLash(canvas, rightX, y, eyeW);
    }

    private void drawLash(Canvas canvas, float x, float y, float width) {
        Path p = new Path();
        p.moveTo(x - width * 0.46f, y);
        p.quadTo(x, y + width * 0.055f, x + width * 0.46f, y);
        canvas.drawPath(p, lashPaint);
    }

    private void drawMicroSaccadeGlints(Canvas canvas, RectF dst) {
        if (blink < 0.72f || "sleep".equals(emotion)) return;

        float saccadeX = ((float) Math.sin(time * 2.73f) + (float) Math.sin(time * 5.11f) * 0.35f)
                * dst.width() * 0.0017f;
        float saccadeY = (float) Math.sin(time * 3.47f + 0.9f) * dst.height() * 0.0008f;
        float y = dst.top + dst.height() * 0.337f + saccadeY;
        float leftX = dst.left + dst.width() * 0.365f + saccadeX;
        float rightX = dst.left + dst.width() * 0.635f + saccadeX;
        float r = Math.max(1.2f, dst.width() * 0.0045f);

        int a = Math.round(58f + eyeGlow * 72f);
        glintPaint.setColor(Color.argb(Math.min(150, a), 224, 243, 255));
        canvas.drawCircle(leftX, y, r, glintPaint);
        canvas.drawCircle(rightX, y, r, glintPaint);
    }

    private void drawEmotionLight(Canvas canvas, RectF dst) {
        int color;
        int alpha;
        if ("happy".equals(emotion)) {
            color = Color.rgb(255, 178, 210);
            alpha = Math.round(22f + expression * 20f);
        } else if ("focused".equals(emotion)) {
            color = Color.rgb(104, 184, 255);
            alpha = 25;
        } else if ("thinking".equals(emotion)) {
            color = Color.rgb(154, 126, 255);
            alpha = 24;
        } else if ("sleep".equals(emotion)) {
            color = Color.rgb(74, 94, 150);
            alpha = 24;
        } else {
            color = Color.rgb(112, 164, 255);
            alpha = 17;
        }

        softPaint.setShader(new RadialGradient(
                dst.left + dst.width() * 0.72f,
                dst.top + dst.height() * 0.28f,
                dst.width() * 0.34f,
                Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawCircle(dst.left + dst.width() * 0.72f,
                dst.top + dst.height() * 0.28f,
                dst.width() * 0.34f,
                softPaint);
        softPaint.setShader(null);
    }

    private static Bitmap loadPortrait(Context context) {
        try {
            StringBuilder encoded = new StringBuilder(180000);
            appendRaw(context, R.raw.ai_avatar_v36_1, encoded);
            appendRaw(context, R.raw.ai_avatar_v36_2, encoded);
            byte[] bytes = Base64.decode(encoded.toString(), Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void appendRaw(Context context, int resourceId, StringBuilder out) throws Exception {
        try (InputStream input = context.getResources().openRawResource(resourceId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            String line;
            while ((line = reader.readLine()) != null) out.append(line.trim());
        }
    }

    public void release() {
        if (humanPortrait != null && !humanPortrait.isRecycled()) humanPortrait.recycle();
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
