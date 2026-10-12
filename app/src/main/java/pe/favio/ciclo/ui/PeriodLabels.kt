package pe.favio.ciclo.ui

import pe.favio.ciclo.domain.DayOptions
import pe.favio.ciclo.domain.PeriodAction
import java.time.LocalDate

/** Button text for a period action, worded for the day it applies to. */
fun PeriodAction.label(day: LocalDate, today: LocalDate, options: DayOptions): String {
    val isToday = day == today
    return when (this) {
        PeriodAction.START_NEW -> if (isToday) "Me llegó la regla" else "Empezó mi regla este día"
        PeriodAction.END_HERE -> when {
            options.isFirstDay && !isToday -> "Solo duró este día"
            isToday -> "Terminó mi regla"
            else -> "Terminó este día"
        }
        PeriodAction.EXTEND_END -> if (isToday) "Sigo con la regla hoy" else "Mi regla siguió hasta este día"
        PeriodAction.MOVE_START_EARLIER -> "Mi regla empezó este día"
        PeriodAction.MOVE_START_LATER -> "En realidad empezó este día"
        PeriodAction.EXCLUDE -> "No contar en las predicciones (manchado u otra causa)"
        PeriodAction.INCLUDE -> "Volver a contar en las predicciones"
        PeriodAction.REMOVE -> "Quitar esta regla"
    }
}

/** Confirmation shown in the undo snackbar. */
fun PeriodAction.doneMessage(): String = when (this) {
    PeriodAction.START_NEW -> "Regla marcada"
    PeriodAction.REMOVE -> "Regla eliminada"
    PeriodAction.EXCLUDE -> "Ya no cuenta en las predicciones"
    PeriodAction.INCLUDE -> "Vuelve a contar en las predicciones"
    else -> "Regla actualizada"
}

/** One-line explanation under the day's title, when it helps. */
fun DayOptions.hint(): String? = when {
    isFuture -> "Podrás marcar tu regla cuando llegue."
    isExcluded -> "Este sangrado no cuenta en las predicciones."
    isFirstDay -> "Primer día de tu regla."
    isLastDay -> "Último día de tu regla."
    primary == PeriodAction.EXTEND_END -> "Está justo después de una regla: puedes alargarla hasta aquí."
    primary == PeriodAction.MOVE_START_EARLIER -> "Está justo antes de una regla: puedes adelantar su inicio."
    else -> null
}
