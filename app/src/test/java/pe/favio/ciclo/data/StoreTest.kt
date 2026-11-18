package pe.favio.ciclo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

class StoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private var now = LocalDateTime.of(2026, 3, 1, 10, 0)
    private fun store(dir: File) = Store(dir) { now }
    private fun withPeriod(start: String) = { d: AppData -> d.copy(periods = d.periods + Period(start)) }

    @Test
    fun savesAndReloads() {
        val dir = tmp.newFolder()
        store(dir).update(withPeriod("2026-02-01"))
        val again = store(dir)
        assertEquals(listOf(Period("2026-02-01")), again.data.value.periods)
        assertNull(again.loadProblem)
    }

    @Test
    fun damagedFile_isSetAsideAndNewestCopyIsRecovered() {
        val dir = tmp.newFolder()
        val s = store(dir)
        s.update(withPeriod("2026-01-01")) // first write, no previous file to copy
        now = now.plusDays(1)
        s.update(withPeriod("2026-01-29")) // copies the 1-period state as today's copy

        File(dir, "ciclo.json").writeText("{ esto no es json")
        val recovered = store(dir)

        assertNotNull(recovered.loadProblem)
        assertEquals(listOf(Period("2026-01-01")), recovered.data.value.periods)
        assertTrue(dir.listFiles()!!.any { it.name.startsWith("ciclo.json.danado-") })
    }

    @Test
    fun damagedFile_withoutCopies_startsEmptyButKeepsTheFile() {
        val dir = tmp.newFolder()
        File(dir, "ciclo.json").writeText("dañado")
        val s = store(dir)
        assertNotNull(s.loadProblem)
        assertEquals(AppData(), s.data.value)
        assertTrue(dir.listFiles()!!.any { it.name.startsWith("ciclo.json.danado-") })
    }

    @Test
    fun oneCopyPerDay_keepsOnlyTheLastSeven() {
        val dir = tmp.newFolder()
        val s = store(dir)
        s.update(withPeriod("2025-01-01"))
        repeat(10) { i ->
            now = now.plusDays(1)
            s.update(withPeriod("2025-02-%02d".format(i + 1)))
            s.update(withPeriod("2025-03-%02d".format(i + 1))) // same day: no second copy
        }
        val dates = s.copyDates()
        assertEquals(Store.KEEP_COPIES, dates.size)
        assertEquals(now.toLocalDate(), dates.first())
    }

    @Test
    fun restoreCopy_bringsBackThatDaysState() {
        val dir = tmp.newFolder()
        val s = store(dir)
        s.update(withPeriod("2026-01-01"))
        now = now.plusDays(1)
        s.update { AppData() } // e.g. an accidental wipe; the day's copy keeps the old state
        assertEquals(0, s.data.value.periods.size)

        assertTrue(s.restoreCopy(LocalDate.of(2026, 3, 2)))
        assertEquals(listOf(Period("2026-01-01")), s.data.value.periods)
    }

    @Test
    fun restoreCopy_keepsTheCurrentPin() {
        val dir = tmp.newFolder()
        val s = store(dir)
        s.update { it.copy(periods = listOf(Period("2026-01-01")), settings = it.settings.copy(pinHash = "old", pinSalt = "old", onboarded = true)) }
        now = now.plusDays(1)
        s.update { it.copy(periods = emptyList(), settings = it.settings.copy(pinHash = "new", pinSalt = "new")) }

        assertTrue(s.restoreCopy(LocalDate.of(2026, 3, 2)))
        assertEquals(listOf(Period("2026-01-01")), s.data.value.periods)
        assertEquals("new", s.data.value.settings.pinHash)
        assertEquals("new", s.data.value.settings.pinSalt)
        assertTrue(s.data.value.settings.onboarded)
    }

    @Test
    fun wipe_leavesNothingOnDisk() {
        val dir = tmp.newFolder()
        val s = store(dir)
        s.update(withPeriod("2026-01-01"))
        now = now.plusDays(1)
        s.update(withPeriod("2026-01-29")) // creates an automatic copy
        File(dir, "ciclo.json.danado-20260101-000000").writeText("x")
        File(dir, "otro.txt").writeText("not ours")
        assertTrue(s.copyDates().isNotEmpty())

        s.wipe()

        assertEquals(AppData(), s.data.value)
        assertTrue(s.copyDates().isEmpty())
        assertEquals(listOf("otro.txt"), dir.walkTopDown().filter { it.isFile }.map { it.name }.toList())
        assertEquals(AppData(), store(dir).data.value)
        assertNull(store(dir).loadProblem)
    }

    @Test
    fun wipe_thenNewData_doesNotBringBackOldCopies() {
        val dir = tmp.newFolder()
        val s = store(dir)
        s.update(withPeriod("2026-01-01"))
        s.wipe()
        s.update(withPeriod("2026-02-01"))
        assertTrue(s.copyDates().isEmpty()) // nothing existed before this first write
        assertEquals(listOf(Period("2026-02-01")), store(dir).data.value.periods)
    }

    @Test
    fun fileWithUnreadableDate_isTreatedAsDamaged() {
        val dir = tmp.newFolder()
        File(dir, "ciclo.json").writeText("""{"periods":[{"start":"2026-02-30"}]}""")
        val s = store(dir)
        assertNotNull(s.loadProblem)
        assertEquals(AppData(), s.data.value)
        assertTrue(s.data.value.bleeds.isEmpty()) // must not throw
    }

    @Test
    fun outOfRangeValuesInTheFile_areBroughtIntoRange() {
        val dir = tmp.newFolder()
        File(dir, "ciclo.json").writeText(
            """{"periods":[{"start":"2026-02-10","end":"2026-02-01"}],
               "logs":{"2026-02-10":{"flow":9,"mood":-3,"pain":40}},
               "settings":{"defaultCycle":400,"defaultPeriod":0,"remindDaysBefore":99}}""",
        )
        val d = store(dir).data.value
        assertEquals(listOf(Period("2026-02-10", "2026-02-10")), d.periods)
        assertEquals(DayLog(flow = 3, mood = 0, pain = 10), d.logs["2026-02-10"])
        assertEquals(45, d.settings.defaultCycle)
        assertEquals(2, d.settings.defaultPeriod)
        assertEquals(3, d.settings.remindDaysBefore)
    }
}
