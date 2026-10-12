package pe.favio.ciclo.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * One bleeding episode. [end] is null while the period is still ongoing.
 * An [excluded] bleed is shown but is not a period start (spotting, bleeding from other causes).
 */
data class Bleed(val start: LocalDate, val end: LocalDate? = null, val excluded: Boolean = false)

/** What the analysis needs from a day's log. */
data class DayEntry(
    val flow: Int = 0,
    val symptoms: List<String> = emptyList(),
    val pain: Int? = null,
    val limited: Boolean = false,
)

enum class Phase { MENSTRUAL, FOLICULAR, OVULATORIA, LUTEA }

enum class DayKind { PERIOD, PREDICTED_PERIOD, OVULATION, FERTILE, NONE }

data class Span(val start: LocalDate, val end: LocalDate) {
    operator fun contains(d: LocalDate) = !d.isBefore(start) && !d.isAfter(end)
    val days: Int get() = daysBetween(start, end) + 1
}

data class CycleStats(
    val cycleLength: Int,
    val periodLength: Int,
    /** How many observed cycles fed the average (0 = using defaults). */
    val cyclesUsed: Int,
    val irregular: Boolean,
    /** Half-width in days of the likely window around the next start (0 = show a single date). */
    val rangeDays: Int = 0,
)

data class DayInfo(
    val kind: DayKind,
    val phase: Phase?,
    val cycleDay: Int?,
    val cycleLength: Int?,
    val ovulation: LocalDate?,
    val fertile: Span?,
)

internal fun daysBetween(a: LocalDate, b: LocalDate): Int = ChronoUnit.DAYS.between(a, b).toInt()

object CyclePredictor {
    /** Luteal phase is the most stable part of the cycle: ovulation ≈ next period − 14 days. */
    const val LUTEAL_DAYS = 14L
    const val MIN_CYCLE = 21
    const val MAX_CYCLE = 45
    /** Shortest-to-longest spread above this counts as irregular (FIGO normal variation ≤ 7–9 days). */
    const val IRREGULAR_SPREAD = 9
    private const val HISTORY = 6
    const val PREDICTED_CYCLES = 12

    fun stats(bleeds: List<Bleed>, defaultCycle: Int, defaultPeriod: Int): CycleStats {
        val sorted = bleeds.filter { !it.excluded }.sortedBy { it.start }
        // Gaps outside 15–60 days are almost always a missed or duplicated entry, not a real cycle.
        val cycles = sorted.zipWithNext { a, b -> daysBetween(a.start, b.start) }
            .filter { it in 15..60 }
            .takeLast(HISTORY)
        val lengths = sorted.mapNotNull { b -> b.end?.let { daysBetween(b.start, it) + 1 } }
            .filter { it in 1..14 }
            .takeLast(HISTORY)
        val cycle = if (cycles.isEmpty()) defaultCycle else cycles.average().roundToInt()
        val period = if (lengths.isEmpty()) defaultPeriod else lengths.average().roundToInt()
        // With 3+ cycles, show the next start as a window of ± one standard deviation (capped).
        val range = if (cycles.size < 3) 0 else {
            val mean = cycles.average()
            sqrt(cycles.sumOf { (it - mean) * (it - mean) } / (cycles.size - 1)).roundToInt().coerceIn(0, 7)
        }
        return CycleStats(
            cycleLength = cycle.coerceIn(MIN_CYCLE, MAX_CYCLE),
            periodLength = period.coerceIn(1, 10),
            cyclesUsed = cycles.size,
            irregular = cycles.size >= 3 && cycles.max() - cycles.min() > IRREGULAR_SPREAD,
            rangeDays = if (range >= 2) range else 0,
        )
    }

