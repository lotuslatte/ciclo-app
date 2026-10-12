package pe.favio.ciclo.domain

import java.time.LocalDate

/** One cycle as a clinician would read it. [length] is null for the cycle still in progress. */
data class CycleRecord(
    val start: LocalDate,
    val length: Int?,
    val periodDays: Int,
    val heavyDays: Int,
    val maxPain: Int?,
    val limitedDays: Int,
)

/** Deviations from the FIGO 2018 limits for normal menstrual bleeding (adults). */
enum class CycleFlag { SHORT_CYCLES, LONG_CYCLES, VARIABLE, LONG_PERIODS, NO_PERIOD_90 }

/** "You usually get [symptom] on cycle days [fromDay]–[toDay]" seen in [cycles] of [of] logged cycles. */
data class SymptomPattern(val symptom: String, val fromDay: Int, val toDay: Int, val cycles: Int, val of: Int)

data class SymptomByPhase(val symptom: String, val total: Int, val byPhase: Map<Phase, Int>)

object Insights {
    const val FIGO_MIN_CYCLE = 24
    const val FIGO_MAX_CYCLE = 38
    const val FIGO_MAX_PERIOD = 8
    const val FIGO_MAX_SPREAD = 9
    private const val RECENT = 6

    fun history(bleeds: List<Bleed>, entries: Map<LocalDate, DayEntry>, today: LocalDate): List<CycleRecord> {
        val counted = bleeds.filter { !it.excluded }.sortedBy { it.start }
        return counted.mapIndexed { i, b ->
            val next = counted.getOrNull(i + 1)?.start
            val cycleEnd = next?.minusDays(1) ?: today
            val periodEnd = b.end ?: maxOf(b.start, today)
            val inCycle = entries.filterKeys { !it.isBefore(b.start) && !it.isAfter(cycleEnd) }
            CycleRecord(
                start = b.start,
                length = next?.let { daysBetween(b.start, it) },
                periodDays = daysBetween(b.start, periodEnd) + 1,
                heavyDays = inCycle.count { (d, e) -> !d.isAfter(periodEnd) && e.flow >= 3 },
                maxPain = inCycle.values.mapNotNull { it.pain }.maxOrNull(),
                limitedDays = inCycle.values.count { it.limited },
            )
        }
    }

    /**
     * Flags need a repeated pattern (2+ of the last 6 cycles), except a gap of 90+ days,
     * which counts on its own.
     */
    fun figoFlags(history: List<CycleRecord>, isOpen: Boolean, today: LocalDate): List<CycleFlag> {
        val recent = history.takeLast(RECENT + 1)
        val lengths = recent.mapNotNull { it.length }.takeLast(RECENT)
        val closedPeriods = recent.dropLast(if (isOpen) 1 else 0).map { it.periodDays }
        val flags = mutableListOf<CycleFlag>()
        if (lengths.count { it < FIGO_MIN_CYCLE } >= 2) flags += CycleFlag.SHORT_CYCLES
        if (lengths.count { it > FIGO_MAX_CYCLE } >= 2) flags += CycleFlag.LONG_CYCLES
        if (lengths.size >= 3 && lengths.max() - lengths.min() > FIGO_MAX_SPREAD) flags += CycleFlag.VARIABLE
        if (closedPeriods.count { it > FIGO_MAX_PERIOD } >= 2) flags += CycleFlag.LONG_PERIODS
        val last = history.lastOrNull()
        val longGap = lengths.any { it > 90 } || (last != null && !isOpen && daysBetween(last.start, today) > 90)
        if (longGap) flags += CycleFlag.NO_PERIOD_90
        return flags
    }

    /**
     * A symptom counts as a pattern on the cycle days where it appeared in at least half of the
     * recent cycles that have any log (and in at least 2 of them).
     */
    fun symptomPatterns(bleeds: List<Bleed>, entries: Map<LocalDate, DayEntry>, today: LocalDate): List<SymptomPattern> {
        val counted = bleeds.filter { !it.excluded }.sortedBy { it.start }.takeLast(RECENT)
        if (counted.size < 2) return emptyList()
        val all = bleeds.filter { !it.excluded }.sortedBy { it.start }
        // cycle day -> set of cycle indices where the symptom appeared, per symptom
        val seen = mutableMapOf<String, MutableMap<Int, MutableSet<Int>>>()
        var loggedCycles = 0
        counted.forEachIndexed { i, b ->
            val next = all.firstOrNull { it.start.isAfter(b.start) }?.start
            val end = next?.minusDays(1) ?: today
            val inCycle = entries.filterKeys { !it.isBefore(b.start) && !it.isAfter(end) }
            if (inCycle.isNotEmpty()) loggedCycles++
            inCycle.forEach { (d, e) ->
                val day = daysBetween(b.start, d) + 1
                e.symptoms.forEach { s -> seen.getOrPut(s) { mutableMapOf() }.getOrPut(day) { mutableSetOf() }.add(i) }
            }
        }
        if (loggedCycles < 2) return emptyList()
        val need = maxOf(2, (loggedCycles + 1) / 2)

        return seen.mapNotNull { (symptom, byDay) ->
            val days = byDay.filterValues { it.size >= need }.keys.sorted()
            if (days.isEmpty()) return@mapNotNull null
            // Longest run of consecutive days.
            var best = days.first()..days.first()
            var runStart = days.first()
            for (k in 1 until days.size) {
                if (days[k] != days[k - 1] + 1) runStart = days[k]
                if (days[k] - runStart > best.last - best.first) best = runStart..days[k]
            }
            val cycles = best.flatMap { byDay[it].orEmpty() }.toSet().size
            SymptomPattern(symptom, best.first, best.last, cycles, loggedCycles)
        }.sortedByDescending { it.cycles }
    }

    fun symptomsByPhase(entries: Map<LocalDate, DayEntry>, phaseOf: (LocalDate) -> Phase?): List<SymptomByPhase> {
        val counts = mutableMapOf<String, MutableMap<Phase, Int>>()
        entries.forEach { (d, e) ->
            val phase = phaseOf(d) ?: return@forEach
            e.symptoms.forEach { s ->
                val m = counts.getOrPut(s) { mutableMapOf() }
                m[phase] = (m[phase] ?: 0) + 1
            }
        }
        return counts.map { (s, m) -> SymptomByPhase(s, m.values.sum(), m) }.sortedByDescending { it.total }
    }
}
