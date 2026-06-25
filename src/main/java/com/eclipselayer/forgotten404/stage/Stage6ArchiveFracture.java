package com.eclipselayer.forgotten404.stage;

import com.eclipselayer.forgotten404.Forgotten404EndlessLoop;
import com.eclipselayer.forgotten404.ending.FragmentTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Stage 6 — ARCHIVE FRACTURE
 *
 * 1. Фрагменты старых построек — небольшие руины со сломанными блоками
 *    рассыпаны по миру (чаще и разнообразнее чем Stage 3)
 * 2. Сундуки с артефактами — книги с фразами о событиях прошлых циклов,
 *    незнакомые предметы, обугленные таблички
 * 3. Книги-записки — написанные "прошлыми игроками", содержат пророчества
 * 4. Никнеймы из альтернативных реальностей — в чат попадают имена
 *    несуществующих игроков
 * 5. Сообщения о прорыве архива
 */
public class Stage6ArchiveFracture {

    // Фразы в книгах — от "прошлых игроков"
    private static final String[][] BOOK_PAGES = {
        {
            "I found it.\nThe core.\nDo not touch it.\n\n— prev_instance_03"
        },
        {
            "Day 1: world stable.\nDay 4: chunks wrong.\nDay 7: I saw myself.\n\n— loop_walker"
        },
        {
            "AURORA_STACK told me\nit was a dream.\nIt wasn't.\nRun.\n\n— deleted_user_404"
        },
        {
            "The ruins were mine.\nI built them.\nI don't remember when.\n\n— echo_player"
        },
        {
            "If you read this:\nDO NOT RESTART.\nThe cycle will\nnever end.\n\n— instance_001"
        },
        {
            "I tried to leave.\nCoordinates looped.\nI am still here.\nSo are you.\n\n— trapped_07"
        },
        {
            "Lucid Watcher lied.\nThe core is a trap.\nDon't find it.\n\n— ???"
        },
        {
            "This is not your\nfirst time here.\nYou just don't\nremember yet.\n\n— ORIGIN_LOOP"
        }
    };

    // Никнеймы из "альтернативных реальностей"
    private static final String[] ALT_NICKNAMES = {
        "prev_instance_03", "loop_walker", "deleted_user_404",
        "echo_player_7", "instance_001", "trapped_07",
        "null_entity", "void_runner", "archive_ghost",
        "cycle_remnant", "forgotten_404", "lost_instance"
    };

    // Сообщения о прорыве
    private static final String[] BREACH_MESSAGES = {
        "[ARCHIVE] BREACH DETECTED — previous cycle data leaking into current instance",
        "[ARCHIVE] whose structures are these?",
        "[ARCHIVE] memory fragment: recovered",
        "[ARCHIVE] KEEP PLAYING",
        "[ARCHIVE] DON'T TRUST THEM",
        "[ARCHIVE] data from cycle #%d corrupted",
        "[AURORA_STACK] archive integrity: 3%%",
        "[SYSTEM] ghost data: manifesting"
    };

    // Паттерны фрагментов построек (разнообразнее чем Stage 3)
    private static final int[][][] RUIN_PATTERNS = {
        // Небольшая башня
        { {0,0,0},{1,0,0},{0,1,0},{1,1,0},{0,2,0},{1,2,0},{0,0,1},{1,0,1},{0,1,1},{1,1,1} },
        // Фрагмент стены
        { {0,0,0},{0,1,0},{0,2,0},{1,0,0},{1,1,0},{2,0,0},{2,1,0},{2,2,0} },
        // Пол со стенами
        { {0,0,0},{1,0,0},{2,0,0},{0,0,1},{2,0,1},{0,0,2},{1,0,2},{2,0,2},
          {0,1,0},{0,1,2},{2,1,0},{2,1,2} },
        // Лестница
        { {0,0,0},{1,1,0},{2,2,0},{3,3,0},{0,0,1},{1,1,1} }
    };

    private static final Random rng = new Random();

    private static long lastRuinTick    = 0;
    private static long lastChestTick   = 0;
    private static long lastBreachTick  = 0;
    private static long lastNickTick    = 0;
    private static boolean entryDone    = false;

    public static void tick(MinecraftServer server, long ticks) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        if (!entryDone) {
            onEntry(server, players);
            entryDone = true;
        }

