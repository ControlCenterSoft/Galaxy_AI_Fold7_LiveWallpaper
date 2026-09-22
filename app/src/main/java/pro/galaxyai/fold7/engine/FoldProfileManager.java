package pro.galaxyai.fold7.engine;

public class FoldProfileManager {
    public enum Mode { COVER, MAIN }

    public Mode detect(int width, int height) {
        return ((float)width / (float)height < 0.72f) ? Mode.COVER : Mode.MAIN;
    }

    public boolean isHighQuality(Mode mode) {
        return mode == Mode.MAIN;
    }
}
