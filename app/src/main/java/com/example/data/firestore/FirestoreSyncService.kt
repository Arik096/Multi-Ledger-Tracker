package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.data.firestore.model.FirestoreLedgerBook
import com.example.data.firestore.model.FirestoreTransactionRecord
import com.example.data.model.LedgerBook
import com.example.data.model.TransactionRecord
import com.example.data.repository.LedgerRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Synced(val lastSyncMillis: Long) : SyncStatus
    data class Error(val message: String) : SyncStatus
}

class FirestoreSyncService(
    private val firestoreRepository: FirestoreRepository,
    private val ledgerRepository: LedgerRepository,
    private val scope: CoroutineScope
) {
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private var activeUser: FirebaseUser? = null
    private var observeJob: Job? = null
    private var isApplyingRemoteUpdates = false

    fun onUserAuthenticated(user: FirebaseUser?) {
        if (user == null) {
            observeJob?.cancel()
            observeJob = null
            activeUser = null
            _syncStatus.value = SyncStatus.Idle
            return
        }

        if (activeUser?.uid == user.uid && observeJob != null) {
            return
        }

        activeUser = user
        observeJob?.cancel()

        scope.launch {
            try {
                _syncStatus.value = SyncStatus.Syncing
                firestoreRepository.saveUserProfile(
                    userId = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: "User",
                    photoUrl = user.photoUrl?.toString() ?: ""
                )
                performFullSync(user.uid)
                startRealtimeSync(user.uid)
            } catch (e: Exception) {
                Log.e("FirestoreSyncService", "Error during initial user sync", e)
                _syncStatus.value = SyncStatus.Error(e.localizedMessage ?: "Sync error")
            }
        }
    }

    suspend fun triggerManualSync(): Boolean = withContext(Dispatchers.IO) {
        val user = activeUser ?: return@withContext false
        try {
            _syncStatus.value = SyncStatus.Syncing
            performFullSync(user.uid)
            _syncStatus.value = SyncStatus.Synced(System.currentTimeMillis())
            true
        } catch (e: Exception) {
            Log.e("FirestoreSyncService", "Manual sync failed", e)
            _syncStatus.value = SyncStatus.Error(e.localizedMessage ?: "Manual sync failed")
            false
        }
    }

    private suspend fun performFullSync(userId: String) = withContext(Dispatchers.IO) {
        // 1. Fetch remote data from Firestore
        val remoteBooks = firestoreRepository.getBooksDirect(userId)
        val remoteTransactions = firestoreRepository.getTransactionsDirect(userId)

        // 2. Fetch local data from Room
        val localBooks = ledgerRepository.getActiveBooksDirect()
        val localTransactions = ledgerRepository.getAllTransactionsDirect()

        if (remoteBooks.isEmpty() && remoteTransactions.isEmpty()) {
            // First time or cloud is empty: push local data to Firestore
            if (localBooks.isNotEmpty() || localTransactions.isNotEmpty()) {
                val fBooks = localBooks.map { FirestoreLedgerBook.fromLocal(it, userId) }
                val fTxs = localTransactions.map { FirestoreTransactionRecord.fromLocal(it, userId) }
                firestoreRepository.syncAllLocalToCloud(userId, fBooks, fTxs)
            }
        } else {
            // Remote has data: merge or restore remote records locally
            isApplyingRemoteUpdates = true
            try {
                val mergedBooks = remoteBooks.map { it.toLocalLedgerBook() }
                val mergedTxs = remoteTransactions.map { it.toLocalTransactionRecord() }
                ledgerRepository.restoreAllData(mergedBooks, mergedTxs)
            } finally {
                isApplyingRemoteUpdates = false
            }
        }

        _syncStatus.value = SyncStatus.Synced(System.currentTimeMillis())
    }

    private fun startRealtimeSync(userId: String) {
        val job = Job()
        observeJob = job

        firestoreRepository.observeBooks(userId)
            .onEach { books ->
                if (isApplyingRemoteUpdates || books.isEmpty()) return@onEach
                withContext(Dispatchers.IO) {
                    for (book in books) {
                        val local = book.toLocalLedgerBook()
                        val existing = ledgerRepository.getBookByIdDirect(local.id)
                        if (existing == null) {
                            ledgerRepository.createBook(
                                name = local.name,
                                currencySymbol = local.currencySymbol,
                                currencyCode = local.currencyCode,
                                colorHex = local.colorHex,
                                iconName = local.iconName
                            )
                        } else {
                            ledgerRepository.updateBook(local)
                        }
                    }
                }
            }
            .catch { e -> Log.e("FirestoreSyncService", "Books stream error", e) }
            .launchIn(scope)

        firestoreRepository.observeTransactions(userId)
            .onEach { txs ->
                if (isApplyingRemoteUpdates || txs.isEmpty()) return@onEach
                withContext(Dispatchers.IO) {
                    for (tx in txs) {
                        val local = tx.toLocalTransactionRecord()
                        ledgerRepository.insertOrUpdateTransaction(local)
                    }
                }
            }
            .catch { e -> Log.e("FirestoreSyncService", "Transactions stream error", e) }
            .launchIn(scope)
    }

    // Handlers called when user modifies data locally
    fun onLocalBookSaved(book: LedgerBook) {
        val user = activeUser ?: return
        if (isApplyingRemoteUpdates) return
        scope.launch(Dispatchers.IO) {
            try {
                firestoreRepository.saveBook(user.uid, FirestoreLedgerBook.fromLocal(book, user.uid))
            } catch (e: Exception) {
                Log.e("FirestoreSyncService", "Failed to sync book to Firestore", e)
            }
        }
    }

    fun onLocalBookDeleted(bookId: Long) {
        val user = activeUser ?: return
        if (isApplyingRemoteUpdates) return
        scope.launch(Dispatchers.IO) {
            try {
                firestoreRepository.deleteBook(user.uid, bookId.toString())
            } catch (e: Exception) {
                Log.e("FirestoreSyncService", "Failed to delete book in Firestore", e)
            }
        }
    }

    fun onLocalTransactionSaved(tx: TransactionRecord) {
        val user = activeUser ?: return
        if (isApplyingRemoteUpdates) return
        scope.launch(Dispatchers.IO) {
            try {
                firestoreRepository.saveTransaction(user.uid, FirestoreTransactionRecord.fromLocal(tx, user.uid))
            } catch (e: Exception) {
                Log.e("FirestoreSyncService", "Failed to sync transaction to Firestore", e)
            }
        }
    }

    fun onLocalTransactionDeleted(txId: Long) {
        val user = activeUser ?: return
        if (isApplyingRemoteUpdates) return
        scope.launch(Dispatchers.IO) {
            try {
                firestoreRepository.deleteTransaction(user.uid, txId.toString())
            } catch (e: Exception) {
                Log.e("FirestoreSyncService", "Failed to delete transaction in Firestore", e)
            }
        }
    }
}
