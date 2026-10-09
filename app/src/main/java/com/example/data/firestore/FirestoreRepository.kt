package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.cloud.OperationType
import com.example.data.cloud.handleFirestoreError
import com.example.data.firestore.model.FirestoreLedgerBook
import com.example.data.firestore.model.FirestoreTransactionRecord
import com.example.data.firestore.model.UserProfileDocument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    // Secondary constructor resolving named database ID from string resources
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        FirebaseAuth.getInstance()
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: error("User must be authenticated to perform Firestore operations")
    }

    suspend fun saveUserProfile(
        userId: String,
        email: String,
        displayName: String,
        photoUrl: String = ""
    ) {
        val currentUid = requireUserId()
        if (currentUid != userId) {
            error("Cannot write user profile for mismatched UID")
        }
        val docRef = firestore.collection("users").document(userId)
        val profile = UserProfileDocument(
            userId = userId,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl
        )
        try {
            docRef.set(profile.toWriteMap(), SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeBooks(userId: String): Flow<List<FirestoreLedgerBook>> = callbackFlow {
        val collectionRef = firestore.collection("users").document(userId).collection("books")
        val registration = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, collectionRef.path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val books = snapshot.documents.mapNotNull { doc ->
                    try {
                        val book = doc.toObject(
                            FirestoreLedgerBook::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        book?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.w("FirestoreRepository", "Failed parsing book doc ${doc.id}", e)
                        null
                    }
                }
                trySend(books)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun getBooksDirect(userId: String): List<FirestoreLedgerBook> {
        val collectionRef = firestore.collection("users").document(userId).collection("books")
        return try {
            val snapshot = collectionRef.get().await()
            snapshot.documents.mapNotNull { doc ->
                val book = doc.toObject(
                    FirestoreLedgerBook::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                book?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, collectionRef.path)
            emptyList()
        }
    }

    suspend fun saveBook(userId: String, book: FirestoreLedgerBook) {
        val currentUid = requireUserId()
        if (currentUid != userId) error("User ID mismatch for saveBook")
        val docRef = firestore.collection("users").document(userId).collection("books").document(book.id)
        try {
            docRef.set(book.toWriteMap(), SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBook(userId: String, bookId: String) {
        val currentUid = requireUserId()
        if (currentUid != userId) error("User ID mismatch for deleteBook")
        val docRef = firestore.collection("users").document(userId).collection("books").document(bookId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    fun observeTransactions(userId: String): Flow<List<FirestoreTransactionRecord>> = callbackFlow {
        val collectionRef = firestore.collection("users").document(userId).collection("transactions")
        val registration = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, collectionRef.path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val transactions = snapshot.documents.mapNotNull { doc ->
                    try {
                        val record = doc.toObject(
                            FirestoreTransactionRecord::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        record?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.w("FirestoreRepository", "Failed parsing tx doc ${doc.id}", e)
                        null
                    }
                }
                trySend(transactions)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun getTransactionsDirect(userId: String): List<FirestoreTransactionRecord> {
        val collectionRef = firestore.collection("users").document(userId).collection("transactions")
        return try {
            val snapshot = collectionRef.get().await()
            snapshot.documents.mapNotNull { doc ->
                val tx = doc.toObject(
                    FirestoreTransactionRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                tx?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, collectionRef.path)
            emptyList()
        }
    }

    suspend fun saveTransaction(userId: String, record: FirestoreTransactionRecord) {
        val currentUid = requireUserId()
        if (currentUid != userId) error("User ID mismatch for saveTransaction")
        val docRef = firestore.collection("users").document(userId).collection("transactions").document(record.id)
        try {
            docRef.set(record.toWriteMap(), SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTransaction(userId: String, transactionId: String) {
        val currentUid = requireUserId()
        if (currentUid != userId) error("User ID mismatch for deleteTransaction")
        val docRef = firestore.collection("users").document(userId).collection("transactions").document(transactionId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun syncAllLocalToCloud(
        userId: String,
        books: List<FirestoreLedgerBook>,
        transactions: List<FirestoreTransactionRecord>
    ) {
        val currentUid = requireUserId()
        if (currentUid != userId) error("User ID mismatch for batch sync")
        val batch = firestore.batch()

        for (book in books) {
            val ref = firestore.collection("users").document(userId).collection("books").document(book.id)
            batch.set(ref, book.toWriteMap(), SetOptions.merge())
        }

        for (tx in transactions) {
            val ref = firestore.collection("users").document(userId).collection("transactions").document(tx.id)
            batch.set(ref, tx.toWriteMap(), SetOptions.merge())
        }

        try {
            batch.commit().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "/users/$userId/batchSync")
            throw e
        }
    }
}
