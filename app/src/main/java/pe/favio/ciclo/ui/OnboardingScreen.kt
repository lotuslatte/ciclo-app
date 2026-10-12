package pe.favio.ciclo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import pe.favio.ciclo.data.Settings
import pe.favio.ciclo.domain.Phase
import pe.favio.ciclo.ui.theme.C
import pe.favio.ciclo.ui.theme.Fraunces
import pe.favio.ciclo.ui.theme.ringColor
import java.time.LocalDate

/** What the setup page collected. [lastStart] is null when the user will mark it later. */
data class SetupResult(val settings: Settings, val lastStart: LocalDate?)

private const val PAGES = 5

@Composable
fun OnboardingScreen(
    initial: Settings,
    hasData: Boolean,
    today: LocalDate,
    onFinish: (SetupResult) -> Unit,
) {
    val pager = rememberPagerState { PAGES }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    var unknownCycle by remember { mutableStateOf(false) }
    var cycle by remember { mutableStateOf(initial.defaultCycle) }
    var period by remember { mutableStateOf(initial.defaultPeriod) }
    var reminders by remember { mutableStateOf(initial.remindPeriod) }
    var lastStart by remember { mutableStateOf<LocalDate?>(null) }
    var picking by remember { mutableStateOf(false) }
    // Beta consent: required once; replaying the guide later starts accepted.
    var accepted by remember { mutableStateOf(initial.onboarded) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun askPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun finish() {
        if (reminders) askPermission()
        onFinish(
            SetupResult(
                initial.copy(
                    defaultCycle = if (unknownCycle) 28 else cycle,
                    defaultPeriod = period,
                    remindPeriod = reminders,
                    onboarded = true,
                ),
                lastStart,
            ),
        )
    }

    Column(Modifier.fillMaxSize().background(C.Bg).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${pager.currentPage + 1} de $PAGES", Modifier.weight(1f).padding(start = 8.dp),
                color = C.Muted, fontSize = 13.sp,
            )
            if (accepted && pager.currentPage < PAGES - 1) {
                TextButton({ scope.launch { pager.animateScrollToPage(PAGES - 1) } }) { Text("Saltar", color = C.Muted) }
            }
        }

        HorizontalPager(pager, Modifier.weight(1f), userScrollEnabled = accepted) { page ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                when (page) {
                    0 -> WelcomePage(accepted) { accepted = it }
                    1 -> MarkPage()
                    2 -> ColorsPage()
                    3 -> LogPage()
                    else -> SetupPage(
                        hasData = hasData,
                        today = today,
                        unknownCycle = unknownCycle, onUnknownCycle = { unknownCycle = it },
                        cycle = cycle, onCycle = { cycle = it },
                        period = period, onPeriod = { period = it },
                        reminders = reminders, onReminders = { reminders = it; if (it) askPermission() },
                        lastStart = lastStart, onLastStart = { lastStart = it },
                        onPickDate = { picking = true },
                    )
                }
            }
        }

        Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(PAGES) { i ->
                    Box(
                        Modifier
                            .height(8.dp)
                            .width(if (i == pager.currentPage) 22.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (i == pager.currentPage) C.RedText else C.BorderStrong),
                    )
                }
            }
            if (pager.currentPage < PAGES - 1) {
                PrimaryButton(
                    "Siguiente", { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    enabled = accepted,
                )
            } else {
                PrimaryButton("Empezar", ::finish)
            }
        }
    }

    if (picking) PastDatePicker(today, onPick = { lastStart = it }, onDismiss = { picking = false })
}

@Composable
private fun PageTitle(text: String) {
    Text(text, fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp)
}

@Composable
private fun Body(text: String, color: Color = C.Soft) {
    Text(text, color = color, fontSize = 16.sp, lineHeight = 24.sp)
}

