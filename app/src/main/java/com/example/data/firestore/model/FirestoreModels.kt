package com.example.data.firestore.model

import com.example.data.model.LedgerBook
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

data class UserProfileDocument(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val lastSyncTime: Timestamp? = null,
    val createdAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "email" to email,
            "displayName" to displayName,
            "photoUrl" to photoUrl,
            "lastSyncTime" to FieldValue.serverTimestamp()
        )
        if (createdAt == null) {
            map["createdAt"] = FieldValue.serverTimestamp()
        }
        return map
    }
}

data class FirestoreLedgerBook(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val currencySymbol: String = "৳",
    val currencyCode: String = "BDT",
    val colorHex: Long = 0xFF0D9488L,
    val iconName: String = "wallet",
    val isArchived: Boolean = false,
    val customCategories: String = "",
    val isCategoryEnabled: Boolean = true,
    val isCategoryMandatory: Boolean = false,
    val isTransactionTypeEnabled: Boolean = true,
    val isTransactionTypeMandatory: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "id" to id,
            "userId" to userId,
            "name" to name,
            "currencySymbol" to currencySymbol,
            "currencyCode" to currencyCode,
            "colorHex" to colorHex,
            "iconName" to iconName,
            "isArchived" to isArchived,
            "customCategories" to customCategories,
            "isCategoryEnabled" to isCategoryEnabled,
            "isCategoryMandatory" to isCategoryMandatory,
            "isTransactionTypeEnabled" to isTransactionTypeEnabled,
            "isTransactionTypeMandatory" to isTransactionTypeMandatory,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (createdAt == null) {
            map["createdAt"] = FieldValue.serverTimestamp()
        }
        return map
    }

    fun toLocalLedgerBook(): LedgerBook {
        return LedgerBook(
            id = id.toLongOrNull() ?: 0L,
            name = name,
            currencySymbol = currencySymbol,
            currencyCode = currencyCode,
            colorHex = colorHex,
            iconName = iconName,
            isArchived = isArchived,
            customCategories = customCategories,
            isCategoryEnabled = isCategoryEnabled,
            isCategoryMandatory = isCategoryMandatory,
            isTransactionTypeEnabled = isTransactionTypeEnabled,
            isTransactionTypeMandatory = isTransactionTypeMandatory,
            createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
        )
    }

    companion object {
        fun fromLocal(book: LedgerBook, userId: String): FirestoreLedgerBook {
            return FirestoreLedgerBook(
                id = book.id.toString(),
                userId = userId,
                name = book.name,
                currencySymbol = book.currencySymbol,
                currencyCode = book.currencyCode,
                colorHex = book.colorHex,
                iconName = book.iconName,
                isArchived = book.isArchived,
                customCategories = book.customCategories,
                isCategoryEnabled = book.isCategoryEnabled,
                isCategoryMandatory = book.isCategoryMandatory,
                isTransactionTypeEnabled = book.isTransactionTypeEnabled,
                isTransactionTypeMandatory = book.isTransactionTypeMandatory
            )
        }
    }
}

data class FirestoreTransactionRecord(
    val id: String = "",
    val userId: String = "",
    val bookId: String = "",
    val type: String = "IN",
    val amount: Double = 0.0,
    val category: String = "General",
    val timestamp: Long = 0L,
    val memo: String = "",
    val paymentMode: String = "Cash",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "id" to id,
            "userId" to userId,
            "bookId" to bookId,
            "type" to type,
            "amount" to amount,
            "category" to category,
            "timestamp" to timestamp,
            "memo" to memo,
            "paymentMode" to paymentMode,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (createdAt == null) {
            map["createdAt"] = FieldValue.serverTimestamp()
        }
        return map
    }

    fun toLocalTransactionRecord(): TransactionRecord {
        return TransactionRecord(
            id = id.toLongOrNull() ?: 0L,
            ledgerBookId = bookId.toLongOrNull() ?: 0L,
            type = if (type.uppercase() == "OUT" || type.uppercase() == "CASH_OUT") TransactionType.CASH_OUT else TransactionType.CASH_IN,
            amount = amount,
            category = category,
            timestamp = timestamp,
            memo = memo,
            paymentMode = paymentMode,
            createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
        )
    }

    companion object {
        fun fromLocal(tx: TransactionRecord, userId: String): FirestoreTransactionRecord {
            return FirestoreTransactionRecord(
                id = tx.id.toString(),
                userId = userId,
                bookId = tx.ledgerBookId.toString(),
                type = if (tx.type == TransactionType.CASH_OUT) "OUT" else "IN",
                amount = tx.amount,
                category = tx.category,
                timestamp = tx.timestamp,
                memo = tx.memo,
                paymentMode = tx.paymentMode
            )
        }
    }
}
