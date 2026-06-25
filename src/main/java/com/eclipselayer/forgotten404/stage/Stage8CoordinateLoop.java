package com.eclipselayer.forgotten404.stage;

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
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Stage 8 — COORDINATE LOOP
 *
 * 1. Телепорт-ловушка — если игрок уходит дальше 1000 блоков от 0,0,
 *    его телепортируют обратно на ~противоположные координаты (симметрично)
 *    с сообщением "you have been here before"
 * 2. NO EXIT FOUND — периодически на экране
 * 3. Компас-ложь — каждые 30 сек игроку клиентски показывается что
 *    компас крутится (звук + сообщение "the compass lies")
 * 4. Дублирование территорий — блоки в радиусе 20 клиентски мерцают
 *    как копии самих себя (тот же блок что и под ногами)
 * 5. Координатные предупреждения — показывает реальные координаты
 *    и говорит что игрок уже был здесь
 * 6. Сохранение точки входа — система запоминает где был игрок
 *    и периодически телепортирует обратно
 */
public class Stage8CoordinateLoop {

    private static final int LOOP_RADIUS = 1000; // блоков от 0,0

    private static final String[] LOOP_MESSAGES = {
        "NO EXIT FOUND",
        "you already tried to leave",
        "coordinates repeat",
        "the compass lies",
        "[COORDINATE LOOP] space is folding",
        "[SYSTEM] territory duplication: active",
        "[AURORA_STACK] escape attempt: logged",
        "you have been here before",
        "[SYSTEM] map: meaningless",
        "NO EXIT FOUND",
    };

    private static final Random rng = new Random();

    // Точки входа в этап — куда возвращать игроков
    private static final Map<UUID, Vec3> entryPoints = new HashMap<>();
    // Счётчик телепортов — усиливается с каждым разом
    private static final Map<UUID, Integer> loopCount = new HashMap<>();

    private static long lastMessageTick   = 0;
    private static long lastCoordTick     = 0;
    private static long lastCompassTick   = 0;
    private static long lastDupeTick      = 0;
    private static long lastReturnTick    = 0;
    private static boolean entryDone      = false;

    public static void tick(MinecraftServer server, long ticks) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        if (!entryDone) {
            onEntry(server, players);
            entryDone = true;
        }

