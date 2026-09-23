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
 * v43 maskless photoreal portrait renderer.
 *
 * The v36 portrait asset remains fully local, but all synthetic facial paint layers
 * introduced in v37/v38 are intentionally removed. In particular this renderer never
 * paints solid eyelid ovals, artificial eye masks, face-sized radial color circles or
 * procedural facial geometry on top of the photographed/generated portrait.
 *
 * Liveliness is preserved through bounded whole-portrait breathing, micro-drift and
 * emotion-aware framing. Higher-level v40/v42 controllers still provide touch, Fold,
 * head/pose and autonomous gesture motion. No camera, microphone, location or raw-media
 * capture is used.
 */
public final class PhotorealAIAvatarRendererV36 {
    private final Paint portraitPaint = new Paint(
            Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);

    private Bitmap portrait;
    private float time;
    private float energy = 0.45f;
    private float presence = 0.75f;
    private float curiosity = 0.38f;
    private float serenity = 0.72f;
    private float blinkClock;
    private float blinkPhase = 1f;
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
            curiosity += (clamp(decision.curiosity, 0f, 1f) - curiosity) * k;
            serenity += (clamp(decision.serenity, 0f, 1f) - serenity) * k;
            emotion = normalizeEmotion(decision.avatarState);
        }
        updateBlinkState(dt);
    }

    public boolean isReady() {
        return portrait != null && !portrait.isRecycled();
    }

    public void draw(Canvas canvas, int width, int height, boolean mainDisplay) {
        if (!isReady() || width <= 0 || height <= 0) return;

        float activity = activityForEmotion();
        float breathe = 1f + (float) Math.sin(time * 0.39f)
                * (0.0032f + energy * 0.0022f) * activity;

        // The movement is deliberately small because v40/v42 already move the entire
        // portrait canvas. This local component only prevents the image from feeling frozen.
        float driftX = ((float) Math.sin(time * 0.17f)
                + (float) Math.sin(time * 0.43f + 0.9f) * 0.26f)
                * width * (mainDisplay ? 0.0038f : 0.0028f)
                * (0.55f + curiosity * 0.45f) * activity;
        float driftY = ((float) Math.sin(time * 0.13f + 0.7f)
                + (float) Math.sin(time * 0.31f + 1.9f) * 0.22f)
                * height * 0.0018f * activity;

        if ("focused".equals(emotion)) {
            driftX *= 0.55f;
            driftY *= 0.55f;
        } else if ("sleep".equals(emotion)) {
            driftX *= 0.20f;
            driftY *= 0.24f;
        }

        float srcW = portrait.getWidth();
        float srcH = portrait.getHeight();
        float coverScale = Math.max(width / srcW, height / srcH);

        // Narrow Fold cover screens previously cropped the forehead too aggressively.
        // A slight zoom-out leaves more natural head/shoulder framing; the underlying
        // galaxy scene fills the tiny outer margin and visually matches the portrait.
        float framing = mainDisplay ? 0.985f : 0.962f;
        float scale = coverScale * framing * breathe;
        float drawW = srcW * scale;
        float drawH = srcH * scale;

        float left = (width - drawW) * 0.5f + driftX;
        float top;
        if (drawH >= height) {
            float cropTravel = height - drawH;
            float portraitFocus = mainDisplay ? 0.41f : 0.39f;
            top = cropTravel * portraitFocus + driftY;
        } else {
            // Keep the small zoom-out vertically centered with a slight downward bias so
            // the hairline remains comfortably clear of the system status area.
            top = (height - drawH) * 0.44f + driftY;
        }

        int alpha = Math.round(232f + presence * 23f);
        if ("sleep".equals(emotion)) alpha = Math.min(alpha, 242);
        portraitPaint.setAlpha(Math.max(0, Math.min(255, alpha)));

        RectF dst = new RectF(left, top, left + drawW, top + drawH);
        canvas.drawBitmap(portrait, null, dst, portraitPaint);

        // IMPORTANT: no face-overlay drawing here. The open-eye source pixels remain
        // untouched; blinkPhase is retained only as state for future texture-based eyelid
        // animation, never rendered as geometric ovals or masks.
    }

    private void updateBlinkState(float dt) {
        if ("sleep".equals(emotion)) {
            blinkPhase = 0.12f;
            blinkClock += dt;
            return;
        }

        blinkClock += dt;
        float interval = "focused".equals(emotion) ? 4.9f
                : ("thinking".equals(emotion) ? 3.35f
                : ("happy".equals(emotion) ? 3.55f : 3.95f));
        interval += (float) Math.sin(time * 0.067f + 0.5f) * 0.38f;

        float duration = 0.24f;
        if (blinkClock >= interval) {
            float p = (blinkClock - interval) / duration;
            if (p < 0.44f) blinkPhase = 1f - p / 0.44f;
            else if (p < 1f) blinkPhase = (p - 0.44f) / 0.56f;
            else {
                blinkPhase = 1f;
                blinkClock = 0f;
            }
        } else {
            blinkPhase = 1f;
        }
    }

    public float getBlinkPhase() {
        return blinkPhase;
    }

    private float activityForEmotion() {
        if ("sleep".equals(emotion)) return 0.18f;
        if ("focused".equals(emotion)) return 0.56f;
        if ("thinking".equals(emotion)) return 1.02f;
        if ("happy".equals(emotion)) return 1.08f;
        return clamp(0.74f + (1f - serenity) * 0.18f + presence * 0.08f, 0.55f, 1.02f);
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
