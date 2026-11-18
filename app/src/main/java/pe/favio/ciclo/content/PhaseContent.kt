package pe.favio.ciclo.content

import pe.favio.ciclo.domain.Phase

// Every claim below was checked against its primary source (Sept 2026). Keep the wording as cautious as the evidence.
// Nov 2026 review: removed an ACOG reaffirmation year that could not be confirmed, stopped attributing a
// "12 to 14 days" luteal figure to Wilcox 2000 (the paper is about how variable the timing is), and added
// cautions to the anti-inflammatory and calcium tips.
// Second Nov 2026 review:
// - Fertile window: now the six days ending on the estimated ovulation day (Wilcox, Weinberg and Baird 1995,
//   abstract read; restated in Wilcox, Dunson and Baird 2000, full text read). It used to run one day longer.
// - Irregularity: FIGO's limit is 9 days at 18-25 and 42-45 and 7 days at 26-41 (read in Jain, Munro and
//   Critchley 2023 and Munro et al. 2022, both open access; the 2018 paper itself is paywalled). The app does
//   not know the age, so it flags above 9 and the text gives the 7-day limit for ages 26-41.
// - Follicular phase: the "more energy and better mood" sentence had no source and the one prospective daily
//   study opened (Doornweerd and Gerritsen 2025, 22 naturally cycling women) found no significant phase
//   difference in energy or happiness, so the sentence was removed.

data class Tip(val title: String, val body: String, val source: String)

data class PhaseInfo(
    val phase: Phase,
    val name: String,
    /** One-line summary for the card on "Hoy". */
    val short: String,
    val what: String,
    val tips: List<Tip>,
)

