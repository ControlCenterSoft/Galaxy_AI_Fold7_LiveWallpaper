package pro.galaxyai.fold7;

import android.graphics.Canvas;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import pro.galaxyai.fold7.engine.AIAvatarRenderer;
import pro.galaxyai.fold7.engine.NeuralMeshEngine;
import pro.galaxyai.fold7.engine.GalaxySceneRenderer;
import pro.galaxyai.fold7.engine.FoldTransitionController;

public class GalaxyAIWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new V4Engine();
    }

    private class V4Engine extends Engine {

        private final Handler handler = new Handler();
        private final AIAvatarRenderer avatar = new AIAvatarRenderer();
        private final NeuralMeshEngine mesh = new NeuralMeshEngine();
        private final GalaxySceneRenderer galaxy = new GalaxySceneRenderer();
        private final FoldTransitionController fold = new FoldTransitionController();

        private boolean visible;

        private final Runnable loop = new Runnable() {
            @Override
            public void run() {
                render();
                if (visible) {
                    handler.postDelayed(this, 33);
                }
            }
        };

        @Override
        public void onVisibilityChanged(boolean state) {
            visible = state;
            handler.removeCallbacks(loop);
            if (state) {
                handler.post(loop);
            }
        }

        private void render() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = holder.lockCanvas();
            if (canvas == null) return;

            int w = canvas.getWidth();
            int h = canvas.getHeight();
            boolean cover = ((float) w / h) < 0.72f;

            galaxy.draw(canvas, w, h, !cover);
            avatar.draw(canvas, cover ? w * 0.65f : w * 0.72f, h * 0.45f, w * 0.22f);
            mesh.draw(canvas, w * 0.65f, h * 0.45f, w * 0.35f);

            holder.unlockCanvasAndPost(canvas);
        }
    }
}
