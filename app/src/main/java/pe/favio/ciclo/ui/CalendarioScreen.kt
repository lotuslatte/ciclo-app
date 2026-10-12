package pe.favio.ciclo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.favio.ciclo.R
import pe.favio.ciclo.content.PhaseContent
import pe.favio.ciclo.data.AppData
import pe.favio.ciclo.data.DayLog
import pe.favio.ciclo.domain.DayKind
import pe.favio.ciclo.domain.PeriodAction
import pe.favio.ciclo.domain.PeriodOps
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.favio.ciclo.domain.Prediction
import pe.favio.ciclo.ui.theme.C
import pe.favio.ciclo.ui.theme.Fraunces
import pe.favio.ciclo.ui.theme.textColor
import java.time.LocalDate
import java.time.YearMonth

val FLOW_LABELS = listOf("Nada", "Ligero", "Medio", "Abundante")
val MOOD_LABELS = listOf("Muy mal", "Mal", "Normal", "Bien", "Genial")
val SYMPTOMS = listOf(
    "Cólicos", "Dolor de cabeza", "Hinchazón", "Acné", "Senos sensibles",
    "Cansancio", "Antojos", "Náuseas", "Dolor de espalda", "Insomnio",
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarioScreen(
    p: Prediction,
    data: AppData,
    today: LocalDate,
    onLog: (LocalDate) -> Unit,
    onAction: (PeriodAction, LocalDate) -> Unit,
    onOpenStats: () -> Unit,
) {
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var selectedKey by rememberSaveable { mutableStateOf(today.toString()) }
    val month = YearMonth.parse(monthKey)
    val selected = LocalDate.parse(selectedKey)
    val dayCard = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                Fmt.month(month), Modifier.weight(1f),
                fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 28.sp,
            )
            if (month != YearMonth.from(today)) {
                TextButton({
                    monthKey = YearMonth.from(today).toString()
                    selectedKey = today.toString()
                }) { Text("Hoy", color = C.RedText, fontWeight = FontWeight.SemiBold) }
            }
            val navColors = IconButtonDefaults.iconButtonColors(containerColor = C.Surface, contentColor = C.Text)
            IconButton({ monthKey = month.minusMonths(1).toString() }, colors = navColors) {
                Icon(painterResource(R.drawable.ic_chevron_left), "Mes anterior")
            }
            Spacer(Modifier.size(4.dp))
            IconButton({ monthKey = month.plusMonths(1).toString() }, colors = navColors) {
                Icon(painterResource(R.drawable.ic_chevron_right), "Mes siguiente")
            }
        }

        CCard(Modifier, bg = C.Surface) {
            Row(Modifier.fillMaxWidth()) {
                listOf("L", "M", "M", "J", "V", "S", "D").forEach {
                    Text(it, Modifier.weight(1f), color = C.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
            }
            val first = month.atDay(1)
            val offset = first.dayOfWeek.value - 1
            val rows = (offset + month.lengthOfMonth() + 6) / 7
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (c in 0..6) {
                        val date = first.plusDays((r * 7 + c - offset).toLong())
                        DayCell(
                            date = date,
                            kind = p.info(date).kind,
                            inMonth = YearMonth.from(date) == month,
                            isToday = date == today,
                            isSelected = date == selected,
                            hasLog = data.logs.containsKey(date.toString()),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedKey = date.toString()
                                if (YearMonth.from(date) != month) monthKey = YearMonth.from(date).toString()
                                // Bring the day's card (and its buttons) into view.
                                scope.launch {
                                    delay(80)
                                    dayCard.bringIntoView()
                                }
                            },
                        )
                    }
                }
            }
        }

        Text("Toca un día para ver sus opciones.", color = C.Muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp))
        Box(Modifier.bringIntoViewRequester(dayCard)) {
            DayCard(p, data.log(selected), data, selected, today, onLog, onAction)
        }
        Legend()
        StatsRow(p)
        SecondaryButton("Ver mis patrones e informe", onOpenStats)
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    kind: DayKind,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    hasLog: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val (bg, fg) = when (kind) {
        DayKind.PERIOD -> C.Red to Color.White
        DayKind.OVULATION -> C.Ovul to Color.White
        DayKind.FERTILE -> C.Lav to C.Text
        DayKind.PREDICTED_PERIOD -> Color.Transparent to C.RedText
        DayKind.NONE -> Color.Transparent to (if (inMonth) C.Text else C.Faint)
    }
    val ring = when {
        isToday -> C.Text
        isSelected -> C.RedLight
        else -> null
    }
    Box(
        modifier
            .height(50.dp)
            .semantics {
                contentDescription = Fmt.full(date) + when (kind) {
                    DayKind.PERIOD -> ", regla"
                    DayKind.PREDICTED_PERIOD -> ", regla prevista"
                    DayKind.OVULATION -> ", ovulación estimada"
                    DayKind.FERTILE -> ", día fértil estimado"
                    DayKind.NONE -> ""
                } + (if (hasLog) ", con anotación" else "")
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .drawBehind {
                    if (ring != null) drawCircle(ring, radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
                }
                .padding(4.dp)
                .clip(CircleShape)
                .background(if (inMonth || kind != DayKind.NONE) bg else Color.Transparent)
                .drawBehind {
                    if (kind == DayKind.PREDICTED_PERIOD) {
                        drawCircle(
                            C.Red, radius = size.minDimension / 2 - 1.dp.toPx(),
                            style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))),
                        )
                    }
                }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${date.dayOfMonth}",
                color = if (inMonth) fg else fg.copy(alpha = 0.55f),
                fontSize = 15.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            )
            if (hasLog) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(fg),
                )
            }
        }
    }
}

