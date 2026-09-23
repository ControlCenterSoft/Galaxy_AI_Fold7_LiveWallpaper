package pro.galaxyai.fold7.ai;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AIDIClient {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean requestInFlight = new AtomicBoolean(false);
    private final LocalFallbackAI fallbackAI = new LocalFallbackAI();
    private volatile SceneDecision decision = SceneDecision.neutral("bootstrap");
    private volatile long nextRefreshAtMs = 0L;

    public SceneDecision getDecision() {
        return decision;
    }

    public void requestIfNeeded(final AIState state) {
        long now = System.currentTimeMillis();
        if (now < nextRefreshAtMs || !requestInFlight.compareAndSet(false, true)) return;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SceneDecision remote = requestRemote(state);
                    decision = remote;
                    nextRefreshAtMs = System.currentTimeMillis()
                            + Math.max(AIDIConfig.MIN_REFRESH_MS, remote.ttlSeconds * 1000L);
                } catch (Exception ignored) {
                    SceneDecision local = fallbackAI.decide(state);
                    decision = local;
                    nextRefreshAtMs = System.currentTimeMillis()
                            + Math.max(AIDIConfig.MIN_REFRESH_MS, local.ttlSeconds * 1000L);
                } finally {
                    requestInFlight.set(false);
                }
            }
        });
    }

    private SceneDecision requestRemote(AIState state) throws Exception {
        URL url = new URL(AIDIConfig.DEFAULT_ENDPOINT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(AIDIConfig.CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(AIDIConfig.READ_TIMEOUT_MS);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("X-AIDI-Protocol", "scene-v1");

        byte[] body = state.toJson().toString().getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(body.length);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(body);
        }

        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            connection.disconnect();
            throw new IllegalStateException("AIDI HTTP " + code);
        }

        StringBuilder response = new StringBuilder();
        try (InputStream stream = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        } finally {
            connection.disconnect();
        }

        JSONObject root = new JSONObject(response.toString());
        return SceneDecision.fromGateway(root);
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
