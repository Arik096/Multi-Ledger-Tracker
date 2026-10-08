package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType?): String {
        return type?.name ?: TransactionType.CASH_OUT.name
    }

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType {
        return try {
            if (value != null) TransactionType.valueOf(value) else TransactionType.CASH_OUT
        } catch (e: Exception) {
            TransactionType.CASH_OUT
        }
    }
}
