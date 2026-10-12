package pe.favio.ciclo.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PeriodOpsTest {
    private fun d(s: String) = LocalDate.parse(s)
    private val today = d("2026-03-10")

    @Test
    fun startToday_staysOpen() {
        val out = PeriodOps.startOn(emptyList(), today, today, 5)
        assertEquals(listOf(Bleed(today, null)), out)
    }

    @Test
    fun startInThePast_isClosedWithAverageLength() {
        val out = PeriodOps.startOn(emptyList(), d("2026-02-01"), today, 5)
        assertEquals(listOf(Bleed(d("2026-02-01"), d("2026-02-05"))), out)
    }

    @Test
    fun startBeforeExistingPeriod_doesNotOverlapIt() {
        val existing = listOf(Bleed(d("2026-02-03"), d("2026-02-07")))
        val out = PeriodOps.startOn(existing, d("2026-01-06"), today, 5)
        assertEquals(Bleed(d("2026-01-06"), d("2026-01-10")), out.first())
        assertEquals(2, out.size)
    }

    @Test
    fun startInsideExistingPeriod_isIgnored() {
        val existing = listOf(Bleed(d("2026-02-03"), d("2026-02-07")))
        assertEquals(existing, PeriodOps.startOn(existing, d("2026-02-05"), today, 5))
    }

    @Test
    fun futureStart_isIgnored() {
        assertEquals(emptyList<Bleed>(), PeriodOps.startOn(emptyList(), d("2026-03-20"), today, 5))
    }

    @Test
    fun endOn_closesOngoingPeriod() {
        val out = PeriodOps.endOn(listOf(Bleed(d("2026-03-07"), null)), today, today, 5)
        assertEquals(listOf(Bleed(d("2026-03-07"), today)), out)
    }

    @Test
    fun endOn_canExtendAClosedPeriod() {
        val out = PeriodOps.endOn(listOf(Bleed(d("2026-03-01"), d("2026-03-04"))), d("2026-03-06"), today, 5)
        assertEquals(listOf(Bleed(d("2026-03-01"), d("2026-03-06"))), out)
    }

    @Test
    fun staleOpenPeriod_isClosed() {
        val out = PeriodOps.normalize(listOf(Bleed(d("2026-02-01"), null)), today, 5)
        assertEquals(listOf(Bleed(d("2026-02-01"), d("2026-02-05"))), out)
    }

    @Test
    fun adjacentPeriods_areMerged() {
        val out = PeriodOps.normalize(
            listOf(Bleed(d("2026-02-01"), d("2026-02-03")), Bleed(d("2026-02-04"), d("2026-02-06"))),
            today, 5,
        )
        assertEquals(listOf(Bleed(d("2026-02-01"), d("2026-02-06"))), out)
    }

    // --- Context-aware options -------------------------------------------------------------

    private val feb = Bleed(d("2026-02-01"), d("2026-02-05"))
    private val mar = Bleed(d("2026-03-01"), d("2026-03-05"))

    private fun opts(day: String, bleeds: List<Bleed> = listOf(feb, mar)) = PeriodOps.optionsFor(bleeds, d(day), today)

    @Test
    fun options_futureDay_hasNoActions() {
        val o = opts("2026-03-20")
        assertEquals(null, o.primary)
        assertEquals(true, o.isFuture)
    }

    @Test
    fun options_farFromAnyPeriod_startsNew() {
        assertEquals(PeriodAction.START_NEW, opts("2026-02-18").primary)
        assertEquals(PeriodAction.START_NEW, PeriodOps.optionsFor(emptyList(), today, today).primary)
    }

    @Test
    fun options_rightAfterAPeriod_extendsIt() {
        assertEquals(PeriodAction.EXTEND_END, opts("2026-02-06").primary)
        assertEquals(PeriodAction.EXTEND_END, opts("2026-03-10").primary)
        assertEquals(PeriodAction.START_NEW, opts("2026-02-13").primary) // 8 days after
    }

    @Test
    fun options_rightBeforeAPeriod_movesItsStart() {
        assertEquals(PeriodAction.MOVE_START_EARLIER, opts("2026-02-26").primary)
        assertEquals(PeriodAction.START_NEW, opts("2026-02-21").primary) // 8 days before
    }

    @Test
    fun options_shortGapBetweenPeriods_offersBoth() {
        val a = Bleed(d("2026-01-01"), d("2026-01-05"))
        val b = Bleed(d("2026-01-10"), d("2026-01-14"))
        val o = PeriodOps.optionsFor(listOf(a, b), d("2026-01-07"), today)
        assertEquals(PeriodAction.EXTEND_END, o.primary)
        assertEquals(listOf(PeriodAction.MOVE_START_EARLIER), o.secondary)
    }

    @Test
    fun options_insideAPeriod() {
        val mid = opts("2026-02-03")
        assertEquals(PeriodAction.END_HERE, mid.primary)
        assertEquals(listOf(PeriodAction.MOVE_START_LATER, PeriodAction.EXCLUDE, PeriodAction.REMOVE), mid.secondary)

        val first = opts("2026-02-01")
        assertEquals(null, first.primary)
        assertEquals(true, first.isFirstDay)
        assertEquals(listOf(PeriodAction.END_HERE, PeriodAction.EXCLUDE, PeriodAction.REMOVE), first.secondary)

        val last = opts("2026-02-05")
        assertEquals(null, last.primary)
        assertEquals(true, last.isLastDay)
    }

    @Test
    fun options_ongoingPeriodToday_endsIt() {
        val o = PeriodOps.optionsFor(listOf(Bleed(d("2026-03-08"), null)), today, today)
        assertEquals(PeriodAction.END_HERE, o.primary)
        assertEquals(listOf(PeriodAction.MOVE_START_LATER, PeriodAction.EXCLUDE, PeriodAction.REMOVE), o.secondary)
    }

    @Test
    fun exclude_and_include_toggleTheFlag() {
        val out = PeriodOps.apply(listOf(feb, mar), PeriodAction.EXCLUDE, d("2026-02-03"), today, 5)
        assertEquals(true, out.first().excluded)
        val o = PeriodOps.optionsFor(out, d("2026-02-03"), today)
        assertEquals(true, o.isExcluded)
        assertEquals(true, PeriodAction.INCLUDE in o.secondary)
        val back = PeriodOps.apply(out, PeriodAction.INCLUDE, d("2026-02-03"), today, 5)
        assertEquals(false, back.first().excluded)
    }

    @Test
    fun apply_extendEnd_and_moveStartEarlier() {
        val extended = PeriodOps.apply(listOf(feb, mar), PeriodAction.EXTEND_END, d("2026-02-07"), today, 5)
        assertEquals(Bleed(d("2026-02-01"), d("2026-02-07")), extended.first())

        val moved = PeriodOps.apply(listOf(feb, mar), PeriodAction.MOVE_START_EARLIER, d("2026-02-27"), today, 5)
        assertEquals(Bleed(d("2026-02-27"), d("2026-03-05")), moved.last())
    }

    @Test
    fun apply_moveStartLater_and_endHere() {
        val later = PeriodOps.apply(listOf(feb), PeriodAction.MOVE_START_LATER, d("2026-02-03"), today, 5)
        assertEquals(listOf(Bleed(d("2026-02-03"), d("2026-02-05"))), later)

        val ended = PeriodOps.apply(listOf(feb), PeriodAction.END_HERE, d("2026-02-02"), today, 5)
        assertEquals(listOf(Bleed(d("2026-02-01"), d("2026-02-02"))), ended)
    }

    @Test
    fun apply_bridgingTwoPeriods_mergesThem() {
        val a = Bleed(d("2026-01-01"), d("2026-01-05"))
        val b = Bleed(d("2026-01-08"), d("2026-01-10"))
        val out = PeriodOps.apply(listOf(a, b), PeriodAction.EXTEND_END, d("2026-01-07"), today, 5)
        assertEquals(listOf(Bleed(d("2026-01-01"), d("2026-01-10"))), out)
    }

    @Test
    fun apply_futureDay_isIgnored() {
        assertEquals(listOf(feb), PeriodOps.apply(listOf(feb), PeriodAction.START_NEW, d("2026-04-01"), today, 5))
    }

    @Test
    fun remove_deletesThePeriodContainingTheDay() {
        val a = Bleed(d("2026-01-01"), d("2026-01-05"))
        val b = Bleed(d("2026-01-29"), d("2026-02-02"))
        assertEquals(listOf(a), PeriodOps.remove(listOf(a, b), d("2026-01-31"), today))
    }
}
