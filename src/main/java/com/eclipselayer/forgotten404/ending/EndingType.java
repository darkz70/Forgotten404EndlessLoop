package com.eclipselayer.forgotten404.ending;

/**
 * Все 7 концовок мода.
 */
public enum EndingType {
    STABLE_EXIT,        // Стабильный выход (хорошая) — найти 7 фрагментов + убить ORIGIN_LOOP
    CYCLE_ACCEPTANCE,   // Принятие цикла — согласиться на предложение ORIGIN_LOOP
    WORLD_RESTART,      // Перезапуск мира — мир перезапускается, цикл продолжается
    PLAYER_REPLACEMENT, // Замена игрока — двойник занимает место игрока
    ARCHIVE_ABSORPTION, // Поглощение архива — ORIGIN_LOOP поглощает реальность
    SCRIPT_FAILURE,     // Сбой скрипта — взлом кода, всё рушится
    BEYOND_ARCHIVE      // Beyond Archive (секретная, 1%) — портал за пределы мира
}
