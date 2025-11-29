package com.money.tracker.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.money.tracker.data.entity.Transaction
import com.money.tracker.data.entity.TransactionType
import com.money.tracker.ui.components.TransactionCard
import com.money.tracker.ui.theme.ExpenseRed
import com.money.tracker.ui.theme.IncomeGreen
import com.money.tracker.ui.theme.WarningAmber
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onTransactionClick: (Long) -> Unit,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val currentMonth = monthFormat.format(Calendar.getInstance().time)

    var showBudgetDialog by remember { mutableStateOf(false) }

    // Calculate days in month and days remaining
    val calendar = Calendar.getInstance()
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val daysRemaining = daysInMonth - currentDay + 1
    val daysPassed = currentDay

    if (showBudgetDialog) {
        BudgetDialog(
            currentBudget = uiState.budget,
            onDismiss = { showBudgetDialog = false },
            onSave = { amount ->
                viewModel.setBudget(amount)
                showBudgetDialog = false
            },
            onClear = {
                viewModel.clearBudget()
                showBudgetDialog = false
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Xpense",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Budget Card
                item {
                    BudgetCard(
                        budget = uiState.budget,
                        totalExpense = uiState.totalExpense,
                        totalIncome = uiState.totalIncome,
                        showIncome = uiState.showIncome,
                        currentMonth = currentMonth,
                        daysRemaining = daysRemaining,
                        daysPassed = daysPassed,
                        daysInMonth = daysInMonth,
                        currencyFormat = currencyFormat,
                        onToggleVisibility = { viewModel.toggleIncomeVisibility() },
                        onEditBudget = { showBudgetDialog = true }
                    )
                }

                // Budget Insights (only when budget is set)
                if (uiState.budget != null) {
                    item {
                        BudgetInsights(
                            budget = uiState.budget!!,
                            totalExpense = uiState.totalExpense,
                            daysRemaining = daysRemaining,
                            daysPassed = daysPassed,
                            daysInMonth = daysInMonth,
                            currencyFormat = currencyFormat
                        )
                    }
                }

                // Pending Transactions Section
                if (uiState.pendingTransactions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pending Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(uiState.pendingTransactions) { transaction ->
                        PendingTransactionCard(
                            transaction = transaction,
                            currencyFormat = currencyFormat,
                            onDismiss = { viewModel.dismissPendingTransaction(transaction.id) },
                            onEdit = { onTransactionClick(transaction.id) }
                        )
                    }
                }

                // Recent Transactions Header
                item {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Transactions or Empty State
                val displayTransactions = if (uiState.showIncome) {
                    uiState.transactions
                } else {
                    uiState.transactions.filter { it.type == TransactionType.EXPENSE }
                }

                if (displayTransactions.isEmpty()) {
                    item {
                        EmptyState()
                    }
                } else {
                    items(displayTransactions.take(10)) { transaction ->
                        val category = uiState.categories[transaction.categoryId]
                        if (category != null) {
                            TransactionCard(
                                transaction = transaction,
                                category = category,
                                onClick = { onTransactionClick(transaction.id) }
                            )
                        }
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun BudgetCard(
    budget: Double?,
    totalExpense: Double,
    totalIncome: Double,
    showIncome: Boolean,
    currentMonth: String,
    daysRemaining: Int,
    daysPassed: Int,
    daysInMonth: Int,
    currencyFormat: NumberFormat,
    onToggleVisibility: () -> Unit,
    onEditBudget: () -> Unit
) {
    val remaining = if (budget != null) budget - totalExpense else null
    val progress = if (budget != null && budget > 0) (totalExpense / budget).toFloat().coerceIn(0f, 1f) else 0f
    val isOverBudget = remaining != null && remaining < 0

    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "progress"
    )

    // Determine progress bar color based on spending
    val progressColor = when {
        isOverBudget -> ExpenseRed
        progress > 0.85f -> WarningAmber
        else -> IncomeGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = currentMonth,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = if (budget != null) {
                                if (isOverBudget) "Over budget by" else "Remaining budget"
                            } else {
                                "Spent this month"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                    Row {
                        IconButton(onClick = onToggleVisibility) {
                            Icon(
                                imageVector = if (showIncome) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showIncome) "Hide Details" else "Show Details",
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(onClick = onEditBudget) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Budget",
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main amount
                Text(
                    text = if (budget != null) {
                        currencyFormat.format(kotlin.math.abs(remaining ?: 0.0))
                    } else {
                        currencyFormat.format(totalExpense)
                    },
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                // Budget progress bar
                if (budget != null) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Custom progress bar with rounded ends
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(progressColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${currencyFormat.format(totalExpense)} spent",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "of ${currencyFormat.format(budget)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                }

                // Income/Expense breakdown
                AnimatedVisibility(
                    visible = showIncome,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Income
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(IncomeGreen)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currencyFormat.format(totalIncome),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "Income",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                )
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(48.dp)
                                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                            )

                            // Expenses
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ExpenseRed)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currencyFormat.format(totalExpense),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "Expenses",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                )
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(48.dp)
                                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                            )

                            // Net
                            val net = totalIncome - totalExpense
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (net >= 0) IncomeGreen else ExpenseRed)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${if (net >= 0) "+" else ""}${currencyFormat.format(net)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "Net",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetInsights(
    budget: Double,
    totalExpense: Double,
    daysRemaining: Int,
    daysPassed: Int,
    daysInMonth: Int,
    currencyFormat: NumberFormat
) {
    val remaining = budget - totalExpense
    val dailyAllowance = if (daysRemaining > 0 && remaining > 0) remaining / daysRemaining else 0.0

    // Spending pace: compare actual spending to expected spending
    val expectedSpending = budget * (daysPassed.toFloat() / daysInMonth)
    val spendingPace = if (expectedSpending > 0) ((totalExpense / expectedSpending) * 100).roundToInt() else 0
    val paceStatus = when {
        spendingPace <= 90 -> "Under" to IncomeGreen
        spendingPace <= 110 -> "On track" to WarningAmber
        else -> "Over" to ExpenseRed
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Daily Allowance
        InsightCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Wallet,
            title = "Daily Budget",
            value = currencyFormat.format(dailyAllowance),
            subtitle = "$daysRemaining days left",
            accentColor = if (remaining > 0) IncomeGreen else ExpenseRed
        )

        // Spending Pace
        InsightCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Speed,
            title = "Spending Pace",
            value = "${paceStatus.first}",
            subtitle = "$spendingPace% of expected",
            accentColor = paceStatus.second
        )
    }
}

@Composable
private fun InsightCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun BudgetDialog(
    currentBudget: Double?,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onClear: () -> Unit
) {
    var budgetText by remember { mutableStateOf(currentBudget?.toLong()?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Monthly Budget") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Set a budget to track your spending and get insights",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it.filter { c -> c.isDigit() } },
                    label = { Text("Budget Amount") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    budgetText.toDoubleOrNull()?.let { onSave(it) }
                },
                enabled = budgetText.isNotBlank() && budgetText.toDoubleOrNull() != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (currentBudget != null) {
                    TextButton(onClick = onClear) {
                        Text("Clear", color = ExpenseRed)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No transactions yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tap + to add your first transaction",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PendingTransactionCard(
    transaction: Transaction,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val amountColor = if (isExpense) ExpenseRed else IncomeGreen
    val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        onClick = onEdit
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchant ?: transaction.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${transaction.source.name} • ${dateFormat.format(transaction.date)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${if (isExpense) "-" else "+"}${currencyFormat.format(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.error)
                }
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add", color = IncomeGreen)
                }
            }
        }
    }
}
