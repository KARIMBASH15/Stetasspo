package com.example.manager

import android.content.Context
import com.example.model.VaultEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

class VaultManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("filehub_vault_prefs", Context.MODE_PRIVATE)
    private val vaultDir = File(context.filesDir, ".vault_secure").apply { mkdirs() }
    private val indexFile = File(vaultDir, "vault_index.dat")

    companion object {
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val ENCRYPTION_XOR_KEY: Byte = 0x5A
    }

    fun isPinSet(): Boolean {
        return !prefs.getString(KEY_PIN_HASH, null).isNullOrEmpty()
    }

    fun setPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val hash = hashString(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return storedHash == hashString(pin)
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun getLockedFiles(): List<VaultEntry> = withContext(Dispatchers.IO) {
        if (!indexFile.exists()) return@withContext emptyList()
        try {
            val jsonStr = indexFile.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<VaultEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    VaultEntry(
                        id = obj.getString("id"),
                        originalName = obj.getString("originalName"),
                        originalPath = obj.getString("originalPath"),
                        vaultFileName = obj.getString("vaultFileName"),
                        dateLocked = obj.getLong("dateLocked"),
                        size = obj.getLong("size"),
                        mimeType = obj.getString("mimeType")
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun saveLockedFiles(entries: List<VaultEntry>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("originalName", entry.originalName)
                put("originalPath", entry.originalPath)
                put("vaultFileName", entry.vaultFileName)
                put("dateLocked", entry.dateLocked)
                put("size", entry.size)
                put("mimeType", entry.mimeType)
            }
            array.put(obj)
        }
        indexFile.writeText(array.toString())
    }

    suspend fun lockFile(file: File, mimeType: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.isDirectory) return@withContext false
            val id = UUID.randomUUID().toString()
            val vaultFileName = "enc_$id.vault"
            val targetFile = File(vaultDir, vaultFileName)

            // Obfuscate & write to secure vault
            FileInputStream(file).use { fis ->
                FileOutputStream(targetFile).use { fos ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        for (i in 0 until bytesRead) {
                            buffer[i] = (buffer[i].toInt() xor ENCRYPTION_XOR_KEY.toInt()).toByte()
                        }
                        fos.write(buffer, 0, bytesRead)
                    }
                }
            }

            val entry = VaultEntry(
                id = id,
                originalName = file.name,
                originalPath = file.absolutePath,
                vaultFileName = vaultFileName,
                dateLocked = System.currentTimeMillis(),
                size = file.length(),
                mimeType = mimeType
            )

            val current = getLockedFiles().toMutableList()
            current.add(entry)
            saveLockedFiles(current)

            // Securely wipe original file
            file.delete()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun unlockFile(entry: VaultEntry): File? = withContext(Dispatchers.IO) {
        try {
            val vaultEncFile = File(vaultDir, entry.vaultFileName)
            if (!vaultEncFile.exists()) return@withContext null

            var restoredFile = File(entry.originalPath)
            if (restoredFile.parentFile?.exists() != true) {
                restoredFile = File(context.getExternalFilesDir(null) ?: context.filesDir, entry.originalName)
            }

            FileInputStream(vaultEncFile).use { fis ->
                FileOutputStream(restoredFile).use { fos ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        for (i in 0 until bytesRead) {
                            buffer[i] = (buffer[i].toInt() xor ENCRYPTION_XOR_KEY.toInt()).toByte()
                        }
                        fos.write(buffer, 0, bytesRead)
                    }
                }
            }

            // Remove from vault
            vaultEncFile.delete()
            val current = getLockedFiles().filter { it.id != entry.id }
            saveLockedFiles(current)
            restoredFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteVaultFile(entry: VaultEntry): Boolean = withContext(Dispatchers.IO) {
        try {
            val vaultEncFile = File(vaultDir, entry.vaultFileName)
            vaultEncFile.delete()
            val current = getLockedFiles().filter { it.id != entry.id }
            saveLockedFiles(current)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
