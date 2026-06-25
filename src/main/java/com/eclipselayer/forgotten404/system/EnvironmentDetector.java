package com.eclipselayer.forgotten404.system;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Определяет тип окружения — одиночная игра или выделенный сервер.
 *
 * Логика:
 * - EnvType.CLIENT + интегрированный сервер = одиночная игра / LAN
 * - EnvType.SERVER = выделенный сервер с игроками
 *
 * Мод адаптирует поведение:
 * - Одиночная: клиентские эффекты (оверлеи, звуки, глитчи экрана)
 * - Сервер: серверные эффекты (сообщения всем, телепорт, структуры)
 */
public class EnvironmentDetector {

    public enum GameMode {
        SINGLEPLAYER,   // Одиночная / LAN хост
        DEDICATED_SERVER // Выделенный сервер
    }

    private static GameMode cachedMode = null;

    public static GameMode getMode() {
        if (cachedMode != null) return cachedMode;

        EnvType env = FabricLoader.getInstance().getEnvironmentType();
        cachedMode = (env == EnvType.SERVER)
            ? GameMode.DEDICATED_SERVER
            : GameMode.SINGLEPLAYER;

        return cachedMode;
    }

    public static boolean isSingleplayer() {
        return getMode() == GameMode.SINGLEPLAYER;
    }

    public static boolean isDedicatedServer() {
        return getMode() == GameMode.DEDICATED_SERVER;
    }
}
