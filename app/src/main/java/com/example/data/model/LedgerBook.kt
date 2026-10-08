package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ledger_books")
data class LedgerBook(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
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
    val customExpenseCategories: String = "",
    val customIncomeCategories: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Unified independent category list.
     * Any entry (Cash In or Cash Out) can use any category.
     */
    fun getCategories(): List<String> {
        val parsed = customCategories.split("||")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parsed.isNotEmpty()) return parsed

        // Legacy fallback
        val legacy = (customExpenseCategories.split("||") + customIncomeCategories.split("||"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        if (legacy.isNotEmpty()) return legacy

        return CategoryConstants.defaultCategories
    }

    fun getExpenseCategories(): List<String> = getCategories()

    fun getIncomeCategories(): List<String> = getCategories()

    fun withCategories(categories: List<String>): LedgerBook {
        return copy(customCategories = categories.distinct().joinToString("||"))
    }
}
