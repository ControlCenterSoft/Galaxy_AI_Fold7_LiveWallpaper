package pro.galaxyai.fold7.engine;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * Privacy-safe speech articulation controller.
 *
 * v61 introduced pseudo-syllabic timing, v63 added Russian coarticulation and short
 * word-boundary micro-pauses, and v64 hardens the maskless portrait pipeline by removing
 * every legacy speech overlay renderer. This class now produces articulation state only;
 * the bundled photoreal portrait is deformed exclusively by DeformableLivePortraitV45.
 *
 * No microphone samples, audio buffers, network media, painted mouth, synthetic teeth,
 * facial mask or foreground speech glow are used.
 */
public final class SpeechFaceSyncV41 {
    private float mouthOpen;
    private float targetOpen;
    private float speechEnergy;
    private float time;
    private float articulationClock;
    private float articulationDuration = 0.12f;
    private float articulationTarget;
    private float previousArticulationTarget;
    private float phrasePauseClock;
    private int articulationIndex;
    private boolean wasSpeaking;
    private String emotion = "calm";

    public void update(boolean speaking, float activity, SceneDecision decision, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;
        if (decision != null) emotion = normalizeEmotion(decision.avatarState);

        float a = clamp(activity, 0f, 1f);
        if (speaking) {
            if (!wasSpeaking) {
                articulationClock = 0f;
                articulationTarget = 0.24f + a * 0.36f;
                previousArticulationTarget = 0f;
                phrasePauseClock = 0f;
                articulationIndex++;
            }

            phrasePauseClock = Math.max(0f, phrasePauseClock - dt);
            articulationClock -= dt;
            if (articulationClock <= 0f) {
                articulationIndex++;
                previousArticulationTarget = articulationTarget;

                // Russian coarticulation remains fully local: short closures approximate
                // natural word boundaries without inspecting speech audio or microphone data.
                int pausePeriod = 6 + (articulationIndex % 4);
                boolean wordBoundary = articulationIndex > 2 && articulationIndex % pausePeriod == 0;
                if (wordBoundary) {
                    phrasePauseClock = 0.038f + (articulationIndex % 3) * 0.012f;
                    articulationTarget = 0.035f + a * 0.055f;
                    articulationDuration = phrasePauseClock;
                    articulationClock = articulationDuration;
                } else {
                    float primary = (float) Math.sin(articulationIndex * 2.399963f + time * 0.11f);
                    float secondary = (float) Math.sin(articulationIndex * 1.173f + 1.2f);
                    float shape = clamp(0.50f + primary * 0.34f + secondary * 0.16f, 0f, 1f);
                    float floor = "focused".equals(emotion) ? 0.08f : 0.11f;
                    float range = "happy".equals(emotion) ? 0.72f : 0.64f;
                    articulationTarget = clamp(floor + a * (0.38f + range * shape), 0.06f, 0.94f);

                    float cadence = 0.50f + 0.50f
                            * (float) Math.sin(articulationIndex * 1.618034f + 0.45f);
                    articulationDuration = 0.075f + cadence * 0.095f;
                    if ("thinking".equals(emotion)) articulationDuration *= 1.06f;
                    if ("happy".equals(emotion)) articulationDuration *= 0.94f;
                    articulationClock = articulationDuration;
                }
            }

            float progress = articulationDuration <= 0f
                    ? 1f : clamp(1f - articulationClock / articulationDuration, 0f, 1f);
            float coarticulationBlend = smoothStep(clamp(progress * 1.65f, 0f, 1f));
            targetOpen = lerp(previousArticulationTarget, articulationTarget, coarticulationBlend);
            if (phrasePauseClock > 0f) targetOpen = Math.min(targetOpen, 0.10f + a * 0.05f);
            speechEnergy += (a - speechEnergy) * Math.min(1f, dt * 8.0f);
        } else {
            targetOpen = 0f;
            articulationClock = 0f;
            articulationDuration = 0.12f;
            articulationTarget = 0f;
            previousArticulationTarget = 0f;
            phrasePauseClock = 0f;
            speechEnergy += (0f - speechEnergy) * Math.min(1f, dt * 5.0f);
        }
        wasSpeaking = speaking;

        float speed;
        if (phrasePauseClock > 0f) speed = 14.6f;
        else speed = targetOpen > mouthOpen ? 13.8f : 10.2f;
        mouthOpen += (targetOpen - mouthOpen) * Math.min(1f, dt * speed);
        if (mouthOpen < 0.01f) mouthOpen = 0f;
    }

    public float getMouthOpen() {
        return mouthOpen;
    }

    public float getSpeechEnergy() {
        return speechEnergy;
    }

    private static float smoothStep(float value) {
        float v = clamp(value, 0f, 1f);
        return v * v * (3f - 2f * v);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp(t, 0f, 1f);
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
