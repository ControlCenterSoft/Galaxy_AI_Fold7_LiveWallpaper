package pro.galaxyai.fold7.ai;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final AIDIGatewayTransport transport;

    private volatile SceneDecision decision = SceneDecision.neutral("bootstrap");
    private volatile long nextRefreshAtMs = 0L;
    private volatile String decisionSource = "bootstrap";
    private volatile String lastGatewayError = "";

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

    public void requestIfNeeded(final AIState state) {
        long now = System.currentTimeMillis();
        if (now < nextRefreshAtMs || !requestInFlight.compareAndSet(false, true)) return;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SceneDecision remote = transport.request(state);
                    decision = remote;
                    decisionSource = "aidi_gateway";
                    lastGatewayError = "";
                    nextRefreshAtMs = System.currentTimeMillis() + clampRemoteTtl(remote.ttlSeconds);
                } catch (Exception error) {
                    SceneDecision local = fallbackAI.decide(state);
                    decision = local;
                    decisionSource = "local_fallback";
                    lastGatewayError = error.getClass().getSimpleName();
                    nextRefreshAtMs = System.currentTimeMillis() + fallbackRetryDelay(local.ttlSeconds);
                } finally {
                    requestInFlight.set(false);
                }
            }
        });
    }

    private static long clampRemoteTtl(int ttlSeconds) {
        long requested = ttlSeconds > 0
                ? ttlSeconds * 1000L
                : AIDIConfig.DEFAULT_REFRESH_MS;
        return Math.max(AIDIConfig.MIN_REFRESH_MS,
                Math.min(AIDIConfig.MAX_REFRESH_MS, requested));
    }

    private static long fallbackRetryDelay(int ttlSeconds) {
        long localTtl = ttlSeconds > 0
                ? ttlSeconds * 1000L
                : AIDIConfig.FAILURE_BACKOFF_MS;
        long retry = Math.min(AIDIConfig.FAILURE_BACKOFF_MS, localTtl);
        return Math.max(AIDIConfig.MIN_REFRESH_MS, retry);
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
