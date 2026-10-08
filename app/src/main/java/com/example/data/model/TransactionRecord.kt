package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = LedgerBook::class,
            parentColumns = ["id"],
            childColumns = ["ledgerBookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["ledgerBookId"]), Index(value = ["timestamp"])]
)
data class TransactionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ledgerBookId: Long,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val timestamp: Long,
    val memo: String = "",
    val paymentMode: String = "Cash",
    val createdAt: Long = System.currentTimeMillis()
) {
    val title: String
        get() = memo.ifBlank { category }
}
