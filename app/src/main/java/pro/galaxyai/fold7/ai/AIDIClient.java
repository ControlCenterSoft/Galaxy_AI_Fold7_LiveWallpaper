package pro.galaxyai.fold7.ai;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runtime coordinator for AIDI decisions.
 *
 * Networking is isolated behind AIDIGatewayTransport, so the wallpaper engine
 * never blocks on gateway I/O and can transparently fall back to local logic.
 */
public final class AIDIClient {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean requestInFlight = new AtomicBoolean(false);
    private final LocalFallbackAI fallbackAI = new LocalFallbackAI();
    private final GatewayRecoveryPolicy recoveryPolicy = new GatewayRecoveryPolicy();
    private final AIDIGatewayTransport transport;

    private volatile SceneDecision decision = SceneDecision.neutral("bootstrap");
    private volatile long nextRefreshAtMs = 0L;
    private volatile String decisionSource = "bootstrap";
    private volatile String lastGatewayError = "";
    private volatile long lastGatewayLatencyMs = -1L;
    private volatile long lastGatewaySuccessAtMs = 0L;
    private volatile long fallbackSinceAtMs = 0L;

    public AIDIClient() {
        this(new HttpAIDIGatewayTransport());
    }

    public AIDIClient(AIDIGatewayTransport transport) {
        if (transport == null) throw new IllegalArgumentException("transport == null");
        this.transport = transport;
    }

    public SceneDecision getDecision() {
        return decision;
    }

    public String getDecisionSource() {
        return decisionSource;
    }

    public String getLastGatewayError() {
        return lastGatewayError;
    }

    public long getLastGatewayLatencyMs() {
        return lastGatewayLatencyMs;
    }

    public long getLastGatewaySuccessAtMs() {
        return lastGatewaySuccessAtMs;
    }

    public long getFallbackSinceAtMs() {
        return fallbackSinceAtMs;
    }

    public int getConsecutiveGatewayFailures() {
        return recoveryPolicy.getConsecutiveFailures();
    }

    public boolean isInFallbackMode() {
        return "local_fallback".equals(decisionSource);
    }

    /**
     * Lets the renderer avoid collecting battery/display state on every frame.
     * requestIfNeeded() performs the same check again to keep the transition race-safe.
     */
    public boolean needsRefresh() {
        return System.currentTimeMillis() >= nextRefreshAtMs && !requestInFlight.get();
    }

    public void requestIfNeeded(final AIState state) {
        if (state == null) return;
        long now = System.currentTimeMillis();
        if (now < nextRefreshAtMs || !requestInFlight.compareAndSet(false, true)) return;

        try {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    performRequest(state);
                }
            });
        } catch (RejectedExecutionException ignored) {
            requestInFlight.set(false);
        }
    }

    private void performRequest(AIState state) {
        long startedAt = System.currentTimeMillis();
        try {
            SceneDecision remote = transport.request(state);
            if (remote == null) throw new IllegalStateException("Gateway returned null decision");

            decision = remote;
            decisionSource = "aidi_gateway";
            lastGatewayError = "";
            lastGatewayLatencyMs = Math.max(0L, System.currentTimeMillis() - startedAt);
            lastGatewaySuccessAtMs = System.currentTimeMillis();
            fallbackSinceAtMs = 0L;
            recoveryPolicy.onGatewaySuccess();
            nextRefreshAtMs = System.currentTimeMillis() + clampRemoteTtl(remote.ttlSeconds);
        } catch (Exception error) {
            SceneDecision local = fallbackAI.decide(state);
            decision = local;
            decisionSource = "local_fallback";
            lastGatewayError = error.getClass().getSimpleName();
            lastGatewayLatencyMs = Math.max(0L, System.currentTimeMillis() - startedAt);
            if (fallbackSinceAtMs == 0L) fallbackSinceAtMs = System.currentTimeMillis();
            nextRefreshAtMs = System.currentTimeMillis()
                    + recoveryPolicy.onGatewayFailure(local.ttlSeconds);
        } finally {
            requestInFlight.set(false);
        }
    }

    private static long clampRemoteTtl(long ttlSeconds) {
        long requested = ttlSeconds > 0
                ? ttlSeconds * 1000L
                : AIDIConfig.DEFAULT_REFRESH_MS;
        return Math.max(AIDIConfig.MIN_REFRESH_MS,
                Math.min(AIDIConfig.MAX_REFRESH_MS, requested));
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
