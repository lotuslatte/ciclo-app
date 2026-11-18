package pe.favio.ciclo

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.favio.ciclo.data.Store
import pe.favio.ciclo.security.Pin
import pe.favio.ciclo.ui.CicloRoot
import pe.favio.ciclo.ui.LockScreen
import pe.favio.ciclo.ui.theme.CicloTheme

/** Lets the file picker open without re-locking the app when it comes back. */
object LockGuard {
    @Volatile
    private var skip = false

    /**
     * True while the PIN has been entered and the app has not been left since. Kept in memory
     * only: it survives a rotation, and a new process always starts locked.
     */
    @Volatile
    var unlocked = false

    fun skipNext() {
        skip = true
    }

    fun consume(): Boolean = skip.also { skip = false }
}

class MainActivity : ComponentActivity() {
    private var locked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = Store.get(this)
        val pinSet = store.data.value.settings.pinEnabled
        locked = pinSet && !LockGuard.unlocked
        if (!locked) LockGuard.unlocked = true
        // Set before the first frame so the recents screen never captures cycle data.
        if (pinSet) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        setContent {
            CicloTheme {
                val data by store.data.collectAsStateWithLifecycle()
                val pinOn = data.settings.pinEnabled
                // With a PIN, also hide the app's content from screenshots and the recents screen.
                LaunchedEffect(pinOn) {
                    if (pinOn) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                if (locked && pinOn) {
                    LockScreen(
                        check = { Pin.verify(data.settings, it) },
                        onUnlock = { locked = false; LockGuard.unlocked = true },
                    )
                } else {
                    CicloRoot(store, data)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (LockGuard.consume() || isChangingConfigurations) return
        LockGuard.unlocked = false
        if (Store.get(this).data.value.settings.pinEnabled) locked = true
    }
}
