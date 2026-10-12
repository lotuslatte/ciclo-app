package pe.favio.ciclo.ui

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.favio.ciclo.LockGuard
import pe.favio.ciclo.data.AppData
import pe.favio.ciclo.report.DoctorReport
import pe.favio.ciclo.ui.theme.C
import java.time.LocalDate

/** Pick a period, then view or share the PDF. Everything happens on the phone. */
@Composable
fun ReportDialog(data: AppData, today: LocalDate, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var months by remember { mutableIntStateOf(6) }

    fun run(share: Boolean) {
        runCatching {
            val file = DoctorReport.build(ctx, data, months, today)
            LockGuard.skipNext()
            ctx.startActivity(if (share) DoctorReport.shareIntent(ctx, file) else DoctorReport.viewIntent(ctx, file))
        }.onFailure {
            val msg = if (it is ActivityNotFoundException) "No hay una app para ver PDF. Usa «Compartir»." else "No se pudo crear el informe."
            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Surface,
        title = { Text("Informe para tu médico") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Un PDF con tus ciclos, duración de las reglas, dolor, síntomas y la comparación con los rangos habituales. Se crea en tu teléfono; tú decides con quién compartirlo.",
                    color = C.Soft, fontSize = 14.sp, lineHeight = 20.sp,
                )
                Text("Periodo", color = C.Muted, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 6, 12).forEach { m ->
                        val on = months == m
                        Surface(
                            selected = on,
                            onClick = { months = m },
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (on) C.RedTint else C.Bg,
                            contentColor = if (on) C.RedLight else C.Text,
                            border = BorderStroke(1.5.dp, if (on) C.Red else C.BorderStrong),
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text("$m meses", fontWeight = FontWeight.Medium) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton({ run(share = true) }) { Text("Compartir", color = C.RedText) } },
        dismissButton = {
            Row {
                TextButton(onDismiss) { Text("Cancelar", color = C.Muted) }
                TextButton({ run(share = false) }) { Text("Ver", color = C.RedText) }
            }
        },
    )
}
