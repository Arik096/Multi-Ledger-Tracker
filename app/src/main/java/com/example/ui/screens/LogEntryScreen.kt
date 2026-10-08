package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.DatePicker
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.ui.components.CalculatorEntryField
import com.example.util.MathExpressionEvaluator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.components.getIconForCategory
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import com.example.ui.viewmodel.LedgerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogEntryScreen(
    viewModel: LedgerViewModel,
    initialTransaction: TransactionRecord? = null,
    initialBookId: Long? = null,
    initialType: TransactionType = TransactionType.CASH_OUT,
    onDismiss: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()

    // Implied book: user enters from a book directly, no book selector needed
    val targetBookId = remember(initialTransaction, initialBookId, viewModel.selectedBookId.value, allBooks) {
        initialTransaction?.ledgerBookId
            ?: initialBookId
            ?: viewModel.selectedBookId.value
            ?: allBooks.firstOrNull { !it.isArchived }?.id
            ?: allBooks.firstOrNull()?.id
            ?: 0L
    }

    val currentBook = remember(targetBookId, allBooks) {
        allBooks.firstOrNull { it.id == targetBookId }
    }

    val isBookArchived = currentBook?.isArchived == true

    var selectedType by remember(initialTransaction, initialType) {
        mutableStateOf(initialTransaction?.type ?: initialType)
    }

    var selectedPaymentMode by remember {
        mutableStateOf(initialTransaction?.paymentMode ?: "Cash")
    }

    var title by remember {
        mutableStateOf(initialTransaction?.memo ?: "")
    }
    var titleError by remember { mutableStateOf(false) }

    var amountStr by remember {
        mutableStateOf(
            if (initialTransaction != null) {
                if (initialTransaction.amount % 1.0 == 0.0) {
                    initialTransaction.amount.toLong().toString()
                } else {
                    initialTransaction.amount.toString()
                }
            } else ""
        )
    }
    var amountError by remember { mutableStateOf(false) }

    // Categories are unified and independent
    val isCategoryEnabled = currentBook?.isCategoryEnabled ?: true
    val isCategoryMandatory = currentBook?.isCategoryMandatory ?: false
    val isTransactionTypeEnabled = currentBook?.isTransactionTypeEnabled ?: true
    val isTransactionTypeMandatory = currentBook?.isTransactionTypeMandatory ?: false

    val availableCategories = remember(currentBook) {
        currentBook?.getCategories() ?: emptyList()
    }

    var selectedCategory by remember {
        mutableStateOf(
            initialTransaction?.category
                ?: availableCategories.firstOrNull()
                ?: "General"
        )
    }
    var categoryError by remember { mutableStateOf(false) }

    LaunchedEffect(availableCategories) {
        if (selectedCategory.isBlank() && availableCategories.isNotEmpty()) {
            selectedCategory = availableCategories.first()
        }
    }

    var timestamp by remember {
        mutableLongStateOf(initialTransaction?.timestamp ?: System.currentTimeMillis())
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showAddNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }

    // System navigation gesture support
    BackHandler {
        onDismiss()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (initialTransaction == null) "Add Entry" else "Edit Entry",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1
                        )
                        if (currentBook != null) {
                            Text(
                                text = currentBook.name,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("log_entry_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel and go back"
                        )
                    }
                },
                actions = {
                    if (initialTransaction != null) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.testTag("log_entry_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete entry",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Archived Book Warning Banner
            if (isBookArchived) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
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
                                text = "No entry can be added into archived books. Please unarchive '${currentBook?.name}' in Book Settings first.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // 1. Amount Input Field with Built-in Active Calculator
            CalculatorEntryField(
                value = amountStr,
                onValueChange = {
                    amountStr = it
                    if (amountError) amountError = false
                },
                label = "Amount (${currentBook?.currencySymbol ?: "৳"}) *",
                placeholder = "0.00 (e.g. 500+250*2)",
                isError = amountError,
                supportingText = if (amountError) {
                    { Text("Please enter a valid amount or expression", color = MaterialTheme.colorScheme.error) }
                } else null,
                testTag = "log_entry_amount_input",
                showKeypadInitially = false
            )

            // 2. Title Input Field
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError) titleError = false
                },
                label = { Text("Title / Memo *") },
                placeholder = { Text("e.g. Salary, Groceries, Client payment...") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Title is required", color = MaterialTheme.colorScheme.error) }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("log_entry_title_input")
            )

            // 3. Category Dropdown (if enabled in book settings)
            if (isCategoryEnabled) {
                Text(
                    text = if (isCategoryMandatory) "Category *" else "Category (Optional)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (selectedCategory.isBlank()) "(None / Optional)" else selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Category") },
                        isError = categoryError,
                        supportingText = if (categoryError) {
                            { Text("Category is mandatory for this book", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        leadingIcon = {
                            Icon(
                                imageVector = if (selectedCategory.isBlank()) Icons.Default.Category else getIconForCategory(selectedCategory),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("log_entry_category_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        if (!isCategoryMandatory) {
                            DropdownMenuItem(
                                text = { Text("(None / Optional)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = {
                                    selectedCategory = ""
                                    categoryError = false
                                    showCategoryDropdown = false
                                }
                            )
                        }

                        availableCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = getIconForCategory(cat),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(cat)
                                    }
                                },
                                onClick = {
                                    selectedCategory = cat
                                    categoryError = false
                                    showCategoryDropdown = false
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "New Category",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Add New Category...",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {
                                showCategoryDropdown = false
                                showAddNewCategoryDialog = true
                            }
                        )
                    }
                }
            }

            // 4. Transaction Type: Online vs Cash (if enabled in book settings)
            if (isTransactionTypeEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTransactionTypeMandatory) "Transaction Type *" else "Transaction Type (Optional)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Online Payment
                    Surface(
                        onClick = { selectedPaymentMode = "Online" },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selectedPaymentMode == "Online") {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        border = if (selectedPaymentMode == "Online") {
                            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("log_mode_online")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = "Online",
                                tint = if (selectedPaymentMode == "Online") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Online",
                                fontWeight = if (selectedPaymentMode == "Online") FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedPaymentMode == "Online") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Cash Payment
                    Surface(
                        onClick = { selectedPaymentMode = "Cash" },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selectedPaymentMode == "Cash") {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        border = if (selectedPaymentMode == "Cash") {
                            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("log_mode_cash")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = "Cash",
                                tint = if (selectedPaymentMode == "Cash") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cash",
                                fontWeight = if (selectedPaymentMode == "Cash") FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedPaymentMode == "Cash") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 5. Flow Direction: Cash In vs Cash Out
            Text(
                text = "Flow Direction",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cash Out
                Surface(
                    onClick = { selectedType = TransactionType.CASH_OUT },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedType == TransactionType.CASH_OUT) {
                        CashOutRed.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    border = if (selectedType == TransactionType.CASH_OUT) {
                        androidx.compose.foundation.BorderStroke(2.dp, CashOutRed)
                    } else null,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("log_flow_cash_out")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (selectedType == TransactionType.CASH_OUT) CashOutRed else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cash Out",
                            fontWeight = if (selectedType == TransactionType.CASH_OUT) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedType == TransactionType.CASH_OUT) CashOutRed else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    }
                }

                // Cash In
                Surface(
                    onClick = { selectedType = TransactionType.CASH_IN },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedType == TransactionType.CASH_IN) {
                        CashInGreen.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    border = if (selectedType == TransactionType.CASH_IN) {
                        androidx.compose.foundation.BorderStroke(2.dp, CashInGreen)
                    } else null,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("log_flow_cash_in")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (selectedType == TransactionType.CASH_IN) CashInGreen else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cash In",
                            fontWeight = if (selectedType == TransactionType.CASH_IN) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedType == TransactionType.CASH_IN) CashInGreen else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // 6. Date & Time
            Text(
                text = "Date & Time",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date Picker
                Surface(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                        DatePickerDialog(
                            context,
                            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                                val selectedCal = Calendar.getInstance().apply {
                                    timeInMillis = timestamp
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                }
                                timestamp = selectedCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("log_entry_date_picker")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = dateFormat.format(Date(timestamp)),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Time Picker
                val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
                Surface(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                        TimePickerDialog(
                            context,
                            { _, hourOfDay: Int, minute: Int ->
                                val selectedCal = Calendar.getInstance().apply {
                                    timeInMillis = timestamp
                                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    set(Calendar.MINUTE, minute)
                                }
                                timestamp = selectedCal.timeInMillis
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            false
                        ).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("log_entry_time_picker")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Select Time",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timeFormat.format(Date(timestamp)),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 7. Submit Action Button: "Add Entry" (or "Save Changes")
            Button(
                onClick = {
                    var hasError = false

                    if (title.isBlank()) {
                        titleError = true
                        hasError = true
                    }

                    val parsedAmount = amountStr.toDoubleOrNull()
                        ?: MathExpressionEvaluator.evaluate(amountStr)

                    if (parsedAmount == null || parsedAmount <= 0.0) {
                        amountError = true
                        hasError = true
                    } else {
                        // Normalize amountStr to final evaluated string
                        amountStr = MathExpressionEvaluator.formatResult(parsedAmount)
                    }

                    if (isCategoryEnabled && isCategoryMandatory && selectedCategory.isBlank()) {
                        categoryError = true
                        hasError = true
                    }

                    if (isBookArchived) {
                        return@Button
                    }

                    if (hasError) return@Button

                    val finalCategory = if (!isCategoryEnabled) "General" else (if (selectedCategory.isBlank()) "General" else selectedCategory)
                    val finalPaymentMode = if (!isTransactionTypeEnabled) "Cash" else selectedPaymentMode

                    if (initialTransaction == null) {
                        viewModel.addTransaction(
                            bookId = targetBookId,
                            type = selectedType,
                            amount = parsedAmount!!,
                            category = finalCategory,
                            timestamp = timestamp,
                            title = title.trim(),
                            paymentMode = finalPaymentMode
                        )
                    } else {
                        viewModel.updateTransaction(
                            initialTransaction.copy(
                                ledgerBookId = targetBookId,
                                type = selectedType,
                                amount = parsedAmount!!,
                                category = finalCategory,
                                timestamp = timestamp,
                                memo = title.trim(),
                                paymentMode = finalPaymentMode
                            )
                        )
                    }
                    onSaveSuccess()
                },
                enabled = !isBookArchived,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp, max = 54.dp)
                    .testTag("log_entry_save_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialTransaction == null) "Save Entry" else "Save",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    // Add New Category Dialog
    if (showAddNewCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddNewCategoryDialog = false },
            title = { Text("Add Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryInput,
                    onValueChange = { newCategoryInput = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Subscriptions, Freelance...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCategoryInput.trim()
                        if (trimmed.isNotBlank()) {
                            selectedCategory = trimmed
                            categoryError = false
                            if (currentBook != null && !availableCategories.contains(trimmed)) {
                                val updated = (currentBook.getCategories() + trimmed).distinct()
                                viewModel.updateBook(currentBook.withCategories(updated))
                            }
                            newCategoryInput = ""
                            showAddNewCategoryDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNewCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog && initialTransaction != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Entry") },
            text = { Text("Are you sure you want to permanently delete this entry?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(initialTransaction.id)
                        showDeleteConfirmDialog = false
                        onSaveSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
