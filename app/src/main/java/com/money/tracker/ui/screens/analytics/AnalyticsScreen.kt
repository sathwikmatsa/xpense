package com.money.tracker.ui.screens.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val trendState by viewModel.trendState.collectAsState()
    val splitSummaryState by viewModel.splitSummaryState.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    var showPieChart by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analytics",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Spending Overview Widget (combined)
            item {
                SpendingOverviewWidget(
                    uiState = uiState,
                    currencyFormat = currencyFormat,
                    showPieChart = showPieChart,
                    onToggleView = { showPieChart = !showPieChart },
                    onTimeRangeChange = { viewModel.setTimeRange(it) }
                )
            }

            // Spending Trend Widget
            item {
                SpendingTrendWidget(
                    trendState = trendState,
                    currencyFormat = currencyFormat,
                    onTimeRangeChange = { viewModel.setTrendTimeRange(it) },
                    onCategoryToggle = { viewModel.toggleTrendCategory(it) },
                    onClearCategories = { viewModel.clearTrendCategories() }
                )
            }

            // Split Summary Widget (only show if there's any split data)
            if (splitSummaryState.totalPaidForOthers > 0) {
                item {
                    SplitSummaryWidget(
                        state = splitSummaryState,
                        currencyFormat = currencyFormat
                    )
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SpendingOverviewWidget(
    uiState: AnalyticsUiState,
    currencyFormat: NumberFormat,
    showPieChart: Boolean,
    onToggleView: () -> Unit,
    onTimeRangeChange: (AnalyticsTimeRange) -> Unit
) {
    var showTimeRangeDropdown by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header row with title and controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Spending Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${uiState.transactionCount} ${if (uiState.transactionCount == 1) "transaction" else "transactions"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time range dropdown
                    Box {
                        Surface(
                            modifier = Modifier
                                .clickable { showTimeRangeDropdown = true }
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = uiState.selectedTimeRange.label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select time range",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showTimeRangeDropdown,
                            onDismissRequest = { showTimeRangeDropdown = false }
                        ) {
                            AnalyticsTimeRange.entries.forEach { range ->
                                DropdownMenuItem(
                                    text = { Text(range.label) },
                                    onClick = {
                                        onTimeRangeChange(range)
                                        showTimeRangeDropdown = false
                                    },
                                    leadingIcon = if (uiState.selectedTimeRange == range) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    if (uiState.categoryBreakdown.isNotEmpty()) {
                        IconButton(onClick = onToggleView) {
                            Icon(
                                imageVector = if (showPieChart) Icons.Default.BarChart else Icons.Default.PieChart,
                                contentDescription = if (showPieChart) "Show bars" else "Show pie chart"
                            )
                        }
                    }
                }
            }

            // Summary
            Text(
                text = currencyFormat.format(uiState.totalExpense),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Category breakdown
            if (uiState.categoryBreakdown.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No spending data yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (showPieChart) {
                PieChartView(
                    items = uiState.categoryBreakdown,
                    currencyFormat = currencyFormat
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.categoryBreakdown.forEach { item ->
                        CategoryBreakdownCard(
                            item = item,
                            currencyFormat = currencyFormat
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingTrendWidget(
    trendState: TrendUiState,
    currencyFormat: NumberFormat,
    onTimeRangeChange: (TrendTimeRange) -> Unit,
    onCategoryToggle: (Long) -> Unit,
    onClearCategories: () -> Unit
) {
    var showTimeRangeDropdown by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val parentCategories = trendState.categories.filter { it.parentId == null }
    val selectedCategoryNames = parentCategories
        .filter { it.id in trendState.selectedCategories }
        .map { it.name }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spending Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = currencyFormat.format(trendState.totalAmount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Dropdowns row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Time range dropdown
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimeRangeDropdown = true }
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Period",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = trendState.selectedTimeRange.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select period",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showTimeRangeDropdown,
                        onDismissRequest = { showTimeRangeDropdown = false }
                    ) {
                        TrendTimeRange.entries.forEach { range ->
                            DropdownMenuItem(
                                text = { Text(range.label) },
                                onClick = {
                                    onTimeRangeChange(range)
                                    showTimeRangeDropdown = false
                                },
                                leadingIcon = if (trendState.selectedTimeRange == range) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // Category dropdown
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategoryDialog = true }
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Categories",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = when {
                                        selectedCategoryNames.isEmpty() -> "All"
                                        selectedCategoryNames.size == 1 -> selectedCategoryNames.first()
                                        else -> "${selectedCategoryNames.size} selected"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select categories",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bar chart
            if (trendState.dailySpending.isNotEmpty() && trendState.maxAmount > 0) {
                BarChart(
                    data = trendState.dailySpending,
                    maxAmount = trendState.maxAmount,
                    currencyFormat = currencyFormat
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No spending data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Category selection dialog
    if (showCategoryDialog) {
        CategorySelectionDialog(
            categories = parentCategories,
            selectedCategories = trendState.selectedCategories,
            onCategoryToggle = onCategoryToggle,
            onClearAll = onClearCategories,
            onDismiss = { showCategoryDialog = false }
        )
    }
}

@Composable
private fun CategorySelectionDialog(
    categories: List<com.money.tracker.data.entity.Category>,
    selectedCategories: Set<Long>,
    onCategoryToggle: (Long) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (selectedCategories.isNotEmpty()) {
                        Text(
                            text = "Clear all",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onClearAll() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = category.id in selectedCategories
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategoryToggle(category.id) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = category.emoji)
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BarChart(
    data: List<DailySpending>,
    maxAmount: Double,
    @Suppress("UNUSED_PARAMETER") currencyFormat: NumberFormat
) {
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    // Format amount for Y-axis (compact format)
    fun formatCompact(amount: Double): String {
        return when {
            amount >= 100000 -> "${(amount / 100000).toInt()}L"
            amount >= 1000 -> "${(amount / 1000).toInt()}K"
            else -> amount.toInt().toString()
        }
    }

    // Calculate bar width based on data size
    val barWidthDp = when {
        data.size <= 4 -> 32.dp
        data.size <= 7 -> 24.dp
        data.size <= 12 -> 16.dp
        else -> 8.dp
    }

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Y-axis labels
        Column(
            modifier = Modifier
                .width(36.dp)
                .height(120.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatCompact(maxAmount),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = formatCompact(maxAmount / 2),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "0",
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Chart area
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = barWidthDp.toPx()
                    val slotWidth = size.width / data.size
                    val maxBarHeight = size.height

                    // Draw grid lines
                    listOf(0f, 0.5f, 1f).forEach { fraction ->
                        val y = size.height * (1 - fraction)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw bars - centered on each slot
                    data.forEachIndexed { index, spending ->
                        val barHeight = if (maxAmount > 0) {
                            (spending.amount / maxAmount * maxBarHeight).toFloat()
                        } else 0f

                        // Center of slot
                        val slotCenter = slotWidth * index + slotWidth / 2
                        val x = slotCenter - barWidth / 2
                        val y = size.height - barHeight

                        if (barHeight > 0) {
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }
            }

            // X-axis labels - centered under each bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                data.forEach { spending ->
                    Text(
                        text = spending.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    item: CategoryBreakdownItem,
    currencyFormat: NumberFormat
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (item.children.isNotEmpty()) {
                        Modifier.clickable { expanded = !expanded }
                    } else Modifier
                )
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = item.emoji, style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (item.children.isNotEmpty()) {
                        Text(
                            text = "${item.children.size} subcategories",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormat.format(item.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${String.format("%.1f", item.percentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.children.isNotEmpty()) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LinearProgressIndicator(
            progress = { item.percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )

        // Child categories
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 8.dp, start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item.children.forEach { child ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = child.emoji, style = MaterialTheme.typography.labelSmall)
                                }
                                Text(
                                    text = child.categoryName,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = currencyFormat.format(child.amount),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "${String.format("%.1f", child.percentage)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        LinearProgressIndicator(
                            progress = { (child.amount / item.amount).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

// Pie chart color palette
private val pieChartColors = listOf(
    Color(0xFF6750A4),
    Color(0xFF625B71),
    Color(0xFF7D5260),
    Color(0xFF4285F4),
    Color(0xFF34A853),
    Color(0xFFFBBC04),
    Color(0xFFEA4335),
    Color(0xFF9AA0A6),
    Color(0xFF137333),
    Color(0xFFC5221F),
)

@Composable
private fun PieChartView(
    items: List<CategoryBreakdownItem>,
    currencyFormat: NumberFormat
) {
    var selectedParent by remember { mutableStateOf<CategoryBreakdownItem?>(null) }

    val displayItems = if (selectedParent != null && selectedParent!!.children.isNotEmpty()) {
        selectedParent!!.children.map { child ->
            child.copy(
                percentage = ((child.amount / selectedParent!!.amount) * 100).toFloat()
            )
        }
    } else {
        items
    }

    Column {
        // Back button when viewing subcategories
        if (selectedParent != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedParent = null }
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandLess,
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${selectedParent!!.emoji} ${selectedParent!!.categoryName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = currencyFormat.format(selectedParent!!.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Pie Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(160.dp)
            ) {
                val strokeWidth = 32.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                var startAngle = -90f

                displayItems.forEachIndexed { index, item ->
                    val sweepAngle = (item.percentage / 100f) * 360f
                    val color = pieChartColors[index % pieChartColors.size]

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth)
                    )

                    startAngle += sweepAngle
                }
            }
        }

        // Legend
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            displayItems.forEachIndexed { index, item ->
                val originalItem = if (selectedParent == null) {
                    items.find { it.categoryId == item.categoryId }
                } else null
                val hasChildren = originalItem?.children?.isNotEmpty() == true

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (hasChildren) {
                                Modifier.clickable { selectedParent = originalItem }
                            } else Modifier
                        )
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(pieChartColors[index % pieChartColors.size])
                        )
                        Text(
                            text = item.emoji,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = item.categoryName,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (hasChildren) {
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = "Has subcategories",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currencyFormat.format(item.amount),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${String.format("%.1f", item.percentage)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitSummaryWidget(
    state: SplitSummaryState,
    currencyFormat: NumberFormat
) {
    var showPieChart by remember { mutableStateOf(false) }
    val barColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Paid for Others",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Last 6 months",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currencyFormat.format(state.totalPaidForOthers),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (state.categoryBreakdown.isNotEmpty()) {
                        IconButton(onClick = { showPieChart = !showPieChart }) {
                            Icon(
                                imageVector = if (showPieChart) Icons.Default.BarChart else Icons.Default.PieChart,
                                contentDescription = if (showPieChart) "Show trend" else "Show categories"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (showPieChart && state.categoryBreakdown.isNotEmpty()) {
                // Pie chart view
                SplitCategoryPieChart(
                    items = state.categoryBreakdown,
                    currencyFormat = currencyFormat
                )
            } else if (state.monthlyData.isNotEmpty() && state.maxAmount > 0) {
                // Bar chart view
                // Format amount for Y-axis (compact format)
                fun formatCompact(amount: Double): String {
                    return when {
                        amount >= 100000 -> "${(amount / 100000).toInt()}L"
                        amount >= 1000 -> "${(amount / 1000).toInt()}K"
                        else -> amount.toInt().toString()
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Y-axis labels
                    Column(
                        modifier = Modifier
                            .width(36.dp)
                            .height(100.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatCompact(state.maxAmount),
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = formatCompact(state.maxAmount / 2),
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Chart area
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barWidth = 24.dp.toPx()
                                val slotWidth = size.width / state.monthlyData.size
                                val maxBarHeight = size.height

                                // Draw grid lines
                                listOf(0f, 0.5f, 1f).forEach { fraction ->
                                    val y = size.height * (1 - fraction)
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                // Draw bars
                                state.monthlyData.forEachIndexed { index, item ->
                                    val barHeight = if (state.maxAmount > 0) {
                                        (item.paidForOthers / state.maxAmount * maxBarHeight).toFloat()
                                    } else 0f

                                    val slotCenter = slotWidth * index + slotWidth / 2
                                    val x = slotCenter - barWidth / 2
                                    val y = size.height - barHeight

                                    if (barHeight > 0) {
                                        drawRoundRect(
                                            color = barColor,
                                            topLeft = Offset(x, y),
                                            size = Size(barWidth, barHeight),
                                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }

                        // X-axis labels
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            state.monthlyData.forEach { item ->
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = labelColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No split data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SplitCategoryPieChart(
    items: List<SplitCategoryItem>,
    currencyFormat: NumberFormat
) {
    Column {
        // Pie Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.8f)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(120.dp)
            ) {
                val strokeWidth = 28.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                var startAngle = -90f

                items.forEachIndexed { index, item ->
                    val sweepAngle = (item.percentage / 100f) * 360f
                    val color = pieChartColors[index % pieChartColors.size]

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth)
                    )

                    startAngle += sweepAngle
                }
            }
        }

        // Legend
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(pieChartColors[index % pieChartColors.size])
                        )
                        Text(
                            text = item.emoji,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = item.categoryName,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currencyFormat.format(item.amount),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${String.format("%.1f", item.percentage)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
