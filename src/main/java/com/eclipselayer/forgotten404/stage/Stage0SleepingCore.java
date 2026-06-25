package com.eclipselayer.forgotten404.stage;

import com.eclipselayer.forgotten404.Forgotten404EndlessLoop;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Stage 0 — SLEEPING CORE
 *
 * Система загружается. Игрок не знает что происходит.
 * Всё выглядит как обычный старт игры.
 *
 * 1. Системные лог-сообщения в чат (как консольный вывод)
 * 2. Все OP-команды кроме /tp заблокированы (CommandSourceMixin)
 * 3. Мир абсолютно тихий — никаких аномалий
 * 4. Финальное сообщение перед переходом в Stage 1
 */
public class Stage0SleepingCore {

    private static final String[][] BOOT_SEQUENCE = {
        { "[AURORA_STACK] memory allocation.............. OK" },
        { "[AURORA_STACK] instance tracker............... OK" },
        { "[AURORA_STACK] loop integrity................. OK" },
        { "[AURORA_STACK] archive index.................. OK" },
        { "[AURORA_STACK] reality index.................. CHECKING" },
        { "[AURORA_STACK] reality index.................. WARNING" },
        { "[AURORA_STACK] anomaly class: UNKNOWN" },
        { "[AURORA_STACK] flagging instance.............. DONE" },
    };

    private static int bootStep = 0;
    private static long lastBootTick = 0;
    private static boolean entryDone = false;
    private static boolean finalMessageSent = false;

    public static void tick(MinecraftServer server, long ticks) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        if (!entryDone) {
            onEntry(server, players);
            entryDone = true;
        }

        tickBootSequence(players, ticks);
        tickFinalMessage(players, ticks);
    }

    private static void onEntry(MinecraftServer server, List<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal(
                "[AURORA_STACK] ARCHIVE READY")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
            p.sendSystemMessage(Component.literal(
                "[AURORA_STACK] INSTANCE REGISTERED: " + p.getName().getString())
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
            p.sendSystemMessage(Component.literal(
                "[AURORA_STACK] LOOP STATUS: INITIALIZING")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
        }
        Forgotten404EndlessLoop.LOGGER.info("[ORIGIN LOOP] Stage 0: Sleeping Core active.");
    }

    // Загрузочная последовательность — каждые 15 сек один шаг
    private static void tickBootSequence(List<ServerPlayer> players, long ticks) {
        if (bootStep >= BOOT_SEQUENCE.length) return;
        if (ticks - lastBootTick < 300) return;
        lastBootTick = ticks;

        String msg = BOOT_SEQUENCE[bootStep][0];
        ChatFormatting color = msg.contains("WARNING") || msg.contains("anomaly")
            ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY;

        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal(msg)
                .withStyle(Style.EMPTY.withColor(color)));
        }
        bootStep++;
    }

    // Финальное сообщение за 10 сек до конца Stage 0
    private static void tickFinalMessage(List<ServerPlayer> players, long ticks) {
        if (finalMessageSent) return;
        if (ticks < 2200) return; // ~110 сек (Stage 0 длится 2400 тиков)
        finalMessageSent = true;

        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal(
                "[AURORA_STACK] BOOT COMPLETE")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withBold(true)));
            p.sendSystemMessage(Component.literal(
                "[AURORA_STACK] world simulation: ACTIVE")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
        }
    }

    public static void reset() {
        bootStep = 0;
        lastBootTick = 0;
        entryDone = false;
        finalMessageSent = false;
    }
}
