package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Paint;

public class NeuralMeshEngine {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase = 0f;

    public void draw(Canvas canvas, float cx, float cy, float radius) {
        phase += 0.04f;

        paint.setColor(0x6680CFFF);
        paint.setStrokeWidth(2);

        for (int i = 0; i < 16; i++) {
            double angle = phase + i * Math.PI * 2 / 16;
            float x = cx + (float)Math.cos(angle) * radius * 0.5f;
            float y = cy + (float)Math.sin(angle) * radius * 0.5f;
            canvas.drawLine(cx, cy, x, y, paint);
        }
    }
}
