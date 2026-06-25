package com.eclipselayer.forgotten404.stage;

import com.eclipselayer.forgotten404.Forgotten404EndlessLoop;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;
import java.util.Random;

/**
 * Stage 1 — INITIAL STABILITY
 *
 * Мир выглядит абсолютно нормально. Lucid Watcher наблюдает молча.
 * Изредка что-то чуть-чуть не так — но игрок может решить что это норма.
 *
 * 1. Редкие тихие сообщения — "The world is stable." / "Nothing is here."
 * 2. Lucid Watcher один раз представляется
 * 3. Очень редкий звук вдалеке — почти неслышимый
 * 4. Рюкзак Traveler's Backpack уже выдан (PlayerJoinHandler)
 * 5. Ощущение что за тобой наблюдают — раз в минуту имя игрока
 *    появляется в тёмно-сером цвете (будто система тебя сканирует)
 */
public class Stage1InitialStability {

    private static final String[] STABILITY_MESSAGES = {
        "The world is stable.",
        "Everything is fine.",
        "Nothing is here.",
        "Lucid Watcher: watching.",
        "The world is stable.",
        "Everything is fine.",
    };

    private static final Random rng = new Random();

    private static long lastMsgTick    = 0;
    private static long lastSoundTick  = 0;
    private static long lastScanTick   = 0;
    private static boolean entryDone   = false;
    private static boolean lucidIntro  = false;

    public static void tick(MinecraftServer server, long ticks) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        if (!entryDone) {
            onEntry(server, players);
            entryDone = true;
        }

        tickLucidIntro(players, ticks);
        tickStabilityMessages(players, ticks);
        tickDistantSound(players, ticks);
        tickScan(players, ticks);
    }

    private static void onEntry(MinecraftServer server, List<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal(
                "[LUCID WATCHER] Welcome. The world is stable for now. But not for long.")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA).withItalic(true)));
        }
        Forgotten404EndlessLoop.LOGGER.info("[ORIGIN LOOP] Stage 1: Initial Stability active.");
    }

    // Lucid Watcher представляется один раз через 30 сек
    private static void tickLucidIntro(List<ServerPlayer> players, long ticks) {
        if (lucidIntro) return;
        if (ticks < 600) return;
        lucidIntro = true;

        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal(
                "[LUCID WATCHER] I am what AURORA_STACK tried to delete.")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA).withItalic(true)));
            p.sendSystemMessage(Component.literal(
                "[LUCID WATCHER] Stay close to me. Find the core.")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA).withItalic(true)));
        }
    }

    // Тихие сообщения о стабильности — раз в 2 мин
    private static void tickStabilityMessages(List<ServerPlayer> players, long ticks) {
        if (ticks - lastMsgTick < 2400) return;
        lastMsgTick = ticks;

        String msg = STABILITY_MESSAGES[rng.nextInt(STABILITY_MESSAGES.length)];
        ServerPlayer target = players.get(rng.nextInt(players.size()));
        target.sendSystemMessage(Component.literal(msg)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true)));
    }

    // Очень тихий звук вдалеке — раз в 90 сек
    private static void tickDistantSound(List<ServerPlayer> players, long ticks) {
        if (ticks - lastSoundTick < 1800) return;
        lastSoundTick = ticks;

        if (rng.nextInt(100) > 30) return; // 30% шанс

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        target.connection.send(new ClientboundSoundPacket(
            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.STONE_STEP),
            SoundSource.AMBIENT,
            target.getX() + rng.nextInt(30) - 15,
            target.getY(),
            target.getZ() + rng.nextInt(30) - 15,
            0.1f, // очень тихо
            1.0f,
            rng.nextLong()
        ));
    }

    // Сканирование — имя игрока в тёмном цвете, раз в минуту
    private static void tickScan(List<ServerPlayer> players, long ticks) {
        if (ticks - lastScanTick < 1200) return;
        lastScanTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        target.sendSystemMessage(Component.literal(
            "[AURORA_STACK] scan > " + target.getName().getString() + " < OK")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
    }

    public static void reset() {
        entryDone  = false;
        lucidIntro = false;
        lastMsgTick   = 0;
        lastSoundTick = 0;
        lastScanTick  = 0;
    }
}
