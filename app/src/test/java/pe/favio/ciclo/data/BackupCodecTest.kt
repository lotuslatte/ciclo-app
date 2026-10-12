package pe.favio.ciclo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BackupCodecTest {
    private val data = AppData(
        periods = listOf(Period("2026-01-01", "2026-01-05"), Period("2026-01-29", null)),
        logs = mapOf("2026-01-02" to DayLog(flow = 2, symptoms = listOf("Cólicos"), mood = 1, note = "Día pesado \"ñ\"")),
        settings = Settings(defaultCycle = 30, defaultPeriod = 4, pinHash = "secret", pinSalt = "salt"),
    )

    @Test
    fun roundTrip_restoresRecordsButNeverThePin() {
        val text = BackupCodec.encode(data, "2026-02-01T10:00:00")
        assertFalse(text.contains("secret"))

        val restored = BackupCodec.restore(AppData(), BackupCodec.decode(text))
        assertEquals(data.periods, restored.periods)
        assertEquals(data.logs, restored.logs)
        assertEquals(30, restored.settings.defaultCycle)
        assertEquals(4, restored.settings.defaultPeriod)
        assertEquals(null, restored.settings.pinHash)
    }

    @Test
    fun restore_keepsThisPhonesPin() {
        val phone = AppData(settings = Settings(pinHash = "h", pinSalt = "s"))
        val restored = BackupCodec.restore(phone, BackupCodec.decode(BackupCodec.encode(data, "x")))
        assertEquals("h", restored.settings.pinHash)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOtherJson() {
        BackupCodec.decode("""{"hello": "world"}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBadDates() {
        BackupCodec.decode(
            """{"format":"ciclo-respaldo","exportedAt":"x","periods":[{"start":"ayer"}],"logs":{},"defaultCycle":28,"defaultPeriod":5}""",
        )
    }
}
