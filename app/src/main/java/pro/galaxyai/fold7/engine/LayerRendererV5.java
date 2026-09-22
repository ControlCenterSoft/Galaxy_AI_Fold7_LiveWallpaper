package pro.galaxyai.fold7.engine;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class LayerRendererV5 {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    public void draw(Canvas canvas, Bitmap bitmap, float x, float y, float scale) {
        if (bitmap == null) return;
        canvas.save();
        canvas.scale(scale, scale, x, y);
        canvas.drawBitmap(bitmap, x - bitmap.getWidth()/2f, y - bitmap.getHeight()/2f, paint);
        canvas.restore();
    }
}
