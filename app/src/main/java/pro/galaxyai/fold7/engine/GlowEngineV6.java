package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class GlowEngineV6 {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public void draw(Canvas canvas, float x, float y, float size) {
        for (int i = 5; i > 0; i--) {
            paint.setColor(Color.argb(10 * i, 80, 150, 255));
            canvas.drawCircle(x, y, size * i * 0.15f, paint);
        }
    }
}
