package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v52 Conversational Turn Taking.
 *
 * Converts local TTS speaking transitions into a bounded posture envelope so the assistant
 * does not snap into/out of speech. Speech onset gets a tiny attentive lift, sustained speech
 * settles into a stable pose, and the end of an utterance returns through a short recovery
 * phase. Only SceneDecision and local TTS state are consumed; no microphone, camera, precise
 * location, biometrics or raw media are accessed.
 */
public final class ConversationalTurnTakingV52 {
    private static final int PHASE_IDLE = 0;
    private static final int PHASE_ONSET = 1;
    private static final int PHASE_SPEAKING = 2;
    private static final int PHASE_RECOVERY = 3;

    private int phase = PHASE_IDLE;
    private boolean lastSpeaking;
    private float phaseClock;
    private float envelope;
    private float nod;
    private float lean;
    private float lift;
    private float energy = 0.45f;
    private float focus = 0.42f;
    private String emotion = "calm";

    public void update(SceneDecision decision, boolean speaking, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);

        if (decision != null) {
            float k = Math.min(1f, dt * 2.8f);
            energy += (clamp(decision.energy, 0f, 1f) - energy) * k;
            focus += (clamp(decision.focus, 0f, 1f) - focus) * k;
            emotion = normalizeEmotion(decision.avatarState);
        }

        if (speaking != lastSpeaking) {
            phaseClock = 0f;
            phase = speaking ? PHASE_ONSET : PHASE_RECOVERY;
            lastSpeaking = speaking;
        } else {
            phaseClock += dt;
        }

        if (phase == PHASE_ONSET && phaseClock >= 0.32f) {
            phase = PHASE_SPEAKING;
            phaseClock = 0f;
        } else if (phase == PHASE_RECOVERY && phaseClock >= 0.70f) {
            phase = PHASE_IDLE;
            phaseClock = 0f;
        }

        float targetEnvelope;
        float targetNod;
        float targetLean;
        float targetLift;
        if ("sleep".equals(emotion)) {
            targetEnvelope = 0.05f;
            targetNod = 0f;
            targetLean = 0f;
            targetLift = 0f;
        } else if (phase == PHASE_ONSET) {
            float p = clamp(phaseClock / 0.32f, 0f, 1f);
            targetEnvelope = smoothstep(p);
            targetNod = (float) Math.sin(p * Math.PI) * 0.75f;
            targetLean = 0.45f + focus * 0.25f;
            targetLift = 0.75f;
        } else if (phase == PHASE_SPEAKING) {
            targetEnvelope = 1f;
            targetNod = (float) Math.sin(phaseClock * 3.1f) * (0.16f + energy * 0.10f);
            targetLean = 0.38f + focus * 0.22f;
            targetLift = 0.42f;
        } else if (phase == PHASE_RECOVERY) {
            float p = clamp(phaseClock / 0.70f, 0f, 1f);
            float remaining = 1f - smoothstep(p);
            targetEnvelope = remaining;
            targetNod = -(float) Math.sin(p * Math.PI) * 0.25f;
            targetLean = remaining * 0.28f;
            targetLift = remaining * 0.32f;
        } else {
            targetEnvelope = 0f;
            targetNod = 0f;
            targetLean = 0f;
            targetLift = 0f;
        }

        if ("focused".equals(emotion)) {
            targetNod *= 0.58f;
            targetLean *= 0.70f;
        }

        float smooth = 1f - (float) Math.exp(-5.2f * dt);
        envelope += (targetEnvelope - envelope) * smooth;
        nod += (targetNod - nod) * smooth;
        lean += (targetLean - lean) * smooth;
        lift += (targetLift - lift) * smooth;
    }

    public float getEnvelope() {
        return clamp(envelope, 0f, 1f);
    }

    public float getNod() {
        return clamp(nod, -1f, 1f);
    }

    public float getLean() {
        return clamp(lean, -1f, 1f);
    }

    public float getLift() {
        return clamp(lift, -1f, 1f);
    }

    public boolean isConversationalMotionActive() {
        return phase != PHASE_IDLE || envelope > 0.02f;
    }

    private static float smoothstep(float p) {
        float x = clamp(p, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    private static String normalizeEmotion(String value) {
        String v = value == null ? "calm" : value.trim().toLowerCase();
        if ("aware".equals(v)) return "thinking";
        if ("resting".equals(v)) return "sleep";
        if ("calm".equals(v) || "focused".equals(v) || "thinking".equals(v)
                || "happy".equals(v) || "sleep".equals(v)) return v;
        return "calm";
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
