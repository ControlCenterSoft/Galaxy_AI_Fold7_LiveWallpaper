package pro.galaxyai.fold7.ai;

import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runtime coordinator for AIDI decisions with bounded on-device profile memory.
 * Network failures never block rendering: LocalFallbackAI remains active while
 * GatewayRecoveryPolicy schedules progressively paced background retries.
 */
public final class AIDIClient {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean requestInFlight = new AtomicBoolean(false);
    private final LocalFallbackAI fallbackAI = new LocalFallbackAI();
    private final GatewayRecoveryPolicy recoveryPolicy = new GatewayRecoveryPolicy();
    private final AIDIGatewayTransport transport;
    private final AIProfileMemory profileMemory;

    private volatile SceneDecision decision = SceneDecision.neutral("bootstrap");
    private volatile long nextRefreshAtMs = 0L;
    private volatile String decisionSource = "bootstrap";
    private volatile String lastGatewayError = "";
    private volatile long lastGatewayLatencyMs = -1L;
    private volatile long lastGatewaySuccessAtMs = 0L;
    private volatile long fallbackSinceAtMs = 0L;

    public AIDIClient() {
        this(new HttpAIDIGatewayTransport(), null);
    }

    public AIDIClient(Context context) {
        this(new HttpAIDIGatewayTransport(), new AIProfileMemory(context));
    }

    public AIDIClient(Context context, String endpoint) {
        this(new HttpAIDIGatewayTransport(endpoint), new AIProfileMemory(context));
    }

    public AIDIClient(AIDIGatewayTransport transport) {
        this(transport, null);
    }

    AIDIClient(AIDIGatewayTransport transport, AIProfileMemory profileMemory) {
        if (transport == null) throw new IllegalArgumentException("transport == null");
        this.transport = transport;
        this.profileMemory = profileMemory;
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
        AIState requestState = state;
        if (profileMemory != null) {
            requestState = state.withProfile(profileMemory.snapshot());
        }

        try {
            SceneDecision remote = transport.request(requestState);
            if (remote == null) throw new IllegalStateException("Gateway returned null decision");

            decision = remote;
            decisionSource = "aidi_gateway";
            lastGatewayError = "";
            lastGatewayLatencyMs = Math.max(0L, System.currentTimeMillis() - startedAt);
            lastGatewaySuccessAtMs = System.currentTimeMillis();
            fallbackSinceAtMs = 0L;
            recoveryPolicy.onGatewaySuccess();
            if (profileMemory != null) profileMemory.record(remote, true);
            nextRefreshAtMs = System.currentTimeMillis() + clampRemoteTtl(remote.ttlSeconds);
        } catch (Exception error) {
            SceneDecision local = fallbackAI.decide(requestState);
            decision = local;
            decisionSource = "local_fallback";
            lastGatewayError = error.getClass().getSimpleName();
            lastGatewayLatencyMs = Math.max(0L, System.currentTimeMillis() - startedAt);
            if (fallbackSinceAtMs == 0L) fallbackSinceAtMs = System.currentTimeMillis();
            if (profileMemory != null) profileMemory.record(local, false);
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
