package pe.favio.ciclo.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * All app data lives in one JSON file in private storage, rewritten on every change.
 *
 * Safety net: a damaged file is set aside (never overwritten) and the newest daily copy is
 * loaded instead. Copies are taken of the previous state on the first change of each day,
 * and the last [KEEP_COPIES] days are kept.
 */
class Store internal constructor(
    private val dir: File,
    private val clock: () -> LocalDateTime = LocalDateTime::now,
) {
    private val file = File(dir, FILE)
    private val copies = File(dir, "copias")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Set when loading had to recover; shown to the user once. */
    var loadProblem: String? = null
        private set

    private val state = MutableStateFlow(load())
    val data: StateFlow<AppData> = state

    @Synchronized
    fun update(change: (AppData) -> AppData) {
        val next = change(state.value)
        if (next == state.value) return
        save(next)
        state.value = next
    }

    fun clearLoadProblem() {
        loadProblem = null
    }

    /** Dates of the automatic copies, newest first. */
    fun copyDates(): List<LocalDate> = copyFiles().map { it.first }

    /** Replaces current data with the copy from [date]. Returns false if it can't be read. */
    fun restoreCopy(date: LocalDate): Boolean {
        val f = copyFiles().firstOrNull { it.first == date }?.second ?: return false
        val restored = parse(f) ?: return false
        update { restored }
        return true
    }

    private fun parse(f: File): AppData? = try {
        json.decodeFromString(AppData.serializer(), f.readText())
    } catch (e: Exception) {
        null
    }

    private fun load(): AppData {
        if (!file.exists()) return AppData()
        parse(file)?.let { return it }

        // Damaged: keep it for inspection and fall back to the newest readable copy.
        val stamp = clock().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        file.renameTo(File(dir, "$FILE.danado-$stamp"))
        for ((date, f) in copyFiles()) {
            val d = parse(f) ?: continue
            loadProblem = "No se pudo leer tus datos, así que recuperamos la copia automática del $date."
            writeAtomically(d)
            return d
        }
        loadProblem = "No se pudo leer tus datos y no había copias automáticas. El archivo dañado se guardó aparte."
        return AppData()
    }

    private fun save(d: AppData) {
        dailyCopy()
        writeAtomically(d)
    }

    private fun writeAtomically(d: AppData) {
        dir.mkdirs()
        val tmp = File(dir, "$FILE.tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), d))
        if (!tmp.renameTo(file)) {
            file.delete()
            check(tmp.renameTo(file)) { "No se pudo guardar" }
        }
    }

    /** Before the first write of the day, keep the current (last good) file as that day's copy. */
    private fun dailyCopy() {
        if (!file.exists() || parse(file) == null) return
        val today = clock().toLocalDate()
        val target = File(copies, "ciclo-$today.json")
        if (target.exists()) return
        copies.mkdirs()
        file.copyTo(target)
        copyFiles().drop(KEEP_COPIES).forEach { it.second.delete() }
    }

    private fun copyFiles(): List<Pair<LocalDate, File>> =
        (copies.listFiles() ?: emptyArray()).mapNotNull { f ->
            val date = f.name.removePrefix("ciclo-").removeSuffix(".json")
            runCatching { LocalDate.parse(date) }.getOrNull()?.let { it to f }
        }.sortedByDescending { it.first }

    companion object {
        private const val FILE = "ciclo.json"
        const val KEEP_COPIES = 7

        @Volatile
        private var instance: Store? = null

        fun get(context: Context): Store = instance ?: synchronized(this) {
            instance ?: Store(context.applicationContext.filesDir).also { instance = it }
        }
    }
}
