package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

object AmountFormatter {

    /**
     * Floored whole integer calculation with zero decimal places as requested:
     * "there will be no decimal point in the app"
     */
    fun floorToPrecision(amount: Double, precision: Int = 0): Double {
        return floor(amount)
    }

    /**
     * Formats strictly as whole numbers without any decimal points.
     * Examples: 1100, 24000, 1203
     */
    fun format(amount: Double, precision: Int = 0, includeCommas: Boolean = false): String {
        val sign = if (amount < 0) "-" else ""
        val whole = floor(abs(amount)).toLong()
        return if (includeCommas) {
            String.format(Locale.US, "$sign%,d", whole)
        } else {
            "$sign$whole"
        }
    }

    fun formatWithCurrency(symbol: String, amount: Double, precision: Int = 0, includeCommas: Boolean = false): String {
        val formatted = format(amount, includeCommas = includeCommas)
        return if (symbol.isNotBlank()) "$symbol $formatted" else formatted
    }
}
