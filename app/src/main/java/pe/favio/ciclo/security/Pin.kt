package pe.favio.ciclo.security

import pe.favio.ciclo.data.Settings
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Pin {
    const val LENGTH = 4
    private const val ITERATIONS = 60_000

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    /** Returns settings with a freshly salted hash of [pin]. */
    fun set(settings: Settings, pin: String): Settings {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val enc = Base64.getEncoder()
        return settings.copy(pinHash = enc.encodeToString(derive(pin, salt)), pinSalt = enc.encodeToString(salt))
    }

    fun verify(settings: Settings, pin: String): Boolean {
        val hash = settings.pinHash ?: return true
        val salt = settings.pinSalt ?: return true
        val dec = Base64.getDecoder()
        return MessageDigest.isEqual(derive(pin, dec.decode(salt)), dec.decode(hash))
    }
}
