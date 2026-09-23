package pro.galaxyai.fold7.ai;

import java.util.Locale;

/**
 * v33 Russian-first communication contract for the on-device Galaxy AI personality.
 *
 * The phone speaks Russian by default and advertises the same locale to AIDI Gateway.
 * This class is Android-free so the language contract can be verified in CI with javac.
 */
public final class RussianAIPersonalityV33 {
    public static final String LANGUAGE = "ru";
    public static final String LOCALE_TAG = "ru-RU";
    public static final String CULTURE = "russian";
    public static final String RESPONSE_STYLE = "friendly";

    private RussianAIPersonalityV33() {}

    public static Locale locale() {
        return new Locale("ru", "RU");
    }

    public static String normalizeEmotion(String value) {
        String v = value == null ? "calm" : value.trim().toLowerCase(Locale.ROOT);
        if ("aware".equals(v)) return "thinking";
        if ("resting".equals(v)) return "sleep";
        if ("calm".equals(v) || "focused".equals(v) || "thinking".equals(v)
                || "happy".equals(v) || "sleep".equals(v)) return v;
        return "calm";
    }

    public static String phraseFor(String emotion, int level) {
        String normalized = normalizeEmotion(emotion);
        int safeLevel = Math.max(0, Math.min(2, level));

        if ("focused".equals(normalized)) {
            return safeLevel == 0 ? "Фокусируюсь." : "Я рядом и держу фокус вместе с тобой.";
        }
        if ("thinking".equals(normalized)) {
            return safeLevel == 2 ? "Вселенная размышляет вместе с тобой." : "Размышляю.";
        }
        if ("happy".equals(normalized)) {
            return safeLevel == 2 ? "Сегодня энергия особенно яркая." : "Яркая энергия.";
        }
        if ("sleep".equals(normalized)) {
            return safeLevel == 2
                    ? "Я останусь рядом и буду тихо, пока вселенная отдыхает."
                    : "Тихий режим.";
        }
        return safeLevel == 2 ? "Я здесь. Всё спокойно." : "Я рядом.";
    }

    public static String reactionLevelName(int level) {
        if (level >= 2) return "выразительный";
        if (level == 1) return "обычный";
        return "тихий";
    }
}
