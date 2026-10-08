package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.LedgerBook
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.data.preferences.ThemeMode
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.components.AnimatedSplashScreen
import com.example.ui.screens.BookInsideScreen
import com.example.ui.screens.BookSettingsScreen
import com.example.ui.screens.CashPositionScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LedgersScreen
import com.example.ui.screens.LogEntryScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LedgerViewModel
import kotlinx.coroutines.flow.collectLatest

enum class MainTab(val title: String) {
    BOOKS("Books"),
    DASHBOARD("Dashboard"),
    CASH_POSITION("Cash Position"),
    PROFILE("Profile")
}

class MainActivity : ComponentActivity() {
    private var ledgerViewModel: LedgerViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = applicationContext as Application
        val vm: LedgerViewModel = ViewModelProvider(this, LedgerViewModel.provideFactory(app))[LedgerViewModel::class.java]
        ledgerViewModel = vm

        setContent {
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()
            val modernPalette by vm.modernPalette.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                palette = modernPalette
            ) {
                MainAppScreen(viewModel = vm)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        ledgerViewModel?.triggerAutoSync(showToast = false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: LedgerViewModel = viewModel(factory = LedgerViewModel.provideFactory(LocalContext.current.applicationContext as Application))
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()
    val isAccountConnected by viewModel.isAccountConnected.collectAsStateWithLifecycle()
    val driveAccountEmail by viewModel.driveAccountEmail.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(MainTab.BOOKS) }
    var activeBookInside by remember { mutableStateOf<LedgerBook?>(null) }
    var isViewingDashboardForBook by remember { mutableStateOf<LedgerBook?>(null) }
    var isLoggingEntry by remember { mutableStateOf(false) }
    var logEntryInitialType by remember { mutableStateOf(TransactionType.CASH_OUT) }
    var editingTransaction by remember { mutableStateOf<TransactionRecord?>(null) }
    var bookForSettings by remember { mutableStateOf<LedgerBook?>(null) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }

    // First login onboarding state
    var hasDismissedFirstTimeLogin by remember { mutableStateOf(false) }
    var firstLoginEmailInput by remember { mutableStateOf("") }
    var firstLoginNameInput by remember { mutableStateOf("") }
    var isFirstLoginConnecting by remember { mutableStateOf(false) }

    // Keep activeBookInside synced with latest state from allBooks
    val currentActiveBook = remember(activeBookInside, allBooks) {
        if (activeBookInside != null) {
            allBooks.firstOrNull { it.id == activeBookInside!!.id } ?: activeBookInside
        } else null
    }

    if (showSplash) {
        AnimatedSplashScreen(
            onAnimationFinished = {
                showSplash = false
            }
        )
        return
    }

    LaunchedEffect(Unit) {
        viewModel.triggerAutoSync(showToast = true)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    // System Back Gesture Handling for all screens (hierarchical)
    BackHandler(enabled = bookForSettings != null) {
        bookForSettings = null
    }
    BackHandler(enabled = (isLoggingEntry || editingTransaction != null) && bookForSettings == null) {
        isLoggingEntry = false
        editingTransaction = null
    }
    BackHandler(enabled = isViewingDashboardForBook != null && !isLoggingEntry && editingTransaction == null && bookForSettings == null) {
        isViewingDashboardForBook = null
    }
    BackHandler(enabled = currentActiveBook != null && isViewingDashboardForBook == null && !isLoggingEntry && editingTransaction == null && bookForSettings == null) {
        activeBookInside = null
    }
    BackHandler(enabled = currentTab != MainTab.BOOKS && currentActiveBook == null && isViewingDashboardForBook == null && bookForSettings == null && !isLoggingEntry && editingTransaction == null) {
        currentTab = MainTab.BOOKS
    }

    val isTopLevelScreen = bookForSettings == null &&
            !isLoggingEntry &&
            editingTransaction == null &&
            currentActiveBook == null &&
            isViewingDashboardForBook == null &&
            currentTab == MainTab.BOOKS &&
            !showExitConfirmDialog

    BackHandler(enabled = isTopLevelScreen) {
        showExitConfirmDialog = true
    }

    if (bookForSettings != null) {
        // Full view Book Settings Screen (User requirement)
        val currentBook = allBooks.firstOrNull { it.id == bookForSettings!!.id } ?: bookForSettings!!
        val otherBooks = allBooks.filter { it.id != currentBook.id }

        BookSettingsScreen(
            book = currentBook,
            allOtherBooks = otherBooks,
            onDismiss = { bookForSettings = null },
            onSave = { updatedBook ->
                viewModel.updateBook(updatedBook)
                bookForSettings = null
            }
        )
    } else if (isLoggingEntry || editingTransaction != null) {
        // Cash In / Cash Out entry creation or editing
        LogEntryScreen(
            viewModel = viewModel,
            initialTransaction = editingTransaction,
            initialBookId = currentActiveBook?.id,
            initialType = logEntryInitialType,
            onDismiss = {
                isLoggingEntry = false
                editingTransaction = null
            },
            onSaveSuccess = {
                isLoggingEntry = false
                editingTransaction = null
            }
        )
    } else if (isViewingDashboardForBook != null) {
        // Statistics Dashboard for specific book
        HomeScreen(
            viewModel = viewModel,
            onNavigateToBooks = {
                isViewingDashboardForBook = null
                activeBookInside = null
                currentTab = MainTab.BOOKS
            },
            onBookSwitched = { newBook ->
                isViewingDashboardForBook = newBook
                activeBookInside = null
            },
            onEditTransaction = { tx -> editingTransaction = tx },
            onOpenDriveBackup = {
                isViewingDashboardForBook = null
                currentTab = MainTab.PROFILE
            },
            onNavigateBack = {
                isViewingDashboardForBook = null
                activeBookInside = null
            }
        )
    } else if (currentActiveBook != null) {
        // Book Inside View (Simplified view matching image.png, does NOT redirect to dashboard)
        BookInsideScreen(
            book = currentActiveBook,
            viewModel = viewModel,
            onNavigateBack = { activeBookInside = null },
            onNavigateToDashboard = {
                viewModel.selectBook(currentActiveBook.id)
                isViewingDashboardForBook = currentActiveBook
            },
            onNavigateToCashIn = {
                logEntryInitialType = TransactionType.CASH_IN
                isLoggingEntry = true
            },
            onNavigateToCashOut = {
                logEntryInitialType = TransactionType.CASH_OUT
                isLoggingEntry = true
            },
            onEditTransaction = { tx -> editingTransaction = tx },
            onOpenBookSettings = { bookForSettings = currentActiveBook },
            onOpenDriveBackup = {
                activeBookInside = null
                currentTab = MainTab.PROFILE
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_navigation")
                ) {
                    NavigationBarItem(
                        selected = currentTab == MainTab.BOOKS,
                        onClick = { currentTab = MainTab.BOOKS },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Books",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Books",
                                fontSize = 12.sp,
                                fontWeight = if (currentTab == MainTab.BOOKS) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_books")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.DASHBOARD,
                        onClick = { currentTab = MainTab.DASHBOARD },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = "Dashboard",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Dashboard",
                                fontSize = 12.sp,
                                fontWeight = if (currentTab == MainTab.DASHBOARD) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.CASH_POSITION,
                        onClick = { currentTab = MainTab.CASH_POSITION },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = "Cash Position",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Position",
                                fontSize = 12.sp,
                                fontWeight = if (currentTab == MainTab.CASH_POSITION) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_cash_position")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.PROFILE,
                        onClick = { currentTab = MainTab.PROFILE },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Profile",
                                fontSize = 12.sp,
                                fontWeight = if (currentTab == MainTab.PROFILE) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_profile")
                    )
                }
            }
        ) { innerPadding ->
            when (currentTab) {
                MainTab.BOOKS -> {
                    LedgersScreen(
                        viewModel = viewModel,
                        onBookSelected = { book ->
                            viewModel.selectBook(book.id)
                            activeBookInside = book
                        },
                        onOpenBookSettings = { book ->
                            bookForSettings = book
                        },
                        onOpenDriveBackup = { currentTab = MainTab.PROFILE },
                        onNavigateBack = { showExitConfirmDialog = true },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                MainTab.DASHBOARD -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToBooks = { currentTab = MainTab.BOOKS },
                        onEditTransaction = { tx -> editingTransaction = tx },
                        onOpenDriveBackup = { currentTab = MainTab.PROFILE },
                        onNavigateBack = null,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                MainTab.CASH_POSITION -> {
                    val activeBook = allBooks.firstOrNull { it.id == viewModel.selectedBookId.value }
                    CashPositionScreen(
                        currencySymbol = activeBook?.currencySymbol ?: allBooks.firstOrNull()?.currencySymbol ?: "৳",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                MainTab.PROFILE -> {
                    UserProfileScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    // First-time Google Account Login Onboarding Modal
    val showFirstTimeLogin = !isAccountConnected && driveAccountEmail.isBlank() && !hasDismissedFirstTimeLogin
    if (showFirstTimeLogin) {
        AlertDialog(
            onDismissRequest = {
                // User can dismiss to explore as guest
                hasDismissedFirstTimeLogin = true
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Sign In with Google Account",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your Google account to get started. The app will search for any previous records in your cloud vault and restore them, or start fresh with automatic cloud backup so you never lose your data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = firstLoginEmailInput,
                        onValueChange = { firstLoginEmailInput = it },
                        label = { Text("Google Account Email") },
                        placeholder = { Text("name@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("first_login_email_input")
                    )

                    OutlinedTextField(
                        value = firstLoginNameInput,
                        onValueChange = { firstLoginNameInput = it },
                        label = { Text("Your Name (Optional)") },
                        placeholder = { Text("e.g. Arik") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("first_login_name_input")
                    )

                    if (isFirstLoginConnecting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Checking cloud vault for existing records...",
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
                        val email = firstLoginEmailInput.trim()
                        if (email.isBlank() || !email.contains("@")) {
                            Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isFirstLoginConnecting = true
                        viewModel.connectGoogleAccount(email, firstLoginNameInput.trim()) { foundData, message ->
                            isFirstLoginConnecting = false
                            hasDismissedFirstTimeLogin = true
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isFirstLoginConnecting,
                    modifier = Modifier.testTag("first_login_submit_button")
                ) {
                    Text(if (isFirstLoginConnecting) "Connecting..." else "Sign In & Enter")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { hasDismissedFirstTimeLogin = true },
                    enabled = !isFirstLoginConnecting,
                    modifier = Modifier.testTag("first_login_skip_button")
                ) {
                    Text("Skip for Now")
                }
            }
        )
    }

    // Exit App Confirmation Dialog (User requirement: confirmation popup needed before exiting: "when exiting the app, a pop-up will show do you want to exit, after confirmation, it will exit the app")
    if (showExitConfirmDialog) {
        val activity = context as? Activity
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            properties = DialogProperties(dismissOnClickOutside = false),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exit App", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Do you want to exit?",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        coroutineScope.launch {
                            viewModel.triggerAutoSyncDirect(showToast = true)
                            activity?.finish()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("exit_confirm_button")
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitConfirmDialog = false },
                    modifier = Modifier.testTag("exit_cancel_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
