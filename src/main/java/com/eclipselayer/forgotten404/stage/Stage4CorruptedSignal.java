package com.eclipselayer.forgotten404.stage;

import com.eclipselayer.forgotten404.Forgotten404EndlessLoop;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;
import java.util.Random;

/**
 * Stage 4 — CORRUPTED SIGNAL
 *
 * 1. Фоновые звуки воспроизводятся в обратном направлении (реверс через pitch < 0 trick)
 * 2. Системные команды на экране: FIND THE CORE / DO NOT RESTART
 * 3. Нестабильный цикл дня/ночи — время то ускоряется, то останавливается
 * 4. AURORA_STACK начинает отслеживать игрока по имени (чаще)
 * 5. Случайные предупреждения о критическом индексе реальности
 */
public class Stage4CorruptedSignal {

    private static final String[] SYSTEM_COMMANDS = {
        "FIND THE CORE",
        "DO NOT RESTART",
        "signal corrupted",
        "[AURORA_STACK] tracking",
        "FIND THE CORE",
        "DO NOT RESTART",
        "> EXECUTE SCAN <",
        "reality index: 0.03",
        "WARNING: cascade failure imminent",
        "FIND THE CORE"
    };

    private static final String[] AUDIO_GLITCH_MESSAGES = {
        "[AURORA_STACK] audio stream: reversed",
        "[AURORA_STACK] CORRUPTED SIGNAL — audio stream degraded",
        "[SYSTEM] sound index: corrupted",
        "[SYSTEM] playback: ERROR"
    };

    private static final Random rng = new Random();

    private static long lastCommandTick  = 0;
    private static long lastAudioTick    = 0;
    private static long lastTimeTick     = 0;
    private static long lastTrackTick    = 0;
    private static long lastWarningTick  = 0;
    private static boolean entryDone     = false;

    // Режим времени: 0 = нормально, 1 = стоп, 2 = ускорение
    private static int timeMode = 0;
    private static long timeModeChangeTick = 0;

    public static void tick(MinecraftServer server, long ticks) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        if (!entryDone) {
            onEntry(server, players);
            entryDone = true;
        }

