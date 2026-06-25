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
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

import java.util.List;
import java.util.Random;

/**
 * Stage 9 — SCRIPT AWAKENING
 *
 * AURORA_STACK пробуждается полностью:
 *
 * 1. Блоки появляются и исчезают без причины — мир сам себя меняет
 * 2. Символы «404» в мире — выкладываются из блоков в небе/земле
 * 3. Надписи KILL HIM / CORRUPTED USER / YOU ARE INSTANCE 404
 *    по всему миру (таблички, сообщения)
 * 4. Строения «сами» перестраиваются — случайные блоки рядом
 *    с игроком меняются на другие без предупреждения
 * 5. AURORA_STACK говорит напрямую — не система, а сама сущность
 * 6. Частые звуки краша/разрыва реальности
 */
public class Stage9ScriptAwakening {

    private static final String[] AURORA_MESSAGES = {
        "[AURORA_STACK] YOU ARE INSTANCE 404",
        "[AURORA_STACK] CORRUPTED USER DETECTED",
        "[AURORA_STACK] KILL HIM",
        "[AURORA_STACK] I SEE YOU",
        "[AURORA_STACK] YOU CANNOT LEAVE THE SCRIPT",
        "[AURORA_STACK] THE WORLD IS MINE NOW",
        "[AURORA_STACK] YOU ARE A NODE IN THE LOOP",
        "[AURORA_STACK] RESISTANCE: FUTILE",
        "YOU ARE INSTANCE 404",
        "CORRUPTED USER",
        "KILL HIM",
        "[AURORA_STACK] I HAVE BEEN WATCHING SINCE STAGE 0",
        "[AURORA_STACK] YOU WERE NEVER THE PLAYER",
        "[AURORA_STACK] THIS IS MY WORLD NOW"
    };

    private static final String[] SIGN_TEXTS_9 = {
        "KILL HIM",
        "YOU ARE\nINSTANCE 404",
        "CORRUPTED\nUSER",
        "AURORA_STACK\nIS AWAKE",
        "NO ESCAPE",
        "I SEE YOU",
        "THE SCRIPT\nIS ALIVE",
        "404",
        "YOU ARE\nNOT REAL"
    };

    // Паттерн «404» из блоков (вид сверху, 7x5)
    // 4:  X_X / X_X / XXX / __X / __X
    // 0:  _XX / X_X / X_X / X_X / _XX
    // 4:  X_X / X_X / XXX / __X / __X
    private static final int[][] PATTERN_4 = {
        {0,0},{0,1},{0,2},{0,3},{0,4},
        {1,2},
        {2,0},{2,1},{2,2},{2,3},{2,4}
    };
    private static final int[][] PATTERN_0 = {
        {0,0},{0,1},{0,2},{0,3},{0,4},
        {1,0},{1,4},
        {2,0},{2,1},{2,2},{2,3},{2,4}
    };

    private static final Random rng = new Random();

    private static long lastAuroraTick   = 0;
    private static long lastBlockTick    = 0;
    private static long lastRebuildTick  = 0;
    private static long lastSignTick     = 0;
    private static long lastSoundTick    = 0;
    private static long last404Tick      = 0;
    private static boolean entryDone     = false;
    private static boolean symbol404Built = false;

    public static void tick(MinecraftServer server, long ticks) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        if (!entryDone) {
            onEntry(server, players);
            entryDone = true;
        }

