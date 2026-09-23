package pro.galaxyai.fold7.ai;

public final class AIDIConfig {
    // v20 production prototype uses the live LAN/VPN AIDI Gateway directly.
    // TLS + DNS hardening is tracked for the production security release.
    public static final String DEFAULT_ENDPOINT =
            "http://192.168.10.214:8088/api/v1/scene/analyze";
    public static final String PROTOCOL_VERSION = "scene-v1";
    public static final String CLIENT_NAME = "galaxy-fold7-live-wallpaper";

    public static final int CONNECT_TIMEOUT_MS = 1800;
    public static final int READ_TIMEOUT_MS = 2500;
    public static final int MAX_REQUEST_BYTES = 16 * 1024;
    public static final int MAX_RESPONSE_BYTES = 64 * 1024;

    public static final long MIN_REFRESH_MS = 60_000L;
    public static final long DEFAULT_REFRESH_MS = 900_000L;
    public static final long MAX_REFRESH_MS = 3_600_000L;
    public static final long FAILURE_BACKOFF_MS = 120_000L;

    private AIDIConfig() {
    }
}
