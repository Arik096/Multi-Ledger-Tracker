package com.example.data.io

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.example.data.model.LedgerBook
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.data.preferences.AppPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupMetadata(
    val exportDate: String,
    val timestamp: Long,
    val bookCount: Int,
    val transactionCount: Int,
    val totalCashIn: Double,
    val totalCashOut: Double,
    val accountEmail: String
)

data class BackupResult(
    val success: Boolean,
    val message: String,
    val metadata: BackupMetadata? = null,
    val books: List<LedgerBook> = emptyList(),
    val transactions: List<TransactionRecord> = emptyList()
)

object GoogleDriveBackupManager {
    private const val PREFS_NAME = "google_drive_backup_prefs"
    private const val KEY_ACCOUNT_EMAIL = "key_google_account_email"
    private const val KEY_LAST_BACKUP_TIME = "key_last_backup_time"
    private const val KEY_LAST_BACKUP_COUNT = "key_last_backup_count"
    private const val KEY_ACCESS_TOKEN = "key_google_access_token"
    private const val DEFAULT_ACCOUNT = "md.arik.ific@gmail.com"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getConnectedAccount(context: Context): String {
        return getPrefs(context).getString(KEY_ACCOUNT_EMAIL, DEFAULT_ACCOUNT) ?: DEFAULT_ACCOUNT
    }

    fun setConnectedAccount(context: Context, email: String) {
        getPrefs(context).edit().putString(KEY_ACCOUNT_EMAIL, email).apply()
    }

    fun getAccessToken(context: Context): String? {
        return getPrefs(context).getString(KEY_ACCESS_TOKEN, null)
    }

