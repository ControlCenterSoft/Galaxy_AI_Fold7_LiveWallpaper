package pro.galaxyai.fold7;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import java.security.SecureRandom;

import pro.galaxyai.fold7.engine.*;

public class GalaxyAIWallpaperService extends WallpaperService {

    private static final String PREFS_V9 = "personal_universe_v9";
    private static final String KEY_SCENE_SEED = "scene_seed";

    @Override
    public Engine onCreateEngine() {
        return new V9Engine(getOrCreateUniverseSeed());
    }

    private long getOrCreateUniverseSeed() {
        SharedPreferences prefs = getSharedPreferences(PREFS_V9, MODE_PRIVATE);
        long seed = prefs.getLong(KEY_SCENE_SEED, 0L);
        if (seed == 0L) {
            seed = new SecureRandom().nextLong();
            if (seed == 0L) seed = 1L;
            prefs.edit().putLong(KEY_SCENE_SEED, seed).apply();
        }
        return seed;
    }

    private class V9Engine extends Engine {
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
        private final PersonalUniverseControllerV9 universe;
        private boolean visible;

        V9Engine(long universeSeed) {
            universe = new PersonalUniverseControllerV9(universeSeed);
        }

        private final Runnable loop = new Runnable() {
            @Override
            public void run() {
                render();
                if (visible) handler.postDelayed(this, 33);
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

                motion.update(0.033f);
                fold.update(main);
                camera.update(fold.getProgress());
                universe.update(0.033f, main);

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
                hologram.draw(canvas, avatarX, avatarY, avatarSize, motion.getPulse());
                particles.draw(canvas, avatarX, avatarY, avatarSize * 1.35f);

                canvas.restore();
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas);
            }
        }
    }
}
