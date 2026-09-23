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
 * v36 photoreal avatar layer.
 *
 * Loads the embedded AI portrait entirely on-device and animates it with
 * bounded parallax/breathing driven by scene state. No camera, microphone,
 * biometric input or network image download is used.
 */
public final class PhotorealAIAvatarRendererV36 {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Bitmap portrait;
    private float time;
    private float energy = 0.45f;
    private float presence = 0.75f;
    private String emotion = "calm";

    public PhotorealAIAvatarRendererV36(Context context) {
        portrait = loadPortrait(context);
    }

    public void update(SceneDecision decision, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;
        if (decision != null) {
            energy += (clamp(decision.energy, 0f, 1f) - energy) * Math.min(1f, dt * 2.8f);
            presence += (clamp(decision.avatarPresence, 0.35f, 1f) - presence) * Math.min(1f, dt * 2.8f);
            emotion = normalizeEmotion(decision.avatarState);
        }
    }

    public boolean isReady() {
        return portrait != null && !portrait.isRecycled();
    }

    public void draw(Canvas canvas, int width, int height, boolean mainDisplay) {
        if (!isReady() || width <= 0 || height <= 0) return;

        float breathe = 1f + (float) Math.sin(time * 0.42f) * (0.006f + energy * 0.004f);
        float driftX = (float) Math.sin(time * 0.16f) * width * (mainDisplay ? 0.010f : 0.006f);
        float driftY = (float) Math.cos(time * 0.13f) * height * 0.004f;

        if ("thinking".equals(emotion)) driftX *= 1.35f;
        if ("focused".equals(emotion)) driftX *= 0.65f;
        if ("sleep".equals(emotion)) {
            breathe = 1f + (float) Math.sin(time * 0.24f) * 0.0035f;
            driftX *= 0.25f;
            driftY *= 0.35f;
        }

        float srcW = portrait.getWidth();
        float srcH = portrait.getHeight();
        float baseScale = Math.max(width / srcW, height / srcH);
        float scale = baseScale * breathe;
        float drawW = srcW * scale;
        float drawH = srcH * scale;

        float left = (width - drawW) * 0.5f + driftX;
        float cropTravel = height - drawH;
        float focus = mainDisplay ? 0.43f : 0.47f;
        float top = cropTravel * focus + driftY;

        int alpha = Math.round(226f + presence * 29f);
        if ("sleep".equals(emotion)) alpha = Math.min(alpha, 236);
        paint.setAlpha(Math.max(0, Math.min(255, alpha)));

        canvas.drawBitmap(portrait, null,
                new RectF(left, top, left + drawW, top + drawH), paint);
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
                while ((n = input.read(buffer)) >= 0) {
                    out.write(buffer, 0, n);
                }
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
