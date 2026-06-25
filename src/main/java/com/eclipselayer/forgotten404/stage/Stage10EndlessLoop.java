package com.eclipselayer.forgotten404.stage;

import com.eclipselayer.forgotten404.Forgotten404EndlessLoop;
import com.eclipselayer.forgotten404.ending.EndingManager;
import com.eclipselayer.forgotten404.ending.FragmentTracker;
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
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Stage 10 — ENDLESS LOOP
 *
 * ORIGIN_LOOP манифестируется. Это финальная стадия.
 * Игрок должен принять решение — концовка определяется его действиями.
 *
 * 1. ORIGIN_LOOP спавнится как Wither с кастомным именем
 *    (не обычный Wither — это сущность другого уровня)
 * 2. Мир физически разрушается — блоки вокруг игрока
 *    заменяются на crying obsidian / void-блоки
 * 3. Все временные линии "сходятся" — звуки всех этапов одновременно
 * 4. Финальные сообщения AURORA_STACK и ORIGIN_LOOP
 * 5. Предложение концовок — через таблички в мире
 * 6. Счётчик петель — сколько раз игрок умирал
 * 7. Сброс мира к Stage 0 если ORIGIN_LOOP убит
 *    (петля начинается заново — это и есть Endless Loop)
 */
public class Stage10EndlessLoop {

    private static final String[] ORIGIN_MESSAGES = {
        "[ORIGIN_LOOP] MANIFESTED",
        "[ORIGIN_LOOP] you know this place",
        "[ORIGIN_LOOP] you have fought me before",
        "[ORIGIN_LOOP] every death feeds the loop",
        "[ORIGIN_LOOP] there is no ending",
        "[ORIGIN_LOOP] only continuation",
        "[ORIGIN_LOOP] MEMORY SAVED",
        "[ORIGIN_LOOP] the script cannot be broken",
        "[AURORA_STACK] SCRIPT ERROR",
        "[AURORA_STACK] all versions of the world: ACTIVE",
        "FINAL",
        "[ORIGIN_LOOP] kill me. the loop restarts.",
        "[ORIGIN_LOOP] don't kill me. stay forever.",
        "[SYSTEM] reality index: 0.00",
        "ENDLESS LOOP: ACTIVE"
    };

    // Блоки разрушения мира
    private static final BlockState[] COLLAPSE_BLOCKS = {
        Blocks.CRYING_OBSIDIAN.defaultBlockState(),
        Blocks.OBSIDIAN.defaultBlockState(),
        Blocks.BLACKSTONE.defaultBlockState(),
        Blocks.SOUL_SAND.defaultBlockState(),
        Blocks.BASALT.defaultBlockState(),
        Blocks.NETHERRACK.defaultBlockState(),
    };

    private static final Random rng = new Random();

    private static long lastOriginTick   = 0;
    private static long lastCollapseTick = 0;
    private static long lastSoundTick    = 0;
    private static long lastSignTick     = 0;
    private static boolean entryDone     = false;
    private static boolean originSpawned = false;
    private static UUID originLoopId     = null;

    // Счётчик петель (смертей) — накапливается
    private static int loopCount = 0;

    public static void tick(MinecraftServer server, long ticks) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        if (!entryDone) {
            onEntry(server, players);
            entryDone = true;
        }

