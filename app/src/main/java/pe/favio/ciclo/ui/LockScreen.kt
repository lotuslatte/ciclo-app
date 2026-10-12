package pe.favio.ciclo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.favio.ciclo.R
import pe.favio.ciclo.security.Pin
import pe.favio.ciclo.ui.theme.C
import pe.favio.ciclo.ui.theme.Fraunces

@Composable
fun LockScreen(check: (String) -> Boolean, onUnlock: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun press(key: String) {
        if (busy) return
        wrong = false
        if (key == "<") {
            pin = pin.dropLast(1)
            return
        }
        if (pin.length >= Pin.LENGTH) return
        pin += key
        if (pin.length == Pin.LENGTH) {
            busy = true
            val attempt = pin
            scope.launch {
                val ok = withContext(Dispatchers.Default) { check(attempt) }
                busy = false
                if (ok) onUnlock() else {
                    wrong = true
                    pin = ""
                }
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(C.Bg).systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(R.drawable.ic_lock), null, tint = C.RedText, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(12.dp))
        Text("Ciclo", fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 34.sp)
        Text(if (wrong) "PIN incorrecto, intenta de nuevo" else "Escribe tu PIN", color = if (wrong) C.RedText else C.Muted, fontSize = 15.sp)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(Pin.LENGTH) { i ->
                Box(
                    Modifier.size(16.dp).clip(CircleShape)
                        .background(if (i < pin.length) C.RedText else C.BorderStrong),
                )
            }
        }
        Spacer(Modifier.height(36.dp))
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "<"))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(Modifier.size(76.dp))
                        } else {
                            Surface(
                                onClick = { press(key) },
                                modifier = Modifier
                                    .size(76.dp)
                                    .semantics { if (key == "<") contentDescription = "Borrar" },
                                shape = CircleShape,
                                color = C.Surface,
                                contentColor = C.Text,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (key == "<") Icon(painterResource(R.drawable.ic_backspace), null, Modifier.size(26.dp))
                                    else Text(key, fontSize = 28.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
