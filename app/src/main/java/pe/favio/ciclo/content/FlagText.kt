package pe.favio.ciclo.content

import pe.favio.ciclo.domain.CycleFlag

fun CycleFlag.text(): String = when (this) {
    CycleFlag.SHORT_CYCLES -> "Varios de tus ciclos duraron menos de 24 días."
    CycleFlag.LONG_CYCLES -> "Varios de tus ciclos duraron más de 38 días."
    CycleFlag.VARIABLE -> "La duración de tus ciclos cambia más de 9 días de uno a otro."
    CycleFlag.LONG_PERIODS -> "Varias de tus reglas duraron más de 8 días."
    CycleFlag.NO_PERIOD_90 -> "Pasaron más de 90 días sin regla."
}

const val FLAGS_ADVICE =
    "Está fuera de los rangos habituales (FIGO). Podría valer la pena comentarlo con un profesional, sobre todo si es nuevo para ti."

const val FIGO_REFERENCE =
    "Referencia: FIGO 2018 (Munro et al.). Ciclo de 24 a 38 días, sangrado de hasta 8 días, variación de hasta 7–9 días entre ciclos."
