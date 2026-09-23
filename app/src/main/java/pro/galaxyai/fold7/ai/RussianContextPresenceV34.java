package pro.galaxyai.fold7.ai;

/**
 * v34 Russian-first contextual presence model.
 *
 * Produces short on-device Russian status lines from already available scene traits.
 * It is intentionally Android-free and does not collect microphone, camera, location,
 * raw media, account data or any other new signal.
 */
public final class RussianContextPresenceV34 {
    private RussianContextPresenceV34() {}

    public static final class Snapshot {
        public final String headline;
        public final String detail;
        public final String emotion;
        public final float accent;

        Snapshot(String headline, String detail, String emotion, float accent) {
            this.headline = headline;
            this.detail = detail;
            this.emotion = emotion;
            this.accent = clamp(accent, 0f, 1f);
        }
    }

    public static Snapshot describe(String emotion,
                                    float focus,
                                    float curiosity,
                                    float energy,
                                    int reactionLevel,
                                    long timeMillis) {
        String normalized = RussianAIPersonalityV33.normalizeEmotion(emotion);
        int level = Math.max(0, Math.min(2, reactionLevel));
        int variant = (int) Math.floorMod(timeMillis / 60_000L, 3L);

        String headline;
        String detail;
        float accent;

        switch (normalized) {
            case "focused":
                headline = variant == 0 ? "Держу фокус" : variant == 1 ? "Я рядом и сосредоточена" : "Фокус сохраняю";
                detail = focus >= 0.72f ? "Внимание высокое · ничего лишнего" : "Спокойно удерживаю внимание";
                accent = 0.72f + clamp01(focus) * 0.24f;
                break;
            case "thinking":
                headline = variant == 0 ? "Размышляю" : variant == 1 ? "Собираю контекст" : "Ищу лучший ритм";
                detail = curiosity >= 0.62f ? "Любопытство активно · анализ продолжается" : "Мягко сопоставляю сигналы";
                accent = 0.58f + clamp01(curiosity) * 0.30f;
                break;
            case "happy":
                headline = variant == 0 ? "Энергия яркая" : variant == 1 ? "Сегодня больше света" : "Яркое настроение";
                detail = energy >= 0.62f ? "Вселенная отвечает живее" : "Сохраняю тёплый спокойный ритм";
                accent = 0.68f + clamp01(energy) * 0.28f;
                break;
            case "sleep":
                headline = variant == 0 ? "Тихий режим" : variant == 1 ? "Вселенная отдыхает" : "Я остаюсь рядом";
                detail = "Минимум движения · максимум покоя";
                accent = 0.28f;
                break;
            default:
                headline = variant == 0 ? "Я рядом" : variant == 1 ? "Всё спокойно" : "Сохраняю присутствие";
                detail = level == 0 ? "Тихое присутствие" : "Живой контекст без лишних отвлечений";
                accent = 0.48f + clamp01(energy) * 0.18f;
                break;
        }

        if (level == 0 && !"sleep".equals(normalized)) {
            detail = "Тихое присутствие";
        }
        return new Snapshot(headline, detail, normalized, accent);
    }

    public static boolean isRussianText(String value) {
        if (value == null || value.isEmpty()) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'А' && c <= 'я') || c == 'Ё' || c == 'ё') return true;
        }
        return false;
    }

    private static float clamp01(float value) {
        return clamp(value, 0f, 1f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