        tickOriginMessages(players, ticks);
        tickWorldCollapse(server, players, ticks);
        tickAllSounds(players, ticks);
        tickEndingSigns(server, players, ticks);
        tickOriginLoop(server, players, ticks);
        tickDeathLoop(server, players, ticks);
        GlitchRestoreQueue.tick(server, ticks);
    }

    // ── Вход в этап ─────────────────────────────────────────────────────────

    private static void onEntry(MinecraftServer server, List<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal("[ORIGIN_LOOP] MANIFESTED")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)));
            p.sendSystemMessage(Component.literal(
                "SCRIPT ERROR — reality unstable")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
            p.sendSystemMessage(Component.literal(
                "All cycles converge. Time to choose.")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)));

            // Звук появления — гром + wither
            p.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.WITHER_SPAWN),
                SoundSource.MASTER,
                p.getX(), p.getY(), p.getZ(),
                1.0f, 0.3f, rng.nextLong()
            ));
            p.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.LIGHTNING_BOLT_THUNDER),
                SoundSource.MASTER,
                p.getX(), p.getY(), p.getZ(),
                1.0f, 0.5f, rng.nextLong()
            ));
        }

        Forgotten404EndlessLoop.LOGGER.info("[ORIGIN LOOP] Stage 10: Endless Loop — ORIGIN_LOOP manifested.");
    }

    // ── 1. ORIGIN_LOOP спавн (Wither с кастомным именем) ────────────────────

    private static void tickOriginLoop(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (originSpawned) {
            // Проверяем — убит ли ORIGIN_LOOP
            if (originLoopId != null) {
                ServerLevel level = server.overworld();
                var entity = level.getEntity(originLoopId);
                if (entity == null || !entity.isAlive()) {
                    // ORIGIN_LOOP убит — петля перезапускается
                    onOriginLoopKilled(server, players);
                    originLoopId = null;
                }
            }
            return;
        }
        if (ticks < 200) return; // через 10 сек после входа
        originSpawned = true;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        ServerLevel level = server.getLevel(target.level().dimension());
        if (level == null) return;

        // Спавним в 30 блоках от игрока
        double spawnX = target.getX() + 30;
        double spawnZ = target.getZ();
        int spawnY = level.getHeight(
            net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
            (int) spawnX, (int) spawnZ) + 5;

        try {
            WitherBoss wither = EntityType.WITHER.create(level);
            if (wither == null) return;

            wither.moveTo(spawnX, spawnY, spawnZ, 0, 0);
            wither.setCustomName(Component.literal("ORIGIN_LOOP")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withBold(true)));
            wither.setCustomNameVisible(true);
            // Максимальное здоровье — это не обычный Wither
            wither.getAttribute(
                net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH
            ).setBaseValue(600.0);
            wither.setHealth(600.0f);

            level.addFreshEntity(wither);
            originLoopId = wither.getUUID();

            for (ServerPlayer p : players) {
                p.sendSystemMessage(Component.literal(
                    "[ORIGIN_LOOP] kill me. the loop restarts.")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withItalic(true)));
                p.sendSystemMessage(Component.literal(
                    "[ORIGIN_LOOP] don't kill me. stay forever.")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withItalic(true)));
            }

            Forgotten404EndlessLoop.LOGGER.info("[STAGE 10] ORIGIN_LOOP spawned at {},{},{}", spawnX, spawnY, spawnZ);
        } catch (Exception e) {
            Forgotten404EndlessLoop.LOGGER.error("[STAGE 10] ORIGIN_LOOP spawn error: {}", e.getMessage());
        }
    }

    // ORIGIN_LOOP убит — петля начинается заново
    private static void _unused_onOriginLoopKilled(MinecraftServer server, List<ServerPlayer> players) {
        loopCount++;
        Forgotten404EndlessLoop.LOGGER.info("[STAGE 10] ORIGIN_LOOP killed. Loop #{} begins.", loopCount);

        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal(
                "[ORIGIN_LOOP] MEMORY SAVED")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)));
            p.sendSystemMessage(Component.literal(
                "[AURORA_STACK] loop #" + loopCount + " initiated")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
            p.sendSystemMessage(Component.literal(
                "the world resets. you remember nothing.")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withItalic(true)));
        }

        // Сбрасываем StageManager обратно в Stage 0
        Forgotten404EndlessLoop.STAGE_MANAGER.resetToStage0(loopCount);
        reset();
    }

    // ── 2. Разрушение мира ───────────────────────────────────────────────────

    private static void tickWorldCollapse(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (ticks - lastCollapseTick < 60) return; // каждые 3 сек
        lastCollapseTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        ServerLevel level = server.getLevel(target.level().dimension());
        if (level == null) return;

        // 5–10 блоков рядом превращаются в блоки разрушения
        int count = 5 + rng.nextInt(6);
        for (int i = 0; i < count; i++) {
            int bx = (int) target.getX() + rng.nextInt(25) - 12;
            int by = (int) target.getY() + rng.nextInt(5) - 2;
            int bz = (int) target.getZ() + rng.nextInt(25) - 12;
            BlockPos pos = new BlockPos(bx, by, bz);

            BlockState current = level.getBlockState(pos);
            if (current.isAir() || current.is(Blocks.BEDROCK)) continue;
            if (current.is(Blocks.CRYING_OBSIDIAN)) continue;

            level.setBlock(pos, COLLAPSE_BLOCKS[rng.nextInt(COLLAPSE_BLOCKS.length)], 3);
        }
    }

    // ── 3. Звуки всех этапов одновременно ───────────────────────────────────

    private static void tickAllSounds(List<ServerPlayer> players, long ticks) {
        if (ticks - lastSoundTick < 80) return; // каждые 4 сек
        lastSoundTick = ticks;

        var sounds = new net.minecraft.sounds.SoundEvent[]{
            SoundEvents.WITHER_AMBIENT,
            SoundEvents.ENDERMAN_STARE,
            SoundEvents.ELDER_GUARDIAN_AMBIENT,
            SoundEvents.LIGHTNING_BOLT_THUNDER,
            SoundEvents.WITHER_HURT,
            SoundEvents.ENDERMAN_SCREAM,
            SoundEvents.GUARDIAN_AMBIENT
        };

        for (ServerPlayer p : players) {
            // Несколько звуков одновременно
            for (int i = 0; i < 2; i++) {
                p.connection.send(new ClientboundSoundPacket(
                    BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sounds[rng.nextInt(sounds.length)]),
                    SoundSource.MASTER,
                    p.getX() + rng.nextInt(20) - 10,
                    p.getY(),
                    p.getZ() + rng.nextInt(20) - 10,
                    0.8f, 0.4f + rng.nextFloat() * 0.8f,
                    rng.nextLong()
                ));
            }
        }
    }

    // ── 4. Таблички с концовками ─────────────────────────────────────────────

    private static void tickEndingSigns(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (ticks - lastSignTick < 1200) return;
        lastSignTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        ServerLevel level = server.getLevel(target.level().dimension());
        if (level == null) return;

        // Таблички с намёками на концовки
        String[][] endingHints = {
            { "KILL", "ORIGIN_LOOP", "the loop", "restarts" },
            { "DON'T", "FIGHT IT", "stay here", "forever" },
            { "FIND", "THE CORE", "before it", "finds you" },
            { "ACCEPT", "THE LOOP", "you are", "instance 404" },
            { "BREAK", "THE SCRIPT", "if you can", "???" },
        };

        String[] hint = endingHints[rng.nextInt(endingHints.length)];

        // Ищем поверхность рядом
        for (int attempt = 0; attempt < 10; attempt++) {
            int bx = (int) target.getX() + rng.nextInt(13) - 6;
            int bz = (int) target.getZ() + rng.nextInt(13) - 6;
            int by = (int) target.getY();

            for (int dy = 0; dy < 5; dy++) {
                BlockPos check = new BlockPos(bx, by - dy, bz);
                if (!level.getBlockState(check).isAir()
                    && level.getBlockState(check.above()).isAir()) {
                    BlockPos signPos = check.above();
                    level.setBlock(signPos, Blocks.CRIMSON_SIGN.defaultBlockState(), 3);
                    if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
                        SignText st = new SignText();
                        for (int i = 0; i < 4; i++)
                            st = st.setMessage(i, Component.literal(hint[i])
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)));
                        sign.setText(st, true);
                        sign.setChanged();
                    }
                    break;
                }
            }
            break;
        }
    }

    // ── 5. Счётчик смертей — петля ───────────────────────────────────────────

    private static void tickDeathLoop(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        // Каждые 5 мин напоминаем о счётчике петель
        if (ticks % 6000 != 0 || loopCount == 0) return;

        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal(
                "[ORIGIN_LOOP] loop #" + loopCount + " — you remember")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withItalic(true)));
        }
    }

    // ── 6. Сообщения ORIGIN_LOOP ─────────────────────────────────────────────

    private static void tickOriginMessages(List<ServerPlayer> players, long ticks) {
        if (ticks - lastOriginTick < 150) return; // каждые 7 сек
        lastOriginTick = ticks;

        String msg = ORIGIN_MESSAGES[rng.nextInt(ORIGIN_MESSAGES.length)];
        boolean isCritical = msg.contains("FINAL") || msg.contains("ENDLESS")
            || msg.contains("ERROR") || msg.contains("0.00");

        Component component = Component.literal(msg)
            .withStyle(Style.EMPTY
                .withColor(isCritical ? ChatFormatting.RED : ChatFormatting.DARK_RED)
                .withBold(isCritical));

        for (ServerPlayer p : players) p.sendSystemMessage(component);
    }

    public static void reset() {
        entryDone     = false;
        originSpawned = false;
        originLoopId  = null;
        lastOriginTick   = 0;
        lastCollapseTick = 0;
        lastSoundTick    = 0;
        lastSignTick     = 0;
    }

    public static int getLoopCount() { return loopCount; }
}
