package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LedgerBook
import com.example.data.model.TransactionType
import com.example.ui.components.AddEditLedgerDialog
import com.example.ui.components.ImportBookCsvDialog
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.AmountFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LedgersScreen(
    viewModel: LedgerViewModel,
    onBookSelected: (LedgerBook) -> Unit,
    onOpenBookSettings: (LedgerBook) -> Unit,
    onOpenDriveBackup: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()
    val selectedBookId by viewModel.selectedBookId.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val amountPrecision by viewModel.amountPrecision.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportCsvDialog by remember { mutableStateOf(false) }
    var targetBookForImport by remember { mutableStateOf<LedgerBook?>(null) }
    var bookToDelete by remember { mutableStateOf<LedgerBook?>(null) }

    // Tab Selection: 0 = Active Books, 1 = Archived Books
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // Clear search on back gesture
    BackHandler(enabled = searchQuery.isNotBlank()) {
        searchQuery = ""
    }

    // Precalculate balance and latest updated timestamp for each book
    val bookStatsMap = remember(allBooks, allTransactions) {
        allBooks.associate { book ->
            val txs = allTransactions.filter { it.ledgerBookId == book.id }
            val inSum = txs.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount }
            val outSum = txs.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount }
            val balance = inSum - outSum
            val latestUpdated = txs.maxOfOrNull { it.timestamp } ?: book.createdAt
            book.id to Pair(balance, latestUpdated)
        }
    }

    // Sort books by last updated date time (most recently updated first)
    val activeBooks = remember(allBooks, searchQuery, bookStatsMap) {
        allBooks.filter { !it.isArchived }
            .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }
            .sortedByDescending { bookStatsMap[it.id]?.second ?: it.createdAt }
    }

    val archivedBooks = remember(allBooks, searchQuery, bookStatsMap) {
        allBooks.filter { it.isArchived }
            .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }
            .sortedByDescending { bookStatsMap[it.id]?.second ?: it.createdAt }
    }

    val currentList = if (selectedTab == 0) activeBooks else archivedBooks

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Title & Action Icons
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
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("ledgers_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Dashboard"
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Books",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${allBooks.count { !it.isArchived }} Active • ${allBooks.count { it.isArchived }} Archived",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Import Book from CSV
                    FilledTonalIconButton(
                        onClick = {
                            targetBookForImport = null
                            showImportCsvDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("button_import_book_csv")
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Import Book with CSV",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Cloud Sync & Profile Icon Button
                    FilledIconButton(
                        onClick = onOpenDriveBackup,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("button_open_drive_backup")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Cloud Sync & Profile",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Add Book Icon Button
                    FilledIconButton(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("button_add_book")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add New Book",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search books...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("book_search_bar")
            )

            // Tabs: Active vs Archived
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Active", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedTab == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = allBooks.count { !it.isArchived }.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.testTag("tab_active_books")
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Archived", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedTab == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = allBooks.count { it.isArchived }.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.testTag("tab_archived_books")
                )
            }

            // Book List
            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.MenuBook else Icons.Default.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (selectedTab == 0) "No active books" else "No archived books",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedTab == 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showCreateDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create Book", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                OutlinedButton(
                                    onClick = {
                                        targetBookForImport = null
                                        showImportCsvDialog = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import CSV", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(currentList, key = { it.id }) { book ->
                        val isSelected = selectedBookId == book.id
                        val bookColor = Color(book.colorHex)
                        val stats = bookStatsMap[book.id] ?: Pair(0.0, book.createdAt)
                        val balance = stats.first
                        val lastUpdatedTimestamp = stats.second
                        var menuExpanded by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    viewModel.selectBook(book.id)
                                    onBookSelected(book)
                                }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(1.5.dp, bookColor.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                                    } else Modifier
                                )
                                .testTag("book_card_${book.id}"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Book Avatar Icon
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(bookColor.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = bookColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // 2. Book Info (Name, Balance pill, Last Updated Time)
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = book.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = bookColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = bookColor,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Balance pill
                                    val formattedBalance = AmountFormatter.format(balance, amountPrecision, includeCommas = true)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (balance >= 0) CashInGreen.copy(alpha = 0.12f) else CashOutRed.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "Balance: ${book.currencySymbol}$formattedBalance",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (balance >= 0) CashInGreen else CashOutRed,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(5.dp))

                                    // Last Updated Date & Time
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = formatBookLastUpdated(lastUpdatedTimestamp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // 3. Dropdown Menu Icon showing all actions
                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.testTag("book_dropdown_menu_button_${book.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Actions for ${book.name}",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Open Ledger") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                viewModel.selectBook(book.id)
                                                onBookSelected(book)
                                            }
                                        )

                                        if (!isSelected) {
                                            DropdownMenuItem(
                                                text = { Text("Set as Active") },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                onClick = {
                                                    menuExpanded = false
                                                    viewModel.selectBook(book.id)
                                                    Toast.makeText(context, "${book.name} set as active book", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }

                                        DropdownMenuItem(
                                            text = { Text("Book Settings") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                onOpenBookSettings(book)
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Import CSV") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.UploadFile,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                targetBookForImport = book
                                                showImportCsvDialog = true
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text(if (book.isArchived) "Unarchive Book" else "Archive Book") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (book.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                viewModel.archiveBook(book.id, !book.isArchived)
                                                val action = if (book.isArchived) "unarchived" else "archived"
                                                Toast.makeText(context, "${book.name} $action", Toast.LENGTH_SHORT).show()
                                            }
                                        )

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                        DropdownMenuItem(
                                            text = { Text("Delete Book", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                bookToDelete = book
                                            }
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

    // Create New Book Dialog
    if (showCreateDialog) {
        AddEditLedgerDialog(
            initialBook = null,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, colorHex ->
                viewModel.createBook(
                    name = name,
                    colorHex = colorHex
                )
                showCreateDialog = false
            },
            onImportCsvClick = {
                showCreateDialog = false
                targetBookForImport = null
                showImportCsvDialog = true
            }
        )
    }

    // Import Book / Entries with CSV Dialog
    if (showImportCsvDialog) {
        ImportBookCsvDialog(
            viewModel = viewModel,
            targetBook = targetBookForImport,
            onDismiss = {
                showImportCsvDialog = false
                targetBookForImport = null
            },
            onImportSuccess = { bookId ->
                showImportCsvDialog = false
                targetBookForImport = null
                viewModel.selectBook(bookId)
            }
        )
    }

    // Delete Book Confirmation Dialog
    if (bookToDelete != null) {
        val target = bookToDelete!!
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Delete Book '${target.name}'?") },
            text = {
                Text(
                    "This will permanently delete '${target.name}' and all associated logs. This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBook(target.id)
                        bookToDelete = null
                        Toast.makeText(context, "Deleted ${target.name}", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatBookLastUpdated(timestamp: Long): String {
    val cal = Calendar.getInstance()
    val todayYear = cal.get(Calendar.YEAR)
    val todayDay = cal.get(Calendar.DAY_OF_YEAR)

    val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val targetYear = targetCal.get(Calendar.YEAR)
    val targetDay = targetCal.get(Calendar.DAY_OF_YEAR)

    val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date(timestamp))

    return if (todayYear == targetYear && todayDay == targetDay) {
        "Updated today, $timeStr"
    } else if (todayYear == targetYear && todayDay - targetDay == 1) {
        "Updated yesterday, $timeStr"
    } else {
        val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(timestamp))
        "Updated $dateStr"
    }
}

