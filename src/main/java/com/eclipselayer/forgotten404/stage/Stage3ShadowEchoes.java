package com.eclipselayer.forgotten404.stage;

import com.eclipselayer.forgotten404.Forgotten404EndlessLoop;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Stage 3 — SHADOW ECHOES
 *
 * 1. Таблички с фразами от Lucid Watcher / прошлых циклов
 *    — появляются рядом с игроком на земле или стенах
 * 2. Руины — небольшие структуры из булыжника/дерева
 *    появляются в случайных точках рядом с игроком
 * 3. Lucid Watcher шепчет в чат (особые фразы Stage 3)
 * 4. Звук ветра/шёпота вдалеке
 * 5. Небо «мерцает» — смена времени суток рывком (клиентский пакет)
 */
public class Stage3ShadowEchoes {

    private static final String[] SIGN_TEXTS = {
        "We lost.",
        "Don't trust the sky.",
        "You are not alone.",
        "Help us!",
        "Don't answer the door.",
        "He is still here.",
        "Don't look back.",
        "FIND THE CORE",
        "we were here before",
        "this is the third cycle",
        "don't trust the system",
        "Lucid: run."
    };

    private static final String[] CHAT_WHISPERS = {
        "[LUCID WATCHER] Help us!",
        "[LUCID WATCHER] You are not alone.",
        "[LUCID WATCHER] This is not the first time.",
        "[LUCID WATCHER] The ruins are memory. Don't ignore them.",
        "[LUCID WATCHER] We tried to stop this.",
        "[LUCID WATCHER] You see those structures? You built them. In another cycle.",
        "[LUCID WATCHER] Don't trust the sky.",
        "[LUCID WATCHER] AURORA_STACK will not tell you the truth."
    };

    // Простые руины — паттерны блоков относительно точки спауна
    // Каждый массив: {offsetX, offsetY, offsetZ}
    private static final int[][] RUIN_PATTERN = {
        // Пол 3x3
        {0,0,0},{1,0,0},{2,0,0},
        {0,0,1},{1,0,1},{2,0,1},
        {0,0,2},{1,0,2},{2,0,2},
        // Стены (фрагменты)
        {0,1,0},{0,2,0},
        {2,1,0},{2,2,0},
        {0,1,2},
        // Крыша (частичная)
        {0,3,0},{1,3,0},{2,3,0}
    };

    private static final Random rng = new Random();

    private static long lastSignTick   = 0;
    private static long lastRuinTick   = 0;
    private static long lastWhisperTick = 0;
    private static long lastSoundTick  = 0;
    private static long lastSkyTick    = 0;
    private static boolean entryDone   = false;

    // Позиции табличек — чтобы убрать их при выходе из этапа
    private static final List<BlockPos> placedSigns = new ArrayList<>();
    private static final List<BlockPos> placedRuins = new ArrayList<>();

    public static void tick(MinecraftServer server, long ticks) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        if (!entryDone) {
            onEntry(server, players);
            entryDone = true;
        }

