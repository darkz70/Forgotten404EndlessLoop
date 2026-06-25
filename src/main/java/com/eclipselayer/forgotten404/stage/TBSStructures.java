package com.eclipselayer.forgotten404.stage;

import com.eclipselayer.forgotten404.Forgotten404EndlessLoop;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.Random;

/**
 * TBS (The Broken Script) — инспирированные структуры.
 * Активны Stage 3 → конец Stage 4.
 *
 * 1.  Обелиск           — башня из чёрного камня
 * 2.  Круг из обсидиана — ритуальный круг с факелами душ
 * 3.  Запечатанная комната — подземная комната с книгой
 * 4.  Крест / нулевая точка
 * 5.  Сломанный портал
 * 6.  Заброшенная деревня — несколько домов в руинах
 * 7.  Подземный бункер  — длинный коридор под землёй
 * 8.  Алтарь с черепами — из чёрного камня и черепов
 * 9.  Коридор из стекла — прямой тоннель над землёй
 */
public class TBSStructures {

    private static final Random rng = new Random();
    private static long lastSpawnTick = 0;

    public static void tick(MinecraftServer server, long ticks, int currentStage) {
        if (currentStage < 3 || currentStage > 4) return;
        if (ticks - lastSpawnTick < 1400) return;
        lastSpawnTick = ticks;

        if (rng.nextInt(100) > 60) return;

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        ServerLevel level = server.getLevel(target.level().dimension());
        if (level == null) return;

        int dist = 25 + rng.nextInt(40);
        int angle = rng.nextInt(360);
        int bx = (int)(target.getX() + Math.cos(Math.toRadians(angle)) * dist);
        int bz = (int)(target.getZ() + Math.sin(Math.toRadians(angle)) * dist);
        int by = level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz);
        BlockPos origin = new BlockPos(bx, by, bz);

