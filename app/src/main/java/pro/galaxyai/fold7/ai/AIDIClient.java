package pro.galaxyai.fold7.ai;

import android.content.Context;

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
    private final AIProfileMemory profileMemory;
    private final AIPersonalizationProfileV30 personalizationProfile;

    private volatile SceneDecision decision = SceneDecision.neutral("bootstrap");
    private volatile long nextRefreshAtMs = 0L;
    private volatile String decisionSource = "bootstrap";
    private volatile String lastGatewayError = "";
    private volatile long lastGatewayLatencyMs = -1L;
    private volatile long lastGatewaySuccessAtMs = 0L;

    public AIDIClient() {
        this(new HttpAIDIGatewayTransport(), null, null);
    }

    public AIDIClient(Context context) {
        this(new HttpAIDIGatewayTransport(), new AIProfileMemory(context),
                new AIPersonalizationProfileV30(context));
    }

    public AIDIClient(AIDIGatewayTransport transport) {
        this(transport, null, null);
    }

    AIDIClient(AIDIGatewayTransport transport, AIProfileMemory profileMemory) {
        this(transport, profileMemory, null);
    }

    AIDIClient(AIDIGatewayTransport transport, AIProfileMemory profileMemory,
               AIPersonalizationProfileV30 personalizationProfile) {
        if (transport == null) throw new IllegalArgumentException("transport == null");
        this.transport = transport;
        this.profileMemory = profileMemory;
        this.personalizationProfile = personalizationProfile;
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

    public boolean needsRefresh() {
        return System.currentTimeMillis() >= nextRefreshAtMs && !requestInFlight.get();
    }

    public void requestIfNeeded(final AIState state) {
        if (state == null) return;
        long now = System.currentTimeMillis();
        if (now < nextRefreshAtMs || !requestInFlight.compareAndSet(false, true)) return;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                long startedAt = System.currentTimeMillis();
                AIState requestState = state;
                if (profileMemory != null) {
                    requestState = requestState.withProfile(profileMemory.snapshot());
                }
                if (personalizationProfile != null) {
                    requestState = requestState.withPersonalization(personalizationProfile.snapshot());
                }
                try {
                    SceneDecision remote = transport.request(requestState);
                    decision = remote;
                    decisionSource = "aidi_gateway";
                    lastGatewayError = "";
                    lastGatewayLatencyMs = Math.max(0L, System.currentTimeMillis() - startedAt);
                    lastGatewaySuccessAtMs = System.currentTimeMillis();
                    if (profileMemory != null) profileMemory.record(remote, true);
                    nextRefreshAtMs = System.currentTimeMillis() + clampRemoteTtl(remote.ttlSeconds);
                } catch (Exception error) {
                    SceneDecision local = fallbackAI.decide(requestState);
                    decision = local;
                    decisionSource = "local_fallback";
                    lastGatewayError = error.getClass().getSimpleName();
                    lastGatewayLatencyMs = Math.max(0L, System.currentTimeMillis() - startedAt);
                    if (profileMemory != null) profileMemory.record(local, false);
                    nextRefreshAtMs = System.currentTimeMillis() + fallbackRetryDelay(local.ttlSeconds);
                } finally {
                    requestInFlight.set(false);
                }
            }
        });
    }

    private static long clampRemoteTtl(long ttlSeconds) {
        long requested = ttlSeconds > 0
                ? ttlSeconds * 1000L
                : AIDIConfig.DEFAULT_REFRESH_MS;
        return Math.max(AIDIConfig.MIN_REFRESH_MS,
                Math.min(AIDIConfig.MAX_REFRESH_MS, requested));
    }

    private static long fallbackRetryDelay(long ttlSeconds) {
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
