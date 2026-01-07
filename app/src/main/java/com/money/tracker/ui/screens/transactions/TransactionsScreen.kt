package com.money.tracker.ui.screens.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.money.tracker.data.entity.Category
import com.money.tracker.data.entity.Tag
import com.money.tracker.data.entity.TransactionType
import com.money.tracker.ui.components.TransactionCard
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TransactionTypeFilter {
    ALL, INCOME, EXPENSE
}

enum class SplitFilter(val label: String) {
    ALL("All"),
    SPLIT_ONLY("Split"),
    NON_SPLIT("Non-Split")
}

enum class TimeRangeFilter(val label: String, val days: Int?) {
    YEAR("Last Year", 365),
    TODAY("Today", 0),
    WEEK("Last 7 Days", 7),
    MONTH("Last 30 Days", 30),
    THREE_MONTHS("Last 3 Months", 90),
    CUSTOM("Custom", null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onTransactionClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var typeFilter by remember { mutableStateOf(TransactionTypeFilter.ALL) }
    var timeRangeFilter by remember { mutableStateOf(TimeRangeFilter.YEAR) }
    var splitFilter by remember { mutableStateOf(SplitFilter.ALL) }
    var selectedCategories by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedTags by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    val sheetState = rememberModalBottomSheetState()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // Calculate time range
    val (startTime, endTime) = remember(timeRangeFilter, customStartDate, customEndDate) {
        when (timeRangeFilter) {
            TimeRangeFilter.CUSTOM -> {
                val start = customStartDate?.let {
                    Calendar.getInstance().apply {
                        timeInMillis = it
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                } ?: 0L
                val end = customEndDate?.let {
                    Calendar.getInstance().apply {
                        timeInMillis = it
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                } ?: Long.MAX_VALUE
                Pair(start, end)
            }
            else -> {
                val days = timeRangeFilter.days!!
                val start = Calendar.getInstance().apply {
                    if (days == 0) {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    } else {
                        add(Calendar.DAY_OF_YEAR, -days)
                    }
                }.timeInMillis
                Pair(start, Long.MAX_VALUE)
            }
        }
    }

    // Build expanded category set (include children of selected parents)
    val expandedCategories = remember(selectedCategories, uiState.categories) {
        if (selectedCategories.isEmpty()) {
            emptySet()
        } else {
            val expanded = selectedCategories.toMutableSet()
            // For each selected category, add its children
            selectedCategories.forEach { selectedId ->
                uiState.categories.values
                    .filter { it.parentId == selectedId }
                    .forEach { child -> expanded.add(child.id) }
            }
            expanded
        }
    }

    // Apply all filters
    val filteredTransactions = uiState.transactions.filter { txn ->
        val matchesType = when (typeFilter) {
            TransactionTypeFilter.ALL -> true
            TransactionTypeFilter.INCOME -> txn.type == TransactionType.INCOME
            TransactionTypeFilter.EXPENSE -> txn.type == TransactionType.EXPENSE
        }
        val matchesTime = txn.date >= startTime && txn.date <= endTime
        val matchesCategory = expandedCategories.isEmpty() || txn.categoryId in expandedCategories
        // Check if any of the transaction's tags match the selected tags
        val txnTagIds = uiState.transactionTagIds[txn.id] ?: emptyList()
        val matchesTag = selectedTags.isEmpty() || txnTagIds.any { it in selectedTags }
        val matchesSplit = when (splitFilter) {
            SplitFilter.ALL -> true
            SplitFilter.SPLIT_ONLY -> txn.isSplit
            SplitFilter.NON_SPLIT -> !txn.isSplit
        }

        matchesType && matchesTime && matchesCategory && matchesTag && matchesSplit
    }

    // Group transactions by date
    val groupedTransactions = filteredTransactions.groupBy { transaction ->
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.date))
    }

    // Count active filters (YEAR is default, so don't count it)
    val activeFilterCount = listOfNotNull(
        if (typeFilter != TransactionTypeFilter.ALL) typeFilter else null,
        if (timeRangeFilter != TimeRangeFilter.YEAR) timeRangeFilter else null,
        if (selectedCategories.isNotEmpty()) "categories" else null,
        if (selectedTags.isNotEmpty()) "tags" else null,
        if (splitFilter != SplitFilter.ALL) splitFilter else null
    ).size

    // Custom date range label for display
    val customDateLabel = if (timeRangeFilter == TimeRangeFilter.CUSTOM) {
        val start = customStartDate?.let { dateFormat.format(Date(it)) } ?: "Start"
        val end = customEndDate?.let { dateFormat.format(Date(it)) } ?: "End"
        "$start - $end"
    } else null

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            FilterBottomSheet(
                categories = uiState.categories.values.toList(),
                tags = uiState.tags.values.toList(),
                typeFilter = typeFilter,
                onTypeFilterChange = { typeFilter = it },
                timeRangeFilter = timeRangeFilter,
                onTimeRangeChange = { timeRangeFilter = it },
                customStartDate = customStartDate,
                customEndDate = customEndDate,
                onCustomStartDateChange = { customStartDate = it },
                onCustomEndDateChange = { customEndDate = it },
                selectedCategories = selectedCategories,
                onCategoryToggle = { categoryId ->
                    selectedCategories = if (categoryId in selectedCategories) {
                        selectedCategories - categoryId
                    } else {
                        selectedCategories + categoryId
                    }
                },
                selectedTags = selectedTags,
                onTagToggle = { tagId ->
                    selectedTags = if (tagId in selectedTags) {
                        selectedTags - tagId
                    } else {
                        selectedTags + tagId
                    }
                },
                splitFilter = splitFilter,
                onSplitFilterChange = { splitFilter = it },
                onClearFilters = {
                    typeFilter = TransactionTypeFilter.ALL
                    timeRangeFilter = TimeRangeFilter.YEAR
                    selectedCategories = emptySet()
                    selectedTags = emptySet()
                    splitFilter = SplitFilter.ALL
                    customStartDate = null
                    customEndDate = null
                }
            )
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "History",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filters"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Active filters display with summary
            if (activeFilterCount > 0) {
                val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                val filteredTotal = filteredTransactions.sumOf { txn ->
                    if (txn.type == TransactionType.EXPENSE) -txn.amount else txn.amount
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredTransactions.size} ${if (filteredTransactions.size == 1) "transaction" else "transactions"}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currencyFormat.format(kotlin.math.abs(filteredTotal)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (filteredTotal >= 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                        )
                    }

                    LazyRow(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (typeFilter != TransactionTypeFilter.ALL) {
                            item {
                                AssistChip(
                                    onClick = { typeFilter = TransactionTypeFilter.ALL },
                                    label = { Text(typeFilter.name) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.padding(start = 4.dp))
                                    }
                                )
                            }
                        }
                        if (timeRangeFilter != TimeRangeFilter.YEAR) {
                            item {
                                AssistChip(
                                    onClick = {
                                        timeRangeFilter = TimeRangeFilter.YEAR
                                        customStartDate = null
                                        customEndDate = null
                                    },
                                    label = { Text(customDateLabel ?: timeRangeFilter.label) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.padding(start = 4.dp))
                                    }
                                )
                            }
                        }
                        if (selectedCategories.isNotEmpty()) {
                            item {
                                AssistChip(
                                    onClick = { selectedCategories = emptySet() },
                                    label = { Text("${selectedCategories.size} ${if (selectedCategories.size == 1) "category" else "categories"}") },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.padding(start = 4.dp))
                                    }
                                )
                            }
                        }
                        if (selectedTags.isNotEmpty()) {
                            item {
                                AssistChip(
                                    onClick = { selectedTags = emptySet() },
                                    label = { Text("${selectedTags.size} ${if (selectedTags.size == 1) "tag" else "tags"}") },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.padding(start = 4.dp))
                                    }
                                )
                            }
                        }
                        if (splitFilter != SplitFilter.ALL) {
                            item {
                                AssistChip(
                                    onClick = { splitFilter = SplitFilter.ALL },
                                    label = { Text(splitFilter.label) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.padding(start = 4.dp))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.transactions.isEmpty()) {
                            "No transactions yet"
                        } else {
                            "No transactions match filters"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedTransactions.forEach { (date, transactions) ->
                        item {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(transactions) { transaction ->
                            val category = uiState.categories[transaction.categoryId]
                            // Get all tags from junction table
                            val tagIds = uiState.transactionTagIds[transaction.id] ?: emptyList()
                            val tags = tagIds.mapNotNull { uiState.tags[it] }
                            if (category != null) {
                                TransactionCard(
                                    transaction = transaction,
                                    category = category,
                                    tags = tags,
                                    onClick = { onTransactionClick(transaction.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    categories: List<Category>,
    tags: List<Tag>,
    typeFilter: TransactionTypeFilter,
    onTypeFilterChange: (TransactionTypeFilter) -> Unit,
    timeRangeFilter: TimeRangeFilter,
    onTimeRangeChange: (TimeRangeFilter) -> Unit,
    customStartDate: Long?,
    customEndDate: Long?,
    onCustomStartDateChange: (Long?) -> Unit,
    onCustomEndDateChange: (Long?) -> Unit,
    selectedCategories: Set<Long>,
    onCategoryToggle: (Long) -> Unit,
    selectedTags: Set<Long>,
    onTagToggle: (Long) -> Unit,
    splitFilter: SplitFilter,
    onSplitFilterChange: (SplitFilter) -> Unit,
    onClearFilters: () -> Unit
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = customStartDate ?: System.currentTimeMillis(),
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Can't select future dates or dates after end date
                    val notFuture = utcTimeMillis <= today
                    val beforeEnd = customEndDate?.let { utcTimeMillis <= it } ?: true
                    return notFuture && beforeEnd
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onCustomStartDateChange(it) }
                    showStartDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = customEndDate ?: System.currentTimeMillis(),
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Can't select future dates or dates before start date
                    val notFuture = utcTimeMillis <= today
                    val afterStart = customStartDate?.let { utcTimeMillis >= it } ?: true
                    return notFuture && afterStart
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onCustomEndDateChange(it) }
                    showEndDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Clear all",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onClearFilters() }
            )
        }

        // Transaction Type
        Text(
            text = "Transaction Type",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransactionTypeFilter.entries.forEach { filter ->
                FilterChip(
                    selected = typeFilter == filter,
                    onClick = { onTypeFilterChange(filter) },
                    label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        // Split Filter
        Text(
            text = "Split Status",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SplitFilter.entries.forEach { filter ->
                FilterChip(
                    selected = splitFilter == filter,
                    onClick = { onSplitFilterChange(filter) },
                    label = { Text(filter.label) }
                )
            }
        }

        // Time Range
        Text(
            text = "Time Range",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeRangeFilter.entries.forEach { filter ->
                FilterChip(
                    selected = timeRangeFilter == filter,
                    onClick = { onTimeRangeChange(filter) },
                    label = { Text(filter.label) }
                )
            }
        }

        // Custom Date Range (shown when CUSTOM is selected)
        if (timeRangeFilter == TimeRangeFilter.CUSTOM) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(customStartDate?.let { dateFormat.format(Date(it)) } ?: "Start Date")
                }
                OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(customEndDate?.let { dateFormat.format(Date(it)) } ?: "End Date")
                }
            }
        }

        // Categories
        Text(
            text = "Categories",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = category.id in selectedCategories,
                    onClick = { onCategoryToggle(category.id) },
                    label = { Text(category.name) }
                )
            }
        }

        // Tags (Events)
        if (tags.isNotEmpty()) {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tags) { tag ->
                    FilterChip(
                        selected = tag.id in selectedTags,
                        onClick = { onTagToggle(tag.id) },
                        label = { Text("${tag.emoji} ${tag.name}") }
                    )
                }
            }
        }

        // Bottom padding for gesture navigation
        Box(modifier = Modifier.padding(bottom = 32.dp))
    }
}