@Composable
private fun MiniRing(modifier: Modifier = Modifier) {
    Canvas(modifier.size(150.dp)) {
        val stroke = 14.dp.toPx()
        val inset = stroke / 2
        val arc = Size(size.width - stroke, size.height - stroke)
        val parts = listOf(Phase.MENSTRUAL to 5, Phase.FOLICULAR to 7, Phase.OVULATORIA to 3, Phase.LUTEA to 13)
        var start = -90f
        parts.forEach { (phase, days) ->
            val sweep = days / 28f * 360f
            drawArc(phase.ringColor(), start + 1f, sweep - 2f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            start += sweep
        }
    }
}

@Composable
private fun ColumnScope.WelcomePage(accepted: Boolean, onAccept: (Boolean) -> Unit) {
    Spacer(Modifier.height(12.dp))
    MiniRing(Modifier.align(Alignment.CenterHorizontally))
    PageTitle("Te damos la bienvenida a Ciclo")
    Body("Registra tu regla y cómo te sientes. Ciclo calcula cuándo llegará la próxima, en qué fase estás y te da consejos con respaldo científico.")
    CCard(bg = C.OkBg, border = C.OkBg) {
        Text("Tus datos no salen de este teléfono.", color = C.OkText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text("La app no usa internet. Puedes guardar un respaldo cuando quieras desde Ajustes.", color = C.OkText, fontSize = 14.sp, lineHeight = 20.sp)
    }
    CCard {
        Kicker("Versión de prueba", C.RedText)
        Text(
            "Podrías encontrar errores. Si algo falla o te confunde, cuéntanos desde Ajustes → Enviar comentarios.",
            color = C.Soft, fontSize = 14.sp, lineHeight = 20.sp,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .toggleable(value = accepted, onValueChange = onAccept, role = Role.Checkbox)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(accepted, null, colors = CheckboxDefaults.colors(checkedColor = C.Red, uncheckedColor = C.Muted))
            Spacer(Modifier.width(8.dp))
            Text(
                "Entiendo que es una versión de prueba y que la app no reemplaza la consulta médica.",
                fontSize = 14.sp, lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun FakeButton(text: String, primary: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) C.Red else C.Surface)
            .border(1.5.dp, if (primary) C.Red else C.BorderStrong, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (primary) Color.White else C.Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
}

@Composable
private fun Step(n: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(C.RedTint), contentAlignment = Alignment.Center) {
            Text("$n", color = C.RedLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, color = C.Soft, fontSize = 15.sp, lineHeight = 22.sp)
    }
}

@Composable
private fun MarkPage() {
    PageTitle("Marca tu regla")
    Step(1, "El día que te llegue, en la pantalla Hoy toca:")
    FakeButton("Me llegó la regla", primary = true)
    Step(2, "Cuando termine, el mismo botón cambia. Tócalo:")
    FakeButton("Terminó mi regla", primary = true)
    Step(3, "¿Olvidaste marcarla o te equivocaste? Ve al Calendario y toca ese día. Los botones se adaptan: si el día está justo después de una regla te ofrece alargarla, y si está justo antes, adelantar su inicio.")
    Body("Si te equivocas, aparece un aviso con «Deshacer».", C.Muted)
}

@Composable
private fun LegendLine(label: String, swatch: Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(swatch.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = C.Soft, fontSize = 15.sp)
    }
}

@Composable
private fun ColorsPage() {
    PageTitle("Lee tus colores")
    Body("El anillo de Hoy muestra tu ciclo completo, y el punto blanco marca dónde estás:")
    Row(verticalAlignment = Alignment.CenterVertically) {
        MiniRing(Modifier.size(110.dp))
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Phase.MENSTRUAL to "Menstrual", Phase.FOLICULAR to "Folicular", Phase.OVULATORIA to "Ovulatoria", Phase.LUTEA to "Lútea").forEach { (ph, name) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(ph.ringColor()))
                    Spacer(Modifier.width(8.dp))
                    Text(name, color = C.Soft, fontSize = 14.sp)
                }
            }
        }
    }
    HorizontalDivider(color = C.Divider)
    Body("En el Calendario:")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LegendLine("Regla que registraste", Modifier.clip(CircleShape).background(C.Red))
        LegendLine("Regla prevista", Modifier.drawBehind {
            drawCircle(C.Red, radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))))
        })
        LegendLine("Días fértiles (estimado)", Modifier.clip(CircleShape).background(C.Lav))
        LegendLine("Ovulación (estimada)", Modifier.clip(CircleShape).background(C.Ovul))
        LegendLine("Hoy", Modifier.drawBehind { drawCircle(C.Text, radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(2.dp.toPx())) })
    }
    Body("Las fechas son estimaciones y mejoran a medida que registras más reglas.", C.Muted)
}

