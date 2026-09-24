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
 * Privacy-safe speech face synchronizer, extended in v61 with syllabic articulation timing
 * and in v63 with Russian coarticulation plus short word-boundary micro-pauses.
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
    private float articulationDuration = 0.12f;
    private float articulationTarget;
    private float previousArticulationTarget;
    private float phrasePauseClock;
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
                previousArticulationTarget = 0f;
                phrasePauseClock = 0f;
                articulationIndex++;
            }

            phrasePauseClock = Math.max(0f, phrasePauseClock - dt);
            articulationClock -= dt;
            if (articulationClock <= 0f) {
                articulationIndex++;
                previousArticulationTarget = articulationTarget;

                // v63 Russian coarticulation: occasional short closures emulate natural
                // word boundaries without microphone/audio inspection. The interval is
                // deterministic but non-periodic enough to avoid mechanical jaw flapping.
                int pausePeriod = 6 + (articulationIndex % 4);
                boolean wordBoundary = articulationIndex > 2 && articulationIndex % pausePeriod == 0;
                if (wordBoundary) {
                    phrasePauseClock = 0.038f + (articulationIndex % 3) * 0.012f;
                    articulationTarget = 0.035f + a * 0.055f;
                    articulationDuration = phrasePauseClock;
                    articulationClock = articulationDuration;
                } else {
                    float primary = (float) Math.sin(articulationIndex * 2.399963f + time * 0.11f);
                    float secondary = (float) Math.sin(articulationIndex * 1.173f + 1.2f);
                    float shape = clamp(0.50f + primary * 0.34f + secondary * 0.16f, 0f, 1f);
                    float floor = "focused".equals(emotion) ? 0.08f : 0.11f;
                    float range = "happy".equals(emotion) ? 0.72f : 0.64f;
                    articulationTarget = clamp(floor + a * (0.38f + range * shape), 0.06f, 0.94f);

                    float cadence = 0.50f + 0.50f
                            * (float) Math.sin(articulationIndex * 1.618034f + 0.45f);
                    articulationDuration = 0.075f + cadence * 0.095f;
                    if ("thinking".equals(emotion)) articulationDuration *= 1.06f;
                    if ("happy".equals(emotion)) articulationDuration *= 0.94f;
                    articulationClock = articulationDuration;
                }
            }

            // Blend the previous and current pseudo-phoneme target during the first part
            // of every articulation interval. This coarticulation makes adjacent syllables
            // flow into one another instead of snapping between unrelated mouth shapes.
            float progress = articulationDuration <= 0f
                    ? 1f : clamp(1f - articulationClock / articulationDuration, 0f, 1f);
            float coarticulationBlend = smoothStep(clamp(progress * 1.65f, 0f, 1f));
            targetOpen = lerp(previousArticulationTarget, articulationTarget, coarticulationBlend);
            if (phrasePauseClock > 0f) targetOpen = Math.min(targetOpen, 0.10f + a * 0.05f);
            speechEnergy += (a - speechEnergy) * Math.min(1f, dt * 8.0f);
        } else {
            targetOpen = 0f;
            articulationClock = 0f;
            articulationDuration = 0.12f;
            articulationTarget = 0f;
            previousArticulationTarget = 0f;
            phrasePauseClock = 0f;
            speechEnergy += (0f - speechEnergy) * Math.min(1f, dt * 5.0f);
        }
        wasSpeaking = speaking;

        // v61/v63 retain asymmetric inertia: openings arrive briskly while closures settle
        // more softly; word-boundary closures are slightly quicker but never instantaneous.
        float speed;
        if (phrasePauseClock > 0f) speed = 14.6f;
        else speed = targetOpen > mouthOpen ? 13.8f : 10.2f;
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

    private static float smoothStep(float value) {
        float v = clamp(value, 0f, 1f);
        return v * v * (3f - 2f * v);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp(t, 0f, 1f);
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
