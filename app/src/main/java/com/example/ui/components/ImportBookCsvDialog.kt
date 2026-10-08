package com.example.ui.components

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.io.CsvExporterImporter
import com.example.data.io.ImportValidationSummary
import com.example.data.model.LedgerBook
import com.example.data.model.TransactionType
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import com.example.ui.viewmodel.LedgerViewModel
import java.io.ByteArrayInputStream

private val colorOptions = listOf(
    0xFF0D9488L, // Teal
    0xFF3B82F6L, // Blue
    0xFF8B5CF6L, // Purple
    0xFFF59E0BL, // Amber
    0xFF10B981L, // Emerald
    0xFFEC4899L, // Pink
    0xFFF97316L, // Orange
    0xFF64748BL  // Slate
)

@Composable
fun ImportBookCsvDialog(
    viewModel: LedgerViewModel,
    targetBook: LedgerBook? = null,
    onDismiss: () -> Unit,
    onImportSuccess: (Long) -> Unit
) {
    val context = LocalContext.current
    var validationSummary by remember { mutableStateOf<ImportValidationSummary?>(null) }
    var bookName by remember { mutableStateOf(targetBook?.name ?: "") }
    var selectedColor by remember { mutableLongStateOf(targetBook?.colorHex ?: 0xFF0D9488L) }
    var isNameError by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                isProcessing = true
                var extractedFileName = "Imported Ledger"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            extractedFileName = name.substringBeforeLast(".")
                            selectedFileName = name
                        }
                    }
                }

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val summary = CsvExporterImporter.parseAndValidateCsv(inputStream)
                    validationSummary = summary
                    if (targetBook == null) {
                        bookName = summary.suggestedBookName?.takeIf { it.isNotBlank() } ?: extractedFileName
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                isProcessing = false
            }
        }
    }

    fun loadSampleCsv() {
        isProcessing = true
        try {
            val sampleCsv = CsvExporterImporter.generateDemoTemplateCsv()
            val summary = CsvExporterImporter.parseAndValidateCsv(ByteArrayInputStream(sampleCsv.toByteArray()))
            validationSummary = summary
            selectedFileName = "sample_ledger_template.csv"
            if (targetBook == null) {
                bookName = "Sample Business Ledger"
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading sample: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        } finally {
            isProcessing = false
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isProcessing) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (targetBook != null) "Import CSV to '${targetBook.name}'" else "Import Book with CSV",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (validationSummary == null) {
                    // Step 1: Select or Load CSV
                    Text(
                        text = "Create a complete ledger book automatically by importing transactions from any CSV file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Supported CSV Columns:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "• Date (e.g. 2026-09-15, 2026-09-15 14:30, MM/dd/yyyy)\n• Type (Income / Cash In, Expense / Cash Out)\n• Amount (e.g. 150.00)\n• Category (e.g. Supplies, Salary, Food)\n• Memo / Notes (optional)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isProcessing) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("button_select_csv_file"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select CSV File from Storage")
                        }

                        OutlinedButton(
                            onClick = { loadSampleCsv() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("button_load_sample_csv"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Try with Sample CSV Template")
                        }
                    }
                } else {
                    // Step 2: Validate & Customize Book details
                    val summary = validationSummary!!

                    // File badge
                    if (selectedFileName != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedFileName!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Book Name input (if creating new book)
                    if (targetBook == null) {
                        OutlinedTextField(
                            value = bookName,
                            onValueChange = {
                                bookName = it
                                if (isNameError && it.isNotBlank()) isNameError = false
                            },
                            label = { Text("New Book Name") },
                            isError = isNameError,
                            supportingText = if (isNameError) {
                                { Text("Please enter a book name") }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_import_book_name")
                        )

                        // Theme Color Picker
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Book Theme Color",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                colorOptions.forEach { colorHex ->
                                    val isSelected = selectedColor == colorHex
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
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
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Summary Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Total Rows", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${summary.totalRows}", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = CashInGreen.copy(alpha = 0.15f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Ready to Import", fontSize = 11.sp, color = CashInGreen)
                                Text("${summary.validRows.size}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = CashInGreen)
                            }
                        }
                        if (summary.invalidRows.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = CashOutRed.copy(alpha = 0.15f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Errors", fontSize = 11.sp, color = CashOutRed)
                                    Text("${summary.invalidRows.size}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = CashOutRed)
                                }
                            }
                        }
                    }

                    // Validation warnings if any
                    if (summary.invalidRows.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CashOutRed.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Skipped Rows (${summary.invalidRows.size})", fontWeight = FontWeight.Bold, color = CashOutRed, fontSize = 12.sp)
                                summary.invalidRows.take(3).forEach { inv ->
                                    Text("Line ${inv.lineNumber}: ${inv.errors.firstOrNull() ?: "Format error"}", fontSize = 11.sp, color = CashOutRed)
                                }
                            }
                        }
                    }

                    // Preview of valid transactions
                    if (summary.validRows.isNotEmpty()) {
                        Text(
                            text = "Entries Preview (${summary.validRows.size} total)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            summary.validRows.take(4).forEach { row ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (row.type == TransactionType.CASH_IN) CashInGreen.copy(alpha = 0.15f) else CashOutRed.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = if (row.type == TransactionType.CASH_IN) "Cash In" else "Cash Out",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (row.type == TransactionType.CASH_IN) CashInGreen else CashOutRed,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = row.categoryStr,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text(
                                                        text = row.paymentMode,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            if (row.memoStr.isNotBlank()) {
                                                Text(
                                                    text = row.memoStr,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = row.dateStr,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }

                                        Text(
                                            text = "${if (row.type == TransactionType.CASH_IN) "+" else "-"}${row.amountStr}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (row.type == TransactionType.CASH_IN) CashInGreen else CashOutRed
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Button to pick a different file
                    TextButton(
                        onClick = {
                            validationSummary = null
                            selectedFileName = null
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Pick a different CSV file")
                    }
                }
            }
        },
        confirmButton = {
            if (validationSummary != null) {
                val summary = validationSummary!!
                val hasValid = summary.validRows.isNotEmpty()

                Button(
                    onClick = {
                        if (targetBook == null && bookName.trim().isBlank()) {
                            isNameError = true
                            return@Button
                        }
                        isProcessing = true
                        if (targetBook != null) {
                            // Import into existing book
                            viewModel.commitImportedRows(summary.validRows, targetBook.id)
                            onImportSuccess(targetBook.id)
                        } else {
                            // Create new book and import
                            viewModel.createBookAndImport(
                                bookName = bookName.trim(),
                                colorHex = selectedColor,
                                validRows = summary.validRows
                            ) { newBookId ->
                                onImportSuccess(newBookId)
                            }
                        }
                    },
                    enabled = hasValid && !isProcessing,
                    modifier = Modifier.testTag("button_confirm_csv_import")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        if (targetBook != null) "Import ${summary.validRows.size} Entries"
                        else "Create & Import Book (${summary.validRows.size})"
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) {
                Text("Cancel")
            }
        }
    )
}
