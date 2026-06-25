package com.eclipselayer.forgotten404.system;

import com.eclipselayer.forgotten404.Forgotten404EndlessLoop;
import com.eclipselayer.forgotten404.stage.Stage0SleepingCore;
import com.eclipselayer.forgotten404.stage.Stage1InitialStability;
import com.eclipselayer.forgotten404.stage.Stage10EndlessLoop;
import com.eclipselayer.forgotten404.system.EnvironmentManager;
import com.eclipselayer.forgotten404.stage.Stage2Glitches;
import com.eclipselayer.forgotten404.stage.Stage3ShadowEchoes;
import com.eclipselayer.forgotten404.stage.Stage4CorruptedSignal;
import com.eclipselayer.forgotten404.stage.Stage5NullZone;
import com.eclipselayer.forgotten404.stage.Stage6ArchiveFracture;
import com.eclipselayer.forgotten404.stage.Stage7MemoryCollapse;
import com.eclipselayer.forgotten404.stage.Stage8CoordinateLoop;
import com.eclipselayer.forgotten404.stage.Stage9ScriptAwakening;
import com.eclipselayer.forgotten404.stage.TBSStructures;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

import java.util.List;
import java.util.Random;

/**
 * ORIGIN LOOP PROTOCOL — Менеджер этапов.
 *
 *   0  Sleeping Core       — тихая загрузка, логи системы (первые 2 мин)
 *   1  Initial Stability   — мир нормален, Lucid Watcher наблюдает
 *   2  First Glitches      — первые сбои, «Can you see me?»
 *   3  Shadow Echoes       — руины прошлых миров, таблички Lucid Watcher
 *   4  Corrupted Signal    — искажение, «FIND THE CORE», цикл дня/ночи
 *   5  Null Zone           — мёртвые зоны, неподвижные тени
 *   6  Archive Fracture    — прорыв архивов прошлых циклов
 *   7  Memory Collapse     — двойники игрока, коллапс временных линий
 *   8  Coordinate Loop     — пространство закольцовывается, NO EXIT FOUND
 *   9  Script Awakening    — AURORA_STACK пробуждается, YOU ARE INSTANCE 404
 *  10  Endless Loop        — финальная битва с ORIGIN_LOOP
 */
public class StageManager {

    private static final long[] THRESHOLDS = {
        0L,          // 0  — Sleeping Core     (сразу, первые 2 мин)
        2_400L,      // 1  — Initial Stability  (2 мин)
        8_400L,      // 2  — First Glitches     (7 мин)
        16_800L,     // 3  — Shadow Echoes      (14 мин)
        26_400L,     // 4  — Corrupted Signal   (22 мин)
        38_400L,     // 5  — Null Zone          (32 мин)
        52_800L,     // 6  — Archive Fracture   (44 мин)
        68_400L,     // 7  — Memory Collapse    (57 мин)
        86_400L,     // 8  — Coordinate Loop    (72 мин)
        104_400L,    // 9  — Script Awakening   (87 мин)
        122_400L,    // 10 — Endless Loop       (102 мин)
    };

    private static final String[] STAGE_NAMES = {
        "SLEEPING CORE",
        "INITIAL STABILITY",
        "FIRST GLITCHES",
        "SHADOW ECHOES",
        "CORRUPTED SIGNAL",
        "NULL ZONE",
        "ARCHIVE FRACTURE",
        "MEMORY COLLAPSE",
        "COORDINATE LOOP",
        "SCRIPT AWAKENING",
        "ENDLESS LOOP"
    };

    private static final String[][] STAGE_MESSAGES = {
        // 0 — Sleeping Core
        { "ARCHIVE READY", "INSTANCE REGISTERED", "LOOP STATUS: INITIALIZING", "KERNEL: BOOT COMPLETE" },
        // 1 — Initial Stability
        { "The world is stable.", "Everything is fine.", "Lucid Watcher: watching.", "Nothing is here." },
        // 2 — First Glitches
        { "Can you see me?", "Help us.", "the chunk loaded twice", "what was that?" },
        // 3 — Shadow Echoes
        { "Help us!", "You are not alone.", "Don't trust the sky.", "Don't answer the door." },
        // 4 — Corrupted Signal
        { "FIND THE CORE", "DO NOT RESTART", "signal corrupted", "AURORA_STACK: tracking" },
        // 5 — Null Zone
        { "zone is dead", "he stands there", "don't come closer", "null sector detected" },
        // 6 — Archive Fracture
        { "archive breach detected", "whose structures are these?", "KEEP PLAYING", "DON'T TRUST THEM" },
        // 7 — Memory Collapse
        { "is that you? no. that was you.", "timelines collided", "deja vu activated", "SCRIPT: desynced" },
        // 8 — Coordinate Loop
        { "NO EXIT FOUND", "you already tried to leave", "coordinates repeat", "the compass lies" },
        // 9 — Script Awakening
        { "YOU ARE INSTANCE 404", "CORRUPTED USER", "AURORA_STACK: AWAKENED", "LOOP WILL CONTINUE", "KILL HIM" },
        // 10 — Endless Loop
        { "ORIGIN_LOOP: manifested", "MEMORY SAVED", "SCRIPT ERROR", "all cycles converge", "FINAL" }
    };

