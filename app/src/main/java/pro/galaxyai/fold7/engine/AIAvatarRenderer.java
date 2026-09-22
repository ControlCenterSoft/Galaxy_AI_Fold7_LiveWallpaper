package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class AIAvatarRenderer {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float pulse;

    public void draw(Canvas canvas, float x, float y, float size) {
        pulse += 0.04f;
        int alpha = 120 + (int)(60 * Math.sin(pulse));
        paint.setColor(Color.argb(alpha,90,150,255));
        canvas.drawCircle(x,y,size,paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(Color.CYAN);
        canvas.drawCircle(x,y,size*0.55f,paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
