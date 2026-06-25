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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Stage 5 — NULL ZONE
 *
 * 1. Нулевые зоны — радиус 12 блоков вокруг случайной точки:
 *    - Мобы внутри замораживаются (AI отключается)
 *    - Трава/листья/цветы заменяются на мёртвые блоки
 *    - Блоки постепенно разрушаются (воздух клиентски)
 * 2. Тень — скелет-невидимка в центре зоны ночью, тает при приближении
 * 3. Тишина — звуки окружения заглушаются, только далёкий гул
 * 4. Сообщения деградации цикла
 */
public class Stage5NullZone {

    private static final int ZONE_RADIUS = 12;

    private static final String[] ZONE_MESSAGES = {
        "[SYSTEM] NULL ZONE formed. Life processes suspended in sector.",
        "[SYSTEM] zone is dead",
        "[SYSTEM] biological processes: halted",
        "[AURORA_STACK] null sector detected",
        "[SYSTEM] entity AI: suspended in range",
        "he stands there",
        "don't come closer",
        "[AURORA_STACK] degradation cycle: active"
    };

    private static final Random rng = new Random();

    private static long lastZoneTick     = 0;
    private static long lastMessageTick  = 0;
    private static long lastSilenceTick  = 0;
    private static boolean entryDone     = false;

    // Активные зоны: центр + тик исчезновения
    private record NullZoneData(BlockPos center, long expireAt, UUID shadowId) {}
    private static final List<NullZoneData> activeZones = new ArrayList<>();

    // Блоки которые заменяем в зоне
    private static final BlockState[] DEATH_BLOCKS = {
        Blocks.COARSE_DIRT.defaultBlockState(),
        Blocks.GRAVEL.defaultBlockState(),
        Blocks.DEAD_BUSH.defaultBlockState()
    };

    public static void tick(MinecraftServer server, long ticks) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        if (!entryDone) {
            onEntry(server, players);
            entryDone = true;
        }

