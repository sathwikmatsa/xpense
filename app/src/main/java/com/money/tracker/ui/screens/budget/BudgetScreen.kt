package com.money.tracker.ui.screens.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.money.tracker.data.entity.Category
import com.money.tracker.data.entity.Tag
import com.money.tracker.ui.theme.ExpenseRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val editableBudget by viewModel.editableBudget.collectAsState()
    val editablePreallocations by viewModel.editablePreallocations.collectAsState()
    val editableCategoryBudgets by viewModel.editableCategoryBudgets.collectAsState()
    val editableTagBudgets by viewModel.editableTagBudgets.collectAsState()

    var initialized by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var showPreallocationPicker by remember { mutableStateOf(false) }
    var showCategoryBudgetPicker by remember { mutableStateOf(false) }
    var showTagBudgetPicker by remember { mutableStateOf(false) }

    // Initialize editable state when data loads
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && !initialized) {
            viewModel.initializeEditableState(uiState)

            // Copy from previous month if empty
            scope.launch {
                if (editablePreallocations.isEmpty()) {
                    val previous = viewModel.getPreviousMonthPreallocations()
                    previous.forEach { viewModel.addPreallocation(it.categoryId) }
                    previous.forEach { viewModel.updatePreallocation(it.categoryId, it.amount) }
                }
                if (editableCategoryBudgets.isEmpty()) {
                    val previous = viewModel.getPreviousMonthCategoryBudgets()
                    previous.forEach { viewModel.addCategoryBudget(it.categoryId) }
                    previous.forEach { viewModel.updateCategoryBudget(it.categoryId, it.amount) }
                }
                if (editableTagBudgets.isEmpty()) {
                    val previous = viewModel.getPreviousMonthTagBudgets()
                    previous.forEach { viewModel.addTagBudget(it.tagId) }
                    previous.forEach { viewModel.updateTagBudget(it.tagId, it.amount) }
                }
            }
            initialized = true
        }
    }

    val totalPreallocated = editablePreallocations.sumOf { it.amount }
    val totalTagBudgets = editableTagBudgets.sumOf { it.amount }
    val totalAllocated = totalPreallocated + totalTagBudgets
    val discretionaryBudget = (editableBudget ?: 0.0) - totalAllocated
    val isOverAllocated = discretionaryBudget < 0

    // Available items for pickers
    val preallocatedCategoryIds = editablePreallocations.map { it.categoryId }.toSet()
    val availableCategoriesForPreallocation = uiState.categories.values.filter { it.id !in preallocatedCategoryIds }
    val availableCategoriesForBudget = uiState.categories.values.filter { cat ->
        cat.id !in preallocatedCategoryIds && editableCategoryBudgets.none { it.categoryId == cat.id }
    }
    val availableTagsForBudget = uiState.tags.values.filter { tag ->
        editableTagBudgets.none { it.tagId == tag.id }
    }

    // Get current month name
    val monthName = remember {
        val cal = Calendar.getInstance()
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget - $monthName") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveBudget()
                            onNavigateBack()
                        },
                        enabled = !isOverAllocated
                    ) {
                        Text("Save")
                    }
                }
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
            // Monthly Budget Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "Monthly Budget")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = editableBudget?.toLong()?.toString() ?: "",
                            onValueChange = { newValue ->
                                val amount = newValue.filter { it.isDigit() }.toDoubleOrNull()
                                viewModel.setBudgetAmount(amount)
                            },
                            label = { Text("Total Budget") },
                            prefix = { Text("₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (totalAllocated > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Allocated:",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "₹${totalAllocated.toLong()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Discretionary:",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "₹${discretionaryBudget.toLong()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOverAllocated) ExpenseRed else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            if (isOverAllocated) {
                                Text(
                                    text = "Total allocations exceed budget",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                }
            }

            // Pre-allocations Section
            item {
                SectionHeader(
                    title = "Pre-allocations",
                    subtitle = "₹${totalPreallocated.toLong()}",
                    onAdd = if (availableCategoriesForPreallocation.isNotEmpty()) {
                        { showPreallocationPicker = true }
                    } else null
                )
            }

            if (editablePreallocations.isEmpty()) {
                item {
                    EmptyStateCard(text = "No pre-allocations set")
                }
            } else {
                items(editablePreallocations) { preallocation ->
                    val category = uiState.categories[preallocation.categoryId]
                    if (category != null) {
                        BudgetItemCard(
                            emoji = category.emoji,
                            name = category.name,
                            amount = preallocation.amount,
                            onAmountChange = { viewModel.updatePreallocation(preallocation.categoryId, it) },
                            onRemove = { viewModel.removePreallocation(preallocation.categoryId) }
                        )
                    }
                }
            }

            // Category Budgets Section
            item {
                SectionHeader(
                    title = "Category Budgets",
                    subtitle = "Track spending limits",
                    onAdd = if (availableCategoriesForBudget.isNotEmpty()) {
                        { showCategoryBudgetPicker = true }
                    } else null
                )
            }

            if (editableCategoryBudgets.isEmpty()) {
                item {
                    EmptyStateCard(text = "No category budgets set")
                }
            } else {
                items(editableCategoryBudgets) { categoryBudget ->
                    val category = uiState.categories[categoryBudget.categoryId]
                    if (category != null) {
                        BudgetItemCard(
                            emoji = category.emoji,
                            name = category.name,
                            amount = categoryBudget.amount,
                            onAmountChange = { viewModel.updateCategoryBudget(categoryBudget.categoryId, it) },
                            onRemove = { viewModel.removeCategoryBudget(categoryBudget.categoryId) }
                        )
                    }
                }
            }

            // Event Budgets Section (Tag-based budgets)
            item {
                SectionHeader(
                    title = "Event Budgets",
                    subtitle = "₹${totalTagBudgets.toLong()}",
                    onAdd = if (availableTagsForBudget.isNotEmpty()) {
                        { showTagBudgetPicker = true }
                    } else null
                )
            }

            if (editableTagBudgets.isEmpty()) {
                item {
                    EmptyStateCard(text = "No event budgets set")
                }
            } else {
                items(editableTagBudgets) { tagBudget ->
                    val tag = uiState.tags[tagBudget.tagId]
                    if (tag != null) {
                        TagBudgetItemCard(
                            emoji = tag.emoji,
                            name = tag.name,
                            color = tag.color,
                            amount = tagBudget.amount,
                            onAmountChange = { viewModel.updateTagBudget(tagBudget.tagId, it) },
                            onRemove = { viewModel.removeTagBudget(tagBudget.tagId) }
                        )
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Preallocation Picker Dialog
    if (showPreallocationPicker) {
        CategoryPickerDialog(
            title = "Add Pre-allocation",
            categories = availableCategoriesForPreallocation.toList(),
            onSelect = { category ->
                viewModel.addPreallocation(category.id)
                showPreallocationPicker = false
            },
            onDismiss = { showPreallocationPicker = false }
        )
    }

    // Category Budget Picker Dialog
    if (showCategoryBudgetPicker) {
        CategoryPickerDialog(
            title = "Add Category Budget",
            categories = availableCategoriesForBudget.toList(),
            onSelect = { category ->
                viewModel.addCategoryBudget(category.id)
                showCategoryBudgetPicker = false
            },
            onDismiss = { showCategoryBudgetPicker = false }
        )
    }

    // Event Budget Picker Dialog
    if (showTagBudgetPicker) {
        TagPickerDialog(
            title = "Add Event Budget",
            tags = availableTagsForBudget.toList(),
            onSelect = { tag ->
                viewModel.addTagBudget(tag.id)
                showTagBudgetPicker = false
            },
            onDismiss = { showTagBudgetPicker = false }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    onAdd: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (onAdd != null) {
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    }
}

@Composable
private fun EmptyStateCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BudgetItemCard(
    emoji: String,
    name: String,
    amount: Double,
    onAmountChange: (Double) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = if (amount > 0) amount.toLong().toString() else "",
                onValueChange = { newValue ->
                    val newAmount = newValue.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                    onAmountChange(newAmount)
                },
                prefix = { Text("₹", style = MaterialTheme.typography.bodySmall) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(100.dp)
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = ExpenseRed)
            }
        }
    }
}

@Composable
private fun TagBudgetItemCard(
    emoji: String,
    name: String,
    color: Long,
    amount: Double,
    onAmountChange: (Double) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(color).copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(color))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = if (amount > 0) amount.toLong().toString() else "",
                onValueChange = { newValue ->
                    val newAmount = newValue.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                    onAmountChange(newAmount)
                },
                prefix = { Text("₹", style = MaterialTheme.typography.bodySmall) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(100.dp)
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = ExpenseRed)
            }
        }
    }
}

@Composable
private fun CategoryPickerDialog(
    title: String,
    categories: List<Category>,
    onSelect: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(category) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category.emoji, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(category.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TagPickerDialog(
    title: String,
    tags: List<Tag>,
    onSelect: (Tag) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tags) { tag ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(tag) },
                        colors = CardDefaults.cardColors(containerColor = Color(tag.color).copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(tag.color))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tag.emoji, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(tag.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
