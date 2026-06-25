package com.eclipselayer.forgotten404.ending;

import com.eclipselayer.forgotten404.Forgotten404EndlessLoop;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.Random;

/**
 * Менеджер концовок — триггерит и воспроизводит одну из 7 концовок.
 *
 * Вызывается из Stage10EndlessLoop в зависимости от действий игрока:
 *
 * STABLE_EXIT        — игрок имеет все 7 фрагментов + убил ORIGIN_LOOP
 * CYCLE_ACCEPTANCE   — игрок подошёл к ORIGIN_LOOP и не атаковал 30 сек
 * WORLD_RESTART      — ORIGIN_LOOP жив >10 мин в Stage 10 (случайный триггер)
 * PLAYER_REPLACEMENT — игрок встретил двойника и подошёл вплотную
 * ARCHIVE_ABSORPTION — ORIGIN_LOOP убил игрока 3+ раза
 * SCRIPT_FAILURE     — игрок пытается ввести заблокированную команду 10+ раз
 * BEYOND_ARCHIVE     — после STABLE_EXIT, 1% шанс, секретный портал
 */
public class EndingManager {

    private static boolean endingTriggered = false;
    private static final Random rng = new Random();

    // Счётчики для триггеров
    private static int deathCount      = 0;
    private static int blockedCmdCount = 0;
    private static long nearOriginTick = 0;

    public static void onPlayerDeath(ServerPlayer player) {
        deathCount++;
        if (deathCount >= 3 && Forgotten404EndlessLoop.STAGE_MANAGER.getCurrentStage() == 10) {
            triggerEnding(EndingType.ARCHIVE_ABSORPTION, player.getServer());
        }
    }

    public static void onBlockedCommand(ServerPlayer player) {
        blockedCmdCount++;
        if (blockedCmdCount >= 10 && Forgotten404EndlessLoop.STAGE_MANAGER.getCurrentStage() >= 8) {
            triggerEnding(EndingType.SCRIPT_FAILURE, player.getServer());
        }
    }

    public static void onNearOriginLoop(MinecraftServer server, long ticks) {
        if (nearOriginTick == 0) { nearOriginTick = ticks; return; }
        if (ticks - nearOriginTick > 600) { // 30 сек рядом без атаки
            triggerEnding(EndingType.CYCLE_ACCEPTANCE, server);
        }
    }

    public static void onOriginLoopKilled(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();

        // Проверяем — есть ли у кого-то все 7 фрагментов
        boolean hasFragments = players.stream()
            .anyMatch(p -> FragmentTracker.hasAllFragments(p.getUUID()));

        if (hasFragments) {
            // 1% шанс на Beyond Archive
            if (rng.nextInt(100) == 0) {
                triggerEnding(EndingType.BEYOND_ARCHIVE, server);
            } else {
                triggerEnding(EndingType.STABLE_EXIT, server);
            }
        } else {
            triggerEnding(EndingType.WORLD_RESTART, server);
        }
    }

    public static void onPlayerReplacementContact(MinecraftServer server) {
        triggerEnding(EndingType.PLAYER_REPLACEMENT, server);
    }

    // ── Основной метод триггера ──────────────────────────────────────────────

    public static void triggerEnding(EndingType type, MinecraftServer server) {
        if (endingTriggered) return;
        endingTriggered = true;

        Forgotten404EndlessLoop.LOGGER.info("[ENDING] Triggering: {}", type);

        List<ServerPlayer> players = server.getPlayerList().getPlayers();

        switch (type) {
            case STABLE_EXIT       -> playStableExit(server, players);
            case CYCLE_ACCEPTANCE  -> playCycleAcceptance(server, players);
            case WORLD_RESTART     -> playWorldRestart(server, players);
            case PLAYER_REPLACEMENT -> playPlayerReplacement(server, players);
            case ARCHIVE_ABSORPTION -> playArchiveAbsorption(server, players);
            case SCRIPT_FAILURE    -> playScriptFailure(server, players);
            case BEYOND_ARCHIVE    -> playBeyondArchive(server, players);
        }
    }

    // ── 1. STABLE EXIT — Хорошая концовка ───────────────────────────────────

