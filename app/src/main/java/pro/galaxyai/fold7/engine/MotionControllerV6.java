package pro.galaxyai.fold7.engine;

public class MotionControllerV6 {
    private float time;

    public void update(float delta){
        time += delta;
    }

    public float getPulse(){
        return (float)(0.5 + 0.5 * Math.sin(time));
    }

    public float getParallaxX(){
        return (float)Math.sin(time * 0.5f) * 12f;
    }

    public float getParallaxY(){
        return (float)Math.cos(time * 0.4f) * 8f;
    }
}