    private static final String[] LUCID_HINTS = {
        "Find the core.", "FIND THE CORE!", "Don't trust the system.",
        "I tried to stop this.", "Collect the 7 fragments.",
        "Don't trust your copy.", "AURORA_STACK is lying.", "Help us get out."
    };

    private int currentStage = 0;
    private long ticksElapsed = 0;
    private long lastMessageTick = 0;
    private long lastLucidTick = 0;
    private final Random rng = new Random();

    public void tick(MinecraftServer server) {
        ticksElapsed++;

        int newStage = computeStage();
        if (newStage != currentStage) {
            onStageTransition(server, currentStage, newStage);
            currentStage = newStage;
        }

        applyStageEffects(server);
        maybeGlitchMessage(server);
        maybeLucidMessage(server);

        // Stage-специфичные системы
        if (currentStage == 0) Stage0SleepingCore.tick(server, ticksElapsed);
        if (currentStage == 1) Stage1InitialStability.tick(server, ticksElapsed);
        if (currentStage == 2) Stage2Glitches.tick(server, ticksElapsed);
        if (currentStage == 3) Stage3ShadowEchoes.tick(server, ticksElapsed);
        if (currentStage == 4) Stage4CorruptedSignal.tick(server, ticksElapsed);
        if (currentStage == 5) Stage5NullZone.tick(server, ticksElapsed);
        if (currentStage == 6) Stage6ArchiveFracture.tick(server, ticksElapsed);
        if (currentStage == 7) Stage7MemoryCollapse.tick(server, ticksElapsed);
        if (currentStage == 8) Stage8CoordinateLoop.tick(server, ticksElapsed);
        if (currentStage == 9) Stage9ScriptAwakening.tick(server, ticksElapsed);
        if (currentStage == 10) Stage10EndlessLoop.tick(server, ticksElapsed);
        // TBS structures: Stage 3 until end of Stage 4
        if (currentStage >= 3 && currentStage <= 4) TBSStructures.tick(server, ticksElapsed, currentStage);
    }

    private int computeStage() {
        for (int i = THRESHOLDS.length - 1; i >= 0; i--) {
            if (ticksElapsed >= THRESHOLDS[i]) return i;
        }
        return 0;
    }

