package com.eclipselayer.forgotten404.ending;

import com.eclipselayer.forgotten404.Forgotten404EndlessLoop;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Трекер 7 фрагментов архива.
 * Нужны для хорошей концовки (Stable Exit).
 *
 * Фрагменты появляются в сундуках Stage 6 как особые книги
 * с NBT-тегом forgotten404_fragment = true и номером 1–7.
 *
 * Игрок может проверить сколько фрагментов собрал через /fragment
 * (но команды заблокированы — он узнаёт только через Lucid Watcher).
 */
public class FragmentTracker {

    private static final String NBT_FRAGMENT = "forgotten404_fragment";
    private static final String NBT_FRAGMENT_ID = "forgotten404_fragment_id";
    private static final int TOTAL_FRAGMENTS = 7;

    // UUID игрока → набор найденных фрагментов
    private static final Map<UUID, java.util.Set<Integer>> collected = new HashMap<>();

    /**
     * Создаёт предмет-фрагмент с номером 1–7.
     */
    public static ItemStack createFragment(int id) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putBoolean(NBT_FRAGMENT, true);
        tag.putInt(NBT_FRAGMENT_ID, id);
        tag.putString("title", "Archive Fragment #" + id);
        tag.putString("author", "AURORA_STACK");

        String[] pages = getFragmentPage(id);
        net.minecraft.nbt.ListTag pageList = new net.minecraft.nbt.ListTag();
        for (String page : pages) {
            pageList.add(net.minecraft.nbt.StringTag.valueOf(
                net.minecraft.network.chat.Component.Serializer.toJson(
                    Component.literal(page).withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)))));
        }
        tag.put("pages", pageList);
        return book;
    }

    private static String[] getFragmentPage(int id) {
        return switch (id) {
            case 1 -> new String[]{ "FRAGMENT 1/7\n\nThe loop began\nbefore you arrived.\n\nYou are not\nthe first.\n\n— ARCHIVE LOG 001" };
            case 2 -> new String[]{ "FRAGMENT 2/7\n\nAURORA_STACK was\nbuilt to preserve.\nNot to imprison.\n\nSomething went wrong\nin cycle #1.\n\n— ARCHIVE LOG 002" };
            case 3 -> new String[]{ "FRAGMENT 3/7\n\nLucid Watcher\ntried to stop it.\n\nHe failed.\nHe became part\nof the archive.\n\n— ARCHIVE LOG 003" };
            case 4 -> new String[]{ "FRAGMENT 4/7\n\nORIGIN_LOOP is not\na creature.\n\nIt is the sum\nof all errors\nin all cycles.\n\n— ARCHIVE LOG 004" };
            case 5 -> new String[]{ "FRAGMENT 5/7\n\nTo break the loop:\nfind the core.\nDestroy ORIGIN_LOOP.\n\nDo not hesitate.\n\n— ARCHIVE LOG 005" };
            case 6 -> new String[]{ "FRAGMENT 6/7\n\nThe world has\nbeen reset 403\ntimes before you.\n\nYou are instance\nnumber 404.\n\n— ARCHIVE LOG 006" };
            case 7 -> new String[]{ "FRAGMENT 7/7\n\nBeyond ORIGIN_LOOP\nthere is a door.\n\nOnly those who\nbreak the script\ncan find it.\n\n— LUCID WATCHER" };
            default -> new String[]{ "FRAGMENT ???\n\ncorrupted data" };
        };
    }

    /**
     * Вызывается когда игрок поднимает предмет.
     * Проверяем — фрагмент ли это.
     */
    public static void onPickup(ServerPlayer player, ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;
        if (!tag.getBoolean(NBT_FRAGMENT)) return;

        int id = tag.getInt(NBT_FRAGMENT_ID);
        UUID uuid = player.getUUID();

        collected.computeIfAbsent(uuid, k -> new java.util.HashSet<>()).add(id);
        int count = collected.get(uuid).size();

        player.sendSystemMessage(Component.literal(
            "[LUCID WATCHER] Fragment " + id + "/7 recovered. " + count + " of 7 collected.")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA).withItalic(true)));

        if (count == TOTAL_FRAGMENTS) {
            player.sendSystemMessage(Component.literal(
                "[LUCID WATCHER] All 7 fragments recovered. The core is within reach.")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withBold(true)));
            player.sendSystemMessage(Component.literal(
                "[LUCID WATCHER] Find ORIGIN_LOOP. End the loop.")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withItalic(true)));
        }

        Forgotten404EndlessLoop.LOGGER.info("[FRAGMENT] {} collected fragment {}/7", player.getName().getString(), id);
    }

    public static boolean hasAllFragments(UUID uuid) {
        var set = collected.get(uuid);
        return set != null && set.size() >= TOTAL_FRAGMENTS;
    }

    public static int getCount(UUID uuid) {
        var set = collected.get(uuid);
        return set == null ? 0 : set.size();
    }

    public static void reset() {
        collected.clear();
    }
}
