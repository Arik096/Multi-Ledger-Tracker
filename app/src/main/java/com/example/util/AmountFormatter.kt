package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs

object AmountFormatter {

    /**
     * Floors the amount as per math rules to the requested decimal precision point.
     * - precision = 0: floor to integer, e.g. 1100.85 -> 1100
     * - precision = 1: floor to 1 decimal place, e.g. 1100.27 -> 1100.2
     * - precision = 2: floor to 2 decimal places, e.g. 1203.129 -> 1203.12
     */
    fun floorToPrecision(amount: Double, precision: Int): Double {
        val safePrecision = precision.coerceIn(0, 4)
        return try {
            BigDecimal.valueOf(amount)
                .setScale(safePrecision, RoundingMode.FLOOR)
                .toDouble()
        } catch (e: Exception) {
            amount
        }
    }

    /**
     * Formats amount according to precision:
     * - If precision == 0 -> "1100"
     * - If precision == 1 -> "1100.2"
     * - If precision == 2 -> "1203.12"
     */
    fun format(amount: Double, precision: Int = 0, includeCommas: Boolean = false): String {
        val safePrecision = precision.coerceIn(0, 4)
        val sign = if (amount < 0) "-" else ""
        val positive = abs(amount)

        return try {
            val bd = BigDecimal.valueOf(positive).setScale(safePrecision, RoundingMode.FLOOR)
            if (safePrecision == 0) {
                val longVal = bd.toLong()
                if (includeCommas) {
                    String.format(Locale.US, "$sign%,d", longVal)
                } else {
                    "$sign$longVal"
                }
            } else {
                val pattern = if (includeCommas) "$sign%,.${safePrecision}f" else "$sign%.${safePrecision}f"
                String.format(Locale.US, pattern, bd.toDouble())
            }
        } catch (e: Exception) {
            "$sign${positive.toLong()}"
        }
    }

    fun formatWithCurrency(symbol: String, amount: Double, precision: Int = 0, includeCommas: Boolean = false): String {
        val formatted = format(amount, precision, includeCommas = includeCommas)
        return if (symbol.isNotBlank()) "$symbol $formatted" else formatted
    }
}
