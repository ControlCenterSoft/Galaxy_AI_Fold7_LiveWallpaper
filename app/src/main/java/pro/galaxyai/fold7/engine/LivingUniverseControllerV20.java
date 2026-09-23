package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v20 AI Living Universe: v19 deterministic evolution plus a bounded AI decision layer.
 * The renderer never depends on network availability; decisions are cached and clamped.
 */
public final class LivingUniverseControllerV20 {
    private final EventHorizonUniverseControllerV19 eventHorizon;
    private volatile SceneDecision decision = SceneDecision.neutral("bootstrap");

    public LivingUniverseControllerV20(long seed, long evolutionEpoch) {
        eventHorizon = new EventHorizonUniverseControllerV19(seed, evolutionEpoch);
    }

    public void setDecision(SceneDecision decision) {
        if (decision != null) this.decision = decision;
    }

    public void update(float deltaSeconds, boolean mainDisplay) {
        eventHorizon.update(deltaSeconds, mainDisplay);
    }

    public float getSceneScale() {
        return eventHorizon.getSceneScale() * decision.sceneScaleMultiplier;
    }

    public float getAvatarHorizontalBias() {
        return eventHorizon.getAvatarHorizontalBias() + decision.horizontalBias;
    }

    public float getAvatarVerticalBias() {
        return eventHorizon.getAvatarVerticalBias() + decision.verticalBias;
    }

    public float getPulseMultiplier() {
        return eventHorizon.getPulseMultiplier() * decision.pulseMultiplier;
    }

    public float getParticleMultiplier() {
        return eventHorizon.getParticleMultiplier() * decision.particleMultiplier;
    }

    public long getFrameDelayMillis(boolean mainDisplay) {
        long base = eventHorizon.getFrameDelayMillis(mainDisplay);
        float pacing = 1.15f - decision.energy * 0.30f;
        long adjusted = Math.round(base * pacing);
        return Math.max(16L, Math.min(50L, adjusted));
    }

    public String getDecisionSource() {
        return decision.source;
    }
}
