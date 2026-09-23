package pro.galaxyai.fold7.ai;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AIDIClientOfflineTest {
    @Test
    public void gatewayFailureSwitchesToLocalFallbackWithoutBlockingRenderer() throws Exception {
        AIDIGatewayTransport offline = new AIDIGatewayTransport() {
            @Override
            public SceneDecision request(AIState state) throws Exception {
                throw new IOException("offline");
            }
        };

        AIDIClient client = new AIDIClient(offline);
        try {
            AIState state = new AIState(82, false, true, 23, 0.1f,
                    1812, 2176, 1L, 1L);
            long started = System.currentTimeMillis();
            client.requestIfNeeded(state);

            while (!client.isInFallbackMode()
                    && System.currentTimeMillis() - started < 1500L) {
                Thread.sleep(10L);
            }

            assertTrue(client.isInFallbackMode());
            assertEquals("local_fallback", client.getDecisionSource());
            assertEquals("deep_nebula", client.getDecision().sceneName);
            assertEquals("IOException", client.getLastGatewayError());
            assertEquals(1, client.getConsecutiveGatewayFailures());
            assertTrue(client.getFallbackSinceAtMs() > 0L);
        } finally {
            client.shutdown();
        }
    }
}
