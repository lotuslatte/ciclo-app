package pe.favio.ciclo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InsightsTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun closed(start: String, days: Long = 5, excluded: Boolean = false) =
        Bleed(d(start), d(start).plusDays(days - 1), excluded)

    private val today = d("2026-04-10")

    @Test
    fun history_describesEachCycle() {
        val bleeds = listOf(closed("2026-01-01"), closed("2026-01-29", 6), closed("2026-02-26"))
        val entries = mapOf(
            d("2026-01-01") to DayEntry(flow = 3, pain = 7, limited = true),
            d("2026-01-02") to DayEntry(flow = 3, pain = 4),
            d("2026-01-15") to DayEntry(pain = 2),
            d("2026-01-30") to DayEntry(flow = 3),
        )
        val h = Insights.history(bleeds, entries, today)
        assertEquals(3, h.size)
        assertEquals(28, h[0].length)
        assertEquals(5, h[0].periodDays)
        assertEquals(2, h[0].heavyDays)
        assertEquals(7, h[0].maxPain)
        assertEquals(1, h[0].limitedDays)
        assertEquals(6, h[1].periodDays)
        assertEquals(1, h[1].heavyDays)
        assertNull(h[2].length) // in progress
        assertNull(h[2].maxPain)
    }

    @Test
    fun history_skipsExcludedBleeds() {
        val bleeds = listOf(closed("2026-01-01"), closed("2026-01-14", 2, excluded = true), closed("2026-01-29"))
        val h = Insights.history(bleeds, emptyMap(), today)
        assertEquals(2, h.size)
        assertEquals(28, h[0].length)
    }

    @Test
    fun figo_regularCycles_noFlags() {
        val bleeds = listOf(closed("2026-01-01"), closed("2026-01-29"), closed("2026-02-26"), closed("2026-03-26"))
        val h = Insights.history(bleeds, emptyMap(), today)
        assertEquals(emptyList<CycleFlag>(), Insights.figoFlags(h, isOpen = false, today = today))
    }

    @Test
    fun figo_detectsShortLongVariableAndLongPeriods() {
        val short = listOf(closed("2026-01-01", 10), closed("2026-01-21", 10), closed("2026-02-10", 10), closed("2026-03-02"))
        val flags = Insights.figoFlags(Insights.history(short, emptyMap(), today), isOpen = false, today = today)
        assertTrue(CycleFlag.SHORT_CYCLES in flags) // 20-day cycles
        assertTrue(CycleFlag.LONG_PERIODS in flags) // 10-day periods

        val long = listOf(closed("2025-10-01"), closed("2025-11-15"), closed("2025-12-30"), closed("2026-01-25"))
        val lf = Insights.figoFlags(Insights.history(long, emptyMap(), d("2026-02-01")), isOpen = false, today = d("2026-02-01"))
        assertTrue(CycleFlag.LONG_CYCLES in lf) // 45, 45, 26
        assertTrue(CycleFlag.VARIABLE in lf)
    }

    @Test
    fun figo_ninetyDaysWithoutPeriod() {
        val h = Insights.history(listOf(closed("2026-01-01")), emptyMap(), today)
        assertEquals(listOf(CycleFlag.NO_PERIOD_90), Insights.figoFlags(h, isOpen = false, today = today))
    }

    @Test
    fun patterns_findRecurringSymptomDays() {
        val bleeds = listOf(closed("2026-01-01"), closed("2026-01-29"), closed("2026-02-26"), closed("2026-03-26"))
        val cramps = DayEntry(symptoms = listOf("Cólicos"))
        val entries = mapOf(
            d("2026-01-01") to cramps, d("2026-01-02") to cramps,
            d("2026-01-29") to cramps, d("2026-01-30") to cramps,
            d("2026-02-26") to cramps,
            d("2026-03-10") to DayEntry(symptoms = listOf("Acné")),
        )
        val p = Insights.symptomPatterns(bleeds, entries, today)
        assertEquals(1, p.size)
        assertEquals("Cólicos", p[0].symptom)
        assertEquals(1, p[0].fromDay)
        assertEquals(2, p[0].toDay)
        assertEquals(3, p[0].cycles)
        assertEquals(3, p[0].of)
    }

    @Test
    fun patterns_needTwoLoggedCycles() {
        val bleeds = listOf(closed("2026-01-01"), closed("2026-01-29"))
        val entries = mapOf(d("2026-01-01") to DayEntry(symptoms = listOf("Cólicos")))
        assertEquals(emptyList<SymptomPattern>(), Insights.symptomPatterns(bleeds, entries, today))
    }

    @Test
    fun predictor_ignoresExcludedAndGivesRange() {
        val bleeds = listOf(
            closed("2026-01-01"), closed("2026-01-14", 2, excluded = true), closed("2026-01-25"),
            closed("2026-02-25"), closed("2026-03-22"),
        )
        val p = CyclePredictor.predict(bleeds, 28, 5, d("2026-03-30"))
        assertEquals(3, p.stats.cyclesUsed) // 24, 31, 25 — the excluded bleed is not a cycle start
        assertEquals(27, p.stats.cycleLength)
        assertTrue(p.stats.rangeDays >= 2)
        val r = p.nextRange!!
        assertTrue(p.nextStart!! in r)
        assertEquals(DayKind.PERIOD, p.info(d("2026-01-14")).kind) // still painted
    }
}
