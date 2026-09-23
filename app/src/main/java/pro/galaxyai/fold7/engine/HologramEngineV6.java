package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class HologramEngineV6 {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float intensity = 1.0f;

    public void setIntensity(float value){
        intensity = Math.max(0f, Math.min(1f, value));
    }

    public float getIntensity(){
        return intensity;
    }

    public void draw(Canvas canvas, float cx, float cy, float size, float pulse){
        float level = Math.max(0f, Math.min(1f, intensity * (0.72f + pulse * 0.28f)));
        paint.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < 7; i++) {
            float y = cy - size * 0.48f + i * size * 0.16f;
            paint.setStrokeWidth(1f + (i % 2));
            paint.setColor(Color.argb((int)(28 * level), 120, 205, 255));
            canvas.drawLine(cx - size * 0.36f, y, cx + size * 0.34f, y, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }
}
