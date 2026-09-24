package pro.galaxyai.fold7;

import android.content.Context;
import android.graphics.Canvas;
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
import pro.galaxyai.fold7.engine.InteractionPresenceV69;
import pro.galaxyai.fold7.engine.NaturalFaceDynamicsV67;
import pro.galaxyai.fold7.engine.SpeechFaceSyncV41;

/**
 * v69 Touch Presence floating torso.
 *
 * Keeps v67 maskless face dynamics and v68 overlay UX, then adds explicit long-press
 * interaction: a small body acknowledgement plus an optional short Russian TTS phrase.
 * No microphone, camera, precise location or raw-media capture is introduced.
 */
public final class FloatingAssistantViewV69 extends View {
    private final DeformableLivePortraitV45 portrait;
    private final SpeechFaceSyncV41 speechFace = new SpeechFaceSyncV41();
    private final NaturalFaceDynamicsV67 naturalFace = new NaturalFaceDynamicsV67();
    private final InteractionPresenceV69 interactionPresence = new InteractionPresenceV69();
    private final AmbientPersonalityControllerV29 personality;
    private final AIDIClient aidi;
    private final AIStateCollector stateCollector;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Path torsoMask = new Path();

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

    public FloatingAssistantViewV69(Context context) {
        super(context);
        setWillNotDraw(false);
        setBackgroundColor(android.graphics.Color.TRANSPARENT);
        portrait = new DeformableLivePortraitV45(context);
        personality = new AmbientPersonalityControllerV29(context);
        aidi = new AIDIClient(context);
        stateCollector = new AIStateCollector(context);
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
        speechFace.update(
                personality.isSpeaking(),
                personality.getSpeechActivity(),
                decision,
                dt
        );

        naturalFace.update(
                decision,
                speechFace.getMouthOpen(),
                speechFace.getSpeechEnergy(),
                dt
        );
        interactionPresence.update(
                decision,
                personality.isSpeaking(),
                naturalFace.getSpeechEnergy(),
                dt
        );

        portrait.update(decision, dt);
        portrait.setGaze(naturalFace.getGazeX(), naturalFace.getGazeY());
        portrait.setMouthOpen(naturalFace.getMouthOpen());
        portrait.setSpeechEnergy(naturalFace.getSpeechEnergy());

        float speech = naturalFace.getSpeechEnergy();
        float gazeActivity = Math.abs(naturalFace.getGazeX()) + Math.abs(naturalFace.getGazeY());
        float interaction = interactionPresence.getPresenceEnergy();
        motionLevel += ((0.065f + speech * 0.30f + gazeActivity * 0.018f
                + interaction * 0.10f) - motionLevel) * Math.min(1f, dt * 4.2f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        buildTorsoMask(w, h);
        canvas.save();
        canvas.clipPath(torsoMask);

        float headX = naturalFace.getHeadFollowX();
        float headY = naturalFace.getHeadFollowY();
        float headRoll = naturalFace.getHeadRoll();

        canvas.translate(
                headX * w * 0.010f + interactionPresence.getTranslateX() * w,
                headY * h * 0.007f + interactionPresence.getTranslateY() * h
        );
        float scale = interactionPresence.getScale();
        canvas.scale(scale, scale, w * 0.52f, h * 0.43f);
        canvas.rotate(
                headRoll * 22f + interactionPresence.getRollDegrees(),
                w * 0.52f,
                h * 0.34f
        );

        portrait.draw(canvas, w, h, false);
        canvas.restore();
    }

    private void buildTorsoMask(float w, float h) {
        torsoMask.reset();
        torsoMask.addOval(
                w * 0.055f,
                h * 0.005f,
                w * 0.965f,
                h * 0.690f,
                Path.Direction.CW
        );
        torsoMask.moveTo(w * 0.385f, h * 0.470f);
        torsoMask.cubicTo(
                w * 0.305f, h * 0.515f,
                w * 0.180f, h * 0.545f,
                w * 0.095f, h * 0.645f
        );
        torsoMask.cubicTo(
                w * 0.025f, h * 0.730f,
                w * 0.005f, h * 0.860f,
                w * 0.015f, h
        );
        torsoMask.lineTo(w * 0.985f, h);
        torsoMask.cubicTo(
                w * 0.995f, h * 0.855f,
                w * 0.965f, h * 0.720f,
                w * 0.875f, h * 0.625f
        );
        torsoMask.cubicTo(
                w * 0.785f, h * 0.535f,
                w * 0.665f, h * 0.505f,
                w * 0.595f, h * 0.465f
        );
        torsoMask.cubicTo(
                w * 0.545f, h * 0.500f,
                w * 0.435f, h * 0.505f,
                w * 0.385f, h * 0.470f
        );
        torsoMask.close();
    }

    public void reactToTap(float normalizedX, float normalizedY) {
        float x = clamp(normalizedX, -1f, 1f);
        float y = clamp(normalizedY, -1f, 1f);
        naturalFace.onTouch(x, y, true);
        portrait.onTouch(x, y, true);
        handler.postDelayed(() -> {
            if (!destroyed) {
                naturalFace.onTouch(x, y, false);
                portrait.onTouch(x, y, false);
            }
        }, 190L);
    }

    public void reactToLongPress(float normalizedX, float normalizedY) {
        float x = clamp(normalizedX, -1f, 1f);
        float y = clamp(normalizedY, -1f, 1f);
        interactionPresence.triggerAttention(x, y);
        naturalFace.onTouch(x, y, true);
        portrait.onTouch(x, y, true);
        personality.reactToInteraction(aidi.getDecision());
        handler.postDelayed(() -> {
            if (!destroyed) {
                naturalFace.onTouch(x, y, false);
                portrait.onTouch(x, y, false);
            }
        }, 360L);
    }

    public void destroy() {
        if (destroyed) return;
        destroyed = true;
        stop();
        personality.shutdown();
        aidi.shutdown();
        portrait.recycle();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
