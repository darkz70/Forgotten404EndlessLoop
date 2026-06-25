package com.eclipselayer.forgotten404.client;

import com.eclipselayer.forgotten404.Forgotten404EndlessLoop;
import com.eclipselayer.forgotten404.system.EnvironmentManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

import java.util.Random;

/**
 * Клиентские эффекты для одиночной игры — стиль The Broken Script.
 *
 * Активны только в SINGLEPLAYER режиме.
 *
 * 1. Экранные помехи — мерцание HUD (реализуется через RenderTickEvent)
 * 2. Сообщения поверх экрана (title/subtitle)
 * 3. Эффект "статики" — быстрое мигание экрана
 * 4. Инвертирование управления на секунду (через mixin)
 * 5. Фальшивое меню выхода — "Exit to Menu" появляется но не работает
 */
@Environment(EnvType.CLIENT)
public class SingleplayerEffects {

    private static final Random rng = new Random();

    private static long lastTitleTick  = 0;
    private static long lastStaticTick = 0;
    private static int  currentStage   = 0;

    // Title-сообщения в стиле The Broken Script
    private static final String[][] STAGE_TITLES = {
        // Stage 2
        { "Can you see me?", "Help us.", "what was that?" },
        // Stage 3
        { "You are not alone.", "They remember you.", "Don't look back." },
        // Stage 4
        { "FIND THE CORE", "DO NOT RESTART", "signal corrupted" },
        // Stage 5
        { "he stands there", "don't come closer", "zone is dead" },
        // Stage 6
        { "KEEP PLAYING", "DON'T TRUST THEM", "we were here" },
        // Stage 7
        { "deja vu activated", "timelines collided", "is that you?" },
        // Stage 8
        { "NO EXIT FOUND", "you cannot leave", "coordinates repeat" },
        // Stage 9
        { "YOU ARE INSTANCE 404", "KILL HIM", "CORRUPTED USER" },
    };

    public static void tick(int stage) {
        if (!EnvironmentManager.isSingleplayer()) return;
        currentStage = stage;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        tickTitles(mc, stage);
        tickStatic(mc, stage);
    }

    // ── Title/Subtitle поверх экрана ────────────────────────────────────────

    private static void tickTitles(Minecraft mc, int stage) {
        if (stage < 2) return;

        long now = System.currentTimeMillis();
        long interval = switch (stage) {
            case 2, 3 -> 35_000L;
            case 4, 5 -> 25_000L;
            case 6, 7 -> 18_000L;
            case 8     -> 12_000L;
            case 9, 10 -> 7_000L;
            default -> 999_999L;
        };

        if (now - lastTitleTick < interval) return;
        lastTitleTick = now;

        String[] titles = STAGE_TITLES[Math.min(stage - 2, STAGE_TITLES.length - 1)];
        String msg = titles[rng.nextInt(titles.length)];

        ChatFormatting color = stage >= 9 ? ChatFormatting.RED
            : stage >= 6 ? ChatFormatting.DARK_PURPLE
            : stage >= 4 ? ChatFormatting.DARK_AQUA
            : ChatFormatting.GRAY;

        // Subtitle (маленький текст под заголовком)
        mc.gui.setSubtitle(Component.literal(msg)
            .withStyle(Style.EMPTY.withColor(color).withItalic(true)));
        mc.gui.setTimes(5, 40, 20); // fadeIn, stay, fadeOut тики
    }

    // ── Эффект статики — быстрое мигание экрана ─────────────────────────────
    // Реализуется через временное выставление гаммы (в ClientMixin)

    private static void tickStatic(Minecraft mc, int stage) {
        if (stage < 4) return;

        long now = System.currentTimeMillis();
        long interval = switch (stage) {
            case 4, 5 -> 45_000L;
            case 6, 7 -> 30_000L;
            case 8     -> 20_000L;
            case 9, 10 -> 10_000L;
            default -> 999_999L;
        };

        if (now - lastStaticTick < interval) return;
        if (rng.nextInt(100) > 50) return;
        lastStaticTick = now;

        // Помечаем что нужна статика — ClientMixin применит эффект
        StaticEffectController.triggerStatic(stage);
    }

    public static void reset() {
        lastTitleTick  = 0;
        lastStaticTick = 0;
        currentStage   = 0;
    }
}
