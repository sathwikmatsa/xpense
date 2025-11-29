package com.money.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.money.tracker.data.entity.Category

@Composable
fun CategoryPickerDialog(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    onCreateCategory: (name: String, emoji: String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filteredCategories = remember(searchQuery, categories) {
        if (searchQuery.isBlank()) {
            categories
        } else {
            categories.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    if (showCreateDialog) {
        CreateCategoryDialog(
            onConfirm = { name, emoji ->
                onCreateCategory(name, emoji)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search categories...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { })
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredCategories) { category ->
                        CategoryItem(
                            category = category,
                            isSelected = category.id == selectedCategory?.id,
                            onClick = {
                                onCategorySelected(category)
                                onDismiss()
                            }
                        )
                    }

                    if (filteredCategories.isEmpty() && searchQuery.isNotBlank()) {
                        item {
                            Text(
                                text = "No categories found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreateDialog = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Create new category",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.emoji,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CreateCategoryDialog(
    onConfirm: (name: String, emoji: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("") }

    val commonEmojis = listOf(
        "🏷", "💼", "🎁", "✈", "🏥", "🎮", "📱", "💻",
        "🏋", "☕", "🍕", "🎵", "📦", "🔧", "💡", "🎨"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), emoji.ifBlank { "•" }) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
