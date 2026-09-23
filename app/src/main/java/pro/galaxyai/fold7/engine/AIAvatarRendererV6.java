package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

public class AIAvatarRendererV6 {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase;

    public AIAvatarRendererV6() {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
    }

    public void draw(Canvas canvas, float x, float y, float size) {
        phase += 0.018f;
        float s = size;

        Path face = new Path();
        face.moveTo(x - 0.30f*s, y - 0.48f*s);
        face.cubicTo(x - 0.02f*s, y - 0.62f*s, x + 0.30f*s, y - 0.42f*s, x + 0.28f*s, y - 0.08f*s);
        face.cubicTo(x + 0.27f*s, y + 0.10f*s, x + 0.19f*s, y + 0.28f*s, x + 0.06f*s, y + 0.40f*s);
        face.cubicTo(x - 0.02f*s, y + 0.49f*s, x - 0.12f*s, y + 0.50f*s, x - 0.18f*s, y + 0.40f*s);
        face.cubicTo(x - 0.31f*s, y + 0.20f*s, x - 0.39f*s, y - 0.09f*s, x - 0.30f*s, y - 0.48f*s);
        face.close();

        fill.setShader(new LinearGradient(
                x - s*0.35f, y - s*0.45f, x + s*0.35f, y + s*0.45f,
                new int[]{Color.rgb(8,14,32), Color.rgb(42,50,90), Color.rgb(9,17,38)},
                null, Shader.TileMode.CLAMP));
        canvas.drawPath(face, fill);
        fill.setShader(null);

        for (int i=5;i>=1;i--) {
            stroke.setStrokeWidth(1.4f*i);
            stroke.setColor(Color.argb(15 + i*15, 70, 150, 255));
            canvas.drawPath(face, stroke);
        }
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(Color.argb(220, 160, 200, 255));
        canvas.drawPath(face, stroke);

        Path visor = new Path();
        visor.moveTo(x - 0.25f*s, y - 0.14f*s);
        visor.cubicTo(x - 0.04f*s, y - 0.23f*s, x + 0.16f*s, y - 0.22f*s, x + 0.25f*s, y - 0.12f*s);
        stroke.setStrokeWidth(4f);
        stroke.setColor(Color.argb(170, 85, 160, 255));
        canvas.drawPath(visor, stroke);

        float pulse = 0.72f + 0.28f*(float)Math.sin(phase*4.0f);
        fill.setColor(Color.argb(235, 90, 170, 255));
        canvas.drawCircle(x + 0.12f*s, y - 0.14f*s, 4f + 4f*pulse, fill);

        for (int i=0;i<8;i++) {
            float yy = y - 0.28f*s + i*0.075f*s;
            Path circuit = new Path();
            circuit.moveTo(x - 0.24f*s, yy);
            circuit.lineTo(x - 0.06f*s, yy);
            circuit.lineTo(x + 0.01f*s, yy + 0.028f*s);
            stroke.setStrokeWidth(1.4f);
            stroke.setColor(Color.argb(75 + i*12, 90, 155, 255));
            canvas.drawPath(circuit, stroke);
            fill.setColor(Color.argb(165, 105, 180, 255));
            canvas.drawCircle(x + 0.01f*s, yy + 0.028f*s, 2.5f, fill);
        }
    }
}