        tickSystemCommands(players, ticks);
        tickAudioGlitch(players, ticks);
        tickTimeDistortion(server, players, ticks);
        tickTracking(players, ticks);
        tickWarning(players, ticks);
    }

    // ── Вход в этап ─────────────────────────────────────────────────────────

    private static void onEntry(MinecraftServer server, List<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal(
                "[AURORA_STACK] CORRUPTED SIGNAL — audio stream degraded")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withBold(true)));
            p.sendSystemMessage(Component.literal(
                "FIND THE CORE")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)));
        }
        Forgotten404EndlessLoop.LOGGER.info("[ORIGIN LOOP] Stage 4: Corrupted Signal active.");
    }

    // ── 1. Системные команды на экране ──────────────────────────────────────

    private static void tickSystemCommands(List<ServerPlayer> players, long ticks) {
        if (ticks - lastCommandTick < 400) return; // ~20 сек
        lastCommandTick = ticks;

        String cmd = SYSTEM_COMMANDS[(int)(ticks / 400 % SYSTEM_COMMANDS.length)];
        ChatFormatting color = cmd.startsWith("[") ? ChatFormatting.DARK_GRAY : ChatFormatting.RED;

        Component msg = Component.literal(cmd)
            .withStyle(Style.EMPTY.withColor(color).withBold(!cmd.startsWith("[")));

        // Отправляем всем — в Stage 4 система уже не скрывается
        for (ServerPlayer p : players) p.sendSystemMessage(msg);
    }

    // ── 2. Звуковой глюк — искажённый звук ──────────────────────────────────

    private static void tickAudioGlitch(List<ServerPlayer> players, long ticks) {
        if (ticks - lastAudioTick < 350) return; // ~17 сек
        lastAudioTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));

        // Сообщение об искажении
        String msg = AUDIO_GLITCH_MESSAGES[rng.nextInt(AUDIO_GLITCH_MESSAGES.length)];
        target.sendSystemMessage(Component.literal(msg)
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withItalic(true)));

        // Отправляем реальный звук с экстремальным pitch (имитация реверса)
        var sounds = new net.minecraft.sounds.SoundEvent[]{
            SoundEvents.ENDERMAN_STARE,
            SoundEvents.WITHER_AMBIENT,
            SoundEvents.GUARDIAN_AMBIENT,
            SoundEvents.ELDER_GUARDIAN_AMBIENT
        };
        var sound = sounds[rng.nextInt(sounds.length)];

        // pitch очень низкий = замедленный/искажённый звук
        float pitch = 0.3f + rng.nextFloat() * 0.3f;

        target.connection.send(new ClientboundSoundPacket(
            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
            SoundSource.AMBIENT,
            target.getX(), target.getY(), target.getZ(),
            0.8f, pitch,
            rng.nextLong()
        ));
    }

    // ── 3. Нестабильный цикл дня/ночи ───────────────────────────────────────

    private static void tickTimeDistortion(MinecraftServer server, List<ServerPlayer> players, long ticks) {
        ServerLevel overworld = server.overworld();

        // Меняем режим каждые 30–60 сек
        if (ticks - timeModeChangeTick > 600 + rng.nextInt(600)) {
            timeMode = rng.nextInt(3); // 0=норм, 1=стоп, 2=ускорение
            timeModeChangeTick = ticks;

            String modeMsg = switch (timeMode) {
                case 1 -> "[SYSTEM] time cycle disrupted — day/night loop unstable";
                case 2 -> "[SYSTEM] time acceleration: uncontrolled";
                default -> "[SYSTEM] time cycle: restoring...";
            };
            for (ServerPlayer p : players) {
                p.sendSystemMessage(Component.literal(modeMsg)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withItalic(true)));
            }
        }

        switch (timeMode) {
            case 1 -> {
                // Стоп времени — откатываем назад каждый тик
                overworld.setDayTime(overworld.getDayTime() - 1);
            }
            case 2 -> {
                // Ускорение — +5 тиков в секунду дополнительно
                if (ticks % 4 == 0) overworld.setDayTime(overworld.getDayTime() + 20);
            }
            // case 0: нормально
        }
    }

    // ── 4. Отслеживание по имени ─────────────────────────────────────────────

    private static void tickTracking(List<ServerPlayer> players, long ticks) {
        if (ticks - lastTrackTick < 300) return; // ~15 сек
        lastTrackTick = ticks;

        ServerPlayer target = players.get(rng.nextInt(players.size()));
        target.sendSystemMessage(Component.literal(
            "[AURORA_STACK] tracking > " + target.getName().getString() + " <")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
    }

    // ── 5. Предупреждения о критическом состоянии ───────────────────────────

    private static void tickWarning(List<ServerPlayer> players, long ticks) {
        if (ticks - lastWarningTick < 1200) return; // раз в минуту
        lastWarningTick = ticks;

        String[] warnings = {
            "[AURORA_STACK] WARNING: reality index critical",
            "[AURORA_STACK] cascade failure: imminent",
            "[SYSTEM] core signal: weak",
            "DO NOT RESTART",
            "[AURORA_STACK] instance stability: 12%"
        };

        Component msg = Component.literal(warnings[rng.nextInt(warnings.length)])
            .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true));
        for (ServerPlayer p : players) p.sendSystemMessage(msg);
    }

    public static void reset() {
        entryDone          = false;
        lastCommandTick    = 0;
        lastAudioTick      = 0;
        lastTimeTick       = 0;
        lastTrackTick      = 0;
        lastWarningTick    = 0;
        timeMode           = 0;
        timeModeChangeTick = 0;
    }
}
