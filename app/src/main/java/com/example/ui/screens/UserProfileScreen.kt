package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Palette
import coil.compose.AsyncImage
import com.example.data.cloud.CloudStorageManager
import com.example.data.io.BackupResult
import com.example.data.preferences.ModernPalette
import com.example.data.preferences.ThemeMode
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import com.example.ui.viewmodel.LedgerViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val currentModernPalette by viewModel.modernPalette.collectAsStateWithLifecycle()
    val driveAccountEmail by viewModel.driveAccountEmail.collectAsStateWithLifecycle()
    val isAccountConnected by viewModel.isAccountConnected.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val lastBackupSummary by viewModel.lastBackupSummary.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val profilePhotoPath by viewModel.profilePhotoPath.collectAsStateWithLifecycle()
    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()
    val googleDriveFolderName by viewModel.googleDriveFolderName.collectAsStateWithLifecycle()

    // Dialog states
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showConnectGoogleDialog by remember { mutableStateOf(false) }
    var showDisconnectConfirmDialog by remember { mutableStateOf(false) }
    var showChangeDriveFolderDialog by remember { mutableStateOf(false) }
    var driveFolderInput by remember { mutableStateOf("") }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }
    var showBackupFileContentDialog by remember { mutableStateOf(false) }
    var backupFileContentText by remember { mutableStateOf("") }

    var editNameInput by remember { mutableStateOf("") }
    var editEmailInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessStatus by remember { mutableStateOf(true) }

    // JSON Backup file picker & restore states (with 2-confirmation safeguard)
    var pendingRestoreResult by remember { mutableStateOf<BackupResult?>(null) }
    var restoreSourceDescription by remember { mutableStateOf("") }
    var showSourceSelectDialog by remember { mutableStateOf(false) }
    var showFirstRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showSecondRestoreConfirmDialog by remember { mutableStateOf(false) }
    var availableSyncedFile by remember { mutableStateOf<File?>(null) }

    // JSON file picker launcher (OpenDocument contract for picking JSON backup)
    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val result = CloudStorageManager.parseBackupFromUri(context, uri)
                    if (result.success && result.books.isNotEmpty()) {
                        pendingRestoreResult = result
                        val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "backup.json"
                        restoreSourceDescription = "Selected JSON File ($fileName)"
                        showFirstRestoreConfirmDialog = true
                    } else {
                        statusMessage = "Could not load backup from file: ${result.message.ifBlank { "Invalid or empty backup file" }}"
                        isSuccessStatus = false
                    }
                } catch (e: Exception) {
                    statusMessage = "Failed to open backup file: ${e.localizedMessage}"
                    isSuccessStatus = false
                }
            }
        }
    }

    // Photo picker launcher (Android Photo Picker, zero-permission)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val avatarFile = File(context.filesDir, "user_avatar_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(avatarFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    viewModel.updateProfilePhoto(avatarFile.absolutePath)
                    statusMessage = "Profile picture updated successfully!"
                    isSuccessStatus = true
                } catch (e: Exception) {
                    statusMessage = "Failed to save profile picture: ${e.localizedMessage}"
                    isSuccessStatus = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "User Profile & Account",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("user_profile_top_bar")
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Status message banner if any
            if (statusMessage != null) {
                item {
                    StatusBanner(
                        message = statusMessage!!,
                        isSuccess = isSuccessStatus,
                        onDismiss = { statusMessage = null }
                    )
                }
            }

            // 2. User Profile Header with Picture & Quick Stats
            item {
                UserProfileHeaderCard(
                    userName = userName,
                    email = driveAccountEmail,
                    isAccountConnected = isAccountConnected,
                    profilePhotoPath = profilePhotoPath,
                    totalBooks = allBooks.size,
                    totalTransactions = allTransactions.size,
                    onPhotoClick = { showPhotoOptionsDialog = true },
                    onEditClick = {
                        editNameInput = userName
                        editEmailInput = driveAccountEmail
                        showEditProfileDialog = true
                    },
                    onConnectClick = {
                        editNameInput = userName
                        editEmailInput = driveAccountEmail
                        showConnectGoogleDialog = true
                    }
                )
            }

            // 3. Google Account & Cloud Persistence Vault
            item {
                GoogleCloudSyncCard(
                    email = driveAccountEmail,
                    isConnected = isAccountConnected,
                    isSyncing = isCloudSyncing,
                    lastBackupSummary = lastBackupSummary,
                    onConnectClick = {
                        editNameInput = userName
                        editEmailInput = driveAccountEmail
                        showConnectGoogleDialog = true
                    },
                    onSyncNow = {
                        if (isAccountConnected) {
                            viewModel.triggerAutoSync()
                            Toast.makeText(context, "Cloud sync in progress...", Toast.LENGTH_SHORT).show()
                        } else {
                            showConnectGoogleDialog = true
                        }
                    },
                    onDisconnectClick = { showDisconnectConfirmDialog = true },
                    onSearchCloudVault = {
                        if (driveAccountEmail.isNotBlank()) {
                            scope.launch {
                                val cloudData = CloudStorageManager.fetchCloudData(context, driveAccountEmail)
                                if (cloudData.success && cloudData.books.isNotEmpty()) {
                                    pendingRestoreResult = cloudData
                                    restoreSourceDescription = "Google Drive AppData Cloud Vault ($driveAccountEmail)"
                                    showFirstRestoreConfirmDialog = true
                                } else {
                                    statusMessage = "No existing records found in cloud for $driveAccountEmail. All new records will sync automatically."
                                    isSuccessStatus = false
                                }
                            }
                        } else {
                            showConnectGoogleDialog = true
                        }
                    }
                )
            }

            // 3.4 Google Drive Custom Destination Folder Card
            item {
                GoogleDriveFolderSyncCard(
                    folderName = googleDriveFolderName,
                    isAccountConnected = isAccountConnected,
                    email = driveAccountEmail,
                    onChangeFolderClick = {
                        driveFolderInput = googleDriveFolderName
                        showChangeDriveFolderDialog = true
                    },
                    onSyncToFolderClick = {
                        if (isAccountConnected) {
                            scope.launch {
                                viewModel.performGoogleDriveBackup(context, googleDriveFolderName)
                                Toast.makeText(context, "Synced to Google Drive folder '$googleDriveFolderName'!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            showConnectGoogleDialog = true
                        }
                    }
                )
            }

            // 3.5 Backup File Location & Direct Download Card (User explicit requirement)
            val activeBackupEmail = if (driveAccountEmail.isNotBlank()) driveAccountEmail else "cashbook_user"
            item {
                BackupFileLocationCard(
                    email = activeBackupEmail,
                    isAccountConnected = isAccountConnected,
                    onDownloadShare = {
                        val uri = com.example.data.cloud.CloudStorageManager.getShareableBackupUri(context, activeBackupEmail)
                        if (uri != null) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "CashBook Cloud Backup - $activeBackupEmail")
                                putExtra(Intent.EXTRA_TEXT, "Here is my CashBook records cloud backup for $activeBackupEmail.")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Download / Save Backup File"))
                        } else {
                            Toast.makeText(context, "No backup file found yet. Tap 'Sync Now' first.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCopyPath = { path ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Backup Path", path))
                        Toast.makeText(context, "Backup path copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onViewContent = {
                        val file = com.example.data.cloud.CloudStorageManager.getShareableBackupFile(context, activeBackupEmail)
                        if (file != null && file.exists()) {
                            backupFileContentText = file.readText()
                            showBackupFileContentDialog = true
                        } else {
                            Toast.makeText(context, "Backup file not generated yet. Tap 'Sync Now' first.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onUploadRestoreJson = {
                        val synced = CloudStorageManager.getExistingSyncedDownloadsBackupFile(context, activeBackupEmail)
                        if (synced != null && synced.exists()) {
                            availableSyncedFile = synced
                            showSourceSelectDialog = true
                        } else {
                            try {
                                jsonFilePickerLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error opening file picker: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            // 4. Material 3 Expressive Theme Switcher Card
            item {
                ThemeSwitcherCard(
                    currentThemeMode = currentThemeMode,
                    currentModernPalette = currentModernPalette,
                    onThemeSelected = { mode ->
                        viewModel.setThemeMode(mode)
                        val modeName = when (mode) {
                            ThemeMode.LIGHT -> "Light Mode"
                            ThemeMode.DARK -> "Dark Mode"
                            ThemeMode.SYSTEM -> "System Default"
                        }
                        Toast.makeText(context, "$modeName enabled", Toast.LENGTH_SHORT).show()
                    },
                    onPaletteSelected = { palette ->
                        viewModel.setModernPalette(palette)
                        Toast.makeText(context, "${palette.title} color scheme applied", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 5. Regional Preferences & Currency Card
            item {
                AppPreferencesCard(
                    defaultCurrency = defaultCurrency,
                    onChangeCurrency = { showCurrencyDialog = true }
                )
            }

            // 6. Developer Credit Footer (User Explicit Request)
            item {
                DeveloperCreditFooterCard(
                    onOpenLinkedIn = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/arikmdisthiaque/")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open browser: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }

    // Dialog: Photo Options (Pick New / Remove)
    if (showPhotoOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoOptionsDialog = false },
            title = {
                Text(
                    text = "Profile Picture",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Customize your profile avatar across the app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            showPhotoOptionsDialog = false
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_choose_photo")
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose from Gallery")
                    }
                    if (profilePhotoPath.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.removeProfilePhoto()
                                showPhotoOptionsDialog = false
                                Toast.makeText(context, "Profile photo removed", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CashOutRed),
                            modifier = Modifier.fillMaxWidth().testTag("btn_remove_photo")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remove Photo")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoOptionsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: Connect Google Account (First Time or Switch)
    if (showConnectGoogleDialog) {
        var emailInput by remember { mutableStateOf(driveAccountEmail) }
        var nameInput by remember { mutableStateOf(userName) }
        var isConnecting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isConnecting) showConnectGoogleDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Connect Google Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your Google account email to link your isolated Google Drive AppData folder. All transactions, ledger books, and custom categories will mirror securely to your Drive AppData storage.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Google Account Email") },
                        placeholder = { Text("example@gmail.com") },
                        leadingIcon = {
                            Icon(Icons.Default.CloudQueue, contentDescription = null)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_connect_google_email")
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Your Name (Optional)") },
                        placeholder = { Text("e.g. Arik") },
                        leadingIcon = {
                            Icon(Icons.Default.AccountCircle, contentDescription = null)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_connect_google_name")
                    )

                    if (isConnecting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Searching Cloud Vault for records...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedEmail = emailInput.trim()
                        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
                            Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isConnecting = true
                        viewModel.connectGoogleAccount(
                            email = trimmedEmail,
                            name = nameInput.trim(),
                            onConfirmCloudRestore = { cloudResult ->
                                pendingRestoreResult = cloudResult
                                restoreSourceDescription = "Cloud Vault for $trimmedEmail"
                                showFirstRestoreConfirmDialog = true
                            },
                            onComplete = { foundData, message ->
                                isConnecting = false
                                showConnectGoogleDialog = false
                                statusMessage = message
                                isSuccessStatus = true
                            }
                        )
                    },
                    enabled = !isConnecting,
                    modifier = Modifier.testTag("btn_confirm_connect_google")
                ) {
                    Text(if (isConnecting) "Connecting..." else "Connect & Sync")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConnectGoogleDialog = false },
                    enabled = !isConnecting
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Edit Profile Name
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(
                    text = "Edit Profile Info",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Update your display name.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text("Display Name") },
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_edit_name")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameInput.isNotBlank()) {
                            viewModel.setUserName(editNameInput.trim())
                        }
                        showEditProfileDialog = false
                        Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("btn_save_edit_name")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Disconnect Confirm
    if (showDisconnectConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirmDialog = false },
            icon = {
                Icon(Icons.Default.CloudOff, contentDescription = null, tint = CashOutRed, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Disconnect Account?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Cloud auto-sync will pause. Your existing records are safely preserved in the cloud directory and can be reloaded at any time by connecting your email again.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.disconnectAccount()
                        showDisconnectConfirmDialog = false
                        Toast.makeText(context, "Google account disconnected", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CashOutRed),
                    modifier = Modifier.testTag("btn_confirm_disconnect")
                ) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Currency Switcher
    if (showCurrencyDialog) {
        val currencies = listOf(
            "USD ($)" to "USD",
            "EUR (€)" to "EUR",
            "GBP (£)" to "GBP",
            "INR (₹)" to "INR",
            "BDT (৳)" to "BDT",
            "JPY (¥)" to "JPY",
            "CAD ($)" to "CAD",
            "AUD ($)" to "AUD",
            "AED (د.إ)" to "AED",
            "SAR (﷼)" to "SAR",
            "SGD ($)" to "SGD",
            "MYR (RM)" to "MYR"
        )
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Default Currency", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(currencies.size) { index ->
                        val (label, code) = currencies[index]
                        val isSelected = defaultCurrency.equals(code, ignoreCase = true) || defaultCurrency.equals(label, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setDefaultCurrency(code)
                                    showCurrencyDialog = false
                                    Toast.makeText(context, "Currency set to $code", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: Change Google Drive Destination Folder
    if (showChangeDriveFolderDialog) {
        val suggestedFolders = listOf(
            "CashBook Records",
            "Financial Vault",
            "Personal Ledger",
            "Business Finances",
            "Cash Accounts"
        )
        AlertDialog(
            onDismissRequest = { showChangeDriveFolderDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DriveFolderUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Google Drive Sync Folder",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Specify the exact Google Drive folder where your finances, transactions, and backups should be stored. You can change this at any time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = driveFolderInput,
                        onValueChange = { driveFolderInput = it },
                        label = { Text("Google Drive Folder Name") },
                        placeholder = { Text("e.g. CashBook Records") },
                        leadingIcon = {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (driveFolderInput.isNotBlank()) {
                                IconButton(onClick = { driveFolderInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_drive_folder")
                    )

                    Text(
                        text = "Quick Presets:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestedFolders.forEach { folder ->
                            val isSelected = driveFolderInput.trim().equals(folder, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { driveFolderInput = folder }
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = folder,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "If the folder does not already exist in Google Drive, the app will automatically create it for you.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = driveFolderInput.trim().ifBlank { com.example.data.preferences.AppPreferencesManager.DEFAULT_DRIVE_FOLDER }
                        viewModel.setGoogleDriveFolderName(clean)
                        showChangeDriveFolderDialog = false
                        Toast.makeText(context, "Target folder updated to '$clean' & synced!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("btn_confirm_save_drive_folder")
                ) {
                    Text("Save & Sync")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showChangeDriveFolderDialog = false },
                    modifier = Modifier.testTag("btn_cancel_save_drive_folder")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Backup File Content Preview Dialog
    if (showBackupFileContentDialog) {
        AlertDialog(
            onDismissRequest = { showBackupFileContentDialog = false },
            title = {
                Text(
                    text = "Backup File Preview",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = backupFileContentText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, backupFileContentText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Backup Content"))
                    }
                ) {
                    Text("Share JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupFileContentDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: Select JSON Backup Source (Phone's Synced Downloads vs Browse Storage)
    if (showSourceSelectDialog) {
        AlertDialog(
            onDismissRequest = { showSourceSelectDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Restore JSON Backup",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Choose which backup file you want to restore from:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val synced = availableSyncedFile
                    if (synced != null && synced.exists()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSourceSelectDialog = false
                                    scope.launch {
                                        try {
                                            val text = synced.readText()
                                            val result = CloudStorageManager.parseBackupFromJsonString(text)
                                            if (result.success && result.books.isNotEmpty()) {
                                                pendingRestoreResult = result
                                                restoreSourceDescription = "Synced Downloads File (${synced.name})"
                                                showFirstRestoreConfirmDialog = true
                                            } else {
                                                statusMessage = "Could not parse backup from synced file: ${result.message}"
                                                isSuccessStatus = false
                                            }
                                        } catch (e: Exception) {
                                            statusMessage = "Error reading synced file: ${e.localizedMessage}"
                                            isSuccessStatus = false
                                        }
                                    }
                                }
                                .testTag("btn_select_synced_downloads_file")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Phone's Synced Downloads File",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Download/CashBookCloud/${synced.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSourceSelectDialog = false
                                try {
                                    jsonFilePickerLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error opening file picker: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .testTag("btn_select_upload_browse_file")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Browse Phone Storage (Upload JSON)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Select any .json backup from Downloads, Drive, or Files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSourceSelectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Confirmation 1 of 2 (Inspection and Primary Warning)
    if (showFirstRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showFirstRestoreConfirmDialog = false
                pendingRestoreResult = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(34.dp)
                )
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Restore Data from Backup?",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Step 1 of 2: Review & Warning",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Summary of backup contents
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Source: $restoreSourceDescription",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val bookCount = pendingRestoreResult?.books?.size ?: 0
                            val txCount = pendingRestoreResult?.transactions?.size ?: 0
                            val bookNames = pendingRestoreResult?.books?.take(4)?.joinToString(", ") { it.name } ?: ""
                            val moreBooks = if (bookCount > 4) " +${bookCount - 4} more" else ""
                            Text(
                                text = "• Books: $bookCount ($bookNames$moreBooks)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• Transactions: $txCount records",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val email = pendingRestoreResult?.metadata?.accountEmail
                            if (!email.isNullOrBlank()) {
                                Text(
                                    text = "• Account: $email",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val exportDate = pendingRestoreResult?.metadata?.exportDate
                            if (!exportDate.isNullOrBlank() && exportDate != "Unknown date") {
                                Text(
                                    text = "• Exported: $exportDate",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Warning Container
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "WARNING: Restoring will completely OVERWRITE and REPLACE all your existing local ledger books, categories, and entries currently on this phone.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Text(
                        text = "Step 1 of 2: Please verify the backup details above before continuing to the final authorization step.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFirstRestoreConfirmDialog = false
                        showSecondRestoreConfirmDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("btn_proceed_to_second_confirm")
                ) {
                    Text("Proceed to Final Confirmation (1/2)")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showFirstRestoreConfirmDialog = false
                        pendingRestoreResult = null
                    },
                    modifier = Modifier.testTag("btn_cancel_first_confirm")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Confirmation 2 of 2 (Critical Final Overwrite Authorization)
    if (showSecondRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showSecondRestoreConfirmDialog = false
                pendingRestoreResult = null
            },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false),
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⚠️ FINAL CONFIRMATION",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Step 2 of 2: Irreversible Overwrite",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "CRITICAL WARNING: This action CANNOT be reversed!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            val bCount = pendingRestoreResult?.books?.size ?: 0
                            val tCount = pendingRestoreResult?.transactions?.size ?: 0
                            Text(
                                text = "All existing books and entries currently on this phone will be completely wiped out and replaced with $bCount books and $tCount transactions from $restoreSourceDescription.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Text(
                        text = "Are you ABSOLUTELY sure you want to completely overwrite all your data now?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val res = pendingRestoreResult
                        showSecondRestoreConfirmDialog = false
                        pendingRestoreResult = null
                        if (res != null) {
                            scope.launch {
                                viewModel.restoreAllData(res.books, res.transactions)
                                statusMessage = "Successfully restored ${res.books.size} books and ${res.transactions.size} records from $restoreSourceDescription!"
                                isSuccessStatus = true
                                Toast.makeText(context, "Data restored successfully!", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_final_confirm_overwrite_restore")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Yes, Overwrite & Restore")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showSecondRestoreConfirmDialog = false
                        pendingRestoreResult = null
                    },
                    modifier = Modifier.testTag("btn_cancel_final_confirm")
                ) {
                    Text("Cancel (Keep My Data)")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// Component 1: User Profile Header Card with Picture
// -------------------------------------------------------------
@Composable
private fun UserProfileHeaderCard(
    userName: String,
    email: String,
    isAccountConnected: Boolean,
    profilePhotoPath: String,
    totalBooks: Int,
    totalTransactions: Int,
    onPhotoClick: () -> Unit,
    onEditClick: () -> Unit,
    onConnectClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_profile_header_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar with Photo or Fallback Initial
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPhotoClick)
                        .testTag("profile_avatar_box"),
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePhotoPath.isNotBlank() && File(profilePhotoPath).exists()) {
                        AsyncImage(
                            model = File(profilePhotoPath),
                            contentDescription = "User Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val initial = if (userName.isNotBlank()) {
                                userName.first().uppercase()
                            } else if (email.isNotBlank()) {
                                email.first().uppercase()
                            } else "U"
                            Text(
                                text = initial,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Camera Icon Overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Edit photo",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Profile Identity & Email
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (userName.isNotBlank()) userName else if (email.isNotBlank()) email.substringBefore("@") else "CashBook User",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isAccountConnected) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified account",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = if (email.isNotBlank()) email else "No Google account connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Connection Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAccountConnected) CashInGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isAccountConnected) CashInGreen else MaterialTheme.colorScheme.outline)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAccountConnected) "Cloud Auto-Sync Active" else "Local Only (Tap to Connect)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAccountConnected) CashInGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(14.dp))

            // Stats & Action buttons (Responsive)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Total Books Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = totalBooks.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (totalBooks == 1) "Ledger Book" else "Ledger Books",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Total Records Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = totalTransactions.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = if (totalTransactions == 1) "Entry Recorded" else "Entries Recorded",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Edit Button
                OutlinedButton(
                    onClick = if (isAccountConnected) onEditClick else onConnectClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("btn_edit_profile_header")
                ) {
                    Icon(
                        imageVector = if (isAccountConnected) Icons.Default.Edit else Icons.Default.CloudSync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isAccountConnected) "Edit" else "Sign In", fontSize = 13.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component 2: Google Account & Cloud Persistence Vault
// -------------------------------------------------------------
@Composable
private fun GoogleCloudSyncCard(
    email: String,
    isConnected: Boolean,
    isSyncing: Boolean,
    lastBackupSummary: String,
    onConnectClick: () -> Unit,
    onSyncNow: () -> Unit,
    onDisconnectClick: () -> Unit,
    onSearchCloudVault: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("google_cloud_sync_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Google Drive AppData Vault",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Isolated Cloud Storage • Survives Cache Clear",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Explanation & Sync Status
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
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
                                imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isConnected) CashInGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isConnected) "Connected: $email" else "No Google Account Linked",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isConnected) {
                            "All transactions, ledger books, and custom categories automatically mirror to your Google Drive AppData folder in real-time. If you wipe cache/storage in Android Settings, signing in here restores all your records directly from Google Drive AppData."
                        } else {
                            "Connect your Google account so that every transaction is safeguarded to Google Drive AppData folder. You will never lose your financial logs."
                        },
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (lastBackupSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Vault Status: $lastBackupSummary",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            if (isConnected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSyncNow,
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_sync_now")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync Now", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onSearchCloudVault,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_check_vault")
                    ) {
                        Text("Check Vault", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onConnectClick,
                        modifier = Modifier.testTag("btn_switch_account")
                    ) {
                        Text("Switch Account", fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = onDisconnectClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = CashOutRed),
                        modifier = Modifier.testTag("btn_disconnect_account")
                    ) {
                        Text("Disconnect", fontSize = 12.sp)
                    }
                }
            } else {
                Button(
                    onClick = onConnectClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_connect_google_account")
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect Google Account & Restore Data", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component 2.5: Backup File Location & Direct Download Card
// -------------------------------------------------------------
@Composable
private fun BackupFileLocationCard(
    email: String,
    isAccountConnected: Boolean,
    onDownloadShare: () -> Unit,
    onCopyPath: (String) -> Unit,
    onViewContent: () -> Unit,
    onUploadRestoreJson: () -> Unit
) {
    val relativePath = com.example.data.cloud.CloudStorageManager.getPublicStoragePath(email)
    val fullPath = "/storage/emulated/0/$relativePath"

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("backup_file_location_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Backup File & JSON Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Permanent Downloads Storage • Direct Upload & Restore",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Storage Path",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { onCopyPath(fullPath) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Path",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = relativePath,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "This backup file is stored directly in your device's public Downloads directory. Even if you clear app cache and storage in Android Settings, this file is preserved. You can upload any JSON backup or restore from this file anytime with 2-step verification.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action 1: Upload & Restore JSON Backup (Prominent primary button)
            Button(
                onClick = onUploadRestoreJson,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_upload_restore_backup")
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload & Restore JSON File", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action 2: Download / Export and Preview buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDownloadShare,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_download_backup_file")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download File", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = onViewContent,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_view_backup_json")
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preview JSON", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component 3: Theme Switcher Card (Material 3 Expressive)
// -------------------------------------------------------------
@Composable
private fun ThemeSwitcherCard(
    currentThemeMode: ThemeMode,
    currentModernPalette: ModernPalette,
    onThemeSelected: (ThemeMode) -> Unit,
    onPaletteSelected: (ModernPalette) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("theme_switcher_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "App Theme & Modern Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${currentModernPalette.title} • Modern Fintech Styling",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Segmented Theme Mode Options (Dark / Light / System)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOptionButton(
                    title = "Dark",
                    icon = Icons.Default.DarkMode,
                    isSelected = currentThemeMode == ThemeMode.DARK,
                    onClick = { onThemeSelected(ThemeMode.DARK) },
                    modifier = Modifier.weight(1f)
                )

                ThemeOptionButton(
                    title = "Light",
                    icon = Icons.Default.LightMode,
                    isSelected = currentThemeMode == ThemeMode.LIGHT,
                    onClick = { onThemeSelected(ThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f)
                )

                ThemeOptionButton(
                    title = "System",
                    icon = Icons.Default.BrightnessAuto,
                    isSelected = currentThemeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeSelected(ThemeMode.SYSTEM) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(16.dp))

            // 2. Modern Color Scheme Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Modern Color Scheme",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = currentModernPalette.title,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Modern Palettes List
            val palettes = listOf(
                PaletteOption(
                    palette = ModernPalette.EMERALD_MINT,
                    primaryColor = Color(0xFF10B981),
                    accentColor = Color(0xFF34D399),
                    tag = "palette_emerald_mint"
                ),
                PaletteOption(
                    palette = ModernPalette.CYBER_INDIGO,
                    primaryColor = Color(0xFF6366F1),
                    accentColor = Color(0xFF06B6D4),
                    tag = "palette_cyber_indigo"
                ),
                PaletteOption(
                    palette = ModernPalette.SUNSET_ROSE,
                    primaryColor = Color(0xFFF43F5E),
                    accentColor = Color(0xFFF59E0B),
                    tag = "palette_sunset_rose"
                ),
                PaletteOption(
                    palette = ModernPalette.TITANIUM_ICE,
                    primaryColor = Color(0xFF38BDF8),
                    accentColor = Color(0xFF94A3B8),
                    tag = "palette_titanium_ice"
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                palettes.forEach { opt ->
                    val isSelected = currentModernPalette == opt.palette
                    ModernPaletteRowItem(
                        option = opt,
                        isSelected = isSelected,
                        onClick = { onPaletteSelected(opt.palette) }
                    )
                }
            }
        }
    }
}

private data class PaletteOption(
    val palette: ModernPalette,
    val primaryColor: Color,
    val accentColor: Color,
    val tag: String
)

@Composable
private fun ModernPaletteRowItem(
    option: PaletteOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(option.tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Overlapping color preview circles
            Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.CenterStart) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(option.primaryColor)
                )
                Box(
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(option.accentColor)
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.palette.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = option.palette.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        modifier = modifier
            .height(52.dp)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("theme_btn_$title")
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// -------------------------------------------------------------
// Component 4: App Preferences & Currency Card
// -------------------------------------------------------------
@Composable
private fun AppPreferencesCard(
    defaultCurrency: String,
    onChangeCurrency: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_preferences_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Regional & Currency",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize default recording currency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onChangeCurrency)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Default Currency Code", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Applied to new books and entries", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = defaultCurrency,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Security statement
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = CashInGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Encrypted SQLite Storage with Automatic Cloud Mirroring",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component 5: Developer Credit Footer Card (User Requirement)
// -------------------------------------------------------------
@Composable
private fun DeveloperCreditFooterCard(
    onOpenLinkedIn: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenLinkedIn)
            .testTag("developer_credit_footer_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "developed by ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Arik Md Isthiaque",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = " with ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Love",
                    tint = CashOutRed,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Lead Software Architect & Product Designer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0A66C2)), // LinkedIn Blue
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "in",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "linkedin.com/in/arikmdisthiaque",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component 6: Status Banner
// -------------------------------------------------------------
@Composable
private fun StatusBanner(
    message: String,
    isSuccess: Boolean,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSuccess) CashInGreen.copy(alpha = 0.12f) else CashOutRed.copy(alpha = 0.12f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_status_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isSuccess) CashInGreen else CashOutRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSuccess) CashInGreen else CashOutRed
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(28.dp)
            ) {
                Text("Dismiss", fontSize = 11.sp, color = if (isSuccess) CashInGreen else CashOutRed)
            }
        }
    }
}

// -------------------------------------------------------------
// Component 2.3: Google Drive Custom Folder Sync Card
// -------------------------------------------------------------
@Composable
private fun GoogleDriveFolderSyncCard(
    folderName: String,
    isAccountConnected: Boolean,
    email: String,
    onChangeFolderClick: () -> Unit,
    onSyncToFolderClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("google_drive_folder_sync_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DriveFolderUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Google Drive Sync Folder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Custom destination in your Drive account",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target Drive Folder:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Active Sync Target",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = folderName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Backups, records, and auto-sync are directed to 'Google Drive > $folderName'. You can modify this target folder anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onChangeFolderClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_change_drive_folder_setting")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change Folder", fontSize = 13.sp)
                }

                Button(
                    onClick = onSyncToFolderClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_sync_to_drive_folder")
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync to Folder", fontSize = 13.sp)
                }
            }
        }
    }
}
