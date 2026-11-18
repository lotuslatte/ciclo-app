# Publicar Ciclo en Google Play: paso a paso

Estado al 17 de noviembre de 2026. Los textos de la ficha están en `listing_es.md`; los gráficos, en esta misma carpeta (`icon-512.png`, `feature-1024x500.png`).

## 1. Crear la cuenta

1. Entra a play.google.com/console con la cuenta de Google que quieras usar para siempre con la app. Conviene una cuenta que no sea la del trabajo.
2. Elige cuenta personal. Paga los 25 dólares (pago único) y completa la verificación de identidad con tu DNI. La verificación puede tardar varios días.
3. Google también pide verificar un teléfono Android real con la app Play Console. Hazlo cuando lo pida.
4. El nombre de desarrollador que pongas se ve en la tienda. Tu dirección no se muestra en cuentas personales si la app no cobra.

## 2. Crear la app

1. «Crear app». Nombre: Ciclo: calendario menstrual. Idioma predeterminado: español (Latinoamérica). Tipo: app. Gratis.
2. En «Integridad de la app», acepta Play App Signing. Google genera y guarda la clave con la que firma lo que reciben las personas; la clave que ya usa el CI queda como clave de subida. Así, si un día se pierde, Google puede reemplazarla.

## 3. Contenido de la app (menú Política)

- Política de privacidad: https://lotuslatte.github.io/ciclo-app/privacidad/
- Acceso a la app: todas las funciones están disponibles sin credenciales.
- Anuncios: no contiene anuncios.
- Clasificación de contenido: categoría «Referencia, noticias o educativa» o «Utilidad»; responde «no» a violencia, sexo, lenguaje, drogas, apuestas e interacción entre usuarios. El resultado esperado es apta para todo público.
- Público objetivo: 13 años a más. No marcar menores de 13: eso activa requisitos de la política de familias.
- Seguridad de los datos: «¿Tu app recopila o comparte alguno de los tipos de datos de usuario obligatorios?» No. Lo que la persona anota se procesa solo en el dispositivo y nunca se transmite, y Google no cuenta eso como recopilación.
- Apps de salud: marca «Seguimiento del periodo» (ciclo menstrual, ovulación y fertilidad). No es dispositivo médico. No usa Health Connect.
- Apps gubernamentales, financieras, de noticias: no.
- ID de publicidad: la app no lo usa.

## 4. Ficha de la tienda

Copia de `listing_es.md` el nombre, la descripción breve y la completa. Sube `icon-512.png`, `feature-1024x500.png` y al menos 4 capturas de teléfono (mejor 6): Hoy, Calendario, registro del día, Aprende, Ajustes con la sección de privacidad, y la política. Categoría: Salud y bienestar. Correo de contacto: uno que no te importe que sea público.

## 5. Prueba interna (el mismo día)

1. Descarga `ciclo-aab` desde la última corrida de GitHub Actions en `main` y descomprime el `.aab`.
2. Pruebas, «Prueba interna», crea una versión y sube el `.aab`. Agrega tu correo como tester e instálala desde el enlace. Sirve para ver que la subida y la firma funcionan.

## 6. Prueba cerrada: 12 personas, 14 días

Las cuentas personales nuevas no pueden publicar en producción hasta cumplir esto.

1. Pruebas, «Prueba cerrada», crea una pista y sube el mismo `.aab`.
2. Crea una lista de correos de testers (deben ser cuentas de Google). Apunta a 15 o 20 personas: se necesitan 12 que sigan inscritas 14 días seguidos, y siempre alguna se sale.
3. Comparte el enlace de inscripción. Cada persona debe aceptar la invitación e instalar la app desde Play.
4. Pide que la usen de verdad esos 14 días y que te cuenten qué falló. Google puede preguntar qué aprendiste de la prueba.
5. Si alguien se sale, el conteo de 14 días de esa persona se pierde; por eso conviene el margen.

Mensaje sugerido para invitar:

> Hola. Hice una app para llevar el ciclo menstrual que funciona sin internet: lo que anotas no sale de tu teléfono. Para publicarla en Google Play necesito 12 personas con Android que la instalen y la tengan 14 días. ¿Me ayudas? Solo necesito tu correo de Gmail para invitarte. No veo nada de lo que anotes.

## 7. Pedir producción

Cuando la consola muestre que se cumplieron los 12 testers y los 14 días, aparece «Solicitar acceso a producción». El formulario pregunta cómo reclutaste a los testers, qué comentarios recibiste y qué cambiaste. Responde con hechos concretos de la prueba. La revisión suele tardar hasta una semana.

## 8. Publicar

Producción, crea la versión con el `.aab` más reciente, elige países (Perú primero, o todos) y envía a revisión.

## Después de publicar

- Cada actualización necesita un `versionCode` mayor; el CI ya lo sube solo en cada corrida.
- Google exige cada año subir la API objetivo. Hoy la app apunta a API 36, que es lo que se pide desde el 31 de agosto de 2026.
- Si cambia la política de privacidad en la app, hay que regenerar la página pública el mismo día.
