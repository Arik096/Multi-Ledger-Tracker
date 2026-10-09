package com.example.data.repository

import com.example.data.db.LedgerBookDao
import com.example.data.db.TransactionDao
import com.example.data.model.LedgerBook
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class LedgerRepository(
    private val ledgerBookDao: LedgerBookDao,
    private val transactionDao: TransactionDao
) {
    val allBooks: Flow<List<LedgerBook>> = ledgerBookDao.getAllBooks()
    val activeBooks: Flow<List<LedgerBook>> = ledgerBookDao.getActiveBooks()

    suspend fun getActiveBooksDirect(): List<LedgerBook> = ledgerBookDao.getAllBooksDirect()
    suspend fun insertOrUpdateTransaction(transaction: TransactionRecord): Long =
        transactionDao.insertTransaction(transaction)

    fun getTransactionsForBook(bookId: Long): Flow<List<TransactionRecord>> =
        transactionDao.getTransactionsByLedger(bookId)

    suspend fun getTransactionsForBookDirect(bookId: Long): List<TransactionRecord> =
        transactionDao.getTransactionsByLedgerDirect(bookId)

    fun getAllTransactions(): Flow<List<TransactionRecord>> =
        transactionDao.getAllTransactions()

    suspend fun getAllTransactionsDirect(): List<TransactionRecord> =
        transactionDao.getAllTransactionsDirect()

    fun getBookById(id: Long): Flow<LedgerBook?> = ledgerBookDao.getBookById(id)

    suspend fun getBookByIdDirect(id: Long): LedgerBook? = ledgerBookDao.getBookByIdDirect(id)

    suspend fun createBook(
        name: String,
        currencySymbol: String = "৳",
        currencyCode: String = "BDT",
        colorHex: Long = 0xFF0D9488L,
        iconName: String = "wallet"
    ): Long {
        val book = LedgerBook(
            name = name,
            currencySymbol = currencySymbol,
            currencyCode = currencyCode,
            colorHex = colorHex,
            iconName = iconName
        )
        return ledgerBookDao.insertBook(book)
    }

    suspend fun updateBook(book: LedgerBook) = ledgerBookDao.updateBook(book)

    suspend fun archiveBook(bookId: Long, isArchived: Boolean) {
        val existing = ledgerBookDao.getBookByIdDirect(bookId) ?: return
        ledgerBookDao.updateBook(existing.copy(isArchived = isArchived))
    }

    suspend fun deleteBook(bookId: Long) {
        transactionDao.deleteTransactionsByLedger(bookId)
        ledgerBookDao.deleteBookById(bookId)
    }

    suspend fun insertTransaction(transaction: TransactionRecord): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun insertTransactions(transactions: List<TransactionRecord>): List<Long> =
        transactionDao.insertTransactions(transactions)

    suspend fun updateTransaction(transaction: TransactionRecord) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transactionId: Long) =
        transactionDao.deleteTransactionById(transactionId)

    suspend fun duplicateTransaction(transactionId: Long) {
        val original = transactionDao.getTransactionByIdDirect(transactionId) ?: return
        val clone = original.copy(
            id = 0,
            timestamp = System.currentTimeMillis(),
            memo = if (original.memo.isNotBlank()) "${original.memo} (Copy)" else "Copy",
            createdAt = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(clone)
    }

    suspend fun restoreAllData(books: List<LedgerBook>, transactions: List<TransactionRecord>) {
        transactionDao.deleteAllTransactions()
        ledgerBookDao.deleteAllBooks()
        ledgerBookDao.insertBooks(books)
        transactionDao.insertTransactions(transactions)
    }

    suspend fun seedInitialDataIfEmpty() {
        // No pre-added data for record-keeping app per user request.
        // If empty on first launch, it remains empty until the user connects their Google account
        // and either restores their previous cloud records or creates their first ledger book.
    }
}
