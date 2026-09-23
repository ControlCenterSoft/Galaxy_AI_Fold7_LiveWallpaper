package pro.galaxyai.fold7.ai;

/**
 * Small deterministic state machine for Gateway retry pacing.
 *
 * Rendering never waits on this policy: failures switch immediately to local
 * decisions, while retries happen on the existing background AIDI executor.
 */
final class GatewayRecoveryPolicy {
    private int consecutiveFailures;

    synchronized long onGatewayFailure(long localTtlSeconds) {
        if (consecutiveFailures < 16) consecutiveFailures++;

        int exponent = Math.min(4, Math.max(0, consecutiveFailures - 1));
        long retry = AIDIConfig.FAILURE_BACKOFF_BASE_MS * (1L << exponent);
        retry = Math.min(retry, AIDIConfig.FAILURE_BACKOFF_MAX_MS);

        long localTtlMs = localTtlSeconds > 0
                ? localTtlSeconds * 1000L
                : retry;
        retry = Math.min(retry, localTtlMs);
        return Math.max(AIDIConfig.MIN_REFRESH_MS, retry);
    }

    synchronized void onGatewaySuccess() {
        consecutiveFailures = 0;
    }

    synchronized int getConsecutiveFailures() {
        return consecutiveFailures;
    }
}
