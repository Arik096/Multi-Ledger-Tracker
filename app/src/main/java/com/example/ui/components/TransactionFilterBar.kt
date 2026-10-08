package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DateRangeFilter
import com.example.data.model.FilterCriteria
import com.example.data.model.TransactionType

@Composable
fun TransactionFilterBar(
    criteria: FilterCriteria,
    onSearchChange: (String) -> Unit,
    onDateRangeSelect: (DateRangeFilter) -> Unit,
    onTypeSelect: (TransactionType?) -> Unit,
    onOpenFilterDialog: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxWidth()) {
        // Search TextField + Filter Settings Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = criteria.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("global_search_input"),
                placeholder = { Text("Search memos, categories, amounts...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (criteria.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchChange("") },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Filter settings button with badge
            BadgedBox(
                badge = {
                    val activeCount = (if (criteria.typeFilter != null) 1 else 0) +
                            (if (!criteria.categoryFilter.isNullOrBlank()) 1 else 0) +
                            (if (criteria.minAmount != null || criteria.maxAmount != null) 1 else 0) +
                            (if (criteria.dateRange == DateRangeFilter.CUSTOM) 1 else 0)
                    if (activeCount > 0) {
                        Badge { Text("$activeCount") }
                    }
                }
            ) {
                IconButton(
                    onClick = onOpenFilterDialog,
                    modifier = Modifier.testTag("open_filter_dialog_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter options",
                        tint = if (criteria.hasActiveFilters()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal scrolling Quick Date Range & Type Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Quick Chips
            FilterChip(
                selected = criteria.typeFilter == null,
                onClick = { onTypeSelect(null) },
                label = { Text("All Types", fontSize = 12.sp) },
                modifier = Modifier.testTag("filter_chip_type_all")
            )

            FilterChip(
                selected = criteria.typeFilter == TransactionType.CASH_IN,
                onClick = {
                    onTypeSelect(if (criteria.typeFilter == TransactionType.CASH_IN) null else TransactionType.CASH_IN)
                },
                label = { Text("Cash In", fontSize = 12.sp) },
                modifier = Modifier.testTag("filter_chip_type_cash_in")
            )

            FilterChip(
                selected = criteria.typeFilter == TransactionType.CASH_OUT,
                onClick = {
                    onTypeSelect(if (criteria.typeFilter == TransactionType.CASH_OUT) null else TransactionType.CASH_OUT)
                },
                label = { Text("Cash Out", fontSize = 12.sp) },
                modifier = Modifier.testTag("filter_chip_type_cash_out")
            )

            // Date Range Quick Chips
            DateRangeFilter.values().forEach { range ->
                FilterChip(
                    selected = criteria.dateRange == range,
                    onClick = {
                        if (range == DateRangeFilter.CUSTOM) {
                            onOpenFilterDialog()
                        } else {
                            onDateRangeSelect(range)
                        }
                    },
                    label = { Text(range.label, fontSize = 12.sp) },
                    modifier = Modifier.testTag("filter_chip_date_${range.name}")
                )
            }

            // Clear filter button if filters are active
            AnimatedVisibility(visible = criteria.hasActiveFilters()) {
                FilterChip(
                    selected = false,
                    onClick = onClearFilters,
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("filter_chip_reset")
                )
            }
        }
    }
}
