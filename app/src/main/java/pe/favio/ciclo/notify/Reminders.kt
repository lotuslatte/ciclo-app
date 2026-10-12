package pe.favio.ciclo.notify

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import pe.favio.ciclo.MainActivity
import pe.favio.ciclo.R
import pe.favio.ciclo.data.AppData
import pe.favio.ciclo.data.Store
import pe.favio.ciclo.domain.CyclePredictor
import pe.favio.ciclo.domain.DayKind
import pe.favio.ciclo.ui.Fmt
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object Reminders {
    private const val CHANNEL = "recordatorios"
    private const val WORK = "recordatorio-diario"
    private const val PREFS = "recordatorios"
    private const val ID_PERIOD = 1
    private const val ID_FERTILE = 2

    fun createChannel(ctx: Context) {
        val ch = NotificationChannel(CHANNEL, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT)
        ch.description = "Avisos antes de la regla y de los días fértiles"
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    /** One check a day, around 9:00. WorkManager keeps it across reboots. */
    fun schedule(ctx: Context) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(9, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun canNotify(ctx: Context): Boolean = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    fun check(ctx: Context, data: AppData, today: LocalDate = LocalDate.now()) {
        val s = data.settings
        val p = CyclePredictor.predict(data.bleeds, s.defaultCycle, s.defaultPeriod, today)
        if (!p.hasData) return
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val next = p.nextStart
        val until = p.daysUntilNext
        if (s.remindPeriod && next != null && until != null && !p.isOpen && until in 0..s.remindDaysBefore &&
            prefs.getString("period", null) != next.toString()
        ) {
            val text = if (until == 0) "Tu regla podría llegar hoy." else
                "Tu regla podría llegar en $until ${if (until == 1) "día" else "días"} (${Fmt.dayMonth(next)})."
            show(ctx, ID_PERIOD, "Se acerca tu regla", text)
            prefs.edit().putString("period", next.toString()).apply()
        }

        val info = p.info(today)
        val fertile = info.fertile
        if (s.remindFertile && fertile != null && (info.kind == DayKind.FERTILE || info.kind == DayKind.OVULATION) &&
            prefs.getString("fertile", null) != fertile.start.toString()
        ) {
            show(ctx, ID_FERTILE, "Días fértiles", "Según tu calendario, hasta el ${Fmt.dayMonth(fertile.end)} son tus días fértiles (estimado).")
            prefs.edit().putString("fertile", fertile.start.toString()).apply()
        }
    }

    @SuppressLint("MissingPermission")
    fun show(ctx: Context, id: Int, title: String, text: String) {
        if (!canNotify(ctx)) return
        val intent = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFC8374F.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}

class ReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        Reminders.check(applicationContext, Store.get(applicationContext).data.value)
        return Result.success()
    }
}
