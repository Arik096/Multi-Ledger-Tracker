package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import com.example.data.preferences.AppPreferencesManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

data class CashField(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amountStr: String = "",
    val isCustom: Boolean = false
)

data class PositionSnapshot(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val timeLabel: String,
    val physicalTotal: Double,
    val accountTotal: Double,
    val difference: Double,
    val physicalFields: List<CashField>,
    val accountFields: List<CashField>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashPositionScreen(
    currencySymbol: String = "৳",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Initialize fields from saved preferences or defaults
    val physicalFields = remember {
        mutableStateListOf<CashField>().apply {
            addAll(AppPreferencesManager.getPhysicalCashFields(context).map { CashField(name = it) })
        }
    }

    val accountFields = remember {
        mutableStateListOf<CashField>().apply {
            addAll(AppPreferencesManager.getAccountCashFields(context).map { CashField(name = it) })
        }
    }

    // In-memory snapshots to compare positions at different times (no data saved to DB)
    val sessionSnapshots = remember { mutableStateListOf<PositionSnapshot>() }

    var showAddPhysicalDialog by remember { mutableStateOf(false) }
    var newPhysicalName by remember { mutableStateOf("") }

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var newAccountName by remember { mutableStateOf("") }

    // State for modifying an existing field: Pair(isPhysical: Boolean, index: Int)
    var fieldBeingEdited by remember { mutableStateOf<Pair<Boolean, Int>?>(null) }
    var editFieldNameText by remember { mutableStateOf("") }

    // State for deleting an existing field: Pair(isPhysical: Boolean, index: Int)
    var fieldBeingDeleted by remember { mutableStateOf<Pair<Boolean, Int>?>(null) }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // Computations
    val physicalTotal = physicalFields.sumOf { it.amountStr.toDoubleOrNull() ?: 0.0 }
    val accountTotal = accountFields.sumOf { it.amountStr.toDoubleOrNull() ?: 0.0 }
    val difference = physicalTotal - accountTotal

    val timeFormatter = remember { SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cash Position",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "Physical vs Account Comparison",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                actions = {
                    // Quick Clear amounts button
                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.testTag("cash_position_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear all amounts",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Reset all fields back to defaults button
                    IconButton(
                        onClick = { showResetConfirmDialog = true },
                        modifier = Modifier.testTag("cash_position_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset fields to default",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Comparison Result Card (Always visible at the top)
            item(key = "comparison_hero_card") {
                ComparisonResultHeroCard(
                    currencySymbol = currencySymbol,
                    physicalTotal = physicalTotal,
                    accountTotal = accountTotal,
                    difference = difference,
                    onSaveSnapshot = {
                        val snapshot = PositionSnapshot(
                            timeLabel = timeFormatter.format(Date()),
                            physicalTotal = physicalTotal,
                            accountTotal = accountTotal,
                            difference = difference,
                            physicalFields = physicalFields.map { it.copy() },
                            accountFields = accountFields.map { it.copy() }
                        )
                        sessionSnapshots.add(0, snapshot)
                    }
                )
            }

            // 2. Physical Cash Category Card
            item(key = "physical_cash_card") {
                CashCategorySection(
                    title = "Physical Cash",
                    subtitle = "Bank balances, cash in hand, wallets & OD",
                    totalAmount = physicalTotal,
                    currencySymbol = currencySymbol,
                    items = physicalFields,
                    accentColor = Color(0xFF0D9488), // Teal/Emerald
                    badgeContainerColor = Color(0xFF0D9488).copy(alpha = 0.12f),
                    onUpdateAmount = { index, newStr ->
                        physicalFields[index] = physicalFields[index].copy(amountStr = newStr)
                    },
                    onToggleSign = { index ->
                        val current = physicalFields[index].amountStr
                        val updated = toggleNegativeSign(current)
                        physicalFields[index] = physicalFields[index].copy(amountStr = updated)
                    },
                    onEditField = { index ->
                        if (index in physicalFields.indices) {
                            editFieldNameText = physicalFields[index].name
                            fieldBeingEdited = Pair(true, index)
                        }
                    },
                    onDeleteField = { index ->
                        if (index in physicalFields.indices) {
                            fieldBeingDeleted = Pair(true, index)
                        }
                    },
                    onAddNewFieldClick = {
                        newPhysicalName = ""
                        showAddPhysicalDialog = true
                    }
                )
            }

            // 3. Account Cash Category Card
            item(key = "account_cash_card") {
                CashCategorySection(
                    title = "Account Cash",
                    subtitle = "Savings, current month budget, extra & lendings",
                    totalAmount = accountTotal,
                    currencySymbol = currencySymbol,
                    items = accountFields,
                    accentColor = Color(0xFF6366F1), // Indigo
                    badgeContainerColor = Color(0xFF6366F1).copy(alpha = 0.12f),
                    onUpdateAmount = { index, newStr ->
                        accountFields[index] = accountFields[index].copy(amountStr = newStr)
                    },
                    onToggleSign = { index ->
                        val current = accountFields[index].amountStr
                        val updated = toggleNegativeSign(current)
                        accountFields[index] = accountFields[index].copy(amountStr = updated)
                    },
                    onEditField = { index ->
                        if (index in accountFields.indices) {
                            editFieldNameText = accountFields[index].name
                            fieldBeingEdited = Pair(false, index)
                        }
                    },
                    onDeleteField = { index ->
                        if (index in accountFields.indices) {
                            fieldBeingDeleted = Pair(false, index)
                        }
                    },
                    onAddNewFieldClick = {
                        newAccountName = ""
                        showAddAccountDialog = true
                    }
                )
            }

            // 4. Saved Snapshots Comparison Section (Compare at Different Times)
            if (sessionSnapshots.isNotEmpty()) {
                item(key = "snapshots_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Positions at Different Times",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        TextButton(
                            onClick = { sessionSnapshots.clear() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Clear All",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                itemsIndexed(sessionSnapshots, key = { _, snap -> snap.id }) { index, snapshot ->
                    SnapshotItemCard(
                        snapshot = snapshot,
                        currencySymbol = currencySymbol,
                        onRestore = {
                            physicalFields.clear()
                            physicalFields.addAll(snapshot.physicalFields.map { it.copy() })
                            accountFields.clear()
                            accountFields.addAll(snapshot.accountFields.map { it.copy() })
                        },
                        onDelete = {
                            sessionSnapshots.removeAt(index)
                        }
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Dialog: Add Physical Cash Field
    if (showAddPhysicalDialog) {
        AlertDialog(
            onDismissRequest = { showAddPhysicalDialog = false },
            title = { Text("Add Physical Cash Field", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Enter a name for the new physical cash account or wallet:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPhysicalName,
                        onValueChange = { newPhysicalName = it },
                        label = { Text("Field Name") },
                        placeholder = { Text("e.g. City Bank, Petty Cash...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_physical_field_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPhysicalName.isNotBlank()) {
                            physicalFields.add(
                                CashField(
                                    name = newPhysicalName.trim()
                                )
                            )
                            AppPreferencesManager.setPhysicalCashFields(context, physicalFields.map { it.name })
                            showAddPhysicalDialog = false
                        }
                    },
                    enabled = newPhysicalName.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPhysicalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Add Account Cash Field
    if (showAddAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("Add Account Cash Field", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Enter a name for the new account cash category:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newAccountName,
                        onValueChange = { newAccountName = it },
                        label = { Text("Field Name") },
                        placeholder = { Text("e.g. Emergency Fund, Investments...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_account_field_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAccountName.isNotBlank()) {
                            accountFields.add(
                                CashField(
                                    name = newAccountName.trim()
                                )
                            )
                            AppPreferencesManager.setAccountCashFields(context, accountFields.map { it.name })
                            showAddAccountDialog = false
                        }
                    },
                    enabled = newAccountName.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Modify Field Name
    fieldBeingEdited?.let { (isPhysical, index) ->
        val currentField = if (isPhysical) physicalFields.getOrNull(index) else accountFields.getOrNull(index)
        if (currentField != null) {
            AlertDialog(
                onDismissRequest = { fieldBeingEdited = null },
                title = { Text("Modify Field", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = if (isPhysical) "Update the name of this physical cash account or wallet:"
                            else "Update the name of this account cash category:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = editFieldNameText,
                            onValueChange = { editFieldNameText = it },
                            label = { Text("Field Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_field_name_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editFieldNameText.isNotBlank()) {
                                val updatedName = editFieldNameText.trim()
                                if (isPhysical && index in physicalFields.indices) {
                                    physicalFields[index] = physicalFields[index].copy(name = updatedName)
                                    AppPreferencesManager.setPhysicalCashFields(context, physicalFields.map { it.name })
                                } else if (!isPhysical && index in accountFields.indices) {
                                    accountFields[index] = accountFields[index].copy(name = updatedName)
                                    AppPreferencesManager.setAccountCashFields(context, accountFields.map { it.name })
                                }
                                fieldBeingEdited = null
                            }
                        },
                        enabled = editFieldNameText.isNotBlank()
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                val target = fieldBeingEdited
                                fieldBeingEdited = null
                                fieldBeingDeleted = target
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { fieldBeingEdited = null }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }

    // Dialog: Confirm Delete Field
    fieldBeingDeleted?.let { (isPhysical, index) ->
        val currentField = if (isPhysical) physicalFields.getOrNull(index) else accountFields.getOrNull(index)
        if (currentField != null) {
            AlertDialog(
                onDismissRequest = { fieldBeingDeleted = null },
                title = { Text("Delete Field?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "Are you sure you want to delete \"${currentField.name}\"? All fields can be re-added or restored by resetting to default."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (isPhysical && index in physicalFields.indices) {
                                physicalFields.removeAt(index)
                                AppPreferencesManager.setPhysicalCashFields(context, physicalFields.map { it.name })
                            } else if (!isPhysical && index in accountFields.indices) {
                                accountFields.removeAt(index)
                                AppPreferencesManager.setAccountCashFields(context, accountFields.map { it.name })
                            }
                            fieldBeingDeleted = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { fieldBeingDeleted = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Dialog: Confirm Clear Amounts
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Amounts?") },
            text = {
                Text("This will clear all entered amounts to 0.00 while keeping your field names intact.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        for (i in physicalFields.indices) {
                            physicalFields[i] = physicalFields[i].copy(amountStr = "")
                        }
                        for (i in accountFields.indices) {
                            accountFields[i] = accountFields[i].copy(amountStr = "")
                        }
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Amounts")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Confirm Reset Fields to Default
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset to Defaults?") },
            text = {
                Text("This will reset all Physical and Account Cash fields back to their default lists and clear entered amounts.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        physicalFields.clear()
                        physicalFields.addAll(AppPreferencesManager.DEFAULT_PHYSICAL_FIELDS.map { CashField(name = it) })
                        accountFields.clear()
                        accountFields.addAll(AppPreferencesManager.DEFAULT_ACCOUNT_FIELDS.map { CashField(name = it) })
                        AppPreferencesManager.resetCashPositionFields(context)
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Hero Card displaying Physical Total vs Account Total and Shortfall / Extra Status
 */
@Composable
private fun ComparisonResultHeroCard(
    currencySymbol: String,
    physicalTotal: Double,
    accountTotal: Double,
    difference: Double,
    onSaveSnapshot: () -> Unit
) {
    val isShortfall = difference < -0.001
    val isExtra = difference > 0.001
    val isBalanced = !isShortfall && !isExtra
    val diffAmount = abs(difference)

    val statusContainerColor = when {
        isShortfall -> CashOutRed.copy(alpha = 0.12f)
        isExtra -> CashInGreen.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    }

    val statusContentColor = when {
        isShortfall -> CashOutRed
        isExtra -> CashInGreen
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cash_position_hero_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Totals Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Physical Total Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Physical Cash",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(currencySymbol, physicalTotal),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (physicalTotal < 0) CashOutRed else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Divider vertical line
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                        .align(Alignment.CenterVertically)
                )

                // Account Total Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = "Account Cash",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(currencySymbol, accountTotal),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (accountTotal < 0) CashOutRed else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prominent Status Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = statusContainerColor,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, statusContentColor.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(statusContentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isShortfall -> Icons.Default.TrendingDown
                                isExtra -> Icons.Default.TrendingUp
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = statusContentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isShortfall -> "Shortfall in Physical Cash"
                                isExtra -> "Extra Physical Cash (Surplus)"
                                else -> "Balanced (Equal Cash)"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = statusContentColor
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = when {
                                isShortfall -> "Physical cash is ${formatCurrency(currencySymbol, diffAmount)} less than account cash"
                                isExtra -> "Physical cash has ${formatCurrency(currencySymbol, diffAmount)} extra over account cash"
                                else -> "Physical cash matches account cash perfectly"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = when {
                            isShortfall -> "-${formatCurrency(currencySymbol, diffAmount)}"
                            isExtra -> "+${formatCurrency(currencySymbol, diffAmount)}"
                            else -> formatCurrency(currencySymbol, 0.0)
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = statusContentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action: Save Snapshot to compare at another time
            OutlinedButton(
                onClick = onSaveSnapshot,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_position_snapshot_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Capture Time Snapshot",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Category Section Card for Physical Cash or Account Cash
 */
@Composable
private fun CashCategorySection(
    title: String,
    subtitle: String,
    totalAmount: Double,
    currencySymbol: String,
    items: List<CashField>,
    accentColor: Color,
    badgeContainerColor: Color,
    onUpdateAmount: (Int, String) -> Unit,
    onToggleSign: (Int) -> Unit,
    onEditField: (Int) -> Unit,
    onDeleteField: (Int) -> Unit,
    onAddNewFieldClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Subtotal Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeContainerColor,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = formatCurrency(currencySymbol, totalAmount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Fields list
            items.forEachIndexed { index, field ->
                CashFieldInputRow(
                    name = field.name,
                    amountStr = field.amountStr,
                    currencySymbol = currencySymbol,
                    onAmountChange = { onUpdateAmount(index, it) },
                    onToggleSign = { onToggleSign(index) },
                    onEditName = { onEditField(index) },
                    onDelete = { onDeleteField(index) }
                )
                if (index < items.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // "+ Add Field" Button
            OutlinedButton(
                onClick = onAddNewFieldClick,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_field_button_${title.lowercase().replace(" ", "_")}")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Field",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add More Fields",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Individual input row for a single cash field (all fields can be modified and deleted)
 */
@Composable
private fun CashFieldInputRow(
    name: String,
    amountStr: String,
    currencySymbol: String,
    onAmountChange: (String) -> Unit,
    onToggleSign: () -> Unit,
    onEditName: () -> Unit,
    onDelete: () -> Unit
) {
    val isNegative = amountStr.trim().startsWith("-")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Field Name with edit icon & delete icon
        Row(
            modifier = Modifier.weight(1.15f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Delete button for this field
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(26.dp)
                    .testTag("delete_field_${name.lowercase().replace(" ", "_")}")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete $name",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(15.dp)
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Tappable name with edit pencil
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onEditName)
                    .padding(vertical = 4.dp, horizontal = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit $name",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // "+/-" Sign Toggle Button (critical for easy negative input on any Android soft keyboard)
        Surface(
            onClick = onToggleSign,
            shape = RoundedCornerShape(8.dp),
            color = if (isNegative) CashOutRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
            border = if (isNegative) androidx.compose.foundation.BorderStroke(1.dp, CashOutRed) else null,
            modifier = Modifier
                .height(38.dp)
                .width(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (isNegative) "–" else "+",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isNegative) CashOutRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Amount Input Field
        OutlinedTextField(
            value = amountStr,
            onValueChange = { input ->
                // Allow digits, decimal dot, and optional leading minus
                val filtered = input.filterIndexed { idx, c ->
                    c.isDigit() || c == '.' || (c == '-' && idx == 0)
                }
                onAmountChange(filtered)
            },
            placeholder = { Text("0.00", fontSize = 13.sp) },
            prefix = {
                Text(
                    text = currencySymbol,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = if (isNegative) CashOutRed else MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = if (isNegative) CashOutRed else MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .weight(1.35f)
                .height(52.dp)
                .testTag("cash_input_${name.lowercase().replace(" ", "_")}")
        )
    }
}

/**
 * Card displaying an in-memory snapshot comparing positions at a specific time
 */
@Composable
private fun SnapshotItemCard(
    snapshot: PositionSnapshot,
    currencySymbol: String,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val isShortfall = snapshot.difference < -0.001
    val isExtra = snapshot.difference > 0.001

    val statusColor = when {
        isShortfall -> CashOutRed
        isExtra -> CashInGreen
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = snapshot.timeLabel,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Row {
                    TextButton(
                        onClick = onRestore,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Restore",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Load", fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete snapshot",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Physical Cash", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatCurrency(currencySymbol, snapshot.physicalTotal),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Column {
                    Text(text = "Account Cash", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatCurrency(currencySymbol, snapshot.accountTotal),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = when {
                            isShortfall -> "Shortfall"
                            isExtra -> "Extra (Surplus)"
                            else -> "Balanced"
                        },
                        fontSize = 11.sp,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when {
                            isShortfall -> "-${formatCurrency(currencySymbol, abs(snapshot.difference))}"
                            isExtra -> "+${formatCurrency(currencySymbol, abs(snapshot.difference))}"
                            else -> formatCurrency(currencySymbol, 0.0)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = statusColor
                    )
                }
            }
        }
    }
}

/**
 * Helper to toggle negative / positive sign on amount string
 */
private fun toggleNegativeSign(str: String): String {
    val trimmed = str.trim()
    return if (trimmed.startsWith("-")) {
        trimmed.removePrefix("-").trim()
    } else {
        if (trimmed.isEmpty()) "-" else "-$trimmed"
    }
}

/**
 * Formats a currency amount with standard commas and two decimal places
 */
private fun formatCurrency(symbol: String, amount: Double): String {
    return "$symbol ${String.format(Locale.US, "%,.2f", amount)}"
}
