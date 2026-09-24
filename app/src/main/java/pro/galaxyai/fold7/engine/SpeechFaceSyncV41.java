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
 * Privacy-safe speech face synchronizer, extended in v61 with syllabic articulation timing.
 *
 * Mouth motion is synthesized only from the local Russian TTS lifecycle/activity envelope.
 * It never reads microphone samples, audio buffers or network media. The resulting scalar
 * drives the bundled photoreal portrait mesh; no sensor-based lip tracking is used.
 */
public final class SpeechFaceSyncV41 {
    private static final float PORTRAIT_W = 941f;
    private static final float PORTRAIT_H = 1672f;

    private final Paint mouthShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lipLight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerLight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint speechGlow = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float mouthOpen;
    private float targetOpen;
    private float speechEnergy;
    private float time;
    private float articulationClock;
    private float articulationTarget;
    private int articulationIndex;
    private boolean wasSpeaking;
    private String emotion = "calm";

    public SpeechFaceSyncV41() {
        lipLight.setStyle(Paint.Style.STROKE);
        lipLight.setStrokeCap(Paint.Cap.ROUND);
        innerLight.setStyle(Paint.Style.STROKE);
        innerLight.setStrokeCap(Paint.Cap.ROUND);
    }

    public void update(boolean speaking, float activity, SceneDecision decision, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;
        if (decision != null) emotion = normalizeEmotion(decision.avatarState);

        float a = clamp(activity, 0f, 1f);
        if (speaking) {
            if (!wasSpeaking) {
                articulationClock = 0f;
                articulationTarget = 0.24f + a * 0.36f;
                articulationIndex++;
            }

            articulationClock -= dt;
            if (articulationClock <= 0f) {
                articulationIndex++;
                float primary = (float) Math.sin(articulationIndex * 2.399963f + time * 0.11f);
                float secondary = (float) Math.sin(articulationIndex * 1.173f + 1.2f);
                float shape = clamp(0.50f + primary * 0.34f + secondary * 0.16f, 0f, 1f);
                float floor = "focused".equals(emotion) ? 0.08f : 0.11f;
                float range = "happy".equals(emotion) ? 0.72f : 0.64f;
                articulationTarget = clamp(floor + a * (0.38f + range * shape), 0.06f, 0.94f);

                float cadence = 0.50f + 0.50f
                        * (float) Math.sin(articulationIndex * 1.618034f + 0.45f);
                articulationClock = 0.075f + cadence * 0.095f;
                if ("thinking".equals(emotion)) articulationClock *= 1.06f;
                if ("happy".equals(emotion)) articulationClock *= 0.94f;
            }

            targetOpen = articulationTarget;
            speechEnergy += (a - speechEnergy) * Math.min(1f, dt * 8.0f);
        } else {
            targetOpen = 0f;
            articulationClock = 0f;
            articulationTarget = 0f;
            speechEnergy += (0f - speechEnergy) * Math.min(1f, dt * 5.0f);
        }
        wasSpeaking = speaking;

        // v61 uses asymmetric inertia: openings arrive briskly while closures settle a little
        // more softly, which avoids a fixed-frequency jaw flap while keeping speech responsive.
        float speed = targetOpen > mouthOpen ? 13.8f : 10.2f;
        mouthOpen += (targetOpen - mouthOpen) * Math.min(1f, dt * speed);
        if (mouthOpen < 0.01f) mouthOpen = 0f;
    }

    public void draw(Canvas canvas, int width, int height, boolean mainDisplay) {
        if (mouthOpen <= 0.012f || width <= 0 || height <= 0) return;

        float baseScale = Math.max(width / PORTRAIT_W, height / PORTRAIT_H);
        float drawW = PORTRAIT_W * baseScale;
        float drawH = PORTRAIT_H * baseScale;
        float left = (width - drawW) * 0.5f;
        float cropTravel = height - drawH;
        float portraitFocus = mainDisplay ? 0.43f : 0.47f;
        float top = cropTravel * portraitFocus;

        float cx = left + drawW * 0.548f;
        float cy = top + drawH * 0.517f;
        float mouthW = drawW * 0.118f;
        float openH = drawH * (0.0028f + mouthOpen * 0.0105f);

        if ("happy".equals(emotion)) {
            mouthW *= 1.04f;
            cy -= drawH * 0.0008f;
        } else if ("focused".equals(emotion)) {
            mouthW *= 0.96f;
        } else if ("sleep".equals(emotion)) {
            openH *= 0.52f;
        }

        int shadowAlpha = Math.min(112, Math.round(45f + mouthOpen * 62f));
        mouthShadow.setShader(new RadialGradient(
                cx, cy, mouthW * 0.56f,
                Color.argb(shadowAlpha, 78, 31, 44),
                Color.argb(Math.max(4, shadowAlpha / 7), 119, 58, 72),
                Shader.TileMode.CLAMP));
        RectF opening = new RectF(
                cx - mouthW * 0.47f,
                cy - openH * 0.45f,
                cx + mouthW * 0.47f,
                cy + openH * 0.72f);
        canvas.drawOval(opening, mouthShadow);
        mouthShadow.setShader(null);

        float upperY = cy - openH * 0.43f;
        float lowerY = cy + openH * 0.56f;
        lipLight.setStrokeWidth(Math.max(1f, drawW * 0.0015f));
        lipLight.setShader(new LinearGradient(
                cx - mouthW * 0.5f, upperY,
                cx + mouthW * 0.5f, upperY,
                new int[]{Color.argb(18, 255, 180, 192), Color.argb(70, 255, 205, 211), Color.argb(18, 255, 180, 192)},
                null, Shader.TileMode.CLAMP));
        Path upper = new Path();
        upper.moveTo(cx - mouthW * 0.44f, upperY);
        upper.quadTo(cx, upperY - openH * 0.18f, cx + mouthW * 0.44f, upperY);
        canvas.drawPath(upper, lipLight);
        lipLight.setShader(null);

        innerLight.setStrokeWidth(Math.max(0.8f, drawW * 0.0011f));
        innerLight.setColor(Color.argb(Math.min(74, Math.round(20f + speechEnergy * 48f)), 255, 219, 218));
        Path lower = new Path();
        lower.moveTo(cx - mouthW * 0.31f, lowerY);
        lower.quadTo(cx, lowerY + openH * 0.12f, cx + mouthW * 0.31f, lowerY);
        canvas.drawPath(lower, innerLight);

        int glowAlpha = Math.min(24, Math.round(speechEnergy * 22f));
        speechGlow.setShader(new RadialGradient(
                cx, cy, mouthW * 0.92f,
                Color.argb(glowAlpha, 255, 151, 192),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, mouthW * 0.92f, speechGlow);
        speechGlow.setShader(null);
    }

    public float getMouthOpen() {
        return mouthOpen;
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
