package pe.favio.ciclo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.favio.ciclo.content.PhaseContent
import pe.favio.ciclo.domain.Phase
import pe.favio.ciclo.ui.theme.C
import pe.favio.ciclo.ui.theme.textColor

@Composable
fun AprendeScreen(selected: Phase, current: Phase?, onSelect: (Phase) -> Unit) {
    val info = PhaseContent.of(selected)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("Aprende", Modifier.padding(horizontal = 20.dp))
        Text(
            "Información educativa basada en estudios científicos (ver las fuentes en cada consejo). No reemplaza la consulta con un profesional de salud.",
            Modifier.padding(horizontal = 20.dp), color = C.Muted, fontSize = 13.sp, lineHeight = 19.sp,
        )
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PhaseContent.phases.forEach { ph ->
                val on = ph.phase == selected
                Surface(
                    selected = on,
                    onClick = { onSelect(ph.phase) },
                    modifier = Modifier.heightIn(min = 44.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = if (on) C.Red else C.Surface,
                    contentColor = if (on) Color.White else C.Text,
                    border = BorderStroke(1.5.dp, if (on) C.Red else C.BorderStrong),
                ) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(ph.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CCard {
                Kicker(
                    "Fase ${info.name.lowercase()}" + if (selected == current) " · tu fase actual" else "",
                    selected.textColor(),
                )
                Text(info.what, fontSize = 15.sp, lineHeight = 22.sp)
                info.tips.forEach { tip ->
                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = C.Divider)
                    Text(tip.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(tip.body, color = C.Soft, fontSize = 14.sp, lineHeight = 20.sp)
                    SourceLine(tip.source)
                }
            }

            CCard(bg = C.WarnBg, border = C.WarnBorder) {
                Kicker("Cuándo consultar a un profesional", C.WarnText)
                PhaseContent.redFlags.forEach {
                    Row {
                        Text("•  ", color = C.WarnText, fontSize = 14.sp)
                        Text(it, color = C.Text, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
                Text(PhaseContent.redFlagsNote, color = C.Soft, fontSize = 13.sp)
                PhaseContent.redFlagSources.forEach { SourceLine(it) }
            }

            Text(
                "Información educativa. No reemplaza la consulta médica.",
                Modifier.fillMaxWidth(), color = C.Muted, fontSize = 12.sp, textAlign = TextAlign.Center,
            )
        }
    }
}
