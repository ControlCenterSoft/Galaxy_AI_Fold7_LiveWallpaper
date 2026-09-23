package pro.galaxyai.fold7.ai;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** HTTP implementation of the AIDI scene-v1 transport. */
public final class HttpAIDIGatewayTransport implements AIDIGatewayTransport {
    private final String endpoint;

    public HttpAIDIGatewayTransport() {
        this(AIDIConfig.DEFAULT_ENDPOINT);
    }

    public HttpAIDIGatewayTransport(String endpoint) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("AIDI endpoint is empty");
        }
        this.endpoint = endpoint;
    }

    @Override
    public SceneDecision request(AIState state) throws Exception {
        if (state == null) throw new IllegalArgumentException("state == null");

        String requestId = AIDIConfig.CLIENT_NAME + "-"
                + state.sequence + "-" + Long.toHexString(state.capturedAtMs);
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setConnectTimeout(AIDIConfig.CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(AIDIConfig.READ_TIMEOUT_MS);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Cache-Control", "no-store");
            connection.setRequestProperty("X-AIDI-Protocol", AIDIConfig.PROTOCOL_VERSION);
            connection.setRequestProperty("X-AIDI-Client", AIDIConfig.CLIENT_NAME);
            connection.setRequestProperty("X-AIDI-Request-Id", requestId);

            JSONObject payload = state.toJson();
            payload.put("request_id", requestId);
            byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
            if (body.length > AIDIConfig.MAX_REQUEST_BYTES) {
                throw new IllegalStateException("AIDI request exceeds limit");
            }
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("AIDI HTTP " + code);
            }

            String responseProtocol = connection.getHeaderField("X-AIDI-Protocol");
            if (responseProtocol != null && !AIDIConfig.PROTOCOL_VERSION.equals(responseProtocol)) {
                throw new IllegalStateException("AIDI protocol mismatch");
            }
            String echoedRequestId = connection.getHeaderField("X-AIDI-Request-Id");
            if (echoedRequestId != null && !requestId.equals(echoedRequestId)) {
                throw new IllegalStateException("AIDI request id mismatch");
            }

            byte[] response = readBounded(connection.getInputStream(), AIDIConfig.MAX_RESPONSE_BYTES);
            JSONObject root = new JSONObject(new String(response, StandardCharsets.UTF_8));
            return SceneDecision.fromGateway(root);
        } finally {
            connection.disconnect();
        }
    }

    private static byte[] readBounded(InputStream stream, int maxBytes) throws Exception {
        try (InputStream input = stream;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[2048];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new IllegalStateException("AIDI response exceeds limit");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }
}
