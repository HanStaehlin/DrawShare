package com.example.data

import kotlinx.cinterop.*
import platform.CommonCrypto.*
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalForeignApi::class, ExperimentalEncodingApi::class)
internal actual object CryptoUtils {
    private const val IV_SIZE = 16
    private const val V2_PREFIX = "v2:"
    private val LEGACY_IV = "1234567890123456".encodeToByteArray()

    private fun sha256Key(seed: String): ByteArray {
        val seedBytes = seed.encodeToByteArray()
        val hash = ByteArray(CC_SHA256_DIGEST_LENGTH.toInt())
        seedBytes.usePinned { pinned ->
            hash.usePinned { hashPinned ->
                CC_SHA256(pinned.addressOf(0), seedBytes.size.toUInt(), hashPinned.addressOf(0))
            }
        }
        return hash.copyOfRange(0, 16)
    }

    private fun randomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        bytes.usePinned { pinned ->
            SecRandomCopyBytes(kSecRandomDefault, size.toULong(), pinned.addressOf(0))
        }
        return bytes
    }

    private fun aesCbc(
        encrypt: Boolean,
        data: ByteArray,
        key: ByteArray,
        iv: ByteArray,
    ): ByteArray? {
        val outputBuf = ByteArray(data.size + kCCBlockSizeAES128.toInt())
        val op = if (encrypt) kCCEncrypt else kCCDecrypt
        var cryptStatus: Int = -1
        val numOut = memScoped {
            val numOutVar = alloc<ULongVar>()
            data.usePinned { d ->
                key.usePinned { k ->
                    iv.usePinned { ivPin ->
                        outputBuf.usePinned { o ->
                            cryptStatus = CCCrypt(
                                op,
                                kCCAlgorithmAES128,
                                kCCOptionPKCS7Padding,
                                k.addressOf(0), key.size.toULong(),
                                ivPin.addressOf(0),
                                d.addressOf(0), data.size.toULong(),
                                o.addressOf(0), outputBuf.size.toULong(),
                                numOutVar.ptr,
                            ).toInt()
                        }
                    }
                }
            }
            numOutVar.value
        }
        return if (cryptStatus == kCCSuccess) outputBuf.copyOfRange(0, numOut.toInt()) else null
    }

    actual fun encrypt(data: String, seed: String): String {
        val key = sha256Key(seed)
        val iv = randomBytes(IV_SIZE)
        val ciphertext = aesCbc(true, data.encodeToByteArray(), key, iv) ?: return ""
        val payload = iv + ciphertext
        return V2_PREFIX + Base64.encode(payload)
    }

    actual fun decrypt(encryptedData: String, seed: String): String = try {
        val key = sha256Key(seed)
        if (encryptedData.startsWith(V2_PREFIX)) {
            val payload = Base64.decode(encryptedData.substring(V2_PREFIX.length))
            if (payload.size < IV_SIZE) return "[]"
            val iv = payload.copyOfRange(0, IV_SIZE)
            val ciphertext = payload.copyOfRange(IV_SIZE, payload.size)
            aesCbc(false, ciphertext, key, iv)?.decodeToString() ?: "[]"
        } else {
            val decoded = Base64.decode(encryptedData)
            aesCbc(false, decoded, key, LEGACY_IV)?.decodeToString() ?: "[]"
        }
    } catch (_: Exception) {
        "[]"
    }
}
