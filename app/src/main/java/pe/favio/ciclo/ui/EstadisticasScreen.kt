package pe.favio.ciclo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.favio.ciclo.R
import pe.favio.ciclo.content.FLAGS_ADVICE
import pe.favio.ciclo.content.PhaseContent
import pe.favio.ciclo.content.text
import pe.favio.ciclo.data.AppData
import pe.favio.ciclo.domain.Insights
import pe.favio.ciclo.domain.Phase
import pe.favio.ciclo.domain.Prediction
import pe.favio.ciclo.ui.theme.C
import pe.favio.ciclo.ui.theme.Fraunces
import pe.favio.ciclo.ui.theme.ringColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun EstadisticasScreen(p: Prediction, data: AppData, today: LocalDate, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var report by remember { mutableStateOf(false) }

    val entries = remember(data) { data.entries }
    val history = remember(data, today) { Insights.history(data.bleeds, entries, today) }
    val flags = remember(history, p) { Insights.figoFlags(history, p.isOpen, today) }
    val patterns = remember(data, today) { Insights.symptomPatterns(data.bleeds, entries, today) }
    val byPhase = remember(data, p) { Insights.symptomsByPhase(entries) { p.info(it).phase } }
    val completed = history.filter { it.length != null }.takeLast(12)
    val closedPeriods = history.dropLast(if (p.isOpen) 1 else 0).takeLast(12)
    val month = DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("es-PE"))

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack, colors = IconButtonDefaults.iconButtonColors(containerColor = C.Surface, contentColor = C.Text)) {
                Icon(painterResource(R.drawable.ic_chevron_left), "Volver")
            }
            Spacer(Modifier.width(12.dp))
            ScreenTitle("Tus patrones")
        }

        if (completed.isEmpty()) {
            CCard {
                Text("Aún no hay suficientes datos", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    "Registra al menos 2 reglas para ver la duración de tus ciclos. Si además anotas síntomas y dolor, aquí verás lo que se repite.",
                    color = C.Soft, fontSize = 14.sp, lineHeight = 20.sp,
                )
            }
        } else {
            val lengths = completed.map { it.length!! }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("Ciclo promedio", Fmt.days(lengths.average().roundToInt()), Modifier.weight(1f))
                MiniStat("Rango", "${lengths.min()}–${lengths.max()} días", Modifier.weight(1f))
                MiniStat("Ciclos", "${lengths.size}", Modifier.weight(1f))
            }
        }

        if (flags.isNotEmpty()) {
            CCard(bg = C.WarnBg, border = C.WarnBorder) {
                Kicker("A tener en cuenta", C.WarnText)
                flags.forEach { Text("• " + it.text(), fontSize = 14.sp, lineHeight = 20.sp) }
                Text(FLAGS_ADVICE, color = C.Soft, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }

        if (completed.isNotEmpty()) {
            CCard {
                Kicker("Duración de tus ciclos")
                BarChart(
                    completed.map { it.start.format(month).replace(".", "") to it.length!! },
                    minV = 15, maxV = max(45, completed.maxOf { it.length!! } + 3),
                    bandLo = Insights.FIGO_MIN_CYCLE, bandHi = Insights.FIGO_MAX_CYCLE,
                )
                Text("La franja marca el rango habitual (24 a 38 días).", color = C.Muted, fontSize = 12.sp)
            }
        }

        if (closedPeriods.isNotEmpty()) {
            CCard {
                Kicker("Duración de tu regla")
                BarChart(
                    closedPeriods.map { it.start.format(month).replace(".", "") to it.periodDays },
                    minV = 0, maxV = max(12, closedPeriods.maxOf { it.periodDays } + 2),
                    bandLo = 0, bandHi = Insights.FIGO_MAX_PERIOD,
                )
                Text("La franja marca hasta 8 días, el límite habitual.", color = C.Muted, fontSize = 12.sp)
            }
        }

        CCard {
            Kicker("Lo que se repite")
            if (patterns.isEmpty()) {
                Text(
                    "Anota tus síntomas durante al menos 2 ciclos y aquí verás si aparecen siempre en los mismos días.",
                    color = C.Soft, fontSize = 14.sp, lineHeight = 20.sp,
                )
            } else {
                patterns.take(6).forEach { pt ->
                    val days = if (pt.fromDay == pt.toDay) "el día ${pt.fromDay}" else "los días ${pt.fromDay}–${pt.toDay}"
                    Text(
                        "Sueles tener ${pt.symptom.lowercase()} $days de tu ciclo",
                        fontSize = 15.sp, lineHeight = 21.sp,
                    )
                    Text("En ${pt.cycles} de ${pt.of} ciclos anotados", color = C.Muted, fontSize = 12.sp)
                }
            }
        }

        if (byPhase.isNotEmpty()) {
            CCard {
                Kicker("Síntomas por fase")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Phase.entries.forEach { ph ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(ph.ringColor()))
                            Spacer(Modifier.width(4.dp))
                            Text(PhaseContent.of(ph).name, color = C.Muted, fontSize = 11.sp)
                        }
                    }
                }
                byPhase.take(8).forEach { s ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row {
                            Text(s.symptom, Modifier.weight(1f), fontSize = 14.sp)
                            Text(Fmt.days(s.total), color = C.Muted, fontSize = 13.sp)
                        }
                        Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
                            Phase.entries.forEach { ph ->
                                val n = s.byPhase[ph] ?: 0
                                if (n > 0) Box(Modifier.weight(n.toFloat()).fillMaxSize().background(ph.ringColor()))
                            }
                        }
                    }
                }
            }
        }

        if (history.any { it.maxPain != null || it.limitedDays > 0 }) {
            CCard {
                Kicker("Dolor por ciclo")
                history.takeLast(8).reversed().forEachIndexed { i, c ->
                    if (i > 0) HorizontalDivider(color = C.Divider)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Fmt.short(c.start), Modifier.width(64.dp), color = C.Muted, fontSize = 13.sp)
                        Text(
                            c.maxPain?.let { "Dolor máximo $it/10" } ?: "Sin dolor anotado",
                            Modifier.weight(1f), fontSize = 14.sp,
                            color = if ((c.maxPain ?: 0) >= 7) C.RedText else C.Text,
                        )
                        if (c.limitedDays > 0) Text("${Fmt.days(c.limitedDays)} limitada", color = C.Muted, fontSize = 12.sp)
                    }
                }
            }
        }

        PrimaryButton("Informe para tu médico (PDF)", { report = true })
        Text(
            "Todo se calcula en tu teléfono. Nada se envía a ningún servidor.",
            Modifier.fillMaxWidth().padding(bottom = 8.dp), color = C.Muted, fontSize = 12.sp,
        )
    }

    if (report) ReportDialog(data, today, onDismiss = { report = false })
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(CardShape).background(C.Surface).padding(12.dp)) {
        Text(label, color = C.Muted, fontSize = 12.sp)
        Text(value, fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    }
}

