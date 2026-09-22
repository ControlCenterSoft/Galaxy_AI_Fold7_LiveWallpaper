package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

public class GalaxySceneRenderer {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float rotation = 0f;

    public void draw(Canvas canvas, int width, int height, boolean expanded) {
        drawSpace(canvas, width, height);

        if (expanded) {
            drawPlanet(canvas, width, height);
            drawOrbit(canvas, width, height);
        }

        rotation += 0.01f;
    }

    private void drawSpace(Canvas canvas, int width, int height) {
        paint.setShader(new RadialGradient(
                width * 0.7f,
                height * 0.35f,
                width,
                new int[]{
                        Color.rgb(30, 45, 110),
                        Color.rgb(5, 8, 18),
                        Color.BLACK
                },
                null,
                Shader.TileMode.CLAMP));

        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(null);
    }

    private void drawPlanet(Canvas canvas, int width, int height) {
        paint.setColor(Color.argb(90, 90, 140, 255));
        canvas.drawCircle(width * 0.22f, height * 0.18f, width * 0.22f, paint);
    }

    private void drawOrbit(Canvas canvas, int width, int height) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(Color.argb(100, 90, 180, 255));
        canvas.drawOval(
                width * 0.05f,
                height * 0.05f,
                width * 0.55f,
                height * 0.35f,
                paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
