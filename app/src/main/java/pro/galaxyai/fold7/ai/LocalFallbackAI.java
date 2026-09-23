package pro.galaxyai.fold7.ai;

public final class LocalFallbackAI {
    public SceneDecision decide(AIState state) {
        boolean night = state.hourOfDay >= 22 || state.hourOfDay < 7;
        boolean lowBattery = state.batteryPercent <= 15 && !state.charging;
        boolean active = state.motionLevel > 0.55f;

        if (lowBattery) {
            return new SceneDecision("deep_black", "resting", 0.12f,
                    0.55f, 0.72f, 0.995f, 0f, -0.002f,
                    600L, "local-fallback");
        }

        if (night && !active) {
            return new SceneDecision("deep_nebula", "calm", 0.24f,
                    0.72f, 0.82f, 0.998f, -0.001f, -0.002f,
                    600L, "local-fallback");
        }

        if (state.charging && active) {
            return new SceneDecision("energy_core", "focused", 0.82f,
                    1.34f, 1.22f, 1.006f, 0.002f, 0f,
                    300L, "local-fallback");
        }

        if (state.mainDisplay) {
            return new SceneDecision("living_continuum", "aware", 0.52f,
                    1.08f, 1.04f, 1.002f, 0.001f, 0f,
                    420L, "local-fallback");
        }

        return new SceneDecision("compact_continuum", "calm", 0.38f,
                0.92f, 0.96f, 0.999f, 0f, 0f,
                420L, "local-fallback");
    }
}
