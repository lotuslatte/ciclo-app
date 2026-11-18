package pe.favio.ciclo.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.favio.ciclo.LockGuard
import pe.favio.ciclo.R
import pe.favio.ciclo.content.PrivacyPolicy
import pe.favio.ciclo.ui.theme.C

/** Full privacy policy, readable offline. Opened from Ajustes. */
@Composable
fun PrivacidadScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val ctx = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack, colors = IconButtonDefaults.iconButtonColors(containerColor = C.Surface, contentColor = C.Text)) {
                Icon(painterResource(R.drawable.ic_chevron_left), "Volver")
            }
            Spacer(Modifier.width(12.dp))
            // Smaller than the tab titles: the phrase is long and must fit on narrow phones.
            Text(
                "Política de privacidad",
                Modifier.semantics { heading() },
                fontFamily = pe.favio.ciclo.ui.theme.Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp,
            )
        }
        Text("Vigente desde el ${PrivacyPolicy.UPDATED}", color = C.Muted, fontSize = 13.sp)

        CCard(bg = C.OkBg, border = C.OkBg) {
            Text(PrivacyPolicy.SUMMARY, color = C.OkText, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
        }

        PrivacyPolicy.sections.forEach { section ->
            CCard {
                Text(section.title, Modifier.semantics { heading() }, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                section.paragraphs.forEach { Text(it, color = C.Soft, fontSize = 14.sp, lineHeight = 21.sp) }
            }
        }

        TextButton(
            {
                // Opens the browser; Ciclo itself still has no network access.
                LockGuard.skipNext()
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PrivacyPolicy.ISSUES_URL))) }
                    .onFailure { LockGuard.consume() }
            },
            Modifier.fillMaxWidth(),
        ) {
            Text("Abrir la página del proyecto en GitHub", color = C.RedText, fontSize = 14.sp, textDecoration = TextDecoration.Underline)
        }
        Text(
            PrivacyPolicy.ISSUES_URL,
            Modifier.fillMaxWidth().padding(bottom = 8.dp), color = C.Muted, fontSize = 12.sp,
        )
    }
}
