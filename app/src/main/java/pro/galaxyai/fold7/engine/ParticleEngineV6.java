package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class ParticleEngineV6 {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase;

    public void draw(Canvas canvas, float cx, float cy, float radius) {
        phase += 0.02f;
        for (int i = 0; i < 48; i++) {
            double a = phase + i * 0.13;
            float x = cx + (float)Math.cos(a) * radius * (0.4f + (i % 5) * 0.08f);
            float y = cy + (float)Math.sin(a) * radius * (0.3f + (i % 7) * 0.05f);
            paint.setColor(Color.argb(80 + (i % 5) * 25, 90, 170, 255));
            canvas.drawCircle(x, y, 1.5f + (i % 3), paint);
        }
    }
}
