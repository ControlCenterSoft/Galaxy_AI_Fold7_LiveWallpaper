package pro.galaxyai.fold7.engine;

import org.junit.Test;

import pro.galaxyai.fold7.ai.AIState;
import pro.galaxyai.fold7.ai.LocalFallbackAI;
import pro.galaxyai.fold7.ai.SceneDecision;

import static org.junit.Assert.assertTrue;

public class LivingUniverseControllerV20StabilityTest {
    @Test
    public void offlineFallbackDecisionsRemainStableAcrossLongRenderRun() {
        LivingUniverseControllerV20 universe = new LivingUniverseControllerV20(
                0x51A7C0DEL, 42L);
        LocalFallbackAI fallback = new LocalFallbackAI();

        AIState[] states = new AIState[] {
                new AIState(5, false, true, 2, 0.05f),
                new AIState(80, false, true, 23, 0.10f),
                new AIState(80, true, true, 14, 0.90f),
                new AIState(80, false, true, 14, 0.70f),
                new AIState(80, false, false, 14, 0.15f)
        };

        for (AIState state : states) {
            SceneDecision decision = fallback.decide(state);
            universe.setDecision(decision);
            for (int frame = 0; frame < 5000; frame++) {
                boolean main = (frame & 127) < 96 ? state.mainDisplay : !state.mainDisplay;
                universe.update(1f / 60f, main);

                assertFiniteAndBounded(universe.getSceneScale(), 0.5f, 1.5f);
                assertFiniteAndBounded(universe.getAvatarHorizontalBias(), -0.2f, 0.2f);
                assertFiniteAndBounded(universe.getAvatarVerticalBias(), -0.2f, 0.2f);
                assertFiniteAndBounded(universe.getPulseMultiplier(), 0.2f, 3.0f);
                assertFiniteAndBounded(universe.getParticleMultiplier(), 0.2f, 3.0f);
                long delay = universe.getFrameDelayMillis(main);
                assertTrue(delay >= 16L && delay <= 50L);
            }
        }
    }

    private static void assertFiniteAndBounded(float value, float min, float max) {
        assertTrue(Float.isFinite(value));
        assertTrue(value >= min);
        assertTrue(value <= max);
    }
}
