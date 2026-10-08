package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TransactionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE ledgerBookId = :bookId ORDER BY timestamp DESC, id DESC")
    fun getTransactionsByLedger(bookId: Long): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions WHERE ledgerBookId = :bookId ORDER BY timestamp DESC, id DESC")
    suspend fun getTransactionsByLedgerDirect(bookId: Long): List<TransactionRecord>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    suspend fun getAllTransactionsDirect(): List<TransactionRecord>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionById(id: Long): Flow<TransactionRecord?>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionByIdDirect(id: Long): TransactionRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionRecord>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionRecord)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionRecord)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE ledgerBookId = :bookId")
    suspend fun deleteTransactionsByLedger(bookId: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT COUNT(*) FROM transactions WHERE ledgerBookId = :bookId")
    suspend fun countTransactionsByLedger(bookId: Long): Int
}
