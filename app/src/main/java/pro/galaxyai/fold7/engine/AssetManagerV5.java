package pro.galaxyai.fold7.engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.util.HashMap;

public class AssetManagerV5 {
    private final Context context;
    private final HashMap<String, Bitmap> cache = new HashMap<>();

    public AssetManagerV5(Context context) {
        this.context = context;
    }

    public Bitmap loadDrawable(String name) {
        if (cache.containsKey(name)) return cache.get(name);
        int id = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        if (id == 0) return null;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), id);
        cache.put(name, bitmap);
        return bitmap;
    }

    public void clear() {
        for (Bitmap bitmap : cache.values()) {
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        }
        cache.clear();
    }
}
