package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Stack

/**
 * Utility to evaluate mathematical expressions with standard precedence (+, -, *, /).
 * Supports decimals, chained operations (e.g. 100 + 50 * 2 = 200), parentheses, and percentages.
 */
object MathExpressionEvaluator {

    /**
     * Attempts to evaluate an expression string.
     * Returns null if invalid or cannot be parsed.
     */
    fun evaluate(expression: String): Double? {
        val sanitized = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .trim()

        if (sanitized.isBlank()) return null

        return try {
            val tokens = tokenize(sanitized)
            if (tokens.isEmpty()) return null
            evaluateTokens(tokens)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats evaluation result nicely:
     * e.g., 20.0 -> "20", 20.5 -> "20.5", 20.5555 -> "20.56"
     */
    fun formatResult(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            // Keep up to 2 decimal places cleanly
            BigDecimal(value)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
        }
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        val len = expr.length

        while (i < len) {
            val c = expr[i]
            when {
                c.isWhitespace() -> {
                    i++
                }
                c in "+-*/()" -> {
                    // Check if '-' is unary (at start or preceded by operator or '(')
                    if (c == '-' && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/", "("))) {
                        // Unary minus: parse as part of next number
                        var j = i + 1
                        while (j < len && (expr[j].isDigit() || expr[j] == '.')) {
                            j++
                        }
                        if (j > i + 1) {
                            tokens.add(expr.substring(i, j))
                            i = j
                        } else {
                            tokens.add(c.toString())
                            i++
                        }
                    } else {
                        tokens.add(c.toString())
                        i++
                    }
                }
                c == '%' -> {
                    tokens.add("%")
                    i++
                }
                c.isDigit() || c == '.' -> {
                    var j = i
                    while (j < len && (expr[j].isDigit() || expr[j] == '.')) {
                        j++
                    }
                    tokens.add(expr.substring(i, j))
                    i = j
                }
                else -> {
                    // Invalid character
                    i++
                }
            }
        }
        return tokens
    }

    private fun evaluateTokens(tokens: List<String>): Double {
        // Shunting-yard algorithm to evaluate infix expressions with operator precedence
        val values = Stack<Double>()
        val ops = Stack<String>()

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]

            when {
                token.toDoubleOrNull() != null -> {
                    var num = token.toDouble()
                    // Check if immediately followed by %
                    if (i + 1 < tokens.size && tokens[i + 1] == "%") {
                        num /= 100.0
                        i++
                    }
                    values.push(num)
                }
                token == "(" -> {
                    ops.push(token)
                }
                token == ")" -> {
                    while (!ops.isEmpty() && ops.peek() != "(") {
                        applyOp(ops.pop(), values)
                    }
                    if (!ops.isEmpty() && ops.peek() == "(") {
                        ops.pop()
                    }
                }
                token in listOf("+", "-", "*", "/") -> {
                    while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(token)) {
                        applyOp(ops.pop(), values)
                    }
                    ops.push(token)
                }
            }
            i++
        }

        while (!ops.isEmpty()) {
            applyOp(ops.pop(), values)
        }

        return if (values.isEmpty()) 0.0 else values.pop()
    }

    private fun precedence(op: String): Int {
        return when (op) {
            "+", "-" -> 1
            "*", "/" -> 2
            else -> 0
        }
    }

    private fun applyOp(op: String, values: Stack<Double>) {
        if (values.size < 2) return
        val b = values.pop()
        val a = values.pop()
        when (op) {
            "+" -> values.push(a + b)
            "-" -> values.push(a - b)
            "*" -> values.push(a * b)
            "/" -> {
                if (b != 0.0) {
                    values.push(a / b)
                } else {
                    values.push(0.0)
                }
            }
        }
    }
}
