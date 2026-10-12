package pe.favio.ciclo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import pe.favio.ciclo.BetaSupport
import pe.favio.ciclo.BuildConfig
import pe.favio.ciclo.data.Store
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import pe.favio.ciclo.LockGuard
import pe.favio.ciclo.R
import pe.favio.ciclo.data.AppData
import pe.favio.ciclo.data.Backup
import pe.favio.ciclo.data.BackupCodec
import pe.favio.ciclo.data.Settings
import pe.favio.ciclo.domain.Prediction
import pe.favio.ciclo.notify.Reminders
import pe.favio.ciclo.security.Pin
import pe.favio.ciclo.ui.theme.C
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun AjustesScreen(
    data: AppData,
    p: Prediction,
    today: LocalDate,
    onSettings: ((Settings) -> Settings) -> Unit,
    onRestore: (Backup) -> Unit,
    onWipe: () -> Unit,
    onShowGuide: () -> Unit,
    copyDates: () -> List<LocalDate>,
    onRestoreCopy: (LocalDate) -> Boolean,
) {
    val ctx = LocalContext.current
    val s = data.settings
    var pinDialog by remember { mutableStateOf<PinMode?>(null) }
    var pendingImport by remember { mutableStateOf<Backup?>(null) }
    var confirmWipe by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf(false) }
    var copies by remember { mutableStateOf<List<LocalDate>?>(null) }
    var crashLog by remember { mutableStateOf(BetaSupport.crashLog(ctx)) }
    fun launch(intent: android.content.Intent) {
        LockGuard.skipNext()
        runCatching { ctx.startActivity(intent) }
    }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(ctx, "Sin permiso de notificaciones no podremos avisarte.", Toast.LENGTH_LONG).show()
    }
    fun ensureNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            ctx.contentResolver.openOutputStream(uri, "wt")!!.use {
                it.write(BackupCodec.encode(data, LocalDateTime.now().withNano(0).toString()).encodeToByteArray())
            }
        }.onSuccess { Toast.makeText(ctx, "Respaldo guardado", Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(ctx, "No se pudo guardar el respaldo", Toast.LENGTH_LONG).show() }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val text = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            BackupCodec.decode(text)
        }.onSuccess { pendingImport = it }
            .onFailure { Toast.makeText(ctx, it.message ?: "Archivo no válido", Toast.LENGTH_LONG).show() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle("Ajustes")

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.OkBg).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_lock), null, tint = C.OkText, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Sin internet. Tus datos se guardan solo en este teléfono.", color = C.OkText, fontSize = 14.sp, lineHeight = 20.sp)
        }

        Section("Versión de prueba")
        Group {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Estás usando una versión de prueba. Cuéntanos qué te gusta, qué te confunde o qué falla.",
                    color = C.Soft, fontSize = 14.sp, lineHeight = 20.sp,
                )
                SecondaryButton("Enviar comentarios", { launch(BetaSupport.feedbackIntent()) }, height = 48.dp)
                crashLog?.let { log ->
                    Text("La app se cerró por un error. Puedes enviarnos el detalle técnico (no incluye tus datos).", color = C.WarnText, fontSize = 13.sp, lineHeight = 19.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton("Enviar reporte de error", { launch(BetaSupport.errorReportIntent(log)) }, Modifier.weight(1f), height = 48.dp)
                        TextButton({ BetaSupport.clearCrashLog(ctx); crashLog = null }) { Text("Borrar", color = C.Muted) }
                    }
                }
            }
        }

        Section("Recordatorios")
        Group {
            SwitchRow("Aviso antes de la regla", if (s.remindPeriod) "${Fmt.days(s.remindDaysBefore)} antes" else null, s.remindPeriod) { on ->
                if (on) ensureNotifPermission()
                onSettings { it.copy(remindPeriod = on) }
            }
            if (s.remindPeriod) {
                Row(Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..3).forEach { n ->
                        val on = s.remindDaysBefore == n
                        TextButton(
                            { onSettings { it.copy(remindDaysBefore = n) } },
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (on) C.RedTint else C.Bg)
                                .border(1.5.dp, if (on) C.Red else C.BorderStrong, RoundedCornerShape(12.dp)),
                        ) { Text(Fmt.days(n), color = if (on) C.RedLight else C.Text) }
                    }
                }
            }
            HorizontalDivider(color = C.Divider)
            SwitchRow("Aviso de días fértiles", null, s.remindFertile) { on ->
                if (on) ensureNotifPermission()
                onSettings { it.copy(remindFertile = on) }
            }
            HorizontalDivider(color = C.Divider)
            TextButton(
                {
                    ensureNotifPermission()
                    if (Reminders.canNotify(ctx)) {
                        Reminders.show(ctx, 99, "Ciclo", "Así se verán tus recordatorios.")
                    } else {
                        Toast.makeText(ctx, "Activa las notificaciones de Ciclo en los ajustes del teléfono.", Toast.LENGTH_LONG).show()
                    }
                },
                Modifier.padding(horizontal = 4.dp),
            ) { Text("Probar notificación", color = C.RedText) }
        }

        Section("Mi ciclo")
        Group {
            StepperRow(
                "Duración del ciclo",
                if (p.stats.cyclesUsed > 0) "Usando tu promedio: ${Fmt.days(p.stats.cycleLength)}" else "Se usa hasta que registres 2 reglas",
                s.defaultCycle, 21..45,
            ) { v -> onSettings { it.copy(defaultCycle = v) } }
            HorizontalDivider(color = C.Divider)
            StepperRow("Duración de la regla", "Se ajusta sola con tus registros", s.defaultPeriod, 2..10) { v ->
                onSettings { it.copy(defaultPeriod = v) }
            }
        }

        Section("Privacidad")
        Group {
            SwitchRow("Bloquear con PIN", "Se pide al abrir la app", s.pinEnabled) { on ->
                pinDialog = if (on) PinMode.CREATE else PinMode.REMOVE
            }
        }

        Section("Informe para tu médico")
        Group {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Un PDF con tus ciclos, reglas, dolor y síntomas para llevar a tu consulta.",
                    color = C.Soft, fontSize = 14.sp, lineHeight = 20.sp,
                )
                SecondaryButton("Crear informe PDF", { report = true }, height = 48.dp)
            }
        }

        Section("Copia de seguridad")
        Group {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Guarda un archivo con tus datos para no perderlos si cambias de teléfono. El archivo no incluye tu PIN.",
                    color = C.Soft, fontSize = 14.sp, lineHeight = 20.sp,
                )
                Text(
                    "Además, la app guarda sola una copia diaria de los últimos ${Store.KEEP_COPIES} días.",
                    color = C.Muted, fontSize = 13.sp, lineHeight = 19.sp,
                )
                TextButton({ copies = copyDates() }) { Text("Restaurar una copia automática", color = C.RedText) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("Exportar", {
                        LockGuard.skipNext()
                        exporter.launch("ciclo-respaldo-$today.json")
                    }, Modifier.weight(1f), height = 48.dp)
                    SecondaryButton("Importar", {
                        LockGuard.skipNext()
                        importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                    }, Modifier.weight(1f), height = 48.dp)
                }
            }
        }

        Section("Ayuda")
        Group {
            TextButton(onShowGuide, Modifier.padding(horizontal = 4.dp)) { Text("Ver guía de uso", color = C.RedText) }
        }

        TextButton({ confirmWipe = true }) { Text("Borrar todos mis datos", color = C.RedText) }

        Text(
            "Esta app estima fechas y no sirve como método anticonceptivo. La información es educativa y no reemplaza la consulta médica.",
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.WarnBg).padding(horizontal = 14.dp, vertical = 12.dp),
            color = C.WarnText, fontSize = 13.sp, lineHeight = 19.sp,
        )
        Text("Ciclo beta · versión ${BuildConfig.VERSION_NAME}", Modifier.fillMaxWidth(), color = C.Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        TextButton(
            { launch(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(AUTHOR_URL))) },
            Modifier.fillMaxWidth(),
        ) {
            Text("Creado por Favio Samillan", color = C.RedText, fontSize = 13.sp, textDecoration = TextDecoration.Underline)
        }
    }

    pinDialog?.let { mode ->
        PinDialog(
            mode = mode,
            verify = { Pin.verify(s, it) },
            onDone = { pin ->
                onSettings { if (mode == PinMode.CREATE) Pin.set(it, pin!!) else it.copy(pinHash = null, pinSalt = null) }
                pinDialog = null
            },
            onDismiss = { pinDialog = null },
        )
    }

    pendingImport?.let { b ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            containerColor = C.Surface,
            title = { Text("¿Reemplazar tus datos?") },
            text = { Text("Se reemplazarán tus datos actuales por los del respaldo: ${b.periods.size} reglas y ${b.logs.size} días anotados.") },
            confirmButton = {
                TextButton({ onRestore(b); pendingImport = null; Toast.makeText(ctx, "Datos restaurados", Toast.LENGTH_SHORT).show() }) {
                    Text("Reemplazar", color = C.RedText)
                }
            },
            dismissButton = { TextButton({ pendingImport = null }) { Text("Cancelar", color = C.Muted) } },
        )
    }

    if (report) ReportDialog(data, today, onDismiss = { report = false })

    copies?.let { dates ->
        var chosen by remember { mutableStateOf(dates.firstOrNull()) }
        AlertDialog(
            onDismissRequest = { copies = null },
            containerColor = C.Surface,
            title = { Text("Restaurar copia automática") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (dates.isEmpty()) {
                        Text("Todavía no hay copias. Se crea una cada día que usas la app.")
                    } else {
                        Text("Tus datos actuales se reemplazarán por los de la copia elegida.", color = C.Soft, fontSize = 14.sp)
                        dates.forEach { d ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 44.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(chosen == d, { chosen = d }, colors = RadioButtonDefaults.colors(selectedColor = C.RedText))
                                Text(Fmt.fullCap(d))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (dates.isNotEmpty()) {
                    TextButton({
                        val ok = chosen?.let(onRestoreCopy) ?: false
                        Toast.makeText(ctx, if (ok) "Copia restaurada" else "No se pudo leer esa copia", Toast.LENGTH_SHORT).show()
                        copies = null
                    }) { Text("Restaurar", color = C.RedText) }
                }
            },
            dismissButton = { TextButton({ copies = null }) { Text("Cancelar", color = C.Muted) } },
        )
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            containerColor = C.Surface,
            title = { Text("¿Borrar todo?") },
            text = { Text("Se borrarán todas tus reglas, anotaciones y ajustes de este teléfono. Si no tienes un respaldo, no se podrán recuperar.") },
            confirmButton = { TextButton({ onWipe(); confirmWipe = false }) { Text("Borrar", color = C.RedText) } },
            dismissButton = { TextButton({ confirmWipe = false }) { Text("Cancelar", color = C.Muted) } },
        )
    }
}

