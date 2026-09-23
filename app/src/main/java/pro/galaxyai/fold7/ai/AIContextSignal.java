package pro.galaxyai.fold7.ai;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Privacy-bounded environmental context for v22.
 *
 * The wallpaper sends semantic/coarse context only. It never contains image bytes,
 * audio, precise location or raw sensor history. Camera capture is deliberately not
 * required by the wallpaper runtime.
 */
public final class AIContextSignal {
    public final String lightBucket;
    public final String semanticLabel;
    public final float confidence;
    public final String source;
    public final long capturedAtMs;

    public AIContextSignal(String lightBucket, String semanticLabel, float confidence,
                           String source, long capturedAtMs) {
        this.lightBucket = safe(lightBucket, "unknown", 16);
        this.semanticLabel = safe(semanticLabel, "unknown", 48);
        this.confidence = Math.max(0f, Math.min(1f, confidence));
        this.source = safe(source, "none", 32);
        this.capturedAtMs = Math.max(0L, capturedAtMs);
    }

    public static AIContextSignal unknown() {
        return new AIContextSignal("unknown", "unknown", 0f, "none", 0L);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("schema", "context-v1");
        root.put("light", lightBucket);
        root.put("semantic_label", semanticLabel);
        root.put("confidence", confidence);
        root.put("source", source);
        root.put("captured_at_ms", capturedAtMs);
        root.put("raw_media", false);
        return root;
    }

    private static String safe(String value, String fallback, int maxLength) {
        if (value == null || value.length() == 0) return fallback;
        return value.substring(0, Math.min(maxLength, value.length()));
    }
}
