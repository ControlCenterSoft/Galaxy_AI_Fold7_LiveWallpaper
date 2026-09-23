package pro.galaxyai.fold7.ai;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Coarse environmental awareness for v22.
 *
 * Uses the non-dangerous ambient light sensor and emits only a light bucket.
 * No raw light history is stored and no CAMERA permission is required.
 */
public final class AmbientContextCollector implements SensorEventListener {
    private final SensorManager sensorManager;
    private final Sensor lightSensor;

    private volatile String lightBucket = "unknown";
    private volatile long capturedAtMs = 0L;
    private volatile boolean running = false;

    public AmbientContextCollector(Context context) {
        Context app = context.getApplicationContext();
        sensorManager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) : null;
    }

    public synchronized void start() {
        if (running || sensorManager == null || lightSensor == null) return;
        running = sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public synchronized void stop() {
        if (sensorManager != null && running) sensorManager.unregisterListener(this);
        running = false;
    }

    public AIContextSignal snapshot() {
        String bucket = lightBucket;
        String label;
        float confidence;
        if ("dark".equals(bucket)) {
            label = "low_light_environment";
            confidence = 0.86f;
        } else if ("dim".equals(bucket)) {
            label = "dim_environment";
            confidence = 0.80f;
        } else if ("bright".equals(bucket)) {
            label = "bright_environment";
            confidence = 0.84f;
        } else if ("normal".equals(bucket)) {
            label = "normal_light_environment";
            confidence = 0.72f;
        } else {
            label = "unknown";
            confidence = 0f;
        }
        return new AIContextSignal(bucket, label, confidence,
                lightSensor != null ? "android-light-sensor" : "none", capturedAtMs);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null
                || event.sensor.getType() != Sensor.TYPE_LIGHT
                || event.values == null || event.values.length == 0) return;
        float lux = Math.max(0f, event.values[0]);
        // Deliberately coarse: the exact lux value is neither stored nor transmitted.
        if (lux < 8f) lightBucket = "dark";
        else if (lux < 80f) lightBucket = "dim";
        else if (lux < 1000f) lightBucket = "normal";
        else lightBucket = "bright";
        capturedAtMs = System.currentTimeMillis();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Accuracy is intentionally not persisted or transmitted.
    }
}
