package pro.galaxyai.fold7;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import pro.galaxyai.fold7.engine.*;

public class GalaxyAIWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new V5Engine();
    }

    private class V5Engine extends Engine {
        private final Handler handler = new Handler();
        private final AssetManagerV5 assets = new AssetManagerV5(GalaxyAIWallpaperService.this);
        private final LayerRendererV5 layers = new LayerRendererV5();
        private final FoldProfileManager profile = new FoldProfileManager();
        private final CameraController camera = new CameraController();
        private final GalaxySceneRenderer galaxy = new GalaxySceneRenderer();
        private final AIAvatarRenderer avatar = new AIAvatarRenderer();
        private final NeuralMeshEngine mesh = new NeuralMeshEngine();
        private final FoldTransitionController fold = new FoldTransitionController();
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
            avatar.draw(canvas, mode == FoldProfileManager.Mode.MAIN ? w * 0.72f : w * 0.65f, h * 0.45f, w * 0.22f);
            mesh.draw(canvas, w * 0.65f, h * 0.45f, w * 0.35f);

            Bitmap avatarAsset = assets.loadDrawable("ai_avatar_v5");
            if (avatarAsset != null) {
                layers.draw(canvas, avatarAsset, w * 0.65f, h * 0.45f, 0.55f);
            }

            canvas.restore();
            holder.unlockCanvasAndPost(canvas);
        }
    }
}
