package pe.favio.ciclo.report

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import pe.favio.ciclo.content.FIGO_REFERENCE
import pe.favio.ciclo.content.text
import pe.favio.ciclo.data.AppData
import pe.favio.ciclo.domain.CyclePredictor
import pe.favio.ciclo.domain.Insights
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** A4 PDF summary for a clinic visit, built on-device with the platform PdfDocument. */
object DoctorReport {
    private const val W = 595
    private const val H = 842
    private const val M = 48f
    private val dmy = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun build(ctx: Context, data: AppData, months: Int, today: LocalDate): File {
        val from = today.minusMonths(months.toLong())
        val s = data.settings
        val bleeds = data.bleeds
        val entries = data.entries
        val p = CyclePredictor.predict(bleeds, s.defaultCycle, s.defaultPeriod, today)
        val all = Insights.history(bleeds, entries, today)
        val cycles = all.filter { !it.start.isBefore(from) }
        val flags = Insights.figoFlags(all, p.isOpen, today)
        val lengths = cycles.mapNotNull { it.length }
        val periods = cycles.dropLast(if (p.isOpen) 1 else 0).map { it.periodDays }
        val inRange = entries.filterKeys { !it.isBefore(from) && !it.isAfter(today) }
        val symptoms = inRange.values.flatMap { it.symptoms }.groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }
        val severePain = inRange.filter { (it.value.pain ?: 0) >= 7 }.keys.sorted()
        val limited = inRange.count { it.value.limited }

        val w = Writer()
        w.text("Informe del ciclo menstrual", 20f, bold = true, color = 0xFFC8374F.toInt())
        w.text("Periodo: ${from.format(dmy)} – ${today.format(dmy)}  ·  Generado el ${today.format(dmy)}", 10f, color = GRAY)
        w.gap(14f)

        w.heading("Resumen")
        w.text("Ciclos que empezaron en el periodo: ${cycles.size}")
        if (lengths.isNotEmpty()) {
            w.text("Duración del ciclo: media ${lengths.average().roundToInt()} días (mín. ${lengths.min()}, máx. ${lengths.max()})")
        }
        if (periods.isNotEmpty()) w.text("Duración de la regla: media ${periods.average().roundToInt()} días (mín. ${periods.min()}, máx. ${periods.max()})")
        bleeds.filter { !it.excluded }.maxByOrNull { it.start }?.let { w.text("Inicio de la última regla: ${it.start.format(dmy)}") }
        p.nextStart?.let {
            val r = p.nextRange
            w.text("Próxima regla estimada: ${it.format(dmy)}" + if (r != null) " (entre ${r.start.format(dmy)} y ${r.end.format(dmy)})" else "")
        }
        w.gap(8f)

        w.heading("Comparación con los límites FIGO")
        if (flags.isEmpty()) w.text("Sin desviaciones en los últimos ciclos registrados.")
        else flags.forEach { w.text("• " + it.text()) }
        w.gap(8f)

        w.heading("Ciclo a ciclo")
        val cols = floatArrayOf(M, M + 90, M + 165, M + 240, M + 330, M + 420)
        w.row(cols, listOf("Inicio", "Ciclo", "Regla", "Días abundantes", "Dolor máx.", "Días limitada"), bold = true)
        if (cycles.isEmpty()) w.text("Sin reglas registradas en el periodo.", color = GRAY)
        cycles.forEach { c ->
            w.row(
                cols,
                listOf(
                    c.start.format(dmy),
                    c.length?.let { "$it días" } ?: "en curso",
                    "${c.periodDays} días",
                    "${c.heavyDays}",
                    c.maxPain?.let { "$it/10" } ?: "—",
                    "${c.limitedDays}",
                ),
            )
        }
        w.gap(8f)

        w.heading("Síntomas registrados (número de días)")
        if (symptoms.isEmpty()) w.text("Sin síntomas registrados.", color = GRAY)
        else w.text(symptoms.take(10).joinToString("   ·   ") { "${it.key}: ${it.value}" })
        w.gap(4f)
        w.text("Días con dolor intenso (7/10 o más): ${severePain.size}" +
            if (severePain.isNotEmpty()) " — " + severePain.take(20).joinToString(", ") { it.format(DateTimeFormatter.ofPattern("dd/MM")) } else "")
        w.text("Días en que los síntomas le impidieron hacer sus actividades: $limited")
        w.gap(14f)

        w.text(
            "Datos registrados por la paciente en la app Ciclo, que funciona sin conexión. No incluye estimaciones de ovulación ni de días fértiles. " +
                "Dolor en escala numérica 0–10. $FIGO_REFERENCE",
            9f, color = GRAY,
        )

        val dir = File(ctx.cacheDir, "reports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "ciclo-informe-$today.pdf")
        w.finishPage()
        file.outputStream().use { w.doc.writeTo(it) }
        w.doc.close()
        return file
    }

    fun uri(ctx: Context, file: File) = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)

    fun viewIntent(ctx: Context, file: File): Intent {
        val u = uri(ctx, file)
        return Intent(Intent.ACTION_VIEW).setDataAndType(u, "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun shareIntent(ctx: Context, file: File): Intent {
        val u = uri(ctx, file)
        val send = Intent(Intent.ACTION_SEND).setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, u)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri("", u)
        return Intent.createChooser(send, "Compartir informe")
    }

    private const val BLACK = 0xFF1C1320.toInt()
    private const val GRAY = 0xFF6B5E70.toInt()

    /** Tiny top-down text layout with automatic page breaks. */
    private class Writer {
        val doc = PdfDocument()
        private var pageNo = 0
        private var page: PdfDocument.Page = newPage()
        private var y = M
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        private fun newPage(): PdfDocument.Page {
            pageNo++
            return doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
        }

        private fun ensure(space: Float) {
            if (y + space > H - M) {
                doc.finishPage(page)
                page = newPage()
                y = M
            }
        }

        private fun style(size: Float, bold: Boolean, color: Int) {
            paint.textSize = size
            paint.color = color
            paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        fun gap(h: Float) {
            y += h
        }

        fun heading(t: String) {
            ensure(30f)
            y += 6f
            text(t, 13f, bold = true)
            paint.color = 0xFFE0D4DC.toInt()
            page.canvas.drawLine(M, y - 2f, W - M, y - 2f, paint)
            y += 4f
        }

        /** Draws [t], wrapping at the right margin. */
        fun text(t: String, size: Float = 11f, bold: Boolean = false, color: Int = BLACK) {
            style(size, bold, color)
            val maxW = W - 2 * M
            var line = ""
            fun flush() {
                ensure(size * 1.5f)
                y += size * 1.35f
                page.canvas.drawText(line, M, y, paint)
                line = ""
            }
            t.split(" ").forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) > maxW && line.isNotEmpty()) {
                    flush()
                    line = word
                } else {
                    line = candidate
                }
            }
            if (line.isNotEmpty()) flush()
        }

        fun row(xs: FloatArray, cells: List<String>, bold: Boolean = false) {
            style(10f, bold, BLACK)
            ensure(18f)
            y += 15f
            cells.forEachIndexed { i, c -> page.canvas.drawText(c, xs[i], y, paint) }
        }

        /** Must run before writing the document out. */
        fun finishPage() {
            doc.finishPage(page)
        }
    }
}
