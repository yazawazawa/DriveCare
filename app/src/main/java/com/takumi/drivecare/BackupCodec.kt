package com.takumi.drivecare

import java.io.ByteArrayOutputStream
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** バックアップ読み込みの結果。 */
sealed interface BackupReadResult {
    data class Success(val data: BackupData) : BackupReadResult
    /** 暗号化されているがパスワードが未入力。 */
    data object NeedPassword : BackupReadResult
    data object WrongPassword : BackupReadResult
    /** DriveCare のバックアップではない、または壊れている。 */
    data object Invalid : BackupReadResult
}

/**
 * バックアップファイル（.dcbak）の符号化・復号。Android に依存しない。
 *
 * ファイル構造:
 *   "DCB1"(4byte) | flag(1byte: 0=圧縮のみ / 1=暗号化) |
 *   [暗号化時のみ] salt(16byte) | iv(12byte) |
 *   本体 = gzip(JSON)、暗号化時は AES-256-GCM で暗号化（ヘッダ全体を認証データとして改ざん検知）
 *
 * パスワードから PBKDF2(HMAC-SHA256) で鍵を導出するため、端末やアプリの再インストールをまたいで復元できる。
 * 旧バージョンの .json（平文）もそのまま読み込める。
 */
object BackupCodec {
    const val EXTENSION = "dcbak"

    private val MAGIC = byteArrayOf('D'.code.toByte(), 'C'.code.toByte(), 'B'.code.toByte(), '1'.code.toByte())
    private const val FLAG_PLAIN: Byte = 0
    private const val FLAG_ENCRYPTED: Byte = 1
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val PBKDF2_ITERATIONS = 150_000
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    /** 展開後の最大サイズ（壊れたファイルによるメモリ枯渇を防ぐ）。 */
    private const val MAX_JSON_BYTES = 64 * 1024 * 1024

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val random = SecureRandom()

    fun encode(data: BackupData, password: String?): ByteArray {
        val compressed = gzip(json.encodeToString(data).toByteArray(Charsets.UTF_8))
        if (password.isNullOrEmpty()) {
            return MAGIC + byteArrayOf(FLAG_PLAIN) + compressed
        }
        val salt = ByteArray(SALT_SIZE).also(random::nextBytes)
        val iv = ByteArray(IV_SIZE).also(random::nextBytes)
        val header = MAGIC + byteArrayOf(FLAG_ENCRYPTED) + salt + iv
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
            updateAAD(header)
        }
        return header + cipher.doFinal(compressed)
    }

    fun decode(bytes: ByteArray, password: String?): BackupReadResult {
        if (bytes.isEmpty()) return BackupReadResult.Invalid
        // 旧形式: 平文 JSON
        if (bytes[0] == '{'.code.toByte() || bytes[0] == 0xEF.toByte()) {
            return parseJson(bytes.toString(Charsets.UTF_8).removePrefix("﻿"))
        }
        if (bytes.size < MAGIC.size + 1 || !bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            return BackupReadResult.Invalid
        }
        return when (bytes[MAGIC.size]) {
            FLAG_PLAIN -> parseCompressed(bytes.copyOfRange(MAGIC.size + 1, bytes.size))
            FLAG_ENCRYPTED -> {
                val headerSize = MAGIC.size + 1 + SALT_SIZE + IV_SIZE
                if (bytes.size <= headerSize) return BackupReadResult.Invalid
                if (password.isNullOrEmpty()) return BackupReadResult.NeedPassword
                val salt = bytes.copyOfRange(MAGIC.size + 1, MAGIC.size + 1 + SALT_SIZE)
                val iv = bytes.copyOfRange(MAGIC.size + 1 + SALT_SIZE, headerSize)
                val plain = try {
                    Cipher.getInstance("AES/GCM/NoPadding").apply {
                        init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
                        updateAAD(bytes, 0, headerSize)
                    }.doFinal(bytes, headerSize, bytes.size - headerSize)
                } catch (_: GeneralSecurityException) {
                    // パスワード違い、またはファイルの改ざん・破損
                    return BackupReadResult.WrongPassword
                }
                parseCompressed(plain)
            }
            else -> BackupReadResult.Invalid
        }
    }

    private fun parseCompressed(compressed: ByteArray): BackupReadResult = try {
        parseJson(gunzip(compressed).toString(Charsets.UTF_8))
    } catch (_: Exception) {
        BackupReadResult.Invalid
    }

    private fun parseJson(text: String): BackupReadResult = try {
        BackupReadResult.Success(json.decodeFromString<BackupData>(text))
    } catch (_: Exception) {
        BackupReadResult.Invalid
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(key, "AES")
    }

    private fun gzip(input: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(input) }
        return out.toByteArray()
    }

    private fun gunzip(input: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPInputStream(input.inputStream()).use { stream ->
            val buffer = ByteArray(8192)
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
                if (out.size() > MAX_JSON_BYTES) throw IllegalStateException("backup too large")
            }
        }
        return out.toByteArray()
    }
}
