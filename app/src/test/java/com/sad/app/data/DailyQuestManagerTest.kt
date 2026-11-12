package com.sad.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class DailyQuestManagerTest {

    private fun millis(y: Int, m: Int, d: Int, h: Int = 12): Long =
        Calendar.getInstance().apply {
            clear(); set(y, m - 1, d, h, 0, 0)
        }.timeInMillis

    private fun withLocale(locale: Locale, block: () -> Unit) {
        val old = Locale.getDefault()
        Locale.setDefault(locale)
        try { block() } finally { Locale.setDefault(old) }
    }

    @Test
    fun weekStart_isMondayEvenOnSundayWithUsLocale() = withLocale(Locale.US) {
        // Sonntag, 15.11.2026 → Woche begann Montag, 09.11.2026
        val start = DailyQuestManager.weekStart(millis(2026, 11, 15))
        assertEquals(millis(2026, 11, 9, 0), start)
    }

    @Test
    fun weekStart_onMondayIsSameDayMidnight() {
        assertEquals(millis(2026, 11, 9, 0), DailyQuestManager.weekStart(millis(2026, 11, 9, 8)))
    }

    @Test
    fun weekKey_sameForWholeIsoWeek() = withLocale(Locale.US) {
        val monday = DailyQuestManager.weekKey(millis(2026, 11, 9))
        val sunday = DailyQuestManager.weekKey(millis(2026, 11, 15))
        val nextMonday = DailyQuestManager.weekKey(millis(2026, 11, 16))
        assertEquals(monday, sunday)
        assertNotEquals(sunday, nextMonday)
    }

    @Test
    fun weekKey_yearBoundaryDoesNotCollide() {
        // 29.12.2025 (Mo) gehört zu ISO-KW 1/2026, 05.01.2026 zu KW 2/2026.
        // Früher ergab das "2025_W1" und kollidierte mit Anfang Januar 2025.
        assertEquals("2026_W1", DailyQuestManager.weekKey(millis(2025, 12, 29)))
        assertEquals("2026_W2", DailyQuestManager.weekKey(millis(2026, 1, 5)))
        assertEquals("2025_W1", DailyQuestManager.weekKey(millis(2025, 1, 1)))
    }

    @Test
    fun weekStart_isNeverInTheFuture() {
        TimeZone.getDefault()
        var t = millis(2026, 1, 1)
        repeat(400) {
            assertTrue(DailyQuestManager.weekStart(t) <= t)
            t += 24 * 60 * 60 * 1000L
        }
    }
}
