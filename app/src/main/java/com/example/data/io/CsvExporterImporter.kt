package com.example.data.io

import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

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
    val isValid: Boolean get() = errors.isEmpty() && timestamp != null && type != null && amount != null && amount > 0
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
        SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.US),
        SimpleDateFormat("d-MMM-yyyy hh:mm a", Locale.US),
        SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US),
        SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US),
        SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US),
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US),
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    )

    private val dateOnlyFormats = listOf(
        SimpleDateFormat("dd MMM yyyy", Locale.US),
        SimpleDateFormat("d MMM yyyy", Locale.US),
        SimpleDateFormat("dd-MMM-yyyy", Locale.US),
        SimpleDateFormat("d-MMM-yyyy", Locale.US),
        SimpleDateFormat("dd/MMM/yyyy", Locale.US),
        SimpleDateFormat("dd MMM yy", Locale.US),
        SimpleDateFormat("d MMM yy", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("yyyy/MM/dd", Locale.US),
        SimpleDateFormat("yyyy.MM.dd", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("dd-MM-yyyy", Locale.US),
        SimpleDateFormat("dd.MM.yyyy", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US),
        SimpleDateFormat("MM-dd-yyyy", Locale.US),
        SimpleDateFormat("dd/MM/yy", Locale.US),
        SimpleDateFormat("MM/dd/yy", Locale.US)
    )

    private val exportDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val exportTimeFormat = SimpleDateFormat("hh:mm a", Locale.US)

    fun generateDemoTemplateCsv(): String {
        return buildString {
            appendLine("\"Date\",\"Time\",\"Remark\",\"Category\",\"Mode\",\"Cash In\",\"Cash Out\",\"Balance\"")
            appendLine("\"31 Aug 2026\",\"08:20 PM\",\"From August 2026\",\"Balance\",\"Cash\",\"1138\",\"\",\"1138\"")
            appendLine("\"31 Aug 2026\",\"08:21 PM\",\"September 2026 Expenses\",\"Balance\",\"Online\",\"24000\",\"\",\"25138\"")
            appendLine("\"01 Sep 2026\",\"11:21 AM\",\"CNG\",\"Office Traveling\",\"Cash\",\"\",\"30\",\"25108\"")
            appendLine("\"01 Sep 2026\",\"03:04 PM\",\"Mobile Recharge\",\"Bills Payment\",\"Online\",\"\",\"160\",\"24948\"")
            appendLine("\"01 Sep 2026\",\"03:14 PM\",\"Lunch Meal\",\"Food\",\"Cash\",\"\",\"660\",\"24288\"")
            appendLine("\"01 Sep 2026\",\"03:14 PM\",\"Gift / Bonus\",\"Bonus\",\"Cash\",\"200\",\"\",\"24488\"")
            appendLine("\"01 Sep 2026\",\"06:47 PM\",\"Tea & Snacks\",\"Snacks\",\"Cash\",\"\",\"40\",\"24448\"")
        }
    }

    fun exportTransactionsToCsv(
        transactions: List<TransactionRecord>,
        bookName: String = "",
        currencySymbol: String = ""
    ): String {
        val sorted = transactions.sortedBy { it.timestamp }
        return buildString {
            appendLine("\"Date\",\"Time\",\"Remark\",\"Category\",\"Mode\",\"Cash In\",\"Cash Out\",\"Balance\"")
            var runningBalance = 0.0
            sorted.forEach { tx ->
                val dateStr = exportDateFormat.format(Date(tx.timestamp))
                val timeStr = exportTimeFormat.format(Date(tx.timestamp))
                val remark = tx.memo.ifBlank { tx.category }
                val category = tx.category.ifBlank { "General" }
                val mode = tx.paymentMode.ifBlank { "Cash" }

                val wholeAmount = floor(tx.amount).toLong()
                val cashInStr: String
                val cashOutStr: String
                if (tx.type == TransactionType.CASH_IN) {
                    runningBalance += wholeAmount
                    cashInStr = wholeAmount.toString()
                    cashOutStr = ""
                } else {
                    runningBalance -= wholeAmount
                    cashInStr = ""
                    cashOutStr = wholeAmount.toString()
                }
                val balanceStr = floor(runningBalance).toLong().toString()

                appendLine("\"$dateStr\",\"$timeStr\",\"${escapeCsv(remark)}\",\"${escapeCsv(category)}\",\"${escapeCsv(mode)}\",\"$cashInStr\",\"$cashOutStr\",\"$balanceStr\"")
            }
        }
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    fun parseAndValidateCsv(inputStream: InputStream): ImportValidationSummary {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val rawLines = mutableListOf<String>()
        var detectedBookName: String? = null
        var line: String? = reader.readLine()

        while (line != null) {
            var trimmed = line.trim()
            // Strip UTF-8 BOM if present at start of line
            if (trimmed.startsWith("\uFEFF")) {
                trimmed = trimmed.removePrefix("\uFEFF").trim()
            }
            if (trimmed.startsWith("#") && (trimmed.contains("Ledger Book:", ignoreCase = true) || trimmed.contains("Book:", ignoreCase = true))) {
                detectedBookName = trimmed.substringAfter(":", "").trim().takeIf { it.isNotBlank() }
            }
            if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                rawLines.add(trimmed)
            }
            line = reader.readLine()
        }

        if (rawLines.isEmpty()) {
            return ImportValidationSummary(0, emptyList(), emptyList(), emptyList(), detectedBookName)
        }

        // Detect delimiter: check commas vs semicolons vs tabs on first line
        val sampleLine = rawLines[0]
        val delimiter = when {
            sampleLine.count { it == ';' } > sampleLine.count { it == ',' } -> ';'
            sampleLine.count { it == '\t' } > sampleLine.count { it == ',' } -> '\t'
            else -> ','
        }

        // Locate header row among first 5 lines
        var headerRowIndex = 0
        var headerTokens = parseCsvRow(rawLines[0], delimiter).map { cleanHeader(it) }

        val hasRecognizedHeaders = headerTokens.any { token ->
            token.contains("date") || token.contains("amount") || token.contains("cash") ||
                    token.contains("income") || token.contains("expense") || token.contains("credit") ||
                    token.contains("debit") || token.contains("in") || token.contains("out")
        }

        if (!hasRecognizedHeaders && rawLines.size > 1) {
            for (idx in 1 until minOf(rawLines.size, 5)) {
                val candidateTokens = parseCsvRow(rawLines[idx], delimiter).map { cleanHeader(it) }
                if (candidateTokens.any { it.contains("date") || it.contains("amount") || it.contains("cash in") || it.contains("income") || it.contains("expense") }) {
                    headerRowIndex = idx
                    headerTokens = candidateTokens
                    break
                }
            }
        }

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
                token.contains("date") || token == "dt" || token == "day" -> if (dateIdx == -1) dateIdx = index
                token.contains("time") && !token.contains("date") -> if (timeIdx == -1) timeIdx = index
                token.contains("remark") || token.contains("memo") || token.contains("desc") || token.contains("note") ||
                        token.contains("particular") || token.contains("narration") || token.contains("title") || token.contains("detail") -> if (remarkIdx == -1) remarkIdx = index
                token.contains("category") || token.contains("cat") || token.contains("head") || token.contains("group") -> if (categoryIdx == -1) categoryIdx = index
                token.contains("mode") || token.contains("payment") || token.contains("method") || token.contains("channel") -> if (modeIdx == -1) modeIdx = index
                token.contains("type") || token == "in/out" || token == "in / out" || token == "cr/dr" || token == "dr/cr" || token == "entry type" -> if (typeIdx == -1) typeIdx = index
                token.contains("cash in") || token.contains("income") || token.contains("cash_in") || token.contains("credit") ||
                        token == "cr" || token.contains("deposit") || token.contains("received") || token == "in" -> if (cashInIdx == -1) cashInIdx = index
                token.contains("cash out") || token.contains("expense") || token.contains("cash_out") || token.contains("debit") ||
                        token == "dr" || token.contains("withdrawal") || token.contains("paid") || token == "out" -> if (cashOutIdx == -1) cashOutIdx = index
                token.contains("amount") || token.contains("sum") || token.contains("val") || token.contains("total") || token.contains("price") || token == "amt" -> if (amountIdx == -1) amountIdx = index
            }
        }

        // Sensible fallbacks if headers weren't explicitly named
        if (dateIdx == -1) dateIdx = 0
        if (timeIdx == -1 && headerTokens.size > 1 && headerTokens[1].contains("time")) timeIdx = 1

        val validRows = mutableListOf<ParsedImportRow>()
        val invalidRows = mutableListOf<ParsedImportRow>()

        for (i in (headerRowIndex + 1) until rawLines.size) {
            val tokens = parseCsvRow(rawLines[i], delimiter)
            if (tokens.isEmpty() || tokens.all { it.isBlank() }) continue

            val dateStr = tokens.getOrNull(dateIdx)?.trim() ?: ""
            val timeStr = if (timeIdx != -1) tokens.getOrNull(timeIdx)?.trim() ?: "" else ""
            val remarkStr = if (remarkIdx != -1) tokens.getOrNull(remarkIdx)?.trim() ?: "" else ""
            val categoryStr = if (categoryIdx != -1) tokens.getOrNull(categoryIdx)?.trim()?.takeIf { it.isNotBlank() } ?: "General" else "General"
            val rawMode = if (modeIdx != -1) tokens.getOrNull(modeIdx)?.trim() ?: "" else ""
            val modeStr = normalizePaymentMode(rawMode)

            val errors = mutableListOf<String>()

            // 1. Parse Date and Time (gracefully fallback to current time if unparseable so entry is never dropped)
            var parsedTimestamp = parseDateWithTime(dateStr, timeStr)
            if (parsedTimestamp == null) {
                parsedTimestamp = System.currentTimeMillis()
            }

            var parsedType: TransactionType? = null
            var parsedAmount: Double? = null
            var typeStr = ""
            var amountStr = ""

            // 2. Check if separate Cash In / Cash Out columns exist and contain valid values
            if (cashInIdx != -1 || cashOutIdx != -1) {
                val inVal = if (cashInIdx != -1) tokens.getOrNull(cashInIdx)?.trim() ?: "" else ""
                val outVal = if (cashOutIdx != -1) tokens.getOrNull(cashOutIdx)?.trim() ?: "" else ""

                val inAmt = parseNumericAmount(inVal)
                val outAmt = parseNumericAmount(outVal)

                if (inAmt != null && inAmt > 0) {
                    parsedType = TransactionType.CASH_IN
                    parsedAmount = floor(inAmt)
                    typeStr = "Cash In"
                    amountStr = parsedAmount.toLong().toString()
                } else if (outAmt != null && outAmt > 0) {
                    parsedType = TransactionType.CASH_OUT
                    parsedAmount = floor(outAmt)
                    typeStr = "Cash Out"
                    amountStr = parsedAmount.toLong().toString()
                }
            }

            // 3. Fallback to Type and Amount columns if in/out columns didn't produce a value
            if (parsedAmount == null) {
                val tStr = if (typeIdx != -1) tokens.getOrNull(typeIdx)?.trim() ?: "" else ""
                val aStr = if (amountIdx != -1) tokens.getOrNull(amountIdx)?.trim() ?: "" else ""

                var num = parseNumericAmount(aStr)

                // If amountIdx failed or was not set, scan other columns for a valid numeric value
                if (num == null) {
                    for (cIdx in tokens.indices) {
                        if (cIdx != dateIdx && cIdx != timeIdx && cIdx != typeIdx) {
                            val candidate = parseNumericAmount(tokens[cIdx])
                            if (candidate != null && candidate != 0.0) {
                                num = candidate
                                break
                            }
                        }
                    }
                }

                if (num != null && num != 0.0) {
                    val isNegative = (aStr.contains("-") || (aStr.startsWith("(") && aStr.endsWith(")"))) || num < 0
                    val inferredType = if (isNegative) {
                        TransactionType.CASH_OUT
                    } else {
                        parseType(tStr) ?: TransactionType.CASH_OUT
                    }
                    parsedType = inferredType
                    parsedAmount = floor(abs(num))
                    typeStr = if (inferredType == TransactionType.CASH_IN) "Cash In" else "Cash Out"
                    amountStr = parsedAmount.toLong().toString()
                } else {
                    errors.add("Invalid Amount in row")
                }
            }

            val parsedRow = ParsedImportRow(
                lineNumber = i + 1,
                dateStr = if (timeStr.isNotBlank()) "$dateStr $timeStr" else dateStr,
                typeStr = typeStr,
                amountStr = amountStr,
                categoryStr = categoryStr,
                memoStr = remarkStr.ifBlank { categoryStr },
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
            totalRows = validRows.size + invalidRows.size,
            validRows = validRows,
            invalidRows = invalidRows,
            detectedHeaders = headerTokens,
            suggestedBookName = detectedBookName
        )
    }

    private fun cleanHeader(header: String): String {
        return header.trim().lowercase().removePrefix("\uFEFF").replace("\"", "").replace("'", "")
    }

    private fun parseNumericAmount(raw: String): Double? {
        if (raw.isBlank()) return null
        val clean = raw.replace("$", "")
            .replace("€", "")
            .replace("£", "")
            .replace("₹", "")
            .replace("৳", "")
            .replace("¥", "")
            .replace("rs.", "", ignoreCase = true)
            .replace("rs", "", ignoreCase = true)
            .replace("cad", "", ignoreCase = true)
            .replace("usd", "", ignoreCase = true)
            .replace("bdt", "", ignoreCase = true)
            .replace("inr", "", ignoreCase = true)
            .replace("(", "-")
            .replace(")", "")
            .replace("\"", "")
            .replace("'", "")
            .replace(",", "")
            .replace(" ", "")
            .trim()
            .removePrefix("+")
        return clean.toDoubleOrNull()
    }

    private fun parseCsvRow(line: String, delimiter: Char = ','): List<String> {
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
            } else if (c == delimiter && !inQuotes) {
                result.add(curVal.toString().trim())
                curVal = StringBuilder()
            } else {
                curVal.append(c)
            }
            i++
        }
        result.add(curVal.toString().trim())
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

    fun normalizePaymentMode(raw: String): String {
        val clean = raw.trim()
        if (clean.isBlank()) return "Cash"
        val lower = clean.lowercase()
        return when {
            lower == "online" -> "Online"
            lower == "cash" -> "Cash"
            lower.contains("card") || lower.contains("credit") || lower.contains("debit") -> "Card"
            lower.contains("bank") || lower.contains("transfer") || lower.contains("neft") || lower.contains("wire") -> "Bank Transfer"
            lower.contains("cheque") || lower.contains("check") -> "Cheque"
            lower.contains("upi") || lower.contains("gpay") || lower.contains("phonepe") || lower.contains("paytm") || lower.contains("bkash") || lower.contains("nagad") -> "Online"
            else -> clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }
    }

    private fun parseType(typeStr: String): TransactionType? {
        val lower = typeStr.trim().lowercase()
        return when {
            lower.contains("income") || lower.contains("cash in") || lower.contains("credit") ||
                    lower.contains("cr") || lower.contains("deposit") || lower.contains("received") ||
                    lower == "in" || lower.startsWith("+") -> TransactionType.CASH_IN
            lower.contains("expense") || lower.contains("cash out") || lower.contains("debit") ||
                    lower.contains("dr") || lower.contains("payment") || lower.contains("paid") ||
                    lower.contains("withdrawal") || lower == "out" || lower.startsWith("-") -> TransactionType.CASH_OUT
            else -> null
        }
    }
}
