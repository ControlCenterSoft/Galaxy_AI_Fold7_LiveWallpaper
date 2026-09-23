package pro.galaxyai.fold7;

import android.graphics.Canvas;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import pro.galaxyai.fold7.engine.*;

public class GalaxyAIWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new V6Engine();
    }

    private class V6Engine extends Engine {
        private final Handler handler = new Handler();
        private final FoldProfileManager profile = new FoldProfileManager();
        private final CameraController camera = new CameraController();
        private final FoldTransitionController fold = new FoldTransitionController();
        private final GalaxySceneRendererV6 galaxy = new GalaxySceneRendererV6();
        private final AIAvatarRendererV6 avatar = new AIAvatarRendererV6();
        private final ParticleEngineV6 particles = new ParticleEngineV6();
        private final GlowEngineV6 glow = new GlowEngineV6();
        private final HologramEngineV6 hologram = new HologramEngineV6();
        private boolean visible;

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

        private void render() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = holder.lockCanvas();
            if (canvas == null) return;

            int w = canvas.getWidth();
            int h = canvas.getHeight();
            FoldProfileManager.Mode mode = profile.detect(w, h);

            camera.update(fold.getProgress());
            canvas.save();
            canvas.translate(camera.getOffsetX(), camera.getOffsetY());
            canvas.scale(camera.getZoom(), camera.getZoom(), w / 2f, h / 2f);

            galaxy.draw(canvas, w, h, mode == FoldProfileManager.Mode.MAIN);
            avatar.draw(canvas, w * 0.65f, h * 0.45f);
            hologram.draw(canvas, w, h);
            glow.draw(canvas, w, h);
            particles.draw(canvas, w, h);

            canvas.restore();
            holder.unlockCanvasAndPost(canvas);
        }
    }
}
