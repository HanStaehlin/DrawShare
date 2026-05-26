package com.example.data

internal expect object CryptoUtils {
    fun encrypt(data: String, seed: String): String
    fun decrypt(encryptedData: String, seed: String): String
}
