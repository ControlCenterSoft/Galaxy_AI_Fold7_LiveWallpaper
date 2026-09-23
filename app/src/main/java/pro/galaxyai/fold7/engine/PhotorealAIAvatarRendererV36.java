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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import pro.galaxyai.fold7.R;
import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v37 lifelike motion over the v36 photoreal portrait.
 *
 * The portrait remains fully local. Natural blink envelopes, tiny eye glints,
 * micro-saccade drift, breathing and emotion-aware light are generated from
 * bounded scene state. No camera, microphone, biometric input or network
 * image upload/download is used.
 */
public final class PhotorealAIAvatarRendererV36 {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final Paint lidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint softPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Bitmap portrait;
    private float time;
    private float energy = 0.45f;
    private float presence = 0.75f;
    private float focus = 0.42f;
    private float curiosity = 0.38f;
    private float blinkClock;
    private float blink = 1f;
    private String emotion = "calm";

    public PhotorealAIAvatarRendererV36(Context context) {
        portrait = loadPortrait(context);
        lashPaint.setStyle(Paint.Style.STROKE);
        lashPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void update(SceneDecision decision, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;
        if (decision != null) {
            float k = Math.min(1f, dt * 2.8f);
            energy += (clamp(decision.energy, 0f, 1f) - energy) * k;
            presence += (clamp(decision.avatarPresence, 0.35f, 1f) - presence) * k;
            focus += (clamp(decision.focus, 0f, 1f) - focus) * k;
            curiosity += (clamp(decision.curiosity, 0f, 1f) - curiosity) * k;
            emotion = normalizeEmotion(decision.avatarState);
        }
        updateBlink(dt);
    }

    public boolean isReady() {
        return portrait != null && !portrait.isRecycled();
    }

    public void draw(Canvas canvas, int width, int height, boolean mainDisplay) {
        if (!isReady() || width <= 0 || height <= 0) return;

        float activity = "sleep".equals(emotion) ? 0.22f : ("focused".equals(emotion) ? 0.58f : 1f);
        float breathe = 1f + (float) Math.sin(time * 0.42f) * (0.0055f + energy * 0.0035f) * activity;
        float driftX = (float) Math.sin(time * 0.16f) * width * (mainDisplay ? 0.009f : 0.006f) * activity;
        float driftY = (float) Math.cos(time * 0.13f) * height * 0.0035f * activity;

        // Tiny non-periodic-looking motion makes the gaze feel alive without camera tracking.
        float microX = ((float) Math.sin(time * 2.71f) + (float) Math.sin(time * 5.03f + 0.8f) * 0.34f)
                * width * 0.00065f * (0.45f + curiosity * 0.55f) * activity;
        float microY = (float) Math.sin(time * 3.37f + 1.2f) * height * 0.00028f * activity;
        driftX += microX;
        driftY += microY;

        if ("thinking".equals(emotion)) driftX *= 1.25f;
        if ("happy".equals(emotion)) breathe += (float) Math.sin(time * 0.76f) * 0.0015f;

        float srcW = portrait.getWidth();
        float srcH = portrait.getHeight();
        float baseScale = Math.max(width / srcW, height / srcH);
        float scale = baseScale * breathe;
        float drawW = srcW * scale;
        float drawH = srcH * scale;

        float left = (width - drawW) * 0.5f + driftX;
        float cropTravel = height - drawH;
        float portraitFocus = mainDisplay ? 0.43f : 0.47f;
        float top = cropTravel * portraitFocus + driftY;
        RectF dst = new RectF(left, top, left + drawW, top + drawH);

        int alpha = Math.round(226f + presence * 29f);
        if ("sleep".equals(emotion)) alpha = Math.min(alpha, 236);
        paint.setAlpha(Math.max(0, Math.min(255, alpha)));
        canvas.drawBitmap(portrait, null, dst, paint);

        drawEmotionLight(canvas, dst);
        drawNaturalBlink(canvas, dst);
        drawEyeGlints(canvas, dst);
    }

    private void updateBlink(float dt) {
        if ("sleep".equals(emotion)) {
            blink = 0.04f;
            blinkClock += dt;
            return;
        }

        blinkClock += dt;
        float interval = "focused".equals(emotion) ? 4.8f
                : ("thinking".equals(emotion) ? 3.25f : ("happy".equals(emotion) ? 3.45f : 3.85f));
        // Slight deterministic variation avoids a metronome-like blink rhythm.
        interval += (float) Math.sin(time * 0.071f + 0.6f) * 0.42f;
        float duration = 0.26f;
        if (blinkClock >= interval) {
            float p = (blinkClock - interval) / duration;
            if (p < 0.42f) blink = 1f - p / 0.42f;
            else if (p < 1f) blink = (p - 0.42f) / 0.58f;
            else {
                blink = 1f;
                blinkClock = 0f;
            }
        } else {
            blink = 1f;
        }
    }

    private void drawNaturalBlink(Canvas canvas, RectF dst) {
        float closed = 1f - clamp(blink, 0f, 1f);
        if (closed < 0.045f) return;

        // Normalized locations are tied to the bundled portrait crop.
        float y = dst.top + dst.height() * 0.344f;
        float leftX = dst.left + dst.width() * 0.366f;
        float rightX = dst.left + dst.width() * 0.635f;
        float eyeW = dst.width() * 0.111f;
        float eyeH = dst.height() * 0.026f;
        float cover = eyeH * (0.16f + closed * 1.38f);

        int lidA = Math.min(238, Math.round(244f * closed));
        lidPaint.setColor(Color.argb(lidA, 190, 147, 143));
        drawLid(canvas, leftX, y, eyeW, cover);
        drawLid(canvas, rightX, y, eyeW, cover);

        lashPaint.setStrokeWidth(Math.max(1.2f, dst.width() * 0.0034f));
        lashPaint.setColor(Color.argb(Math.min(220, Math.round(225f * closed)), 52, 40, 53));
        drawLash(canvas, leftX, y, eyeW);
        drawLash(canvas, rightX, y, eyeW);
    }

    private void drawLid(Canvas canvas, float x, float y, float eyeW, float coverH) {
        RectF r = new RectF(x - eyeW * 0.5f, y - coverH * 0.5f,
                x + eyeW * 0.5f, y + coverH * 0.5f);
        canvas.drawOval(r, lidPaint);
    }

    private void drawLash(Canvas canvas, float x, float y, float eyeW) {
        Path p = new Path();
        p.moveTo(x - eyeW * 0.47f, y);
        p.quadTo(x, y + eyeW * 0.045f, x + eyeW * 0.47f, y);
        canvas.drawPath(p, lashPaint);
    }

    private void drawEyeGlints(Canvas canvas, RectF dst) {
        if (blink < 0.74f || "sleep".equals(emotion)) return;

        float microX = ((float) Math.sin(time * 2.77f) + (float) Math.sin(time * 4.93f) * 0.28f)
                * dst.width() * 0.00135f;
        float microY = (float) Math.sin(time * 3.61f + 0.9f) * dst.height() * 0.00055f;
        float y = dst.top + dst.height() * 0.337f + microY;
        float leftX = dst.left + dst.width() * 0.366f + microX;
        float rightX = dst.left + dst.width() * 0.635f + microX;
        float r = Math.max(1f, dst.width() * 0.0033f);

        int a = Math.min(132, Math.round(55f + focus * 42f + curiosity * 22f));
        glintPaint.setColor(Color.argb(a, 231, 246, 255));
        canvas.drawCircle(leftX, y, r, glintPaint);
        canvas.drawCircle(rightX, y, r, glintPaint);
    }

    private void drawEmotionLight(Canvas canvas, RectF dst) {
        int color;
        int alpha;
        if ("happy".equals(emotion)) {
            color = Color.rgb(255, 171, 215); alpha = 24;
        } else if ("thinking".equals(emotion)) {
            color = Color.rgb(159, 133, 255); alpha = 22;
        } else if ("focused".equals(emotion)) {
            color = Color.rgb(101, 182, 255); alpha = 20;
        } else if ("sleep".equals(emotion)) {
            color = Color.rgb(70, 85, 138); alpha = 17;
        } else {
            color = Color.rgb(112, 167, 255); alpha = 15;
        }

        float cx = dst.left + dst.width() * 0.72f;
        float cy = dst.top + dst.height() * 0.28f;
        float radius = dst.width() * 0.34f;
        softPaint.setShader(new RadialGradient(cx, cy, radius,
                Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)),
                Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius, softPaint);
        softPaint.setShader(null);
    }

    public void recycle() {
        if (portrait != null && !portrait.isRecycled()) portrait.recycle();
        portrait = null;
    }

    private static Bitmap loadPortrait(Context context) {
        try {
            int[] parts = {
                    R.raw.ai_avatar_v36_1,
                    R.raw.ai_avatar_v36_2,
                    R.raw.ai_avatar_v36_3,
                    R.raw.ai_avatar_v36_4
            };
            StringBuilder base64 = new StringBuilder(36000);
            for (int part : parts) {
                InputStream input = context.getResources().openRawResource(part);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int n;
                while ((n = input.read(buffer)) >= 0) out.write(buffer, 0, n);
                input.close();
                base64.append(out.toString("UTF-8").trim());
            }
            byte[] bytes = Base64.decode(base64.toString(), Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception ignored) {
            return null;
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