@Composable
private fun Section(text: String) {
    Kicker(text, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
}

@Composable
internal fun Group(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(C.Surface)
            .border(1.dp, C.Border, CardShape),
        content = content,
    )
}

@Composable
internal fun SwitchRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (sub != null) Text(sub, color = C.Muted, fontSize = 13.sp)
        }
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = C.Red, checkedThumbColor = Color.White, checkedBorderColor = C.Red,
                uncheckedTrackColor = C.SwitchOff, uncheckedThumbColor = C.Soft, uncheckedBorderColor = C.SwitchOff,
            ),
        )
    }
}

@Composable
internal fun StepperRow(title: String, sub: String, value: Int, range: IntRange, enabled: Boolean = true, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(sub, color = C.Muted, fontSize = 13.sp)
        }
        val colors = IconButtonDefaults.iconButtonColors(containerColor = C.Bg, contentColor = C.Text)
        IconButton({ onChange((value - 1).coerceIn(range)) }, colors = colors, enabled = enabled && value > range.first) {
            Text("−", fontSize = 20.sp)
        }
        Text("$value", Modifier.width(36.dp), color = if (enabled) C.RedText else C.Faint, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        IconButton({ onChange((value + 1).coerceIn(range)) }, colors = colors, enabled = enabled && value < range.last) {
            Text("+", fontSize = 20.sp)
        }
    }
}

