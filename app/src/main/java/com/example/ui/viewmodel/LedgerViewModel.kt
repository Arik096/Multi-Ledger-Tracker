package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.cloud.CloudStorageManager
import com.example.data.db.AppDatabase
import com.example.data.io.BackupResult
import com.example.data.io.CsvExporterImporter
import com.example.data.io.GoogleDriveBackupManager
import com.example.data.io.ParsedImportRow
import com.example.data.io.PdfReportGenerator
import com.example.data.model.CategoryConstants
import com.example.data.model.DateRangeFilter
import com.example.data.model.FilterCriteria
import com.example.data.model.LedgerBook
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.data.preferences.AppPreferencesManager
import com.example.data.preferences.ModernPalette
import com.example.data.preferences.ThemeMode
import com.example.data.repository.LedgerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

data class CategorySummary(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val count: Int
)

data class TrendPoint(
    val label: String,
    val timestamp: Long,
    val income: Double,
    val expense: Double
)

class LedgerViewModel(
    application: Application,
    private val repository: LedgerRepository
) : AndroidViewModel(application) {

    val allBooks: StateFlow<List<LedgerBook>> = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeBooks: StateFlow<List<LedgerBook>> = repository.activeBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionRecord>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Book-wise selection for dashboard statistics (strictly individual books, no consolidated view)
    private val _selectedBookId = MutableStateFlow<Long?>(null)
    val selectedBookId: StateFlow<Long?> = _selectedBookId.asStateFlow()

    private val _filterCriteria = MutableStateFlow(FilterCriteria())
    val filterCriteria: StateFlow<FilterCriteria> = _filterCriteria.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    // App Preferences & User Profile States
    private val _themeMode = MutableStateFlow(AppPreferencesManager.getThemeMode(application))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _modernPalette = MutableStateFlow(AppPreferencesManager.getModernPalette(application))
    val modernPalette: StateFlow<ModernPalette> = _modernPalette.asStateFlow()

    private val _driveAccountEmail = MutableStateFlow(AppPreferencesManager.getGoogleAccountEmail(application))
    val driveAccountEmail: StateFlow<String> = _driveAccountEmail.asStateFlow()

    private val _isDriveConnected = MutableStateFlow(AppPreferencesManager.isGoogleDriveConnected(application))
    val isDriveConnected: StateFlow<Boolean> = _isDriveConnected.asStateFlow()

    private val _googleDriveFolderName = MutableStateFlow(AppPreferencesManager.getGoogleDriveFolderName(application))
    val googleDriveFolderName: StateFlow<String> = _googleDriveFolderName.asStateFlow()

    private val _isAutoBackupEnabled = MutableStateFlow(AppPreferencesManager.isAutoBackupEnabled(application))
    val isAutoBackupEnabled: StateFlow<Boolean> = _isAutoBackupEnabled.asStateFlow()

    private val _isWifiOnlySync = MutableStateFlow(AppPreferencesManager.isWifiOnlySync(application))
    val isWifiOnlySync: StateFlow<Boolean> = _isWifiOnlySync.asStateFlow()

    private val _lastBackupSummary = MutableStateFlow(GoogleDriveBackupManager.getLastBackupSummary(application))
    val lastBackupSummary: StateFlow<String> = _lastBackupSummary.asStateFlow()

    private val _userName = MutableStateFlow(AppPreferencesManager.getUserName(application))
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _defaultCurrency = MutableStateFlow(AppPreferencesManager.getDefaultCurrency(application))
    val defaultCurrency: StateFlow<String> = _defaultCurrency.asStateFlow()

    private val _profilePhotoPath = MutableStateFlow(AppPreferencesManager.getProfilePhotoPath(application))
    val profilePhotoPath: StateFlow<String> = _profilePhotoPath.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    val isAccountConnected: StateFlow<Boolean> = combine(_driveAccountEmail) { emails ->
        emails[0].isNotBlank()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferencesManager.getGoogleAccountEmail(application).isNotBlank())

    // Current active book object for book-wise dashboard statistics (never consolidates)
    val currentBook: StateFlow<LedgerBook?> = combine(allBooks, _selectedBookId) { books, id ->
        if (books.isEmpty()) null
        else {
            val found = books.firstOrNull { it.id == id }
            found ?: books.firstOrNull { !it.isArchived } ?: books.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Raw transactions strictly for the active book (book-wise statistics)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val rawTransactions: StateFlow<List<TransactionRecord>> = currentBook
        .flatMapLatest { book ->
            if (book == null) {
                flowOf(emptyList())
            } else {
                repository.getTransactionsForBook(book.id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered transactions based on search, date, type, category, amount
    val filteredTransactions: StateFlow<List<TransactionRecord>> = combine(
        rawTransactions,
        _filterCriteria
    ) { txList, filter ->
        var list = txList

        // 1. Date Range
        if (filter.dateRange != DateRangeFilter.ALL_TIME) {
            val (start, end) = filter.getTimeRangeMillis()
            list = list.filter { it.timestamp in start..end }
        }

        // 2. Type Filter
        if (filter.typeFilter != null) {
            list = list.filter { it.type == filter.typeFilter }
        }

        // 3. Category Filter
        if (!filter.categoryFilter.isNullOrBlank()) {
            list = list.filter { it.category.equals(filter.categoryFilter, ignoreCase = true) }
        }

        // 4. Amount Range
        if (filter.minAmount != null) {
            list = list.filter { it.amount >= filter.minAmount }
        }
        if (filter.maxAmount != null) {
            list = list.filter { it.amount <= filter.maxAmount }
        }

        // 5. Keyword Search across memo, category, amount
        if (filter.searchQuery.isNotBlank()) {
            val q = filter.searchQuery.trim().lowercase()
            list = list.filter { tx ->
                tx.memo.lowercase().contains(q) ||
                        tx.category.lowercase().contains(q) ||
                        String.format("%.2f", tx.amount).contains(q)
            }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall Net Totals for currently selected book / all books (Unfiltered)
    val bookTotalIncome: StateFlow<Double> = rawTransactions.combine(flowOf(Unit)) { txs, _ ->
        txs.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val bookTotalExpense: StateFlow<Double> = rawTransactions.combine(flowOf(Unit)) { txs, _ ->
        txs.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val bookNetBalance: StateFlow<Double> = combine(bookTotalIncome, bookTotalExpense) { inc, exp ->
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Filtered Period Totals
    val periodTotalIncome: StateFlow<Double> = filteredTransactions.combine(flowOf(Unit)) { txs, _ ->
        txs.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val periodTotalExpense: StateFlow<Double> = filteredTransactions.combine(flowOf(Unit)) { txs, _ ->
        txs.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val periodNetSavings: StateFlow<Double> = combine(periodTotalIncome, periodTotalExpense) { inc, exp ->
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private var lastToastTime = 0L

    init {
        viewModelScope.launch {
            val email = _driveAccountEmail.value.trim().ifBlank {
                AppPreferencesManager.getGoogleAccountEmail(getApplication()).trim().ifBlank {
                    GoogleDriveBackupManager.getConnectedAccount(getApplication())
                }
            }
            val existingBooks = repository.activeBooks.first()

            if (existingBooks.isEmpty()) {
                // If user was previously logged in or app data was cleared, check cloud vault
                val cloudData = CloudStorageManager.fetchCloudData(getApplication(), email)
                if (cloudData.success && cloudData.books.isNotEmpty()) {
                    repository.restoreAllData(cloudData.books, cloudData.transactions)
                    val restoredBooks = repository.activeBooks.first()
                    if (restoredBooks.isNotEmpty()) {
                        _selectedBookId.value = restoredBooks.first().id
                    }
                }
            } else if (existingBooks.isNotEmpty() && _selectedBookId.value == null) {
                _selectedBookId.value = existingBooks.first().id
            }

            // Sync on app open with Toast
            triggerAutoSync(showToast = true)
        }
    }

    fun triggerAutoSync(showToast: Boolean = true, toastMessage: String = "Syncing Data...") {
        viewModelScope.launch(Dispatchers.Main) {
            if (showToast) {
                val now = System.currentTimeMillis()
                if (now - lastToastTime > 500L) {
                    lastToastTime = now
                    try {
                        Toast.makeText(getApplication(), toastMessage, Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {}
                }
            }
            withContext(Dispatchers.IO) {
                _isCloudSyncing.value = true
                val email = _driveAccountEmail.value.trim().ifBlank {
                    AppPreferencesManager.getGoogleAccountEmail(getApplication()).trim().ifBlank {
                        GoogleDriveBackupManager.getConnectedAccount(getApplication())
                    }
                }
                val books = repository.allBooks.first()
                val txs = repository.getAllTransactionsDirect()
                CloudStorageManager.autoSyncToCloud(getApplication(), email, books, txs)
                val summary = AppPreferencesManager.getLastBackupSummary(getApplication())
                _lastBackupSummary.value = summary
                _isCloudSyncing.value = false
            }
        }
    }

    suspend fun triggerAutoSyncDirect(showToast: Boolean = true, toastMessage: String = "Syncing Data..."): Boolean {
        if (showToast) {
            withContext(Dispatchers.Main) {
                val now = System.currentTimeMillis()
                if (now - lastToastTime > 500L) {
                    lastToastTime = now
                    try {
                        Toast.makeText(getApplication(), toastMessage, Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {}
                }
            }
        }
        return withContext(Dispatchers.IO) {
            _isCloudSyncing.value = true
            val email = _driveAccountEmail.value.trim().ifBlank {
                AppPreferencesManager.getGoogleAccountEmail(getApplication()).trim().ifBlank {
                    GoogleDriveBackupManager.getConnectedAccount(getApplication())
                }
            }
            val books = repository.allBooks.first()
            val txs = repository.getAllTransactionsDirect()
            val success = CloudStorageManager.autoSyncToCloud(getApplication(), email, books, txs)
            val summary = AppPreferencesManager.getLastBackupSummary(getApplication())
            _lastBackupSummary.value = summary
            _isCloudSyncing.value = false
            success
        }
    }

    fun connectGoogleAccount(
        email: String,
        name: String,
        onConfirmCloudRestore: ((com.example.data.io.BackupResult) -> Unit)? = null,
        onComplete: (foundCloudData: Boolean, message: String) -> Unit
    ) {
        val cleanEmail = email.trim()
        val cleanName = if (name.isBlank()) {
            cleanEmail.substringBefore("@").replace(".", " ")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        } else name.trim()

        _driveAccountEmail.value = cleanEmail
        _userName.value = cleanName
        AppPreferencesManager.setGoogleAccountEmail(getApplication(), cleanEmail)
        AppPreferencesManager.setUserName(getApplication(), cleanName)
        GoogleDriveBackupManager.setConnectedAccount(getApplication(), cleanEmail)

        viewModelScope.launch {
            _isCloudSyncing.value = true
            val cloudResult = CloudStorageManager.fetchCloudData(getApplication(), cleanEmail)
            _isCloudSyncing.value = false
            val currentBooks = repository.allBooks.first()
            val currentTxs = repository.getAllTransactionsDirect()
            val hasExistingData = currentBooks.isNotEmpty() || currentTxs.isNotEmpty()

            if (cloudResult.success && cloudResult.books.isNotEmpty()) {
                if (hasExistingData && onConfirmCloudRestore != null) {
                    onConfirmCloudRestore(cloudResult)
                    onComplete(true, "Connected $cleanEmail. Found ${cloudResult.books.size} books in Cloud Vault. Please confirm before restoring.")
                } else {
                    repository.restoreAllData(cloudResult.books, cloudResult.transactions)
                    val restoredBooks = repository.activeBooks.first()
                    if (restoredBooks.isNotEmpty()) {
                        _selectedBookId.value = restoredBooks.first().id
                    }
                    _lastBackupSummary.value = "${cloudResult.books.size} Books • ${cloudResult.transactions.size} Records"
                    AppPreferencesManager.setLastBackupSummary(getApplication(), _lastBackupSummary.value)
                    _snackbarMessage.emit("Restored ${cloudResult.books.size} books and ${cloudResult.transactions.size} records from Cloud")
                    onComplete(true, "Found existing records! Restored ${cloudResult.books.size} books and ${cloudResult.transactions.size} records.")
                }
            } else {
                // Brand new user or no prior cloud data: starts clean
                _snackbarMessage.emit("Connected Google account $cleanEmail. Auto-sync active.")
                onComplete(false, "Account connected. All new records will automatically sync to cloud.")
            }
        }
    }

    fun disconnectAccount() {
        viewModelScope.launch {
            AppPreferencesManager.clearUserData(getApplication())
            _driveAccountEmail.value = ""
            _userName.value = ""
            _profilePhotoPath.value = ""
            _lastBackupSummary.value = ""
            _snackbarMessage.emit("Disconnected Google account")
        }
    }

    fun updateProfilePhoto(path: String) {
        _profilePhotoPath.value = path
        AppPreferencesManager.setProfilePhotoPath(getApplication(), path)
        triggerAutoSync()
    }

    fun removeProfilePhoto() {
        _profilePhotoPath.value = ""
        AppPreferencesManager.setProfilePhotoPath(getApplication(), "")
        triggerAutoSync()
    }

    fun selectBook(bookId: Long?) {
        _selectedBookId.value = bookId
    }

    fun createBook(
        name: String,
        colorHex: Long = 0xFF0D9488L,
        iconName: String = "wallet"
    ) {
        viewModelScope.launch {
            val newId = repository.createBook(name, colorHex = colorHex, iconName = iconName)
            _selectedBookId.value = newId
            _snackbarMessage.emit("Created ledger book '$name'")
            triggerAutoSync()
        }
    }

    fun createBookAndImport(
        bookName: String,
        colorHex: Long = 0xFF0D9488L,
        validRows: List<ParsedImportRow>,
        onDone: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val bookId = repository.createBook(
                name = bookName,
                colorHex = colorHex
            )
            val uniqueCategories = validRows.map { it.categoryStr.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            if (uniqueCategories.isNotEmpty()) {
                val created = repository.getBookByIdDirect(bookId)
                if (created != null) {
                    val merged = (CategoryConstants.defaultCategories + uniqueCategories).distinct()
                    repository.updateBook(created.withCategories(merged))
                }
            }

            val records = validRows.mapNotNull { row ->
                if (row.isValid) {
                    TransactionRecord(
                        ledgerBookId = bookId,
                        type = row.type!!,
                        amount = row.amount!!,
                        category = row.categoryStr,
                        timestamp = row.timestamp!!,
                        memo = row.memoStr,
                        paymentMode = row.paymentMode.ifBlank { "Cash" }
                    )
                } else null
            }
            if (records.isNotEmpty()) {
                repository.insertTransactions(records)
            }
            _selectedBookId.value = bookId
            _snackbarMessage.emit("Imported '$bookName' with ${records.size} transactions")
            triggerAutoSync()
            onDone(bookId)
        }
    }

    fun updateBook(book: LedgerBook) {
        viewModelScope.launch {
            repository.updateBook(book)
            _snackbarMessage.emit("Updated ledger book '${book.name}'")
            triggerAutoSync()
        }
    }

    fun archiveBook(bookId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            repository.archiveBook(bookId, isArchived)
            val action = if (isArchived) "Archived" else "Restored"
            _snackbarMessage.emit("$action ledger book")
            if (isArchived && _selectedBookId.value == bookId) {
                val remaining = repository.activeBooks.first()
                if (remaining.isNotEmpty()) {
                    _selectedBookId.value = remaining.first().id
                }
            }
            triggerAutoSync()
        }
    }

    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            repository.deleteBook(bookId)
            _snackbarMessage.emit("Deleted ledger book")
            val remaining = repository.activeBooks.first()
            if (remaining.isNotEmpty()) {
                _selectedBookId.value = remaining.first().id
            } else {
                _selectedBookId.value = null
            }
            triggerAutoSync()
        }
    }

    suspend fun getAllTransactionsDirect(): List<TransactionRecord> {
        return repository.getAllTransactionsDirect()
    }

    fun restoreAllData(books: List<LedgerBook>, transactions: List<TransactionRecord>) {
        viewModelScope.launch {
            repository.restoreAllData(books, transactions)
            _snackbarMessage.emit("Restored ${books.size} books and ${transactions.size} entries!")
            triggerAutoSync()
        }
    }

    fun addTransaction(
        bookId: Long,
        type: TransactionType,
        amount: Double,
        category: String,
        timestamp: Long,
        title: String,
        paymentMode: String = "Cash"
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionRecord(
                    ledgerBookId = bookId,
                    type = type,
                    amount = amount,
                    category = category.trim(),
                    timestamp = timestamp,
                    memo = title.trim(),
                    paymentMode = paymentMode
                )
            )
            _snackbarMessage.emit("Logged ${type.label} (${paymentMode}): ${String.format("%.2f", amount)}")
            triggerAutoSync()
        }
    }

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        timestamp: Long,
        memo: String
    ) {
        val book = currentBook.value ?: allBooks.value.firstOrNull() ?: return
        addTransaction(
            bookId = book.id,
            type = type,
            amount = amount,
            category = category,
            timestamp = timestamp,
            title = memo,
            paymentMode = "Cash"
        )
    }

    fun updateBookCategories(bookId: Long, expenseCats: List<String>, incomeCats: List<String>) {
        viewModelScope.launch {
            val book = allBooks.value.firstOrNull { it.id == bookId } ?: return@launch
            val updated = book.copy(
                customExpenseCategories = expenseCats.distinct().joinToString("||"),
                customIncomeCategories = incomeCats.distinct().joinToString("||")
            )
            repository.updateBook(updated)
            _snackbarMessage.emit("Categories updated for ${book.name}")
            triggerAutoSync()
        }
    }

    fun importCategoriesFromBook(
        targetBookId: Long,
        sourceBookId: Long,
        importExpenses: Boolean = true,
        importIncome: Boolean = true
    ): Pair<Int, Int> {
        val targetBook = allBooks.value.firstOrNull { it.id == targetBookId } ?: return Pair(0, 0)
        val sourceBook = allBooks.value.firstOrNull { it.id == sourceBookId } ?: return Pair(0, 0)

        val curExp = targetBook.getExpenseCategories().toMutableList()
        val curInc = targetBook.getIncomeCategories().toMutableList()
        var added = 0
        var skipped = 0

        if (importExpenses) {
            sourceBook.getExpenseCategories().forEach { srcCat ->
                if (curExp.none { it.equals(srcCat, ignoreCase = true) }) {
                    curExp.add(srcCat)
                    added++
                } else {
                    skipped++
                }
            }
        }

        if (importIncome) {
            sourceBook.getIncomeCategories().forEach { srcCat ->
                if (curInc.none { it.equals(srcCat, ignoreCase = true) }) {
                    curInc.add(srcCat)
                    added++
                } else {
                    skipped++
                }
            }
        }

        viewModelScope.launch {
            repository.updateBook(
                targetBook.copy(
                    customExpenseCategories = curExp.joinToString("||"),
                    customIncomeCategories = curInc.joinToString("||")
                )
            )
            _snackbarMessage.emit("Imported $added categories ($skipped duplicates skipped)")
            triggerAutoSync()
        }
        return Pair(added, skipped)
    }

    fun updateTransaction(transaction: TransactionRecord) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            _snackbarMessage.emit("Transaction updated")
            triggerAutoSync()
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
            _snackbarMessage.emit("Transaction deleted")
            triggerAutoSync()
        }
    }

    fun duplicateTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.duplicateTransaction(transactionId)
            _snackbarMessage.emit("Transaction duplicated")
            triggerAutoSync()
        }
    }

    fun commitImportedRows(validRows: List<ParsedImportRow>, targetBookId: Long) {
        viewModelScope.launch {
            val records = validRows.mapNotNull { row ->
                if (row.isValid) {
                    TransactionRecord(
                        ledgerBookId = targetBookId,
                        type = row.type!!,
                        amount = row.amount!!,
                        category = row.categoryStr,
                        timestamp = row.timestamp!!,
                        memo = row.memoStr,
                        paymentMode = row.paymentMode.ifBlank { "Cash" }
                    )
                } else null
            }
            if (records.isNotEmpty()) {
                repository.insertTransactions(records)
                _snackbarMessage.emit("Successfully imported ${records.size} transactions!")
                triggerAutoSync()
            }
        }
    }

    // Filter controls
    fun setSearchQuery(query: String) {
        _filterCriteria.value = _filterCriteria.value.copy(searchQuery = query)
    }

    fun setDateRange(range: DateRangeFilter, start: Long? = null, end: Long? = null) {
        _filterCriteria.value = _filterCriteria.value.copy(
            dateRange = range,
            customStartDate = start,
            customEndDate = end
        )
    }

    fun setTypeFilter(type: TransactionType?) {
        _filterCriteria.value = _filterCriteria.value.copy(typeFilter = type)
    }

    fun setCategoryFilter(category: String?) {
        _filterCriteria.value = _filterCriteria.value.copy(categoryFilter = category)
    }

    fun setAmountRange(min: Double?, max: Double?) {
        _filterCriteria.value = _filterCriteria.value.copy(minAmount = min, maxAmount = max)
    }

    fun clearFilters() {
        _filterCriteria.value = FilterCriteria()
    }

    // Analytics Helpers
    fun getCategoryBreakdown(type: TransactionType): List<CategorySummary> {
        val txs = filteredTransactions.value.filter { it.type == type }
        val total = txs.sumOf { it.amount }
        if (total == 0.0) return emptyList()

        return txs.groupBy { it.category }
            .map { (cat, list) ->
                val sum = list.sumOf { it.amount }
                CategorySummary(
                    category = cat,
                    amount = sum,
                    percentage = ((sum / total) * 100).toFloat(),
                    count = list.size
                )
            }
            .sortedByDescending { it.amount }
    }

    fun getSpendingTrends(): List<TrendPoint> {
        val txs = filteredTransactions.value.sortedBy { it.timestamp }
        if (txs.isEmpty()) return emptyList()

        // Group by day format: "MM/dd"
        val cal = Calendar.getInstance()
        val groups = linkedMapOf<String, Pair<Double, Double>>() // key -> (income, expense)
        val timestamps = linkedMapOf<String, Long>()

        txs.forEach { tx ->
            cal.timeInMillis = tx.timestamp
            val key = "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}"
            val current = groups.getOrDefault(key, Pair(0.0, 0.0))
            if (tx.type == TransactionType.CASH_IN) {
                groups[key] = Pair(current.first + tx.amount, current.second)
            } else {
                groups[key] = Pair(current.first, current.second + tx.amount)
            }
            if (!timestamps.containsKey(key)) {
                timestamps[key] = tx.timestamp
            }
        }

        return groups.map { (key, pair) ->
            TrendPoint(
                label = key,
                timestamp = timestamps[key] ?: 0L,
                income = pair.first,
                expense = pair.second
            )
        }
    }

    suspend fun exportCurrentViewToCsv(context: Context): File {
        val txs = filteredTransactions.value
        val book = currentBook.value
        val bookName = book?.name ?: "All_Ledgers"
        val csv = CsvExporterImporter.exportTransactionsToCsv(
            transactions = txs,
            bookName = bookName
        )
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "${bookName.replace(" ", "_")}_${System.currentTimeMillis()}.csv")
        file.writeText(csv)
        return file
    }

    suspend fun exportCurrentViewToPdf(context: Context): File {
        val txs = filteredTransactions.value
        val book = currentBook.value ?: LedgerBook(id = 0, name = "All Ledgers Consolidated", colorHex = 0xFF0D9488L)
        val filterLabel = filterCriteria.value.dateRange.label
        return PdfReportGenerator.generateFinancialStatement(
            context = context,
            book = book,
            transactions = txs,
            timeframeLabel = filterLabel
        )
    }

    suspend fun exportBookToPdf(context: Context, bookId: Long): File? {
        val book = repository.getBookByIdDirect(bookId) ?: return null
        val txs = repository.getTransactionsForBookDirect(bookId)
        return PdfReportGenerator.generateFinancialStatement(
            context = context,
            book = book,
            transactions = txs,
            timeframeLabel = "All Time"
        )
    }

    suspend fun exportBookToCsv(context: Context, bookId: Long): File? {
        val book = repository.getBookByIdDirect(bookId) ?: return null
        val txs = repository.getTransactionsForBookDirect(bookId)
        val csv = CsvExporterImporter.exportTransactionsToCsv(
            transactions = txs,
            bookName = book.name
        )
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "${book.name.replace(" ", "_")}_${System.currentTimeMillis()}.csv")
        file.writeText(csv)
        return file
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        AppPreferencesManager.setThemeMode(getApplication(), mode)
        triggerAutoSync()
    }

    fun setModernPalette(palette: ModernPalette) {
        _modernPalette.value = palette
        AppPreferencesManager.setModernPalette(getApplication(), palette)
        triggerAutoSync()
    }

    fun setDriveAccountEmail(email: String) {
        _driveAccountEmail.value = email
        AppPreferencesManager.setGoogleAccountEmail(getApplication(), email)
        GoogleDriveBackupManager.setConnectedAccount(getApplication(), email)
        triggerAutoSync()
    }

    fun setDriveConnected(connected: Boolean) {
        _isDriveConnected.value = connected
        AppPreferencesManager.setGoogleDriveConnected(getApplication(), connected)
        triggerAutoSync()
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        _isAutoBackupEnabled.value = enabled
        AppPreferencesManager.setAutoBackupEnabled(getApplication(), enabled)
        triggerAutoSync()
    }

    fun setWifiOnlySync(enabled: Boolean) {
        _isWifiOnlySync.value = enabled
        AppPreferencesManager.setWifiOnlySync(getApplication(), enabled)
        triggerAutoSync()
    }

    fun setUserName(name: String) {
        _userName.value = name
        AppPreferencesManager.setUserName(getApplication(), name)
        triggerAutoSync()
    }

    fun setDefaultCurrency(currency: String) {
        _defaultCurrency.value = currency
        AppPreferencesManager.setDefaultCurrency(getApplication(), currency)
        triggerAutoSync()
    }

    fun setGoogleDriveFolderName(folderName: String) {
        val clean = folderName.trim().ifBlank { AppPreferencesManager.DEFAULT_DRIVE_FOLDER }
        AppPreferencesManager.setGoogleDriveFolderName(getApplication(), clean)
        _googleDriveFolderName.value = clean
        triggerAutoSync(showToast = true, toastMessage = "Google Drive folder set to '$clean'")
    }

    suspend fun performGoogleDriveBackup(
        context: Context,
        folderName: String = _googleDriveFolderName.value
    ): BackupResult {
        val books = allBooks.value
        val txs = allTransactions.value
        val result = GoogleDriveBackupManager.backupToGoogleDrive(context, books, txs, folderName)
        if (result.success) {
            val summary = GoogleDriveBackupManager.getLastBackupSummary(context)
            _lastBackupSummary.value = summary
            AppPreferencesManager.setLastBackupSummary(context, summary)
            AppPreferencesManager.setLastBackupTime(context, System.currentTimeMillis())
            _snackbarMessage.emit("Backup saved to Google Drive folder '$folderName'")
        } else {
            _snackbarMessage.emit("Backup failed: ${result.message}")
        }
        return result
    }

    suspend fun performGoogleDriveRestore(
        context: Context,
        folderName: String = _googleDriveFolderName.value
    ): BackupResult {
        val result = GoogleDriveBackupManager.restoreFromGoogleDrive(context, folderName)
        if (result.success && result.books.isNotEmpty()) {
            restoreAllData(result.books, result.transactions)
            _snackbarMessage.emit("Restored ${result.books.size} books and ${result.transactions.size} records from '$folderName'")
        } else if (!result.success) {
            _snackbarMessage.emit(result.message)
        }
        return result
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application)
                    val repo = LedgerRepository(db.ledgerBookDao(), db.transactionDao())
                    return LedgerViewModel(application, repo) as T
                }
            }
        }
    }
}
