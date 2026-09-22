package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

public class AIAvatarRenderer {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase;

    public enum State { IDLE, WAKE, ACTIVE, PROCESSING, CHARGING }
    private State state = State.ACTIVE;

    public AIAvatarRenderer() {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setState(State value) {
        state = value;
    }

    public void draw(Canvas c, float cx, float cy, float size) {
        float speed = state == State.PROCESSING ? 0.030f : 0.014f;
        phase += speed;
        float s = size * 2.25f;

        Path face = new Path();
        face.moveTo(cx - 0.18f*s, cy - 0.53f*s);
        face.cubicTo(cx + 0.04f*s, cy - 0.58f*s,
                cx + 0.20f*s, cy - 0.42f*s,
                cx + 0.22f*s, cy - 0.25f*s);
        face.cubicTo(cx + 0.24f*s, cy - 0.15f*s,
                cx + 0.34f*s, cy - 0.10f*s,
                cx + 0.32f*s, cy - 0.035f*s);
        face.cubicTo(cx + 0.30f*s, cy + 0.02f*s,
                cx + 0.23f*s, cy + 0.015f*s,
                cx + 0.22f*s, cy + 0.07f*s);
        face.cubicTo(cx + 0.30f*s, cy + 0.12f*s,
                cx + 0.25f*s, cy + 0.19f*s,
                cx + 0.18f*s, cy + 0.21f*s);
        face.cubicTo(cx + 0.12f*s, cy + 0.34f*s,
                cx + 0.01f*s, cy + 0.42f*s,
                cx - 0.09f*s, cy + 0.44f*s);
        face.cubicTo(cx - 0.14f*s, cy + 0.55f*s,
                cx - 0.12f*s, cy + 0.68f*s,
                cx - 0.09f*s, cy + 0.80f*s);
        face.lineTo(cx - 0.53f*s, cy + 0.80f*s);
        face.cubicTo(cx - 0.44f*s, cy + 0.52f*s,
                cx - 0.43f*s, cy + 0.20f*s,
                cx - 0.43f*s, cy - 0.08f*s);
        face.cubicTo(cx - 0.42f*s, cy - 0.31f*s,
                cx - 0.34f*s, cy - 0.48f*s,
                cx - 0.18f*s, cy - 0.53f*s);
        face.close();

        fill.setShader(new LinearGradient(
                cx - 0.50f*s, cy - 0.45f*s,
                cx + 0.35f*s, cy + 0.45f*s,
                new int[] {
                        Color.rgb(10,17,38),
                        Color.rgb(42,48,84),
                        Color.rgb(12,19,40)
                }, null, Shader.TileMode.CLAMP));
        c.drawPath(face, fill);
        fill.setShader(null);

        for (int i=4;i>=1;i--) {
            stroke.setStrokeWidth(2.4f*i);
            stroke.setColor(Color.argb(18+i*16, 78,112,255));
            c.drawPath(face, stroke);
        }
        stroke.setStrokeWidth(2.4f);
        stroke.setColor(Color.argb(220,165,185,255));
        c.drawPath(face, stroke);

        RectF helmet = new RectF(
                cx - 0.48f*s, cy - 0.59f*s,
                cx + 0.28f*s, cy + 0.28f*s);
        stroke.setStrokeWidth(5f);
        stroke.setColor(Color.argb(175,75,105,255));
        c.drawArc(helmet,128,258,false,stroke);
        stroke.setStrokeWidth(2.3f);
        stroke.setColor(Color.argb(220,90,190,255));
        c.drawArc(helmet,166,116,false,stroke);

        Path eye = new Path();
        eye.moveTo(cx + 0.03f*s, cy - 0.18f*s);
        eye.cubicTo(cx + 0.09f*s, cy - 0.22f*s,
                cx + 0.17f*s, cy - 0.21f*s,
                cx + 0.20f*s, cy - 0.17f*s);
        eye.cubicTo(cx + 0.14f*s, cy - 0.15f*s,
                cx + 0.08f*s, cy - 0.15f*s,
                cx + 0.03f*s, cy - 0.18f*s);
        stroke.setStrokeWidth(2.7f);
        stroke.setColor(Color.argb(235,200,218,255));
        c.drawPath(eye,stroke);

        float pulse = 0.75f + 0.25f*(float)Math.sin(phase*4f);
        fill.setColor(Color.argb(240,80,145,255));
        c.drawCircle(cx+0.13f*s,cy-0.18f*s,5.5f+2.5f*pulse,fill);

        for (int j=0;j<8;j++) {
            float sy=cy-0.40f*s+j*0.080f*s;
            Path circuit=new Path();
            circuit.moveTo(cx-0.34f*s,sy);
            circuit.lineTo(cx-(0.17f+0.02f*(j%2))*s,sy);
            circuit.lineTo(cx-0.11f*s,sy+0.033f*s);
            circuit.lineTo(cx-0.02f*s,sy+0.033f*s);
            stroke.setStrokeWidth(1.7f);
            stroke.setColor(Color.argb(95+j*12,80,142,255));
            c.drawPath(circuit,stroke);
            fill.setColor(Color.argb(180,95,165,255));
            c.drawCircle(cx-0.02f*s,sy+0.033f*s,3.0f,fill);
        }

        for (int j=0;j<5;j++) {
            float sx=cx-0.06f*s-j*0.055f*s;
            Path neck=new Path();
            neck.moveTo(sx,cy+0.40f*s);
            neck.cubicTo(sx-0.03f*s,cy+0.52f*s,
                    sx+0.02f*s,cy+0.65f*s,
                    sx-0.04f*s,cy+0.78f*s);
            stroke.setStrokeWidth(2f);
            stroke.setColor(Color.argb(105+j*12,92,130,255));
            c.drawPath(neck,stroke);
        }
    }
}
