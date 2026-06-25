package com.eclipselayer.forgotten404.mixin;

import net.minecraft.server.level.ServerPlayer;
import com.eclipselayer.forgotten404.ending.EndingManager;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Принудительно включает keepInventory для всех игроков навсегда.
 * Гейм-рул keepInventory игнорируется — инвентарь всегда сохраняется.
 */
@Mixin(ServerPlayer.class)
public class PlayerDeathMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void forgotten404_forceKeepInventory(DamageSource source, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer)(Object)this;
        // Считаем смерть для триггера концовки
        EndingManager.onPlayerDeath((ServerPlayer)(Object)this);
        // Принудительно выставляем keepInventory = true перед смертью
        self.level().getGameRules()
            .getRule(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)
            .set(true, self.server);
    }
}
