package pro.galaxyai.fold7;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.security.SecureRandom;

import pro.galaxyai.fold7.ai.AIDIClient;
import pro.galaxyai.fold7.ai.AIPersonalizationProfileV30;
import pro.galaxyai.fold7.ai.AIState;
import pro.galaxyai.fold7.ai.AIStateCollector;
import pro.galaxyai.fold7.ai.AmbientContextCollector;
import pro.galaxyai.fold7.ai.SceneDecision;
import pro.galaxyai.fold7.engine.*;

public class GalaxyAIWallpaperService extends WallpaperService {

    private static final String PREFS_UNIVERSE = "personal_universe_v9";
    private static final String KEY_SCENE_SEED = "scene_seed";
    private static final String KEY_EVOLUTION_EPOCH = "v10_evolution_epoch";

    @Override
    public Engine onCreateEngine() {
        UniverseIdentity identity = loadUniverseIdentity();
        return new V43Engine(identity.seed, identity.evolutionEpoch);
    }

    private UniverseIdentity loadUniverseIdentity() {
        SharedPreferences prefs = getSharedPreferences(PREFS_UNIVERSE, MODE_PRIVATE);
        long seed = prefs.getLong(KEY_SCENE_SEED, 0L);
        if (seed == 0L) {
            seed = new SecureRandom().nextLong();
            if (seed == 0L) seed = 1L;
        }

        long epoch = prefs.getLong(KEY_EVOLUTION_EPOCH, 0L) + 1L;
        prefs.edit()
                .putLong(KEY_SCENE_SEED, seed)
                .putLong(KEY_EVOLUTION_EPOCH, epoch)
                .apply();
        return new UniverseIdentity(seed, epoch);
    }

    private static final class UniverseIdentity {
        final long seed;
        final long evolutionEpoch;

        UniverseIdentity(long seed, long evolutionEpoch) {
            this.seed = seed;
            this.evolutionEpoch = evolutionEpoch;
        }
    }

    private class V43Engine extends Engine {
        private final Handler handler = new Handler();
        private final FoldProfileManager profile = new FoldProfileManager();
        private final CameraController camera = new CameraController();
        private final FoldTransitionController fold = new FoldTransitionController();
        private final GalaxySceneRendererV6 galaxy = new GalaxySceneRendererV6();
        private final PhotorealAIAvatarRendererV36 avatar =
                new PhotorealAIAvatarRendererV36(GalaxyAIWallpaperService.this);
        private final CinematicPortraitDepthV38 cinematicDepth = new CinematicPortraitDepthV38();
        private final AdaptiveRenderQualityV39 renderQuality = new AdaptiveRenderQualityV39();
        private final LivingAvatarMotionV40 livingMotion = new LivingAvatarMotionV40();
        private final SpeechFaceSyncV41 speechFace = new SpeechFaceSyncV41();
        private final AutonomousGestureV42 autonomousGesture = new AutonomousGestureV42();
        private final PortraitPresenceControllerV32 portraitPresence = new PortraitPresenceControllerV32();
        private final AvatarStyleControllerV28 avatarStyle =
                new AvatarStyleControllerV28(GalaxyAIWallpaperService.this);
        private final AmbientPersonalityControllerV29 ambientPersonality =
                new AmbientPersonalityControllerV29(GalaxyAIWallpaperService.this);
        private final AIPersonalizationProfileV30 personalization =
                new AIPersonalizationProfileV30(GalaxyAIWallpaperService.this);
        private final AIStatusOverlayRendererV34 statusOverlay =
                new AIStatusOverlayRendererV34(GalaxyAIWallpaperService.this);
        private final ParticleEngineV6 particles = new ParticleEngineV6();
        private final GlowEngineV6 glow = new GlowEngineV6();
        private final HologramEngineV6 hologram = new HologramEngineV6();
        private final MotionControllerV6 motion = new MotionControllerV6();
        private final AIStateCollector stateCollector =
                new AIStateCollector(GalaxyAIWallpaperService.this);
        private final AmbientContextCollector ambientContext =
                new AmbientContextCollector(GalaxyAIWallpaperService.this);
        private final AIDIClient aidi = new AIDIClient(GalaxyAIWallpaperService.this);
        private final MemoryAwareUniverseControllerV21 universe;
        private final AIPersonalityControllerV23 personality = new AIPersonalityControllerV23();
        private boolean visible;
        private long frameDelayMillis = 37L;
        private int surfaceWidth = 1;
        private int surfaceHeight = 1;

