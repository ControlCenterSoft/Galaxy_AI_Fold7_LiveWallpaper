package pro.galaxyai.fold7.engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import pro.galaxyai.fold7.R;
import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * Deformable Live Portrait renderer, extended by v46 attentive gaze, v47 micro-expressions,
 * v59 saccade-aware blink coupling and v60 natural blink dynamics.
 *
 * The photoreal portrait remains fully local. A bounded bitmap mesh deforms only original
 * portrait pixels: head/face motion, shoulder breathing, texture blink, attentive gaze,
 * emotion-driven brow/cheek/lip micro-expressions and TTS-driven mouth articulation.
 * No solid eye ovals, facial masks, painted mouth opening, camera, microphone, location
 * or raw-media capture are used.
 */
public final class DeformableLivePortraitV45 {
    private static final int MESH_X = 20;
    private static final int MESH_Y = 36;
    private static final float EYE_Y = 0.347f;
    private static final float MOUTH_X = 0.548f;
    private static final float MOUTH_Y = 0.517f;

    private final Paint paint = new Paint(
            Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final float[] verts = new float[(MESH_X + 1) * (MESH_Y + 1) * 2];
    private final AttentionGazeControllerV46 attentionGaze = new AttentionGazeControllerV46();
    private final MicroExpressionControllerV47 microExpression = new MicroExpressionControllerV47();

    private Bitmap portrait;
    private float time;
    private float energy = 0.45f;
    private float presence = 0.75f;
    private float curiosity = 0.38f;
    private float serenity = 0.72f;
    private float mouthOpen;
    private float gazeX;
    private float gazeY;
    private float externalGazeX;
    private float externalGazeY;
    private float gazeMotion;
    private boolean hasExternalGaze;
    private float saccadeBlinkCooldown;
    private float touchX;
    private float touchY;
    private float touchWeight;
    private float blinkClock;
    private float blinkPhaseLeft = 1f;
    private float blinkPhaseRight = 1f;
    private float blinkDuration = 0.22f;
    private float doubleBlinkDelay;
    private float doubleBlinkCooldown;
    private boolean doubleBlinkActive;
    private int blinkSequence;
    private String emotion = "calm";

    public DeformableLivePortraitV45(Context context) {
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
        attentionGaze.update(decision, dt);
        microExpression.update(decision, dt);
        gazeX = attentionGaze.getGazeX();
        gazeY = attentionGaze.getGazeY();
        touchWeight *= (float) Math.pow(0.075f, dt);
        updateBlinkState(dt);
    }

    public void setMouthOpen(float value) {
        mouthOpen = clamp(value, 0f, 1f);
    }

    /**
     * v58/v59 shared gaze input. v58 makes this vector authoritative for both eye mesh and
     * head follow; v59 also derives a privacy-safe local saccade velocity from frame-to-frame
     * gaze deltas so a fast attention shift may naturally coincide with a brief blink.
     */
    public void setGaze(float normalizedX, float normalizedY) {
        float nx = clamp(normalizedX, -1f, 1f);
        float ny = clamp(normalizedY, -1f, 1f);
        if (hasExternalGaze) {
            float dx = nx - externalGazeX;
            float dy = ny - externalGazeY;
            float frameDelta = (float) Math.sqrt(dx * dx + dy * dy);
            gazeMotion = clamp(frameDelta * 22f, 0f, 2f);
        } else {
            hasExternalGaze = true;
            gazeMotion = 0f;
        }
        externalGazeX = nx;
        externalGazeY = ny;
        gazeX = nx;
        gazeY = ny;
    }

    public void onTouch(float normalizedX, float normalizedY, boolean pressed) {
        touchX = clamp(normalizedX, -1f, 1f);
        touchY = clamp(normalizedY, -1f, 1f);
        attentionGaze.onTouch(normalizedX, normalizedY, pressed);
        if (pressed) touchWeight = 1f;
        else touchWeight = Math.max(touchWeight, 0.52f);
    }

    public boolean isReady() {
        return portrait != null && !portrait.isRecycled();
    }

    public void draw(Canvas canvas, int width, int height, boolean mainDisplay) {
        if (!isReady() || width <= 0 || height <= 0) return;

        float srcW = portrait.getWidth();
        float srcH = portrait.getHeight();
        float activity = activityForEmotion();
        float breath = (float) Math.sin(time * ("sleep".equals(emotion) ? 0.22f : 0.36f));
        float coverScale = Math.max(width / srcW, height / srcH);
        float framing = mainDisplay ? 0.982f : 0.958f;
        float scale = coverScale * framing
                * (1f + breath * (0.0027f + energy * 0.0018f) * activity);
        float drawW = srcW * scale;
        float drawH = srcH * scale;

        float driftX = ((float) Math.sin(time * 0.17f)
                + (float) Math.sin(time * 0.41f + 1.1f) * 0.24f)
                * width * (mainDisplay ? 0.0028f : 0.0021f) * activity;
        float driftY = (float) Math.sin(time * 0.13f + 0.7f)
                * height * 0.0013f * activity;

        float left = (width - drawW) * 0.5f + driftX;
        float top;
        if (drawH >= height) {
            float cropTravel = height - drawH;
            top = cropTravel * (mainDisplay ? 0.405f : 0.385f) + driftY;
        } else {
            top = (height - drawH) * 0.44f + driftY;
        }

        buildMesh(srcW, srcH, activity, breath);
        int alpha = Math.round(234f + presence * 21f);
        if ("sleep".equals(emotion)) alpha = Math.min(alpha, 242);
        paint.setAlpha(Math.max(0, Math.min(255, alpha)));

        canvas.save();
        canvas.translate(left, top);
        canvas.scale(scale, scale);
        canvas.drawBitmapMesh(portrait, MESH_X, MESH_Y, verts, 0, null, 0, paint);
        canvas.restore();
    }

    private void buildMesh(float srcW, float srcH, float activity, float breath) {
        float headSway = ((float) Math.sin(time * 0.31f)
                + (float) Math.sin(time * 0.57f + 1.8f) * 0.32f)
                * activity;
        float headLift = (float) Math.sin(time * 0.23f + 0.9f) * activity;
        float localRoll = ((float) Math.sin(time * 0.19f + 0.3f) * 0.010f
                + touchX * touchWeight * 0.013f) * activity;
        float blinkAmountLeft = 1f - blinkPhaseLeft;
        float blinkAmountRight = 1f - blinkPhaseRight;
        float smile = microExpression.getSmile();
        float browLift = microExpression.getBrowLift();
        float browPinch = microExpression.getBrowPinch();
        float cheekLift = microExpression.getCheekLift();

        int p = 0;
        for (int row = 0; row <= MESH_Y; row++) {
            float ny = row / (float) MESH_Y;
            for (int col = 0; col <= MESH_X; col++) {
                float nx = col / (float) MESH_X;
                float x = nx * srcW;
                float y = ny * srcH;

                float head = radialWeight(nx, ny, 0.535f, 0.355f, 0.39f, 0.32f);
                float torso = radialWeight(nx, ny, 0.53f, 0.745f, 0.52f, 0.34f);

                float dx = head * (headSway * srcW * 0.0052f
                        + touchX * touchWeight * srcW * 0.0065f);
                float dy = head * (headLift * srcH * 0.0015f
                        + touchY * touchWeight * srcH * 0.0032f);

                float cx = 0.535f * srcW;
                float cy = 0.355f * srcH;
                dx += -(y - cy) * localRoll * head;
                dy += (x - cx) * localRoll * head;

                dy += torso * breath * srcH * (0.0019f + energy * 0.0011f) * activity;
                dx += (nx - 0.53f) * torso * breath * srcW * 0.0022f * activity;

                float eyeLeft = gaussian(nx, 0.405f, 0.095f) * gaussian(ny, EYE_Y, 0.036f);
                float eyeRight = gaussian(nx, 0.625f, 0.095f) * gaussian(ny, EYE_Y, 0.036f);
                float eyeWeight = Math.min(1f, eyeLeft + eyeRight);
                dx += gazeX * srcW * 0.0058f * eyeWeight;
                dy += gazeY * srcH * 0.0027f * eyeWeight;
                float localBlink = Math.min(1f,
                        blinkAmountLeft * eyeLeft + blinkAmountRight * eyeRight);
                dy += (EYE_Y - ny) * srcH * localBlink * 0.82f;

                // v47 eyebrow expression deforms the original brow/forehead pixels.
                float browLeft = gaussian(nx, 0.405f, 0.105f) * gaussian(ny, 0.294f, 0.035f);
                float browRight = gaussian(nx, 0.625f, 0.105f) * gaussian(ny, 0.294f, 0.035f);
                float browWeight = Math.min(1f, browLeft + browRight);
                dy -= browLift * srcH * 0.0052f * browWeight;
                float centerPull = (0.515f - nx);
                dx += centerPull * browPinch * srcW * 0.017f * browWeight;
                dy += browPinch * srcH * 0.0018f * browWeight;

                // v47 cheek lift uses original skin pixels and remains intentionally subtle.
                float cheekLeft = gaussian(nx, 0.395f, 0.12f) * gaussian(ny, 0.445f, 0.07f);
                float cheekRight = gaussian(nx, 0.665f, 0.12f) * gaussian(ny, 0.445f, 0.07f);
                float cheekWeight = Math.min(1f, cheekLeft + cheekRight);
                dy -= cheekLift * srcH * 0.0033f * cheekWeight;
                dx += (nx < 0.53f ? -1f : 1f) * cheekLift * srcW * 0.0015f * cheekWeight;

                float mouthWeight = gaussian(nx, MOUTH_X, 0.094f)
                        * gaussian(ny, MOUTH_Y, 0.043f);
                float mouthDirection = ny < MOUTH_Y ? -1f : 1f;
                dy += mouthDirection * mouthOpen * srcH * 0.0085f * mouthWeight;

                float cornerDistance = Math.abs(nx - MOUTH_X);
                float cornerWeight = mouthWeight * clamp(cornerDistance / 0.065f, 0f, 1f);
                dy -= smile * srcH * 0.0041f * cornerWeight;
                dx += (nx < MOUTH_X ? -1f : 1f) * smile * srcW * 0.0019f * cornerWeight;

                verts[p++] = x + dx;
                verts[p++] = y + dy;
            }
        }
    }

    private void updateBlinkState(float dt) {
        if ("sleep".equals(emotion)) {
            blinkPhaseLeft = 0.16f;
            blinkPhaseRight = 0.20f;
            blinkClock += dt;
            saccadeBlinkCooldown = 0f;
            doubleBlinkDelay = 0f;
            doubleBlinkActive = false;
            return;
        }

        saccadeBlinkCooldown = Math.max(0f, saccadeBlinkCooldown - dt);
        doubleBlinkCooldown = Math.max(0f, doubleBlinkCooldown - dt);
        blinkClock += dt;
        float interval = "focused".equals(emotion) ? 4.9f
                : ("thinking".equals(emotion) ? 3.3f
                : ("happy".equals(emotion) ? 3.55f : 3.9f));
        interval += (float) Math.sin(time * 0.071f + 0.8f) * 0.42f;
        blinkDuration = "focused".equals(emotion) ? 0.185f
                : ("thinking".equals(emotion) ? 0.195f
                : ("happy".equals(emotion) ? 0.235f : 0.225f));

        // v59 Saccade Blink Coupling: a sufficiently fast local gaze transition may advance
        // the next natural blink. The cooldown prevents repeated blinking during one movement.
        if (gazeMotion > 0.44f && saccadeBlinkCooldown <= 0f && blinkClock < interval) {
            blinkClock = interval;
            saccadeBlinkCooldown = 1.45f;
        }

        // v60 Natural Blink Dynamics: occasionally follow a completed natural blink with a
        // brief second blink. Timing is deterministic and local so behavior stays reproducible.
        if (doubleBlinkDelay > 0f) {
            doubleBlinkDelay -= dt;
            if (doubleBlinkDelay <= 0f) {
                blinkClock = interval;
                doubleBlinkActive = true;
            }
        }

        if (blinkClock >= interval) {
            float phase = (blinkClock - interval) / blinkDuration;
            float asymmetry = 0.018f + 0.007f
                    * (float) Math.sin((blinkSequence + 1) * 1.37f);
            blinkPhaseLeft = eyelidPhase(phase, -asymmetry);
            blinkPhaseRight = eyelidPhase(phase, asymmetry);
            if (phase >= 1f) {
                blinkPhaseLeft = 1f;
                blinkPhaseRight = 1f;
                blinkClock = 0f;
                blinkSequence++;
                if (!doubleBlinkActive && doubleBlinkCooldown <= 0f
                        && ((blinkSequence % 7) == 3
                        || ("happy".equals(emotion) && (blinkSequence % 5) == 2))) {
                    doubleBlinkDelay = 0.16f;
                    doubleBlinkCooldown = 8.5f;
                }
                doubleBlinkActive = false;
            }
        } else {
            blinkPhaseLeft = 1f;
            blinkPhaseRight = 1f;
        }
    }

    private static float eyelidPhase(float phase, float offset) {
        float p = clamp(phase + offset, 0f, 1f);
        final float closeFraction = 0.39f;
        if (p < closeFraction) return 1f - p / closeFraction;
        return (p - closeFraction) / (1f - closeFraction);
    }

    private float activityForEmotion() {
        if ("sleep".equals(emotion)) return 0.16f;
        if ("focused".equals(emotion)) return 0.54f;
        if ("thinking".equals(emotion)) return 1.07f;
        if ("happy".equals(emotion)) return 1.12f;
        return clamp(0.82f + (1f - serenity) * 0.16f + curiosity * 0.08f, 0.68f, 1.08f);
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

    private static float radialWeight(float x, float y, float cx, float cy,
                                      float radiusX, float radiusY) {
        float dx = (x - cx) / radiusX;
        float dy = (y - cy) / radiusY;
        float d2 = dx * dx + dy * dy;
        if (d2 >= 1f) return 0f;
        float t = 1f - d2;
        return t * t;
    }

    private static float gaussian(float value, float center, float sigma) {
        float d = (value - center) / sigma;
        return (float) Math.exp(-0.5f * d * d);
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
