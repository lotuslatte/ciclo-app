package pe.favio.ciclo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.favio.ciclo.ui.theme.C
import pe.favio.ciclo.ui.theme.Fraunces
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {
    private val es: Locale = Locale.forLanguageTag("es-PE")
    private val full = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", es)
    private val dayMonth = DateTimeFormatter.ofPattern("d 'de' MMMM", es)
    private val short = DateTimeFormatter.ofPattern("d MMM", es)
    private val month = DateTimeFormatter.ofPattern("MMMM yyyy", es)

    fun full(d: LocalDate): String = d.format(full)
    fun fullCap(d: LocalDate): String = cap(full(d))
    fun dayMonth(d: LocalDate): String = d.format(dayMonth)
    fun short(d: LocalDate): String = d.format(short).replace(".", "")
    fun month(m: YearMonth): String = m.format(month)
    fun cap(s: String): String = s.replaceFirstChar { it.titlecase(es) }
    fun days(n: Int): String = if (n == 1) "1 día" else "$n días"
}

val CardShape = RoundedCornerShape(18.dp)

@Composable
fun CCard(
    modifier: Modifier = Modifier,
    bg: Color = C.Surface,
    border: Color = C.Border,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(bg)
            .border(1.dp, border, CardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun Kicker(text: String, color: Color = C.Muted, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, color = C.Text)
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = C.Red, contentColor = Color.White),
    ) { Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 52.dp) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = height),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, C.BorderStrong),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = C.Surface, contentColor = C.Text),
    ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium) }
}

/** Small blood drop used for flow levels. */
@Composable
fun Drop(color: Color, size: Dp = 12.dp) {
    Canvas(Modifier.size(size * 0.8f, size)) {
        val w = this.size.width
        val h = this.size.height
        val p = Path().apply {
            moveTo(w / 2, 0f)
            cubicTo(w * 0.9f, h * 0.35f, w, h * 0.55f, w, h * 0.66f)
            cubicTo(w, h * 0.9f, w * 0.75f, h, w / 2, h)
            cubicTo(w * 0.25f, h, 0f, h * 0.9f, 0f, h * 0.66f)
            cubicTo(0f, h * 0.55f, w * 0.1f, h * 0.35f, w / 2, 0f)
            close()
        }
        drawPath(p, color)
    }
}

/** Line-drawn face; [mood] 0 (sad) … 4 (happy) bends the mouth. */
@Composable
fun MoodFace(mood: Int, color: Color, size: Dp = 26.dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = s * 0.075f
        drawCircle(color, radius = s * 0.42f, style = Stroke(stroke))
        drawCircle(color, radius = stroke * 0.8f, center = Offset(s * 0.37f, s * 0.42f))
        drawCircle(color, radius = stroke * 0.8f, center = Offset(s * 0.63f, s * 0.42f))
        val y = s * 0.66f
        val bend = (mood - 2) * s * 0.07f
        val mouth = Path().apply {
            moveTo(s * 0.34f, y - bend / 2)
            quadraticTo(s * 0.5f, y + bend * 1.5f, s * 0.66f, y - bend / 2)
        }
        drawPath(mouth, color, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
fun SourceLine(text: String) {
    Text(text, color = C.Muted, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
}
