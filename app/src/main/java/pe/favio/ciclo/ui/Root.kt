package pe.favio.ciclo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import pe.favio.ciclo.R
import pe.favio.ciclo.data.AppData
import pe.favio.ciclo.data.Backup
import pe.favio.ciclo.data.BackupCodec
import pe.favio.ciclo.data.DayLog
import pe.favio.ciclo.data.Period
import pe.favio.ciclo.data.Settings
import pe.favio.ciclo.data.Store
import pe.favio.ciclo.domain.CyclePredictor
import pe.favio.ciclo.domain.Insights
import pe.favio.ciclo.domain.Phase
import pe.favio.ciclo.domain.PeriodAction
import pe.favio.ciclo.domain.PeriodOps
import kotlinx.coroutines.launch
import pe.favio.ciclo.ui.theme.C
import java.time.LocalDate

enum class Tab(val label: String, val icon: Int) {
    HOY("Hoy", R.drawable.ic_tab_hoy),
    CALENDARIO("Calendario", R.drawable.ic_tab_calendario),
    APRENDE("Aprende", R.drawable.ic_tab_aprende),
    AJUSTES("Ajustes", R.drawable.ic_tab_ajustes),
}

/** Every write to the store goes through here. */
class Actions(private val store: Store) {
    private var undoPeriods: List<Period>? = null

    private fun avgLen(d: AppData) =
        CyclePredictor.stats(d.bleeds, d.settings.defaultCycle, d.settings.defaultPeriod).periodLength

    /** Returns true when the periods changed (and can be undone). */
    fun period(action: PeriodAction, date: LocalDate, today: LocalDate): Boolean {
        val before = store.data.value.periods
        store.update { it.withBleeds(PeriodOps.apply(it.bleeds, action, date, today, avgLen(it))) }
        val changed = store.data.value.periods != before
        if (changed) undoPeriods = before
        return changed
    }

    fun undo() {
        val previous = undoPeriods ?: return
        store.update { it.copy(periods = previous) }
        undoPeriods = null
    }

    fun saveLog(date: LocalDate, log: DayLog) = store.update { it.withLog(date, log) }

    fun settings(change: (Settings) -> Settings) = store.update { it.copy(settings = change(it.settings)) }

    fun finishSetup(result: SetupResult, today: LocalDate) {
        store.update { it.copy(settings = result.settings) }
        result.lastStart?.let { period(PeriodAction.START_NEW, it, today) }
        undoPeriods = null
    }

    fun restore(b: Backup) = store.update { BackupCodec.restore(it, b) }

    fun copyDates(): List<LocalDate> = store.copyDates()

    fun restoreCopy(date: LocalDate): Boolean = store.restoreCopy(date)

    fun wipe() = store.update { AppData() }
}

@Composable
fun CicloRoot(store: Store, data: AppData) {
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleResumeEffect(Unit) {
        today = LocalDate.now()
        onPauseOrDispose { }
    }
    val p = remember(data, today) {
        CyclePredictor.predict(data.bleeds, data.settings.defaultCycle, data.settings.defaultPeriod, today)
    }
    val actions = remember(store) { Actions(store) }

    var tab by rememberSaveable { mutableStateOf(Tab.HOY) }
    var logDate by rememberSaveable { mutableStateOf<String?>(null) }
    var learnPhase by rememberSaveable { mutableStateOf<Phase?>(null) }
    val currentPhase = p.info(today).phase

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showGuide by rememberSaveable { mutableStateOf(false) }
    var showStats by rememberSaveable { mutableStateOf(false) }
    val flags = remember(data, today) {
        Insights.figoFlags(Insights.history(data.bleeds, data.entries, today), p.isOpen, today)
    }

    fun doPeriod(action: PeriodAction, date: LocalDate) {
        if (!actions.period(action, date, today)) return
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(action.doneMessage(), actionLabel = "Deshacer", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) actions.undo()
        }
    }

    var loadProblem by remember { mutableStateOf(store.loadProblem) }
    loadProblem?.let { msg ->
        AlertDialog(
            onDismissRequest = {},
            containerColor = C.Surface,
            title = { Text("Recuperamos tus datos") },
            text = { Text(msg) },
            confirmButton = {
                TextButton({ store.clearLoadProblem(); loadProblem = null }) { Text("Entendido", color = C.RedText) }
            },
        )
    }

    if (!data.settings.onboarded || showGuide) {
        OnboardingScreen(data.settings, p.hasData, today) { result ->
            actions.finishSetup(result, today)
            showGuide = false
            tab = Tab.HOY
        }
        return
    }

    Scaffold(
        containerColor = C.Bg,
        contentColor = C.Text,
        snackbarHost = {
            SnackbarHost(snackbar) { Snackbar(it, containerColor = C.Text, contentColor = C.Bg, actionColor = C.Red) }
        },
        bottomBar = {
            NavigationBar(containerColor = C.Surface) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t; showStats = false },
                        icon = { Icon(painterResource(t.icon), null) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = C.RedText,
                            selectedTextColor = C.RedText,
                            indicatorColor = C.RedTint,
                            unselectedIconColor = C.Muted,
                            unselectedTextColor = C.Muted,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (showStats) {
                EstadisticasScreen(p, data, today, onBack = { showStats = false })
            } else when (tab) {
                Tab.HOY -> HoyScreen(
                    p, data, today,
                    onAction = ::doPeriod,
                    onLog = { logDate = today.toString() },
                    onLearn = { learnPhase = it; tab = Tab.APRENDE },
                    onOpenCalendar = { tab = Tab.CALENDARIO },
                    flags = flags,
                    onOpenStats = { showStats = true },
                )
                Tab.CALENDARIO -> CalendarioScreen(
                    p, data, today,
                    onLog = { logDate = it.toString() },
                    onAction = ::doPeriod,
                    onOpenStats = { showStats = true },
                )
                Tab.APRENDE -> AprendeScreen(
                    selected = learnPhase ?: currentPhase ?: Phase.MENSTRUAL,
                    current = currentPhase,
                    onSelect = { learnPhase = it },
                )
                Tab.AJUSTES -> AjustesScreen(
                    data, p, today,
                    onSettings = actions::settings,
                    onRestore = actions::restore,
                    onWipe = actions::wipe,
                    onShowGuide = { showGuide = true },
                    copyDates = actions::copyDates,
                    onRestoreCopy = actions::restoreCopy,
                )
            }
        }
    }

    logDate?.let { key ->
        val date = LocalDate.parse(key)
        // Logging bleeding on a day outside any period offers to mark it as one.
        val mark = PeriodOps.optionsFor(data.bleeds, date, today).primary
            ?.takeIf { it == PeriodAction.START_NEW || it == PeriodAction.EXTEND_END || it == PeriodAction.MOVE_START_EARLIER }
        RegistroSheet(
            date = date,
            initial = data.log(date),
            markAction = mark,
            onSave = { log, markPeriod ->
                actions.saveLog(date, log)
                if (markPeriod && mark != null) doPeriod(mark, date)
            },
            onDismiss = { logDate = null },
        )
    }
}
