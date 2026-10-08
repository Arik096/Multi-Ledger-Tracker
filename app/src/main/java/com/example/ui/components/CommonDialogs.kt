package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.io.ImportValidationSummary
import com.example.data.model.CategoryConstants
import com.example.data.model.DateRangeFilter
import com.example.data.model.FilterCriteria
import com.example.data.model.LedgerBook
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditLedgerDialog(
    initialBook: LedgerBook? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: Long) -> Unit,
    onImportCsvClick: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialBook?.name ?: "") }
    var selectedColor by remember { mutableStateOf(initialBook?.colorHex ?: 0xFF0D9488L) }
    var isError by remember { mutableStateOf(false) }

    val colorOptions = listOf(
        0xFF0D9488L, // Emerald
        0xFF3B82F6L, // Blue
        0xFFF59E0BL, // Amber
        0xFF8B5CF6L, // Purple
        0xFFEC4899L, // Pink
        0xFF10B981L, // Green
        0xFFF97316L, // Orange
        0xFF64748BL  // Slate
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialBook == null) "Create Ledger Book" else "Edit Ledger Book",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (isError && it.isNotBlank()) isError = false
                    },
                    label = { Text("Book Name") },
                    placeholder = { Text("e.g. Personal, Business, Travel") },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Book name cannot be empty") }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ledger_name_input")
                )

                // Color Theme selection
                Text(
                    text = "Theme Color",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colorOptions.forEach { colorHex ->
                        val isSelected = selectedColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .clickable { selectedColor = colorHex }
                                .then(
                                    if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (initialBook == null && onImportCsvClick != null) {
                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                    OutlinedButton(
                        onClick = onImportCsvClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_import_csv_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Book with CSV")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isBlank()) {
                        isError = true
                    } else {
                        onConfirm(name.trim(), selectedColor)
                    }
                },
                modifier = Modifier.testTag("save_ledger_button")
            ) {
                Text(if (initialBook == null) "Create Book" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionDialog(
    initialTransaction: TransactionRecord? = null,
    currencySymbol: String = "",
    onDismiss: () -> Unit,
    onConfirm: (type: TransactionType, amount: Double, category: String, timestamp: Long, memo: String) -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(initialTransaction?.type ?: TransactionType.CASH_OUT) }
    var amountStr by remember { mutableStateOf(if (initialTransaction != null) String.format(Locale.US, "%.2f", initialTransaction.amount) else "") }
    var selectedCategory by remember { mutableStateOf(initialTransaction?.category ?: if (selectedType == TransactionType.CASH_IN) "Salary" else "Food & Dining") }
    var customCategoryInput by remember { mutableStateOf("") }
    var showCustomCategoryField by remember { mutableStateOf(false) }
    var timestamp by remember { mutableStateOf(initialTransaction?.timestamp ?: System.currentTimeMillis()) }
    var memo by remember { mutableStateOf(initialTransaction?.memo ?: "") }
    var amountError by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val calendar = remember(timestamp) {
        Calendar.getInstance().apply { timeInMillis = timestamp }
    }

    val availableCategories = if (selectedType == TransactionType.CASH_IN) {
        CategoryConstants.defaultIncomeCategories
    } else {
        CategoryConstants.defaultExpenseCategories
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_edit_transaction_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialTransaction == null) "Log Transaction" else "Edit Transaction",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Cash In / Cash Out Toggle Tabs
                TabRow(
                    selectedTabIndex = if (selectedType == TransactionType.CASH_IN) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedType == TransactionType.CASH_IN,
                        onClick = {
                            selectedType = TransactionType.CASH_IN
                            if (!CategoryConstants.defaultIncomeCategories.contains(selectedCategory)) {
                                selectedCategory = CategoryConstants.defaultIncomeCategories.first()
                            }
                        },
                        text = {
                            Text(
                                "Cash In (Income)",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedType == TransactionType.CASH_IN) CashInGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.testTag("toggle_cash_in")
                    )
                    Tab(
                        selected = selectedType == TransactionType.CASH_OUT,
                        onClick = {
                            selectedType = TransactionType.CASH_OUT
                            if (!CategoryConstants.defaultExpenseCategories.contains(selectedCategory)) {
                                selectedCategory = CategoryConstants.defaultExpenseCategories.first()
                            }
                        },
                        text = {
                            Text(
                                "Cash Out (Expense)",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedType == TransactionType.CASH_OUT) CashOutRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.testTag("toggle_cash_out")
                    )
                }

                // Amount Field
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        if (amountError) amountError = false
                    },
                    label = { Text("Amount *") },
                    placeholder = { Text("0.00") },
                    isError = amountError,
                    supportingText = if (amountError) {
                        { Text("Please enter a valid amount greater than 0", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_amount_input")
                )

                // Category Selection
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableCategories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = cat
                                showCustomCategoryField = false
                            },
                            label = { Text(cat, fontSize = 12.sp) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("category_chip_$cat")
                        )
                    }

                    // Custom category chip
                    FilterChip(
                        selected = showCustomCategoryField,
                        onClick = { showCustomCategoryField = true },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Custom", fontSize = 12.sp)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("custom_category_button")
                    )
                }

                if (showCustomCategoryField) {
                    OutlinedTextField(
                        value = customCategoryInput,
                        onValueChange = {
                            customCategoryInput = it
                            selectedCategory = it
                        },
                        label = { Text("Custom Category Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_category_input")
                    )
                }

                // Date & Time Picker Pickers
                Text(
                    text = "Date & Time",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Date Button
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    calendar.set(Calendar.YEAR, year)
                                    calendar.set(Calendar.MONTH, month)
                                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    timestamp = calendar.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("date_picker_button")
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Date", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(dateFormat.format(Date(timestamp)), fontSize = 12.sp)
                    }

                    // Time Button
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    calendar.set(Calendar.MINUTE, minute)
                                    timestamp = calendar.timeInMillis
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("time_picker_button")
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = "Time", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(timeFormat.format(Date(timestamp)), fontSize = 12.sp)
                    }
                }

                // Memo / Notes
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("Memo / Notes (Optional)") },
                    placeholder = { Text("Context, receipt reference, details...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_memo_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsedAmount = amountStr.toDoubleOrNull()
                            if (parsedAmount == null || parsedAmount <= 0) {
                                amountError = true
                            } else {
                                val finalCategory = if (showCustomCategoryField && customCategoryInput.isNotBlank()) {
                                    customCategoryInput.trim()
                                } else {
                                    selectedCategory
                                }
                                onConfirm(selectedType, parsedAmount, finalCategory, timestamp, memo.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == TransactionType.CASH_IN) CashInGreen else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("save_transaction_button")
                    ) {
                        Text(if (initialTransaction == null) "Log Entry" else "Save Changes")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    transaction: TransactionRecord?,
    currencySymbol: String = "",
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    if (transaction == null) return
    val sheetState = rememberModalBottomSheetState()
    val isIncome = transaction.type == TransactionType.CASH_IN
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM dd, yyyy • HH:mm", Locale.getDefault()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Entry?") },
            text = {
                Text("Delete this ${transaction.category} entry for ${currencySymbol}${String.format(Locale.US, "%.2f", transaction.amount)}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDismiss()
                        onDeleteClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isIncome) CashInGreen.copy(alpha = 0.15f) else CashOutRed.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = transaction.type.label,
                        color = if (isIncome) CashInGreen else CashOutRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "${if (isIncome) "+" else "-"}${String.format(Locale.US, "%,.2f", transaction.amount)}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (isIncome) CashInGreen else CashOutRed
                )
            }

            Column {
                Text(
                    text = "Category",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Column {
                Text(
                    text = "Date & Time",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateFormat.format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (transaction.memo.isNotBlank()) {
                Column {
                    Text(
                        text = "Memo / Notes",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = transaction.memo,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onDuplicateClick()
                    },
                    modifier = Modifier.weight(1f).testTag("detail_duplicate_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Duplicate")
                }

                Button(
                    onClick = {
                        onDismiss()
                        onEditClick()
                    },
                    modifier = Modifier.weight(1f).testTag("detail_edit_button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit")
                }

                IconButton(
                    onClick = {
                        showDeleteConfirmation = true
                    },
                    modifier = Modifier.testTag("detail_delete_button")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedFilterDialog(
    currentCriteria: FilterCriteria,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onApply: (FilterCriteria) -> Unit
) {
    var dateRange by remember { mutableStateOf(currentCriteria.dateRange) }
    var typeFilter by remember { mutableStateOf(currentCriteria.typeFilter) }
    var selectedCategory by remember { mutableStateOf(currentCriteria.categoryFilter) }
    var minAmountStr by remember { mutableStateOf(currentCriteria.minAmount?.toString() ?: "") }
    var maxAmountStr by remember { mutableStateOf(currentCriteria.maxAmount?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Transactions", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Type Filter
                Text("Transaction Type", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = typeFilter == null,
                        onClick = { typeFilter = null },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = typeFilter == TransactionType.CASH_IN,
                        onClick = { typeFilter = TransactionType.CASH_IN },
                        label = { Text("Cash In") }
                    )
                    FilterChip(
                        selected = typeFilter == TransactionType.CASH_OUT,
                        onClick = { typeFilter = TransactionType.CASH_OUT },
                        label = { Text("Cash Out") }
                    )
                }

                // Date Range
                Text("Date Period", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DateRangeFilter.values().forEach { range ->
                        FilterChip(
                            selected = dateRange == range,
                            onClick = { dateRange = range },
                            label = { Text(range.label, fontSize = 12.sp) }
                        )
                    }
                }

                // Amount Range
                Text("Amount Range", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minAmountStr,
                        onValueChange = { minAmountStr = it },
                        label = { Text("Min") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxAmountStr,
                        onValueChange = { maxAmountStr = it },
                        label = { Text("Max") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Category Selection
                if (availableCategories.isNotEmpty()) {
                    Text("Category", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All Categories", fontSize = 11.sp) }
                        )
                        availableCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val min = minAmountStr.toDoubleOrNull()
                    val max = maxAmountStr.toDoubleOrNull()
                    onApply(
                        currentCriteria.copy(
                            dateRange = dateRange,
                            typeFilter = typeFilter,
                            categoryFilter = selectedCategory,
                            minAmount = min,
                            maxAmount = max
                        )
                    )
                }
            ) {
                Text("Apply Filters")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onApply(FilterCriteria())
                }
            ) {
                Text("Reset All")
            }
        }
    )
}

@Composable
fun ImportValidationDialog(
    summary: ImportValidationSummary,
    bookName: String,
    onDismiss: () -> Unit,
    onCommit: () -> Unit
) {
    val isValidReady = summary.validRows.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Import Validation", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Target Book indicator
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Destination: $bookName",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Total Rows", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${summary.totalRows}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = CashInGreen.copy(alpha = 0.15f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Valid", fontSize = 11.sp, color = CashInGreen)
                            Text("${summary.validRows.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CashInGreen)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (summary.invalidRows.isNotEmpty()) CashOutRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Errors", fontSize = 11.sp, color = if (summary.invalidRows.isNotEmpty()) CashOutRed else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${summary.invalidRows.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (summary.invalidRows.isNotEmpty()) CashOutRed else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Error list if any
                if (summary.invalidRows.isNotEmpty()) {
                    Text("Validation Warnings / Errors", fontWeight = FontWeight.Bold, color = CashOutRed, fontSize = 12.sp)
                    summary.invalidRows.take(5).forEach { inv ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CashOutRed.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Row ${inv.lineNumber}: ${inv.errors.joinToString("; ")}", fontSize = 11.sp, color = CashOutRed)
                            }
                        }
                    }
                    if (summary.invalidRows.size > 5) {
                        Text("+ ${summary.invalidRows.size - 5} more error rows skipped", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Preview of valid rows
                if (summary.validRows.isNotEmpty()) {
                    Text("Valid Entries Preview (${summary.validRows.size} ready)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    summary.validRows.take(4).forEach { valid ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(valid.categoryStr, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text(valid.dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    "${if (valid.type == TransactionType.CASH_IN) "+" else "-"}${valid.amountStr}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (valid.type == TransactionType.CASH_IN) CashInGreen else CashOutRed,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCommit,
                enabled = isValidReady,
                modifier = Modifier.testTag("commit_import_button")
            ) {
                Text("Commit ${summary.validRows.size} Entries")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