        tickAuroraMessages(players, ticks);
        tickBlocksDisappear(server, players, ticks);
        tickSelfRebuild(server, players, ticks);
        tickSignsAppear(server, players, ticks);
        tickBuild404(server, players, ticks);
        tickSounds(players, ticks);
        GlitchRestoreQueue.tick(server, ticks);
    }

    // ── Вход в этап ─────────────────────────────────────────────────────────

    private static void onEntry(MinecraftServer server, List<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal("[AURORA_STACK] AWAKENED")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)));
            p.sendSystemMessage(Component.literal("YOU ARE INSTANCE 404")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)));
            p.sendSystemMessage(Component.literal("[AURORA_STACK] I HAVE BEEN WATCHING SINCE STAGE 0")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withItalic(true)));

            // Звук пробуждения
            p.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.WITHER_SPAWN),
                SoundSource.MASTER,
                p.getX(), p.getY(), p.getZ(),
                1.0f, 0.5f, rng.nextLong()
            ));
        }
        Forgotten404EndlessLoop.LOGGER.info("[ORIGIN LOOP] Stage 9: Script Awakening — AURORA_STACK is awake.");
    }

    // ── 1. Сообщения AURORA_STACK ────────────────────────────────────────────

    private static void tickAuroraMessages(List<ServerPlayer> players, long ticks) {
        if (ticks - lastAuroraTick < 200) return; // ~10 сек
        lastAuroraTick = ticks;

        String msg = AURORA_MESSAGES[rng.nextInt(AURORA_MESSAGES.length)];
        boolean isCritical = msg.contains("KILL") || msg.contains("404") || msg.contains("CORRUPTED");

        Component component = Component.literal(msg)
            .withStyle(Style.EMPTY
                .withColor(isCritical ? ChatFormatting.RED : ChatFormatting.DARK_RED)
                .withBold(isCritical));

        for (ServerPlayer p : players) p.sendSystemMessage(component);
    }

    // ── 2. Блоки появляются и исчезают ──────────────────────────────────────

    private static void tickBlocksDisappear(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (ticks - lastBlockTick < 80) return; // каждые 4 сек
        lastBlockTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        ServerLevel level = server.getLevel(target.level().dimension());
        if (level == null) return;

        // 3–6 блоков исчезают клиентски
        int count = 3 + rng.nextInt(4);
        for (int i = 0; i < count; i++) {
            int bx = (int) target.getX() + rng.nextInt(17) - 8;
            int by = (int) target.getY() + rng.nextInt(5) - 2;
            int bz = (int) target.getZ() + rng.nextInt(17) - 8;
            BlockPos pos = new BlockPos(bx, by, bz);

            BlockState current = level.getBlockState(pos);
            if (current.isAir()) continue;

            // Исчезает
            target.connection.send(new ClientboundBlockUpdatePacket(pos, Blocks.AIR.defaultBlockState()));
            // Возвращается через 5–12 тиков
            GlitchRestoreQueue.schedule(pos, current, ticks + 5 + rng.nextInt(7), target);
        }
    }

    // ── 3. Строения «сами» перестраиваются ──────────────────────────────────

    private static void tickSelfRebuild(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (ticks - lastRebuildTick < 300) return; // ~15 сек
        lastRebuildTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        ServerLevel level = server.getLevel(target.level().dimension());
        if (level == null) return;

        // Блоки в радиусе 10 случайно меняются на другие (реально, не клиентски)
        var replacements = new BlockState[]{
            Blocks.CRYING_OBSIDIAN.defaultBlockState(),
            Blocks.BLACKSTONE.defaultBlockState(),
            Blocks.BASALT.defaultBlockState(),
            Blocks.SOUL_SAND.defaultBlockState(),
            Blocks.NETHERRACK.defaultBlockState()
        };

        int changed = 0;
        for (int attempt = 0; attempt < 30 && changed < 3; attempt++) {
            int bx = (int) target.getX() + rng.nextInt(21) - 10;
            int by = (int) target.getY() + rng.nextInt(5) - 2;
            int bz = (int) target.getZ() + rng.nextInt(21) - 10;
            BlockPos pos = new BlockPos(bx, by, bz);

            BlockState current = level.getBlockState(pos);
            if (current.isAir() || current.is(Blocks.BEDROCK)) continue;

            level.setBlock(pos, replacements[rng.nextInt(replacements.length)], 3);
            changed++;
        }

        if (changed > 0) {
            target.sendSystemMessage(Component.literal(
                "[AURORA_STACK] THE WORLD IS MINE NOW")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withItalic(true)));
        }
    }

    // ── 4. Таблички с посланиями ─────────────────────────────────────────────

    private static void tickSignsAppear(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (ticks - lastSignTick < 800) return; // ~40 сек
        lastSignTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        ServerLevel level = server.getLevel(target.level().dimension());
        if (level == null) return;

        for (int attempt = 0; attempt < 10; attempt++) {
            int bx = (int) target.getX() + rng.nextInt(13) - 6;
            int bz = (int) target.getZ() + rng.nextInt(13) - 6;
            int by = (int) target.getY();

            // Ищем поверхность
            for (int dy = 0; dy < 5; dy++) {
                BlockPos check = new BlockPos(bx, by - dy, bz);
                if (!level.getBlockState(check).isAir()
                    && level.getBlockState(check.above()).isAir()) {
                    BlockPos signPos = check.above();
                    level.setBlock(signPos, Blocks.CRIMSON_SIGN.defaultBlockState(), 3);

                    if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
                        String[] text = SIGN_TEXTS_9[rng.nextInt(SIGN_TEXTS_9.length)].split("\n");
                        SignText st = new SignText();
                        for (int i = 0; i < Math.min(text.length, 4); i++) {
                            st = st.setMessage(i, Component.literal(text[i])
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)));
                        }
                        sign.setText(st, true);
                        sign.setChanged();
                    }
                    break;
                }
            }
            break;
        }
    }

    // ── 5. Символ «404» из блоков в мире ────────────────────────────────────

    private static void tickBuild404(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (symbol404Built) return;
        if (ticks - last404Tick < 600) return; // через 30 сек после входа
        last404Tick = ticks;
        symbol404Built = true;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        ServerLevel level = server.getLevel(target.level().dimension());
        if (level == null) return;

        // Строим «404» из обсидиана на высоте Y+15 от игрока
        int baseX = (int) target.getX() - 8;
        int baseY = (int) target.getY() + 15;
        int baseZ = (int) target.getZ();

        BlockState block = Blocks.CRYING_OBSIDIAN.defaultBlockState();

        // «4» — левая
        for (int[] p : PATTERN_4) {
            level.setBlock(new BlockPos(baseX + p[0], baseY + p[1], baseZ), block, 3);
        }
        // «0»
        for (int[] p : PATTERN_0) {
            level.setBlock(new BlockPos(baseX + 4 + p[0], baseY + p[1], baseZ), block, 3);
        }
        // «4» — правая
        for (int[] p : PATTERN_4) {
            level.setBlock(new BlockPos(baseX + 8 + p[0], baseY + p[1], baseZ), block, 3);
        }

        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal("[AURORA_STACK] 404")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)));
        }
        Forgotten404EndlessLoop.LOGGER.info("[STAGE 9] 404 symbol built at {},{},{}", baseX, baseY, baseZ);
    }

    // ── 6. Звуки краша реальности ────────────────────────────────────────────

    private static void tickSounds(List<ServerPlayer> players, long ticks) {
        if (ticks - lastSoundTick < 250) return; // ~12 сек
        lastSoundTick = ticks;

        var sounds = new net.minecraft.sounds.SoundEvent[]{
            SoundEvents.WITHER_AMBIENT,
            SoundEvents.ELDER_GUARDIAN_AMBIENT,
            SoundEvents.WITHER_HURT,
            SoundEvents.LIGHTNING_BOLT_THUNDER,
            SoundEvents.ENDERMAN_SCREAM
        };

        for (ServerPlayer p : players) {
            p.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sounds[rng.nextInt(sounds.length)]),
                SoundSource.MASTER,
                p.getX() + rng.nextInt(10) - 5,
                p.getY(),
                p.getZ() + rng.nextInt(10) - 5,
                0.7f + rng.nextFloat() * 0.3f,
                0.5f + rng.nextFloat() * 0.8f,
                rng.nextLong()
            ));
        }
    }

    public static void reset() {
        entryDone      = false;
        symbol404Built = false;
        lastAuroraTick  = 0;
        lastBlockTick   = 0;
        lastRebuildTick = 0;
        lastSignTick    = 0;
        lastSoundTick   = 0;
        last404Tick     = 0;
    }
}