    private void onStageTransition(MinecraftServer server, int from, int to) {
        String name = STAGE_NAMES[Math.min(to, STAGE_NAMES.length - 1)];
        Forgotten404EndlessLoop.LOGGER.info("[ORIGIN LOOP] Stage {} → {} ({})", from, to, name);

        // Сброс этапов при выходе из них
        if (from == 2) Stage2Glitches.reset();
        if (from == 3) Stage3ShadowEchoes.reset();
        if (from == 4) Stage4CorruptedSignal.reset();
        if (from == 5) Stage5NullZone.reset();
        if (from == 6) Stage6ArchiveFracture.reset();
        if (from == 7) Stage7MemoryCollapse.reset();
        if (from == 8) Stage8CoordinateLoop.reset();
        if (from == 0) Stage0SleepingCore.reset();
        if (from == 1) Stage1InitialStability.reset();
        if (from == 9) Stage9ScriptAwakening.reset();
        if (from == 10) Stage10EndlessLoop.reset();
        if (from == 4) TBSStructures.reset(); // TBS ends after Stage 4

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        ChatFormatting color = stageColor(to);
        Component header = Component.literal("━━ STAGE " + to + " — " + name + " ━━")
            .withStyle(Style.EMPTY.withColor(color).withBold(to >= 7));
        for (ServerPlayer p : players) p.sendSystemMessage(header);

        switch (to) {
            case 0 -> {
                // Stage 0 никогда не вызывается через transition (стартует с 0)
                // но на случай рестарта:
                broadcast(server, "[AURORA_STACK] SLEEPING CORE: boot sequence initiated", ChatFormatting.DARK_GRAY, false);
            }
            case 1 -> broadcast(server,
                "[LUCID WATCHER] Welcome. The world is stable for now. But not for long.",
                ChatFormatting.DARK_AQUA, true);
            case 2 -> broadcast(server,
                "[SYSTEM] ANOMALY DETECTED — chunk duplication in sector 0x3F",
                ChatFormatting.DARK_GRAY, false);
            case 3 -> broadcast(server,
                "[LUCID WATCHER] You see the ruins? They remember you. You were here before.",
                ChatFormatting.DARK_AQUA, true);
            case 4 -> broadcast(server,
                "[AURORA_STACK] CORRUPTED SIGNAL — audio stream degraded",
                ChatFormatting.DARK_PURPLE, false);
            case 5 -> broadcast(server,
                "[SYSTEM] NULL ZONE formed. Life processes suspended in sector.",
                ChatFormatting.DARK_PURPLE, false);
            case 6 -> broadcast(server,
                "[ARCHIVE] BREACH DETECTED — previous cycle data leaking into current instance",
                ChatFormatting.DARK_RED, true);
            case 7 -> {
                broadcast(server, "[SYSTEM] MEMORY COLLAPSE — timeline desync detected", ChatFormatting.RED, true);
                broadcast(server, "[LUCID WATCHER] Don't trust your copy. That is not you.", ChatFormatting.DARK_AQUA, true);
            }
            case 8 -> broadcast(server, "NO EXIT FOUND", ChatFormatting.RED, true);
            case 9 -> {
                broadcast(server, "[AURORA_STACK] AWAKENED", ChatFormatting.RED, true);
                broadcast(server, "YOU ARE INSTANCE 404", ChatFormatting.RED, true);
                broadcast(server, "[AURORA_STACK] LOOP WILL CONTINUE", ChatFormatting.DARK_RED, false);
            }
            case 10 -> {
                broadcast(server, "[ORIGIN_LOOP] MANIFESTED", ChatFormatting.RED, true);
                broadcast(server, "SCRIPT ERROR — reality unstable", ChatFormatting.RED, false);
                broadcast(server, "All cycles converge. Time to choose.", ChatFormatting.GOLD, true);
            }
        }
    }

