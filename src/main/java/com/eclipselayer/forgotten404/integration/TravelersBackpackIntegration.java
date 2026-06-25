package com.eclipselayer.forgotten404.integration;

import com.eclipselayer.forgotten404.Forgotten404EndlessLoop;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Интеграция с Traveler's Backpack — полностью опциональная.
 * Никаких импортов из travelersbackpack — только стандартный MC реестр.
 * Мод компилируется и работает без него.
 */
public class TravelersBackpackIntegration {

    private static final String BACKPACK_MOD_ID = "travelersbackpack";
    private static final String BACKPACK_ITEM   = "travelersbackpack:leather_backpack";

    private static Boolean installed = null;

    public static boolean isInstalled() {
        if (installed == null) {
            installed = FabricLoader.getInstance().isModLoaded(BACKPACK_MOD_ID);
            if (installed) {
                Forgotten404EndlessLoop.LOGGER.info("[AURORA_STACK] Traveler's Backpack detected.");
            } else {
                Forgotten404EndlessLoop.LOGGER.info("[AURORA_STACK] Traveler's Backpack not found — backpack will not be issued.");
            }
        }
        return installed;
    }

    public static boolean giveBackpack(ServerPlayer player) {
        if (!isInstalled()) return false;

        try {
            var opt = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(BACKPACK_ITEM));
            if (opt.isEmpty()) {
                Forgotten404EndlessLoop.LOGGER.warn("[AURORA_STACK] Item '{}' not found in registry.", BACKPACK_ITEM);
                return false;
            }

            ItemStack stack = new ItemStack(opt.get());
            boolean added = player.getInventory().add(stack);
            if (!added) player.drop(stack, false);

            return true;

        } catch (Exception e) {
            Forgotten404EndlessLoop.LOGGER.error("[AURORA_STACK] Error issuing backpack: {}", e.getMessage());
            return false;
        }
    }
}
