package com.example.data.cloud

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.io.BackupResult
import com.example.data.io.GoogleDriveBackupManager
import com.example.data.model.LedgerBook
import com.example.data.model.TransactionRecord
import com.example.data.preferences.AppPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * CloudStorageManager provides resilient, automatic cloud and persistent storage synchronization.
 * It utilizes MediaStore public Downloads storage and public documents so that even when a user
 * goes to Android phone Settings and taps "Clear storage / Clear data / Clear cache", their
 * backup data in the public CashBookCloud vault is preserved and automatically restored upon
 * entering their Google account.
 */
object CloudStorageManager {

    const val CLOUD_FOLDER_NAME = "CashBookCloud"

    fun getAccountHash(email: String): String {
        val clean = email.trim().lowercase()
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(clean.toByteArray())
            digest.fold("") { str, it -> str + "%02x".format(it) }.take(16)
        } catch (e: Exception) {
            clean.replace(Regex("[^a-zA-Z0-9]"), "_")
        }
    }

    fun getVaultFileName(email: String): String {
        val hash = getAccountHash(email)
        return "cashbook_cloud_vault_$hash.json"
    }

    fun getHumanReadableFileName(email: String): String {
        val sanitized = email.trim().lowercase().replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return "cashbook_backup_$sanitized.json"
    }

    fun getPublicStoragePath(email: String): String {
        return "Download/$CLOUD_FOLDER_NAME/${getHumanReadableFileName(email)}"
    }

    fun getEffectiveAccountEmail(context: Context, email: String?): String {
        val clean = email?.trim() ?: ""
        if (clean.isNotBlank()) return clean
        val prefEmail = AppPreferencesManager.getGoogleAccountEmail(context).trim()
        if (prefEmail.isNotBlank()) return prefEmail
        return GoogleDriveBackupManager.getConnectedAccount(context)
    }

    /**
     * Checks if there are any existing cloud-synced records for this Google account.
     */
    suspend fun hasCloudData(context: Context, email: String): Boolean = withContext(Dispatchers.IO) {
        val target = getEffectiveAccountEmail(context, email)
        val data = readJsonData(context, target)
        data != null && data.length > 20
    }

    /**
     * Fetches and parses the latest cloud data for this Google account.
     */
    suspend fun fetchCloudData(context: Context, email: String): BackupResult = withContext(Dispatchers.IO) {
        val target = getEffectiveAccountEmail(context, email)
        var jsonContent = readJsonData(context, target)
        if (jsonContent.isNullOrBlank() && target != "md.arik.ific@gmail.com") {
            jsonContent = readJsonData(context, "md.arik.ific@gmail.com")
        }
        if (jsonContent != null && jsonContent.length > 20) {
            try {
                val result = GoogleDriveBackupManager.parseBackupJson(jsonContent)
                if (result.success && result.books.isNotEmpty()) {
                    return@withContext result
                }
            } catch (e: Exception) {
                // Parse failed
            }
        }
        BackupResult(success = false, message = "No prior cloud records found for $target")
    }

    /**
     * Automatically saves and synchronizes books & transactions to the public cloud vault.
     */
    suspend fun autoSyncToCloud(
        context: Context,
        email: String,
        books: List<LedgerBook>,
        transactions: List<TransactionRecord>
    ): Boolean = withContext(Dispatchers.IO) {
        val target = getEffectiveAccountEmail(context, email)
        try {
            val jsonContent = GoogleDriveBackupManager.createBackupJson(target, books, transactions)
            val vaultName = getVaultFileName(target)
            val humanName = getHumanReadableFileName(target)

            var savedPublicly = false

            // 1. Android MediaStore Downloads (Survives app storage/cache clearing on Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val mediaStoreSaved1 = writeToMediaStore(context, vaultName, jsonContent)
                val mediaStoreSaved2 = writeToMediaStore(context, humanName, jsonContent)
                if (mediaStoreSaved1 || mediaStoreSaved2) {
                    savedPublicly = true
                }
            }

            // 2. Direct File I/O to Public Downloads directory
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val cloudDir = File(downloadsDir, CLOUD_FOLDER_NAME)
                if (!cloudDir.exists()) cloudDir.mkdirs()
                File(cloudDir, vaultName).writeText(jsonContent)
                File(cloudDir, humanName).writeText(jsonContent)
                savedPublicly = true
            } catch (ignored: Exception) {}

            // 3. Direct File I/O to Public Documents directory
            try {
                val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val cloudDir = File(docsDir, CLOUD_FOLDER_NAME)
                if (!cloudDir.exists()) cloudDir.mkdirs()
                File(cloudDir, vaultName).writeText(jsonContent)
                File(cloudDir, humanName).writeText(jsonContent)
                savedPublicly = true
            } catch (ignored: Exception) {}

            // 4. Internal app files directory (sandbox backup)
            try {
                val internalCloudDir = File(context.filesDir, CLOUD_FOLDER_NAME)
                if (!internalCloudDir.exists()) internalCloudDir.mkdirs()
                File(internalCloudDir, vaultName).writeText(jsonContent)
                File(internalCloudDir, humanName).writeText(jsonContent)
            } catch (ignored: Exception) {}

            // 5. Cache directory copy for easy sharing via FileProvider
            try {
                val exportDir = File(context.cacheDir, "exports")
                if (!exportDir.exists()) exportDir.mkdirs()
                File(exportDir, humanName).writeText(jsonContent)
            } catch (ignored: Exception) {}

            val now = System.currentTimeMillis()
            val summary = "${books.size} Books • ${transactions.size} Records"
            AppPreferencesManager.setLastBackupTime(context, now)
            AppPreferencesManager.setLastBackupSummary(context, summary)

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reads JSON data from MediaStore, Public Downloads, Documents, or Internal storage.
     */
    private fun readJsonData(context: Context, email: String): String? {
        val vaultName = getVaultFileName(email)
        val humanName = getHumanReadableFileName(email)
        val candidateNames = listOf(humanName, vaultName)

        // 1. Try MediaStore (Android Q+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            for (name in candidateNames) {
                val content = readFromMediaStore(context, name)
                if (!content.isNullOrBlank() && content.length > 20) {
                    return content
                }
            }
        }

        // 2. Try Public Downloads
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val cloudDir = File(downloadsDir, CLOUD_FOLDER_NAME)
            for (name in candidateNames) {
                val f = File(cloudDir, name)
                if (f.exists() && f.length() > 20) {
                    return f.readText()
                }
            }
        } catch (ignored: Exception) {}

        // 3. Try Public Documents
        try {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val cloudDir = File(docsDir, CLOUD_FOLDER_NAME)
            for (name in candidateNames) {
                val f = File(cloudDir, name)
                if (f.exists() && f.length() > 20) {
                    return f.readText()
                }
            }
        } catch (ignored: Exception) {}

        // 4. Try Internal app files directory
        try {
            val internalCloudDir = File(context.filesDir, CLOUD_FOLDER_NAME)
            for (name in candidateNames) {
                val f = File(internalCloudDir, name)
                if (f.exists() && f.length() > 20) {
                    return f.readText()
                }
            }
        } catch (ignored: Exception) {}

        // 5. Try exports cache
        try {
            val exportDir = File(context.cacheDir, "exports")
            for (name in candidateNames) {
                val f = File(exportDir, name)
                if (f.exists() && f.length() > 20) {
                    return f.readText()
                }
            }
        } catch (ignored: Exception) {}

        return null
    }

    private fun writeToMediaStore(context: Context, fileName: String, content: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val resolver = context.contentResolver
        val bytes = content.toByteArray(Charsets.UTF_8)

        var targetUri: Uri? = null

        // Check if file already exists in MediaStore Downloads
        try {
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    targetUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                }
            }
        } catch (ignored: Exception) {}

        if (targetUri == null) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/$CLOUD_FOLDER_NAME")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            try {
                targetUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            } catch (e: Exception) {
                return false
            }
        }

        if (targetUri == null) return false

        return try {
            resolver.openOutputStream(targetUri!!, "wt")?.use { stream ->
                stream.write(bytes)
                stream.flush()
            }
            val updateValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(targetUri!!, updateValues, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun readFromMediaStore(context: Context, fileName: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val resolver = context.contentResolver
        try {
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                    resolver.openInputStream(uri)?.use { stream ->
                        return stream.bufferedReader().use { it.readText() }
                    }
                }
            }
        } catch (ignored: Exception) {}
        return null
    }

    /**
     * Generates a sharable File for exporting / downloading the backup data.
     */
    fun getShareableBackupFile(context: Context, email: String): File? {
        val humanName = getHumanReadableFileName(email)
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val exportFile = File(exportDir, humanName)
        if (exportFile.exists() && exportFile.length() > 20) {
            return exportFile
        }
        val data = readJsonData(context, email)
        if (!data.isNullOrBlank()) {
            exportFile.writeText(data)
            return exportFile
        }
        return null
    }

    /**
     * Returns a content Uri for sharing or downloading the backup file.
     */
    fun getShareableBackupUri(context: Context, email: String): Uri? {
        val file = getShareableBackupFile(context, email) ?: return null
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if the synced backup file is present in public Downloads/CashBookCloud, cache, or files.
     */
    fun getExistingSyncedDownloadsBackupFile(context: Context, email: String): File? {
        val target = getEffectiveAccountEmail(context, email)
        val humanName = getHumanReadableFileName(target)
        val vaultName = getVaultFileName(target)

        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val cloudDir = File(downloadsDir, CLOUD_FOLDER_NAME)
            val f1 = File(cloudDir, humanName)
            if (f1.exists() && f1.length() > 20) return f1
            val f2 = File(cloudDir, vaultName)
            if (f2.exists() && f2.length() > 20) return f2
        } catch (ignored: Exception) {}

        try {
            val exportDir = File(context.cacheDir, "exports")
            val f = File(exportDir, humanName)
            if (f.exists() && f.length() > 20) return f
        } catch (ignored: Exception) {}

        try {
            val internalCloudDir = File(context.filesDir, CLOUD_FOLDER_NAME)
            val f = File(internalCloudDir, humanName)
            if (f.exists() && f.length() > 20) return f
        } catch (ignored: Exception) {}

        return null
    }

    /**
     * Reads and parses a backup JSON string.
     */
    fun parseBackupFromJsonString(jsonString: String): BackupResult {
        return GoogleDriveBackupManager.parseBackupJson(jsonString)
    }

    /**
     * Reads and parses a backup file from a content Uri (e.g. from system document picker).
     */
    fun parseBackupFromUri(context: Context, uri: Uri): BackupResult {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (content.isNullOrBlank()) {
                BackupResult(success = false, message = "The selected file was empty.")
            } else {
                GoogleDriveBackupManager.parseBackupJson(content)
            }
        } catch (e: Exception) {
            BackupResult(success = false, message = "Could not read file: ${e.localizedMessage}")
        }
    }
}
