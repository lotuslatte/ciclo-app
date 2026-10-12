package pe.favio.ciclo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import pe.favio.ciclo.content.FLAGS_ADVICE
import pe.favio.ciclo.content.PhaseContent
import pe.favio.ciclo.content.text
import pe.favio.ciclo.domain.CycleFlag
import pe.favio.ciclo.data.AppData
import pe.favio.ciclo.domain.PeriodAction
import pe.favio.ciclo.domain.PeriodOps
import pe.favio.ciclo.domain.DayKind
import pe.favio.ciclo.domain.Phase
import pe.favio.ciclo.domain.Prediction
import pe.favio.ciclo.ui.theme.C
import pe.favio.ciclo.ui.theme.Fraunces
import pe.favio.ciclo.ui.theme.ringColor
import pe.favio.ciclo.ui.theme.textColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HoyScreen(
    p: Prediction,
    data: AppData,
    today: LocalDate,
    onAction: (PeriodAction, LocalDate) -> Unit,
    onLog: () -> Unit,
    onLearn: (Phase) -> Unit,
    onOpenCalendar: () -> Unit,
    flags: List<CycleFlag>,
    onOpenStats: () -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    val info = p.info(today)
    val options = PeriodOps.optionsFor(data.bleeds, today, today)
    // Picking another day applies whatever that day's main action is (start, extend or move start).
    val onPick: (LocalDate) -> Unit = { d -> PeriodOps.optionsFor(data.bleeds, d, today).primary?.let { onAction(it, d) } }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(Fmt.full(today), color = C.Muted, fontSize = 14.sp)
                ScreenTitle("Hoy")
            }
            if (p.hasData && (info.kind == DayKind.FERTILE || info.kind == DayKind.OVULATION)) {
                Text(
                    if (info.kind == DayKind.OVULATION) "Ovulación" else "Días fértiles",
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(C.Lav)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    color = C.LavText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (!p.hasData) {
            CCard {
                Text("Marca tu regla", fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 24.sp)
                Text(
                    "Para calcular tus fechas, Ciclo necesita saber cuándo empezó tu última regla.",
                    color = C.Soft, fontSize = 15.sp, lineHeight = 22.sp,
                )
            }
            PrimaryButton("Me llegó hoy", { onAction(PeriodAction.START_NEW, today) })
            SecondaryButton("Empezó otro día", { picking = true })
            Text(
                "Si registras también las reglas anteriores (desde el Calendario), las predicciones serán más precisas.",
                color = C.Muted, fontSize = 13.sp, lineHeight = 19.sp,
            )
        } else {
            CycleRing(
                p, today,
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .clickable(onClickLabel = "Abrir calendario", onClick = onOpenCalendar),
            )
            StatusBlock(p, today, info.kind == DayKind.PERIOD)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.primary?.let { action ->
                    PrimaryButton(action.label(today, today, options), { onAction(action, today) })
                }
                SecondaryButton("Anotar cómo me siento hoy", onLog)
                if (p.periodAt(today) == null) {
                    TextButton({ picking = true }, Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Me llegó otro día", color = C.RedText, fontSize = 14.sp)
                    }
                }
            }

            if (p.stats.cyclesUsed == 0) {
                Text(
                    "Por ahora las fechas usan un ciclo de ${Fmt.days(p.stats.cycleLength)}. Con 2 reglas registradas empezarán a basarse en las tuyas.",
                    color = C.Muted, fontSize = 13.sp, lineHeight = 19.sp,
                )
            }

            if (flags.isNotEmpty()) {
                CCard(bg = C.WarnBg, border = C.WarnBorder) {
                    Kicker("A tener en cuenta", C.WarnText)
                    flags.forEach { Text("• " + it.text(), fontSize = 14.sp, lineHeight = 20.sp) }
                    Text(FLAGS_ADVICE, color = C.Soft, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }

            info.phase?.let { phase ->
                val content = PhaseContent.of(phase)
                CCard {
                    Kicker("Fase ${content.name.lowercase()}", phase.textColor())
                    Text(content.short, fontSize = 15.sp, lineHeight = 22.sp)
                    TextButton({ onLearn(phase) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text("Ver consejos de esta fase", color = C.RedText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }

            SecondaryButton("Ver mis patrones e informe", onOpenStats)
        }
    }

    if (picking) PastDatePicker(today, onPick = onPick, onDismiss = { picking = false })
}

@Composable
private fun StatusBlock(p: Prediction, today: LocalDate, inPeriod: Boolean) {
    val until = p.daysUntilNext ?: return
    val next = p.nextStart ?: return
    val (lead, big, sub) = when {
        inPeriod -> {
            val start = p.periodAt(today)!!.start
            Triple("Estás en el", "día ${java.time.temporal.ChronoUnit.DAYS.between(start, today) + 1} de tu regla", if (p.isOpen) "Cuando termine, toca «Terminó mi regla»." else "")
        }
        p.lateDays > 0 -> Triple(
            "Tu regla lleva", "${Fmt.days(p.lateDays)} de retraso",
            "Los ciclos varían. Si pudieras estar embarazada, considera hacerte una prueba.",
        )
        until == 0 -> Triple("Tu regla podría llegar", "hoy", "${Fmt.full(next)} · estimado")
        p.nextRange != null -> Triple(
            "Tu regla llega en", Fmt.days(until),
            "probablemente entre el ${Fmt.dayMonth(p.nextRange!!.start)} y el ${Fmt.dayMonth(p.nextRange!!.end)}",
        )
        else -> Triple("Tu regla llega en", Fmt.days(until), "${Fmt.full(next)} · estimado")
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(lead, color = C.Muted, fontSize = 15.sp)
        Text(big, fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, color = C.RedText, textAlign = TextAlign.Center)
        if (sub.isNotEmpty()) Text(sub, color = C.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CycleRing(p: Prediction, today: LocalDate, modifier: Modifier = Modifier) {
    val info = p.info(today)
    val day = info.cycleDay ?: return
    val length = info.cycleLength ?: p.stats.cycleLength
    val start = today.minusDays(day - 1L)
    // Segment colors come from the same phase logic the rest of the app uses.
    val phases = remember(p, today) { (0 until length).map { p.info(start.plusDays(it.toLong())).phase ?: Phase.LUTEA } }

    Box(modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2 + 14.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val per = 360f / length
            val gap = 1.2f
            var i = 0
            while (i < length) {
                var j = i
                while (j + 1 < length && phases[j + 1] == phases[i]) j++
                drawArc(
                    phases[i].ringColor(), startAngle = -90f + i * per + gap / 2, sweepAngle = (j - i + 1) * per - gap,
                    useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke),
                )
                i = j + 1
            }
            val shown = day.coerceAtMost(length)
            val angle = Math.toRadians((-90f + (shown - 0.5f) * per).toDouble())
            val r = arcSize.width / 2
            val c = Offset(size.width / 2 + r * cos(angle).toFloat(), size.height / 2 + r * sin(angle).toFloat())
            drawCircle(C.Text, radius = 13.dp.toPx(), center = c)
            drawCircle(C.Bg, radius = 13.dp.toPx(), center = c, style = Stroke(3.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Día", color = C.Muted, fontSize = 14.sp)
            Text("$day", fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 72.sp, lineHeight = 72.sp)
            val phase = info.phase
            if (p.lateDays > 0) {
                Text("Retraso", color = C.RedText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            } else if (phase != null) {
                Text("Fase ${PhaseContent.of(phase).name.lowercase()}", color = phase.textColor(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastDatePicker(today: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val max = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = max,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= max
            override fun isSelectableYear(year: Int) = year <= today.year
        },
    )
    val colors = DatePickerDefaults.colors(
        containerColor = C.Surface,
        selectedDayContainerColor = C.Red,
        selectedDayContentColor = Color.White,
        todayDateBorderColor = C.RedText,
        todayContentColor = C.RedText,
        selectedYearContainerColor = C.Red,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton({
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("Listo", color = C.RedText) }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar", color = C.Muted) } },
        colors = colors,
    ) {
        DatePicker(state, colors = colors, title = {
            Text("¿Qué día empezó tu regla?", Modifier.padding(start = 24.dp, top = 16.dp, end = 12.dp), color = C.Muted)
        })
    }
}
