package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

public class NeuralMeshEngine {
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint node = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase;

    public NeuralMeshEngine() {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
    }

    public void draw(Canvas c,float cx,float cy,float radius) {
        phase += 0.018f;

        for(int j=0;j<7;j++) {
            Path p=new Path();
            float baseY=cy + (j-3)*radius*0.12f;
            p.moveTo(cx-radius*1.75f,baseY);
            for(int i=0;i<=12;i++) {
                float x=cx-radius*1.75f+i*radius*0.30f;
                float y=baseY+(float)Math.sin(phase*(1f+j*0.05f)+i*0.72f+j)*radius*0.055f;
                p.lineTo(x,y);
            }
            stroke.setStrokeWidth(1.6f+(j%2)*0.8f);
            stroke.setColor(Color.argb(35+j*7,72,130,255));
            c.drawPath(p,stroke);
        }

        for(int i=0;i<18;i++) {
            float a=(float)(i*Math.PI*2/18.0)+phase*0.7f;
            float rr=radius*(0.65f+0.22f*(float)Math.sin(i*1.7f));
            float x=cx+(float)Math.cos(a)*rr;
            float y=cy+(float)Math.sin(a)*rr*0.55f;
            int alpha=70+(i%5)*20;
            node.setColor(Color.argb(alpha,105,165,255));
            c.drawCircle(x,y,2.2f+(i%3),node);
        }
    }
}
