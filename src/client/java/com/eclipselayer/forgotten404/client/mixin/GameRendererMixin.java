package com.eclipselayer.forgotten404.client.mixin;

import com.eclipselayer.forgotten404.client.StaticEffectController;
import com.eclipselayer.forgotten404.system.EnvironmentManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Клиентский Mixin — эффект статики поверх экрана.
 * Работает только в одиночной игре (SINGLEPLAYER mode).
 *
 * Применяет случайное смещение матрицы рендеринга — имитирует
 * "дрожание" экрана как в The Broken Script.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    private static final Random rng = new Random();

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void forgotten404_staticEffect(float tickDelta, long limitTime,
                                            PoseStack poseStack, CallbackInfo ci) {
        if (!EnvironmentManager.isSingleplayer()) return;
        if (!StaticEffectController.isActive()) return;

        float intensity = StaticEffectController.getIntensity();
        float shakeX = (rng.nextFloat() - 0.5f) * intensity * 0.03f;
        float shakeY = (rng.nextFloat() - 0.5f) * intensity * 0.03f;

        poseStack.translate(shakeX, shakeY, 0);
    }
}
