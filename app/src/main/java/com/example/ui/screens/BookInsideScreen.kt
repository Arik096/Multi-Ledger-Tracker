package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LedgerBook
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import com.example.ui.viewmodel.LedgerViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class BookDateQuickFilter(val label: String) {
    ALL("Select Date"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month")
}

/**
 * Simplified inside-the-book view replicating the clean, dark ledger design.
 * Features:
 * - Direct back navigation to Books list
 * - Search by remark or amount
 * - Date and Entry Type filters
 * - Summary Card: Net Balance, Total In (+), Total Out (-), with "View Reports >"
 * - Entries grouped by date with category/mode badges and running balances
 * - Split bottom buttons: [+ Cash In] (green) and [- Cash Out] (red) with mic action
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookInsideScreen(
    book: LedgerBook,
    viewModel: LedgerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToCashIn: () -> Unit,
    onNavigateToCashOut: () -> Unit,
    onEditTransaction: (TransactionRecord) -> Unit,
    onOpenBookSettings: () -> Unit,
    onOpenDriveBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedDateFilter by remember { mutableStateOf(BookDateQuickFilter.ALL) }
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) } // null = All
    var showDateFilterMenu by remember { mutableStateOf(false) }
    var showTypeFilterMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<TransactionRecord?>(null) }

    // Intercept back gesture to return cleanly to Books list
    BackHandler {
        if (searchQuery.isNotBlank()) {
            searchQuery = ""
        } else if (selectedDateFilter != BookDateQuickFilter.ALL || selectedTypeFilter != null) {
            selectedDateFilter = BookDateQuickFilter.ALL
            selectedTypeFilter = null
        } else {
            onNavigateBack()
        }
    }

    // 1. All transactions belonging to this book
    val bookTransactions = remember(allTransactions, book.id) {
        allTransactions.filter { it.ledgerBookId == book.id }
    }

    // 2. Compute Running Balances chronologically (oldest to newest)
    val runningBalanceMap = remember(bookTransactions) {
        val sortedAscending = bookTransactions.sortedBy { it.timestamp }
        var currentRunning = 0.0
        val map = mutableMapOf<Long, Double>()
        for (tx in sortedAscending) {
            if (tx.type == TransactionType.CASH_IN) {
                currentRunning += tx.amount
            } else {
                currentRunning -= tx.amount
            }
            map[tx.id] = currentRunning
        }
        map
    }

    // 3. Totals for the book
    val totalIn = remember(bookTransactions) {
        bookTransactions.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount }
    }
    val totalOut = remember(bookTransactions) {
        bookTransactions.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount }
    }
    val netBalance = totalIn - totalOut

    // 4. Filtered transactions for the display list
    val displayedTransactions = remember(bookTransactions, searchQuery, selectedDateFilter, selectedTypeFilter) {
        val nowCal = Calendar.getInstance()
        val todayYear = nowCal.get(Calendar.YEAR)
        val todayDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)

        bookTransactions.filter { tx ->
            // Type filter
            val matchesType = selectedTypeFilter == null || tx.type == selectedTypeFilter

            // Search query filter (memo or amount)
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                tx.memo.lowercase().contains(q) ||
                        tx.category.lowercase().contains(q) ||
                        tx.amount.toString().contains(q) ||
                        (tx.paymentMode?.lowercase()?.contains(q) == true)
            }

            // Date filter
            val matchesDate = when (selectedDateFilter) {
                BookDateQuickFilter.ALL -> true
                BookDateQuickFilter.TODAY -> {
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                    txCal.get(Calendar.YEAR) == todayYear && txCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear
                }
                BookDateQuickFilter.YESTERDAY -> {
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                    txCal.get(Calendar.YEAR) == todayYear && (todayDayOfYear - txCal.get(Calendar.DAY_OF_YEAR) == 1)
                }
                BookDateQuickFilter.THIS_WEEK -> {
                    val weekCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
                    tx.timestamp >= weekCal.timeInMillis
                }
                BookDateQuickFilter.THIS_MONTH -> {
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                    txCal.get(Calendar.YEAR) == todayYear && txCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH)
                }
            }

            matchesType && matchesSearch && matchesDate
        }.sortedByDescending { it.timestamp }
    }

    // 5. Group displayed transactions by formatted date string (e.g. "01 September 2026")
    val groupedByDate = remember(displayedTransactions) {
        val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.US)
        displayedTransactions.groupBy { tx ->
            dateFormatter.format(Date(tx.timestamp))
        }
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
            context.startActivity(Intent.createChooser(shareIntent, "Share ${book.name} Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "Saved file: ${file.name}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("book_inside_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = book.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Activity & Members",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("book_inside_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Books"
                        )
                    }
                },
                actions = {
                    // Member icon
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Members management for ${book.name}", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Member",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // PDF Button
                    Surface(
                        onClick = {
                            viewModel.selectBook(book.id)
                            scope.launch {
                                val file = viewModel.exportCurrentViewToPdf(context)
                                if (file != null) {
                                    shareExportedFile(file, "application/pdf")
                                } else {
                                    Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8)),
                        color = Color.Transparent,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .testTag("book_inside_pdf_button")
                    ) {
                        Text(
                            text = "PDF",
                            color = Color(0xFF818CF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    // Overflow Menu
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("book_inside_more_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options"
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("View Reports & Statistics") },
                                leadingIcon = {
                                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToDashboard()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Export CSV") },
                                leadingIcon = {
                                    Icon(Icons.Default.TableChart, contentDescription = null)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.selectBook(book.id)
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
                                text = { Text("Book Settings") },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenBookSettings()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Google Drive Backup") },
                                leadingIcon = {
                                    Icon(Icons.Default.CloudDone, contentDescription = null)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenDriveBackup()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Split Bottom Action Bar: [+ Cash In] (Green) and [- Cash Out] (Red)
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // + Cash In Button (Vibrant Green)
                    Button(
                        onClick = onNavigateToCashIn,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("button_cash_in")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cash In",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // - Cash Out Button (Vibrant Red)
                    Button(
                        onClick = onNavigateToCashOut,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("button_cash_out")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cash Out",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search by remark or amount",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF818CF8),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("book_inside_search_input")
                )
            }

            // 2. Filter Row: Filter icon + "Select Date [v]" + "Entry Type [v]"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter Icon Button
                    Surface(
                        onClick = {
                            // Reset filters or cycle
                            if (selectedDateFilter != BookDateQuickFilter.ALL || selectedTypeFilter != null) {
                                selectedDateFilter = BookDateQuickFilter.ALL
                                selectedTypeFilter = null
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = if (selectedDateFilter != BookDateQuickFilter.ALL || selectedTypeFilter != null) {
                                    Color(0xFF818CF8)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Select Date Dropdown Chip
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            onClick = { showDateFilterMenu = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedDateFilter != BookDateQuickFilter.ALL) {
                                Color(0xFF818CF8).copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = if (selectedDateFilter != BookDateQuickFilter.ALL) {
                                androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8))
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedDateFilter.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedDateFilter != BookDateQuickFilter.ALL) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedDateFilter != BookDateQuickFilter.ALL) Color(0xFF818CF8) else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showDateFilterMenu,
                            onDismissRequest = { showDateFilterMenu = false }
                        ) {
                            BookDateQuickFilter.values().forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(filter.label) },
                                    onClick = {
                                        selectedDateFilter = filter
                                        showDateFilterMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Entry Type Dropdown Chip
                    Box(modifier = Modifier.weight(1f)) {
                        val typeLabel = when (selectedTypeFilter) {
                            null -> "Entry Type"
                            TransactionType.CASH_IN -> "Cash In"
                            TransactionType.CASH_OUT -> "Cash Out"
                        }

                        Surface(
                            onClick = { showTypeFilterMenu = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTypeFilter != null) {
                                Color(0xFF818CF8).copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = if (selectedTypeFilter != null) {
                                androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8))
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = typeLabel,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTypeFilter != null) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTypeFilter != null) Color(0xFF818CF8) else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showTypeFilterMenu,
                            onDismissRequest = { showTypeFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Entry Types") },
                                onClick = {
                                    selectedTypeFilter = null
                                    showTypeFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Cash In (+)") },
                                onClick = {
                                    selectedTypeFilter = TransactionType.CASH_IN
                                    showTypeFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Cash Out (-)") },
                                onClick = {
                                    selectedTypeFilter = TransactionType.CASH_OUT
                                    showTypeFilterMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // 3. Summary Card (Matching Dashboard Card UI Design)
            item {
                val bookColor = Color(book.colorHex)
                val isPositive = netBalance >= 0

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A) // Dark slate background consistent with dashboard
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("book_summary_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        bookColor.copy(alpha = 0.28f),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Top Row: Book Name Pill & Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(bookColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = book.name,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.08f)
                                ) {
                                    Text(
                                        text = if (book.isArchived) "Archived" else "Active",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Net Balance Title & Amount
                            Text(
                                text = "NET BALANCE",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isPositive) {
                                        "${book.currencySymbol} ${String.format(Locale.US, "%,.2f", netBalance)}"
                                    } else {
                                        "-${book.currencySymbol} ${String.format(Locale.US, "%,.2f", kotlin.math.abs(netBalance))}"
                                    },
                                    color = Color.White,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Cash In & Cash Out Stat Cards Row (Side by side matching dashboard)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Cash In Card
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = Color(0xFF4ADE80),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "TOTAL IN",
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp,
                                                maxLines = 1
                                            )
                                            val formattedIn = formatDisplayAmount(totalIn)
                                            val inFontSize = if (formattedIn.length > 9) 12.sp else if (formattedIn.length > 6) 13.sp else 15.sp
                                            Text(
                                                text = "+${book.currencySymbol}$formattedIn",
                                                color = Color(0xFF4ADE80),
                                                fontSize = inFontSize,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Cash Out Card
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                tint = Color(0xFFF87171),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "TOTAL OUT",
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp,
                                                maxLines = 1
                                            )
                                            val formattedOut = formatDisplayAmount(totalOut)
                                            val outFontSize = if (formattedOut.length > 9) 12.sp else if (formattedOut.length > 6) 13.sp else 15.sp
                                            Text(
                                                text = "-${book.currencySymbol}$formattedOut",
                                                color = Color(0xFFF87171),
                                                fontSize = outFontSize,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Showing entries count divider
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Text(
                        text = "Showing ${displayedTransactions.size} entries",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }

            // 5. Grouped Transactions List
            if (groupedByDate.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedDateFilter != BookDateQuickFilter.ALL || selectedTypeFilter != null) {
                                "No entries match your search or filter"
                            } else {
                                "No transactions recorded yet in this book"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tap '+ Cash In' or '- Cash Out' below to add an entry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                groupedByDate.forEach { (dateHeader, entries) ->
                    // Date Header
                    item(key = "header_$dateHeader") {
                        Text(
                            text = dateHeader,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    // Transactions for this date
                    items(entries, key = { it.id }) { tx ->
                        val runningBal = runningBalanceMap[tx.id] ?: 0.0
                        BookTransactionRowItem(
                            tx = tx,
                            runningBalance = runningBal,
                            onClick = { onEditTransaction(tx) },
                            onDelete = { transactionToDelete = tx }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (transactionToDelete != null) {
        val tx = transactionToDelete!!
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Entry?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Delete '${tx.memo.ifBlank { tx.category }}' (${if (tx.type == TransactionType.CASH_IN) "+" else "-"}${formatDisplayAmount(tx.amount)})?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(tx.id)
                        transactionToDelete = null
                        Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Clean transaction row designed identically to image.png
 */
@Composable
fun BookTransactionRowItem(
    tx: TransactionRecord,
    runningBalance: Double,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCashIn = tx.type == TransactionType.CASH_IN
    val dateTimeFormatted = remember(tx.timestamp) {
        SimpleDateFormat("dd MMM, yyyy • h:mm a", Locale.US).format(Date(tx.timestamp))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A) // Dark slate container consistent with dashboard cards
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("tx_row_${tx.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Badges on left, Amount on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category & Payment Mode Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.10f)
                    ) {
                        Text(
                            text = tx.category.ifBlank { "General" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF93C5FD),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Payment Mode Badge
                    val mode = tx.paymentMode ?: "Online"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = mode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF7DD3FC),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Amount (Red for Out, Green for In)
                Text(
                    text = "${if (isCashIn) "+" else "-"}${formatDisplayAmount(tx.amount)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCashIn) Color(0xFF4ADE80) else Color(0xFFF87171)
                )
            }

            // Middle Row: Remark/Memo on left, Running Balance on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tx.memo.ifBlank { tx.category },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Balance: ${formatDisplayAmount(runningBalance)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }

            // Bottom Row: Date and Time on left (without "entry by you"), quick delete on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = dateTimeFormatted,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Normal
                    )
                }

                // Quick delete icon
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Format numbers with standard comma separators e.g. 10,000 or 33,87,000
 */
private fun formatDisplayAmount(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
    }
    return formatter.format(amount)
}
