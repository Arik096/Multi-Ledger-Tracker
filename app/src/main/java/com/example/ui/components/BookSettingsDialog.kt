package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LedgerBook

private val LEDGER_COLOR_PALETTE = listOf(
    0xFF0D9488L, // Teal
    0xFF2563EBL, // Royal Blue
    0xFF7C3AEDL, // Deep Purple
    0xFFDB2777L, // Pink
    0xFFEA580CL, // Amber / Orange
    0xFF059669L, // Emerald
    0xFF4B5563L  // Slate
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookSettingsDialog(
    book: LedgerBook,
    allOtherBooks: List<LedgerBook>,
    onDismiss: () -> Unit,
    onSave: (updatedBook: LedgerBook) -> Unit
) {
    var bookName by remember { mutableStateOf(book.name) }
    var bookNameError by remember { mutableStateOf(false) }
    var selectedColorHex by remember { mutableStateOf(book.colorHex) }

    var expenseCategories by remember {
        mutableStateOf(book.getExpenseCategories().toMutableList())
    }
    var incomeCategories by remember {
        mutableStateOf(book.getIncomeCategories().toMutableList())
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Expense, 1: Income

    // New Category input
    var newCategoryText by remember { mutableStateOf("") }
    var duplicateCategoryError by remember { mutableStateOf<String?>(null) }

    // Import from another book
    var selectedSourceBook by remember {
        mutableStateOf(allOtherBooks.firstOrNull())
    }
    var showSourceBookDropdown by remember { mutableStateOf(false) }
    var importFeedbackMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        title = {
            Text(
                text = "Book Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Book Name
                OutlinedTextField(
                    value = bookName,
                    onValueChange = {
                        bookName = it
                        if (bookNameError) bookNameError = false
                    },
                    label = { Text("Book Name") },
                    isError = bookNameError,
                    supportingText = if (bookNameError) {
                        { Text("Book name cannot be empty") }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("book_settings_name_input")
                )

                // Color Selection
                Text(
                    text = "Color Accent",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LEDGER_COLOR_PALETTE.forEach { colorVal ->
                        val isSelected = selectedColorHex == colorVal
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .clickable { selectedColorHex = colorVal }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                        )
                    }
                }

                // Category Management Section
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            duplicateCategoryError = null
                        },
                        text = { Text("Expenses (${expenseCategories.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            duplicateCategoryError = null
                        },
                        text = { Text("Income (${incomeCategories.size})") }
                    )
                }

                // Add Category Input with Duplicate Check
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryText,
                        onValueChange = {
                            newCategoryText = it
                            duplicateCategoryError = null
                        },
                        label = { Text("Add Category") },
                        placeholder = { Text("e.g. Subscriptions") },
                        isError = duplicateCategoryError != null,
                        supportingText = duplicateCategoryError?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("book_settings_new_category_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val clean = newCategoryText.trim()
                            if (clean.isEmpty()) return@Button

                            val targetList = if (selectedTab == 0) expenseCategories else incomeCategories

                            // Check for duplicate category
                            if (targetList.any { it.equals(clean, ignoreCase = true) }) {
                                duplicateCategoryError = "Category already exists in this book"
                                return@Button
                            }

                            if (selectedTab == 0) {
                                expenseCategories = (expenseCategories + clean).toMutableList()
                            } else {
                                incomeCategories = (incomeCategories + clean).toMutableList()
                            }
                            newCategoryText = ""
                            duplicateCategoryError = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("book_settings_add_category_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }

                // List of current categories as chips
                val currentList = if (selectedTab == 0) expenseCategories else incomeCategories
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    currentList.forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove $cat",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            if (selectedTab == 0) {
                                                if (expenseCategories.size > 1) {
                                                    expenseCategories = expenseCategories.filter { it != cat }.toMutableList()
                                                }
                                            } else {
                                                if (incomeCategories.size > 1) {
                                                    incomeCategories = incomeCategories.filter { it != cat }.toMutableList()
                                                }
                                            }
                                        }
                                )
                            }
                        }
                    }
                }

                // Section: Import Categories from Another Book
                if (allOtherBooks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Import Categories from Another Book",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Select a book to import its categories into this book with automatic duplicate check.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            ExposedDropdownMenuBox(
                                expanded = showSourceBookDropdown,
                                onExpandedChange = { showSourceBookDropdown = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedSourceBook?.name ?: "Select Book",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Source Book") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSourceBookDropdown) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )

                                ExposedDropdownMenu(
                                    expanded = showSourceBookDropdown,
                                    onDismissRequest = { showSourceBookDropdown = false }
                                ) {
                                    allOtherBooks.forEach { src ->
                                        DropdownMenuItem(
                                            text = { Text(src.name) },
                                            onClick = {
                                                selectedSourceBook = src
                                                showSourceBookDropdown = false
                                                importFeedbackMessage = null
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    val src = selectedSourceBook ?: return@Button
                                    var addedCount = 0
                                    var skippedCount = 0

                                    if (selectedTab == 0) {
                                        val srcExpense = src.getExpenseCategories()
                                        srcExpense.forEach { cat ->
                                            if (expenseCategories.none { it.equals(cat, ignoreCase = true) }) {
                                                expenseCategories.add(cat)
                                                addedCount++
                                            } else {
                                                skippedCount++
                                            }
                                        }
                                        expenseCategories = expenseCategories.toMutableList()
                                    } else {
                                        val srcIncome = src.getIncomeCategories()
                                        srcIncome.forEach { cat ->
                                            if (incomeCategories.none { it.equals(cat, ignoreCase = true) }) {
                                                incomeCategories.add(cat)
                                                addedCount++
                                            } else {
                                                skippedCount++
                                            }
                                        }
                                        incomeCategories = incomeCategories.toMutableList()
                                    }

                                    importFeedbackMessage = "Added $addedCount new categories ($skippedCount duplicates skipped)"
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import ${if (selectedTab == 0) "Expense" else "Income"} Categories")
                            }

                            importFeedbackMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = bookName.trim()
                    if (cleanName.isEmpty()) {
                        bookNameError = true
                        return@Button
                    }
                    val updated = book.copy(
                        name = cleanName,
                        colorHex = selectedColorHex,
                        customExpenseCategories = expenseCategories.distinct().joinToString("||"),
                        customIncomeCategories = incomeCategories.distinct().joinToString("||")
                    )
                    onSave(updated)
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("book_settings_save_button")
            ) {
                Text("Save Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
