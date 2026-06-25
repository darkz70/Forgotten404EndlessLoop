package com.eclipselayer.forgotten404.mixin;

import com.eclipselayer.forgotten404.Forgotten404EndlessLoop;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import com.eclipselayer.forgotten404.ending.EndingManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Запрет всех команд кроме /tp — навсегда, для всех игроков.
 * Серверная консоль не ограничивается (entity == null).
 */
@Mixin(Commands.class)
public class CommandSourceMixin {

    private static final String[] WHITELIST = { "tp" };

    @Inject(method = "performCommand", at = @At("HEAD"), cancellable = true)
    private void forgotten404_blockCommands(CommandSourceStack source, String command,
                                            CallbackInfo ci) {
        // Консоль сервера — не трогаем
        if (!(source.getEntity() instanceof ServerPlayer player)) return;

        // Нормализуем команду
        String cmd = command.trim().toLowerCase();
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        String root = cmd.split(" ")[0];

        // Проверяем белый список
        for (String allowed : WHITELIST) {
            if (root.equals(allowed)) return;
        }

        // Блокируем
        ci.cancel();
        EndingManager.incrementBlockedCmd();
        player.sendSystemMessage(Component.literal(
            "[AURORA_STACK] COMMAND REJECTED")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
    }
}
