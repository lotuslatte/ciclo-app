package pe.favio.ciclo.domain

import java.time.LocalDate

enum class PeriodAction {
    START_NEW,
    END_HERE,
    /** The previous period actually lasted until this day. */
    EXTEND_END,
    /** The next period actually started on this day. */
    MOVE_START_EARLIER,
    /** The period containing this day actually started on it. */
    MOVE_START_LATER,
    /** Keep the bleed on the calendar but leave it out of cycles and predictions. */
    EXCLUDE,
    INCLUDE,
    REMOVE,
}

/** What a tap on a day can do, given where that day sits relative to logged periods. */
data class DayOptions(
    val primary: PeriodAction?,
    val secondary: List<PeriodAction> = emptyList(),
    val isFuture: Boolean = false,
    val isFirstDay: Boolean = false,
    val isLastDay: Boolean = false,
    val isExcluded: Boolean = false,
)

/** Edits to the list of periods. Every function returns a normalized list. */
object PeriodOps {
    /** An ongoing period left open this long is assumed to have ended. */
    private const val STALE_OPEN_DAYS = 14
    /** Days next to a period that count as "part of it" rather than a new one. */
    const val NEAR_DAYS = 7

    private fun closedBefore(bleeds: List<Bleed>, d: LocalDate): Bleed? =
        bleeds.filter { it.end != null && it.end.isBefore(d) }.maxByOrNull { it.start }

    private fun startingAfter(bleeds: List<Bleed>, d: LocalDate): Bleed? =
        bleeds.filter { it.start.isAfter(d) }.minByOrNull { it.start }

    fun optionsFor(bleeds: List<Bleed>, d: LocalDate, today: LocalDate): DayOptions {
        if (d.isAfter(today)) return DayOptions(null, isFuture = true)

        val c = covering(bleeds, d, today)
        if (c != null) {
            val open = c.end == null
            val first = d == c.start
            val last = !open && d == c.end
            val endsHere = open && d == today
            val primary = when {
                endsHere -> PeriodAction.END_HERE
                first || last -> null
                else -> PeriodAction.END_HERE
            }
            val secondary = buildList {
                if (first && primary == null && !(c.end == c.start)) add(PeriodAction.END_HERE)
                if (!first) add(PeriodAction.MOVE_START_LATER)
                add(if (c.excluded) PeriodAction.INCLUDE else PeriodAction.EXCLUDE)
                add(PeriodAction.REMOVE)
            }
            return DayOptions(primary, secondary, isFirstDay = first, isLastDay = last, isExcluded = c.excluded)
        }

        val prev = closedBefore(bleeds, d)
        val next = startingAfter(bleeds, d)
        val afterPrev = prev != null && daysBetween(prev.end!!, d) in 1..NEAR_DAYS
        val beforeNext = next != null && daysBetween(d, next.start) in 1..NEAR_DAYS
        return when {
            afterPrev && beforeNext -> DayOptions(PeriodAction.EXTEND_END, listOf(PeriodAction.MOVE_START_EARLIER))
            afterPrev -> DayOptions(PeriodAction.EXTEND_END)
            beforeNext -> DayOptions(PeriodAction.MOVE_START_EARLIER)
            else -> DayOptions(PeriodAction.START_NEW)
        }
    }

    /** Applies [action] for day [d]; returns the list unchanged when it doesn't apply. */
    fun apply(bleeds: List<Bleed>, action: PeriodAction, d: LocalDate, today: LocalDate, avgLen: Int): List<Bleed> {
        if (d.isAfter(today)) return bleeds
        fun replace(target: Bleed?, with: (Bleed) -> Bleed): List<Bleed> =
            if (target == null) bleeds else normalize(bleeds.map { if (it == target) with(it) else it }, today, avgLen)
        return when (action) {
            PeriodAction.START_NEW -> startOn(bleeds, d, today, avgLen)
            PeriodAction.END_HERE -> replace(covering(bleeds, d, today)) { it.copy(end = d) }
            PeriodAction.EXTEND_END -> replace(closedBefore(bleeds, d)) { it.copy(end = d) }
            PeriodAction.MOVE_START_EARLIER -> replace(startingAfter(bleeds, d)) { it.copy(start = d) }
            PeriodAction.MOVE_START_LATER -> replace(covering(bleeds, d, today)) { it.copy(start = d) }
            PeriodAction.EXCLUDE -> replace(covering(bleeds, d, today)) { it.copy(excluded = true) }
            PeriodAction.INCLUDE -> replace(covering(bleeds, d, today)) { it.copy(excluded = false) }
            PeriodAction.REMOVE -> remove(bleeds, d, today)
        }
    }

    /** Sorts, closes stale open periods and merges overlapping or back-to-back ones. */
    fun normalize(bleeds: List<Bleed>, today: LocalDate, avgLen: Int): List<Bleed> {
        val sorted = bleeds.sortedBy { it.start }.distinctBy { it.start }
        val closed = sorted.mapIndexed { i, b ->
            val guess = b.start.plusDays(avgLen - 1L)
            when {
                b.end != null -> b.copy(end = maxOf(b.end, b.start))
                i < sorted.lastIndex -> b.copy(end = maxOf(b.start, minOf(guess, sorted[i + 1].start.minusDays(1))))
                daysBetween(b.start, today) >= STALE_OPEN_DAYS -> b.copy(end = minOf(guess, today))
                else -> b
            }
        }
        val out = mutableListOf<Bleed>()
        for (b in closed) {
            val prev = out.lastOrNull()
            val prevEnd = prev?.end
            if (prev != null && prevEnd != null && !b.start.isAfter(prevEnd.plusDays(1))) {
                out[out.lastIndex] = prev.copy(end = b.end?.let { maxOf(prevEnd, it) })
            } else {
                out += b
            }
        }
        return out
    }

    fun covering(bleeds: List<Bleed>, d: LocalDate, today: LocalDate): Bleed? =
        bleeds.firstOrNull { !d.isBefore(it.start) && !d.isAfter(it.end ?: maxOf(it.start, today)) }

    /** Marks [d] as the first day of a period. Recent starts stay open until the user ends them. */
    fun startOn(bleeds: List<Bleed>, d: LocalDate, today: LocalDate, avgLen: Int): List<Bleed> {
        if (d.isAfter(today) || covering(bleeds, d, today) != null) return bleeds
        val nextStart = bleeds.map { it.start }.filter { it.isAfter(d) }.minOrNull()
        val new = if (nextStart == null && daysBetween(d, today) < avgLen) {
            Bleed(d, null)
        } else {
            var end = minOf(d.plusDays(avgLen - 1L), today)
            if (nextStart != null) end = minOf(end, nextStart.minusDays(1))
            Bleed(d, maxOf(end, d))
        }
        return normalize(bleeds + new, today, avgLen)
    }

    /** Marks [d] as the last day of the period that contains it (or the ongoing one before it). */
    fun endOn(bleeds: List<Bleed>, d: LocalDate, today: LocalDate, avgLen: Int): List<Bleed> {
        val target = bleeds.filter { !it.start.isAfter(d) }.maxByOrNull { it.start } ?: return bleeds
        if (daysBetween(target.start, d) > STALE_OPEN_DAYS) return bleeds
        return normalize(bleeds.map { if (it == target) it.copy(end = d) else it }, today, avgLen)
    }

    fun remove(bleeds: List<Bleed>, d: LocalDate, today: LocalDate): List<Bleed> {
        val target = covering(bleeds, d, today) ?: return bleeds
        return bleeds - target
    }
}