        tickSpawnZone(server, players, ticks);
        tickActiveZones(server, players, ticks);
        tickMessages(players, ticks);
        tickSilence(players, ticks);
    }

    // ── Вход в этап ─────────────────────────────────────────────────────────

    private static void onEntry(MinecraftServer server, List<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal(
                "[SYSTEM] NULL ZONE formed. Life processes suspended in sector.")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withBold(true)));
            p.sendSystemMessage(Component.literal(
                "[AURORA_STACK] degradation cycle: active")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
        }
        Forgotten404EndlessLoop.LOGGER.info("[ORIGIN LOOP] Stage 5: Null Zone active.");
    }

    // ── 1. Спавн нулевой зоны ───────────────────────────────────────────────

    private static void tickSpawnZone(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (ticks - lastZoneTick < 1800) return; // каждые 1.5 мин
        lastZoneTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        ServerLevel level = server.getLevel(target.level().dimension());
        if (level == null) return;

        // Центр зоны — 10–25 блоков от игрока
        int angle = rng.nextInt(360);
        double rad = Math.toRadians(angle);
        int dist = 10 + rng.nextInt(16);
        int cx = (int)(target.getX() + Math.cos(rad) * dist);
        int cz = (int)(target.getZ() + Math.sin(rad) * dist);
        int cy = level.getHeight(
            net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, cx, cz);
        BlockPos center = new BlockPos(cx, cy, cz);

        // Убиваем растительность в зоне
        applyNullZone(level, center);

        // Замораживаем мобов
        freezeMobs(level, center);

        // Ночью — спавним тень (скелет-невидимку)
        UUID shadowId = null;
        long dayTime = level.getDayTime() % 24000;
        boolean isNight = dayTime > 13000 && dayTime < 23000;
        if (isNight) {
            shadowId = spawnShadow(level, center);
        }

        // Зона активна 2 минуты
        long expireAt = ticks + 2400;
        activeZones.add(new NullZoneData(center, expireAt, shadowId));

        // Уведомляем игрока
        target.sendSystemMessage(Component.literal(
            "[SYSTEM] null sector detected — " + dist + " blocks away")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withItalic(true)));

        Forgotten404EndlessLoop.LOGGER.info("[NULL ZONE] Zone spawned at {}", center);
    }

    // ── Применяем эффекты нулевой зоны ──────────────────────────────────────

    private static void applyNullZone(ServerLevel level, BlockPos center) {
        for (int x = -ZONE_RADIUS; x <= ZONE_RADIUS; x++) {
            for (int z = -ZONE_RADIUS; z <= ZONE_RADIUS; z++) {
                if (x * x + z * z > ZONE_RADIUS * ZONE_RADIUS) continue;

                for (int y = -3; y <= 3; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    // Убиваем растительность
                    if (state.is(Blocks.GRASS_BLOCK)) {
                        level.setBlock(pos, Blocks.COARSE_DIRT.defaultBlockState(), 3);
                    } else if (state.is(Blocks.SHORT_GRASS) || state.is(Blocks.GRASS)
                            || state.is(Blocks.TALL_GRASS)
                            || state.is(Blocks.FERN)
                            || state.is(Blocks.LARGE_FERN)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    } else if (isFlower(state)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static boolean isFlower(BlockState state) {
        return state.is(Blocks.DANDELION) || state.is(Blocks.POPPY)
            || state.is(Blocks.BLUE_ORCHID) || state.is(Blocks.ALLIUM)
            || state.is(Blocks.CORNFLOWER) || state.is(Blocks.OXEYE_DAISY)
            || state.is(Blocks.AZURE_BLUET) || state.is(Blocks.LILY_OF_THE_VALLEY);
    }

    // ── Замораживаем мобов ───────────────────────────────────────────────────

    private static void freezeMobs(ServerLevel level, BlockPos center) {
        AABB box = new AABB(
            center.getX() - ZONE_RADIUS, center.getY() - 5, center.getZ() - ZONE_RADIUS,
            center.getX() + ZONE_RADIUS, center.getY() + 5, center.getZ() + ZONE_RADIUS
        );

        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, box);
        for (Mob mob : mobs) {
            mob.setNoAi(true); // замораживаем AI
            mob.setInvisible(false);
        }
    }

    // ── Тень — скелет-невидимка ──────────────────────────────────────────────

    private static UUID spawnShadow(ServerLevel level, BlockPos center) {
        try {
            Skeleton skeleton = EntityType.SKELETON.create(level);
            if (skeleton == null) return null;

            skeleton.moveTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0, 0);
            skeleton.setNoAi(true);
            skeleton.setInvisible(false);
            skeleton.setSilent(true);
            // Делаем его тёмным через эффект — нет, просто он стоит неподвижно
            // Тень исчезает при приближении игрока (проверяется в tickActiveZones)

            level.addFreshEntity(skeleton);
            Forgotten404EndlessLoop.LOGGER.info("[NULL ZONE] Shadow spawned at {}", center);
            return skeleton.getUUID();

        } catch (Exception e) {
            Forgotten404EndlessLoop.LOGGER.error("[NULL ZONE] Shadow spawn error: {}", e.getMessage());
            return null;
        }
    }

    // ── Обработка активных зон ───────────────────────────────────────────────

    private static void tickActiveZones(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        activeZones.removeIf(zone -> {
            // Истекло время зоны
            if (ticks >= zone.expireAt()) {
                // Убираем тень
                removeShadow(server, zone);
                return true;
            }

            ServerLevel level = server.overworld();

            // Проверяем: если игрок приближается к тени — тает
            if (zone.shadowId() != null) {
                for (ServerPlayer p : players) {
                    double dx = p.getX() - zone.center().getX();
                    double dz = p.getZ() - zone.center().getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);

                    if (dist < 5) {
                        // Тень тает
                        removeShadow(server, zone);
                        p.sendSystemMessage(Component.literal(
                            "he is gone")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
                        return true;
                    }
                }
            }

            // Каждые 100 тиков продолжаем убивать растительность (новые блоки могли появиться)
            if (ticks % 100 == 0) {
                freezeMobs(level, zone.center());
            }

            return false;
        });
    }

    private static void removeShadow(MinecraftServer server, NullZoneData zone) {
        if (zone.shadowId() == null) return;
        ServerLevel level = server.overworld();
        Entity entity = level.getEntity(zone.shadowId());
        if (entity != null) entity.discard();
    }

    // ── 3. Тишина — глушим звуки ────────────────────────────────────────────

    private static void tickSilence(List<ServerPlayer> players, long ticks) {
        if (ticks - lastSilenceTick < 600) return; // раз в 30 сек
        lastSilenceTick = ticks;

        // Отправляем очень тихий низкочастотный гул — "тишина"
        for (ServerPlayer p : players) {
            p.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMBIENT_CAVE),
                SoundSource.AMBIENT,
                p.getX(), p.getY(), p.getZ(),
                0.15f, 0.3f,
                rng.nextLong()
            ));
        }
    }

    // ── 4. Сообщения ────────────────────────────────────────────────────────

    private static void tickMessages(List<ServerPlayer> players, long ticks) {
        if (ticks - lastMessageTick < 500) return; // ~25 сек
        lastMessageTick = ticks;

        String msg = ZONE_MESSAGES[rng.nextInt(ZONE_MESSAGES.length)];
        ServerPlayer target = players.get(rng.nextInt(players.size()));
        target.sendSystemMessage(Component.literal(msg)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withItalic(true)));
    }

    public static void reset() {
        entryDone       = false;
        lastZoneTick    = 0;
        lastMessageTick = 0;
        lastSilenceTick = 0;
        activeZones.clear();
    }
}
