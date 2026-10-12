package pe.favio.ciclo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.favio.ciclo.R
import pe.favio.ciclo.data.DayLog
import pe.favio.ciclo.domain.PeriodAction
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import pe.favio.ciclo.ui.theme.C
import pe.favio.ciclo.ui.theme.Fraunces
import java.time.LocalDate

@Composable
private fun Choice(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedBg: Color = C.Red,
    selectedBorder: Color = C.Red,
    selectedFg: Color = Color.White,
    content: @Composable () -> Unit,
) {
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) selectedBg else C.Surface,
        contentColor = if (selected) selectedFg else C.Text,
        border = BorderStroke(1.5.dp, if (selected) selectedBorder else C.BorderStrong),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegistroSheet(
    date: LocalDate,
    initial: DayLog,
    /** Set when this day is outside any period: saving with bleeding can mark it as one. */
    markAction: PeriodAction?,
    onSave: (log: DayLog, markPeriod: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var flow by remember { mutableIntStateOf(initial.flow) }
    var symptoms by remember { mutableStateOf(initial.symptoms.toSet()) }
    var mood by remember { mutableStateOf(initial.mood) }
    var note by remember { mutableStateOf(initial.note) }
    var pain by remember { mutableStateOf(initial.pain) }
    var limited by remember { mutableStateOf(initial.limited) }
    var markPeriod by remember { mutableStateOf(true) }
    var saved by remember { mutableStateOf(false) }

    // Every way of closing the sheet saves, so nothing typed is lost.
    fun saveAndClose() {
        if (!saved) {
            saved = true
            val log = DayLog(flow, SYMPTOMS.filter { it in symptoms }, mood, note.trim(), pain, limited)
            if (log != initial || (markAction != null && flow > 0 && markPeriod)) {
                onSave(log, markAction != null && flow > 0 && markPeriod)
            }
        }
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = ::saveAndClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = C.Bg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = C.BorderStrong) },
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Fmt.fullCap(date), Modifier.weight(1f),
                    fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 24.sp,
                )
                IconButton(::saveAndClose, colors = IconButtonDefaults.iconButtonColors(containerColor = C.Surface, contentColor = C.Text)) {
                    Icon(painterResource(R.drawable.ic_close), "Cerrar")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Kicker("Sangrado")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FLOW_LABELS.forEachIndexed { i, label ->
                        Choice(flow == i, { flow = i }, Modifier.weight(1f).heightIn(min = 56.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                if (i > 0) Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    repeat(i) { Drop(if (flow == i) Color.White else C.Red, 11.dp) }
                                }
                                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            if (markAction != null && flow > 0) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(C.RedTint)
                        .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Marcar como día de regla", color = C.RedLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            when (markAction) {
                                PeriodAction.EXTEND_END -> "Se sumará a tu regla anterior."
                                PeriodAction.MOVE_START_EARLIER -> "Tu siguiente regla empezará este día."
                                else -> "Empezará una regla este día."
                            },
                            color = C.Soft, fontSize = 13.sp,
                        )
                    }
                    Switch(
                        markPeriod, { markPeriod = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = C.Red, checkedThumbColor = Color.White, checkedBorderColor = C.Red,
                            uncheckedTrackColor = C.SwitchOff, uncheckedThumbColor = C.Soft, uncheckedBorderColor = C.SwitchOff,
                        ),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Kicker("Síntomas")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SYMPTOMS.forEach { s ->
                        val on = s in symptoms
                        Choice(
                            on, { symptoms = if (on) symptoms - s else symptoms + s },
                            Modifier.heightIn(min = 42.dp),
                            selectedBg = C.RedTint, selectedBorder = C.Red, selectedFg = C.RedLight,
                        ) {
                            Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(s, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Kicker("Dolor")
                Text("0 = nada · 10 = el peor dolor imaginable", color = C.Muted, fontSize = 13.sp)
                // 11 buttons need ~390dp to stay tappable; split in two rows on narrower screens.
                BoxWithConstraints {
                    val rows = if (maxWidth < 380.dp) listOf(0..5, 6..10) else listOf(0..10)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        rows.forEach { range ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                range.forEach { n ->
                                    val on = pain == n
                                    Choice(
                                        on, { pain = if (on) null else n },
                                        Modifier.weight(1f).heightIn(min = 44.dp),
                                        selectedBg = painColor(n), selectedBorder = painColor(n),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("$n", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                                // Keep the 6-button second row the same cell width as the first.
                                if (rows.size > 1 && range.count() < 6) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Me impidió hacer mis actividades", Modifier.weight(1f), fontSize = 15.sp)
                    Switch(
                        limited, { limited = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = C.Red, checkedThumbColor = Color.White, checkedBorderColor = C.Red,
                            uncheckedTrackColor = C.SwitchOff, uncheckedThumbColor = C.Soft, uncheckedBorderColor = C.SwitchOff,
                        ),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Kicker("Ánimo")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MOOD_LABELS.forEachIndexed { i, label ->
                        val on = mood == i
                        Choice(
                            on, { mood = if (on) null else i },
                            Modifier.weight(1f).heightIn(min = 62.dp),
                            selectedBg = C.Lav, selectedBorder = C.Ovul, selectedFg = C.LavText,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                MoodFace(i, if (on) C.LavText else C.Muted)
                                Text(label, fontSize = 11.sp, color = if (on) C.LavText else C.Muted)
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Kicker("Nota")
                OutlinedTextField(
                    note, { note = it },
                    Modifier.fillMaxWidth(),
                    placeholder = { Text("Algo que quieras recordar de hoy…", color = C.Faint) },
                    minLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = C.Surface, unfocusedContainerColor = C.Surface,
                        focusedBorderColor = C.RedText, unfocusedBorderColor = C.BorderStrong,
                        cursorColor = C.RedText,
                    ),
                )
            }

            PrimaryButton("Guardar", ::saveAndClose)
        }
    }
}

/** Violet for mild pain shading to crimson for severe; white text stays readable on all of them. */
fun painColor(n: Int): Color = when {
    n <= 3 -> C.Ovul
    n <= 6 -> Color(0xFF9E3A8C)
    else -> C.Red
}