        V43Engine(long universeSeed, long evolutionEpoch) {
            universe = new MemoryAwareUniverseControllerV21(universeSeed, evolutionEpoch);
            setTouchEventsEnabled(true);
        }

        private final Runnable loop = new Runnable() {
            @Override
            public void run() {
                render();
                if (visible) handler.postDelayed(this, frameDelayMillis);
            }
        };

        @Override
        public void onVisibilityChanged(boolean state) {
            visible = state;
            handler.removeCallbacks(loop);
            if (state) {
                ambientContext.start();
                handler.post(loop);
            } else {
                ambientContext.stop();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            surfaceWidth = Math.max(1, width);
            surfaceHeight = Math.max(1, height);
            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            if (event != null) {
                float nx = clampTouch(event.getX() / Math.max(1f, surfaceWidth) * 2f - 1f);
                float ny = clampTouch(event.getY() / Math.max(1f, surfaceHeight) * 2f - 1f);
                int action = event.getActionMasked();
                boolean pressed = action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE;
                livingMotion.onTouch(nx, ny, pressed);
            }
            super.onTouchEvent(event);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            visible = false;
            handler.removeCallbacks(loop);
            ambientContext.stop();
            super.onSurfaceDestroyed(holder);
        }

        @Override
        public void onDestroy() {
            visible = false;
            handler.removeCallbacks(loop);
            ambientContext.stop();
            ambientPersonality.shutdown();
            avatar.recycle();
            aidi.shutdown();
            super.onDestroy();
        }

        private void render() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas == null) return;

                int w = canvas.getWidth();
                int h = canvas.getHeight();
                surfaceWidth = Math.max(1, w);
                surfaceHeight = Math.max(1, h);
                FoldProfileManager.Mode mode = profile.detect(w, h);
                boolean main = mode == FoldProfileManager.Mode.MAIN;

                float deltaSeconds = frameDelayMillis / 1000f;
                motion.update(deltaSeconds);
                fold.update(main);
                camera.update(fold.getProgress());

                if (aidi.needsRefresh()) {
                    AIState state = stateCollector.capture(
                            main,
                            Math.abs(motion.getPulse()),
                            w,
                            h
                    ).withContext(ambientContext.snapshot());
                    aidi.requestIfNeeded(state);
                }

                SceneDecision decision = aidi.getDecision();
                AIPersonalizationProfileV30.Snapshot personal = personalization.snapshot();
                universe.setDecision(decision);
                personality.setDecision(decision);
                universe.update(deltaSeconds, main);
                personality.update(deltaSeconds);
                avatar.update(decision, deltaSeconds);
                cinematicDepth.update(decision, deltaSeconds);
                renderQuality.update(decision, deltaSeconds, main);
                livingMotion.update(decision, deltaSeconds, main, fold.getProgress());

                portraitPresence.update(
                        decision == null ? "calm" : decision.avatarState,
                        decision == null ? 0.55f : decision.avatarPresence,
                        decision == null ? 0.42f : decision.focus,
                        decision == null ? 0.38f : decision.curiosity,
                        decision == null ? 0.45f : decision.energy,
                        personal.expressionIntensity,
                        main,
                        deltaSeconds
                );

                ambientPersonality.applyVoiceManner(personal.voicePitch, personal.voiceRate);
                ambientPersonality.maybeReact(decision);
                boolean speaking = ambientPersonality.isSpeaking();
                speechFace.update(
                        speaking,
                        ambientPersonality.getSpeechActivity(),
                        decision,
                        deltaSeconds
                );
                autonomousGesture.update(decision, speaking, deltaSeconds);
                frameDelayMillis = renderQuality.adjustFrameDelay(
                        portraitPresence.getSuggestedFrameDelayMillis(
                                universe.getFrameDelayMillis(main)),
                        main);

