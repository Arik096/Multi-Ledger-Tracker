package com.example.data.io

import android.util.Xml
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ExcelExporterImporter {

    private val exportDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    fun createDemoTemplateXlsx(): ByteArray {
        val demoRows = listOf(
            listOf("Date", "Type", "Amount", "Category", "Memo"),
            listOf("2026-09-01 09:00", "Income", "4500.00", "Salary", "Monthly corporate salary"),
            listOf("2026-09-03 13:30", "Expense", "68.50", "Food & Dining", "Lunch with team"),
            listOf("2026-09-05 18:00", "Expense", "125.00", "Utilities", "Power & broadband bill"),
            listOf("2026-09-08 10:15", "Income", "350.00", "Freelance", "UI design contract"),
            listOf("2026-09-10 16:45", "Expense", "42.00", "Transportation", "Transit card reload"),
            listOf("2026-09-12 20:00", "Expense", "89.90", "Groceries", "Weekly groceries")
        )
        return buildXlsxBytes(demoRows)
    }

    fun exportTransactionsToXlsx(
        transactions: List<TransactionRecord>,
        bookName: String,
        currencySymbol: String
    ): ByteArray {
        val rows = mutableListOf<List<String>>()
        rows.add(listOf("Date", "Type", "Amount ($currencySymbol)", "Category", "Memo"))
        transactions.forEach { tx ->
            val dateStr = exportDateFormat.format(Date(tx.timestamp))
            val typeStr = if (tx.type == TransactionType.CASH_IN) "Income" else "Expense"
            val amountStr = String.format(Locale.US, "%.2f", tx.amount)
            rows.add(listOf(dateStr, typeStr, amountStr, tx.category, tx.memo))
        }
        return buildXlsxBytes(rows)
    }

    private fun buildXlsxBytes(rows: List<List<String>>): ByteArray {
        val baos = ByteArrayOutputStream()
        val zos = ZipOutputStream(baos)

        // 1. [Content_Types].xml
        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""
        addZipEntry(zos, "[Content_Types].xml", contentTypes.toByteArray())

        // 2. _rels/.rels
        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
        addZipEntry(zos, "_rels/.rels", rels.toByteArray())

        // 3. xl/workbook.xml
        val workbook = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Transactions" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""
        addZipEntry(zos, "xl/workbook.xml", workbook.toByteArray())

        // 4. xl/_rels/workbook.xml.rels
        val workbookRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""
        addZipEntry(zos, "xl/_rels/workbook.xml.rels", workbookRels.toByteArray())

        // 5. xl/worksheets/sheet1.xml
        val sheetBuilder = StringBuilder()
        sheetBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>""")

        val colLetters = listOf("A", "B", "C", "D", "E", "F", "G")
        for (rIdx in rows.indices) {
            val rowNum = rIdx + 1
            sheetBuilder.append("""<row r="$rowNum">""")
            val rowData = rows[rIdx]
            for (cIdx in rowData.indices) {
                val colLetter = colLetters.getOrElse(cIdx) { "A" }
                val cellRef = "$colLetter$rowNum"
                val cellValue = escapeXml(rowData[cIdx])
                // Use inlineStr for text
                sheetBuilder.append("""<c r="$cellRef" t="inlineStr"><is><t>$cellValue</t></is></c>""")
            }
            sheetBuilder.append("""</row>""")
        }

        sheetBuilder.append("""</sheetData>
</worksheet>""")
        addZipEntry(zos, "xl/worksheets/sheet1.xml", sheetBuilder.toString().toByteArray())

        zos.finish()
        zos.close()
        return baos.toByteArray()
    }

    private fun addZipEntry(zos: ZipOutputStream, path: String, data: ByteArray) {
        val entry = ZipEntry(path)
        zos.putNextEntry(entry)
        zos.write(data)
        zos.closeEntry()
    }

    private fun escapeXml(str: String): String {
        return str
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun parseAndValidateXlsx(inputStream: InputStream): ImportValidationSummary {
        val rawZipBytes = inputStream.readBytes()
        var sheetXmlBytes: ByteArray? = null
        val sharedStrings = mutableListOf<String>()

        val zis1 = ZipInputStream(ByteArrayInputStream(rawZipBytes))
        var entry: ZipEntry? = zis1.nextEntry
        while (entry != null) {
            if (entry.name.contains("sharedStrings.xml")) {
                parseSharedStrings(zis1.readBytes(), sharedStrings)
            } else if (entry.name.contains("sheet1.xml") || entry.name.contains("worksheets/sheet")) {
                sheetXmlBytes = zis1.readBytes()
            }
            entry = zis1.nextEntry
        }
        zis1.close()

        if (sheetXmlBytes == null) {
            // Could not find sheet XML, fallback to CSV parsing if it was text
            return CsvExporterImporter.parseAndValidateCsv(ByteArrayInputStream(rawZipBytes))
        }

        val rows = parseSheetXml(sheetXmlBytes, sharedStrings)
        if (rows.isEmpty()) {
            return ImportValidationSummary(0, emptyList(), emptyList(), emptyList())
        }

        // Convert parsed rows to CSV format representation and validate using CsvExporterImporter's logic
        val csvRepresentation = buildString {
            rows.forEach { row ->
                appendLine(row.joinToString(",") { cell ->
                    var s = cell.replace("\"", "\"\"")
                    if (s.contains(",") || s.contains("\"") || s.contains("\n")) s = "\"$s\""
                    s
                })
            }
        }

        return CsvExporterImporter.parseAndValidateCsv(ByteArrayInputStream(csvRepresentation.toByteArray()))
    }

    private fun parseSharedStrings(bytes: ByteArray, outList: MutableList<String>) {
        try {
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
            var eventType = parser.eventType
            var inT = false
            val currentText = StringBuilder()
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "t") {
                            inT = true
                            currentText.setLength(0)
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inT) {
                            currentText.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "t") {
                            inT = false
                            outList.add(currentText.toString())
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
        }
    }

    private fun parseSheetXml(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val result = mutableListOf<List<String>>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
            var eventType = parser.eventType

            var currentRow = mutableListOf<String>()
            var currentCell = StringBuilder()
            var cellType = ""
            var inV = false
            var inT = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "row" -> {
                                currentRow = mutableListOf()
                            }
                            "c" -> {
                                cellType = parser.getAttributeValue(null, "t") ?: ""
                                currentCell.setLength(0)
                            }
                            "v" -> inV = true
                            "t" -> inT = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inV || inT) {
                            currentCell.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "v" -> inV = false
                            "t" -> inT = false
                            "c" -> {
                                val rawText = currentCell.toString().trim()
                                val resolvedText = if (cellType == "s") {
                                    val idx = rawText.toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else rawText
                                } else {
                                    rawText
                                }
                                currentRow.add(resolvedText)
                            }
                            "row" -> {
                                if (currentRow.any { it.isNotBlank() }) {
                                    result.add(currentRow)
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
        }
        return result
    }
}
