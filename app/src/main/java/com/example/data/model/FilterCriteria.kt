package com.example.data.model

import java.util.Calendar

enum class DateRangeFilter(val label: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    YEAR_TO_DATE("Year to Date"),
    CUSTOM("Custom Range")
}

data class FilterCriteria(
    val searchQuery: String = "",
    val dateRange: DateRangeFilter = DateRangeFilter.ALL_TIME,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val typeFilter: TransactionType? = null, // null means both
    val categoryFilter: String? = null,     // null means all categories
    val minAmount: Double? = null,
    val maxAmount: Double? = null
) {
    fun hasActiveFilters(): Boolean {
        return searchQuery.isNotBlank() ||
                dateRange != DateRangeFilter.ALL_TIME ||
                typeFilter != null ||
                !categoryFilter.isNullOrBlank() ||
                minAmount != null ||
                maxAmount != null
    }

    fun getTimeRangeMillis(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        return when (dateRange) {
            DateRangeFilter.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
            DateRangeFilter.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            DateRangeFilter.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            DateRangeFilter.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, maxDay)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            DateRangeFilter.YEAR_TO_DATE -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                val end = System.currentTimeMillis() + 86400000L * 365L // covers rest of year
                Pair(start, end)
            }
            DateRangeFilter.CUSTOM -> {
                val start = customStartDate ?: 0L
                val end = customEndDate ?: Long.MAX_VALUE
                Pair(start, end)
            }
        }
    }
}
