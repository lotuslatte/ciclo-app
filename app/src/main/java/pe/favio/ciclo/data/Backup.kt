package pe.favio.ciclo.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/** Export file. Never contains the PIN. */
@Serializable
data class Backup(
    val format: String = FORMAT,
    val version: Int = 1,
    val exportedAt: String,
    val periods: List<Period>,
    val logs: Map<String, DayLog>,
    val defaultCycle: Int,
    val defaultPeriod: Int,
) {
    companion object {
        const val FORMAT = "ciclo-respaldo"
    }
}

object BackupCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(data: AppData, exportedAt: String): String = json.encodeToString(
        Backup.serializer(),
        Backup(
            exportedAt = exportedAt,
            periods = data.periods,
            logs = data.logs,
            defaultCycle = data.settings.defaultCycle,
            defaultPeriod = data.settings.defaultPeriod,
        ),
    )

    /** Throws [IllegalArgumentException] when the text is not a valid backup. */
    fun decode(text: String): Backup {
        val b = try {
            json.decodeFromString(Backup.serializer(), text)
        } catch (e: Exception) {
            throw IllegalArgumentException("El archivo no es un respaldo de Ciclo", e)
        }
        require(b.format == Backup.FORMAT) { "El archivo no es un respaldo de Ciclo" }
        try {
            b.periods.forEach { p -> LocalDate.parse(p.start); p.end?.let(LocalDate::parse) }
            b.logs.keys.forEach(LocalDate::parse)
        } catch (e: Exception) {
            throw IllegalArgumentException("El respaldo tiene fechas inválidas", e)
        }
        return b
    }

    /** Replaces records and cycle defaults; keeps this phone's reminders and PIN. */
    fun restore(current: AppData, b: Backup): AppData = current.copy(
        periods = b.periods,
        logs = b.logs,
        settings = current.settings.copy(defaultCycle = b.defaultCycle, defaultPeriod = b.defaultPeriod),
    )
}
