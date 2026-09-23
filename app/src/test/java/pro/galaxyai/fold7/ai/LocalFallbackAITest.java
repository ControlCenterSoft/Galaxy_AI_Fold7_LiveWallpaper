package pro.galaxyai.fold7.ai;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LocalFallbackAITest {
    private final LocalFallbackAI fallback = new LocalFallbackAI();

    @Test
    public void selectsDeterministicOfflineScenes() {
        assertEquals("deep_black", fallback.decide(state(5, false, true, 12, 0.1f)).sceneName);
        assertEquals("deep_black", fallback.decide(state(12, false, true, 12, 0.1f)).sceneName);
        assertEquals("deep_nebula", fallback.decide(state(80, false, true, 23, 0.1f)).sceneName);
        assertEquals("energy_core", fallback.decide(state(80, true, true, 14, 0.8f)).sceneName);
        assertEquals("living_continuum", fallback.decide(state(80, false, true, 14, 0.8f)).sceneName);
        assertEquals("living_continuum", fallback.decide(state(80, false, true, 14, 0.2f)).sceneName);
        assertEquals("compact_continuum", fallback.decide(state(80, false, false, 14, 0.2f)).sceneName);
    }

    @Test
    public void decisionsRemainInsideRendererSafetyEnvelope() {
        AIState[] states = new AIState[] {
                state(5, false, true, 2, 0f),
                state(12, false, false, 18, 0.4f),
                state(90, false, true, 23, 0.1f),
                state(90, true, true, 14, 1f),
                state(90, false, false, 14, 0.2f)
        };

        for (AIState state : states) {
            SceneDecision d = fallback.decide(state);
            assertEquals("local-fallback", d.source);
            assertTrue(d.energy >= 0f && d.energy <= 1f);
            assertTrue(d.particleMultiplier >= 0.45f && d.particleMultiplier <= 1.8f);
            assertTrue(d.pulseMultiplier >= 0.6f && d.pulseMultiplier <= 1.5f);
            assertTrue(d.sceneScaleMultiplier >= 0.97f && d.sceneScaleMultiplier <= 1.03f);
            assertTrue(d.ttlSeconds >= 60L && d.ttlSeconds <= 3600L);
        }
    }

    @Test
    public void repeatedOfflineInputProducesSameDecision() {
        AIState state = state(73, false, true, 21, 0.42f);
        SceneDecision first = fallback.decide(state);
        for (int i = 0; i < 100; i++) {
            SceneDecision next = fallback.decide(state);
            assertEquals(first.sceneName, next.sceneName);
            assertEquals(first.avatarState, next.avatarState);
            assertEquals(first.energy, next.energy, 0f);
            assertEquals(first.particleMultiplier, next.particleMultiplier, 0f);
            assertEquals(first.ttlSeconds, next.ttlSeconds);
        }
    }

    private static AIState state(int battery, boolean charging, boolean main,
                                 int hour, float motion) {
        return new AIState(battery, charging, main, hour, motion,
                main ? 1812 : 968, main ? 2176 : 2376, 1L, 1L);
    }
}