                canvas.save();
                canvas.translate(
                        camera.getOffsetX() + motion.getParallaxX(),
                        camera.getOffsetY() + motion.getParallaxY()
                );
                float sceneScale = camera.getZoom() * universe.getSceneScale();
                canvas.scale(sceneScale, sceneScale, w / 2f, h / 2f);

                galaxy.draw(canvas, w, h, main);

                float avatarX = (main ? w * 0.54f : w * 0.50f)
                        + w * (universe.getAvatarHorizontalBias() + personality.getHorizontalDrift()) * 0.72f
                        + w * portraitPresence.getHorizontalShift()
                        + livingMotion.getPixelOffsetX(w)
                        + autonomousGesture.getPixelOffsetX(w);
                float avatarY = h * (main ? 0.405f : 0.385f)
                        + h * (universe.getAvatarVerticalBias() + personality.getVerticalDrift()) * 0.55f
                        + h * portraitPresence.getVerticalShift()
                        + livingMotion.getPixelOffsetY(h)
                        + autonomousGesture.getPixelOffsetY(h);
                float personalScale = 0.96f + personal.expressionIntensity * 0.12f;
                float baseAvatarSize = main
                        ? Math.min(w, h) * 0.84f
                        : w * 1.02f;
                float avatarSize = baseAvatarSize
                        * personality.getAvatarScale()
                        * personalScale
                        * portraitPresence.getScaleMultiplier()
                        * livingMotion.getScale()
                        * autonomousGesture.getScale();

                AvatarStyleV28 style = avatarStyle.getStyle();
                float eyeGlow = 0.84f + personal.eyeGlow * 0.24f;
                float warmthHologram = 1.06f - personal.appearanceWarmth * 0.20f;
                float styleGlow = (0.84f + style.hologramStrength * 0.18f) * eyeGlow;
                float effectScale = renderQuality.getEffectScale()
                        * (0.98f + livingMotion.getMotionIntensity() * 0.05f)
                        * (0.99f + autonomousGesture.getIntensity() * 0.03f);

                // v43 surface-guard invariant: every broad legacy effect is rendered behind
                // the opaque photographic portrait. Nothing may paint a blue circle or scan
                // mask over the face.
                glow.draw(
                        canvas,
                        avatarX,
                        avatarY,
                        avatarSize * (0.68f + motion.getPulse() * 0.045f)
                                * universe.getPulseMultiplier()
                                * personality.getGlowMultiplier()
                                * styleGlow
                                * portraitPresence.getGlowMultiplier()
                                * effectScale
                );
                if (style != AvatarStyleV28.HUMAN) {
                    hologram.draw(
                            canvas,
                            avatarX,
                            avatarY,
                            avatarSize * 1.02f,
                            motion.getPulse() * universe.getPulseMultiplier()
                                    * personality.getHologramMultiplier()
                                    * style.hologramStrength
                                    * warmthHologram
                                    * renderQuality.getHologramScale()
                    );
                }
                particles.draw(
                        canvas,
                        avatarX,
                        avatarY,
                        avatarSize * 1.05f * universe.getParticleMultiplier()
                                * personality.getAuraParticleMultiplier()
                                * portraitPresence.getParticleMultiplier()
                                * (0.84f + personal.expressionIntensity * 0.18f)
                                * renderQuality.getParticleScale()
                );

                canvas.save();
                livingMotion.applyPortraitTransform(canvas, w, h);
                autonomousGesture.applyGestureTransform(canvas, w, h);
                cinematicDepth.drawBehind(canvas, w, h, main);
                avatar.draw(canvas, w, h, main);
                // Lip articulation remains local and is only visible while Russian TTS is
                // actually speaking. The old full-face post overlays stay disabled.
                speechFace.draw(canvas, w, h, main);
                canvas.restore();

                float overlayWidth = main
                        ? Math.min(w * 0.78f, avatarSize * 0.90f)
                        : Math.min(w * 0.90f, avatarSize * 0.96f);
                statusOverlay.draw(
                        canvas,
                        avatarX,
                        avatarY + avatarSize * 0.66f,
                        overlayWidth,
                        decision,
                        ambientPersonality.getReactionLevel(),
                        personal.expressionIntensity,
                        main
                );

                canvas.restore();
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas);
            }
        }

        private float clampTouch(float value) {
            return Math.max(-1f, Math.min(1f, value));
        }
    }
}
