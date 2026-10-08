package com.example.data.model

enum class TransactionType(val label: String) {
    CASH_IN("Cash In"),
    CASH_OUT("Cash Out");

    val isIncome: Boolean
        get() = this == CASH_IN

    val isExpense: Boolean
        get() = this == CASH_OUT
}
