package pro.galaxyai.fold7.engine;

public class FoldTransitionController {
    private float progress;

    public void update(boolean opened){
        progress += opened ? 0.02f : -0.02f;
        if(progress<0) progress=0;
        if(progress>1) progress=1;
    }

    public float getProgress(){
        return progress;
    }

    public boolean isExpanded(){
        return progress >= 0.95f;
    }

    public boolean isTransitioning(){
        return progress > 0f && progress < 1f;
    }
}
