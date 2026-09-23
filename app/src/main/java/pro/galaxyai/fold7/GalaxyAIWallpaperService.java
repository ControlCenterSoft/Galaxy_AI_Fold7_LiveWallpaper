package pro.galaxyai.fold7;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import java.security.SecureRandom;

import pro.galaxyai.fold7.engine.*;

public class GalaxyAIWallpaperService extends WallpaperService {

    // Preserve the v9+ namespace so upgrades keep the same personal universe.
    private static final String PREFS_UNIVERSE = "personal_universe_v9";
    private static final String KEY_SCENE_SEED = "scene_seed";
    private static final String KEY_EVOLUTION_EPOCH = "v10_evolution_epoch";

    @Override
    public Engine onCreateEngine() {
        UniverseIdentity identity = loadUniverseIdentity();
        return new V17Engine(identity.seed, identity.evolutionEpoch);
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

    private class V17Engine extends Engine {
        private final Handler handler = new Handler();
        private final FoldProfileManager profile = new FoldProfileManager();
        private final CameraController camera = new CameraController();
        private final FoldTransitionController fold = new FoldTransitionController();
        private final GalaxySceneRendererV6 galaxy = new GalaxySceneRendererV6();
        private final AIAvatarRendererV6 avatar = new AIAvatarRendererV6();
        private final ParticleEngineV6 particles = new ParticleEngineV6();
        private final GlowEngineV6 glow = new GlowEngineV6();
        private final HologramEngineV6 hologram = new HologramEngineV6();
        private final MotionControllerV6 motion = new MotionControllerV6();
        private final ContinuumUniverseControllerV17 universe;
        private boolean visible;
        private long frameDelayMillis = 37L;

        V17Engine(long universeSeed, long evolutionEpoch) {
            universe = new ContinuumUniverseControllerV17(universeSeed, evolutionEpoch);
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
            if (state) handler.post(loop);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            visible = false;
            handler.removeCallbacks(loop);
            super.onSurfaceDestroyed(holder);
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
                universe.update(deltaSeconds, main);
                frameDelayMillis = universe.getFrameDelayMillis(main);

                canvas.save();
                canvas.translate(
                        camera.getOffsetX() + motion.getParallaxX(),
                        camera.getOffsetY() + motion.getParallaxY()
                );
                float sceneScale = camera.getZoom() * universe.getSceneScale();
                canvas.scale(sceneScale, sceneScale, w / 2f, h / 2f);

                galaxy.draw(canvas, w, h, main);

                float avatarX = (main ? w * 0.68f : w * 0.62f)
                        + w * universe.getAvatarHorizontalBias();
                float avatarY = h * (0.46f + universe.getAvatarVerticalBias());
                float avatarSize = main ? Math.min(w, h) * 0.44f : w * 0.38f;

                glow.draw(
                        canvas,
                        avatarX,
                        avatarY,
                        avatarSize * (0.90f + motion.getPulse() * 0.08f)
                                * universe.getPulseMultiplier()
                );
                avatar.draw(canvas, avatarX, avatarY, avatarSize);
                hologram.draw(
                        canvas,
                        avatarX,
                        avatarY,
                        avatarSize,
                        motion.getPulse() * universe.getPulseMultiplier()
                );
                particles.draw(
                        canvas,
                        avatarX,
                        avatarY,
                        avatarSize * 1.35f * universe.getParticleMultiplier()
                );

                canvas.restore();
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas);
            }
        }
    }
}