        tickRuins(server, players, ticks);
        tickChests(server, players, ticks);
        tickBreachMessages(players, ticks);
        tickAltNicknames(players, ticks);
    }

    // ── Вход в этап ─────────────────────────────────────────────────────────

    private static void onEntry(MinecraftServer server, List<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal(
                "[ARCHIVE] BREACH DETECTED — previous cycle data leaking into current instance")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withBold(true)));
            p.sendSystemMessage(Component.literal(
                "[AURORA_STACK] archive integrity: 3%")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
        }
        Forgotten404EndlessLoop.LOGGER.info("[ORIGIN LOOP] Stage 6: Archive Fracture active.");
    }

    // ── 1. Фрагменты построек ───────────────────────────────────────────────

    private static void tickRuins(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (ticks - lastRuinTick < 1200) return; // раз в минуту
        lastRuinTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        ServerLevel level = server.getLevel(target.level().dimension());
        if (level == null) return;

        int angle = rng.nextInt(360);
        double rad = Math.toRadians(angle);
        int dist = 20 + rng.nextInt(30);
        int bx = (int)(target.getX() + Math.cos(rad) * dist);
        int bz = (int)(target.getZ() + Math.sin(rad) * dist);
        int by = level.getHeight(
            net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, bx, bz);

        int[][] pattern = RUIN_PATTERNS[rng.nextInt(RUIN_PATTERNS.length)];

        // Блоки архивных руин — разнообразнее
        var blocks = new net.minecraft.world.level.block.state.BlockState[]{
            Blocks.COBBLESTONE.defaultBlockState(),
            Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
            Blocks.CRACKED_STONE_BRICKS.defaultBlockState(),
            Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
            Blocks.STONE_BRICKS.defaultBlockState(),
            Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
        };

        for (int[] offset : pattern) {
            if (rng.nextInt(100) < 25) continue; // 25% пропуск — руина
            BlockPos pos = new BlockPos(bx + offset[0], by + offset[1], bz + offset[2]);
            var block = blocks[rng.nextInt(blocks.length)];
            level.setBlock(pos, block, 3);
        }

        // Иногда добавляем табличку с именем "прошлого игрока"
        if (rng.nextBoolean()) {
            BlockPos signPos = new BlockPos(bx, by + 1, bz + 1);
            if (level.getBlockState(signPos).isAir()) {
                level.setBlock(signPos, Blocks.OAK_SIGN.defaultBlockState(), 3);
                if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
                    String nick = ALT_NICKNAMES[rng.nextInt(ALT_NICKNAMES.length)];
                    SignText text = new SignText()
                        .setMessage(0, Component.literal("was here")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)))
                        .setMessage(1, Component.literal(nick)
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
                    sign.setText(text, true);
                    sign.setChanged();
                }
            }
        }

        target.sendSystemMessage(Component.literal(
            "[ARCHIVE] echo structure materialized — " + dist + " blocks away")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withItalic(true)));
    }

    // ── 2. Сундуки с артефактами ────────────────────────────────────────────

    private static void tickChests(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        if (ticks - lastChestTick < 2400) return; // каждые 2 мин
        lastChestTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        ServerLevel level = server.getLevel(target.level().dimension());
        if (level == null) return;

        int dist = 8 + rng.nextInt(12);
        int angle = rng.nextInt(360);
        int bx = (int)(target.getX() + Math.cos(Math.toRadians(angle)) * dist);
        int bz = (int)(target.getZ() + Math.sin(Math.toRadians(angle)) * dist);
        int by = level.getHeight(
            net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, bx, bz);

        BlockPos chestPos = new BlockPos(bx, by, bz);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);

        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            // 30% шанс что в сундуке — фрагмент архива
            ItemStack book;
            if (rng.nextInt(100) < 30) {
                int fragId = rng.nextInt(7) + 1;
                book = FragmentTracker.createFragment(fragId);
            } else {
                book = createBook(BOOK_PAGES[rng.nextInt(BOOK_PAGES.length)]);
            }
            chest.setItem(0, book);

            // Случайные "артефакты" — обычные предметы, но контекст делает их жуткими
            var artifacts = new ItemStack[]{
                new ItemStack(Items.COMPASS),
                new ItemStack(Items.CLOCK),
                new ItemStack(Items.MAP),
                new ItemStack(Items.TORCH, 1 + rng.nextInt(8)),
                new ItemStack(Items.BONE, 1 + rng.nextInt(4)),
                new ItemStack(Items.PAPER)
            };
            int slot = 1;
            for (ItemStack artifact : artifacts) {
                if (rng.nextBoolean() && slot < 27) {
                    chest.setItem(slot++, artifact);
                }
            }
        }

        target.sendSystemMessage(Component.literal(
            "[ARCHIVE] memory container detected nearby")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withItalic(true)));
        target.sendSystemMessage(Component.literal(
            "[ARCHIVE] contents: unknown")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
    }

    // ── 3. Никнеймы из альтернативных реальностей ───────────────────────────

    private static void tickAltNicknames(List<ServerPlayer> players, long ticks) {
        if (ticks - lastNickTick < 900) return; // ~45 сек
        lastNickTick = ticks;

        String nick = ALT_NICKNAMES[rng.nextInt(ALT_NICKNAMES.length)];
        ServerPlayer target = players.get(rng.nextInt(players.size()));

        // Имитируем системное сообщение "от другого игрока"
        target.sendSystemMessage(Component.literal(
            "[ARCHIVE] ghost instance detected: " + nick)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
    }

    // ── 4. Сообщения о прорыве ──────────────────────────────────────────────

    private static void tickBreachMessages(List<ServerPlayer> players, long ticks) {
        if (ticks - lastBreachTick < 450) return; // ~22 сек
        lastBreachTick = ticks;

        String template = BREACH_MESSAGES[rng.nextInt(BREACH_MESSAGES.length)];
        // Подставляем случайный номер цикла если нужно
        String msg = template.contains("%d")
            ? String.format(template, rng.nextInt(998) + 2)
            : template;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        target.sendSystemMessage(Component.literal(msg)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withItalic(true)));
    }

    // ── Утилиты ─────────────────────────────────────────────────────────────

    public static ItemStack createBookPublic(String[] pages) { return createBook(pages); }

    public static ItemStack createBook(String[] pages) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "Archive Fragment");
        tag.putString("author", "???");

        net.minecraft.nbt.ListTag pageList = new net.minecraft.nbt.ListTag();
        for (String page : pages) {
            pageList.add(net.minecraft.nbt.StringTag.valueOf(
                Component.Serializer.toJson(Component.literal(page)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)))));
        }
        tag.put("pages", pageList);
        return book;
    }

    public static void reset() {
        entryDone      = false;
        lastRuinTick   = 0;
        lastChestTick  = 0;
        lastBreachTick = 0;
        lastNickTick   = 0;
    }
}
