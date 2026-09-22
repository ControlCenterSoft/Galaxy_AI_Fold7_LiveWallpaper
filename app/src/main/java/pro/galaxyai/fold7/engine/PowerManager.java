package pro.galaxyai.fold7.engine;

public class PowerManager {
    public enum Mode {
        ULTRA,
        BALANCED,
        OLED_ECO
    }

    private Mode mode = Mode.BALANCED;

    public void setMode(Mode value) {
        mode = value;
    }

    public Mode getMode() {
        return mode;
    }

    public int getParticleCount() {
        if (mode == Mode.ULTRA) return 80;
        if (mode == Mode.OLED_ECO) return 10;
        return 35;
    }
}
