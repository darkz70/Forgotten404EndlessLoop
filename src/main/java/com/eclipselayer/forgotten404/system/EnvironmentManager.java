package com.eclipselayer.forgotten404.system;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Определяет окружение запуска мода.
 *
 * SINGLEPLAYER — одиночная игра (встроенный сервер)
 * MULTIPLAYER  — выделенный сервер с игроками
 *
 * Используется для переключения логики:
 * - Одиночная: клиентские эффекты + локальный сервер
 * - Мультиплеер: серверные события для всех игроков
 */
public class EnvironmentManager {

    public enum Mode { SINGLEPLAYER, MULTIPLAYER }

    private static Mode currentMode = null;

    /**
     * Вызывается при старте мира.
     * На выделенном сервере EnvType всегда SERVER.
     * На клиенте определяем через количество игроков и тип сервера.
     */
    public static void detect(net.minecraft.server.MinecraftServer server) {
        boolean isDedicatedServer = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;

        if (isDedicatedServer) {
            currentMode = Mode.MULTIPLAYER;
        } else {
            // Встроенный сервер (одиночная / LAN)
            // Если онлайн > 1 или это LAN — считаем мультиплеером
            boolean isLan = server.isPublished();
            int playerCount = server.getPlayerList().getPlayerCount();

            currentMode = (isLan || playerCount > 1) ? Mode.MULTIPLAYER : Mode.SINGLEPLAYER;
        }
    }

    /**
     * Переопределяем при изменении числа игроков.
     * Вызывается при каждом JOIN/LEAVE.
     */
    public static void redetect(net.minecraft.server.MinecraftServer server) {
        boolean isDedicatedServer = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
        if (isDedicatedServer) {
            currentMode = Mode.MULTIPLAYER;
            return;
        }
        int playerCount = server.getPlayerList().getPlayerCount();
        boolean isLan = server.isPublished();
        currentMode = (isLan || playerCount > 1) ? Mode.MULTIPLAYER : Mode.SINGLEPLAYER;
    }

    public static boolean isSingleplayer() {
        return currentMode == Mode.SINGLEPLAYER;
    }

    public static boolean isMultiplayer() {
        return currentMode == Mode.MULTIPLAYER;
    }

    public static Mode getMode() {
        return currentMode == null ? Mode.SINGLEPLAYER : currentMode;
    }
}
