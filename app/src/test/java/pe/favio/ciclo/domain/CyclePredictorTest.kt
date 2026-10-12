package pe.favio.ciclo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CyclePredictorTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun closed(start: String, days: Long = 5) = Bleed(d(start), d(start).plusDays(days - 1))

    private val regular = listOf(closed("2026-01-01"), closed("2026-01-29"), closed("2026-02-26"))

    @Test
    fun noData_usesDefaultsAndPredictsNothing() {
        val p = CyclePredictor.predict(emptyList(), 28, 5, d("2026-03-01"))
        assertFalse(p.hasData)
        assertNull(p.nextStart)
        assertEquals(DayKind.NONE, p.info(d("2026-03-01")).kind)
        assertEquals(28, p.stats.cycleLength)
    }

    @Test
    fun regularCycles_averageAndNextStart() {
        val p = CyclePredictor.predict(regular, 30, 4, d("2026-03-05"))
        assertEquals(28, p.stats.cycleLength)
        assertEquals(5, p.stats.periodLength)
        assertEquals(2, p.stats.cyclesUsed)
        assertEquals(d("2026-03-26"), p.nextStart)
        assertEquals(21, p.daysUntilNext)
        assertEquals(0, p.lateDays)
    }

    @Test
    fun ovulationAndFertileWindow_countBackFromNextPeriod() {
        val p = CyclePredictor.predict(regular, 28, 5, d("2026-03-05"))
        val info = p.info(d("2026-03-12"))
        assertEquals(d("2026-03-12"), info.ovulation)
        assertEquals(DayKind.OVULATION, info.kind)
        assertEquals(Span(d("2026-03-07"), d("2026-03-13")), info.fertile)
        assertEquals(DayKind.FERTILE, p.info(d("2026-03-07")).kind)
        assertEquals(DayKind.NONE, p.info(d("2026-03-14")).kind)
    }

    @Test
    fun phases_followCycleDay() {
        val p = CyclePredictor.predict(regular, 28, 5, d("2026-03-05"))
        // Cycle starting 26 Feb: period days 1–5, ovulation day 15.
        assertEquals(Phase.MENSTRUAL, p.info(d("2026-03-02")).phase) // day 5
        assertEquals(Phase.FOLICULAR, p.info(d("2026-03-03")).phase) // day 6
        assertEquals(Phase.FOLICULAR, p.info(d("2026-03-10")).phase) // day 13
        assertEquals(Phase.OVULATORIA, p.info(d("2026-03-11")).phase) // day 14
        assertEquals(Phase.OVULATORIA, p.info(d("2026-03-13")).phase) // day 16
        assertEquals(Phase.LUTEA, p.info(d("2026-03-14")).phase) // day 17
        assertEquals(28, p.info(d("2026-03-25")).cycleDay)
    }

    @Test
    fun loggedAndPredictedPeriods_areMarked() {
        val p = CyclePredictor.predict(regular, 28, 5, d("2026-03-05"))
        assertEquals(DayKind.PERIOD, p.info(d("2026-02-28")).kind)
        assertEquals(DayKind.PREDICTED_PERIOD, p.info(d("2026-03-26")).kind)
        assertEquals(DayKind.PREDICTED_PERIOD, p.info(d("2026-03-30")).kind)
        assertEquals(DayKind.NONE, p.info(d("2026-03-31")).kind)
        assertEquals(Phase.MENSTRUAL, p.info(d("2026-03-27")).phase)
    }

    @Test
    fun singlePeriod_fallsBackToDefaultCycle() {
        val p = CyclePredictor.predict(listOf(closed("2026-03-01", 4)), 30, 5, d("2026-03-10"))
        assertEquals(30, p.stats.cycleLength)
        assertEquals(4, p.stats.periodLength)
        assertEquals(0, p.stats.cyclesUsed)
        assertEquals(d("2026-03-31"), p.nextStart)
    }

    @Test
    fun implausibleGaps_areIgnored() {
        // A 90-day gap is a missing entry, not a cycle.
        val bleeds = listOf(closed("2025-09-01"), closed("2025-11-30"), closed("2025-12-28"))
        val p = CyclePredictor.predict(bleeds, 30, 5, d("2026-01-05"))
        assertEquals(28, p.stats.cycleLength)
        assertEquals(1, p.stats.cyclesUsed)
    }

    @Test
    fun irregular_whenSpreadIsLarge() {
        val bleeds = listOf(closed("2026-01-01"), closed("2026-01-23"), closed("2026-02-27"), closed("2026-03-27"))
        val p = CyclePredictor.predict(bleeds, 28, 5, d("2026-04-01"))
        assertTrue(p.stats.irregular) // 22, 35, 28 days
        assertFalse(CyclePredictor.predict(regular, 28, 5, d("2026-03-05")).stats.irregular)
    }

    @Test
    fun latePeriod_reportsDelayAndStaysLuteal() {
        val today = d("2026-03-29") // expected 26 Mar
        val p = CyclePredictor.predict(regular, 28, 5, today)
        assertEquals(3, p.lateDays)
        assertEquals(-3, p.daysUntilNext)
        val info = p.info(today)
        assertEquals(Phase.LUTEA, info.phase)
        assertEquals(32, info.cycleDay)
        assertEquals(DayKind.NONE, info.kind)
    }

    @Test
    fun openPeriod_runsToTodayAndPredictsTheRest() {
        val today = d("2026-03-27")
        val p = CyclePredictor.predict(regular + Bleed(d("2026-03-26"), null), 28, 5, today)
        assertTrue(p.isOpen)
        assertEquals(0, p.lateDays)
        assertEquals(DayKind.PERIOD, p.info(today).kind)
        assertEquals(DayKind.PREDICTED_PERIOD, p.info(d("2026-03-28")).kind)
        assertEquals(DayKind.PREDICTED_PERIOD, p.info(d("2026-03-30")).kind)
        assertEquals(DayKind.NONE, p.info(d("2026-03-31")).kind)
        assertEquals(d("2026-04-23"), p.nextStart)
    }

    @Test
    fun cycleLength_isClampedToPlausibleRange() {
        val bleeds = listOf(closed("2026-01-01"), closed("2026-01-17"), closed("2026-02-02"))
        val p = CyclePredictor.predict(bleeds, 28, 5, d("2026-02-05"))
        assertEquals(CyclePredictor.MIN_CYCLE, p.stats.cycleLength)
    }
}
