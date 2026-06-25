package com.eclipselayer.forgotten404.stage;

import com.eclipselayer.forgotten404.Forgotten404EndlessLoop;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Random;

/**
 * Stage 2 — FIRST GLITCHES
 *
 * 1. Сообщения: "Can you see me?" / "Help us." — случайному игроку
 * 2. Фейковые звуки шагов/шёпота рядом с игроком (без источника)
 * 3. Случайные лаги — принудительный sleep серверного тика
 * 4. Текстурный глюк — блок мерцает (воздух → оригинал) только на клиенте
 * 5. Запись instance системой AURORA_STACK (одноразово)
 */
public class Stage2Glitches {

    private static final String[] GLITCH_MESSAGES = {
        "Can you see me?",
        "Help us.",
        "we are still here",
        "don't ignore this",
        "Can you hear us?",
        "why did you come back"
    };

    private static final Random rng = new Random();

    private static long lastMessageTick  = 0;
    private static long lastSoundTick    = 0;
    private static long lastLagTick      = 0;
    private static long lastGlitchTick   = 0;
    private static boolean instanceRecorded = false;

    public static void tick(MinecraftServer server, long ticks) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        // Запись instance — один раз при входе в Stage 2
        if (!instanceRecorded && ticks > 0) {
            recordInstance(server, players);
            instanceRecorded = true;
        }

        tickMessages(players, ticks);
        tickSounds(players, ticks);
        tickLag(ticks);
        tickBlockGlitch(server, players, ticks);
        GlitchRestoreQueue.tick(server, ticks);
    }

    // ── 1. Сообщения ────────────────────────────────────────────────────────

    private static void tickMessages(List<ServerPlayer> players, long ticks) {
        if (ticks - lastMessageTick < 700) return; // ~35 сек
        lastMessageTick = ticks;

        String msg = GLITCH_MESSAGES[rng.nextInt(GLITCH_MESSAGES.length)];
        ServerPlayer target = players.get(rng.nextInt(players.size()));
        target.sendSystemMessage(Component.literal(msg)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
    }

    // ── 2. Звуки шёпота / шагов ─────────────────────────────────────────────

    private static void tickSounds(List<ServerPlayer> players, long ticks) {
        if (ticks - lastSoundTick < 500) return; // ~25 сек
        lastSoundTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));

        // Случайная точка в 4–12 блоках от игрока
        double offsetX = (rng.nextDouble() - 0.5) * 24;
        double offsetZ = (rng.nextDouble() - 0.5) * 24;
        double x = target.getX() + offsetX;
        double y = target.getY();
        double z = target.getZ() + offsetZ;

        var sound = rng.nextBoolean()
            ? SoundEvents.STONE_STEP        // шаги
            : SoundEvents.ENDERMAN_STARE;   // "шёпот"

        target.connection.send(new ClientboundSoundPacket(
            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
            SoundSource.AMBIENT,
            x, y, z,
            0.5f + rng.nextFloat() * 0.5f,
            0.7f + rng.nextFloat() * 0.6f,
            rng.nextLong()
        ));
    }

    // ── 3. Лаг-спайк ────────────────────────────────────────────────────────

    private static void tickLag(long ticks) {
        if (ticks - lastLagTick < 1200) return; // раз в минуту
        lastLagTick = ticks;

        if (rng.nextInt(100) > 35) return; // 35% шанс

        try {
            int lagMs = 150 + rng.nextInt(350); // 150–500 мс
            Forgotten404EndlessLoop.LOGGER.info("[SYSTEM] lag spike: {}ms", lagMs);
            Thread.sleep(lagMs);
        } catch (InterruptedException ignored) {}
    }

    // ── 4. Текстурный глюк — мерцание блока ─────────────────────────────────

    private static void tickBlockGlitch(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (ticks - lastGlitchTick < 400) return; // ~20 сек
        lastGlitchTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        ServerLevel level = server.getLevel(target.level().dimension());
        if (level == null) return;

        // Случайный блок в радиусе 6 блоков
        int bx = (int) target.getX() + rng.nextInt(13) - 6;
        int by = (int) target.getY() + rng.nextInt(3);
        int bz = (int) target.getZ() + rng.nextInt(13) - 6;
        BlockPos pos = new BlockPos(bx, by, bz);

        BlockState original = level.getBlockState(pos);
        if (original.isAir()) return;

        // Отправляем воздух
        target.connection.send(new ClientboundBlockUpdatePacket(pos, Blocks.AIR.defaultBlockState()));

        // Через 8–15 тиков возвращаем
        long restoreAt = ticks + 8 + rng.nextInt(8);
        GlitchRestoreQueue.schedule(pos, original, restoreAt, target);
    }

    // ── 5. Запись instance ───────────────────────────────────────────────────

    private static void recordInstance(MinecraftServer server, List<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal(
                "[AURORA_STACK] INSTANCE FLAGGED: " + p.getName().getString())
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
            p.sendSystemMessage(Component.literal(
                "[AURORA_STACK] anomaly class: UNIQUE — monitoring initiated")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
        }
        Forgotten404EndlessLoop.LOGGER.info("[AURORA_STACK] Stage 2: instances recorded.");
    }

    public static void reset() {
        instanceRecorded = false;
        lastMessageTick  = 0;
        lastSoundTick    = 0;
        lastLagTick      = 0;
        lastGlitchTick   = 0;
    }
}
