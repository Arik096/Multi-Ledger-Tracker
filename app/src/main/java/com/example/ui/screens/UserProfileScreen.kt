package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import coil.compose.AsyncImage
import com.example.data.cloud.CloudStorageManager
import com.example.data.io.BackupResult
import com.example.data.preferences.ModernPalette
import com.example.data.preferences.ThemeMode
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import com.example.ui.theme.ClashGroteskFont
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.AmountFormatter
import kotlinx.coroutines.launch
import java.io.File

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

    // Dialog states
    var showDisconnectConfirmDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessStatus by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "User Profile & Settings",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = ClashGroteskFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Account, Preferences & Cloud Sync",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = ClashGroteskFont,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (isAccountConnected) {
                                    viewModel.triggerAutoSync(showToast = true)
                                }
                            },
                            modifier = Modifier.testTag("user_profile_sync_action")
                        ) {
                            Icon(
                                imageVector = if (isAccountConnected) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                contentDescription = "Cloud Status",
                                tint = if (isAccountConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("user_profile_top_bar")
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    thickness = 0.75.dp
                )
            }
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

            // 1. Status Message Banner
            if (statusMessage != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSuccessStatus) CashInGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth().testTag("status_message_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSuccessStatus) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isSuccessStatus) CashInGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = statusMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSuccessStatus) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { statusMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("✕", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // 2. User Profile Header (Fetched from Gmail, no manual editing)
            item {
                UserProfileHeaderCard(
                    userName = userName,
                    email = driveAccountEmail,
                    isAccountConnected = isAccountConnected,
                    profilePhotoPath = profilePhotoPath,
                    totalBooks = allBooks.size,
                    totalTransactions = allTransactions.size
                )
            }

            // 3. Firestore Cloud Persistence Vault (Google Drive Removed)
            item {
                FirestoreCloudSyncCard(
                    email = driveAccountEmail,
                    isConnected = isAccountConnected,
                    isSyncing = isCloudSyncing,
                    lastBackupSummary = lastBackupSummary,
                    onSyncNow = {
                        viewModel.triggerAutoSync(showToast = true)
                    },
                    onSignOutClick = { showDisconnectConfirmDialog = true }
                )
            }

            // 4. Theme & Color Scheme Card (Soft 5-shade schemes)
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

            // 7. Regional Preferences & Currency Card
            item {
                AppPreferencesCard(
                    defaultCurrency = defaultCurrency,
                    onChangeCurrency = { showCurrencyDialog = true }
                )
            }

            // 8. Developer Credit Footer
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

    // Dialog: Disconnect Account Confirm
    if (showDisconnectConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirmDialog = false },
            icon = {
                Icon(Icons.Default.CloudOff, contentDescription = null, tint = CashOutRed, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(text = "Sign out from Google?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "You will be signed out of your Gmail account. Local data will remain intact.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectConfirmDialog = false
                        viewModel.disconnectAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CashOutRed),
                    modifier = Modifier.testTag("btn_confirm_sign_out")
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Currency Selector
    if (showCurrencyDialog) {
        val currencies = listOf(
            Triple("৳", "BDT", "Bangladeshi Taka"),
            Triple("$", "USD", "US Dollar"),
            Triple("€", "EUR", "Euro"),
            Triple("£", "GBP", "British Pound"),
            Triple("₹", "INR", "Indian Rupee"),
            Triple("¥", "JPY", "Japanese Yen"),
            Triple("₩", "KRW", "South Korean Won"),
            Triple("A$", "AUD", "Australian Dollar"),
            Triple("C$", "CAD", "Canadian Dollar"),
            Triple("CHF", "CHF", "Swiss Franc"),
            Triple("د.إ", "AED", "UAE Dirham"),
            Triple("﷼", "SAR", "Saudi Riyal")
        )
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = {
                Text(text = "Select Default Currency", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currencies.forEach { (symbol, code, name) ->
                        val isSelected = defaultCurrency == symbol
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setDefaultCurrency(symbol)
                                    showCurrencyDialog = false
                                    Toast.makeText(context, "Default currency set to $code ($symbol)", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = symbol,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(36.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = code,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
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
}

// -------------------------------------------------------------
// Component 1: User Profile Header Card (Fetched from Gmail)
// -------------------------------------------------------------
@Composable
private fun UserProfileHeaderCard(
    userName: String,
    email: String,
    isAccountConnected: Boolean,
    profilePhotoPath: String,
    totalBooks: Int,
    totalTransactions: Int
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_profile_header_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar fetched from Gmail account
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePhotoPath.isNotBlank()) {
                        AsyncImage(
                            model = profilePhotoPath,
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else if (userName.isNotBlank()) {
                        Text(
                            text = userName.take(1).uppercase(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Default Avatar",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName.ifBlank { "Google User" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = email.ifBlank { "Not Connected" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAccountConnected) CashInGreen.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = if (isAccountConnected) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isAccountConnected) CashInGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAccountConnected) "Gmail Linked & Synced" else "No Account",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAccountConnected) CashInGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            // Quick Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalBooks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Total Books",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalTransactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component 2: Firestore Cloud Persistence Card
// -------------------------------------------------------------
@Composable
private fun FirestoreCloudSyncCard(
    email: String,
    isConnected: Boolean,
    isSyncing: Boolean,
    lastBackupSummary: String,
    onSyncNow: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("firestore_cloud_sync_card")
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
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Firestore Cloud Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isConnected) "Active connection: $email" else "Not connected to cloud",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (isConnected) CashInGreen else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isConnected) "Continuous Real-Time Cloud Sync" else "Cloud Disconnected",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (lastBackupSummary.isNotBlank()) {
                            Text(
                                text = "Last Synced: $lastBackupSummary",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSyncNow,
                    enabled = isConnected && !isSyncing,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_sync_now"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Syncing...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync Now")
                    }
                }

                if (isConnected) {
                    OutlinedButton(
                        onClick = onSignOutClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CashOutRed),
                        border = BorderStroke(1.dp, CashOutRed.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("btn_sign_out")
                    ) {
                        Text("Sign Out")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component 3: Theme Switcher Card (Soft 5-shade palettes)
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "App Theme & Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${currentModernPalette.title} • Soft 5-Shade Color System",
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

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            // 2. Modern Soft Color Schemes (5 distinct options)
            Text(
                text = "Soft Color Schemes (5 shades with card distinction)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            val palettes = listOf(
                ModernPalette.EMERALD_MINT,
                ModernPalette.CYBER_INDIGO,
                ModernPalette.SUNSET_ROSE,
                ModernPalette.TITANIUM_ICE,
                ModernPalette.AMBER_HONEY
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                palettes.forEach { pal ->
                    val isSelected = currentModernPalette == pal
                    val accentColor = when (pal) {
                        ModernPalette.EMERALD_MINT -> Color(0xFF10B981)
                        ModernPalette.CYBER_INDIGO -> Color(0xFF6366F1)
                        ModernPalette.SUNSET_ROSE -> Color(0xFFF43F5E)
                        ModernPalette.TITANIUM_ICE -> Color(0xFF0EA5E9)
                        ModernPalette.AMBER_HONEY -> Color(0xFFD97706)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPaletteSelected(pal) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pal.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = pal.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
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
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// -------------------------------------------------------------
// Component 6: Regional Preferences & Currency Card
// -------------------------------------------------------------
@Composable
private fun AppPreferencesCard(
    defaultCurrency: String,
    onChangeCurrency: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_preferences_card")
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
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Currency Symbol",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Active currency: $defaultCurrency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onChangeCurrency,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Change")
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component 7: Developer Credit Footer
// -------------------------------------------------------------
@Composable
private fun DeveloperCreditFooterCard(
    onOpenLinkedIn: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("developer_credit_footer_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Crafted with Material 3 & Jetpack Compose",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenLinkedIn() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Arik Md. Isthiaque",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = "LinkedIn Profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
