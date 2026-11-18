package pe.favio.ciclo.content

// Shown in Ajustes > Política de privacidad. The same text has to be published at a public URL for
// the Google Play listing; when one changes, change the other and the date.

data class PolicySection(val title: String, val paragraphs: List<String>)

object PrivacyPolicy {
    const val UPDATED = "17 de noviembre de 2026"
    const val ISSUES_URL = "https://github.com/lotuslatte/ciclo-app/issues"

    const val SUMMARY =
        "Ciclo funciona sin internet. Lo que anotas se queda en tu teléfono y nadie más lo recibe: ni quien hizo la app ni terceros."

    val sections: List<PolicySection> = listOf(
        PolicySection(
            "Qué datos guarda Ciclo",
            listOf(
                "Solo lo que tú escribes: las fechas de tus reglas, y por cada día el sangrado, los síntomas, el dolor, el ánimo y las notas que quieras anotar.",
                "También guarda tus ajustes: la duración habitual de tu ciclo y de tu regla, tus recordatorios y, si lo activas, un PIN. El PIN no se guarda tal cual, sino transformado para que no pueda leerse.",
                "Ciclo no te pide nombre, correo, teléfono ni ubicación. No hay cuentas ni registro.",
            ),
        ),
        PolicySection(
            "Dónde se guardan",
            listOf(
                "En el almacenamiento privado de la app dentro de tu teléfono, al que las demás apps no tienen acceso. Ahí mismo la app guarda una copia automática por día de los últimos 7 días, para recuperar tus datos si algo falla.",
                "Tus datos no se incluyen en la copia de seguridad de Android en la nube ni en el traspaso automático a un teléfono nuevo.",
                "Los datos no están cifrados por la app. Dependen de la protección de tu teléfono, así que conviene que tenga bloqueo de pantalla. El PIN de Ciclo impide que alguien abra la app, pero no cifra los datos.",
            ),
        ),
        PolicySection(
            "Qué no hace Ciclo",
            listOf(
                "No tiene permiso de internet, así que no puede enviar nada a ningún servidor.",
                "No usa herramientas de analítica ni de seguimiento, no muestra publicidad y no comparte ni vende datos.",
                "El único permiso que pide es el de notificaciones, para los recordatorios. Los recordatorios se calculan en tu teléfono.",
            ),
        ),
        PolicySection(
            "Cuándo salen datos de tu teléfono",
            listOf(
                "Solo cuando tú lo decides, en estos casos:",
                "Respaldo. Si tocas «Exportar», se crea un archivo con tus reglas y anotaciones en el lugar que elijas. Ese archivo no está cifrado: quien lo abra puede leerlo. Guárdalo en un lugar privado.",
                "Informe para tu médico. Si creas el PDF y lo compartes, llega a la app o a la persona que tú elijas. El PDF tampoco está cifrado.",
                "Comentarios y reportes de error. Si eliges enviarlos, se abre la app que tú escojas (por ejemplo, el correo) con un texto que puedes revisar antes de mandarlo. Incluye la versión de la app y el modelo de tu teléfono. No incluye tus reglas ni tus anotaciones.",
                "Una vez que un archivo o un mensaje sale de Ciclo, queda fuera del control de la app.",
            ),
        ),
        PolicySection(
            "Cómo borrar tus datos",
            listOf(
                "En Ajustes, toca «Borrar todos mis datos». Se eliminan tus reglas, anotaciones, ajustes, PIN, copias automáticas, informes guardados temporalmente y registros de errores. No se puede deshacer.",
                "Desinstalar la app también borra todo lo que está guardado en ella.",
                "Los respaldos y los PDF que hayas exportado no se borran con esto, porque están fuera de la app. Tienes que borrarlos tú.",
            ),
        ),
        PolicySection(
            "Menores de edad",
            listOf(
                "Ciclo no recoge datos de nadie, tampoco de menores. Como no hay cuentas ni envío de datos, quien hizo la app no sabe quién la usa.",
            ),
        ),
        PolicySection(
            "Cambios en esta política",
            listOf(
                "Si la app cambia la forma de tratar tus datos, esta página se actualizará junto con la versión que traiga el cambio, con una fecha nueva.",
            ),
        ),
        PolicySection(
            "Contacto",
            listOf(
                "Para dudas o pedidos sobre privacidad, escribe en la página de «Issues» del proyecto en GitHub. Esa página es pública: no pongas ahí datos personales ni de salud.",
            ),
        ),
    )
}