private const val AUTHOR_URL = "https://lotuslatte.github.io/"

enum class PinMode { CREATE, REMOVE }

@Composable
private fun PinDialog(mode: PinMode, verify: (String) -> Boolean, onDone: (String?) -> Unit, onDismiss: () -> Unit) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = C.Bg, unfocusedContainerColor = C.Bg,
        focusedBorderColor = C.RedText, unfocusedBorderColor = C.BorderStrong, cursorColor = C.RedText,
    )
    fun digits(v: String) = v.filter(Char::isDigit).take(Pin.LENGTH)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Surface,
        title = { Text(if (mode == PinMode.CREATE) "Crea tu PIN" else "Quitar el PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (mode == PinMode.CREATE) "4 números. Si lo olvidas, tendrás que borrar los datos de la app para volver a entrar."
                    else "Escribe tu PIN actual.",
                    color = C.Soft, fontSize = 14.sp,
                )
                OutlinedTextField(
                    first, { first = digits(it); error = null },
                    label = { Text(if (mode == PinMode.CREATE) "PIN" else "PIN actual") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), colors = fieldColors,
                )
                if (mode == PinMode.CREATE) {
                    OutlinedTextField(
                        second, { second = digits(it); error = null },
                        label = { Text("Repite el PIN") },
                        singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), colors = fieldColors,
                    )
                }
                error?.let { Text(it, color = C.RedText, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton({
                when {
                    first.length != Pin.LENGTH -> error = "El PIN debe tener 4 números"
                    mode == PinMode.CREATE && first != second -> error = "Los PIN no coinciden"
                    mode == PinMode.REMOVE && !verify(first) -> error = "PIN incorrecto"
                    else -> onDone(first)
                }
            }) { Text("Listo", color = C.RedText) }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar", color = C.Muted) } },
    )
}
