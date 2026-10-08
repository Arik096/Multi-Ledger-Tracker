package com.example.data.io

import android.content.Context
import com.example.data.model.CategoryConstants
import com.example.data.model.LedgerBook
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.InflaterInputStream

object PdfImporter {

    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US),
        SimpleDateFormat("MMM dd, yyyy", Locale.US),
        SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US),
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US)
    )

    data class PdfImportResult(
        val detectedBookName: String,
        val summary: ImportValidationSummary
    )

    /**
     * Parses a PDF file input stream, extracts textual data from both uncompressed
     * and FlateDecode streams, and detects ledger book transactions.
     */
    fun parsePdfFile(inputStream: InputStream, fallbackTitle: String = "Imported PDF Ledger"): PdfImportResult {
        val pdfBytes = inputStream.readBytes()
        val extractedText = extractAllTextFromPdfBytes(pdfBytes)

        // Try to identify a book name in header lines
        var bookName = fallbackTitle
        val ledgerNameRegex = Regex("""(?:Ledger(?:\s*Book)?\s*:\s*|Book\s*:\s*)([^\r\n•\(\)]+)""", RegexOption.IGNORE_CASE)
        val match = ledgerNameRegex.find(extractedText)
        if (match != null) {
            val title = match.groupValues[1].trim()
            if (title.isNotBlank()) {
                bookName = title
            }
        }

        val parsedRows = parseTransactionsFromExtractedText(extractedText)

        val valid = parsedRows.filter { it.isValid }
        val invalid = parsedRows.filter { !it.isValid }

        val summary = ImportValidationSummary(
            totalRows = parsedRows.size,
            validRows = valid,
            invalidRows = invalid,
            detectedHeaders = listOf("Date", "Type", "Amount", "Category", "Memo")
        )

        return PdfImportResult(bookName, summary)
    }

    private fun extractAllTextFromPdfBytes(pdfBytes: ByteArray): String {
        val textBuilder = StringBuilder()

        // 1. Search for FlateDecode streams and decompress them
        val streamTag = "stream".toByteArray(Charsets.ISO_8859_1)
        val endStreamTag = "endstream".toByteArray(Charsets.ISO_8859_1)

        var index = 0
        while (index < pdfBytes.size) {
            val streamStart = indexOf(pdfBytes, streamTag, index)
            if (streamStart == -1) break

            val dataStart = skipNewline(pdfBytes, streamStart + streamTag.size)
            val streamEnd = indexOf(pdfBytes, endStreamTag, dataStart)
            if (streamEnd == -1) break

            val streamBytes = pdfBytes.copyOfRange(dataStart, streamEnd)

            // Try decompressing with Inflater
            val decompressed = tryDecompressFlate(streamBytes)
            if (decompressed != null) {
                textBuilder.append(extractStringsFromPdfStream(decompressed)).append("\n")
            } else {
                // If not flate-compressed, try extracting directly
                val rawStr = String(streamBytes, Charsets.ISO_8859_1)
                textBuilder.append(extractStringsFromPdfStream(rawStr)).append("\n")
            }

            index = streamEnd + endStreamTag.size
        }

        // Also search for literal text outside streams
        val wholePdfStr = String(pdfBytes, Charsets.ISO_8859_1)
        textBuilder.append(extractStringsFromPdfStream(wholePdfStr))

        return textBuilder.toString()
    }

    private fun tryDecompressFlate(data: ByteArray): String? {
        return try {
            val bis = ByteArrayInputStream(data)
            val iis = InflaterInputStream(bis)
            val baos = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var len: Int
            while (iis.read(buffer).also { len = it } > 0) {
                baos.write(buffer, 0, len)
            }
            baos.toString("UTF-8")
        } catch (_: Exception) {
            null
        }
    }

    private fun extractStringsFromPdfStream(streamText: String): String {
        val sb = StringBuilder()

        // Match PDF text operators: (string) Tj or [(str1) 20 (str2)] TJ
        val tjRegex = Regex("""\(([^)]*)\)\s*Tj""")
        tjRegex.findAll(streamText).forEach { m ->
            sb.append(m.groupValues[1]).append(" ")
        }

        val arrayTjRegex = Regex("""\[(.*?)\]\s*TJ""")
        arrayTjRegex.findAll(streamText).forEach { m ->
            val inner = m.groupValues[1]
            val innerMatches = Regex("""\(([^)]*)\)""").findAll(inner)
            for (im in innerMatches) {
                sb.append(im.groupValues[1]).append(" ")
            }
            sb.append("\n")
        }

        // Plain lines
        streamText.lines().forEach { line ->
            if (line.contains("202") || line.contains("Cash In") || line.contains("Cash Out") ||
                line.contains("Income") || line.contains("Expense") || line.contains("Ledger:")
            ) {
                sb.append(line).append("\n")
            }
        }

        return sb.toString()
    }

    private fun parseTransactionsFromExtractedText(text: String): List<ParsedImportRow> {
        val rows = mutableListOf<ParsedImportRow>()
        val lines = text.split("\n", "\r\n")

        var lineNum = 1
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#") || line.startsWith("FINANCIAL STATEMENT") || line.startsWith("Generated on")) {
                continue
            }

            // Look for Date pattern: YYYY-MM-DD or MMM DD, YYYY or MM/DD/YYYY
            val dateMatch = Regex("""(\d{4}-\d{2}-\d{2}(?:\s+\d{2}:\d{2})?|[A-Za-z]{3}\s+\d{1,2},?\s+\d{4}(?:\s+\d{2}:\d{2})?|\d{1,2}/\d{1,2}/\d{4}(?:\s+\d{2}:\d{2})?)""").find(line)
            if (dateMatch == null) {
                continue
            }

            val dateStr = dateMatch.groupValues[1]
            val rest = line.removePrefix(dateStr).trim()

            // Look for Type: Cash In, Cash Out, Income, Expense, Deposit, Withdrawal, CR, DR
            val typeRegex = Regex("""\b(Cash In|Cash Out|Income|Expense|Deposit|Withdrawal|CR|DR)\b""", RegexOption.IGNORE_CASE)
            val typeMatch = typeRegex.find(rest)
            val typeStr = typeMatch?.value ?: "Expense"
            val type = when (typeStr.lowercase()) {
                "cash in", "income", "deposit", "cr" -> TransactionType.CASH_IN
                else -> TransactionType.CASH_OUT
            }

            // Look for amount: numeric like 1,234.56 or 50.00
            val amountRegex = Regex("""(?:[\$€£₹¥])?(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+\.\d{1,2}|\d+)""")
            val amountMatches = amountRegex.findAll(rest).toList()
            val amountStr = amountMatches.lastOrNull()?.groupValues?.get(1)?.replace(",", "") ?: ""
            val amount = amountStr.toDoubleOrNull()

            // Parse timestamp
            var timestamp: Long? = null
            for (df in dateFormats) {
                try {
                    val d = df.parse(dateStr)
                    if (d != null) {
                        timestamp = d.time
                        break
                    }
                } catch (_: Exception) {}
            }

            // Extract category and memo
            var category = if (type == TransactionType.CASH_IN) "Other Income" else "Other Expense"
            var memo = ""

            // Check known categories
            val allKnownCategories = CategoryConstants.defaultExpenseCategories + CategoryConstants.defaultIncomeCategories
            for (cat in allKnownCategories) {
                if (rest.contains(cat, ignoreCase = true)) {
                    category = cat
                    break
                }
            }

            // Use leftover words for memo
            val cleanedRest = rest
                .replace(typeStr, "", ignoreCase = true)
                .replace(amountStr, "")
                .replace(category, "", ignoreCase = true)
                .replace(Regex("""[•|$€£¥,;|\\]"""), " ")
                .trim()
            if (cleanedRest.isNotBlank() && cleanedRest.length > 2) {
                memo = cleanedRest.take(60)
            }

            val errors = mutableListOf<String>()
            if (timestamp == null) errors.add("Invalid date '$dateStr'")
            if (amount == null || amount <= 0) errors.add("Invalid amount '$amountStr'")

            rows.add(
                ParsedImportRow(
                    lineNumber = lineNum++,
                    dateStr = dateStr,
                    typeStr = typeStr,
                    amountStr = amountStr,
                    categoryStr = category,
                    memoStr = memo,
                    timestamp = timestamp,
                    type = type,
                    amount = amount,
                    errors = errors
                )
            )
        }

        return rows
    }

    /**
     * Generates a sample PDF financial statement to enable one-tap demo import.
     */
    fun createSamplePdfStatement(context: Context): File {
        val sampleBook = LedgerBook(
            id = 999L,
            name = "Quarterly Statement",
            colorHex = 0xFF0D9488L
        )

        val cal = java.util.Calendar.getInstance()
        val now = cal.timeInMillis

        val sampleTxs = listOf(
            TransactionRecord(
                id = 1,
                ledgerBookId = 999,
                type = TransactionType.CASH_IN,
                amount = 4800.00,
                category = "Salary",
                timestamp = now - (86400000L * 15),
                memo = "Primary consulting retainer"
            ),
            TransactionRecord(
                id = 2,
                ledgerBookId = 999,
                type = TransactionType.CASH_OUT,
                amount = 1350.00,
                category = "Housing / Rent",
                timestamp = now - (86400000L * 12),
                memo = "Monthly office lease"
            ),
            TransactionRecord(
                id = 3,
                ledgerBookId = 999,
                type = TransactionType.CASH_OUT,
                amount = 185.50,
                category = "Utilities",
                timestamp = now - (86400000L * 9),
                memo = "Fiber optic internet & electricity"
            ),
            TransactionRecord(
                id = 4,
                ledgerBookId = 999,
                type = TransactionType.CASH_IN,
                amount = 750.00,
                category = "Freelance",
                timestamp = now - (86400000L * 6),
                memo = "UI/UX component design"
            ),
            TransactionRecord(
                id = 5,
                ledgerBookId = 999,
                type = TransactionType.CASH_OUT,
                amount = 142.20,
                category = "Groceries",
                timestamp = now - (86400000L * 3),
                memo = "Weekly supermarket provisions"
            ),
            TransactionRecord(
                id = 6,
                ledgerBookId = 999,
                type = TransactionType.CASH_OUT,
                amount = 65.00,
                category = "Food & Dining",
                timestamp = now - (86400000L * 1),
                memo = "Client business dinner"
            )
        )

        return PdfReportGenerator.generateFinancialStatement(
            context = context,
            book = sampleBook,
            transactions = sampleTxs,
            timeframeLabel = "Current Quarter"
        )
    }

    private fun indexOf(source: ByteArray, target: ByteArray, fromIndex: Int): Int {
        if (target.isEmpty() || fromIndex >= source.size) return -1
        outer@ for (i in fromIndex..(source.size - target.size)) {
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    continue@outer
                }
            }
            return i
        }
        return -1
    }

    private fun skipNewline(source: ByteArray, start: Int): Int {
        var idx = start
        while (idx < source.size && (source[idx] == '\r'.code.toByte() || source[idx] == '\n'.code.toByte())) {
            idx++
        }
        return idx
    }
}
