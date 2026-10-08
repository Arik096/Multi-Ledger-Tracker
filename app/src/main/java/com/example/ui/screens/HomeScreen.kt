package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DateRangeFilter
import com.example.data.model.LedgerBook
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.components.AdvancedFilterDialog
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.CategoryExpensesBreakdownCard
import com.example.ui.components.NetBalanceHeaderCard
import com.example.ui.components.PeriodSummaryCard
import com.example.ui.components.SwitchBookBottomSheet
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import com.example.ui.viewmodel.LedgerViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: LedgerViewModel,
    onNavigateToBooks: () -> Unit,
    onEditTransaction: (TransactionRecord) -> Unit,
    onOpenDriveBackup: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    onBookSwitched: ((LedgerBook) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentBook by viewModel.currentBook.collectAsStateWithLifecycle()
    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val selectedBookId by viewModel.selectedBookId.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val filterCriteria by viewModel.filterCriteria.collectAsStateWithLifecycle()

    val periodTotalIncome by viewModel.periodTotalIncome.collectAsStateWithLifecycle()
    val periodTotalExpense by viewModel.periodTotalExpense.collectAsStateWithLifecycle()
    val periodNetSavings by viewModel.periodNetSavings.collectAsStateWithLifecycle()

    var showBookSwitchSheet by remember { mutableStateOf(false) }
    var showAdvancedFilterDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    // Support navigation gesture back without closing app
    BackHandler(enabled = showAdvancedFilterDialog) {
        showAdvancedFilterDialog = false
    }

    val isCurrentBookArchived = currentBook?.isArchived == true

    // Category breakdown for Cash Out
    val cashOutCategories = remember(filteredTransactions) {
        viewModel.getCategoryBreakdown(TransactionType.CASH_OUT)
    }

    // Cash Out breakdown by Transaction Type / Payment Mode
    val cashOutByPaymentMode = remember(filteredTransactions, periodTotalExpense) {
        val cashOutTx = filteredTransactions.filter { it.type == TransactionType.CASH_OUT }
        val total = if (periodTotalExpense > 0) periodTotalExpense else cashOutTx.sumOf { it.amount }
        cashOutTx
            .groupBy { it.paymentMode.ifBlank { "Cash" } }
            .map { (mode, txs) ->
                val sum = txs.sumOf { it.amount }
                val pct = if (total > 0) (sum / total) * 100.0 else 0.0
                TransactionTypeBreakdown(
                    modeName = mode,
                    totalAmount = sum,
                    count = txs.size,
                    percentage = pct
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    // Date-wise Cash Out breakdown for the Book
    val dateWiseCashOutList = remember(filteredTransactions, periodTotalExpense) {
        val cashOutTx = filteredTransactions.filter { it.type == TransactionType.CASH_OUT }
        val total = if (periodTotalExpense > 0) periodTotalExpense else cashOutTx.sumOf { it.amount }
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val shortDisplayFormat = SimpleDateFormat("dd MMM", Locale.US)
        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.US)

        val calNow = Calendar.getInstance()
        val todayKey = keyFormat.format(calNow.time)
        calNow.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayKey = keyFormat.format(calNow.time)

        cashOutTx
            .groupBy { keyFormat.format(Date(it.timestamp)) }
            .map { (dateKey, txs) ->
                val sum = txs.sumOf { it.amount }
                val pct = if (total > 0) (sum / total) * 100.0 else 0.0
                val dateObj = runCatching { keyFormat.parse(dateKey) }.getOrNull() ?: Date(txs.first().timestamp)
                DateWiseCashOutSummary(
                    dateKey = dateKey,
                    displayDate = displayFormat.format(dateObj),
                    shortDisplayDate = shortDisplayFormat.format(dateObj),
                    dayOfWeek = dayOfWeekFormat.format(dateObj),
                    isToday = dateKey == todayKey,
                    isYesterday = dateKey == yesterdayKey,
                    totalCashOut = sum,
                    transactionCount = txs.size,
                    percentageOfTotal = pct,
                    transactions = txs.sortedByDescending { it.timestamp }
                )
            }
            .sortedByDescending { it.dateKey }
    }

    fun shareExportedFile(file: File, mimeType: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Exported Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export saved: ${file.name}", Toast.LENGTH_LONG).show()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // 1. Top App Bar Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (onNavigateBack != null) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showBookSwitchSheet = true }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                                .testTag("dashboard_book_switch_header")
                        ) {
                            Column {
                                Text(
                                    text = currentBook?.name ?: "Select Book",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${currentBook?.currencyCode ?: "BDT"} • Tap to switch book",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Switch book",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Cloud Sync & Profile Icon Button
                        IconButton(
                            onClick = onOpenDriveBackup,
                            modifier = Modifier.testTag("home_drive_backup_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Cloud Sync & Profile",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Export Button
                        Box {
                            IconButton(
                                onClick = { showExportMenu = true },
                                modifier = Modifier.testTag("export_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Export Report",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Export to CSV") },
                                    leadingIcon = {
                                        Icon(Icons.Default.TableChart, contentDescription = null)
                                    },
                                    onClick = {
                                        showExportMenu = false
                                        scope.launch {
                                            val file = viewModel.exportCurrentViewToCsv(context)
                                            if (file != null) {
                                                shareExportedFile(file, "text/csv")
                                            } else {
                                                Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Export PDF Statement") },
                                    leadingIcon = {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                    },
                                    onClick = {
                                        showExportMenu = false
                                        scope.launch {
                                            val file = viewModel.exportCurrentViewToPdf(context)
                                            if (file != null) {
                                                shareExportedFile(file, "application/pdf")
                                            } else {
                                                Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Archived Book Banner (User requirement: no entry can be added into archived books)
            if (isCurrentBookArchived) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Archived Book",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Archived. Read-only mode.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // 3. Date Filter Chips Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        DateRangeFilter.THIS_MONTH,
                        DateRangeFilter.THIS_WEEK,
                        DateRangeFilter.TODAY,
                        DateRangeFilter.YEAR_TO_DATE,
                        DateRangeFilter.ALL_TIME
                    ).forEach { range ->
                        val isSelected = filterCriteria.dateRange == range
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setDateRange(range) },
                            label = { Text(range.label, fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("date_filter_${range.name}")
                        )
                    }

                    FilterChip(
                        selected = filterCriteria.dateRange == DateRangeFilter.CUSTOM || filterCriteria.hasActiveFilters(),
                        onClick = { showAdvancedFilterDialog = true },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Custom Filters",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Filter", fontSize = 12.sp)
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // 4. Net Balance Header Card (Cash In and Cash Out only)
            item {
                NetBalanceHeaderCard(
                    book = currentBook,
                    netBalance = periodNetSavings,
                    totalIncome = periodTotalIncome,
                    totalExpense = periodTotalExpense,
                    onSwitchBookClick = { showBookSwitchSheet = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 5. Cash Out by Categories Breakdown Card (User requirement: only categories imported, no cash in by category)
            item {
                CategoryExpensesBreakdownCard(
                    categories = cashOutCategories,
                    totalExpense = periodTotalExpense,
                    filterDescription = "${currentBook?.name ?: "Current Book"} • ${filterCriteria.dateRange.label}",
                    currencySymbol = currentBook?.currencySymbol ?: "৳",
                    onCategoryClick = { categoryName ->
                        viewModel.setCategoryFilter(categoryName)
                    }
                )
            }

            // 6. Cash Out by Transaction Type / Payment Mode Card
            item {
                CashOutByTransactionTypeCard(
                    paymentModes = cashOutByPaymentMode,
                    totalExpense = periodTotalExpense,
                    currencySymbol = currentBook?.currencySymbol ?: "৳"
                )
            }

            // 7. Date-wise Cash Out for the Book (Daily expense breakdown)
            item {
                DateWiseCashOutCard(
                    dateSummaries = dateWiseCashOutList,
                    totalExpense = periodTotalExpense,
                    currencySymbol = currentBook?.currencySymbol ?: "৳"
                )
            }
        }
    }

    // Advanced Multi-Criteria Filter Dialog
    if (showAdvancedFilterDialog) {
        val categories = remember(filteredTransactions) {
            filteredTransactions.map { it.category }.distinct()
        }
        AdvancedFilterDialog(
            currentCriteria = filterCriteria,
            availableCategories = categories,
            onDismiss = { showAdvancedFilterDialog = false },
            onApply = { newCriteria ->
                if (newCriteria.dateRange != filterCriteria.dateRange) {
                    viewModel.setDateRange(newCriteria.dateRange, newCriteria.customStartDate, newCriteria.customEndDate)
                }
                viewModel.setTypeFilter(newCriteria.typeFilter)
                viewModel.setCategoryFilter(newCriteria.categoryFilter)
                viewModel.setAmountRange(newCriteria.minAmount, newCriteria.maxAmount)
                showAdvancedFilterDialog = false
            }
        )
    }

    // Switch Book Bottom Sheet (updates dashboard information in-place, without navigating into the book)
    if (showBookSwitchSheet) {
        SwitchBookBottomSheet(
            allBooks = allBooks,
            selectedBookId = selectedBookId,
            allTransactions = allTransactions,
            onSelectBook = { selectedBook ->
                viewModel.selectBook(selectedBook.id)
                onBookSwitched?.invoke(selectedBook)
            },
            onDismiss = { showBookSwitchSheet = false },
            onManageBooksClick = onNavigateToBooks
        )
    }
}

data class TransactionTypeBreakdown(
    val modeName: String,
    val totalAmount: Double,
    val count: Int,
    val percentage: Double
)

@Composable
fun CashOutByTransactionTypeCard(
    paymentModes: List<TransactionTypeBreakdown>,
    totalExpense: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = "Cash Out by Mode",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${paymentModes.size} transaction mode${if (paymentModes.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Total Cash Out",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "$currencySymbol${String.format(java.util.Locale.US, "%,.2f", totalExpense)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = CashOutRed,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (paymentModes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No cash out transactions recorded for this period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    paymentModes.forEach { mode ->
                        val icon = when {
                            mode.modeName.contains("cash", ignoreCase = true) -> Icons.Default.Payments
                            mode.modeName.contains("bank", ignoreCase = true) || mode.modeName.contains("transfer", ignoreCase = true) -> Icons.Default.AccountBalance
                            mode.modeName.contains("card", ignoreCase = true) || mode.modeName.contains("credit", ignoreCase = true) || mode.modeName.contains("debit", ignoreCase = true) -> Icons.Default.CreditCard
                            mode.modeName.contains("online", ignoreCase = true) || mode.modeName.contains("upi", ignoreCase = true) || mode.modeName.contains("mobile", ignoreCase = true) -> Icons.Default.PhoneAndroid
                            mode.modeName.contains("cheque", ignoreCase = true) || mode.modeName.contains("check", ignoreCase = true) -> Icons.Default.ReceiptLong
                            else -> Icons.Default.LocalAtm
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = mode.modeName,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = mode.modeName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${mode.count} transaction${if (mode.count > 1) "s" else ""}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$currencySymbol${String.format(java.util.Locale.US, "%,.2f", mode.totalAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = CashOutRed,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CashOutRed.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "${String.format(java.util.Locale.US, "%.1f", mode.percentage)}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CashOutRed,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            LinearProgressIndicator(
                                progress = { (mode.percentage / 100f).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = CashOutRed,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

data class DateWiseCashOutSummary(
    val dateKey: String,
    val displayDate: String,
    val shortDisplayDate: String,
    val dayOfWeek: String,
    val isToday: Boolean,
    val isYesterday: Boolean,
    val totalCashOut: Double,
    val transactionCount: Int,
    val percentageOfTotal: Double,
    val transactions: List<TransactionRecord>
)

@Composable
fun DateWiseCashOutCard(
    dateSummaries: List<DateWiseCashOutSummary>,
    totalExpense: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    var expandedDateKey by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("date_wise_cash_out_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = "Date-wise Cash Out",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${dateSummaries.size} active spending day${if (dateSummaries.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Total Cash Out",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.2f", totalExpense)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = CashOutRed,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (dateSummaries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No cash out recorded for this period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.US) }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    dateSummaries.forEach { item ->
                        val isExpanded = expandedDateKey == item.dateKey

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    expandedDateKey = if (isExpanded) null else item.dateKey
                                }
                                .testTag("date_spend_row_${item.dateKey}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left: Calendar icon badge + Date info
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (item.isToday) CashOutRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarToday,
                                                    contentDescription = null,
                                                    tint = if (item.isToday) CashOutRed else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f, fill = false)) {
                                            val dateTitle = when {
                                                item.isToday -> "Today (${item.shortDisplayDate})"
                                                item.isYesterday -> "Yesterday (${item.shortDisplayDate})"
                                                else -> "${item.shortDisplayDate}, ${item.dayOfWeek.take(3)}"
                                            }
                                            Text(
                                                text = dateTitle,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.transactionCount} expense${if (item.transactionCount > 1) "s" else ""}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Right: Amount + Percentage Badge + Expand Arrow
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "$currencySymbol${String.format(Locale.US, "%,.2f", item.totalCashOut)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = CashOutRed,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = CashOutRed.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = "${String.format(Locale.US, "%.1f", item.percentageOfTotal)}%",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CashOutRed,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Proportion indicator bar
                                LinearProgressIndicator(
                                    progress = { (item.percentageOfTotal / 100f).toFloat().coerceIn(0.01f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = CashOutRed,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )

                                // Expanded Itemized Breakdown for this Date
                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier.padding(top = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        Text(
                                            text = "SPENT ON THIS DAY",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        item.transactions.forEach { tx ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant
                                                    ) {
                                                        Text(
                                                            text = tx.paymentMode.ifBlank { "Cash" },
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                                        Text(
                                                            text = if (tx.memo.isNotBlank()) tx.memo else tx.category,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = tx.category,
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Text(
                                                                text = " • ${timeFormat.format(Date(tx.timestamp))}",
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "-$currencySymbol${String.format(Locale.US, "%,.2f", tx.amount)}",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CashOutRed
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
