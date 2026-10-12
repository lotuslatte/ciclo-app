package pe.favio.ciclo

import android.app.Application
import pe.favio.ciclo.notify.Reminders

class CicloApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BetaSupport.installCrashLog(this)
        Reminders.createChannel(this)
        Reminders.schedule(this)
    }
}
