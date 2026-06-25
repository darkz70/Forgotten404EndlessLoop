package com.eclipselayer.forgotten404.stage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Очередь отложенного восстановления блоков после глюка.
 * Отправляет клиенту пакет восстановления блока через N тиков.
 */
public class GlitchRestoreQueue {

    private record Entry(BlockPos pos, BlockState state, long restoreAt, ServerPlayer player) {}

    private static final List<Entry> queue = new ArrayList<>();

    public static void schedule(BlockPos pos, BlockState state, long restoreAt, ServerPlayer player) {
        queue.add(new Entry(pos, state, restoreAt, player));
    }

    public static void tick(MinecraftServer server, long currentTick) {
        if (queue.isEmpty()) return;

        Iterator<Entry> it = queue.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (currentTick < e.restoreAt()) continue;

            // Восстанавливаем блок только на клиенте игрока
            if (e.player().isAlive() && e.player().connection != null) {
                e.player().connection.send(
                    new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(
                        e.pos(), e.state()
                    )
                );
            }
            it.remove();
        }
    }
}