        tickLoopTrap(server, players, ticks);
        tickMessages(players, ticks);
        tickCoordWarning(players, ticks);
        tickCompassLie(players, ticks);
        tickDuplication(server, players, ticks);
        tickForcedReturn(server, players, ticks);
        GlitchRestoreQueue.tick(server, ticks);
    }

    // ── Вход в этап ─────────────────────────────────────────────────────────

    private static void onEntry(MinecraftServer server, List<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            // Запоминаем точку входа
            entryPoints.put(p.getUUID(), p.position());
            loopCount.put(p.getUUID(), 0);

            p.sendSystemMessage(Component.literal("NO EXIT FOUND")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)));
            p.sendSystemMessage(Component.literal(
                "[COORDINATE LOOP] space is folding around you")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withItalic(true)));
        }
        Forgotten404EndlessLoop.LOGGER.info("[ORIGIN LOOP] Stage 8: Coordinate Loop active.");
    }

    // ── 1. Ловушка радиуса — телепорт при выходе за 1000 блоков ────────────

    private static void tickLoopTrap(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (ticks % 40 != 0) return; // проверяем каждые 2 сек

        for (ServerPlayer p : players) {
            double x = p.getX();
            double z = p.getZ();
            double dist = Math.sqrt(x * x + z * z);

            if (dist < LOOP_RADIUS) continue;

            // Игрок вышел за радиус — телепортируем на симметричные координаты
            int count = loopCount.getOrDefault(p.getUUID(), 0) + 1;
            loopCount.put(p.getUUID(), count);

            // Симметрично отражаем координаты (как бы "другой конец петли")
            double newX = -x * 0.7 + rng.nextInt(200) - 100;
            double newZ = -z * 0.7 + rng.nextInt(200) - 100;
            double newY = p.getY();

            ServerLevel level = server.getLevel(p.level().dimension());
            if (level != null) {
                // Находим безопасную Y
                int safeY = level.getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                    (int) newX, (int) newZ);
                newY = Math.max(safeY, 64);
            }

            p.teleportTo(newX, newY, newZ);

            p.sendSystemMessage(Component.literal("you have been here before")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)));
            p.sendSystemMessage(Component.literal(
                String.format("[COORDINATE LOOP] attempt #%d — distance was %.0f", count, dist))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));

            // Звук дезориентации
            p.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ENDERMAN_TELEPORT),
                SoundSource.MASTER,
                p.getX(), p.getY(), p.getZ(),
                1.0f, 0.4f, rng.nextLong()
            ));

            Forgotten404EndlessLoop.LOGGER.info("[COORDINATE LOOP] {} teleported back (attempt #{})",
                p.getName().getString(), count);
        }
    }

    // ── 2. NO EXIT FOUND и другие сообщения ─────────────────────────────────

    private static void tickMessages(List<ServerPlayer> players, long ticks) {
        if (ticks - lastMessageTick < 300) return; // ~15 сек
        lastMessageTick = ticks;

        String msg = LOOP_MESSAGES[(int)(ticks / 300 % LOOP_MESSAGES.length)];
        boolean isImportant = msg.equals("NO EXIT FOUND") || msg.equals("you have been here before");

        Component component = Component.literal(msg)
            .withStyle(Style.EMPTY
                .withColor(isImportant ? ChatFormatting.RED : ChatFormatting.DARK_PURPLE)
                .withBold(isImportant)
                .withItalic(!isImportant));

        for (ServerPlayer p : players) p.sendSystemMessage(component);
    }

    // ── 3. Координатные предупреждения ───────────────────────────────────────

    private static void tickCoordWarning(List<ServerPlayer> players, long ticks) {
        if (ticks - lastCoordTick < 600) return; // ~30 сек
        lastCoordTick = ticks;

        for (ServerPlayer p : players) {
            double dist = Math.sqrt(p.getX() * p.getX() + p.getZ() * p.getZ());
            p.sendSystemMessage(Component.literal(
                String.format("[COORDINATE LOOP] distance %.0f — you have been here before", dist))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE)));
        }
    }

    // ── 4. Компас-ложь ──────────────────────────────────────────────────────

    private static void tickCompassLie(List<ServerPlayer> players, long ticks) {
        if (ticks - lastCompassTick < 600) return; // ~30 сек
        lastCompassTick = ticks;

        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal("the compass lies")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
            p.sendSystemMessage(Component.literal("[SYSTEM] map: meaningless")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));

            // Звук вращающегося компаса (clock tick sound)
            p.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.UI_BUTTON_CLICK.value()),
                SoundSource.MASTER,
                p.getX(), p.getY(), p.getZ(),
                0.3f, 0.5f + rng.nextFloat(),
                rng.nextLong()
            ));
        }
    }

    // ── 5. Дублирование территорий — блоки мерцают ──────────────────────────

    private static void tickDuplication(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (ticks - lastDupeTick < 500) return; // ~25 сек
        lastDupeTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        ServerLevel level = server.getLevel(target.level().dimension());
        if (level == null) return;

        // Клиентски показываем блоки рядом как будто они дублируются
        // (берём блок из-под ног и рисуем его рядом)
        BlockPos underFeet = target.blockPosition().below();
        var underBlock = level.getBlockState(underFeet);
        if (underBlock.isAir()) return;

        int count = 5 + rng.nextInt(8);
        for (int i = 0; i < count; i++) {
            int dx = rng.nextInt(15) - 7;
            int dz = rng.nextInt(15) - 7;
            BlockPos pos = target.blockPosition().offset(dx, rng.nextInt(3) - 1, dz);

            if (level.getBlockState(pos).isAir()) {
                target.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(
                    pos, underBlock));
                GlitchRestoreQueue.schedule(pos,
                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                    ticks + 15 + rng.nextInt(10), target);
            }
        }

        target.sendSystemMessage(Component.literal("[SYSTEM] territory duplication: active")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withItalic(true)));
    }

    // ── 6. Принудительный возврат к точке входа ─────────────────────────────

    private static void tickForcedReturn(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (ticks - lastReturnTick < 3600) return; // каждые 3 мин
        lastReturnTick = ticks;

        if (rng.nextInt(100) > 40) return; // 40% шанс

        for (ServerPlayer p : players) {
            Vec3 entry = entryPoints.get(p.getUUID());
            if (entry == null) continue;

            // Телепортируем к точке входа в этап
            ServerLevel level = server.getLevel(p.level().dimension());
            if (level == null) continue;

            int safeY = level.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                (int) entry.x, (int) entry.z);

            p.teleportTo(entry.x, Math.max(safeY, entry.y), entry.z);

            p.sendSystemMessage(Component.literal("NO EXIT FOUND")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)));
            p.sendSystemMessage(Component.literal("you cannot leave")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withItalic(true)));

            p.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ENDERMAN_TELEPORT),
                SoundSource.MASTER,
                p.getX(), p.getY(), p.getZ(),
                1.0f, 0.3f, rng.nextLong()
            ));
        }
    }

    public static void reset() {
        entryDone      = false;
        lastMessageTick  = 0;
        lastCoordTick    = 0;
        lastCompassTick  = 0;
        lastDupeTick     = 0;
        lastReturnTick   = 0;
        entryPoints.clear();
        loopCount.clear();
    }
}
