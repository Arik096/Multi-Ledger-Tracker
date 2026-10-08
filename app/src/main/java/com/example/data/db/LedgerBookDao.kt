package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LedgerBook
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerBookDao {
    @Query("SELECT * FROM ledger_books ORDER BY isArchived ASC, createdAt DESC")
    fun getAllBooks(): Flow<List<LedgerBook>>

    @Query("SELECT * FROM ledger_books WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getActiveBooks(): Flow<List<LedgerBook>>

    @Query("SELECT * FROM ledger_books WHERE id = :id LIMIT 1")
    fun getBookById(id: Long): Flow<LedgerBook?>

    @Query("SELECT * FROM ledger_books WHERE id = :id LIMIT 1")
    suspend fun getBookByIdDirect(id: Long): LedgerBook?

    @Query("SELECT * FROM ledger_books")
    suspend fun getAllBooksDirect(): List<LedgerBook>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: LedgerBook): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<LedgerBook>): List<Long>

    @Update
    suspend fun updateBook(book: LedgerBook)

    @Delete
    suspend fun deleteBook(book: LedgerBook)

    @Query("DELETE FROM ledger_books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query("DELETE FROM ledger_books")
    suspend fun deleteAllBooks()

    @Query("SELECT COUNT(*) FROM ledger_books")
    suspend fun countBooks(): Int
}
