package pro.galaxyai.fold7;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import java.security.SecureRandom;

import pro.galaxyai.fold7.ai.AIDIClient;
import pro.galaxyai.fold7.ai.AIState;
import pro.galaxyai.fold7.ai.AIStateCollector;
import pro.galaxyai.fold7.ai.AmbientContextCollector;
import pro.galaxyai.fold7.engine.*;

public class GalaxyAIWallpaperService extends WallpaperService {

    private static final String PREFS_UNIVERSE = "personal_universe_v9";
    private static final String KEY_SCENE_SEED = "scene_seed";
    private static final String KEY_EVOLUTION_EPOCH = "v10_evolution_epoch";

    @Override
    public Engine onCreateEngine() {
        UniverseIdentity identity = loadUniverseIdentity();
        return new V28Engine(identity.seed, identity.evolutionEpoch);
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

    private class V28Engine extends Engine {
        private final Handler handler = new Handler();
        private final FoldProfileManager profile = new FoldProfileManager();
        private final CameraController camera = new CameraController();
        private final FoldTransitionController fold = new FoldTransitionController();
        private final GalaxySceneRendererV6 galaxy = new GalaxySceneRendererV6();
        private final AIAvatarRendererV28 avatar = new AIAvatarRendererV28();
        private final AvatarStyleControllerV28 avatarStyle =
                new AvatarStyleControllerV28(GalaxyAIWallpaperService.this);
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

        V28Engine(long universeSeed, long evolutionEpoch) {
            universe = new MemoryAwareUniverseControllerV21(universeSeed, evolutionEpoch);
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

                universe.setDecision(aidi.getDecision());
                personality.setDecision(aidi.getDecision());
                universe.update(deltaSeconds, main);
                personality.update(deltaSeconds);
                avatar.update(aidi.getDecision(), avatarStyle.getStyle(), deltaSeconds, main);
                frameDelayMillis = universe.getFrameDelayMillis(main);

                canvas.save();
                canvas.translate(
                        camera.getOffsetX() + motion.getParallaxX(),
                        camera.getOffsetY() + motion.getParallaxY()
                );
                float sceneScale = camera.getZoom() * universe.getSceneScale();
                canvas.scale(sceneScale, sceneScale, w / 2f, h / 2f);

                galaxy.draw(canvas, w, h, main);

                float avatarX = (main ? w * 0.56f : w * 0.50f)
                        + w * (universe.getAvatarHorizontalBias() + personality.getHorizontalDrift());
                float avatarY = h * (0.43f + universe.getAvatarVerticalBias()
                        + personality.getVerticalDrift());
                float baseAvatarSize = main
                        ? Math.min(w, h) * 0.72f
                        : w * 0.82f;
                float avatarSize = baseAvatarSize * personality.getAvatarScale();

                AvatarStyleV28 style = avatarStyle.getStyle();
                float styleGlow = 0.88f + style.hologramStrength * 0.20f;
                glow.draw(
                        canvas,
                        avatarX,
                        avatarY,
                        avatarSize * (0.72f + motion.getPulse() * 0.05f)
                                * universe.getPulseMultiplier()
                                * personality.getGlowMultiplier()
                                * styleGlow
                );
                if (style != AvatarStyleV28.HUMAN) {
                    hologram.draw(
                            canvas,
                            avatarX,
                            avatarY,
                            avatarSize * 1.04f,
                            motion.getPulse() * universe.getPulseMultiplier()
                                    * personality.getHologramMultiplier()
                                    * style.hologramStrength
                    );
                }
                avatar.draw(canvas, avatarX, avatarY, avatarSize);
                particles.draw(
                        canvas,
                        avatarX,
                        avatarY,
                        avatarSize * 1.08f * universe.getParticleMultiplier()
                                * personality.getAuraParticleMultiplier()
                );

                canvas.restore();
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas);
            }
        }
    }
}
