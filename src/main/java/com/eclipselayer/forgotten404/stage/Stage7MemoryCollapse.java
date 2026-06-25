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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Stage 7 — MEMORY COLLAPSE
 *
 * 1. Двойники — зомби с именем игрока, стоят неподвижно, смотрят в никуда.
 *    При приближении исчезают с сообщением "timeline desynced"
 * 2. Флэшбэки построек — клиентски рядом с игроком на секунду
 *    появляется случайная структура из прошлого (блоки мерцают)
 * 3. Déjà vu сообщения — фразы которые игрок уже "видел" (Stage 2 фразы)
 *    но с приставкой [DEJA VU]
 * 4. Десинхрон времени — резкие скачки дня/ночи
 * 5. Звуки из прошлого — звуки предыдущих этапов в случайном порядке
 */
public class Stage7MemoryCollapse {

    private static final String[] DEJAVU_MESSAGES = {
        "[DEJA VU] Can you see me?",
        "[DEJA VU] the chunk loaded twice",
        "[DEJA VU] Help us.",
        "[DEJA VU] You are not alone.",
        "[DEJA VU] Don't trust the sky.",
        "[DEJA VU] FIND THE CORE",
        "[DEJA VU] he stands there",
        "[DEJA VU] this is the third cycle",
        "[DEJA VU] we were here before",
    };

    private static final String[] COLLAPSE_MESSAGES = {
        "[SYSTEM] MEMORY COLLAPSE — timeline desync detected",
        "[SYSTEM] duplicate instance detected",
        "[SYSTEM] temporal overlap: critical",
        "[AURORA_STACK] timeline collision imminent",
        "this already happened",
        "you've been here",
        "[SYSTEM] SCRIPT: desynced",
        "is that you? no. that was you.",
        "timelines collided",
    };

    // Флэшбэк-структура — случайные блоки вокруг игрока (только клиентски)
    private static final BlockState[] FLASHBACK_BLOCKS = {
        Blocks.COBBLESTONE.defaultBlockState(),
        Blocks.OAK_PLANKS.defaultBlockState(),
        Blocks.STONE_BRICKS.defaultBlockState(),
        Blocks.OAK_LOG.defaultBlockState(),
        Blocks.DIRT.defaultBlockState(),
    };

    private static final Random rng = new Random();

    private static long lastDoubleTick    = 0;
    private static long lastFlashTick     = 0;
    private static long lastDejavuTick    = 0;
    private static long lastCollapseTick  = 0;
    private static long lastSoundTick     = 0;
    private static boolean entryDone      = false;

    private record DoubleData(UUID entityId, BlockPos pos, String playerName) {}
    private static final List<DoubleData> activeDoubles = new ArrayList<>();

    public static void tick(MinecraftServer server, long ticks) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        if (!entryDone) {
            onEntry(server, players);
            entryDone = true;
        }

