package pro.galaxyai.fold7.ai;

import java.net.URI;

/**
 * Transport boundary for AIDI Gateway endpoints.
 *
 * HTTPS is accepted for production endpoints. Plain HTTP is accepted only for
 * loopback/private LAN hosts used by the local AIDI fabric, so a configuration
 * mistake cannot silently send device context to a public clear-text endpoint.
 */
public final class AIDIEndpointPolicy {
    private static final String SCENE_PATH = "/api/v1/scene/analyze";

    private AIDIEndpointPolicy() {
    }

    public static String requireAllowed(String endpoint) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("AIDI endpoint is empty");
        }
        final URI uri;
        try {
            uri = URI.create(endpoint.trim());
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Invalid AIDI endpoint", error);
        }

        String scheme = lower(uri.getScheme());
        String host = lower(uri.getHost());
        if (host.isEmpty()) {
            throw new IllegalArgumentException("AIDI endpoint host is empty");
        }
        if (uri.getUserInfo() != null || uri.getFragment() != null || uri.getQuery() != null) {
            throw new IllegalArgumentException("AIDI endpoint contains forbidden URL components");
        }
        if (!SCENE_PATH.equals(uri.getPath())) {
            throw new IllegalArgumentException("AIDI endpoint path must be " + SCENE_PATH);
        }

        if ("https".equals(scheme)) {
            return uri.toString();
        }
        if ("http".equals(scheme) && isPrivateHost(host)) {
            return uri.toString();
        }
        throw new IllegalArgumentException("Clear-text AIDI endpoint must stay on private LAN");
    }

    static boolean isPrivateHost(String host) {
        if (host == null) return false;
        host = lower(host);
        if ("localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host)) {
            return true;
        }
        if (host.endsWith(".hm.dm") || "hm.dm".equals(host)) {
            return true;
        }

        String[] parts = host.split("\\.");
        if (parts.length != 4) return false;
        int[] octets = new int[4];
        try {
            for (int i = 0; i < 4; i++) {
                octets[i] = Integer.parseInt(parts[i]);
                if (octets[i] < 0 || octets[i] > 255) return false;
            }
        } catch (NumberFormatException error) {
            return false;
        }

        if (octets[0] == 10) return true;
        if (octets[0] == 192 && octets[1] == 168) return true;
        return octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31;
    }

    private static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.US);
    }
}