        tickSigns(server, players, ticks);
        tickRuins(server, players, ticks);
        tickWhispers(players, ticks);
        tickSounds(players, ticks);
        tickSkySurge(server, players, ticks);
    }

    // ── Вход в этап ─────────────────────────────────────────────────────────

    private static void onEntry(MinecraftServer server, List<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal(
                "[LUCID WATCHER] You see the ruins? They remember you.")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA).withItalic(true)));
            p.sendSystemMessage(Component.literal(
                "[LUCID WATCHER] You were here before. In another cycle.")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA).withItalic(true)));
        }
        Forgotten404EndlessLoop.LOGGER.info("[ORIGIN LOOP] Stage 3: Shadow Echoes active.");
    }

    // ── 1. Таблички ─────────────────────────────────────────────────────────

    private static void tickSigns(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (ticks - lastSignTick < 800) return; // ~40 сек
        lastSignTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        ServerLevel level = server.getLevel(target.level().dimension());
        if (level == null) return;

        // Ищем твёрдый блок в радиусе 8 блоков
        for (int attempt = 0; attempt < 10; attempt++) {
            int bx = (int) target.getX() + rng.nextInt(17) - 8;
            int bz = (int) target.getZ() + rng.nextInt(17) - 8;
            int by = (int) target.getY();

            // Ищем поверхность вниз
            BlockPos ground = new BlockPos(bx, by, bz);
            for (int dy = 0; dy < 5; dy++) {
                BlockPos check = ground.below(dy);
                if (!level.getBlockState(check).isAir()
                    && level.getBlockState(check.above()).isAir()) {
                    ground = check.above();
                    break;
                }
            }

            if (!level.getBlockState(ground).isAir()) continue;

            // Ставим табличку на земле (oak sign)
            BlockState signState = Blocks.OAK_SIGN.defaultBlockState();
            level.setBlock(ground, signState, 3);

            if (level.getBlockEntity(ground) instanceof SignBlockEntity sign) {
                String text = SIGN_TEXTS[rng.nextInt(SIGN_TEXTS.length)];
                // Разбиваем на строки по 15 символов
                String[] lines = splitLines(text, 15);
                SignText signText = new SignText();
                for (int i = 0; i < Math.min(lines.length, 4); i++) {
                    signText = signText.setMessage(i,
                        Component.literal(lines[i])
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
                }
                sign.setText(signText, true);
                sign.setChanged();
            }

            placedSigns.add(ground);
            Forgotten404EndlessLoop.LOGGER.info("[SHADOW ECHO] Sign placed at {}", ground);
            break;
        }
    }

    // ── 2. Руины ────────────────────────────────────────────────────────────

    private static void tickRuins(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (ticks - lastRuinTick < 2400) return; // раз в 2 мин
        lastRuinTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        ServerLevel level = server.getLevel(target.level().dimension());
        if (level == null) return;

        // Руины появляются в 15–30 блоках от игрока
        int angle = rng.nextInt(360);
        double rad = Math.toRadians(angle);
        int dist = 15 + rng.nextInt(16);
        int bx = (int)(target.getX() + Math.cos(rad) * dist);
        int bz = (int)(target.getZ() + Math.sin(rad) * dist);

        // Находим поверхность
        int by = level.getHeight(
            net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
            bx, bz);

        // Строим руины из булыжника
        for (int[] offset : RUIN_PATTERN) {
            // Пропускаем 30% блоков — руина разрушена
            if (rng.nextInt(100) < 30) continue;

            BlockPos pos = new BlockPos(bx + offset[0], by + offset[1], bz + offset[2]);
            if (level.getBlockState(pos).isAir() || rng.nextBoolean()) {
                BlockState block = rng.nextInt(3) == 0
                    ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                    : Blocks.COBBLESTONE.defaultBlockState();
                level.setBlock(pos, block, 3);
                placedRuins.add(pos);
            }
        }

        Forgotten404EndlessLoop.LOGGER.info("[SHADOW ECHO] Ruins spawned at {},{}", bx, bz);

        // Уведомляем игрока
        target.sendSystemMessage(Component.literal(
            "[SYSTEM] echo structure detected — " + dist + " blocks away")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
    }

    // ── 3. Шёпот Lucid Watcher ───────────────────────────────────────────────

    private static void tickWhispers(List<ServerPlayer> players, long ticks) {
        if (ticks - lastWhisperTick < 600) return; // ~30 сек
        lastWhisperTick = ticks;

        String msg = CHAT_WHISPERS[rng.nextInt(CHAT_WHISPERS.length)];
        ServerPlayer target = players.get(rng.nextInt(players.size()));
        target.sendSystemMessage(Component.literal(msg)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA).withItalic(true)));
    }

    // ── 4. Звуки ────────────────────────────────────────────────────────────

    private static void tickSounds(List<ServerPlayer> players, long ticks) {
        if (ticks - lastSoundTick < 450) return; // ~22 сек
        lastSoundTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));

        double offsetX = (rng.nextDouble() - 0.5) * 30;
        double offsetZ = (rng.nextDouble() - 0.5) * 30;

        // Чередуем: ветер (wolf howl) / шёпот (enderman stare) / шаги
        var sounds = new net.minecraft.sounds.SoundEvent[]{
            SoundEvents.ENDERMAN_STARE,
            SoundEvents.STONE_STEP,
            SoundEvents.WOLF_HOWL
        };
        var sound = sounds[rng.nextInt(sounds.length)];

        target.connection.send(new ClientboundSoundPacket(
            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
            SoundSource.AMBIENT,
            target.getX() + offsetX,
            target.getY(),
            target.getZ() + offsetZ,
            0.4f + rng.nextFloat() * 0.4f,
            0.6f + rng.nextFloat() * 0.5f,
            rng.nextLong()
        ));
    }

    // ── 5. Рывок неба (смена времени суток) ─────────────────────────────────

    private static void tickSkySurge(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (ticks - lastSkyTick < 1200) return; // раз в минуту
        lastSkyTick = ticks;

        if (rng.nextInt(100) > 40) return; // 40% шанс

        ServerLevel overworld = server.overworld();
        long currentTime = overworld.getDayTime();
        // Рывок на 1000–3000 тиков вперёд
        long surge = 1000L + rng.nextInt(2000);
        overworld.setDayTime(currentTime + surge);

        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal(
                "[SYSTEM] time anomaly detected")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
        }
    }

    // ── Утилиты ─────────────────────────────────────────────────────────────

    private static String[] splitLines(String text, int maxLen) {
        if (text.length() <= maxLen) return new String[]{text};
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (sb.length() + w.length() + 1 > maxLen) {
                lines.add(sb.toString().trim());
                sb = new StringBuilder();
            }
            sb.append(w).append(" ");
        }
        if (!sb.isEmpty()) lines.add(sb.toString().trim());
        return lines.toArray(new String[0]);
    }

    public static void reset() {
        entryDone      = false;
        lastSignTick   = 0;
        lastRuinTick   = 0;
        lastWhisperTick = 0;
        lastSoundTick  = 0;
        lastSkyTick    = 0;
        placedSigns.clear();
        placedRuins.clear();
    }
}
