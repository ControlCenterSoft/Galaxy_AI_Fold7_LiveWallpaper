package pro.galaxyai.fold7.engine;

import android.graphics.Color;

/** Visual palette/style selected independently from live emotional state. */
public enum AvatarStyleV28 {
    HUMAN("human", "Human", Color.rgb(238, 219, 224), Color.rgb(154, 181, 225),
            Color.rgb(92, 177, 220), Color.rgb(241, 142, 186), 0.42f),
    SCI_FI("sci_fi", "Sci-Fi", Color.rgb(185, 230, 255), Color.rgb(96, 105, 235),
            Color.rgb(55, 225, 255), Color.rgb(226, 104, 255), 0.78f),
    CUSTOM("custom", "Custom Aurora", Color.rgb(205, 238, 255), Color.rgb(115, 99, 225),
            Color.rgb(102, 235, 212), Color.rgb(255, 132, 205), 0.62f);

    public final String id;
    public final String displayName;
    public final int faceLight;
    public final int faceShade;
    public final int irisColor;
    public final int lipColor;
    public final float hologramStrength;

    AvatarStyleV28(String id, String displayName, int faceLight, int faceShade,
                   int irisColor, int lipColor, float hologramStrength) {
        this.id = id;
        this.displayName = displayName;
        this.faceLight = faceLight;
        this.faceShade = faceShade;
        this.irisColor = irisColor;
        this.lipColor = lipColor;
        this.hologramStrength = hologramStrength;
    }

    public static AvatarStyleV28 fromId(String id) {
        for (AvatarStyleV28 style : values()) {
            if (style.id.equals(id)) return style;
        }
        return SCI_FI;
    }
}
