@file:OptIn(ExperimentalTextApi::class)

package pe.favio.ciclo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import pe.favio.ciclo.R
import pe.favio.ciclo.domain.Phase

/** Palette from the approved design (dark plum + crimson + violet). */
object C {
    val Bg = Color(0xFF1C1320)
    val Surface = Color(0xFF2A1D30)
    val Border = Color(0xFF3A2A42)
    val BorderStrong = Color(0xFF4A3854)
    val Divider = Color(0xFF34253B)
    val Text = Color(0xFFF3EAF3)
    val Muted = Color(0xFFB9A8BD)
    val Soft = Color(0xFFD6C8D9)
    val Faint = Color(0xFF7E6D84)

    val Red = Color(0xFFC8374F)
    val RedText = Color(0xFFFF8093)
    val RedTint = Color(0xFF4A2033)
    val RedLight = Color(0xFFFFB1BD)

    val Lav = Color(0xFF3A2D5C)
    val LavBorder = Color(0xFF6B58A8)
    val LavText = Color(0xFFCDBEFF)
    val Ovul = Color(0xFF7657D1)

    val Fol = Color(0xFFD98FB6)
    val FolText = Color(0xFFE9A6C8)
    val OvulRing = Color(0xFF8B6FE0)
    val Lut = Color(0xFF4B3B7A)
    val LutText = Color(0xFFB6A3F0)

    val WarnBg = Color(0xFF3A1520)
    val WarnBorder = Color(0xFF6A2635)
    val WarnText = Color(0xFFFFB3B3)
    val OkBg = Color(0xFF173A33)
    val OkText = Color(0xFF9FE0C9)
    val SwitchOff = Color(0xFF55465C)
}

fun Phase.ringColor(): Color = when (this) {
    Phase.MENSTRUAL -> C.Red
    Phase.FOLICULAR -> C.Fol
    Phase.OVULATORIA -> C.OvulRing
    Phase.LUTEA -> C.Lut
}

fun Phase.textColor(): Color = when (this) {
    Phase.MENSTRUAL -> C.RedText
    Phase.FOLICULAR -> C.FolText
    Phase.OVULATORIA -> C.LavText
    Phase.LUTEA -> C.LutText
}

private fun fraunces(w: Int) = Font(
    R.font.fraunces,
    FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w), FontVariation.Setting("opsz", 72f)),
)

private fun dmSans(w: Int) = Font(
    R.font.dmsans,
    FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w)),
)

val Fraunces = FontFamily(fraunces(500), fraunces(600))
val DmSans = FontFamily(dmSans(400), dmSans(500), dmSans(600), dmSans(700))

private fun TextStyle.sans() = copy(fontFamily = DmSans)

private val base = Typography()
private val typography = Typography(
    displayLarge = base.displayLarge.sans(),
    displayMedium = base.displayMedium.sans(),
    displaySmall = base.displaySmall.sans(),
    headlineLarge = base.headlineLarge.sans(),
    headlineMedium = base.headlineMedium.sans(),
    headlineSmall = base.headlineSmall.sans(),
    titleLarge = base.titleLarge.sans(),
    titleMedium = base.titleMedium.sans(),
    titleSmall = base.titleSmall.sans(),
    bodyLarge = base.bodyLarge.sans(),
    bodyMedium = base.bodyMedium.sans(),
    bodySmall = base.bodySmall.sans(),
    labelLarge = base.labelLarge.sans(),
    labelMedium = base.labelMedium.sans(),
    labelSmall = base.labelSmall.sans(),
)

private val scheme = darkColorScheme(
    primary = C.Red,
    onPrimary = Color.White,
    primaryContainer = C.RedTint,
    onPrimaryContainer = C.RedLight,
    secondary = C.Ovul,
    onSecondary = Color.White,
    background = C.Bg,
    onBackground = C.Text,
    surface = C.Surface,
    onSurface = C.Text,
    surfaceVariant = C.Surface,
    onSurfaceVariant = C.Muted,
    surfaceContainer = C.Surface,
    surfaceContainerLow = C.Surface,
    surfaceContainerHigh = C.Surface,
    surfaceContainerHighest = C.Border,
    outline = C.BorderStrong,
    outlineVariant = C.Border,
    error = C.RedText,
)

@Composable
fun CicloTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
