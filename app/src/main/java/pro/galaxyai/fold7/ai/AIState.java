package pro.galaxyai.fold7.ai;

import org.json.JSONException;
import org.json.JSONObject;

/** Immutable snapshot of device context sent to AIDI Gateway. */
public final class AIState {
    public final int batteryPercent;
    public final boolean charging;
    public final boolean mainDisplay;
    public final int hourOfDay;
    public final float motionLevel;
    public final int displayWidthPx;
    public final int displayHeightPx;
    public final long capturedAtMs;
    public final long sequence;
    public final AIProfileMemory.ProfileSnapshot profile;

    public AIState(int batteryPercent, boolean charging, boolean mainDisplay,
                   int hourOfDay, float motionLevel) {
        this(batteryPercent, charging, mainDisplay, hourOfDay, motionLevel,
                0, 0, System.currentTimeMillis(), 0L, null);
    }

    public AIState(int batteryPercent, boolean charging, boolean mainDisplay,
                   int hourOfDay, float motionLevel, int displayWidthPx,
                   int displayHeightPx, long capturedAtMs, long sequence) {
        this(batteryPercent, charging, mainDisplay, hourOfDay, motionLevel,
                displayWidthPx, displayHeightPx, capturedAtMs, sequence, null);
    }

    private AIState(int batteryPercent, boolean charging, boolean mainDisplay,
                    int hourOfDay, float motionLevel, int displayWidthPx,
                    int displayHeightPx, long capturedAtMs, long sequence,
                    AIProfileMemory.ProfileSnapshot profile) {
        this.batteryPercent = Math.max(0, Math.min(100, batteryPercent));
        this.charging = charging;
        this.mainDisplay = mainDisplay;
        this.hourOfDay = Math.max(0, Math.min(23, hourOfDay));
        this.motionLevel = Math.max(0f, Math.min(1f, motionLevel));
        this.displayWidthPx = Math.max(0, displayWidthPx);
        this.displayHeightPx = Math.max(0, displayHeightPx);
        this.capturedAtMs = Math.max(0L, capturedAtMs);
        this.sequence = Math.max(0L, sequence);
        this.profile = profile;
    }

    public AIState withProfile(AIProfileMemory.ProfileSnapshot profile) {
        return new AIState(
                batteryPercent,
                charging,
                mainDisplay,
                hourOfDay,
                motionLevel,
                displayWidthPx,
                displayHeightPx,
                capturedAtMs,
                sequence,
                profile
        );
    }

    public JSONObject toJson() throws JSONException {
        JSONObject display = new JSONObject();
        display.put("width_px", displayWidthPx);
        display.put("height_px", displayHeightPx);
        display.put("orientation", displayWidthPx > displayHeightPx ? "landscape" : "portrait");

        JSONObject state = new JSONObject();
        state.put("battery", batteryPercent);
        state.put("charging", charging);
        state.put("fold_state", mainDisplay ? "main" : "cover");
        state.put("hour", hourOfDay);
        state.put("motion_level", motionLevel);
        state.put("display", display);

        JSONObject root = new JSONObject();
        root.put("device", "GalaxyFold7");
        root.put("app_version", "21.0");
        root.put("captured_at_ms", capturedAtMs);
        root.put("state_sequence", sequence);
        root.put("state", state);
        if (profile != null) root.put("profile", profile.toJson());
        return root;
    }
}
