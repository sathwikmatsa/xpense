package com.money.tracker.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.money.tracker.data.entity.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onNavigateBack: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }

    // Group categories by parent
    val parentCategories = categories.filter { it.parentId == null }
    val childrenByParent = categories.filter { it.parentId != null }.groupBy { it.parentId }

    // Handle delete state dialogs
    when (val state = deleteState) {
        is DeleteState.NeedsReassignment -> {
            ReassignmentDialog(
                category = state.category,
                transactionCount = state.transactionCount,
                availableCategories = categories.filter { it.id != state.category.id },
                onReassign = { targetCategory ->
                    viewModel.deleteWithReassignment(state.category, targetCategory)
                },
                onDismiss = { viewModel.cancelDelete() }
            )
        }
        DeleteState.Idle -> { }
    }

    if (showEditDialog) {
        CategoryEditDialog(
            category = editingCategory,
            parentCategories = viewModel.getParentCategories(editingCategory?.id),
            allCategories = categories,
            onSave = { name, emoji, parentId ->
                if (editingCategory != null) {
                    viewModel.updateCategory(editingCategory!!, name, emoji, parentId)
                } else {
                    viewModel.createCategory(name, emoji, parentId)
                }
                showEditDialog = false
                editingCategory = null
            },
            onDismiss = {
                showEditDialog = false
                editingCategory = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCategory = null
                    showEditDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            parentCategories.forEach { parent ->
                item(key = "parent_${parent.id}") {
                    CategoryItem(
                        category = parent,
                        isChild = false,
                        onEdit = {
                            editingCategory = parent
                            showEditDialog = true
                        },
                        onDelete = { viewModel.checkAndDeleteCategory(parent) }
                    )
                }

                val children = childrenByParent[parent.id] ?: emptyList()
                items(children, key = { "child_${it.id}" }) { child ->
                    CategoryItem(
                        category = child,
                        isChild = true,
                        onEdit = {
                            editingCategory = child
                            showEditDialog = true
                        },
                        onDelete = { viewModel.checkAndDeleteCategory(child) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    isChild: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isChild) 24.dp else 0.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.emoji,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (category.isDefault) {
                        Text(
                            text = "Default",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditDialog(
    category: Category?,
    parentCategories: List<Category>,
    allCategories: List<Category>,
    onSave: (name: String, emoji: String, parentId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(category) { mutableStateOf(category?.name ?: "") }
    var emoji by remember(category) { mutableStateOf(category?.emoji ?: "") }
    var selectedParentId by remember(category) { mutableStateOf(category?.parentId) }
    var parentExpanded by remember { mutableStateOf(false) }

    val selectedParent = parentCategories.find { it.id == selectedParentId }

    val commonEmojis = listOf(
        "🏷", "💼", "🎁", "✈", "🏥", "🎮", "📱", "💻",
        "🏋", "☕", "🍕", "🎵", "📦", "🔧", "💡", "🎨"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "New Category" else "Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = emoji,
                    onValueChange = { if (it.length <= 2) emoji = it },
                    label = { Text("Emoji") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Type or select below") }
                )

                Text(
                    text = "Quick select:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    commonEmojis.take(8).forEach { e ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (emoji == e) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = e, fontSize = 16.sp)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    commonEmojis.drop(8).forEach { e ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (emoji == e) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = e, fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Parent category selector
                ExposedDropdownMenuBox(
                    expanded = parentExpanded,
                    onExpandedChange = { parentExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedParent?.let { "${it.emoji} ${it.name}" } ?: "None (Top level)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Parent Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = parentExpanded,
                        onDismissRequest = { parentExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Top level)") },
                            onClick = {
                                selectedParentId = null
                                parentExpanded = false
                            }
                        )
                        parentCategories.forEach { parent ->
                            DropdownMenuItem(
                                text = { Text("${parent.emoji} ${parent.name}") },
                                onClick = {
                                    selectedParentId = parent.id
                                    parentExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), emoji.ifBlank { "•" }, selectedParentId) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReassignmentDialog(
    category: Category,
    transactionCount: Int,
    availableCategories: List<Category>,
    onReassign: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    // Default to parent category if exists, otherwise "Other"
    val defaultCategory = category.parentId?.let { parentId ->
        availableCategories.find { it.id == parentId }
    } ?: availableCategories.find { it.name == "Other" }

    if (selectedCategory == null && defaultCategory != null) {
        selectedCategory = defaultCategory
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reassign Transactions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "\"${category.name}\" has $transactionCount transaction${if (transactionCount > 1) "s" else ""}. " +
                            "Please choose a category to reassign them to before deleting."
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.let { "${it.emoji} ${it.name}" } ?: "Select category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reassign to") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.emoji} ${cat.name}") },
                                onClick = {
                                    selectedCategory = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedCategory?.let { onReassign(it) } },
                enabled = selectedCategory != null
            ) {
                Text("Delete & Reassign", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
