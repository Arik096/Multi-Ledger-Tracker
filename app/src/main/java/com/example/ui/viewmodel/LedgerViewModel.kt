package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthResult
import com.example.data.auth.GoogleAuthManager
import com.example.data.cloud.CloudStorageManager
import com.example.data.db.AppDatabase
import com.example.data.firestore.FirestoreRepository
import com.example.data.firestore.FirestoreSyncService
import com.example.data.firestore.SyncStatus
import com.example.data.io.BackupResult
import com.example.data.io.CsvExporterImporter
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
import com.example.util.AmountFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    private val repository: LedgerRepository,
    private val authManager: GoogleAuthManager = GoogleAuthManager(application),
    val syncService: FirestoreSyncService = FirestoreSyncService(
        firestoreRepository = FirestoreRepository(application),
        ledgerRepository = repository,
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    )
) : AndroidViewModel(application) {

    val syncStatus: StateFlow<SyncStatus> = syncService.syncStatus

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

    private val _dynamicColor = MutableStateFlow(AppPreferencesManager.isDynamicColor(application))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _amountPrecision = MutableStateFlow(AppPreferencesManager.getAmountPrecision(application))
    val amountPrecision: StateFlow<Int> = _amountPrecision.asStateFlow()

    private val _driveAccountEmail = MutableStateFlow(AppPreferencesManager.getGoogleAccountEmail(application))
    val driveAccountEmail: StateFlow<String> = _driveAccountEmail.asStateFlow()

    private val _isAutoBackupEnabled = MutableStateFlow(AppPreferencesManager.isAutoBackupEnabled(application))
    val isAutoBackupEnabled: StateFlow<Boolean> = _isAutoBackupEnabled.asStateFlow()

    private val _isWifiOnlySync = MutableStateFlow(AppPreferencesManager.isWifiOnlySync(application))
    val isWifiOnlySync: StateFlow<Boolean> = _isWifiOnlySync.asStateFlow()

    private val _lastBackupSummary = MutableStateFlow(AppPreferencesManager.getLastBackupSummary(application))
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

    fun formatAmount(amount: Double, includeCommas: Boolean = false): String {
        return AmountFormatter.format(amount, _amountPrecision.value, includeCommas)
    }

    fun formatAmountWithCurrency(symbol: String, amount: Double, includeCommas: Boolean = false): String {
        return AmountFormatter.formatWithCurrency(symbol, amount, _amountPrecision.value, includeCommas)
    }

    init {
        viewModelScope.launch {
            authManager.authStateFlow().collectLatest { user ->
                syncService.onUserAuthenticated(user)
                if (user != null) {
                    val email = user.email ?: ""
                    _driveAccountEmail.value = email
                    AppPreferencesManager.setGoogleAccountEmail(getApplication(), email)
                    val name = user.displayName ?: email.substringBefore("@")
                    _userName.value = name
                    AppPreferencesManager.setUserName(getApplication(), name)
                    val photo = user.photoUrl?.toString() ?: ""
                    _profilePhotoPath.value = photo
                    AppPreferencesManager.setProfilePhotoPath(getApplication(), photo)
                }
            }
        }
    }

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

    fun triggerAutoSync(showToast: Boolean = false, toastMessage: String = "Syncing...") {
        viewModelScope.launch {
            val user = authManager.currentUser
            if (user != null) {
                _isCloudSyncing.value = true
                val success = syncService.triggerManualSync()
                _isCloudSyncing.value = false
                if (success) {
                    val summary = "${allBooks.value.size} Books • ${allTransactions.value.size} Records"
                    _lastBackupSummary.value = summary
                    AppPreferencesManager.setLastBackupSummary(getApplication(), summary)
                    AppPreferencesManager.setLastBackupTime(getApplication(), System.currentTimeMillis())
                    if (showToast) _snackbarMessage.emit("Synced to Cloud")
                }
            } else {
                authManager.trySilentSignIn()
            }
        }
    }

    suspend fun triggerAutoSyncDirect(showToast: Boolean = true, toastMessage: String = "Syncing..."): Boolean {
        return withContext(Dispatchers.IO) {
            val user = authManager.currentUser
            if (user != null) {
                _isCloudSyncing.value = true
                val success = syncService.triggerManualSync()
                _isCloudSyncing.value = false
                if (success) {
                    val summary = "${allBooks.value.size} Books • ${allTransactions.value.size} Records"
                    _lastBackupSummary.value = summary
                    AppPreferencesManager.setLastBackupSummary(getApplication(), summary)
                    AppPreferencesManager.setLastBackupTime(getApplication(), System.currentTimeMillis())
                }
                success
            } else {
                false
            }
        }
    }

    fun signInWithGoogle(onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            when (val result = authManager.signInWithGoogle()) {
                is AuthResult.Success -> {
                    val email = result.user.email ?: ""
                    _driveAccountEmail.value = email
                    AppPreferencesManager.setGoogleAccountEmail(getApplication(), email)
                    val name = result.user.displayName ?: email.substringBefore("@")
                    _userName.value = name
                    AppPreferencesManager.setUserName(getApplication(), name)
                    val photo = result.user.photoUrl?.toString() ?: ""
                    _profilePhotoPath.value = photo
                    AppPreferencesManager.setProfilePhotoPath(getApplication(), photo)

                    // Look for previous cloud data and restore if available, else start clean
                    val (foundData, restoreMsg) = syncService.checkAndRestorePreviousData(result.user.uid)
                    _isCloudSyncing.value = false
                    _snackbarMessage.emit(restoreMsg)
                    onComplete?.invoke(true, restoreMsg)
                }
                is AuthResult.Cancelled -> {
                    _isCloudSyncing.value = false
                    onComplete?.invoke(false, "Sign-in cancelled.")
                }
                is AuthResult.Error -> {
                    _isCloudSyncing.value = false
                    _snackbarMessage.emit("Sign-in failed: ${result.message}")
                    onComplete?.invoke(false, "Error: ${result.message}")
                }
            }
        }
    }

    fun connectGoogleAccount(
        email: String,
        name: String,
        onConfirmCloudRestore: ((com.example.data.io.BackupResult) -> Unit)? = null,
        onComplete: (foundCloudData: Boolean, message: String) -> Unit
    ) {
        signInWithGoogle { success, msg ->
            onComplete(success, msg)
        }
    }

    fun disconnectAccount() {
        viewModelScope.launch {
            authManager.signOut()
            AppPreferencesManager.clearUserData(getApplication())
            _driveAccountEmail.value = ""
            _userName.value = ""
            _profilePhotoPath.value = ""
            _lastBackupSummary.value = ""
            _snackbarMessage.emit("Signed out of Google account")
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
            val created = repository.getBookByIdDirect(newId)
            if (created != null) {
                syncService.onLocalBookSaved(created)
            }
            _snackbarMessage.emit("Created '$name'")
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
            val created = repository.getBookByIdDirect(bookId)
            if (created != null) {
                syncService.onLocalBookSaved(created)
            }
            _snackbarMessage.emit("Imported '$bookName' (${records.size} entries)")
            triggerAutoSync()
            onDone(bookId)
        }
    }

    fun updateBook(book: LedgerBook) {
        viewModelScope.launch {
            repository.updateBook(book)
            syncService.onLocalBookSaved(book)
            _snackbarMessage.emit("Updated '${book.name}'")
            triggerAutoSync()
        }
    }

    fun archiveBook(bookId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            repository.archiveBook(bookId, isArchived)
            val action = if (isArchived) "Archived" else "Restored"
            val updated = repository.getBookByIdDirect(bookId)
            if (updated != null) {
                syncService.onLocalBookSaved(updated)
            }
            _snackbarMessage.emit("$action book")
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
            syncService.onLocalBookDeleted(bookId)
            _snackbarMessage.emit("Deleted book")
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

    fun restoreFromBackupResult(result: BackupResult) {
        restoreAllData(result.books, result.transactions)
    }

    fun restoreAllData(books: List<LedgerBook>, transactions: List<TransactionRecord>) {
        viewModelScope.launch {
            repository.restoreAllData(books, transactions)
            _snackbarMessage.emit("Restored ${books.size} books, ${transactions.size} entries")
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
            val record = TransactionRecord(
                ledgerBookId = bookId,
                type = type,
                amount = amount,
                category = category.trim(),
                timestamp = timestamp,
                memo = title.trim(),
                paymentMode = paymentMode
            )
            val id = repository.insertTransaction(record)
            syncService.onLocalTransactionSaved(record.copy(id = id))
            _snackbarMessage.emit("Logged ${type.label}: ${String.format("%.2f", amount)}")
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
            syncService.onLocalBookSaved(updated)
            _snackbarMessage.emit("Categories updated")
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
            val updated = targetBook.copy(
                customExpenseCategories = curExp.joinToString("||"),
                customIncomeCategories = curInc.joinToString("||")
            )
            repository.updateBook(updated)
            syncService.onLocalBookSaved(updated)
            _snackbarMessage.emit("Imported $added categories")
            triggerAutoSync()
        }
        return Pair(added, skipped)
    }

    fun updateTransaction(transaction: TransactionRecord) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            syncService.onLocalTransactionSaved(transaction)
            _snackbarMessage.emit("Updated entry")
            triggerAutoSync()
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
            syncService.onLocalTransactionDeleted(transactionId)
            _snackbarMessage.emit("Deleted entry")
            triggerAutoSync()
        }
    }

    fun duplicateTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.duplicateTransaction(transactionId)
            _snackbarMessage.emit("Duplicated entry")
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

    fun setAmountPrecision(precision: Int) {
        val safe = precision.coerceIn(0, 4)
        _amountPrecision.value = safe
        AppPreferencesManager.setAmountPrecision(getApplication(), safe)
        triggerAutoSync()
    }

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        AppPreferencesManager.setDynamicColor(getApplication(), enabled)
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

    fun setDefaultCurrency(currency: String) {
        _defaultCurrency.value = currency
        AppPreferencesManager.setDefaultCurrency(getApplication(), currency)
        triggerAutoSync()
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