        int type = rng.nextInt(9);
        switch (type) {
            case 0 -> buildObelisk(level, origin, target, dist);
            case 1 -> buildObsidianCircle(level, origin, target, dist);
            case 2 -> buildAbandonedRoom(level, origin, target, dist);
            case 3 -> buildCross(level, origin, target, dist);
            case 4 -> buildBrokenPortal(level, origin, target, dist);
            case 5 -> buildAbandonedVillage(level, origin, target, dist);
            case 6 -> buildUndergroundBunker(level, origin, target, dist);
            case 7 -> buildSkullAltar(level, origin, target, dist);
            case 8 -> buildGlassCorridor(level, origin, target, dist);
        }
    }

    // ── 1. Обелиск ──────────────────────────────────────────────────────────

    private static void buildObelisk(ServerLevel level, BlockPos o,
                                     ServerPlayer p, int dist) {
        for (int y = 0; y < 9; y++) {
            if (y == 0) {
                for (int x = -1; x <= 1; x++)
                    for (int z = -1; z <= 1; z++)
                        set(level, o.offset(x, y, z), Blocks.BLACKSTONE);
            } else if (y < 6) {
                set(level, o.offset(0, y, 0), Blocks.POLISHED_BLACKSTONE);
                set(level, o.offset(1, y, 0), Blocks.BLACKSTONE);
                set(level, o.offset(-1, y, 0), Blocks.BLACKSTONE);
                set(level, o.offset(0, y, 1), Blocks.BLACKSTONE);
                set(level, o.offset(0, y, -1), Blocks.BLACKSTONE);
            } else {
                set(level, o.offset(0, y, 0), Blocks.POLISHED_BLACKSTONE);
            }
        }
        placeSign(level, o.offset(0, 1, 2),
            "0 0 0", "NULL POINT", "do not return", "");
        notify(p, "obelisk", dist);
    }

    // ── 2. Круг из обсидиана ────────────────────────────────────────────────

    private static void buildObsidianCircle(ServerLevel level, BlockPos o,
                                             ServerPlayer p, int dist) {
        int r = 5;
        for (int x = -r; x <= r; x++)
            for (int z = -r; z <= r; z++) {
                double d = Math.sqrt(x*x + z*z);
                if (d >= r - 0.6 && d <= r + 0.6)
                    set(level, o.offset(x, 0, z), Blocks.OBSIDIAN);
            }
        for (int deg = 0; deg < 360; deg += 45) {
            int fx = (int)(Math.cos(Math.toRadians(deg)) * (r - 1));
            int fz = (int)(Math.sin(Math.toRadians(deg)) * (r - 1));
            BlockPos tp = o.offset(fx, 1, fz);
            if (level.getBlockState(tp).isAir()) set(level, tp, Blocks.SOUL_TORCH);
        }
        set(level, o, Blocks.NETHERRACK);
        set(level, o.above(), Blocks.SOUL_FIRE);
        notify(p, "ritual circle", dist);
    }

    // ── 3. Запечатанная комната ──────────────────────────────────────────────

    private static void buildAbandonedRoom(ServerLevel level, BlockPos o,
                                            ServerPlayer p, int dist) {
        BlockPos base = o.below(4);
        for (int x = 0; x <= 4; x++)
            for (int z = 0; z <= 4; z++) {
                set(level, base.offset(x, 0, z), Blocks.STONE_BRICKS);
                set(level, base.offset(x, 4, z), Blocks.STONE_BRICKS);
                if (x == 0 || x == 4 || z == 0 || z == 4)
                    for (int y = 1; y <= 3; y++)
                        set(level, base.offset(x, y, z), Blocks.CRACKED_STONE_BRICKS);
                else
                    for (int y = 1; y <= 3; y++)
                        set(level, base.offset(x, y, z), Blocks.AIR);
            }
        set(level, base.offset(2, 1, 2), Blocks.OAK_PLANKS);
        set(level, base.offset(0, 2, 2), Blocks.SOUL_TORCH);
        set(level, base.offset(4, 2, 2), Blocks.SOUL_TORCH);

        // Книга
        BlockPos bp = base.offset(2, 2, 2);
        var book = Stage6ArchiveFracture.createBookPublic(
            new String[]{"This room was sealed\nfor a reason.\n\nDo not open\nthe archive.\n\n— AURORA_STACK"});
        level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
            level, bp.getX()+.5, bp.getY()+.5, bp.getZ()+.5, book));
        notify(p, "sealed room", dist);
    }

    // ── 4. Крест / нулевая точка ─────────────────────────────────────────────

    private static void buildCross(ServerLevel level, BlockPos o,
                                   ServerPlayer p, int dist) {
        for (int i = -2; i <= 2; i++) {
            set(level, o.offset(i, 0, 0), Blocks.BLACKSTONE);
            set(level, o.offset(0, 0, i), Blocks.BLACKSTONE);
        }
        set(level, o, Blocks.POLISHED_BLACKSTONE);
        for (int y = 1; y <= 4; y++) set(level, o.above(y), Blocks.BLACKSTONE);
        placeSign(level, o.offset(1, 1, 1), "ZERO POINT", "the cycle", "starts here", "");
        notify(p, "zero point marker", dist);
    }

    // ── 5. Сломанный портал ──────────────────────────────────────────────────

    private static void buildBrokenPortal(ServerLevel level, BlockPos o,
                                           ServerPlayer p, int dist) {
        for (int y = 0; y <= 4; y++) {
            set(level, o.offset(0, y, 0), Blocks.OBSIDIAN);
            set(level, o.offset(3, y, 0), Blocks.OBSIDIAN);
        }
        for (int x = 0; x <= 3; x++) {
            set(level, o.offset(x, 0, 0), Blocks.OBSIDIAN);
            set(level, o.offset(x, 4, 0), Blocks.OBSIDIAN);
        }
        if (rng.nextBoolean()) set(level, o.offset(0, 3, 0), Blocks.AIR);
        if (rng.nextBoolean()) set(level, o.offset(3, 2, 0), Blocks.AIR);
        for (int x = 1; x <= 2; x++)
            for (int y = 1; y <= 3; y++)
                set(level, o.offset(x, y, 0), Blocks.AIR);
        set(level, o.offset(-1, 0, 0), Blocks.CRACKED_STONE_BRICKS);
        set(level, o.offset(4, 0, 0), Blocks.CRACKED_STONE_BRICKS);
        notify(p, "broken portal", dist);
    }

    // ── 6. Заброшенная деревня ───────────────────────────────────────────────

    private static void buildAbandonedVillage(ServerLevel level, BlockPos o,
                                               ServerPlayer p, int dist) {
        // 3 дома в разных точках
        for (int h = 0; h < 3; h++) {
            int ox = rng.nextInt(30) - 15;
            int oz = rng.nextInt(30) - 15;
            int oy = level.getHeight(Heightmap.Types.WORLD_SURFACE,
                o.getX() + ox, o.getZ() + oz);
            BlockPos houseBase = new BlockPos(o.getX() + ox, oy, o.getZ() + oz);
            buildRuinedHouse(level, houseBase);
        }

        // Колодец в центре
        buildWell(level, o);

        // Табличка
        placeSign(level, o.offset(0, 1, 3),
            "ABANDONED", "cycle " + (rng.nextInt(99) + 2), "residents: 0", "");

        notify(p, "abandoned village", dist);
    }

    private static void buildRuinedHouse(ServerLevel level, BlockPos o) {
        // Пол 5x5
        for (int x = 0; x < 5; x++)
            for (int z = 0; z < 5; z++)
                set(level, o.offset(x, 0, z), Blocks.COBBLESTONE);

        // Стены — частичные
        for (int y = 1; y <= 3; y++)
            for (int x = 0; x < 5; x++) {
                if (rng.nextInt(100) < 65) set(level, o.offset(x, y, 0), Blocks.OAK_PLANKS);
                if (rng.nextInt(100) < 65) set(level, o.offset(x, y, 4), Blocks.OAK_PLANKS);
                if (rng.nextInt(100) < 65) set(level, o.offset(0, y, x), Blocks.OAK_PLANKS);
                if (rng.nextInt(100) < 65) set(level, o.offset(4, y, x), Blocks.OAK_PLANKS);
            }

        // Крыша — частичная
        for (int x = 0; x < 5; x++)
            for (int z = 0; z < 5; z++)
                if (rng.nextInt(100) < 50)
                    set(level, o.offset(x, 4, z), Blocks.OAK_PLANKS);

        // Дверной проём
        set(level, o.offset(2, 1, 0), Blocks.AIR);
        set(level, o.offset(2, 2, 0), Blocks.AIR);

        // Обугленные блоки внутри
        if (rng.nextBoolean()) {
            set(level, o.offset(2, 1, 2), Blocks.CAMPFIRE);
        }
    }

    private static void buildWell(ServerLevel level, BlockPos o) {
        // Основание 3x3
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                set(level, o.offset(x, 0, z), Blocks.COBBLESTONE);
        // Стенки колодца
        for (int y = 1; y <= 2; y++) {
            set(level, o.offset(-1, y, -1), Blocks.COBBLESTONE_WALL);
            set(level, o.offset(1, y, -1), Blocks.COBBLESTONE_WALL);
            set(level, o.offset(-1, y, 1), Blocks.COBBLESTONE_WALL);
            set(level, o.offset(1, y, 1), Blocks.COBBLESTONE_WALL);
        }
        // Вода внутри
        set(level, o.offset(0, 0, 0), Blocks.WATER);
    }

    // ── 7. Подземный бункер ──────────────────────────────────────────────────

    private static void buildUndergroundBunker(ServerLevel level, BlockPos o,
                                                ServerPlayer p, int dist) {
        BlockPos base = o.below(6);
        int len = 12 + rng.nextInt(8); // длина коридора

        // Главный коридор 3x3xlen
        for (int x = 0; x < len; x++) {
            for (int y = 0; y <= 2; y++)
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = base.offset(x, y, z);
                    if (y == 0 || y == 2 || z == -1 || z == 1)
                        set(level, pos, Blocks.STONE_BRICKS);
                    else
                        set(level, pos, Blocks.AIR);
                }
            // Факелы каждые 4 блока
            if (x % 4 == 0) {
                set(level, base.offset(x, 2, -1), Blocks.SOUL_TORCH);
                set(level, base.offset(x, 2, 1), Blocks.SOUL_TORCH);
            }
        }

        // Комнаты по бокам
        for (int room = 0; room < 2; room++) {
            int rx = 3 + room * 5;
            for (int x = 0; x < 4; x++)
                for (int y = 0; y <= 2; y++)
                    for (int z = 2; z <= 4; z++) {
                        BlockPos pos = base.offset(rx + x, y, z);
                        if (y == 0 || y == 2 || x == 0 || x == 3 || z == 4)
                            set(level, pos, Blocks.CRACKED_STONE_BRICKS);
                        else
                            set(level, pos, Blocks.AIR);
                    }
        }

        // Табличка у входа
        placeSign(level, base.offset(0, 1, 0),
            "SECTOR 0x" + Integer.toHexString(rng.nextInt(255)).toUpperCase(),
            "ACCESS: DENIED",
            "AURORA_STACK", "AUTHORIZED ONLY");

        // Вход — яма сверху
        for (int y = 0; y < 6; y++)
            set(level, o.offset(0, -y, 0), Blocks.AIR);

        // Лестница вниз
        for (int y = 0; y < 6; y++)
            set(level, o.offset(1, -y, 0), Blocks.LADDER);

        notify(p, "underground bunker", dist);
    }

    // ── 8. Алтарь с черепами ────────────────────────────────────────────────

    private static void buildSkullAltar(ServerLevel level, BlockPos o,
                                         ServerPlayer p, int dist) {
        // Ступени 3 уровня
        // Уровень 1 — 5x5
        for (int x = -2; x <= 2; x++)
            for (int z = -2; z <= 2; z++)
                set(level, o.offset(x, 0, z), Blocks.POLISHED_BLACKSTONE);

        // Уровень 2 — 3x3
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                set(level, o.offset(x, 1, z), Blocks.BLACKSTONE);

        // Уровень 3 — 1x1
        set(level, o.offset(0, 2, 0), Blocks.POLISHED_BLACKSTONE);

        // Черепа по углам уровня 1
        int[][] skullPos = {{-2,1,-2},{2,1,-2},{-2,1,2},{2,1,2}};
        for (int[] sp : skullPos) {
            set(level, o.offset(sp[0], sp[1], sp[2]), Blocks.WITHER_SKELETON_SKULL);
        }

        // Центральный череп на вершине
        set(level, o.offset(0, 3, 0), Blocks.WITHER_SKELETON_SKULL);

        // Душевой огонь по углам
        int[][] firePos = {{-1,1,-1},{1,1,-1},{-1,1,1},{1,1,1}};
        for (int[] fp : firePos) {
            set(level, o.offset(fp[0], fp[1], fp[2]), Blocks.SOUL_FIRE);
        }

        // Табличка
        placeSign(level, o.offset(0, 1, 3),
            "ORIGIN_LOOP", "was here", "before you", "existed");

        notify(p, "skull altar", dist);
    }

    // ── 9. Коридор из стекла ────────────────────────────────────────────────

    private static void buildGlassCorridor(ServerLevel level, BlockPos o,
                                            ServerPlayer p, int dist) {
        int len = 16 + rng.nextInt(8);
        // Коридор 3x3 из стекла над землёй (Y+2)
        BlockPos base = o.above(2);

        for (int x = 0; x < len; x++) {
            for (int y = 0; y <= 2; y++)
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = base.offset(x, y, z);
                    if (y == 0 || y == 2 || z == -1 || z == 1) {
                        // Стены/пол/потолок — стекло, иногда с трещинами
                        set(level, pos, rng.nextInt(100) < 20
                            ? Blocks.CRACKED_STONE_BRICKS
                            : Blocks.GLASS);
                    } else {
                        set(level, pos, Blocks.AIR);
                    }
                }
        }

        // Таблички внутри коридора
        BlockPos mid = base.offset(len / 2, 0, 0);
        placeSign(level, mid.above(),
            "YOU CANNOT", "LEAVE", "THE LOOP", "");

        // Колонны-опоры каждые 4 блока
        for (int x = 0; x < len; x += 4) {
            for (int y = -1; y >= -3; y--)
                set(level, base.offset(x, y, -1), Blocks.STONE_BRICKS);
            for (int y = -1; y >= -3; y--)
                set(level, base.offset(x, y, 1), Blocks.STONE_BRICKS);
        }

        notify(p, "glass corridor", dist);
    }

    // ── Утилиты ─────────────────────────────────────────────────────────────

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        try { level.setBlock(pos, block.defaultBlockState(), 3); }
        catch (Exception ignored) {}
    }

    private static void placeSign(ServerLevel level, BlockPos pos,
                                   String l1, String l2, String l3, String l4) {
        try {
            level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
                sign.setText(new SignText()
                    .setMessage(0, Component.literal(l1).withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)))
                    .setMessage(1, Component.literal(l2).withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)))
                    .setMessage(2, Component.literal(l3).withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)))
                    .setMessage(3, Component.literal(l4).withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY))),
                    true);
                sign.setChanged();
            }
        } catch (Exception ignored) {}
    }

    private static void notify(ServerPlayer player, String type, int dist) {
        player.sendSystemMessage(Component.literal(
            "[SYSTEM] unknown structure detected (" + type + ") — " + dist + " blocks away")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
        Forgotten404EndlessLoop.LOGGER.info("[TBS] '{}' spawned {}b from {}",
            type, dist, player.getName().getString());
    }

    public static void reset() { lastSpawnTick = 0; }
}
