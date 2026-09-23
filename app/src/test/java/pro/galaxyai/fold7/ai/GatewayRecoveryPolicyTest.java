package pro.galaxyai.fold7.ai;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GatewayRecoveryPolicyTest {
    @Test
    public void exponentialBackoffIsBoundedAndResetsAfterSuccess() {
        GatewayRecoveryPolicy policy = new GatewayRecoveryPolicy();

        assertEquals(60_000L, policy.onGatewayFailure(3600L));
        assertEquals(120_000L, policy.onGatewayFailure(3600L));
        assertEquals(240_000L, policy.onGatewayFailure(3600L));
        assertEquals(480_000L, policy.onGatewayFailure(3600L));
        assertEquals(900_000L, policy.onGatewayFailure(3600L));
        assertEquals(900_000L, policy.onGatewayFailure(3600L));
        assertEquals(6, policy.getConsecutiveFailures());

        policy.onGatewaySuccess();
        assertEquals(0, policy.getConsecutiveFailures());
        assertEquals(60_000L, policy.onGatewayFailure(3600L));
    }

    @Test
    public void localDecisionTtlCanShortenButNeverBreakMinimumRefresh() {
        GatewayRecoveryPolicy policy = new GatewayRecoveryPolicy();
        assertEquals(60_000L, policy.onGatewayFailure(30L));
        assertEquals(60_000L, policy.onGatewayFailure(60L));
    }
}
