package pro.galaxyai.fold7.ai;

import org.json.JSONException;
import org.json.JSONObject;

public final class AIState {
    public final int batteryPercent;
    public final boolean charging;
    public final boolean mainDisplay;
    public final int hourOfDay;
    public final float motionLevel;

    public AIState(int batteryPercent, boolean charging, boolean mainDisplay,
                   int hourOfDay, float motionLevel) {
        this.batteryPercent = Math.max(0, Math.min(100, batteryPercent));
        this.charging = charging;
        this.mainDisplay = mainDisplay;
        this.hourOfDay = Math.max(0, Math.min(23, hourOfDay));
        this.motionLevel = Math.max(0f, Math.min(1f, motionLevel));
    }

    public JSONObject toJson() throws JSONException {
        JSONObject state = new JSONObject();
        state.put("battery", batteryPercent);
        state.put("charging", charging);
        state.put("fold_state", mainDisplay ? "main" : "cover");
        state.put("hour", hourOfDay);
        state.put("motion_level", motionLevel);

        JSONObject root = new JSONObject();
        root.put("device", "GalaxyFold7");
        root.put("app_version", "20.0");
        root.put("state", state);
        return root;
    }
}