object PhaseContent {
    val phases: List<PhaseInfo> = listOf(
        PhaseInfo(
            phase = Phase.MENSTRUAL,
            name = "Menstrual",
            short = "Bajan las hormonas y el útero se desprende de su revestimiento. El calor y el movimiento suave pueden aliviar los cólicos.",
            what = "Bajan el estrógeno y la progesterona, y el revestimiento del útero se desprende. Los cólicos se deben a las prostaglandinas, que hacen que el útero se contraiga.",
            tips = listOf(
                Tip(
                    "Calor en el abdomen bajo",
                    "Una bolsa de agua tibia o un parche térmico podría aliviar el dolor. Los estudios son pocos y pequeños, pero apuntan en esa dirección.",
                    "Jo y Lee, 2018 · Scientific Reports",
                ),
                Tip(
                    "Antiinflamatorios al empezar el dolor",
                    "Ibuprofeno o naproxeno alivian los cólicos mejor que un placebo, aunque pueden causar molestias de estómago. Evítalos si tienes úlcera, asma por aspirina o estás embarazada. Consulta antes con un profesional si tomas otros medicamentos, tienes enfermedad renal, usas anticoagulantes o eres alérgica a estos medicamentos.",
                    "Marjoribanks et al., 2015 · Cochrane",
                ),
                Tip(
                    "Moverte con regularidad",
                    "Hacer ejercicio de 45 a 60 minutos, 3 veces por semana o más, podría reducir la intensidad de los cólicos.",
                    "Armour et al., 2019 · Cochrane",
                ),
                Tip(
                    "Si tu regla es abundante",
                    "Las reglas abundantes pueden causar falta de hierro o anemia. Si es tu caso, pregunta en tu centro de salud por un análisis de sangre.",
                    "NICE NG88, 2018 (actualizada 2026)",
                ),
            ),
        ),
        PhaseInfo(
            phase = Phase.FOLICULAR,
            name = "Folicular",
            short = "El estrógeno sube y el útero reconstruye su revestimiento. Entrena como te sientas.",
            what = "El estrógeno sube y el útero reconstruye su revestimiento. Cómo te sientes en estos días cambia de una persona a otra.",
            tips = listOf(
                Tip(
                    "Entrena como te sientas",
                    "El rendimiento físico cambia muy poco entre fases (quizás un poco menos durante la regla). No hace falta planificar el ejercicio según el ciclo: guíate por cómo te sientes.",
                    "McNulty et al., 2020 · Sports Medicine",
                ),
            ),
        ),
        PhaseInfo(
            phase = Phase.OVULATORIA,
            name = "Ovulatoria",
            short = "Se libera un óvulo. La fecha es una estimación: no uses la app como anticonceptivo.",
            what = "Un pico de la hormona LH libera un óvulo. Algunas personas sienten un dolor leve a un lado del vientre o notan un flujo más claro y elástico.",
            tips = listOf(
                Tip(
                    "Es una estimación",
                    "El día de la ovulación cambia de una persona a otra y de un ciclo a otro, incluso en ciclos regulares. La app lo calcula contando 14 días hacia atrás desde la próxima regla prevista, así que sus fechas son aproximadas. En un estudio, solo cerca del 30 % de las mujeres tuvo todos sus días fértiles entre los días 10 y 17 del ciclo.",
                    "Wilcox et al., 2000 · BMJ",
                ),
                Tip(
                    "Cuáles son los días fértiles",
                    "En un estudio con 221 mujeres que buscaban un embarazo, casi todos los embarazos se debieron a relaciones en un periodo de seis días que termina el día de la ovulación. La app marca esos seis días a partir de una ovulación que solo estima, así que en tu ciclo real pueden caer varios días antes o después.",
                    "Wilcox et al., 1995 · N Engl J Med",
                ),
                Tip(
                    "No es un anticonceptivo",
                    "Predecir los días fértiles solo con el calendario no protege de un embarazo. Incluso el método formal de días fijos falla en unas 12 de cada 100 mujeres al año con el uso habitual. Pide orientación en un centro de salud.",
                    "OMS/JHU, Planificación familiar: manual mundial, 2022",
                ),
            ),
        ),
        PhaseInfo(
            phase = Phase.LUTEA,
            name = "Lútea",
            short = "Sube la progesterona. Al final pueden aparecer síntomas premenstruales; el calcio y el ejercicio pueden ayudar.",
            what = "Sube la progesterona. En los últimos días pueden aparecer síntomas premenstruales: hinchazón, senos sensibles, irritabilidad o antojos.",
            tips = listOf(
                Tip(
                    "Calcio",
                    "En un ensayo con 466 mujeres, 1200 mg diarios de calcio redujeron los síntomas premenstruales un 48 %, frente a un 30 % con placebo, al tercer ciclo. Es un solo estudio. Antes de tomar un suplemento, coméntalo con un profesional de salud.",
                    "Thys-Jacobs et al., 1998 · Am J Obstet Gynecol",
                ),
                Tip(
                    "Ejercicio regular",
                    "Hacer ejercicio con regularidad podría reducir los síntomas premenstruales, aunque los estudios tienen limitaciones.",
                    "Pearce et al., 2020 · BJGP Open",
                ),
                Tip(
                    "Si el SPM afecta tu vida diaria",
                    "Puede tratarse de un trastorno disfórico premenstrual (TDPM). Existen tratamientos eficaces, como algunos antidepresivos (ISRS), que requieren receta. Coméntalo con un profesional.",
                    "Jespersen et al., 2024 · Cochrane",
                ),
            ),
        ),
    )

    fun of(phase: Phase): PhaseInfo = phases.first { it.phase == phase }

    val redFlags: List<String> = listOf(
        "Tus ciclos duran menos de 24 días o más de 38.",
        "Entre tu ciclo más corto y tu ciclo más largo hay más de 9 días de diferencia (más de 7 si tienes entre 26 y 41 años).",
        "Pasan más de 90 días sin regla y no estás embarazada.",
        "La regla dura más de 8 días.",
        "Empapas una toalla o tampón cada 1 o 2 horas, o el sangrado afecta tu vida diaria.",
        "Sangras entre reglas o después de tener relaciones, o tienes dolor o presión en la pelvis.",
        "El dolor no mejora con analgésicos o te impide hacer tu vida.",
    )

    const val redFlagsNote = "En adolescentes, ciclos de 21 a 45 días pueden ser normales."

    val redFlagSources = listOf(
        "Munro et al., 2018 · FIGO, Int J Gynecol Obstet",
        "Jain, Munro y Critchley, 2023 · FIGO, Int J Gynecol Obstet",
        "NICE NG88, 2018 (actualizada 2026)",
        "ACOG, Opinión del Comité 651, 2015",
    )
}
