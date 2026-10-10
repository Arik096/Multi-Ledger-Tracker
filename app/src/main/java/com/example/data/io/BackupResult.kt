package com.example.data.io

import com.example.data.model.LedgerBook
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupMetadata(
    val accountEmail: String = "",
    val exportDate: String = ""
)

data class BackupResult(
    val success: Boolean,
    val message: String = "",
    val books: List<LedgerBook> = emptyList(),
    val transactions: List<TransactionRecord> = emptyList(),
    val metadata: BackupMetadata? = null
)

object JsonBackupHelper {

    fun createBackupJson(
        account: String,
        books: List<LedgerBook>,
        transactions: List<TransactionRecord>
    ): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())
        root.put("account", account)

        val booksArr = JSONArray()
        for (b in books) {
            val bObj = JSONObject()
            bObj.put("id", b.id)
            bObj.put("name", b.name)
            bObj.put("currencySymbol", b.currencySymbol)
            bObj.put("currencyCode", b.currencyCode)
            bObj.put("colorHex", b.colorHex)
            bObj.put("iconName", b.iconName)
            bObj.put("isArchived", b.isArchived)
            bObj.put("customExpenseCategories", b.customExpenseCategories)
            bObj.put("customIncomeCategories", b.customIncomeCategories)
            booksArr.put(bObj)
        }
        root.put("books", booksArr)

        val txsArr = JSONArray()
        for (tx in transactions) {
            val txObj = JSONObject()
            txObj.put("id", tx.id)
            txObj.put("ledgerBookId", tx.ledgerBookId)
            txObj.put("type", tx.type.name)
            txObj.put("amount", tx.amount)
            txObj.put("category", tx.category)
            txObj.put("timestamp", tx.timestamp)
            txObj.put("memo", tx.memo)
            txObj.put("paymentMode", tx.paymentMode)
            txsArr.put(txObj)
        }
        root.put("transactions", txsArr)

        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String): BackupResult {
        return try {
            val root = JSONObject(jsonString)
            val account = root.optString("account", "")
            val timestamp = root.optLong("timestamp", System.currentTimeMillis())
            val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

            val booksArr = root.optJSONArray("books") ?: JSONArray()
            val txsArr = root.optJSONArray("transactions") ?: JSONArray()

            val books = mutableListOf<LedgerBook>()
            for (i in 0 until booksArr.length()) {
                val bObj = booksArr.getJSONObject(i)
                books.add(
                    LedgerBook(
                        id = bObj.optLong("id", 0L),
                        name = bObj.optString("name", "Ledger"),
                        currencySymbol = bObj.optString("currencySymbol", "৳"),
                        currencyCode = bObj.optString("currencyCode", "BDT"),
                        colorHex = bObj.optLong("colorHex", 0xFF0D9488L),
                        iconName = bObj.optString("iconName", "wallet"),
                        isArchived = bObj.optBoolean("isArchived", false),
                        customExpenseCategories = bObj.optString("customExpenseCategories", ""),
                        customIncomeCategories = bObj.optString("customIncomeCategories", "")
                    )
                )
            }

            val transactions = mutableListOf<TransactionRecord>()
            for (i in 0 until txsArr.length()) {
                val txObj = txsArr.getJSONObject(i)
                val typeStr = txObj.optString("type", TransactionType.CASH_OUT.name)
                val type = try {
                    TransactionType.valueOf(typeStr)
                } catch (e: Exception) {
                    TransactionType.CASH_OUT
                }

                transactions.add(
                    TransactionRecord(
                        id = txObj.optLong("id", 0L),
                        ledgerBookId = txObj.optLong("ledgerBookId", 0L),
                        type = type,
                        amount = txObj.optDouble("amount", 0.0),
                        category = txObj.optString("category", "General"),
                        timestamp = txObj.optLong("timestamp", System.currentTimeMillis()),
                        memo = txObj.optString("memo", ""),
                        paymentMode = txObj.optString("paymentMode", "Cash")
                    )
                )
            }

            BackupResult(
                success = true,
                message = "Parsed ${books.size} books and ${transactions.size} transactions",
                books = books,
                transactions = transactions,
                metadata = BackupMetadata(accountEmail = account, exportDate = dateStr)
            )
        } catch (e: Exception) {
            BackupResult(
                success = false,
                message = "Parsing failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }
}
