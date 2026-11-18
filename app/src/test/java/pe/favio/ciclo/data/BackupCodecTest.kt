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

    @Test
    fun restore_bringsOutOfRangeValuesIntoRange() {
        val b = BackupCodec.decode(
            """{"format":"ciclo-respaldo","exportedAt":"x",
               "periods":[{"start":"2026-01-05","end":"2026-01-01"}],
               "logs":{"2026-01-05":{"flow":7,"mood":12,"pain":-1}},
               "defaultCycle":5,"defaultPeriod":99}""",
        )
        val restored = BackupCodec.restore(AppData(), b)
        assertEquals(listOf(Period("2026-01-05", "2026-01-05")), restored.periods)
        assertEquals(DayLog(flow = 3, mood = 4, pain = 0), restored.logs["2026-01-05"])
        assertEquals(21, restored.settings.defaultCycle)
        assertEquals(10, restored.settings.defaultPeriod)
    }

    @Test
    fun readText_readsAStreamWithinTheLimit() {
        val text = BackupCodec.encode(data, "x")
        assertEquals(text, BackupCodec.readText(text.byteInputStream()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun readText_refusesOversizedFiles() {
        BackupCodec.readText(ByteArray(5000).inputStream(), maxBytes = 4096)
    }
}
