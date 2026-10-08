package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.io.GoogleDriveBackupManager
import com.example.ui.theme.CashInGreen
import com.example.ui.viewmodel.LedgerViewModel
import kotlinx.coroutines.launch

@Composable
fun GoogleDriveBackupDialog(
    viewModel: LedgerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var accountEmail by remember {
        mutableStateOf(GoogleDriveBackupManager.getConnectedAccount(context))
    }
    var isEditingAccount by remember { mutableStateOf(false) }
    var accountInput by remember { mutableStateOf(accountEmail) }

    var lastBackupSummary by remember {
        mutableStateOf(GoogleDriveBackupManager.getLastBackupSummary(context))
    }

    var driveFolder by remember {
        mutableStateOf(GoogleDriveBackupManager.getDriveFolderName(context))
    }
    var isEditingFolder by remember { mutableStateOf(false) }
    var folderInput by remember { mutableStateOf(driveFolder) }

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessStatus by remember { mutableStateOf(true) }

    // Confirm restore state
    var pendingRestoreResult by remember { mutableStateOf<com.example.data.io.BackupResult?>(null) }
    var showRestoreDetailsDialog by remember { mutableStateOf(false) }
    var showSecondConfirmDialog by remember { mutableStateOf(false) }

    // File import launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                statusMessage = "Inspecting backup file..."
                val result = GoogleDriveBackupManager.importFromLocalUri(context, uri)
                isLoading = false
                if (result.success && result.books.isNotEmpty()) {
                    pendingRestoreResult = result
                    showRestoreDetailsDialog = true
                } else {
                    statusMessage = result.message
                    isSuccessStatus = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Google Drive Backup", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Secure cloud sync & restore", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Google Account Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Google Account",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            IconButton(
                                onClick = { isEditingAccount = !isEditingAccount },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Account", modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (isEditingAccount) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = accountInput,
                                    onValueChange = { accountInput = it },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (accountInput.isNotBlank()) {
                                            accountEmail = accountInput.trim()
                                            GoogleDriveBackupManager.setConnectedAccount(context, accountEmail)
                                            isEditingAccount = false
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Set")
                                }
                            }
                        } else {
                            Text(
                                text = accountEmail,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Connected to Google Drive (OAuth Authorized)",
                                fontSize = 11.sp,
                                color = CashInGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 1.5. Google Drive Destination Folder Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().testTag("drive_destination_folder_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Drive Destination Folder",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            TextButton(
                                onClick = {
                                    isEditingFolder = !isEditingFolder
                                    folderInput = driveFolder
                                },
                                modifier = Modifier.testTag("btn_change_drive_folder")
                            ) {
                                Text(if (isEditingFolder) "Cancel" else "Change Folder", fontSize = 12.sp)
                            }
                        }

                        if (isEditingFolder) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = folderInput,
                                    onValueChange = { folderInput = it },
                                    label = { Text("Folder Name", fontSize = 11.sp) },
                                    placeholder = { Text("e.g. CashBook Records") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("input_drive_folder_name")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val clean = folderInput.trim().ifBlank { com.example.data.preferences.AppPreferencesManager.DEFAULT_DRIVE_FOLDER }
                                        driveFolder = clean
                                        viewModel.setGoogleDriveFolderName(clean)
                                        isEditingFolder = false
                                        statusMessage = "Destination folder changed to '$clean'"
                                        isSuccessStatus = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_save_drive_folder")
                                ) {
                                    Text("Save")
                                }
                            }
                        } else {
                            Text(
                                text = "📁 $driveFolder",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "All syncs and backups go directly to this Google Drive folder",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 2. Backup Status Summary
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Last Drive Backup", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(lastBackupSummary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Status message alert
                if (statusMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSuccessStatus) CashInGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusMessage ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSuccessStatus) CashInGreen else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Processing Google Drive sync...", fontSize = 13.sp)
                    }
                }

                // 3. Primary Google Drive Actions
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            statusMessage = "Uploading backup to Google Drive ($driveFolder)..."
                            val books = viewModel.allBooks.value
                            val txs = viewModel.getAllTransactionsDirect()
                            val result = GoogleDriveBackupManager.backupToGoogleDrive(context, books, txs, driveFolder)
                            isLoading = false
                            statusMessage = result.message
                            isSuccessStatus = result.success
                            if (result.success) {
                                lastBackupSummary = GoogleDriveBackupManager.getLastBackupSummary(context)
                                Toast.makeText(context, "Google Drive backup complete in '$driveFolder'!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("backup_to_drive_button")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync to Drive ($driveFolder)", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            statusMessage = "Fetching backup from Google Drive ($driveFolder)..."
                            val result = GoogleDriveBackupManager.restoreFromGoogleDrive(context, driveFolder)
                            isLoading = false
                            if (result.success && result.books.isNotEmpty()) {
                                pendingRestoreResult = result
                                showRestoreDetailsDialog = true
                            } else {
                                statusMessage = result.message
                                isSuccessStatus = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("restore_from_drive_button")
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore from Drive ($driveFolder)", fontWeight = FontWeight.SemiBold)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 4. Offline / Local JSON File Actions
                Text("Offline / Local Backup Options", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                val books = viewModel.allBooks.value
                                val txs = viewModel.getAllTransactionsDirect()
                                val file = GoogleDriveBackupManager.exportToLocalFile(context, books, txs)
                                isLoading = false
                                statusMessage = "Exported backup to: ${file.name}"
                                isSuccessStatus = true
                                Toast.makeText(context, "Exported ${file.name}", Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export JSON", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch("application/json")
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import JSON", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Close")
            }
        }
    )

    val currentPending = pendingRestoreResult

    // Confirmation 1 of 2: Detailed Restore Preview Dialog
    if (showRestoreDetailsDialog && currentPending != null) {
        val res = currentPending
        val inSum = res.transactions.filter { it.type == com.example.data.model.TransactionType.CASH_IN }.sumOf { it.amount }
        val outSum = res.transactions.filter { it.type == com.example.data.model.TransactionType.CASH_OUT }.sumOf { it.amount }
        val net = inSum - outSum

        AlertDialog(
            onDismissRequest = {
                showRestoreDetailsDialog = false
                pendingRestoreResult = null
            },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Details (Confirm 1/2)", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "The following backup content was found. Please review the details carefully before proceeding:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Connected Account:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(accountEmail, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Ledger Books:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${res.books.size} Books", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = res.books.joinToString(", ") { it.name },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Transactions:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${res.transactions.size} entries", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Cash In:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format(java.util.Locale.US, "%,.2f", inSum), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CashInGreen)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Cash Out:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format(java.util.Locale.US, "%,.2f", outSum), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Net Balance:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format(java.util.Locale.US, "%,.2f", net), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    Text(
                        text = "Step 1 of 2: Confirm you wish to proceed to the final restore authorization.",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDetailsDialog = false
                        showSecondConfirmDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Proceed to Final Confirmation")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreDetailsDialog = false
                        pendingRestoreResult = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation 2 of 2: Warning & Overwrite Authorization
    if (showSecondConfirmDialog && currentPending != null) {
        val res = currentPending
        AlertDialog(
            onDismissRequest = {
                showSecondConfirmDialog = false
                pendingRestoreResult = null
            },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Final Confirmation (2/2)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "CRITICAL WARNING: Restoring will completely overwrite and replace all current local data with the ${res.books.size} books and ${res.transactions.size} entries from this backup.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Text(
                        text = "Are you absolutely sure you want to proceed with overwriting your data? This action cannot be reversed.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSecondConfirmDialog = false
                        pendingRestoreResult = null
                        scope.launch {
                            isLoading = true
                            viewModel.restoreAllData(res.books, res.transactions)
                            statusMessage = "Successfully restored ${res.books.size} books and ${res.transactions.size} entries!"
                            isSuccessStatus = true
                            isLoading = false
                            Toast.makeText(context, "Restore completed successfully!", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Overwrite & Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSecondConfirmDialog = false
                        pendingRestoreResult = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
