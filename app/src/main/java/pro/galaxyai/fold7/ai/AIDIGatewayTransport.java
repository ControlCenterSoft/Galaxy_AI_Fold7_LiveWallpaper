package pro.galaxyai.fold7.ai;

/**
 * Transport boundary between the wallpaper runtime and AIDI Gateway.
 *
 * Keeping the transport behind this interface lets the renderer remain
 * independent from HTTP and enables a deterministic simulator in later
 * integration tests without changing the AI orchestration layer.
 */
public interface AIDIGatewayTransport {
    SceneDecision request(AIState state) throws Exception;
}
