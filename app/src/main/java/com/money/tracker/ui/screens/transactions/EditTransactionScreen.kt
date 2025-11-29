package com.money.tracker.ui.screens.transactions

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.money.tracker.data.entity.Category
import com.money.tracker.data.entity.TransactionSource
import com.money.tracker.data.entity.TransactionType
import com.money.tracker.ui.components.CategoryPickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    viewModel: EditTransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedSource by remember { mutableStateOf(TransactionSource.UPI) }
    var selectedDateTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var initialized by remember { mutableStateOf(false) }

    var showCategoryPicker by remember { mutableStateOf(false) }
    var sourceExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    // Initialize fields when transaction loads
    LaunchedEffect(uiState.transaction, categories) {
        if (!initialized && uiState.transaction != null) {
            val txn = uiState.transaction!!
            amount = txn.amount.toString()
            description = txn.description
            selectedType = txn.type
            selectedSource = txn.source
            selectedDateTime = txn.date
            selectedCategory = txn.categoryId?.let { catId ->
                categories.find { it.id == catId }
            }
            initialized = true
        }
    }

    // Navigate back on save or delete
    LaunchedEffect(uiState.isSaved, uiState.isDeleted) {
        if (uiState.isSaved || uiState.isDeleted) {
            onNavigateBack()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCategoryPicker) {
        CategoryPickerDialog(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            onCreateCategory = { name, emoji ->
                viewModel.createCategory(name, emoji)
            },
            onDismiss = { showCategoryPicker = false }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateTime,
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate ->
                        val calendar = Calendar.getInstance()
                        val timeCalendar = Calendar.getInstance().apply { timeInMillis = selectedDateTime }
                        calendar.timeInMillis = selectedDate
                        calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                        calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                        selectedDateTime = calendar.timeInMillis
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDateTime }
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE)
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                TimePicker(state = timePickerState)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Cancel")
                    }
                    TextButton(onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateTime }
                        cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        cal.set(Calendar.MINUTE, timePickerState.minute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val now = System.currentTimeMillis()
                        if (cal.timeInMillis > now) {
                            selectedDateTime = now
                            scope.launch {
                                snackbarHostState.showSnackbar("Cannot select future time. Set to current time.")
                            }
                        } else {
                            selectedDateTime = cal.timeInMillis
                        }
                        showTimePicker = false
                    }) {
                        Text("OK")
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Transaction", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    when {
                        amountValue == null -> {
                            scope.launch { snackbarHostState.showSnackbar("Please enter a valid amount") }
                        }
                        description.isBlank() -> {
                            scope.launch { snackbarHostState.showSnackbar("Please enter a description") }
                        }
                        selectedCategory == null -> {
                            scope.launch { snackbarHostState.showSnackbar("Please select a category") }
                        }
                        else -> {
                            viewModel.updateTransaction(
                                amount = amountValue,
                                description = description,
                                type = selectedType,
                                categoryId = selectedCategory!!.id,
                                source = selectedSource,
                                date = selectedDateTime
                            )
                        }
                    }
                }
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.transaction == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Transaction not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Transaction Type
                Text(
                    text = "Type",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = { selectedType = TransactionType.EXPENSE },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = { selectedType = TransactionType.INCOME },
                        label = { Text("Income") }
                    )
                }

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Date and Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = dateFormat.format(Date(selectedDateTime)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date") },
                            trailingIcon = {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Select date")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = timeFormat.format(Date(selectedDateTime)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Time") },
                            trailingIcon = {
                                Icon(Icons.Default.Schedule, contentDescription = "Select time")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showTimePicker = true }
                        )
                    }
                }

                // Category Picker
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategoryPicker = true }
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    if (selectedCategory != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedCategory!!.emoji,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = selectedCategory!!.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        Text(
                            text = "Tap to select category",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Source Dropdown
                ExposedDropdownMenuBox(
                    expanded = sourceExpanded,
                    onExpandedChange = { sourceExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSource.name.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Source") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = sourceExpanded,
                        onDismissRequest = { sourceExpanded = false }
                    ) {
                        TransactionSource.entries.forEach { source ->
                            DropdownMenuItem(
                                text = { Text(source.name.replace("_", " ")) },
                                onClick = {
                                    selectedSource = source
                                    sourceExpanded = false
                                }
                            )
                        }
                    }
                }

                // Show original message if from SMS
                uiState.transaction?.rawMessage?.let { raw ->
                    Text(
                        text = "Original Message",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = raw,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
