package com.example

import com.example.data.model.LedgerBook
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testBookCategorySeparationAndList() {
    val book = LedgerBook(
      id = 1,
      name = "Main Store",
      customCategories = "Supplies||Inventory||Utilities"
    )

    val cats = book.getCategories()
    assertEquals(3, cats.size)
    assertEquals("Supplies", cats[0])
    assertEquals("Inventory", cats[1])
    assertEquals("Utilities", cats[2])

    val updatedBook = book.withCategories(listOf("Supplies", "Marketing", "Consulting"))
    assertEquals("Supplies||Marketing||Consulting", updatedBook.customCategories)
    assertEquals(3, updatedBook.getCategories().size)
  }

  @Test
  fun testActiveVsArchivedBooks() {
    val books = listOf(
      LedgerBook(id = 1, name = "Active Book 1", isArchived = false),
      LedgerBook(id = 2, name = "Archived Book 1", isArchived = true),
      LedgerBook(id = 3, name = "Active Book 2", isArchived = false),
      LedgerBook(id = 4, name = "Archived Book 2", isArchived = true)
    )

    val active = books.filter { !it.isArchived }
    val archived = books.filter { it.isArchived }

    assertEquals(2, active.size)
    assertEquals(2, archived.size)
    assertTrue(active.all { !it.isArchived })
    assertTrue(archived.all { it.isArchived })
  }

  @Test
  fun testCsvBookImportParsingAndHeaderDetection() {
    val csvContent = """
      # Ledger Book: Coffee Shop Operations
      # Exported on: 2026-09-15 10:00
      Date,Type,Amount,Category,Memo
      2026-09-15 08:30,Cash In,240.50,Beverages,Morning espresso sales
      2026-09-15 11:15,Cash Out,45.00,Supplies,Paper cups restocking
      2026-09-15 14:00,Income,180.00,Pastries,Bakery sales
      2026-09-15 17:30,Expense,65.20,Utilities,Water bill
    """.trimIndent()

    val summary = com.example.data.io.CsvExporterImporter.parseAndValidateCsv(
      java.io.ByteArrayInputStream(csvContent.toByteArray())
    )

    assertEquals("Coffee Shop Operations", summary.suggestedBookName)
    assertEquals(4, summary.totalRows)
    assertEquals(4, summary.validRows.size)
    assertEquals(0, summary.invalidRows.size)

    assertEquals(TransactionType.CASH_IN, summary.validRows[0].type)
    assertEquals(240.50, summary.validRows[0].amount!!, 0.001)
    assertEquals("Beverages", summary.validRows[0].categoryStr)

    assertEquals(TransactionType.CASH_OUT, summary.validRows[1].type)
    assertEquals(45.00, summary.validRows[1].amount!!, 0.001)
    assertEquals("Supplies", summary.validRows[1].categoryStr)

    // Verify categories can be extracted
    val extractedCategories = summary.validRows.map { it.categoryStr }.distinct()
    assertEquals(listOf("Beverages", "Supplies", "Pastries", "Utilities"), extractedCategories)
  }
}

