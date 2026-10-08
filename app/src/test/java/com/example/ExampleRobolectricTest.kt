package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Multi-Ledger", appName)
  }

  @Test
  fun `test csv export and import roundtrip`() {
    val tx1 = com.example.data.model.TransactionRecord(
      id = 1L,
      ledgerBookId = 1L,
      type = com.example.data.model.TransactionType.CASH_IN,
      amount = 5000.0,
      category = "Salary",
      timestamp = 1700000000000L,
      memo = "Monthly payroll"
    )
    val tx2 = com.example.data.model.TransactionRecord(
      id = 2L,
      ledgerBookId = 1L,
      type = com.example.data.model.TransactionType.CASH_OUT,
      amount = 120.50,
      category = "Groceries",
      timestamp = 1700000000000L,
      memo = "Supermarket"
    )

    val csv = com.example.data.io.CsvExporterImporter.exportTransactionsToCsv(
      listOf(tx1, tx2),
      "Personal",
      "$"
    )

    val validation = com.example.data.io.CsvExporterImporter.parseAndValidateCsv(
      csv.byteInputStream()
    )

    assertEquals(2, validation.validRows.size)
    assertEquals(0, validation.invalidRows.size)
    assertEquals(5000.0, validation.validRows[0].amount!!, 0.001)
    assertEquals(120.50, validation.validRows[1].amount!!, 0.001)
    assertEquals("Salary", validation.validRows[0].categoryStr)
    assertEquals(com.example.data.model.TransactionType.CASH_IN, validation.validRows[0].type)
    assertEquals(com.example.data.model.TransactionType.CASH_OUT, validation.validRows[1].type)
  }

  @Test
  fun `test csv import error handling on corrupt rows`() {
    val corruptCsv = """
      Date,Type,Amount,Category,Memo
      invalid-date,Income,100,Bonus,Good
      2024-01-15,BadType,200,Dining,Lunch
      2024-01-16,Expense,-50,Dining,Negative
      2024-01-17,Expense,abc,Groceries,BadAmount
      2024-01-18,Expense,75.50,Groceries,Valid record
    """.trimIndent()

    val validation = com.example.data.io.CsvExporterImporter.parseAndValidateCsv(
      corruptCsv.byteInputStream()
    )

    assertEquals(5, validation.totalRows)
    assertEquals(1, validation.validRows.size)
    assertEquals(4, validation.invalidRows.size)
    assertEquals(75.50, validation.validRows[0].amount!!, 0.001)
  }

  @Test
  fun `test backup json payload serialization and deserialization`() {
    val books = listOf(
      com.example.data.model.LedgerBook(
        id = 10,
        name = "Trip 2026",
        colorHex = 0xFF0D9488L,
        isArchived = false,
        customCategories = "Flights||Hotels"
      )
    )
    val transactions = listOf(
      com.example.data.model.TransactionRecord(
        id = 100,
        ledgerBookId = 10,
        type = com.example.data.model.TransactionType.CASH_OUT,
        amount = 450.0,
        category = "Hotels",
        timestamp = 1700000000000L,
        memo = "Resort stay"
      ),
      com.example.data.model.TransactionRecord(
        id = 101,
        ledgerBookId = 10,
        type = com.example.data.model.TransactionType.CASH_IN,
        amount = 1000.0,
        category = "Salary",
        timestamp = 1700000050000L,
        memo = "Travel allowance"
      )
    )

    val jsonString = com.example.data.io.GoogleDriveBackupManager.createBackupJson("test@gmail.com", books, transactions)
    org.junit.Assert.assertTrue(jsonString.contains("Trip 2026"))
    org.junit.Assert.assertTrue(jsonString.contains("Resort stay"))
    org.junit.Assert.assertTrue(jsonString.contains("Travel allowance"))

    val parsed = com.example.data.io.GoogleDriveBackupManager.parseBackupJson(jsonString)
    org.junit.Assert.assertTrue(parsed.success)
    assertEquals(1, parsed.books.size)
    assertEquals("Trip 2026", parsed.books[0].name)
    assertEquals("Flights||Hotels", parsed.books[0].customCategories)

    assertEquals(2, parsed.transactions.size)
    assertEquals(com.example.data.model.TransactionType.CASH_OUT, parsed.transactions[0].type)
    assertEquals(450.0, parsed.transactions[0].amount, 0.001)
    assertEquals(com.example.data.model.TransactionType.CASH_IN, parsed.transactions[1].type)
    assertEquals(1000.0, parsed.transactions[1].amount, 0.001)
  }
}