    private static void playStableExit(MinecraftServer server, List<ServerPlayer> players) {
        delay(server, 0, () -> broadcast(players,
            "LOOP TERMINATED", ChatFormatting.AQUA, true));
        delay(server, 60, () -> broadcast(players,
            "the archive is silent", ChatFormatting.GRAY, false));
        delay(server, 120, () -> broadcast(players,
            "[AURORA_STACK] cycle error: resolved", ChatFormatting.GRAY, false));
        delay(server, 200, () -> broadcast(players,
            "[LUCID WATCHER] You did it. We are free.", ChatFormatting.AQUA, false));
        delay(server, 300, () -> broadcast(players,
            "Welcome to a new reality.", ChatFormatting.WHITE, true));
        delay(server, 400, () -> {
            // Мир восстанавливается — возвращаем нормальный вид
            for (ServerPlayer p : players) {
                p.setGameMode(GameType.SURVIVAL);
                sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE);
            }
            // Сбрасываем мод
            Forgotten404EndlessLoop.STAGE_MANAGER.resetToStage0(0);
            FragmentTracker.reset();
        });
    }

    // ── 2. CYCLE ACCEPTANCE — Принятие цикла ────────────────────────────────

    private static void playCycleAcceptance(MinecraftServer server, List<ServerPlayer> players) {
        delay(server, 0, () -> broadcast(players,
            "JOIN THE ARCHIVE", ChatFormatting.DARK_PURPLE, true));
        delay(server, 60, () -> broadcast(players,
            "[ORIGIN_LOOP] you have made the right choice", ChatFormatting.DARK_RED, false));
        delay(server, 120, () -> broadcast(players,
            "[AURORA_STACK] instance absorbed", ChatFormatting.DARK_GRAY, false));
        delay(server, 200, () -> {
            for (ServerPlayer p : players) {
                sound(p, SoundEvents.ENDERMAN_TELEPORT);
                // Игрок становится спектатором — "исчез" из мира
                p.setGameMode(GameType.SPECTATOR);
                p.sendSystemMessage(Component.literal(
                    "You are now part of the archive.")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
            }
        });
    }

    // ── 3. WORLD RESTART — Перезапуск мира ──────────────────────────────────

    private static void playWorldRestart(MinecraftServer server, List<ServerPlayer> players) {
        delay(server, 0, () -> broadcast(players,
            "WELCOME BACK", ChatFormatting.RED, true));
        delay(server, 60, () -> broadcast(players,
            "[AURORA_STACK] cycle #" + (rng.nextInt(400) + 2) + " initiated",
            ChatFormatting.DARK_GRAY, false));
        delay(server, 120, () -> broadcast(players,
            "the loop continues", ChatFormatting.DARK_RED, false));
        delay(server, 200, () -> broadcast(players,
            "you remember nothing", ChatFormatting.DARK_GRAY, true));
        delay(server, 300, () -> {
            for (ServerPlayer p : players) sound(p, SoundEvents.WITHER_SPAWN);
            // Сброс к Stage 0 — петля начинается заново
            Forgotten404EndlessLoop.STAGE_MANAGER.resetToStage0(
                com.eclipselayer.forgotten404.stage.Stage10EndlessLoop.getLoopCount() + 1);
            endingTriggered = false; // можно снова триггерить
        });
    }

    // ── 4. PLAYER REPLACEMENT — Замена игрока ───────────────────────────────

    private static void playPlayerReplacement(MinecraftServer server, List<ServerPlayer> players) {
        delay(server, 0, () -> broadcast(players,
            "SCRIPT FAILURE", ChatFormatting.RED, true));
        delay(server, 40, () -> broadcast(players,
            "unknown error...", ChatFormatting.DARK_GRAY, false));
        delay(server, 100, () -> broadcast(players,
            "[SYSTEM] original instance: archived", ChatFormatting.DARK_GRAY, false));
        delay(server, 160, () -> broadcast(players,
            "[SYSTEM] replacement instance: active", ChatFormatting.DARK_RED, false));
        delay(server, 220, () -> {
            for (ServerPlayer p : players) {
                sound(p, SoundEvents.ENDERMAN_SCREAM);
                // Отправляем в спектатор — "игрок заменён"
                p.setGameMode(GameType.SPECTATOR);
                p.sendSystemMessage(Component.literal(
                    "You are now the shadow.")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
                p.sendSystemMessage(Component.literal(
                    "Watch the new instance live your life.")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
            }
        });
    }

    // ── 5. ARCHIVE ABSORPTION — Поглощение архива ───────────────────────────

    private static void playArchiveAbsorption(MinecraftServer server, List<ServerPlayer> players) {
        delay(server, 0, () -> broadcast(players,
            "MEMORY SAVED", ChatFormatting.DARK_RED, true));
        delay(server, 60, () -> broadcast(players,
            "[ORIGIN_LOOP] absorption complete", ChatFormatting.DARK_RED, false));
        delay(server, 120, () -> broadcast(players,
            "[AURORA_STACK] reality index: 0.00", ChatFormatting.DARK_GRAY, false));
        delay(server, 200, () -> {
            // Телепортируем всех в void
            ServerLevel level = server.overworld();
            for (ServerPlayer p : players) {
                p.teleportTo(0, -64, 0);
                sound(p, SoundEvents.WITHER_AMBIENT);
                p.sendSystemMessage(Component.literal(
                    "The world is gone. Only the archive remains.")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
                // Постоянный ночной режим — пустота
                level.setDayTime(18000);
            }
        });
        delay(server, 400, () -> broadcast(players,
            "awaiting next cycle...", ChatFormatting.DARK_GRAY, false));
    }

    // ── 6. SCRIPT FAILURE — Сбой скрипта ────────────────────────────────────

    private static void playScriptFailure(MinecraftServer server, List<ServerPlayer> players) {
        // Быстрые сообщения — система рушится
        for (int i = 0; i < 10; i++) {
            final int idx = i;
            delay(server, idx * 20, () -> {
                String[] errors = {
                    "SCRIPT ERROR", "NULL POINTER", "OVERFLOW",
                    "0x404", "!!!", "STACK CORRUPTION",
                    "ALL VERSIONS ACTIVE", "CANNOT RECOVER", "FATAL", "..."
                };
                broadcast(players, errors[idx % errors.length], ChatFormatting.RED, true);
            });
        }
        delay(server, 220, () -> {
            for (ServerPlayer p : players) sound(p, SoundEvents.LIGHTNING_BOLT_THUNDER);
            broadcast(players, "SCRIPT ERROR", ChatFormatting.RED, true);
        });
        delay(server, 300, () -> broadcast(players,
            "the script has collapsed", ChatFormatting.DARK_RED, false));
        delay(server, 400, () -> {
            // "Краш" — кидаем игроков в спектатор и замораживаем мир
            for (ServerPlayer p : players) {
                p.setGameMode(GameType.SPECTATOR);
                sound(p, SoundEvents.WITHER_DEATH);
            }
            // Останавливаем время
            server.overworld().setDayTime(18000);
        });
    }

    // ── 7. BEYOND ARCHIVE — Секретная ───────────────────────────────────────

    private static void playBeyondArchive(MinecraftServer server, List<ServerPlayer> players) {
        delay(server, 0, () -> broadcast(players,
            "LOOP TERMINATED", ChatFormatting.AQUA, true));
        delay(server, 100, () -> broadcast(players,
            "...", ChatFormatting.WHITE, false));
        delay(server, 200, () -> broadcast(players,
            "ORIGIN_LOOP WAS ONLY THE FIRST ERROR", ChatFormatting.GOLD, true));
        delay(server, 300, () -> broadcast(players,
            "[???] there are others", ChatFormatting.DARK_AQUA, false));
        delay(server, 400, () -> {
            // Строим секретный портал из crying obsidian в мире
            ServerPlayer target = players.get(0);
            ServerLevel level = server.overworld();
            buildSecretPortal(level, target.blockPosition().offset(5, 0, 0));

            broadcast(players,
                "[LUCID WATCHER] find the door. go beyond.",
                ChatFormatting.AQUA, true);

            for (ServerPlayer p : players) sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE);
        });
        delay(server, 600, () -> broadcast(players,
            "what lies beyond the archive?", ChatFormatting.GRAY, false));
    }

    private static void buildSecretPortal(ServerLevel level, BlockPos origin) {
        // Портал 3x4 из crying obsidian с подсветкой
        for (int y = 0; y <= 4; y++) {
            level.setBlock(origin.offset(0, y, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
            level.setBlock(origin.offset(3, y, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
        }
        for (int x = 0; x <= 3; x++) {
            level.setBlock(origin.offset(x, 0, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
            level.setBlock(origin.offset(x, 4, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
        }
        // Внутри — end gateway (визуальный эффект "за пределами")
        for (int x = 1; x <= 2; x++)
            for (int y = 1; y <= 3; y++)
                level.setBlock(origin.offset(x, y, 0), Blocks.END_GATEWAY.defaultBlockState(), 3);

        // Табличка
        BlockPos signPos = origin.offset(0, 5, 0);
        level.setBlock(signPos, Blocks.CRIMSON_SIGN.defaultBlockState(), 3);
        if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            sign.setText(new SignText()
                .setMessage(0, Component.literal("BEYOND")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA)))
                .setMessage(1, Component.literal("ARCHIVE")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA)))
                .setMessage(2, Component.literal("???")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD))), true);
            sign.setChanged();
        }
    }

    // ── Утилиты ─────────────────────────────────────────────────────────────

    private static void broadcast(List<ServerPlayer> players, String text,
                                   ChatFormatting color, boolean bold) {
        Component msg = Component.literal(text)
            .withStyle(Style.EMPTY.withColor(color).withBold(bold));
        for (ServerPlayer p : players) p.sendSystemMessage(msg);
    }

    private static void sound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound) {
        player.connection.send(new ClientboundSoundPacket(
            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
            SoundSource.MASTER,
            player.getX(), player.getY(), player.getZ(),
            1.0f, 1.0f, new Random().nextLong()
        ));
    }

    /** Откладывает выполнение через серверный тик. */
    private static void delay(MinecraftServer server, int tickDelay, Runnable action) {
        long targetTick = server.getTickCount() + tickDelay;
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s -> {
            if (s.getTickCount() >= targetTick) action.run();
        });
    }

    public static void incrementBlockedCmd() { blockedCmdCount++; }
    public static void resetNearOrigin() { nearOriginTick = 0; }
    public static boolean isTriggered() { return endingTriggered; }

    public static void reset() {
        endingTriggered = false;
        deathCount = 0;
        blockedCmdCount = 0;
        nearOriginTick = 0;
    }
}