        tickDoubles(server, players, ticks);
        tickFlashback(server, players, ticks);
        tickDejavu(players, ticks);
        tickCollapseMessages(players, ticks);
        tickSounds(players, ticks);
        GlitchRestoreQueue.tick(server, ticks);
    }

    // ── Вход в этап ─────────────────────────────────────────────────────────

    private static void onEntry(MinecraftServer server, List<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal(
                "[SYSTEM] MEMORY COLLAPSE — timeline desync detected")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)));
            p.sendSystemMessage(Component.literal(
                "[LUCID WATCHER] Don't trust your copy. That is not you.")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA).withItalic(true)));
        }
        Forgotten404EndlessLoop.LOGGER.info("[ORIGIN LOOP] Stage 7: Memory Collapse active.");
    }

    // ── 1. Двойники ─────────────────────────────────────────────────────────

    private static void tickDoubles(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (ticks - lastDoubleTick < 1400) return; // раз в ~70 сек
        lastDoubleTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        ServerLevel level = server.getLevel(target.level().dimension());
        if (level == null) return;

        // Двойник в 8–15 блоках
        int dist = 8 + rng.nextInt(8);
        int angle = rng.nextInt(360);
        double dx = Math.cos(Math.toRadians(angle)) * dist;
        double dz = Math.sin(Math.toRadians(angle)) * dist;

        try {
            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie == null) return;

            zombie.moveTo(target.getX() + dx, target.getY(), target.getZ() + dz, target.getYRot(), 0);
            zombie.setNoAi(true);
            zombie.setSilent(true);
            // Имя игрока над двойником
            zombie.setCustomName(Component.literal(target.getName().getString())
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
            zombie.setCustomNameVisible(true);
            // Броня для похожести (упрощённо — просто зомби без доспехов)
            level.addFreshEntity(zombie);

            activeDoubles.add(new DoubleData(zombie.getUUID(),
                zombie.blockPosition(), target.getName().getString()));

            target.sendSystemMessage(Component.literal(
                "[SYSTEM] duplicate instance detected near " + target.getName().getString())
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withItalic(true)));

        } catch (Exception e) {
            Forgotten404EndlessLoop.LOGGER.error("[STAGE7] Double spawn error: {}", e.getMessage());
        }

        // Проверяем старых двойников — исчезают при приближении
        activeDoubles.removeIf(d -> {
            net.minecraft.world.entity.Entity entity = server.overworld().getEntity(d.entityId());
            if (entity == null) return true;

            for (ServerPlayer p : players) {
                if (p.getName().getString().equals(d.playerName())) {
                    double dist2 = p.distanceTo(entity);
                    if (dist2 < 4) {
                        entity.discard();
                        p.sendSystemMessage(Component.literal("timeline desynced")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
                        return true;
                    }
                }
            }
            // Автоудаление через 3 мин
            if (ticks - lastDoubleTick > 3600) {
                entity.discard();
                return true;
            }
            return false;
        });
    }

    // ── 2. Флэшбэк построек ─────────────────────────────────────────────────

    private static void tickFlashback(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (ticks - lastFlashTick < 600) return; // ~30 сек
        lastFlashTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        ServerLevel level = server.getLevel(target.level().dimension());
        if (level == null) return;

        // Генерируем 8–15 случайных блоков рядом только клиентски
        int count = 8 + rng.nextInt(8);
        for (int i = 0; i < count; i++) {
            int bx = (int) target.getX() + rng.nextInt(13) - 6;
            int by = (int) target.getY() + rng.nextInt(4) - 1;
            int bz = (int) target.getZ() + rng.nextInt(13) - 6;
            BlockPos pos = new BlockPos(bx, by, bz);

            BlockState original = level.getBlockState(pos);
            if (!original.isAir()) continue;

            BlockState flash = FLASHBACK_BLOCKS[rng.nextInt(FLASHBACK_BLOCKS.length)];
            // Отправляем клиенту фейковый блок
            target.connection.send(new ClientboundBlockUpdatePacket(pos, flash));
            // Через 12–20 тиков возвращаем воздух
            GlitchRestoreQueue.schedule(pos, Blocks.AIR.defaultBlockState(),
                ticks + 12 + rng.nextInt(8), target);
        }

        target.sendSystemMessage(Component.literal("deja vu activated")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withItalic(true)));
    }

    // ── 3. Déjà vu сообщения ────────────────────────────────────────────────

    private static void tickDejavu(List<ServerPlayer> players, long ticks) {
        if (ticks - lastDejavuTick < 500) return; // ~25 сек
        lastDejavuTick = ticks;

        String msg = DEJAVU_MESSAGES[rng.nextInt(DEJAVU_MESSAGES.length)];
        players.get(rng.nextInt(players.size())).sendSystemMessage(
            Component.literal(msg)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withItalic(true)));
    }

    // ── 4. Коллапс-сообщения ────────────────────────────────────────────────

    private static void tickCollapseMessages(List<ServerPlayer> players, long ticks) {
        if (ticks - lastCollapseTick < 350) return; // ~17 сек
        lastCollapseTick = ticks;

        String msg = COLLAPSE_MESSAGES[rng.nextInt(COLLAPSE_MESSAGES.length)];
        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal(msg)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withItalic(true)));
        }
    }

    // ── 5. Звуки из прошлого ────────────────────────────────────────────────

    private static void tickSounds(List<ServerPlayer> players, long ticks) {
        if (ticks - lastSoundTick < 400) return; // ~20 сек
        lastSoundTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        var sounds = new net.minecraft.sounds.SoundEvent[]{
            SoundEvents.ENDERMAN_STARE,
            SoundEvents.WITHER_AMBIENT,
            SoundEvents.ELDER_GUARDIAN_AMBIENT,
            SoundEvents.SKELETON_AMBIENT,
            SoundEvents.WOLF_HOWL
        };

        target.connection.send(new ClientboundSoundPacket(
            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sounds[rng.nextInt(sounds.length)]),
            SoundSource.AMBIENT,
            target.getX() + rng.nextInt(20) - 10,
            target.getY(),
            target.getZ() + rng.nextInt(20) - 10,
            0.5f + rng.nextFloat() * 0.5f,
            0.5f + rng.nextFloat() * 0.7f,
            rng.nextLong()
        ));
    }

    public static void reset() {
        entryDone         = false;
        lastDoubleTick    = 0;
        lastFlashTick     = 0;
        lastDejavuTick    = 0;
        lastCollapseTick  = 0;
        lastSoundTick     = 0;
        activeDoubles.clear();
    }
}