@Composable
private fun Legend() {
    @Composable
    fun Item(label: String, modifier: Modifier, swatch: Modifier) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Box(swatch.size(14.dp))
            Spacer(Modifier.size(8.dp))
            Text(label, color = C.Soft, fontSize = 13.sp)
        }
    }
    Column(Modifier.padding(horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row {
            Item("Regla", Modifier.weight(1f), Modifier.clip(CircleShape).background(C.Red))
            Item("Regla prevista", Modifier.weight(1f), Modifier.drawBehind {
                drawCircle(C.Red, radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f))))
            })
        }
        Row {
            Item("Días fértiles (estimado)", Modifier.weight(1f), Modifier.clip(CircleShape).background(C.Lav))
            Item("Ovulación (estimada)", Modifier.weight(1f), Modifier.clip(CircleShape).background(C.Ovul))
        }
        Row {
            Item("Hoy", Modifier.weight(1f), Modifier.drawBehind {
                drawCircle(C.Text, radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
            })
            Item("Día con anotación", Modifier.weight(1f), Modifier.drawBehind { drawCircle(C.Text, radius = 2.5.dp.toPx()) })
        }
    }
}

@Composable
private fun DayCard(
    p: Prediction,
    log: DayLog,
    data: AppData,
    date: LocalDate,
    today: LocalDate,
    onLog: (LocalDate) -> Unit,
    onAction: (PeriodAction, LocalDate) -> Unit,
) {
    val info = p.info(date)
    val future = date.isAfter(today)
    val options = PeriodOps.optionsFor(data.bleeds, date, today)

    CCard {
        Text(Fmt.fullCap(date), fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
        val phase = info.phase
        if (phase != null && info.cycleDay != null) {
            Text(
                "Fase ${PhaseContent.of(phase).name.lowercase()} · día ${info.cycleDay} del ciclo" + if (future) " (estimado)" else "",
                color = phase.textColor(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            )
        }
        when (info.kind) {
            DayKind.PERIOD -> null // the hint below already says where in the period this day is
            DayKind.PREDICTED_PERIOD -> "Regla prevista"
            DayKind.OVULATION -> "Ovulación (estimada)"
            DayKind.FERTILE -> "Día fértil (estimado)"
            DayKind.NONE -> null
        }?.let { Text(it, color = C.Muted, fontSize = 14.sp) }
        (options.hint() ?: if (info.kind == DayKind.PERIOD) "Día de regla." else null)?.let {
            Text(it, color = C.Soft, fontSize = 14.sp, lineHeight = 20.sp)
        }

        if (!future) {
            if (log.isEmpty) {
                Text("Sin anotaciones.", color = C.Muted, fontSize = 14.sp)
            } else {
                val parts = buildList {
                    if (log.flow > 0) add("Sangrado ${FLOW_LABELS[log.flow].lowercase()}")
                    if (log.symptoms.isNotEmpty()) add(log.symptoms.joinToString(", "))
                    log.mood?.let { add("Ánimo: ${MOOD_LABELS[it].lowercase()}") }
                    log.pain?.let { add("Dolor $it/10") }
                    if (log.limited) add("limitó tus actividades")
                }
                if (parts.isNotEmpty()) Text(parts.joinToString(" · "), fontSize = 15.sp, lineHeight = 21.sp)
                if (log.note.isNotBlank()) Text("“${log.note}”", color = C.Soft, fontSize = 14.sp, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(4.dp))
            options.primary?.let { action ->
                PrimaryButton(action.label(date, today, options), { onAction(action, date) })
            }
            SecondaryButton(if (log.isEmpty) "Anotar este día" else "Editar anotación", { onLog(date) }, height = 48.dp)
            options.secondary.forEach { action ->
                TextButton({ onAction(action, date) }, Modifier.fillMaxWidth()) {
                    Text(
                        action.label(date, today, options),
                        color = if (action == PeriodAction.REMOVE) C.RedText else C.Soft,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(p: Prediction) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Stat("Ciclo promedio", Fmt.days(p.stats.cycleLength), C.Text, Modifier.weight(1f))
        Stat("Regla promedio", Fmt.days(p.stats.periodLength), C.Text, Modifier.weight(1f))
        Stat("Próxima regla", p.nextStart?.let { Fmt.short(it) } ?: "—", C.RedText, Modifier.weight(1f))
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(CardShape)
            .background(C.Surface)
            .padding(12.dp),
    ) {
        Text(label, color = C.Muted, fontSize = 12.sp)
        Text(value, color = color, fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
    }
}
