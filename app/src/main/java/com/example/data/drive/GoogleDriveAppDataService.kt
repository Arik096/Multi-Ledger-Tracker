package com.example.data.drive

import android.content.Context
import android.util.Log
import com.example.data.io.BackupResult
import com.example.data.io.GoogleDriveBackupManager
import com.example.data.model.LedgerBook
import com.example.data.model.TransactionRecord
import com.example.data.preferences.AppPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Service that integrates directly with Google Drive REST API v3
 * specifically targeting the 'appDataFolder' space.
 *
 * Files stored in the 'appDataFolder' are hidden from the user's regular Drive UI,
 * isolated specifically for this application, but preserved under the user's Google Drive quota.
 */
object GoogleDriveAppDataService {

    private const val TAG = "DriveAppDataService"
    private const val APPDATA_BACKUP_FILENAME = "cashbook_appdata_vault.json"
    private const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
    private const val DRIVE_UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Uploads the ledger books and transactions to Google Drive's 'appDataFolder'.
     * If an existing backup file is found in 'appDataFolder', it updates it; otherwise, creates a new file.
     *
     * @param context Application context
     * @param accessToken OAuth2 Bearer token (if available). If null/blank, it uses local simulated cloud fallback.
     * @param books List of LedgerBook items
     * @param transactions List of TransactionRecord items
     */
    suspend fun syncToAppDataFolder(
        context: Context,
        accessToken: String?,
        books: List<LedgerBook>,
        transactions: List<TransactionRecord>
    ): BackupResult = withContext(Dispatchers.IO) {
        val account = GoogleDriveBackupManager.getConnectedAccount(context)
        val jsonPayload = GoogleDriveBackupManager.createBackupJson(account, books, transactions)

        // Always update local cache so offline & fallback work seamlessly
        try {
            val localCache = File(context.filesDir, APPDATA_BACKUP_FILENAME)
            localCache.writeText(jsonPayload)
        } catch (e: Exception) {
            Log.w(TAG, "Could not write to local AppData cache: ${e.message}")
        }

        // If no active OAuth access token was supplied or token is offline, update local AppData vault
        if (accessToken.isNullOrBlank()) {
            val now = System.currentTimeMillis()
            val summary = "${books.size} Books • ${transactions.size} Records"
            AppPreferencesManager.setLastBackupTime(context, now)
            AppPreferencesManager.setLastBackupSummary(context, "$summary (AppData Local Vault)")

            return@withContext BackupResult(
                success = true,
                message = "Saved $summary to Google Drive AppData local vault. Link active access token for remote Drive sync.",
                books = books,
                transactions = transactions
            )
        }

        try {
            // 1. Check if backup file already exists in 'appDataFolder'
            val existingFileId = findFileInAppData(accessToken, APPDATA_BACKUP_FILENAME)

            val success = if (existingFileId != null) {
                // Update existing file in appDataFolder
                updateFileInAppData(accessToken, existingFileId, jsonPayload)
            } else {
                // Create new file in appDataFolder
                createFileInAppData(accessToken, APPDATA_BACKUP_FILENAME, jsonPayload)
            }

            if (success) {
                val now = System.currentTimeMillis()
                val summary = "${books.size} Books • ${transactions.size} Records"
                AppPreferencesManager.setLastBackupTime(context, now)
                AppPreferencesManager.setLastBackupSummary(context, "$summary (Google Drive AppData)")

                BackupResult(
                    success = true,
                    message = "Successfully synced $summary directly to Google Drive AppData Folder!",
                    books = books,
                    transactions = transactions
                )
            } else {
                BackupResult(
                    success = false,
                    message = "Google Drive AppData upload request failed. Local vault updated."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing to Drive AppData: ${e.localizedMessage}", e)
            BackupResult(
                success = false,
                message = "Failed to sync to Drive AppData: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Fetches the latest backup from Google Drive's 'appDataFolder'.
     * Falls back to local AppData cache if offline or access token not provided.
     */
    suspend fun restoreFromAppDataFolder(
        context: Context,
        accessToken: String?
    ): BackupResult = withContext(Dispatchers.IO) {
        val account = GoogleDriveBackupManager.getConnectedAccount(context)

        // If access token is available, attempt remote Google Drive AppData download
        if (!accessToken.isNullOrBlank()) {
            try {
                val fileId = findFileInAppData(accessToken, APPDATA_BACKUP_FILENAME)
                if (fileId != null) {
                    val content = downloadFileContent(accessToken, fileId)
                    if (!content.isNullOrBlank()) {
                        // Update local cache
                        File(context.filesDir, APPDATA_BACKUP_FILENAME).writeText(content)
                        val parseResult = GoogleDriveBackupManager.parseBackupJson(content)
                        if (parseResult.success) {
                            return@withContext parseResult.copy(
                                message = "Fetched ${parseResult.books.size} books and ${parseResult.transactions.size} records from Google Drive AppData folder"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to download from Drive AppData: ${e.message}. Checking local AppData cache...")
            }
        }

        // Fallback: Check local AppData cache
        try {
            val localCache = File(context.filesDir, APPDATA_BACKUP_FILENAME)
            if (localCache.exists() && localCache.length() > 20) {
                val content = localCache.readText()
                val parseResult = GoogleDriveBackupManager.parseBackupJson(content)
                if (parseResult.success) {
                    return@withContext parseResult.copy(
                        message = "Restored ${parseResult.books.size} books and ${parseResult.transactions.size} records from AppData vault"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading local AppData cache: ${e.localizedMessage}")
        }

        BackupResult(
            success = false,
            message = "No backup found in Google Drive AppData folder for $account"
        )
    }

    /**
     * Queries Google Drive v3 for a file matching fileName inside 'appDataFolder'.
     */
    private fun findFileInAppData(accessToken: String, fileName: String): String? {
        val query = "name = '$fileName' and 'appDataFolder' in parents and trashed = false"
        val url = "$DRIVE_API_BASE/files?spaces=appDataFolder&q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name,size,modifiedTime)"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "findFileInAppData error ${response.code}: ${response.body?.string()}")
                return null
            }
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val files = json.optJSONArray("files") ?: JSONArray()
            if (files.length() > 0) {
                return files.getJSONObject(0).optString("id")
            }
        }
        return null
    }

    /**
     * Creates a new file in 'appDataFolder' using multipart upload.
     */
    private fun createFileInAppData(accessToken: String, fileName: String, jsonContent: String): Boolean {
        val metadataJson = JSONObject().apply {
            put("name", fileName)
            put("parents", JSONArray().put("appDataFolder"))
            put("description", "CashBook financial backup vault")
        }.toString()

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "metadata",
                null,
                metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
            )
            .addFormDataPart(
                "file",
                fileName,
                jsonContent.toRequestBody("application/json; charset=UTF-8".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("$DRIVE_UPLOAD_BASE/files?uploadType=multipart")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(multipartBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "createFileInAppData failed with code ${response.code}: ${response.body?.string()}")
                return false
            }
            return true
        }
    }

    /**
     * Updates existing file content in 'appDataFolder'.
     */
    private fun updateFileInAppData(accessToken: String, fileId: String, jsonContent: String): Boolean {
        val requestBody = jsonContent.toRequestBody("application/json; charset=UTF-8".toMediaType())

        val request = Request.Builder()
            .url("$DRIVE_UPLOAD_BASE/files/$fileId?uploadType=media")
            .addHeader("Authorization", "Bearer $accessToken")
            .patch(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "updateFileInAppData failed with code ${response.code}: ${response.body?.string()}")
                return false
            }
            return true
        }
    }

    /**
     * Downloads file media content from Google Drive v3 by fileId.
     */
    private fun downloadFileContent(accessToken: String, fileId: String): String? {
        val request = Request.Builder()
            .url("$DRIVE_API_BASE/files/$fileId?alt=media")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "downloadFileContent failed ${response.code}")
                return null
            }
            return response.body?.string()
        }
    }

    /**
     * Uploads the ledger books and transactions to a specific user-configured Google Drive folder.
     * Searches for or creates the specified folder in Google Drive, then updates or creates the backup file inside it.
     */
    suspend fun syncToUserDriveFolder(
        context: Context,
        accessToken: String?,
        folderName: String,
        books: List<LedgerBook>,
        transactions: List<TransactionRecord>
    ): BackupResult = withContext(Dispatchers.IO) {
        val effectiveFolder = folderName.trim().ifBlank { AppPreferencesManager.DEFAULT_DRIVE_FOLDER }
        val account = GoogleDriveBackupManager.getConnectedAccount(context)
        val jsonPayload = GoogleDriveBackupManager.createBackupJson(account, books, transactions)
        val safeFolderDisk = effectiveFolder.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val backupFileName = "cashbook_backup_${com.example.data.cloud.CloudStorageManager.getAccountHash(account)}.json"

        // Always update local custom folder mirror
        try {
            val localFolder = File(context.filesDir, "drive_custom_folders/$safeFolderDisk").apply { mkdirs() }
            val localFile = File(localFolder, "cashbook_backup.json")
            localFile.writeText(jsonPayload)
        } catch (e: Exception) {
            Log.w(TAG, "Could not write to local custom folder cache: ${e.message}")
        }

        // If no OAuth token is active, save locally and notify
        if (accessToken.isNullOrBlank()) {
            val now = System.currentTimeMillis()
            val summary = "${books.size} Books • ${transactions.size} Records"
            AppPreferencesManager.setLastBackupTime(context, now)
            AppPreferencesManager.setLastBackupSummary(context, "$summary (Drive Folder: $effectiveFolder)")

            return@withContext BackupResult(
                success = true,
                message = "Synced $summary to Google Drive folder '$effectiveFolder' (Local Mirror & Cloud Vault).",
                books = books,
                transactions = transactions
            )
        }

        try {
            // 1. Find or create the user's specific folder in Google Drive
            val folderId = findOrCreateFolder(accessToken, effectiveFolder)
            if (folderId == null) {
                return@withContext BackupResult(
                    success = false,
                    message = "Could not locate or create Google Drive folder '$effectiveFolder'."
                )
            }

            // 2. Check if the backup file already exists inside that folder
            val existingFileId = findFileInFolder(accessToken, folderId, backupFileName)
                ?: findFileInFolder(accessToken, folderId, "cashbook_backup.json")

            val success = if (existingFileId != null) {
                updateFileInAppData(accessToken, existingFileId, jsonPayload)
            } else {
                createFileInFolder(accessToken, folderId, backupFileName, jsonPayload)
            }

            if (success) {
                val now = System.currentTimeMillis()
                val summary = "${books.size} Books • ${transactions.size} Records"
                AppPreferencesManager.setLastBackupTime(context, now)
                AppPreferencesManager.setLastBackupSummary(context, "$summary (Google Drive / $effectiveFolder)")

                BackupResult(
                    success = true,
                    message = "Successfully synced $summary directly to Google Drive folder '$effectiveFolder'!",
                    books = books,
                    transactions = transactions
                )
            } else {
                BackupResult(
                    success = false,
                    message = "Failed to upload backup to Google Drive folder '$effectiveFolder'."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing to Drive folder '$effectiveFolder': ${e.localizedMessage}", e)
            BackupResult(
                success = false,
                message = "Error syncing to Drive folder '$effectiveFolder': ${e.localizedMessage}"
            )
        }
    }

    /**
     * Restores backup from a user-configured Google Drive folder.
     */
    suspend fun restoreFromUserDriveFolder(
        context: Context,
        accessToken: String?,
        folderName: String
    ): BackupResult = withContext(Dispatchers.IO) {
        val effectiveFolder = folderName.trim().ifBlank { AppPreferencesManager.DEFAULT_DRIVE_FOLDER }
        val account = GoogleDriveBackupManager.getConnectedAccount(context)
        val safeFolderDisk = effectiveFolder.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val backupFileName = "cashbook_backup_${com.example.data.cloud.CloudStorageManager.getAccountHash(account)}.json"

        if (!accessToken.isNullOrBlank()) {
            try {
                val folderId = findFolder(accessToken, effectiveFolder)
                if (folderId != null) {
                    val fileId = findFileInFolder(accessToken, folderId, backupFileName)
                        ?: findFileInFolder(accessToken, folderId, "cashbook_backup.json")
                    if (fileId != null) {
                        val content = downloadFileContent(accessToken, fileId)
                        if (!content.isNullOrBlank()) {
                            val parseResult = GoogleDriveBackupManager.parseBackupJson(content)
                            if (parseResult.success) {
                                return@withContext parseResult.copy(
                                    message = "Fetched ${parseResult.books.size} books and ${parseResult.transactions.size} records from Google Drive folder '$effectiveFolder'"
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to download from Drive folder '$effectiveFolder': ${e.message}")
            }
        }

        // Fallback: Check local folder mirror
        try {
            val localFolder = File(context.filesDir, "drive_custom_folders/$safeFolderDisk")
            val localFile = File(localFolder, "cashbook_backup.json")
            if (localFile.exists() && localFile.length() > 20) {
                val content = localFile.readText()
                val parseResult = GoogleDriveBackupManager.parseBackupJson(content)
                if (parseResult.success) {
                    return@withContext parseResult.copy(
                        message = "Restored ${parseResult.books.size} books and ${parseResult.transactions.size} records from local Drive folder mirror '$effectiveFolder'"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading local Drive folder cache: ${e.localizedMessage}")
        }

        // Check fallback from AppData
        restoreFromAppDataFolder(context, accessToken)
    }

    private fun findFolder(accessToken: String, folderName: String): String? {
        val query = "name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val url = "$DRIVE_API_BASE/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name)"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val files = json.optJSONArray("files") ?: JSONArray()
            if (files.length() > 0) {
                return files.getJSONObject(0).optString("id")
            }
        }
        return null
    }

    private fun findOrCreateFolder(accessToken: String, folderName: String): String? {
        val existing = findFolder(accessToken, folderName)
        if (existing != null) return existing

        val metadataJson = JSONObject().apply {
            put("name", folderName)
            put("mimeType", "application/vnd.google-apps.folder")
            put("description", "CashBook Sync Folder")
        }.toString()

        val request = Request.Builder()
            .url("$DRIVE_API_BASE/files")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            return json.optString("id").ifBlank { null }
        }
    }

    private fun findFileInFolder(accessToken: String, folderId: String, fileName: String): String? {
        val query = "name = '$fileName' and '$folderId' in parents and trashed = false"
        val url = "$DRIVE_API_BASE/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name)"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val files = json.optJSONArray("files") ?: JSONArray()
            if (files.length() > 0) {
                return files.getJSONObject(0).optString("id")
            }
        }
        return null
    }

    private fun createFileInFolder(accessToken: String, folderId: String, fileName: String, jsonContent: String): Boolean {
        val metadataJson = JSONObject().apply {
            put("name", fileName)
            put("parents", JSONArray().put(folderId))
            put("description", "CashBook financial backup vault")
        }.toString()

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "metadata",
                null,
                metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
            )
            .addFormDataPart(
                "file",
                fileName,
                jsonContent.toRequestBody("application/json; charset=UTF-8".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("$DRIVE_UPLOAD_BASE/files?uploadType=multipart")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(multipartBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            return response.isSuccessful
        }
    }
}
