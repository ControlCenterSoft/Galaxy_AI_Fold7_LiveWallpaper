package pro.galaxyai.fold7.ai;

public final class AIDIConfig {
    public static final String DEFAULT_ENDPOINT =
            "https://aidi-gateway.hm.dm/api/v1/scene/analyze";
    public static final int CONNECT_TIMEOUT_MS = 1800;
    public static final int READ_TIMEOUT_MS = 2500;
    public static final long MIN_REFRESH_MS = 60_000L;
    public static final long DEFAULT_REFRESH_MS = 900_000L;

    private AIDIConfig() {
    }
}
