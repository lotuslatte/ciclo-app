package pe.favio.ciclo.data

import kotlinx.serialization.Serializable
import pe.favio.ciclo.domain.Bleed
import pe.favio.ciclo.domain.DayEntry
import java.time.LocalDate

// Dates are ISO strings (yyyy-MM-dd) so the saved file and backups are human-readable.

@Serializable
data class Period(
    val start: String,
    val end: String? = null,
    /** Bleeding that isn't a period (spotting, other causes): shown, but not used for cycles. */
    val excluded: Boolean = false,
)

@Serializable
data class DayLog(
    /** 0 = nada, 1 = ligero, 2 = medio, 3 = abundante. */
    val flow: Int = 0,
    val symptoms: List<String> = emptyList(),
    /** 0 (muy mal) … 4 (genial), null = sin registrar. */
    val mood: Int? = null,
    val note: String = "",
    /** Worst pain of the day, 0–10 (ASRM/ESHRE numeric scale). */
    val pain: Int? = null,
    /** Symptoms kept the user from usual activities. */
    val limited: Boolean = false,
) {
    val isEmpty: Boolean
        get() = flow == 0 && symptoms.isEmpty() && mood == null && note.isBlank() && pain == null && !limited
}

@Serializable
data class Settings(
    val defaultCycle: Int = 28,
    val defaultPeriod: Int = 5,
    val remindPeriod: Boolean = true,
    val remindDaysBefore: Int = 2,
    val remindFertile: Boolean = false,
    val pinHash: String? = null,
    val pinSalt: String? = null,
    /** False until the first-use guide has been completed or skipped. */
    val onboarded: Boolean = false,
) {
    val pinEnabled: Boolean get() = pinHash != null && pinSalt != null
}

@Serializable
data class AppData(
    val periods: List<Period> = emptyList(),
    val logs: Map<String, DayLog> = emptyMap(),
    val settings: Settings = Settings(),
) {
    val bleeds: List<Bleed>
        get() = periods.map { p -> Bleed(LocalDate.parse(p.start), p.end?.let(LocalDate::parse), p.excluded) }

    fun withBleeds(bleeds: List<Bleed>): AppData =
        copy(periods = bleeds.map { Period(it.start.toString(), it.end?.toString(), it.excluded) })

    /** Logs keyed by date, in the shape the analysis code uses. */
    val entries: Map<LocalDate, DayEntry>
        get() = logs.mapKeys { LocalDate.parse(it.key) }
            .mapValues { (_, l) -> DayEntry(l.flow, l.symptoms, l.pain, l.limited) }

    fun log(d: LocalDate): DayLog = logs[d.toString()] ?: DayLog()

    fun withLog(d: LocalDate, log: DayLog): AppData =
        copy(logs = if (log.isEmpty) logs - d.toString() else logs + (d.toString() to log))

    /**
     * The same data with every value forced into the range the UI can show, or null when a date
     * cannot be read. Files can be damaged, edited by hand or written by another version; nothing
     * read from one may crash the app later.
     */
    fun sanitizedOrNull(): AppData? {
        fun date(s: String): LocalDate? = runCatching { LocalDate.parse(s) }.getOrNull()
        val cleanPeriods = periods.map { p ->
            val start = date(p.start) ?: return null
            val end = p.end?.let { date(it) ?: return null }
            if (end != null && end.isBefore(start)) p.copy(end = p.start) else p
        }
        if (logs.keys.any { date(it) == null }) return null
        return copy(
            periods = cleanPeriods,
            logs = logs.mapValues { (_, l) ->
                l.copy(
                    flow = l.flow.coerceIn(Limits.FLOW),
                    mood = l.mood?.coerceIn(Limits.MOOD),
                    pain = l.pain?.coerceIn(Limits.PAIN),
                )
            },
            settings = settings.copy(
                defaultCycle = settings.defaultCycle.coerceIn(Limits.CYCLE),
                defaultPeriod = settings.defaultPeriod.coerceIn(Limits.PERIOD),
                remindDaysBefore = settings.remindDaysBefore.coerceIn(Limits.REMIND_DAYS),
            ),
        )
    }
}

/** Valid ranges for stored values. */
object Limits {
    val CYCLE = 21..45
    val PERIOD = 2..10
    val FLOW = 0..3
    val MOOD = 0..4
    val PAIN = 0..10
    val REMIND_DAYS = 1..3
}