/** Vertical bars with a shaded "normal range" band; values outside it are drawn in crimson. */
@Composable
private fun BarChart(values: List<Pair<String, Int>>, minV: Int, maxV: Int, bandLo: Int, bandHi: Int) {
    val soft = C.Soft.toArgb()
    val muted = C.Muted.toArgb()
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        val top = 20.dp.toPx()
        val bottom = size.height - 20.dp.toPx()
        fun y(v: Int) = bottom - (v.coerceIn(minV, maxV) - minV).toFloat() / (maxV - minV) * (bottom - top)

        drawRect(C.Lav, Offset(0f, y(bandHi)), Size(size.width, y(bandLo) - y(bandHi)))
        drawLine(C.Border, Offset(0f, bottom), Offset(size.width, bottom), strokeWidth = 1.dp.toPx())

        val slot = size.width / values.size.coerceAtLeast(1)
        val barW = (slot * 0.55f).coerceAtMost(28.dp.toPx())
        val avg = values.map { it.second }.average()
        values.forEachIndexed { i, (label, v) ->
            val x = slot * i + (slot - barW) / 2
            val inBand = v in bandLo..bandHi
            drawRoundRect(
                if (inBand) C.OvulRing else C.Red,
                Offset(x, y(v)), Size(barW, bottom - y(v)),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
            text("$v", x + barW / 2, y(v) - 5.dp.toPx(), soft, 11.sp.toPx())
            text(label, x + barW / 2, size.height - 4.dp.toPx(), muted, 10.sp.toPx())
        }
        val ay = y(avg.roundToInt())
        drawLine(
            C.Text.copy(alpha = 0.6f), Offset(0f, ay), Offset(size.width, ay),
            strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
        )
    }
}

private fun DrawScope.text(t: String, x: Float, y: Float, color: Int, size: Float) {
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        textAlign = android.graphics.Paint.Align.CENTER
    }
    drawContext.canvas.nativeCanvas.drawText(t, x, y, paint)
}
