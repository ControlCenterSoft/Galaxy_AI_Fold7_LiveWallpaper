package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v21 AI Memory Universe.
 *
 * Keeps the deterministic v19 universe underneath, while AI decisions are
 * interpolated over several seconds. This prevents visible jumps when a remembered
 * or newly generated AIDI decision replaces the previous scene state.
 */
public final class MemoryAwareUniverseControllerV21 {
    private final EventHorizonUniverseControllerV19 eventHorizon;
    private volatile SceneDecision target = SceneDecision.neutral("bootstrap");

    private float energy = 0.45f;
    private float particles = 1f;
    private float pulse = 1f;
    private float scale = 1f;
    private float biasX = 0f;
    private float biasY = 0f;

    public MemoryAwareUniverseControllerV21(long seed, long evolutionEpoch) {
        eventHorizon = new EventHorizonUniverseControllerV19(seed, evolutionEpoch);
    }

    public void setDecision(SceneDecision decision) {
        if (decision != null) target = decision;
    }

    public void update(float deltaSeconds, boolean mainDisplay) {
        float dt = Math.max(0f, Math.min(deltaSeconds, 0.25f));
        eventHorizon.update(dt, mainDisplay);

        // About a 2-4 second visual convergence at normal wallpaper frame rates.
        float blend = Math.min(1f, dt * 0.55f);
        SceneDecision desired = target;
        energy = lerp(energy, desired.energy, blend);
        particles = lerp(particles, desired.particleMultiplier, blend);
        pulse = lerp(pulse, desired.pulseMultiplier, blend);
        scale = lerp(scale, desired.sceneScaleMultiplier, blend);
        biasX = lerp(biasX, desired.horizontalBias, blend);
        biasY = lerp(biasY, desired.verticalBias, blend);
    }

    public float getSceneScale() {
        return eventHorizon.getSceneScale() * scale;
    }

    public float getAvatarHorizontalBias() {
        return eventHorizon.getAvatarHorizontalBias() + biasX;
    }

    public float getAvatarVerticalBias() {
        return eventHorizon.getAvatarVerticalBias() + biasY;
    }

    public float getPulseMultiplier() {
        return eventHorizon.getPulseMultiplier() * pulse;
    }

    public float getParticleMultiplier() {
        return eventHorizon.getParticleMultiplier() * particles;
    }

    public long getFrameDelayMillis(boolean mainDisplay) {
        long base = eventHorizon.getFrameDelayMillis(mainDisplay);
        float pacing = 1.15f - energy * 0.30f;
        long adjusted = Math.round(base * pacing);
        return Math.max(16L, Math.min(50L, adjusted));
    }

    public String getDecisionSource() {
        return target.source;
    }

    private static float lerp(float current, float target, float amount) {
        return current + (target - current) * amount;
    }
}
