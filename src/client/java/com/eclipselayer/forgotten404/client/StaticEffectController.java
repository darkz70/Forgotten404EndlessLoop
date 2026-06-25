package com.eclipselayer.forgotten404.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Контроллер эффекта статики экрана.
 * Флаги читаются ClientMixin при рендеринге.
 */
@Environment(EnvType.CLIENT)
public class StaticEffectController {

    private static boolean staticActive = false;
    private static long staticEndMs     = 0;
    private static float staticIntensity = 0f;

    /**
     * Запускает эффект статики на 200–600 мс.
     */
    public static void triggerStatic(int stage) {
        int durationMs = 200 + (int)(Math.random() * 400);
        staticEndMs = System.currentTimeMillis() + durationMs;
        staticActive = true;
        staticIntensity = 0.3f + (stage / 10f) * 0.7f; // интенсивность растёт с этапом
    }

    public static boolean isActive() {
        if (staticActive && System.currentTimeMillis() > staticEndMs) {
            staticActive = false;
        }
        return staticActive;
    }

    public static float getIntensity() { return staticIntensity; }
}
