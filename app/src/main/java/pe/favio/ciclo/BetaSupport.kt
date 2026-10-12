package pe.favio.ciclo

import android.content.Context
import android.content.Intent
import android.os.Build
import java.io.File
import java.time.LocalDateTime

/** Offline stand-ins for crash reporting and feedback: nothing leaves the phone unless the user shares it. */
object BetaSupport {
    private const val LOG = "errores.txt"
    private const val MAX_LOG = 60_000

    val deviceLine: String
        get() = "Ciclo ${BuildConfig.VERSION_NAME} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}"

    /** Records uncaught exceptions to a private file, then lets the app crash as usual. */
    fun installCrashLog(ctx: Context) {
        val file = File(ctx.filesDir, LOG)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                file.appendText("--- ${LocalDateTime.now().withNano(0)} · $deviceLine\n${error.stackTraceToString()}\n")
                if (file.length() > MAX_LOG) file.writeText(file.readText().takeLast(MAX_LOG / 2))
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun crashLog(ctx: Context): String? = File(ctx.filesDir, LOG).takeIf { it.exists() && it.length() > 0 }?.readText()

    fun clearCrashLog(ctx: Context) {
        File(ctx.filesDir, LOG).delete()
    }

    fun errorReportIntent(log: String): Intent = share(
        "Reporte de error de Ciclo",
        "Hola, la app Ciclo se cerró con un error. Este es el detalle técnico (no incluye mis datos del ciclo):\n\n${log.takeLast(20_000)}",
    )

    fun feedbackIntent(): Intent = share(
        "Comentarios sobre Ciclo",
        """
        Comentarios sobre Ciclo (versión de prueba)
        $deviceLine

        1. ¿Qué te gustó?

        2. ¿Qué te confundió o no funcionó?

        3. ¿Qué te gustaría que tuviera?
        """.trimIndent(),
    )

    private fun share(subject: String, text: String): Intent {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, text)
        return Intent.createChooser(send, subject)
    }
}
