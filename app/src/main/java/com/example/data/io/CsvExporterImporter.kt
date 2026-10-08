package com.example.data.io

import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ParsedImportRow(
    val lineNumber: Int,
    val dateStr: String,
    val typeStr: String,
    val amountStr: String,
    val categoryStr: String,
    val memoStr: String,
    val paymentMode: String = "Cash",
    val timestamp: Long?,
    val type: TransactionType?,
    val amount: Double?,
    val errors: List<String> = emptyList()
) {
    val isValid: Boolean get() = errors.isEmpty() && timestamp != null && type != null && amount != null
}

data class ImportValidationSummary(
    val totalRows: Int,
    val validRows: List<ParsedImportRow>,
    val invalidRows: List<ParsedImportRow>,
    val detectedHeaders: List<String>,
    val suggestedBookName: String? = null
)

object CsvExporterImporter {

    private val dateTimeFormats = listOf(
        SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.US),
        SimpleDateFormat("d MMM yyyy hh:mm a", Locale.US),
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US),
        SimpleDateFormat("d MMM yyyy HH:mm", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US),
        SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US),
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    )

    private val dateOnlyFormats = listOf(
        SimpleDateFormat("dd MMM yyyy", Locale.US),
        SimpleDateFormat("d MMM yyyy", Locale.US),
        SimpleDateFormat("dd MMM yy", Locale.US),
        SimpleDateFormat("d MMM yy", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("yyyy/MM/dd", Locale.US),
        SimpleDateFormat("dd-MM-yyyy", Locale.US)
    )

    private val exportDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val exportTimeFormat = SimpleDateFormat("hh:mm a", Locale.US)

    fun generateDemoTemplateCsv(): String {
        return buildString {
            appendLine("\"Date\",\"Time\",\"Remark\",\"Category\",\"Mode\",\"Cash In\",\"Cash Out\",\"Balance\"")
            appendLine("\"31 Aug 2026\",\"08:20 PM\",\"From August 2026\",\"Balance\",\"Cash\",\"1138\",\"\",\"1138\"")
            appendLine("\"31 Aug 2026\",\"08:21 PM\",\"September 2026 Expenses\",\"Balance\",\"Online\",\"24000\",\"\",\"25138\"")
            appendLine("\"01 Sep 2026\",\"11:21 AM\",\"CNG\",\"Office Traveling\",\"Cash\",\"\",\"30\",\"25108\"")
            appendLine("\"01 Sep 2026\",\"03:04 PM\",\"Clash of Clans\",\"Bills Payment\",\"Online\",\"\",\"161\",\"24947\"")
            appendLine("\"01 Sep 2026\",\"03:14 PM\",\"Kichuri\",\"Lunch\",\"Cash\",\"\",\"666\",\"24281\"")
            appendLine("\"01 Sep 2026\",\"03:14 PM\",\"Gautam Training Gift\",\"Balance\",\"Cash\",\"206\",\"\",\"24487\"")
            appendLine("\"01 Sep 2026\",\"06:47 PM\",\"Tea\",\"Snacks\",\"Cash\",\"\",\"40\",\"24447\"")
        }
    }

    fun exportTransactionsToCsv(
        transactions: List<TransactionRecord>,
        bookName: String = "",
        currencySymbol: String = ""
    ): String {
        val sorted = transactions.sortedBy { it.timestamp }
        return buildString {
            // Header exactly matching user structure
            appendLine("\"Date\",\"Time\",\"Remark\",\"Category\",\"Mode\",\"Cash In\",\"Cash Out\",\"Balance\"")
            var runningBalance = 0.0
            sorted.forEach { tx ->
                val dateStr = exportDateFormat.format(Date(tx.timestamp))
                val timeStr = exportTimeFormat.format(Date(tx.timestamp))
                val remark = tx.memo.ifBlank { tx.category }
                val category = tx.category.ifBlank { "Balance" }
                val mode = tx.paymentMode.ifBlank { "Cash" }

                val cashInStr: String
                val cashOutStr: String
                if (tx.type == TransactionType.CASH_IN) {
                    runningBalance += tx.amount
                    cashInStr = formatAmount(tx.amount)
                    cashOutStr = ""
                } else {
                    runningBalance -= tx.amount
                    cashInStr = ""
                    cashOutStr = formatAmount(tx.amount)
                }
                val balanceStr = formatAmount(runningBalance)

                appendLine("\"$dateStr\",\"$timeStr\",\"${escapeCsv(remark)}\",\"${escapeCsv(category)}\",\"${escapeCsv(mode)}\",\"$cashInStr\",\"$cashOutStr\",\"$balanceStr\"")
            }
        }
    }

    private fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", amount)
        } else {
            String.format(Locale.US, "%.2f", amount)
        }
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    fun parseAndValidateCsv(inputStream: InputStream): ImportValidationSummary {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val rawLines = mutableListOf<String>()
        var detectedBookName: String? = null
        var line: String? = reader.readLine()
        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#") && (trimmed.contains("Ledger Book:", ignoreCase = true) || trimmed.contains("Book:", ignoreCase = true))) {
                detectedBookName = trimmed.substringAfter(":", "").trim().takeIf { it.isNotBlank() }
            }
            if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                rawLines.add(line)
            }
            line = reader.readLine()
        }

        if (rawLines.isEmpty()) {
            return ImportValidationSummary(0, emptyList(), emptyList(), emptyList(), detectedBookName)
        }

        // Header detection
        val headerTokens = parseCsvRow(rawLines[0]).map { it.trim().lowercase() }
        var dateIdx = -1
        var timeIdx = -1
        var remarkIdx = -1
        var categoryIdx = -1
        var modeIdx = -1
        var cashInIdx = -1
        var cashOutIdx = -1
        var typeIdx = -1
        var amountIdx = -1

        headerTokens.forEachIndexed { index, token ->
            when {
                token.contains("date") -> if (dateIdx == -1) dateIdx = index
                token.contains("time") -> if (timeIdx == -1) timeIdx = index
                token.contains("remark") || token.contains("memo") || token.contains("desc") || token.contains("title") || token.contains("note") -> if (remarkIdx == -1) remarkIdx = index
                token.contains("category") || token.contains("cat") -> if (categoryIdx == -1) categoryIdx = index
                token.contains("mode") || token.contains("payment") || token.contains("method") || token.contains("channel") -> if (modeIdx == -1) modeIdx = index
                token.contains("cash in") || token.contains("income") || token.contains("cash_in") -> if (cashInIdx == -1) cashInIdx = index
                token.contains("cash out") || token.contains("expense") || token.contains("cash_out") -> if (cashOutIdx == -1) cashOutIdx = index
                token.contains("type") -> if (typeIdx == -1) typeIdx = index
                token.contains("amount") || token.contains("sum") || token.contains("val") -> if (amountIdx == -1) amountIdx = index
            }
        }

        // Defaults if headers are standard positions
        if (dateIdx == -1) dateIdx = 0
        if (timeIdx == -1 && headerTokens.size > 1 && headerTokens[1].contains("time")) timeIdx = 1

        val validRows = mutableListOf<ParsedImportRow>()
        val invalidRows = mutableListOf<ParsedImportRow>()

        for (i in 1 until rawLines.size) {
            val tokens = parseCsvRow(rawLines[i])
            val dateStr = tokens.getOrNull(dateIdx)?.trim() ?: ""
            val timeStr = if (timeIdx != -1) tokens.getOrNull(timeIdx)?.trim() ?: "" else ""
            val remarkStr = if (remarkIdx != -1) tokens.getOrNull(remarkIdx)?.trim() ?: "" else ""
            val categoryStr = if (categoryIdx != -1) tokens.getOrNull(categoryIdx)?.trim()?.takeIf { it.isNotBlank() } ?: "Balance" else "Balance"
            val rawMode = if (modeIdx != -1) tokens.getOrNull(modeIdx)?.trim() ?: "" else ""
            val modeStr = normalizePaymentMode(rawMode)

            val errors = mutableListOf<String>()

            // Parse Date and Time
            val parsedTimestamp = parseDateWithTime(dateStr, timeStr)
            if (parsedTimestamp == null) {
                errors.add("Invalid Date/Time: '$dateStr $timeStr'. Formats: dd MMM yyyy hh:mm a, yyyy-MM-dd")
            }

            var parsedType: TransactionType? = null
            var parsedAmount: Double? = null
            var typeStr = ""
            var amountStr = ""

            // Check if separate Cash In / Cash Out columns exist
            if (cashInIdx != -1 || cashOutIdx != -1) {
                val inVal = if (cashInIdx != -1) tokens.getOrNull(cashInIdx)?.trim()?.replace("$", "")?.replace(",", "") ?: "" else ""
                val outVal = if (cashOutIdx != -1) tokens.getOrNull(cashOutIdx)?.trim()?.replace("$", "")?.replace(",", "") ?: "" else ""

                val inAmt = inVal.toDoubleOrNull()
                val outAmt = outVal.toDoubleOrNull()

                if (inAmt != null && inAmt > 0) {
                    parsedType = TransactionType.CASH_IN
                    parsedAmount = inAmt
                    typeStr = "Cash In"
                    amountStr = inVal
                } else if (outAmt != null && outAmt > 0) {
                    parsedType = TransactionType.CASH_OUT
                    parsedAmount = outAmt
                    typeStr = "Cash Out"
                    amountStr = outVal
                } else {
                    errors.add("Missing Cash In or Cash Out amount")
                }
            } else {
                // Fallback to Type and Amount columns
                val tStr = if (typeIdx != -1) tokens.getOrNull(typeIdx)?.trim() ?: "" else ""
                val aStr = if (amountIdx != -1) tokens.getOrNull(amountIdx)?.trim() ?: "" else ""
                parsedType = parseType(tStr)
                if (parsedType == null) {
                    errors.add("Invalid Type: '$tStr'")
                }
                val cleanAmount = aStr.replace("$", "").replace("€", "").replace("£", "").replace(",", "").trim()
                parsedAmount = cleanAmount.toDoubleOrNull()
                if (parsedAmount == null || parsedAmount <= 0) {
                    errors.add("Invalid Amount: '$aStr'")
                }
                typeStr = tStr
                amountStr = aStr
            }

            val parsedRow = ParsedImportRow(
                lineNumber = i + 1,
                dateStr = if (timeStr.isNotBlank()) "$dateStr $timeStr" else dateStr,
                typeStr = typeStr,
                amountStr = amountStr,
                categoryStr = categoryStr,
                memoStr = remarkStr,
                paymentMode = modeStr,
                timestamp = parsedTimestamp,
                type = parsedType,
                amount = parsedAmount,
                errors = errors
            )

            if (parsedRow.isValid) {
                validRows.add(parsedRow)
            } else {
                invalidRows.add(parsedRow)
            }
        }

        return ImportValidationSummary(
            totalRows = rawLines.size - 1,
            validRows = validRows,
            invalidRows = invalidRows,
            detectedHeaders = headerTokens,
            suggestedBookName = detectedBookName
        )
    }

    private fun parseCsvRow(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    curVal.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(curVal.toString())
                curVal = StringBuilder()
            } else {
                curVal.append(c)
            }
            i++
        }
        result.add(curVal.toString())
        return result
    }

    fun parseDateWithTime(dateStr: String, timeStr: String): Long? {
        if (dateStr.isBlank()) return null
        if (timeStr.isNotBlank()) {
            val combined = "$dateStr $timeStr".trim()
            for (format in dateTimeFormats) {
                try {
                    val d = format.parse(combined)
                    if (d != null) return d.time
                } catch (_: Exception) {
                }
            }
        }
        for (format in dateOnlyFormats) {
            try {
                val d = format.parse(dateStr)
                if (d != null) return d.time
            } catch (_: Exception) {
            }
        }
        for (format in dateTimeFormats) {
            try {
                val d = format.parse(dateStr)
                if (d != null) return d.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun parseDate(dateStr: String): Long? {
        return parseDateWithTime(dateStr, "")
    }

    fun normalizePaymentMode(raw: String): String {
        val clean = raw.trim()
        if (clean.isBlank()) return "Cash"
        val lower = clean.lowercase()
        return when {
            lower == "online" -> "Online"
            lower == "cash" -> "Cash"
            lower.contains("card") || lower.contains("credit") || lower.contains("debit") -> "Card"
            lower.contains("bank") || lower.contains("transfer") || lower.contains("neft") || lower.contains("rtgs") || lower.contains("wire") -> "Bank Transfer"
            lower.contains("cheque") || lower.contains("check") -> "Cheque"
            lower.contains("upi") || lower.contains("gpay") || lower.contains("phonepe") || lower.contains("paytm") || lower.contains("bkash") || lower.contains("nagad") || lower.contains("rocket") -> "Online"
            else -> clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }
    }

    private fun parseType(typeStr: String): TransactionType? {
        val lower = typeStr.trim().lowercase()
        return when {
            lower.contains("income") || lower.contains("cash in") || lower.contains("in") || lower.contains("credit") || lower.startsWith("+") -> TransactionType.CASH_IN
            lower.contains("expense") || lower.contains("cash out") || lower.contains("out") || lower.contains("debit") || lower.startsWith("-") -> TransactionType.CASH_OUT
            else -> null
        }
    }
}
