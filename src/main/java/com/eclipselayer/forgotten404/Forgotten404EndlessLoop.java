package com.eclipselayer.forgotten404;

import com.eclipselayer.forgotten404.event.PlayerJoinHandler;
import com.eclipselayer.forgotten404.system.EnvironmentManager;
import com.eclipselayer.forgotten404.system.StageManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.PlayerPickupItemCallback;
import com.eclipselayer.forgotten404.ending.FragmentTracker;
import com.eclipselayer.forgotten404.ending.EndingManager;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Forgotten404EndlessLoop implements ModInitializer {

    public static final String MOD_ID = "forgotten-404-endless-loop";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static StageManager STAGE_MANAGER;

    @Override
    public void onInitialize() {
        LOGGER.info("[AURORA_STACK] Initializing... ORIGIN LOOP PROTOCOL active.");

        STAGE_MANAGER = new StageManager();

        // Определяем окружение при старте сервера
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            EnvironmentManager.detect(server);
            LOGGER.info("[AURORA_STACK] Environment: {}", EnvironmentManager.getMode());
        });

        // Переопределяем при JOIN/LEAVE (одиночная → LAN и т.д.)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            EnvironmentManager.redetect(server);
            PlayerJoinHandler.onJoin(handler.player, server);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            EnvironmentManager.redetect(server);
        });

        // Подбор предметов — проверяем фрагменты
        PlayerPickupItemCallback.EVENT.register((player, entity, stack) -> {
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                FragmentTracker.onPickup(sp, stack);
            }
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        });

        // Основной тик
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            PlayerJoinHandler.tickPendingBackpacks(server);
            STAGE_MANAGER.tick(server);
        });

        LOGGER.info("[ORIGIN LOOP] Core systems online. Stage 0 — SLEEPING CORE.");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
