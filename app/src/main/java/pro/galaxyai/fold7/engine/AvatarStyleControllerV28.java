package pro.galaxyai.fold7.engine;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists style independently from avatar emotion so switching themes never resets AI state. */
public final class AvatarStyleControllerV28 {
    public static final String PREFS = "avatar_style_v28";
    public static final String KEY_STYLE = "style";

    private final SharedPreferences prefs;
    private AvatarStyleV28 style;

    public AvatarStyleControllerV28(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        style = AvatarStyleV28.fromId(prefs.getString(KEY_STYLE, AvatarStyleV28.SCI_FI.id));
    }

    public AvatarStyleV28 getStyle() {
        String stored = prefs.getString(KEY_STYLE, style.id);
        AvatarStyleV28 current = AvatarStyleV28.fromId(stored);
        if (current != style) style = current;
        return style;
    }

    public void setStyle(AvatarStyleV28 style) {
        if (style == null) return;
        this.style = style;
        prefs.edit().putString(KEY_STYLE, style.id).apply();
    }

    public static void persist(Context context, AvatarStyleV28 style) {
        if (context == null || style == null) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_STYLE, style.id).apply();
    }
}
