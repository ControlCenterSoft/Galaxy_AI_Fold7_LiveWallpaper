package pro.galaxyai.fold7.ai;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GatewayRecoveryPolicyTest {
    @Test
    public void backoffProgressesAndCapsThenResets() {
        GatewayRecoveryPolicy policy = new GatewayRecoveryPolicy();

        assertEquals(60_000L, policy.onGatewayFailure(900));
        assertEquals(120_000L, policy.onGatewayFailure(900));
        assertEquals(240_000L, policy.onGatewayFailure(900));
        assertEquals(480_000L, policy.onGatewayFailure(900));
        assertEquals(900_000L, policy.onGatewayFailure(900));
        assertEquals(900_000L, policy.onGatewayFailure(3600));
        assertEquals(6, policy.getConsecutiveFailures());

        policy.onGatewaySuccess();
        assertEquals(0, policy.getConsecutiveFailures());
        assertEquals(60_000L, policy.onGatewayFailure(900));
    }

    @Test
    public void localTtlCanShortenRetryButNeverBelowMinimum() {
        GatewayRecoveryPolicy policy = new GatewayRecoveryPolicy();
        assertEquals(AIDIConfig.MIN_REFRESH_MS, policy.onGatewayFailure(5));
    }
}
