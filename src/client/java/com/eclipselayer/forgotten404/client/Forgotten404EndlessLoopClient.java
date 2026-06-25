package com.eclipselayer.forgotten404.client;

import com.eclipselayer.forgotten404.Forgotten404EndlessLoop;
import com.eclipselayer.forgotten404.system.EnvironmentManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

@Environment(EnvType.CLIENT)
public class Forgotten404EndlessLoopClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Forgotten404EndlessLoop.LOGGER.info("[AURORA_STACK] Client initialized.");

        // Клиентский тик — только для одиночной игры
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!EnvironmentManager.isSingleplayer()) return;
            if (Forgotten404EndlessLoop.STAGE_MANAGER == null) return;
            if (client.player == null) return;

            int stage = Forgotten404EndlessLoop.STAGE_MANAGER.getCurrentStage();
            SingleplayerEffects.tick(stage);
        });
    }
}
