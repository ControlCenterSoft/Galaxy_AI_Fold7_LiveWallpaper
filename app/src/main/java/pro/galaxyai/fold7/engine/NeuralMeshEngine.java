package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.Random;

public class NeuralMeshEngine {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();

    public void draw(Canvas canvas,float cx,float cy,float radius){
        paint.setColor(0x6680CFFF);
        paint.setStrokeWidth(2);
        for(int i=0;i<12;i++){
            float x=cx+random.nextFloat()*radius-radius/2;
            float y=cy+random.nextFloat()*radius-radius/2;
            canvas.drawLine(cx,cy,x,y,paint);
        }
    }
}
