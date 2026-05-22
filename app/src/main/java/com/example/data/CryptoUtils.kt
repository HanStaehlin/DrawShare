package com.example.data

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

object CryptoUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private val IV = "1234567890123456".toByteArray() // In a real app, use a random IV and transmit it

    private fun generateKey(seed: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = seed.toByteArray(Charsets.UTF_8)
        val hash = digest.digest(bytes)
        return SecretKeySpec(hash.copyOfRange(0, 16), "AES")
    }

    fun encrypt(data: String, seed: String): String {
        val key = generateKey(seed)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, javax.crypto.spec.IvParameterSpec(IV))
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    fun decrypt(encryptedData: String, seed: String): String {
        return try {
            val key = generateKey(seed)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, javax.crypto.spec.IvParameterSpec(IV))
            val decodedBytes = Base64.decode(encryptedData, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            "[]"
        }
    }
}
