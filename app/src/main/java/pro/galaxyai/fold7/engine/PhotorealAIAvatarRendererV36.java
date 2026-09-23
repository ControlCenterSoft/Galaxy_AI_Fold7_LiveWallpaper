package pro.galaxyai.fold7.engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import pro.galaxyai.fold7.R;
import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v43 photoreal surface guard over the original v36 portrait asset.
 *
 * Important rendering rule: the photographic face itself is never painted over with
 * procedural eyelid ovals, eye masks, face circles or synthetic skin fills. The avatar
 * remains alive through bounded portrait transforms (v40/v42), breathing and tiny local
 * parallax while the source portrait stays opaque. This prevents the visible "blue mask"
 * regression reported on-device while retaining all local/privacy-first behavior.
 */
public final class PhotorealAIAvatarRendererV36 {
    private static final float COVER_FRAME_SCALE = 0.970f;
    private static final float MAIN_FRAME_SCALE = 0.955f;

    private final Paint paint = new Paint(
            Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);

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

        float activity = "sleep".equals(emotion) ? 0.20f
                : ("focused".equals(emotion) ? 0.56f : 1f);
        float breathe = 1f + (float) Math.sin(time * 0.42f)
                * (0.0048f + energy * 0.0030f) * activity;
        float driftX = (float) Math.sin(time * 0.16f)
                * width * (mainDisplay ? 0.0075f : 0.0048f) * activity;
        float driftY = (float) Math.cos(time * 0.13f)
                * height * 0.0028f * activity;

        // Local micro-saccade-like portrait drift. It moves the photographic portrait
        // itself; no synthetic eye geometry is drawn on top of the face.
        float microX = ((float) Math.sin(time * 2.71f)
                + (float) Math.sin(time * 5.03f + 0.8f) * 0.34f)
                * width * 0.00052f * (0.45f + curiosity * 0.55f) * activity;
        float microY = (float) Math.sin(time * 3.37f + 1.2f)
                * height * 0.00022f * activity;
        driftX += microX;
        driftY += microY;

        if ("thinking".equals(emotion)) driftX *= 1.18f;
        if ("happy".equals(emotion)) {
            breathe += (float) Math.sin(time * 0.76f) * 0.0012f;
        }

        // A blink is retained as state for gesture/timing continuity, but v43 deliberately
        // avoids painting synthetic eyelids over the real eyes. A very small whole-portrait
        // vertical ease keeps the timing perceptible without creating a face mask.
        float closed = 1f - clamp(blink, 0f, 1f);
        float blinkEase = 1f - closed * 0.0018f;

        float srcW = portrait.getWidth();
        float srcH = portrait.getHeight();
        float cover = Math.max(width / srcW, height / srcH);
        float frameScale = mainDisplay ? MAIN_FRAME_SCALE : COVER_FRAME_SCALE;
        float scale = cover * frameScale * breathe;
        float drawW = srcW * scale;
        float drawH = srcH * scale * blinkEase;

        float left = (width - drawW) * 0.5f + driftX;
        float freeY = height - drawH;
        float focusY = mainDisplay ? 0.44f : 0.43f;
        float top = freeY * focusY + driftY;
        RectF dst = new RectF(left, top, left + drawW, top + drawH);

        // Keep the photographic face opaque. In previous builds a semi-transparent source
        // allowed the blue glow layer to contaminate skin tones and amplify the mask effect.
        paint.setAlpha(255);
        canvas.drawBitmap(portrait, null, dst, paint);
    }

    private void updateBlink(float dt) {
        if ("sleep".equals(emotion)) {
            blink = 0.94f;
            blinkClock += dt;
            return;
        }

        blinkClock += dt;
        float interval = "focused".equals(emotion) ? 4.8f
                : ("thinking".equals(emotion) ? 3.25f
                : ("happy".equals(emotion) ? 3.45f : 3.85f));
        interval += (float) Math.sin(time * 0.071f + 0.6f) * 0.42f;
        float duration = 0.24f;
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

    public float getBlink() {
        return blink;
    }

    public float getFocus() {
        return focus;
    }

    public float getPresence() {
        return presence;
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
