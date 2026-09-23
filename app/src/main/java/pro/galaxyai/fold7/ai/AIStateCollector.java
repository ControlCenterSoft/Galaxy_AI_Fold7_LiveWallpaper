package pro.galaxyai.fold7.ai;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import java.util.Calendar;

public final class AIStateCollector {
    private final Context context;
    private final BatteryManager batteryManager;

    public AIStateCollector(Context context) {
        this.context = context.getApplicationContext();
        this.batteryManager = (BatteryManager) this.context.getSystemService(Context.BATTERY_SERVICE);
    }

    public AIState capture(boolean mainDisplay, float motionLevel) {
        int battery = 50;
        if (batteryManager != null) {
            int measured = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            if (measured >= 0 && measured <= 100) battery = measured;
        }

        boolean charging = false;
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent != null) {
            int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
        }

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return new AIState(battery, charging, mainDisplay, hour, motionLevel);
    }
}