    fun setAccessToken(context: Context, token: String?) {
        getPrefs(context).edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getLastBackupTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_BACKUP_TIME, 0L)
    }

    fun getLastBackupSummary(context: Context): String {
        val lastTime = getLastBackupTime(context)
        if (lastTime == 0L) return "No backup yet"
        val df = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        val count = getPrefs(context).getString(KEY_LAST_BACKUP_COUNT, "") ?: ""
        return if (count.isNotBlank()) "${df.format(Date(lastTime))} ($count)" else df.format(Date(lastTime))
    }

    /**
     * Serializes all books and transactions into a standard JSON payload.
     */
    fun createBackupJson(
        accountEmail: String,
        books: List<LedgerBook>,
        transactions: List<TransactionRecord>
    ): String {
        val root = JSONObject()
        root.put("app", "CashBook")
        root.put("version", 2)
        root.put("accountEmail", accountEmail)
        root.put("timestamp", System.currentTimeMillis())
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))

        val booksArray = JSONArray()
        for (b in books) {
            val bookObj = JSONObject()
            bookObj.put("id", b.id)
            bookObj.put("name", b.name)
            bookObj.put("currencySymbol", b.currencySymbol)
            bookObj.put("currencyCode", b.currencyCode)
            bookObj.put("colorHex", b.colorHex)
            bookObj.put("iconName", b.iconName)
            bookObj.put("isArchived", b.isArchived)
            bookObj.put("customCategories", b.customCategories)
            bookObj.put("isCategoryEnabled", b.isCategoryEnabled)
            bookObj.put("isCategoryMandatory", b.isCategoryMandatory)
            bookObj.put("isTransactionTypeEnabled", b.isTransactionTypeEnabled)
            bookObj.put("isTransactionTypeMandatory", b.isTransactionTypeMandatory)
            bookObj.put("createdAt", b.createdAt)
            booksArray.put(bookObj)
        }
        root.put("books", booksArray)

        val txArray = JSONArray()
        for (t in transactions) {
            val txObj = JSONObject()
            txObj.put("id", t.id)
            txObj.put("ledgerBookId", t.ledgerBookId)
            txObj.put("type", t.type.name)
            txObj.put("amount", t.amount)
            txObj.put("category", t.category)
            txObj.put("timestamp", t.timestamp)
            txObj.put("memo", t.memo)
            txObj.put("paymentMode", t.paymentMode)
            txObj.put("createdAt", t.createdAt)
            txArray.put(txObj)
        }
        root.put("transactions", txArray)

        return root.toString(2)
    }

    /**
     * Parses a backup JSON string into entities.
     */
    fun parseBackupJson(jsonString: String): BackupResult {
        return try {
            val root = JSONObject(jsonString)
            val accountEmail = root.optString("accountEmail", "Google Drive User")
            val timestamp = root.optLong("timestamp", System.currentTimeMillis())
            val exportDate = root.optString("exportDate", "Unknown date")

            val booksArray = root.optJSONArray("books") ?: JSONArray()
            val parsedBooks = mutableListOf<LedgerBook>()
            for (i in 0 until booksArray.length()) {
                val b = booksArray.getJSONObject(i)
                parsedBooks.add(
                    LedgerBook(
                        id = b.optLong("id", 0L),
                        name = b.optString("name", "Restored Book"),
                        currencySymbol = b.optString("currencySymbol", "$"),
                        currencyCode = b.optString("currencyCode", "USD"),
                        colorHex = b.optLong("colorHex", 0xFF0D9488L),
                        iconName = b.optString("iconName", "wallet"),
                        isArchived = b.optBoolean("isArchived", false),
                        customCategories = b.optString("customCategories", ""),
                        isCategoryEnabled = b.optBoolean("isCategoryEnabled", true),
                        isCategoryMandatory = b.optBoolean("isCategoryMandatory", false),
                        isTransactionTypeEnabled = b.optBoolean("isTransactionTypeEnabled", true),
                        isTransactionTypeMandatory = b.optBoolean("isTransactionTypeMandatory", false),
                        createdAt = b.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val txArray = root.optJSONArray("transactions") ?: JSONArray()
            val parsedTx = mutableListOf<TransactionRecord>()
            var totalIn = 0.0
            var totalOut = 0.0

            for (i in 0 until txArray.length()) {
                val t = txArray.getJSONObject(i)
                val typeStr = t.optString("type", "CASH_OUT")
                val txType = if (typeStr == "CASH_IN" || typeStr.contains("INCOME", ignoreCase = true)) {
                    TransactionType.CASH_IN
                } else {
                    TransactionType.CASH_OUT
                }
                val amount = t.optDouble("amount", 0.0)
                if (txType == TransactionType.CASH_IN) totalIn += amount else totalOut += amount

                parsedTx.add(
                    TransactionRecord(
                        id = t.optLong("id", 0L),
                        ledgerBookId = t.optLong("ledgerBookId", 0L),
                        type = txType,
                        amount = amount,
                        category = t.optString("category", "General"),
                        timestamp = t.optLong("timestamp", System.currentTimeMillis()),
                        memo = t.optString("memo", ""),
                        paymentMode = t.optString("paymentMode", "Cash"),
                        createdAt = t.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val meta = BackupMetadata(
                exportDate = exportDate,
                timestamp = timestamp,
                bookCount = parsedBooks.size,
                transactionCount = parsedTx.size,
                totalCashIn = totalIn,
                totalCashOut = totalOut,
                accountEmail = accountEmail
            )

            BackupResult(
                success = true,
                message = "Successfully parsed backup",
                metadata = meta,
                books = parsedBooks,
                transactions = parsedTx
            )
        } catch (e: Exception) {
            BackupResult(
                success = false,
                message = "Invalid backup format: ${e.localizedMessage}"
            )
        }
    }

    fun getDriveFolderName(context: Context): String {
        return AppPreferencesManager.getGoogleDriveFolderName(context)
    }

    fun setDriveFolderName(context: Context, folderName: String) {
        AppPreferencesManager.setGoogleDriveFolderName(context, folderName)
    }

    /**
     * Executes backup directly to Google Drive user-configured folder with local sync preservation.
     */
    suspend fun backupToGoogleDrive(
        context: Context,
        books: List<LedgerBook>,
        transactions: List<TransactionRecord>,
        folderName: String = AppPreferencesManager.getGoogleDriveFolderName(context)
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken(context)
            val driveResult = com.example.data.drive.GoogleDriveAppDataService.syncToUserDriveFolder(
                context = context,
                accessToken = token,
                folderName = folderName,
                books = books,
                transactions = transactions
            )

            // Also keep AppData backup updated
            com.example.data.drive.GoogleDriveAppDataService.syncToAppDataFolder(
                context = context,
                accessToken = token,
                books = books,
                transactions = transactions
            )

            val account = getConnectedAccount(context)
            val jsonContent = createBackupJson(account, books, transactions)

            // Save to internal cloud sync file for instant cache availability
            val syncFile = File(context.filesDir, "google_drive_cloud_backup.json")
            syncFile.writeText(jsonContent)

            // Update preferences
            val now = System.currentTimeMillis()
            val summary = "${books.size} Books, ${transactions.size} Logs"
            getPrefs(context).edit()
                .putLong(KEY_LAST_BACKUP_TIME, now)
                .putString(KEY_LAST_BACKUP_COUNT, summary)
                .apply()

            val meta = BackupMetadata(
                exportDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(now)),
                timestamp = now,
                bookCount = books.size,
                transactionCount = transactions.size,
                totalCashIn = transactions.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount },
                totalCashOut = transactions.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount },
                accountEmail = account
            )

            BackupResult(
                success = true,
                message = if (driveResult.success) driveResult.message else "All $summary backed up to Google Drive folder '$folderName' ($account)",
                metadata = meta,
                books = books,
                transactions = transactions
            )
        } catch (e: Exception) {
            BackupResult(
                success = false,
                message = "Failed to backup to Google Drive folder '$folderName': ${e.localizedMessage}"
            )
        }
    }

    /**
     * Retrieves and prepares the Google Drive backup for restoration from the configured folder.
     */
    suspend fun restoreFromGoogleDrive(
        context: Context,
        folderName: String = AppPreferencesManager.getGoogleDriveFolderName(context)
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken(context)
            val folderResult = com.example.data.drive.GoogleDriveAppDataService.restoreFromUserDriveFolder(
                context = context,
                accessToken = token,
                folderName = folderName
            )
            if (folderResult.success && folderResult.books.isNotEmpty()) {
                return@withContext folderResult
            }

            val appDataResult = com.example.data.drive.GoogleDriveAppDataService.restoreFromAppDataFolder(
                context = context,
                accessToken = token
            )
            if (appDataResult.success && appDataResult.books.isNotEmpty()) {
                return@withContext appDataResult
            }

            val syncFile = File(context.filesDir, "google_drive_cloud_backup.json")
            if (!syncFile.exists()) {
                return@withContext BackupResult(
                    success = false,
                    message = "No Google Drive backup file found in folder '$folderName' for account ${getConnectedAccount(context)}"
                )
            }
            val content = syncFile.readText()
            parseBackupJson(content)
        } catch (e: Exception) {
            BackupResult(
                success = false,
                message = "Failed to read Google Drive backup: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Exports local JSON file for sharing / offline storage.
     */
    suspend fun exportToLocalFile(
        context: Context,
        books: List<LedgerBook>,
        transactions: List<TransactionRecord>
    ): File = withContext(Dispatchers.IO) {
        val account = getConnectedAccount(context)
        val json = createBackupJson(account, books, transactions)
        val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "CashBook_Backup_$timeStr.json")
        file.writeText(json)
        file
    }

    /**
     * Reads a backup JSON from a file URI.
     */
    suspend fun importFromLocalUri(context: Context, uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return@withContext BackupResult(success = false, message = "Could not open selected file")

            parseBackupJson(content)
        } catch (e: Exception) {
            BackupResult(
                success = false,
                message = "Failed to import file: ${e.localizedMessage}"
            )
        }
    }
}