    fun predict(bleeds: List<Bleed>, defaultCycle: Int, defaultPeriod: Int, today: LocalDate): Prediction {
        val sorted = bleeds.sortedBy { it.start }
        // Excluded bleeds are painted but never start a cycle.
        val counted = sorted.filter { !it.excluded }
        val stats = stats(counted, defaultCycle, defaultPeriod)
        val last = counted.lastOrNull()
        val open = last?.takeIf { it.end == null }

        val actual = sorted.map { b -> Span(b.start, b.end ?: maxOf(b.start, today)) }
        val openRemainder = open?.let {
            val expectedEnd = it.start.plusDays(stats.periodLength - 1L)
            if (expectedEnd.isAfter(today)) Span(maxOf(today.plusDays(1), it.start), expectedEnd) else null
        }

        val nextStart = last?.start?.plusDays(stats.cycleLength.toLong())
        // Future cycles; when the period is late the overdue one is dropped rather than painted in the past.
        val predictedStarts = if (last == null) emptyList() else
            (1..PREDICTED_CYCLES).map { last.start.plusDays(stats.cycleLength.toLong() * it) }
                .filter { !it.isBefore(today) }
        val predictedPeriods = predictedStarts.map { Span(it, it.plusDays(stats.periodLength - 1L)) }

        return Prediction(
            today = today,
            stats = stats,
            actual = actual,
            isOpen = open != null,
            openRemainder = openRemainder,
            predictedPeriods = predictedPeriods,
            actualStarts = counted.map { it.start },
            predictedStarts = predictedStarts,
            nextStart = nextStart,
        )
    }
}

class Prediction internal constructor(
    val today: LocalDate,
    val stats: CycleStats,
    /** Logged periods; an ongoing one runs up to today. */
    val actual: List<Span>,
    val isOpen: Boolean,
    /** Days still expected for an ongoing period. */
    val openRemainder: Span?,
    val predictedPeriods: List<Span>,
    private val actualStarts: List<LocalDate>,
    private val predictedStarts: List<LocalDate>,
    /** Expected start of the next period (may be in the past when late). */
    val nextStart: LocalDate?,
) {
    val hasData: Boolean get() = actualStarts.isNotEmpty()

    val daysUntilNext: Int? get() = nextStart?.let { daysBetween(today, it) }

    /** Likely window for the next start when cycles vary; null means a single date is a fair estimate. */
    val nextRange: Span?
        get() {
            val next = nextStart ?: return null
            val r = stats.rangeDays.toLong()
            if (r == 0L || lateDays > 0) return null
            return Span(maxOf(next.minusDays(r), today), next.plusDays(r))
        }

    val lateDays: Int
        get() {
            val d = daysUntilNext ?: return 0
            return if (!isOpen && d < 0) -d else 0
        }

    fun periodAt(d: LocalDate): Span? = actual.firstOrNull { d in it }

    fun info(d: LocalDate): DayInfo {
        val starts = (actualStarts + predictedStarts).distinct().sorted()
        val s = starts.lastOrNull { !it.isAfter(d) }
            ?: return DayInfo(DayKind.NONE, null, null, null, null, null)

        // A cycle ends at the next logged period if there is one, otherwise at the expected length.
        val next = actualStarts.firstOrNull { it.isAfter(s) } ?: s.plusDays(stats.cycleLength.toLong())
        val length = daysBetween(s, next)
        val ovulation = next.minusDays(CyclePredictor.LUTEAL_DAYS)
        val fertile = Span(ovulation.minusDays(5), ovulation.plusDays(1))
        val cycleDay = daysBetween(s, d) + 1
        val ovDay = daysBetween(s, ovulation) + 1

        val logged = actual.firstOrNull { it.start == s }
        val periodLen = when {
            logged == null -> stats.periodLength
            isOpen && s == actualStarts.last() -> maxOf(logged.days, stats.periodLength)
            else -> logged.days
        }
        val phase = when {
            cycleDay <= periodLen -> Phase.MENSTRUAL
            cycleDay < ovDay - 1 -> Phase.FOLICULAR
            cycleDay <= ovDay + 1 -> Phase.OVULATORIA
            else -> Phase.LUTEA
        }
        val kind = when {
            actual.any { d in it } -> DayKind.PERIOD
            openRemainder?.contains(d) == true || predictedPeriods.any { d in it } -> DayKind.PREDICTED_PERIOD
            d == ovulation -> DayKind.OVULATION
            d in fertile -> DayKind.FERTILE
            else -> DayKind.NONE
        }
        return DayInfo(kind, phase, cycleDay, length, ovulation, fertile)
    }
}
