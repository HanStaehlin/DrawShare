package com.example.data

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import java.security.SecureRandom

object CryptoUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val IV_SIZE = 16
    private const val V2_PREFIX = "v2:"

    // Legacy fixed IV. Kept only for decrypting messages written before per-message IVs.
    private val LEGACY_IV = "1234567890123456".toByteArray()

    private val secureRandom = SecureRandom()

    private fun generateKey(seed: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(seed.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(hash.copyOfRange(0, 16), "AES")
    }

    fun encrypt(data: String, seed: String): String {
        val key = generateKey(seed)
        val iv = ByteArray(IV_SIZE).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val ciphertext = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        val payload = ByteArray(IV_SIZE + ciphertext.size).also {
            System.arraycopy(iv, 0, it, 0, IV_SIZE)
            System.arraycopy(ciphertext, 0, it, IV_SIZE, ciphertext.size)
        }
        return V2_PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(encryptedData: String, seed: String): String {
        return try {
            val key = generateKey(seed)
            val cipher = Cipher.getInstance(ALGORITHM)

            if (encryptedData.startsWith(V2_PREFIX)) {
                val payload = Base64.decode(encryptedData.substring(V2_PREFIX.length), Base64.NO_WRAP)
                if (payload.size < IV_SIZE) return "[]"
                val iv = payload.copyOfRange(0, IV_SIZE)
                val ciphertext = payload.copyOfRange(IV_SIZE, payload.size)
                cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
                String(cipher.doFinal(ciphertext), Charsets.UTF_8)
            } else {
                cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(LEGACY_IV))
                val decoded = Base64.decode(encryptedData, Base64.NO_WRAP)
                String(cipher.doFinal(decoded), Charsets.UTF_8)
            }
        } catch (_: Exception) {
            "[]"
        }
    }
}
