package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyOption
import com.example.ui.theme.FinanceExpense
import com.example.ui.theme.FinanceGreenPrimary
import com.example.ui.theme.FinanceIncome
import com.example.ui.theme.getCategoryColor
import com.example.ui.viewmodel.CategorySpendItem
import com.example.ui.viewmodel.MonthlyTrendItem
import kotlin.math.max

@Composable
fun IncomeExpenseBarChart(
    items: List<MonthlyTrendItem>,
    currency: CurrencyOption,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty() || items.all { it.income == 0.0 && it.expense == 0.0 }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No transaction data for this period",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        return
    }

    val maxVal = remember(items) {
        val highest = items.maxOfOrNull { max(it.income, it.expense) } ?: 1.0
        if (highest <= 0.0) 100.0 else highest * 1.15
    }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(items) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(700))
    }

    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val textStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(FinanceIncome, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Income", style = textStyle, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(FinanceExpense, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Expense", style = textStyle, fontWeight = FontWeight.Medium)
        }

        // Canvas Bars
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height - 24.dp.toPx() // leave room for labels
                val slotCount = items.size
                val slotWidth = canvasWidth / slotCount
                val barWidth = (slotWidth * 0.30f).coerceAtMost(22.dp.toPx())
                val barGap = 4.dp.toPx()

                // Baseline
                drawLine(
                    color = gridColor,
                    start = Offset(0f, canvasHeight),
                    end = Offset(canvasWidth, canvasHeight),
                    strokeWidth = 1.dp.toPx()
                )

                // Mid grid line
                drawLine(
                    color = gridColor,
                    start = Offset(0f, canvasHeight / 2),
                    end = Offset(canvasWidth, canvasHeight / 2),
                    strokeWidth = 1.dp.toPx()
                )

                items.forEachIndexed { index, item ->
                    val centerX = (index * slotWidth) + (slotWidth / 2)

                    // Income bar
                    val incomeHeight = ((item.income / maxVal) * canvasHeight * animationProgress.value)
                        .toFloat().coerceAtLeast(0f)
                    val incomeLeft = centerX - barWidth - (barGap / 2)
                    val incomeTop = canvasHeight - incomeHeight

                    if (incomeHeight > 0) {
                        drawRoundRect(
                            color = FinanceIncome,
                            topLeft = Offset(incomeLeft, incomeTop),
                            size = Size(barWidth, incomeHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    // Expense bar
                    val expenseHeight = ((item.expense / maxVal) * canvasHeight * animationProgress.value)
                        .toFloat().coerceAtLeast(0f)
                    val expenseLeft = centerX + (barGap / 2)
                    val expenseTop = canvasHeight - expenseHeight

                    if (expenseHeight > 0) {
                        drawRoundRect(
                            color = FinanceExpense,
                            topLeft = Offset(expenseLeft, expenseTop),
                            size = Size(barWidth, expenseHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }

            // Month Labels underneath
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(20.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                items.forEach { item ->
                    Text(
                        text = item.monthLabel,
                        style = textStyle,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDonutChart(
    items: List<CategorySpendItem>,
    currency: CurrencyOption,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No expenses recorded this month",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        return
    }

    val totalSpent = remember(items) { items.sumOf { it.amount } }
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(items) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(800))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Donut Chart Box
        Box(
            modifier = Modifier
                .size(170.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 24.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)
                var startAngle = -90f

                // Background ring
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.15f),
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth)
                )

                // Segments
                items.forEach { item ->
                    val sweepAngle = (item.percentage * 360f * animProgress.value).coerceAtLeast(0f)
                    val catColor = getCategoryColor(item.category)

                    if (sweepAngle > 0.5f) {
                        drawArc(
                            color = catColor,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    startAngle += sweepAngle
                }
            }

            // Center text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = FinanceFormatters.formatAmount(totalSpent, currency, showDecimals = false),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Legend (FlowRow)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.take(6).forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(getCategoryColor(item.category), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${item.category} (${(item.percentage * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun SavingsTrendChart(
    items: List<MonthlyTrendItem>,
    currency: CurrencyOption,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty() || items.all { it.savings == 0.0 }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No savings data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        return
    }

    val maxSavings = remember(items) {
        val highest = items.maxOfOrNull { it.savings } ?: 1.0
        if (highest <= 0.0) 100.0 else highest * 1.2
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(items) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(700))
    }

    val primaryColor = FinanceGreenPrimary
    val textStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height - 20.dp.toPx()
                val stepX = canvasWidth / (items.size - 1).coerceAtLeast(1)

                val points = items.mapIndexed { index, item ->
                    val x = index * stepX
                    val y = canvasHeight - ((item.savings / maxSavings) * canvasHeight * animProgress.value).toFloat()
                    Offset(x, y)
                }

                // Fill gradient under curve
                if (points.isNotEmpty()) {
                    val fillPath = Path().apply {
                        moveTo(points.first().x, canvasHeight)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, canvasHeight)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.35f),
                                primaryColor.copy(alpha = 0.02f)
                            ),
                            startY = 0f,
                            endY = canvasHeight
                        )
                    )

                    // Stroke line
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }

                    drawPath(
                        path = linePath,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw dots on points
                    points.forEach { pt ->
                        drawCircle(
                            color = primaryColor,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }

            // Month Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                items.forEach { item ->
                    Text(text = item.monthLabel, style = textStyle)
                }
            }
        }
    }
}
