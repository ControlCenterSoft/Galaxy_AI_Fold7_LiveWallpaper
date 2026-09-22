package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class AIAvatarRenderer {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float pulse;

    public enum State { IDLE, WAKE, ACTIVE, PROCESSING, CHARGING }

    private State state = State.IDLE;

    public void setState(State value) {
        state = value;
    }

    public void draw(Canvas canvas, float x, float y, float size) {
        pulse += state == State.PROCESSING ? 0.09f : 0.04f;
        int alpha = 120 + (int)(80 * ((Math.sin(pulse)+1)/2));

        paint.setColor(Color.argb(alpha,90,150,255));
        canvas.drawCircle(x,y,size,paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(Color.argb(220,86,198,255));
        canvas.drawCircle(x,y,size*0.55f,paint);

        paint.setStyle(Paint.Style.FILL);
    }
}
