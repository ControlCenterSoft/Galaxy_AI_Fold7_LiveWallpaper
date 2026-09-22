package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

public class GalaxySceneRenderer {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase;

    public GalaxySceneRenderer() {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
    }

    public void draw(Canvas canvas, int width, int height, boolean expanded) {
        phase += 0.006f;
        drawSpace(canvas,width,height);
        drawStars(canvas,width,height);
        drawPlanet(canvas,width,height,expanded);
        if (expanded) drawOrbit(canvas,width,height);
    }

    private void drawSpace(Canvas canvas,int width,int height) {
        paint.setShader(new LinearGradient(
                0,0,0,height,
                new int[]{
                        Color.rgb(2,5,15),
                        Color.rgb(7,12,31),
                        Color.rgb(2,5,14)
                },
                new float[]{0f,0.55f,1f},
                Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,width,height,paint);
        paint.setShader(null);

        paint.setShader(new RadialGradient(
                width*0.72f,height*0.34f,Math.max(width,height)*0.62f,
                new int[]{
                        Color.argb(42,45,68,190),
                        Color.argb(16,20,34,90),
                        Color.TRANSPARENT
                },
                new float[]{0f,0.45f,1f},
                Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,width,height,paint);
        paint.setShader(null);
    }

    private void drawStars(Canvas c,int w,int h) {
        paint.setStyle(Paint.Style.FILL);
        for(int i=0;i<125;i++) {
            float x=((i*173)%997)/997f*w;
            float y=((i*269)%991)/991f*h;
            float twinkle=0.55f+0.45f*(float)Math.sin(phase*4f+i*0.73f);
            int alpha=55+(int)(150*twinkle);
            paint.setColor(Color.argb(alpha,175,205,255));
            float r=0.8f+(i%4)*0.55f;
            c.drawCircle(x,y,r,paint);
        }
    }

    private void drawPlanet(Canvas c,int w,int h,boolean expanded) {
        float r=expanded ? Math.min(w,h)*0.56f : w*0.62f;
        float cx=expanded ? w*0.16f : -w*0.10f;
        float cy=expanded ? h*0.02f : h*0.08f;

        paint.setColor(Color.rgb(3,8,24));
        c.drawCircle(cx,cy,r,paint);

        for(int i=5;i>=1;i--) {
            stroke.setStrokeWidth(4f*i);
            stroke.setColor(Color.argb(9+i*11,74,110,255));
            c.drawCircle(cx,cy,r+i*3f,stroke);
        }

        stroke.setStrokeWidth(expanded?4.5f:3.5f);
        stroke.setColor(Color.argb(215,170,198,255));
        RectF oval=new RectF(cx-r,cy-r,cx+r,cy+r);
        c.drawArc(oval,205,135,false,stroke);
    }

    private void drawOrbit(Canvas c,int w,int h) {
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(Color.argb(70,85,145,255));
        RectF orbit=new RectF(
                -w*0.08f,-h*0.20f,
                w*0.62f,h*0.36f);
        c.drawOval(orbit,stroke);
    }
}
