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
        // Six days that end on the estimated ovulation day (Wilcox et al. 1995), none after it.
        assertEquals(Span(d("2026-03-07"), d("2026-03-12")), info.fertile)
        assertEquals(6, info.fertile!!.days)
        assertEquals(DayKind.NONE, p.info(d("2026-03-06")).kind)
        assertEquals(DayKind.FERTILE, p.info(d("2026-03-07")).kind)
        assertEquals(DayKind.FERTILE, p.info(d("2026-03-11")).kind)
        assertEquals(DayKind.NONE, p.info(d("2026-03-13")).kind)
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

    @Test
    fun firstPeriodStartedToday_usesDefaultsAndASingleDate() {
        val today = d("2026-03-10")
        val p = CyclePredictor.predict(listOf(Bleed(today, null)), 28, 5, today)
        assertTrue(p.hasData)
        assertTrue(p.isOpen)
        assertEquals(0, p.stats.cyclesUsed)
        assertEquals(d("2026-04-07"), p.nextStart)
        assertNull(p.nextRange)
        assertEquals(1, p.info(today).cycleDay)
        assertEquals(DayKind.PREDICTED_PERIOD, p.info(d("2026-03-14")).kind)
    }

    @Test
    fun leapDay_isCountedLikeAnyOtherDay() {
        val p = CyclePredictor.predict(listOf(closed("2028-02-02"), closed("2028-03-01")), 30, 5, d("2028-03-03"))
        assertEquals(28, p.stats.cycleLength) // February 2028 has 29 days
        assertEquals(d("2028-03-29"), p.nextStart)

        val single = CyclePredictor.predict(listOf(closed("2028-02-15")), 28, 5, d("2028-02-20"))
        assertEquals(15, single.info(d("2028-02-29")).cycleDay)
        assertEquals(d("2028-03-14"), single.nextStart)
    }

    @Test
    fun periodLeftOpenForWeeks_isReadAsEndedAtTheUsualLength() {
        val today = d("2026-04-28") // opened 26 Mar and never closed
        val p = CyclePredictor.predict(regular + Bleed(d("2026-03-26"), null), 28, 5, today)
        assertFalse(p.isOpen)
        assertEquals(DayKind.PERIOD, p.info(d("2026-03-30")).kind)
        assertEquals(DayKind.NONE, p.info(d("2026-03-31")).kind)
        assertEquals(DayKind.NONE, p.info(today).kind)
        assertEquals(d("2026-04-23"), p.nextStart)
        assertEquals(5, p.lateDays)
        assertNull(p.periodAt(today))
    }

    @Test
    fun gapThatHidesAMissedEntry_getsNoOvulationOrPhaseEstimate() {
        // 1 Sep -> 30 Nov is 90 days: some period in between was not logged.
        val bleeds = listOf(closed("2025-09-01"), closed("2025-11-30"), closed("2025-12-28"))
        val p = CyclePredictor.predict(bleeds, 28, 5, d("2026-01-05"))

        val guess = p.info(d("2025-11-16")) // 14 days before the next logged start
        assertEquals(DayKind.NONE, guess.kind)
        assertNull(guess.ovulation)
        assertNull(guess.fertile)
        assertNull(guess.phase)
        assertEquals(77, guess.cycleDay)
        assertEquals(Phase.MENSTRUAL, p.info(d("2025-09-03")).phase)
        assertEquals(DayKind.PERIOD, p.info(d("2025-09-03")).kind)

        // The 28-day cycle right after it is estimated as usual.
        assertEquals(DayKind.OVULATION, p.info(d("2025-12-14")).kind)
    }

    @Test
    fun overdueByMoreThanACycle_stopsProjectingDates() {
        // Expected 26 Mar; 28 days overdue is still "late", 29 is lapsed.
        val late = CyclePredictor.predict(regular, 28, 5, d("2026-04-23"))
        assertFalse(late.lapsed)
        assertEquals(28, late.lateDays)
        assertTrue(late.predictedPeriods.isNotEmpty())

        val today = d("2026-04-24")
        val p = CyclePredictor.predict(regular, 28, 5, today)
        assertTrue(p.lapsed)
        assertEquals(29, p.lateDays)
        assertTrue(p.predictedPeriods.isEmpty())
        assertNull(p.nextRange)
        assertEquals(d("2026-02-26"), p.lastStart)
        val info = p.info(today)
        assertEquals(DayKind.NONE, info.kind)
        assertNull(info.phase)
        assertNull(info.ovulation)
        assertEquals(DayKind.NONE, p.info(d("2026-05-21")).kind)
        // What was logged stays as it was.
        assertEquals(DayKind.PERIOD, p.info(d("2026-02-27")).kind)
        assertEquals(DayKind.OVULATION, p.info(d("2026-02-12")).kind)
    }

    @Test
    fun newPeriodAfterALongGap_resumesPredictionsWithoutCountingTheGap() {
        val today = d("2026-06-15")
        val p = CyclePredictor.predict(regular + closed("2026-06-10"), 30, 5, today)
        assertFalse(p.lapsed)
        assertEquals(28, p.stats.cycleLength) // the 104-day gap is not a cycle
        assertEquals(2, p.stats.cyclesUsed)
        assertEquals(d("2026-07-08"), p.nextStart)
        assertEquals(DayKind.PREDICTED_PERIOD, p.info(d("2026-07-08")).kind)
    }

    @Test
    fun unsortedInput_givesTheSamePrediction() {
        val today = d("2026-03-05")
        val a = CyclePredictor.predict(regular, 28, 5, today)
        val b = CyclePredictor.predict(regular.reversed(), 28, 5, today)
        assertEquals(a.nextStart, b.nextStart)
        assertEquals(a.stats, b.stats)
        assertEquals(a.info(today), b.info(today))
    }

    /** Periods whose starts are [lengths] days apart, beginning 1 Jan 2026. */
    private fun withCycles(vararg lengths: Long): List<Bleed> =
        lengths.runningFold(d("2026-01-01")) { start, len -> start.plusDays(len) }.map { Bleed(it, it.plusDays(4)) }

    @Test
    fun irregular_onlyAboveNineDaysBetweenShortestAndLongest() {
        // FIGO: up to 9 days is normal at 18-25 and 42-45 (7 at 26-41); the app does not know the age.
        val nine = CyclePredictor.predict(withCycles(26, 30, 35), 28, 5, d("2026-04-10"))
        assertEquals(3, nine.stats.cyclesUsed)
        assertFalse(nine.stats.irregular)
        val ten = CyclePredictor.predict(withCycles(26, 30, 36), 28, 5, d("2026-04-10"))
        assertTrue(ten.stats.irregular)
        // Two cycles are too few to call a pattern.
        assertFalse(CyclePredictor.predict(withCycles(24, 38), 28, 5, d("2026-03-10")).stats.irregular)
    }
}