@Composable
private fun LogPage() {
    PageTitle("Anota y aprende")
    Step(1, "Toca «Anotar cómo me siento hoy» para registrar sangrado, síntomas, ánimo y una nota. Si anotas sangrado, puedes marcar ese día como regla.")
    Step(2, "En Aprende encontrarás qué pasa en cada fase, qué ayuda con los síntomas y cuándo consultar a un profesional. Cada consejo muestra su fuente.")
    Step(3, "En Ajustes puedes activar recordatorios, poner un PIN y guardar un respaldo.")
    CCard(bg = C.WarnBg, border = C.WarnBorder) {
        Text(
            "Ciclo no sirve como método anticonceptivo. La información es educativa y no reemplaza la consulta médica.",
            color = C.WarnText, fontSize = 14.sp, lineHeight = 20.sp,
        )
    }
}

@Composable
private fun SetupChoice(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier.heightIn(min = 46.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) C.RedTint else C.Surface,
        contentColor = if (selected) C.RedLight else C.Text,
        border = BorderStroke(1.5.dp, if (selected) C.Red else C.BorderStrong),
    ) {
        Box(contentAlignment = Alignment.Center) { Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center) }
    }
}

@Composable
private fun SetupPage(
    hasData: Boolean,
    today: LocalDate,
    unknownCycle: Boolean, onUnknownCycle: (Boolean) -> Unit,
    cycle: Int, onCycle: (Int) -> Unit,
    period: Int, onPeriod: (Int) -> Unit,
    reminders: Boolean, onReminders: (Boolean) -> Unit,
    lastStart: LocalDate?, onLastStart: (LocalDate?) -> Unit,
    onPickDate: () -> Unit,
) {
    PageTitle("Empecemos")
    Body("Con estos datos las primeras predicciones serán más precisas. Puedes cambiarlos después en Ajustes.")

    Group {
        StepperRow("Tu ciclo suele durar", if (unknownCycle) "Usaremos 28 días" else "Del primer día de una regla al de la siguiente", cycle, 21..45, enabled = !unknownCycle, onChange = onCycle)
        Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
            SetupChoice("No lo sé", unknownCycle, { onUnknownCycle(!unknownCycle) }, Modifier.width(120.dp))
        }
        HorizontalDivider(color = C.Divider)
        StepperRow("Tu regla suele durar", "Días de sangrado", period, 2..10, onChange = onPeriod)
    }

    if (!hasData) {
        Text("¿Cuándo empezó tu última regla?", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        val pickedOther = lastStart != null && lastStart != today
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SetupChoice("Hoy", lastStart == today, { onLastStart(today) }, Modifier.weight(1f))
            SetupChoice(if (pickedOther) Fmt.short(lastStart!!) else "Elegir fecha", pickedOther, onPickDate, Modifier.weight(1f))
            SetupChoice("Después", lastStart == null, { onLastStart(null) }, Modifier.weight(1f))
        }
        if (lastStart == null) Text("Podrás marcarla en Hoy o en el Calendario.", color = C.Muted, fontSize = 13.sp)
    }

    Group {
        SwitchRow("Avisarme antes de la regla", "Una notificación 2 días antes", reminders, onReminders)
    }
}
