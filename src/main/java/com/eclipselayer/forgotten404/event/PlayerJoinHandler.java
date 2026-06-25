package com.eclipselayer.forgotten404.event;

import com.eclipselayer.forgotten404.Forgotten404EndlessLoop;
import com.eclipselayer.forgotten404.integration.TravelersBackpackIntegration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Обрабатывает первый вход игрока в мир.
 *
 * Stage 1 — Initial Stability:
 * При первом подключении игрок получает рюкзак Traveler's Backpack.
 * Рюкзак выдаётся ОДИН РАЗ — флаг записывается в persistentData (NBT).
 * Выдача откладывается на 100 тиков (5 сек) через pendingBackpacks.
 * Инвентарь рюкзака сохраняется самим Traveler's Backpack, не теряется при смерти.
 */
public class PlayerJoinHandler {

    private static final String NBT_FIRST_JOIN    = "forgotten404_firstJoin";
    private static final String NBT_BACKPACK_GIVEN = "forgotten404_backpackGiven";

    // UUID игрока → тик, на котором нужно выдать рюкзак
    private static final Map<String, Long> pendingBackpacks = new HashMap<>();

    public static void onJoin(ServerPlayer player, MinecraftServer server) {
        CompoundTag pd = player.getPersistentData();
        boolean isFirstJoin = !pd.getBoolean(NBT_FIRST_JOIN);

        if (isFirstJoin) {
            pd.putBoolean(NBT_FIRST_JOIN, true);
            handleFirstJoin(player, server);
        } else {
            // Повторный вход — тихое системное сообщение
            player.sendSystemMessage(Component.literal("[AURORA_STACK] INSTANCE RECONNECTED")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
        }
    }

    private static void handleFirstJoin(ServerPlayer player, MinecraftServer server) {
        String name = player.getName().getString();
        Forgotten404EndlessLoop.LOGGER.info("[AURORA_STACK] New instance registered: {}", name);

        // Атмосферные сообщения первого входа (Stage 0 → Stage 1)
        player.sendSystemMessage(Component.literal("[AURORA_STACK] ARCHIVE READY")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
        player.sendSystemMessage(Component.literal("[AURORA_STACK] INSTANCE REGISTERED: " + name)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));
        player.sendSystemMessage(Component.literal("[AURORA_STACK] LOOP STATUS: STABLE")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)));

        // Откладываем выдачу рюкзака на 100 тиков (5 сек)
        // чтобы инвентарь игрока успел полностью загрузиться
        if (!player.getPersistentData().getBoolean(NBT_BACKPACK_GIVEN)) {
            long giftAt = server.getTickCount() + 100L;
            pendingBackpacks.put(name, giftAt);
        }
    }

    /**
     * Вызывается из StageManager каждый тик сервера.
     * Проверяет очередь отложенных выдач рюкзаков.
     */
    public static void tickPendingBackpacks(MinecraftServer server) {
        if (pendingBackpacks.isEmpty()) return;

        long now = server.getTickCount();
        pendingBackpacks.entrySet().removeIf(entry -> {
            if (now < entry.getValue()) return false; // ещё рано

            String playerName = entry.getKey();
            ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);

            if (player == null) {
                // Игрок вышел — оставляем флаг, попробуем при следующем входе
                // (удаляем из очереди, при следующем JOIN снова добавится)
                return true;
            }

            // Двойная проверка — вдруг уже выдан
            if (player.getPersistentData().getBoolean(NBT_BACKPACK_GIVEN)) return true;

            boolean success = TravelersBackpackIntegration.giveBackpack(player);
            if (success) {
                player.getPersistentData().putBoolean(NBT_BACKPACK_GIVEN, true);
                Forgotten404EndlessLoop.LOGGER.info(
                    "[AURORA_STACK] Backpack issued to instance: {}", playerName);

                player.sendSystemMessage(Component.literal(
                    "[LUCID WATCHER] You found it. Keep it.")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA).withItalic(true)));
                player.sendSystemMessage(Component.literal(
                    "[LUCID WATCHER] It remembers more than you think.")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA).withItalic(true)));
            } else {
                Forgotten404EndlessLoop.LOGGER.warn(
                    "[AURORA_STACK] Failed to give backpack to {}. Traveler's Backpack installed?",
                    playerName);
            }

            return true; // убираем из очереди в любом случае
        });
    }
}
