package pe.favio.ciclo.content

import pe.favio.ciclo.domain.CycleFlag

fun CycleFlag.text(): String = when (this) {
    CycleFlag.SHORT_CYCLES -> "Varios de tus ciclos duraron menos de 24 días."
    CycleFlag.LONG_CYCLES -> "Varios de tus ciclos duraron más de 38 días."
    CycleFlag.VARIABLE -> "Entre el más corto y el más largo de tus últimos ciclos hay más de 9 días de diferencia."
    CycleFlag.LONG_PERIODS -> "Varias de tus reglas duraron más de 8 días."
    CycleFlag.NO_PERIOD_90 -> "Pasaron más de 90 días sin regla."
}

const val FLAGS_ADVICE =
    "Está fuera de los rangos habituales (FIGO). Podría valer la pena comentarlo con un profesional, sobre todo si es nuevo para ti."

const val FIGO_REFERENCE =
    "Referencia: FIGO 2018 (Munro et al.). Ciclo de 24 a 38 días, sangrado de hasta 8 días, diferencia entre el ciclo más corto y el más largo de hasta 9 días (hasta 7 entre los 26 y 41 años). La app no registra la edad y señala la variación solo cuando supera 9 días."
