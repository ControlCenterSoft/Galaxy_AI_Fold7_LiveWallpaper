package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * v43-compatible portrait aura.
 *
 * Older revisions painted several translucent filled circles directly over the face.
 * The current implementation keeps only faint contour rings so the photoreal portrait
 * remains visually untouched while SCI-FI/CUSTOM styles retain a subtle aura.
 */
public class GlowEngineV6 {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GlowEngineV6() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void draw(Canvas canvas, float x, float y, float size) {
        float[] radii = {0.60f, 0.70f, 0.80f};
        int[] alpha = {18, 12, 7};
        for (int i = 0; i < radii.length; i++) {
            paint.setStrokeWidth(Math.max(1.2f, size * (0.0045f + i * 0.0015f)));
            paint.setColor(Color.argb(alpha[i], 96, 176, 255));
            canvas.drawCircle(x, y, size * radii[i], paint);
        }
    }
}
