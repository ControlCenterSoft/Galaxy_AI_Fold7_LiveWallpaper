package pro.galaxyai.fold7.engine;

public class CameraController {
    private float zoom = 1.0f;
    private float offsetX;
    private float offsetY;

    public void update(float progress) {
        zoom = 1.0f + progress * 0.15f;
        offsetX = progress * 20f;
        offsetY = progress * 10f;
    }

    public float getZoom() { return zoom; }
    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
}
