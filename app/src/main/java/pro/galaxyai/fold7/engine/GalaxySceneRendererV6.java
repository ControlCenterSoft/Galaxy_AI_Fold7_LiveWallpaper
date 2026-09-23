package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class GalaxySceneRendererV6 {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase;

    public void draw(Canvas canvas, int width, int height, boolean mainMode) {
        phase += 0.01f;

        paint.setColor(Color.rgb(2, 5, 18));
        canvas.drawRect(0, 0, width, height, paint);

        float cx = mainMode ? width * 0.25f : width * 0.5f;
        float cy = height * 0.35f;
        float radius = mainMode ? width * 0.35f : width * 0.25f;

        for (int i = 5; i > 0; i--) {
            paint.setColor(Color.argb(15 * i, 70, 130, 255));
            canvas.drawCircle(cx, cy, radius + i * 10, paint);
        }

        paint.setColor(Color.rgb(8, 15, 40));
        canvas.drawCircle(cx, cy, radius, paint);

        for (int i = 0; i < 40; i++) {
            float x = (i * 97) % width;
            float y = (i * 53) % height;
            paint.setColor(Color.argb(80, 160, 200, 255));
            canvas.drawCircle(x, y, 1.5f, paint);
        }
    }
}
