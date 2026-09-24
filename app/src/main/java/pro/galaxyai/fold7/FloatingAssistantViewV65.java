package pro.galaxyai.fold7;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import pro.galaxyai.fold7.ai.AIDIClient;
import pro.galaxyai.fold7.ai.AIState;
import pro.galaxyai.fold7.ai.AIStateCollector;
import pro.galaxyai.fold7.ai.SceneDecision;
import pro.galaxyai.fold7.engine.AmbientPersonalityControllerV29;
import pro.galaxyai.fold7.engine.DeformableLivePortraitV45;
import pro.galaxyai.fold7.engine.SpeechFaceSyncV41;

/**
 * v65 floating assistant renderer.
 *
 * The assistant is no longer a wallpaper. It is a small always-on-top torso card rendered
 * from the bundled photoreal portrait. Blink, gaze and mouth motion deform the original
 * portrait pixels; no synthetic face mask is drawn over the portrait.
 */
public final class FloatingAssistantViewV65 extends View {
    private final DeformableLivePortraitV45 portrait;
    private final SpeechFaceSyncV41 speechFace = new SpeechFaceSyncV41();
    private final AmbientPersonalityControllerV29 personality;
    private final AIDIClient aidi;
    private final AIStateCollector stateCollector;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();

    private boolean running;
    private boolean destroyed;
    private long lastFrameNanos;
    private float motionLevel = 0.08f;

    private final Runnable frame = new Runnable() {
        @Override
        public void run() {
            if (!running || destroyed) return;
            updateFrame();
            invalidate();
            handler.postDelayed(this, 33L);
        }
    };

    public FloatingAssistantViewV65(Context context) {
        super(context);
        setWillNotDraw(false);
        portrait = new DeformableLivePortraitV45(context);
        personality = new AmbientPersonalityControllerV29(context);
        aidi = new AIDIClient(context);
        stateCollector = new AIStateCollector(context);

        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(dp(1.2f));
        border.setColor(Color.argb(145, 142, 196, 255));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    private void start() {
        if (destroyed || running) return;
        running = true;
        lastFrameNanos = System.nanoTime();
        handler.post(frame);
    }

    private void stop() {
        running = false;
        handler.removeCallbacks(frame);
    }

    private void updateFrame() {
        long now = System.nanoTime();
        float dt = Math.max(0.008f, Math.min(0.050f,
                (now - lastFrameNanos) / 1_000_000_000f));
        lastFrameNanos = now;

        if (aidi.needsRefresh()) {
            AIState state = stateCollector.capture(false, motionLevel, getWidth(), getHeight());
            aidi.requestIfNeeded(state);
        }

        SceneDecision decision = aidi.getDecision();
        personality.maybeReact(decision);
        speechFace.update(personality.isSpeaking(), personality.getSpeechActivity(), decision, dt);
        portrait.setMouthOpen(speechFace.getMouthOpen());
        portrait.update(decision, dt);

        float speech = speechFace.getSpeechEnergy();
        motionLevel += ((0.08f + speech * 0.30f) - motionLevel) * Math.min(1f, dt * 4f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float inset = dp(2f);
        float radius = Math.min(w, h) * 0.29f;
        clip.reset();
        clip.addRoundRect(inset, inset, w - inset, h - inset,
                radius, radius, Path.Direction.CW);

        canvas.save();
        canvas.clipPath(clip);
        portrait.draw(canvas, w, h, false);
        canvas.restore();

        canvas.drawRoundRect(inset, inset, w - inset, h - inset,
                radius, radius, border);
    }

    public void reactToTap(float normalizedX, float normalizedY) {
        float x = clamp(normalizedX, -1f, 1f);
        float y = clamp(normalizedY, -1f, 1f);
        portrait.onTouch(x, y, true);
        handler.postDelayed(() -> {
            if (!destroyed) portrait.onTouch(x, y, false);
        }, 180L);
    }

    public void destroy() {
        if (destroyed) return;
        destroyed = true;
        stop();
        personality.shutdown();
        aidi.shutdown();
        portrait.recycle();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