    private void applyStageEffects(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        switch (currentStage) {
            // Stage 0 — тихие системные логи каждые 30 сек
            case 0 -> {
                if (ticksElapsed % 600 == 0) {
                    String[] bootLog = {
                        "AURORA_STACK: memory allocation... OK",
                        "AURORA_STACK: instance tracker... OK",
                        "AURORA_STACK: loop integrity... OK",
                        "AURORA_STACK: reality index... CHECKING"
                    };
                    String msg = bootLog[(int)(ticksElapsed / 600 % bootLog.length)];
                    for (ServerPlayer p : players) {
                        p.sendSystemMessage(Component.literal(msg)
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
                    }
                }
            }
            case 4 -> {
                if (ticksElapsed % 500 == 0) {
                    ServerPlayer p = randomPlayer(players);
                    p.sendSystemMessage(Component.literal(
                        "[AURORA_STACK] tracking > " + p.getName().getString() + " <")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
                }
            }
            case 7 -> {
                if (ticksElapsed % 700 == 0) {
                    ServerPlayer p = randomPlayer(players);
                    p.sendSystemMessage(Component.literal(
                        "[SYSTEM] duplicate instance detected near " + p.getName().getString())
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withItalic(true)));
                }
            }
            case 8 -> {
                if (ticksElapsed % 600 == 0) {
                    for (ServerPlayer p : players) {
                        double dist = Math.sqrt(p.getX() * p.getX() + p.getZ() * p.getZ());
                        p.sendSystemMessage(Component.literal(String.format(
                            "[COORDINATE LOOP] distance %.0f — you have been here before", dist))
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE)));
                    }
                }
            }
            case 9 -> {
                if (ticksElapsed % 300 == 0) {
                    String[] threats = {
                        "CORRUPTED USER DETECTED", "KILL HIM", "YOU ARE INSTANCE 404",
                        "LOOP WILL CONTINUE", "DON'T TRUST THEM"
                    };
                    Component msg = Component.literal(threats[(int)(ticksElapsed / 300 % threats.length)])
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true));
                    for (ServerPlayer p : players) p.sendSystemMessage(msg);
                }
            }
            case 10 -> {
                if (ticksElapsed % 100 == 0) {
                    String[] collapse = {
                        "SCRIPT ERROR", "NULL", "0x404", "MEMORY SAVED",
                        "ORIGIN_LOOP: active", "!!!", "all world versions active"
                    };
                    Component msg = Component.literal(collapse[(int)(ticksElapsed / 100 % collapse.length)])
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true));
                    for (ServerPlayer p : players) p.sendSystemMessage(msg);
                }
            }
        }
    }

    private void maybeGlitchMessage(MinecraftServer server) {
        long interval = switch (currentStage) {
            case 0  -> 1200L; // каждую минуту — редко, атмосферно
            case 1  -> 2400L;
            case 2  -> 1000L;
            case 3  -> 800L;
            case 4  -> 600L;
            case 5  -> 450L;
            case 6  -> 320L;
            case 7  -> 240L;
            case 8  -> 180L;
            case 9  -> 120L;
            case 10 -> 60L;
            default -> 99999L;
        };

        if (ticksElapsed - lastMessageTick < interval) return;
        lastMessageTick = ticksElapsed;

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        String[] msgs = STAGE_MESSAGES[Math.min(currentStage, STAGE_MESSAGES.length - 1)];
        String msg = msgs[(int)(ticksElapsed % msgs.length)];

        Component component = Component.literal(msg)
            .withStyle(Style.EMPTY.withColor(stageColor(currentStage)).withItalic(currentStage > 0));

        if (currentStage >= 8) {
            for (ServerPlayer p : players) p.sendSystemMessage(component);
        } else {
            randomPlayer(players).sendSystemMessage(component);
        }
    }

    private void maybeLucidMessage(MinecraftServer server) {
        if (currentStage < 2 || currentStage > 8) return;

        long interval = 3000L - (currentStage * 200L);
        if (ticksElapsed - lastLucidTick < interval) return;
        lastLucidTick = ticksElapsed;

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        String hint = LUCID_HINTS[rng.nextInt(LUCID_HINTS.length)];
        randomPlayer(players).sendSystemMessage(
            Component.literal("[LUCID WATCHER] " + hint)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA).withItalic(true)));
    }

    private void broadcast(MinecraftServer server, String text, ChatFormatting color, boolean bold) {
        Component msg = Component.literal(text)
            .withStyle(Style.EMPTY.withColor(color).withBold(bold));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) p.sendSystemMessage(msg);
    }

    private ServerPlayer randomPlayer(List<ServerPlayer> players) {
        return players.get(rng.nextInt(players.size()));
    }

    private ChatFormatting stageColor(int stage) {
        return switch (stage) {
            case 0     -> ChatFormatting.DARK_GRAY;
            case 1     -> ChatFormatting.GRAY;
            case 2, 3  -> ChatFormatting.DARK_GRAY;
            case 4     -> ChatFormatting.DARK_AQUA;
            case 5, 6  -> ChatFormatting.DARK_PURPLE;
            case 7, 8  -> ChatFormatting.DARK_RED;
            case 9, 10 -> ChatFormatting.RED;
            default    -> ChatFormatting.WHITE;
        };
    }

    public int getCurrentStage() { return currentStage; }
    public long getTicksElapsed() { return ticksElapsed; }

    /**
     * Сбрасывает мод обратно в Stage 0 — петля начинается заново.
     * Вызывается когда ORIGIN_LOOP убит в Stage 10.
     */
    public void resetToStage0(int loopCount) {
        Forgotten404EndlessLoop.LOGGER.info("[ORIGIN LOOP] Loop #{} — resetting to Stage 0.", loopCount);
        currentStage = 0;
        ticksElapsed = 0;
        lastMessageTick = 0;
        lastLucidTick = 0;

        // Сбрасываем все этапы
        Stage0SleepingCore.reset();
        Stage1InitialStability.reset();
        Stage2Glitches.reset();
        Stage3ShadowEchoes.reset();
        Stage4CorruptedSignal.reset();
        Stage5NullZone.reset();
        Stage6ArchiveFracture.reset();
        Stage7MemoryCollapse.reset();
        Stage8CoordinateLoop.reset();
        Stage9ScriptAwakening.reset();
        TBSStructures.reset();
    }

}