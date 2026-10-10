package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import com.example.ui.theme.CategoryColors
import com.example.ui.viewmodel.CategorySummary
import com.example.ui.viewmodel.TrendPoint
import com.example.util.AmountFormatter
import java.util.Locale
import kotlin.math.max

@Composable
fun SpendingTrendChart(
    points: List<TrendPoint>,
    currencySymbol: String = "",
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No trend data available for current period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxExpense = points.maxOfOrNull { it.expense } ?: 1.0
    val maxIncome = points.maxOfOrNull { it.income } ?: 1.0
    val ceiling = max(maxExpense, maxIncome).coerceAtLeast(10.0) * 1.15

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spending_trend_chart"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Spending & Income Trends",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Interactive daily activity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CashOutRed))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cash Out", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CashInGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cash In", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Tooltip if selected
            val activePoint = selectedIndex?.let { points.getOrNull(it) }
            if (activePoint != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${activePoint.label}: ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Exp: ${String.format(Locale.US, "%.2f", activePoint.expense)}",
                            fontSize = 12.sp,
                            color = CashOutRed,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = " • ", fontSize = 12.sp)
                        Text(
                            text = "Inc: ${String.format(Locale.US, "%.2f", activePoint.income)}",
                            fontSize = 12.sp,
                            color = CashInGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Chart
            val primaryColor = MaterialTheme.colorScheme.primary
            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val step = if (points.size > 1) width / (points.size - 1) else width
                            val idx = (offset.x / step).toInt().coerceIn(0, points.size - 1)
                            selectedIndex = if (selectedIndex == idx) null else idx
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val bottomPad = 24.dp.toPx()
                val chartHeight = h - bottomPad
                val n = points.size

                // Draw horizontal guide lines
                val numGridLines = 3
                for (i in 0..numGridLines) {
                    val y = chartHeight * (i.toFloat() / numGridLines)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                if (n == 1) {
                    // Single point fallback
                    val x = w / 2f
                    val yExp = chartHeight * (1f - (points[0].expense / ceiling).toFloat())
                    drawCircle(CashOutRed, radius = 6.dp.toPx(), center = Offset(x, yExp))
                    return@Canvas
                }

                val step = w / (n - 1)

                // Expense Path
                val expensePath = Path()
                val expenseFillPath = Path()

                points.forEachIndexed { i, p ->
                    val x = i * step
                    val y = chartHeight * (1f - (p.expense / ceiling).toFloat().coerceIn(0f, 1f))
                    if (i == 0) {
                        expensePath.moveTo(x, y)
                        expenseFillPath.moveTo(x, chartHeight)
                        expenseFillPath.lineTo(x, y)
                    } else {
                        val prevX = (i - 1) * step
                        val prevY = chartHeight * (1f - (points[i - 1].expense / ceiling).toFloat().coerceIn(0f, 1f))
                        val cx1 = prevX + (x - prevX) / 2
                        val cy1 = prevY
                        val cx2 = prevX + (x - prevX) / 2
                        val cy2 = y
                        expensePath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                        expenseFillPath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                    }
                    if (i == n - 1) {
                        expenseFillPath.lineTo(x, chartHeight)
                        expenseFillPath.close()
                    }
                }

                // Draw Gradient Fill under Expense
                drawPath(
                    path = expenseFillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(CashOutRed.copy(alpha = 0.25f), Color.Transparent),
                        startY = 0f,
                        endY = chartHeight
                    )
                )

                // Draw Expense Line
                drawPath(
                    path = expensePath,
                    color = CashOutRed,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Income Path
                val incomePath = Path()
                points.forEachIndexed { i, p ->
                    val x = i * step
                    val y = chartHeight * (1f - (p.income / ceiling).toFloat().coerceIn(0f, 1f))
                    if (i == 0) {
                        incomePath.moveTo(x, y)
                    } else {
                        val prevX = (i - 1) * step
                        val prevY = chartHeight * (1f - (points[i - 1].income / ceiling).toFloat().coerceIn(0f, 1f))
                        val cx1 = prevX + (x - prevX) / 2
                        val cy1 = prevY
                        val cx2 = prevX + (x - prevX) / 2
                        val cy2 = y
                        incomePath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                    }
                }

                // Draw Income Line
                drawPath(
                    path = incomePath,
                    color = CashInGreen,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Points
                points.forEachIndexed { i, p ->
                    val x = i * step
                    val yExp = chartHeight * (1f - (p.expense / ceiling).toFloat().coerceIn(0f, 1f))
                    val isSelected = selectedIndex == i

                    drawCircle(
                        color = Color.White,
                        radius = if (isSelected) 7.dp.toPx() else 4.dp.toPx(),
                        center = Offset(x, yExp)
                    )
                    drawCircle(
                        color = CashOutRed,
                        radius = if (isSelected) 5.dp.toPx() else 2.5.dp.toPx(),
                        center = Offset(x, yExp)
                    )

                    if (p.income > 0) {
                        val yInc = chartHeight * (1f - (p.income / ceiling).toFloat().coerceIn(0f, 1f))
                        drawCircle(
                            color = Color.White,
                            radius = if (isSelected) 6.dp.toPx() else 3.dp.toPx(),
                            center = Offset(x, yInc)
                        )
                        drawCircle(
                            color = CashInGreen,
                            radius = if (isSelected) 4.5.dp.toPx() else 2.dp.toPx(),
                            center = Offset(x, yInc)
                        )
                    }
                }
            }

            // X-Axis Labels row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val stepCount = (points.size / 5).coerceAtLeast(1)
                points.forEachIndexed { index, point ->
                    if (index == 0 || index == points.lastIndex || index % stepCount == 0) {
                        Text(
                            text = point.label,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDonutChart(
    categories: List<CategorySummary>,
    currencySymbol: String = "",
    title: String,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No category data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val total = categories.sumOf { it.amount }
    var selectedCategory by remember { mutableStateOf<CategorySummary?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_donut_chart"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Proportionate distribution by category",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Center Donut Chart with interactive middle readout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(170.dp)
                ) {
                    val strokeWidth = 26.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter, diameter)

                    var startAngle = -90f
                    categories.forEachIndexed { index, cat ->
                        val sweepAngle = (cat.amount / total * 360f).toFloat()
                        val color = CategoryColors[index % CategoryColors.size]
                        val isSelected = selectedCategory?.category == cat.category

                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = if (isSelected) strokeWidth * 1.25f else strokeWidth,
                                cap = StrokeCap.Butt
                            )
                        )
                        startAngle += sweepAngle
                    }
                }

                // Center Text (Dynamic based on selected category or grand total)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (selectedCategory != null) {
                        Text(
                            text = selectedCategory!!.category,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format(Locale.US, "%,.2f", selectedCategory!!.amount),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", selectedCategory!!.percentage)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "TOTAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%,.2f", total),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Legend with Tap to Select
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEachIndexed { index, cat ->
                    val color = CategoryColors[index % CategoryColors.size]
                    val isSelected = selectedCategory?.category == cat.category

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedCategory = if (isSelected) null else cat
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${cat.category} (${String.format(Locale.US, "%.0f", cat.percentage)}%)",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CashInCashOutBarChart(
    points: List<TrendPoint>,
    currencySymbol: String = "",
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val maxAmount = points.maxOfOrNull { max(it.income, it.expense) } ?: 1.0
    val ceiling = (maxAmount * 1.15).coerceAtLeast(10.0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cash_in_cash_out_bar_chart"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cash In vs. Cash Out Comparison",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Side-by-side volume comparison",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val w = size.width
                val h = size.height
                val bottomPad = 22.dp.toPx()
                val chartHeight = h - bottomPad

                // Guide lines
                val numGridLines = 3
                for (i in 0..numGridLines) {
                    val y = chartHeight * (i.toFloat() / numGridLines)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                val n = points.size
                val groupWidth = w / n
                val barWidth = (groupWidth * 0.32f).coerceAtMost(24.dp.toPx())

                points.forEachIndexed { index, point ->
                    val groupCenter = index * groupWidth + (groupWidth / 2)

                    // Income Bar (Left)
                    val incHeight = chartHeight * (point.income / ceiling).toFloat().coerceIn(0f, 1f)
                    val incTop = chartHeight - incHeight
                    val incLeft = groupCenter - barWidth - 2.dp.toPx()

                    drawRoundRect(
                        color = CashInGreen,
                        topLeft = Offset(incLeft, incTop),
                        size = Size(barWidth, incHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Expense Bar (Right)
                    val expHeight = chartHeight * (point.expense / ceiling).toFloat().coerceIn(0f, 1f)
                    val expTop = chartHeight - expHeight
                    val expLeft = groupCenter + 2.dp.toPx()

                    drawRoundRect(
                        color = CashOutRed,
                        topLeft = Offset(expLeft, expTop),
                        size = Size(barWidth, expHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }

            // Labels row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val stepCount = (points.size / 6).coerceAtLeast(1)
                points.forEachIndexed { index, point ->
                    if (index == 0 || index == points.lastIndex || index % stepCount == 0) {
                        Text(
                            text = point.label,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodSummaryCard(
    totalIncome: Double,
    totalExpense: Double,
    netSavings: Double,
    currencySymbol: String = "",
    modifier: Modifier = Modifier
) {
    val savingsRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100).coerceAtLeast(0.0) else 0.0
    val isPositive = netSavings >= 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("period_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "PERIOD SUMMARY",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Cash In",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%,.2f", totalIncome),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = CashInGreen
                    )
                }

                Column {
                    Text(
                        text = "Total Cash Out",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%,.2f", totalExpense),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = CashOutRed
                    )
                }

                Column {
                    Text(
                        text = "Net Savings",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isPositive) String.format(Locale.US, "%,.2f", netSavings)
                        else "-${String.format(Locale.US, "%,.2f", kotlin.math.abs(netSavings))}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) CashInGreen else CashOutRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Savings rate badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isPositive) CashInGreen.copy(alpha = 0.12f) else CashOutRed.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPositive) "Savings Rate: " else "Deficit Rate: ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isPositive) CashInGreen else CashOutRed
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", savingsRate)}% of total cash in",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) CashInGreen else CashOutRed
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryExpensesBreakdownCard(
    categories: List<CategorySummary>,
    totalExpense: Double,
    filterDescription: String,
    currencySymbol: String = "৳",
    onCategoryClick: (String) -> Unit = {},
    precision: Int = 0,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_expenses_breakdown_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = "Cash Out by Categories",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = filterDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Total Cash Out",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "$currencySymbol ${AmountFormatter.format(totalExpense, precision, includeCommas = true)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = CashOutRed,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (categories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No cash out recorded for selected period or book",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Donut Chart
                CategoryDonutChart(
                    categories = categories,
                    title = "Proportions"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SUM OF CASH OUT BY CATEGORY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Itemized Category Expense Sums
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    categories.forEachIndexed { index, item ->
                        val color = CategoryColors[index % CategoryColors.size]
                        val icon = getIconForCategory(item.category)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onCategoryClick(item.category) }
                                .testTag("category_sum_row_${item.category}"),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Category icon badge
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = item.category,
                                            tint = color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Category Name & Entries count
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.category,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${item.count} ${if (item.count == 1) "entry" else "entries"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Sum of expenses in this category
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = AmountFormatter.format(item.amount, precision, includeCommas = true),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", item.percentage)}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = color,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Visual Proportion Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((item.percentage / 100f).coerceIn(0.01f, 1f))
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(color)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
