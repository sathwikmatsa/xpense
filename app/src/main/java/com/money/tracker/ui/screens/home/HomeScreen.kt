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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.money.tracker.data.entity.BudgetPreallocation
import com.money.tracker.data.entity.Category
import com.money.tracker.data.entity.CategoryBudget
import com.money.tracker.data.entity.Transaction
import com.money.tracker.data.entity.TransactionType
import com.money.tracker.data.entity.UpiReminder
import com.money.tracker.ui.components.TransactionCard
import com.money.tracker.ui.screens.settings.getSavedSharingApp
import com.money.tracker.ui.theme.ExpenseRed
import com.money.tracker.ui.theme.GradientEnd
import com.money.tracker.ui.theme.GradientStart
import com.money.tracker.ui.theme.IncomeGreen
import com.money.tracker.ui.theme.WarningAmber
import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.money.tracker.service.UpiMonitorService
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
    onSettingsClick: () -> Unit,
    onBudgetClick: () -> Unit,
    onOpenUpiApp: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val currentMonth = monthFormat.format(Calendar.getInstance().time)

    // Calculate days in month and days remaining
    val calendar = Calendar.getInstance()
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val daysRemaining = daysInMonth - currentDay + 1
    val daysPassed = currentDay

    // Count of pending items
    val pendingCount = uiState.pendingTransactions.size + uiState.upiReminders.size + uiState.unsyncedSplitTransactions.size

    // Check permission states for settings badge
    val context = LocalContext.current
    val hasSmsPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    }
    val isUpiMonitorEnabled = remember {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == context.packageName &&
            it.resolveInfo.serviceInfo.name == UpiMonitorService::class.java.name
        }
    }
    val isNotificationListenerEnabled = remember {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
    val disabledPermissionsCount = listOf(!hasSmsPermission, !isUpiMonitorEnabled, !isNotificationListenerEnabled).count { it }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Xpense",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (pendingCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(ExpenseRed, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (pendingCount > 9) "9+" else pendingCount.toString(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(26.dp)
                            )
                            if (disabledPermissionsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(14.dp)
                                        .background(ExpenseRed, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = disabledPermissionsCount.toString(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 9.sp
                                    )
                                }
                            }
                        }
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
                        preallocatedBudget = uiState.preallocatedBudget,
                        tagBudgetTotal = uiState.tagBudgetTotal,
                        discretionaryExpense = uiState.discretionaryExpense,
                        preallocatedExpense = uiState.preallocatedExpense,
                        tagExpense = uiState.tagExpense,
                        showIncome = uiState.showIncome,
                        currentMonth = currentMonth,
                        daysRemaining = daysRemaining,
                        daysPassed = daysPassed,
                        daysInMonth = daysInMonth,
                        currencyFormat = currencyFormat,
                        onToggleVisibility = { viewModel.toggleIncomeVisibility() },
                        onEditBudget = onBudgetClick
                    )
                }

                // Budget Insights (only when budget is set)
                if (uiState.budget != null) {
                    item {
                        // Use discretionary values when allocations exist (preallocations + tag budgets)
                        val totalAllocated = uiState.preallocatedBudget + uiState.tagBudgetTotal
                        val hasAllocated = totalAllocated > 0
                        val trackingBudget = if (hasAllocated) uiState.budget!! - totalAllocated else uiState.budget!!
                        val trackingExpense = if (hasAllocated) uiState.discretionaryExpense - uiState.tagExpense else uiState.totalExpense

                        BudgetInsights(
                            budget = trackingBudget,
                            totalExpense = trackingExpense,
                            paidForOthers = uiState.paidForOthers,
                            daysRemaining = daysRemaining,
                            daysPassed = daysPassed,
                            daysInMonth = daysInMonth,
                            currencyFormat = currencyFormat,
                            isDiscretionary = hasAllocated
                        )
                    }
                }

                // Paid for Others card (when no budget but has split transactions)
                if (uiState.budget == null && uiState.paidForOthers > 0) {
                    item {
                        PaidForOthersCard(
                            amount = uiState.paidForOthers,
                            currencyFormat = currencyFormat
                        )
                    }
                }

                // UPI Reminders Section
                if (uiState.upiReminders.isNotEmpty()) {
                    item {
                        Text(
                            text = "UPI Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(uiState.upiReminders) { reminder ->
                        UpiReminderCard(
                            reminder = reminder,
                            onDismiss = { viewModel.dismissUpiReminder(reminder.id) },
                            onOpenApp = { onOpenUpiApp(reminder.packageName) }
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

                // Pending Split Updates Section
                if (uiState.unsyncedSplitTransactions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pending Splits",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(uiState.unsyncedSplitTransactions) { transaction ->
                        val category = uiState.categories[transaction.categoryId]
                        UnsyncedSplitCard(
                            transaction = transaction,
                            categoryName = category?.name,
                            currencyFormat = currencyFormat,
                            onMarkSynced = { viewModel.markSplitSynced(transaction.id) },
                            onEdit = { onTransactionClick(transaction.id) }
                        )
                    }
                }

                // Event Budget Cards (Tag Budgets) - shown above category budgets
                if (uiState.tagBudgetsWithSpending.isNotEmpty()) {
                    item {
                        Text(
                            text = "Event Budgets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(uiState.tagBudgetsWithSpending) { item ->
                        TagBudgetCard(
                            tagName = item.tag.name,
                            emoji = item.tag.emoji,
                            color = item.tag.color,
                            budget = item.tagBudget.amount,
                            spent = item.spent,
                            currencyFormat = currencyFormat
                        )
                    }
                }

                // Category Budget Cards
                if (uiState.categoryBudgetsWithSpending.isNotEmpty()) {
                    item {
                        Text(
                            text = "Category Budgets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(uiState.categoryBudgetsWithSpending) { item ->
                        CategoryBudgetCard(
                            categoryName = item.category.name,
                            emoji = item.category.emoji,
                            budget = item.categoryBudget.amount,
                            spent = item.spent,
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
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun BudgetCard(
    budget: Double?,
    totalExpense: Double,
    totalIncome: Double,
    preallocatedBudget: Double,
    tagBudgetTotal: Double,
    discretionaryExpense: Double,
    preallocatedExpense: Double,
    tagExpense: Double,
    showIncome: Boolean,
    currentMonth: String,
    daysRemaining: Int,
    daysPassed: Int,
    daysInMonth: Int,
    currencyFormat: NumberFormat,
    onToggleVisibility: () -> Unit,
    onEditBudget: () -> Unit
) {
    // Calculate discretionary budget (total budget - preallocated - tag budgets)
    val totalAllocated = preallocatedBudget + tagBudgetTotal
    val hasAllocated = totalAllocated > 0
    val discretionaryBudget = if (budget != null && hasAllocated) budget - totalAllocated else budget

    // Use discretionary values for budget tracking when allocations exist
    // Discretionary expense excludes both preallocated category expenses and tag budget expenses
    val actualDiscretionaryExpense = discretionaryExpense - tagExpense
    val trackingExpense = if (hasAllocated) actualDiscretionaryExpense else totalExpense
    val trackingBudget = discretionaryBudget
    val allocatedExpense = preallocatedExpense + tagExpense

    val remaining = if (trackingBudget != null) trackingBudget - trackingExpense else null
    val progress = if (trackingBudget != null && trackingBudget > 0) (trackingExpense / trackingBudget).toFloat().coerceIn(0f, 1f) else 0f
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
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            GradientStart,
                            GradientEnd
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
                            color = Color.White
                        )
                        Text(
                            text = if (budget != null) {
                                if (isOverBudget) "Over budget by" else if (hasAllocated) "Discretionary remaining" else "Remaining budget"
                            } else {
                                "Spent this month"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                    Row {
                        IconButton(onClick = onToggleVisibility) {
                            Icon(
                                imageVector = if (showIncome) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showIncome) "Hide Details" else "Show Details",
                                tint = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        IconButton(onClick = onEditBudget) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Budget",
                                tint = Color.White.copy(alpha = 0.85f)
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
                    color = Color.White
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
                            .background(Color.White.copy(alpha = 0.2f))
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
                            text = if (hasAllocated) "${currencyFormat.format(trackingExpense)} discretionary" else "${currencyFormat.format(totalExpense)} spent",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Text(
                            text = if (hasAllocated) "of ${currencyFormat.format(trackingBudget)}" else "of ${currencyFormat.format(budget)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    // Show total spent including allocated when there are allocations
                    if (hasAllocated) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Total spent: ${currencyFormat.format(totalExpense)} (incl. ${currencyFormat.format(allocatedExpense)} allocated)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
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
                                    color = Color.White
                                )
                                Text(
                                    text = "Income",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(48.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
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
                                    color = Color.White
                                )
                                Text(
                                    text = "Expenses",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(48.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
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
                                    color = Color.White
                                )
                                Text(
                                    text = "Net",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
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
    paidForOthers: Double,
    daysRemaining: Int,
    daysPassed: Int,
    daysInMonth: Int,
    currencyFormat: NumberFormat,
    isDiscretionary: Boolean = false
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

    val budgetLabel = if (isDiscretionary) "Daily Discretionary" else "Daily Budget"

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Daily Allowance
            InsightCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Wallet,
                title = budgetLabel,
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

        // Paid for Others (only show if > 0)
        if (paidForOthers > 0) {
            PaidForOthersCard(
                amount = paidForOthers,
                currencyFormat = currencyFormat
            )
        }
    }
}

@Composable
private fun PaidForOthersCard(
    amount: Double,
    currencyFormat: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Paid for Others",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currencyFormat.format(amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                text = "This month",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryBudgetCard(
    categoryName: String,
    emoji: String,
    budget: Double,
    spent: Double,
    currencyFormat: NumberFormat
) {
    val remaining = budget - spent
    val progress = if (budget > 0) (spent / budget).coerceIn(0.0, 1.0).toFloat() else 0f
    val isOverBudget = spent > budget

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverBudget)
                ExpenseRed.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(emoji, fontSize = 20.sp)
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = if (isOverBudget) "Over by ${currencyFormat.format(-remaining)}" else "${currencyFormat.format(remaining)} left",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverBudget) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isOverBudget) ExpenseRed else if (progress > 0.8f) WarningAmber else IncomeGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: ${currencyFormat.format(spent)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Budget: ${currencyFormat.format(budget)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TagBudgetCard(
    tagName: String,
    emoji: String,
    color: Long,
    budget: Double,
    spent: Double,
    currencyFormat: NumberFormat
) {
    val remaining = budget - spent
    val progress = if (budget > 0) (spent / budget).coerceIn(0.0, 1.0).toFloat() else 0f
    val isOverBudget = spent > budget
    val tagColor = Color(color)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverBudget)
                ExpenseRed.copy(alpha = 0.1f)
            else
                tagColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(tagColor)
                    )
                    Text(emoji, fontSize = 20.sp)
                    Text(
                        text = tagName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = if (isOverBudget) "Over by ${currencyFormat.format(-remaining)}" else "${currencyFormat.format(remaining)} left",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverBudget) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isOverBudget) ExpenseRed else if (progress > 0.8f) WarningAmber else tagColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: ${currencyFormat.format(spent)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Budget: ${currencyFormat.format(budget)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
private fun UpiReminderCard(
    reminder: UpiReminder,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit
) {
    val timeDisplay = getTimeDisplay(reminder.timestamp)
    val context = LocalContext.current

    val appIcon = remember(reminder.packageName) {
        try {
            context.packageManager.getApplicationIcon(reminder.packageName)
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        ),
        onClick = onOpenApp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon.toBitmap(width = 96, height = 96).asImageBitmap(),
                        contentDescription = reminder.appName,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = reminder.appName.first().toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${reminder.appName} payment?",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = timeDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Dismiss")
                }
                Button(
                    onClick = onOpenApp,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Open ↗")
                }
            }
        }
    }
}

private fun getTimeDisplay(timestamp: Long): String {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val time = timeFormat.format(timestamp)

    val calNow = Calendar.getInstance()
    val calTimestamp = Calendar.getInstance().apply { timeInMillis = timestamp }

    val daysDiff = ((calNow.timeInMillis - calTimestamp.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
    val isSameDay = calNow.get(Calendar.YEAR) == calTimestamp.get(Calendar.YEAR) &&
            calNow.get(Calendar.DAY_OF_YEAR) == calTimestamp.get(Calendar.DAY_OF_YEAR)

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(Calendar.YEAR) == calTimestamp.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == calTimestamp.get(Calendar.DAY_OF_YEAR)

    return when {
        isSameDay -> "Today, $time"
        isYesterday -> "Yesterday, $time"
        else -> "$daysDiff days ago, $time"
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

@Composable
private fun UnsyncedSplitCard(
    transaction: Transaction,
    categoryName: String?,
    currencyFormat: NumberFormat,
    onMarkSynced: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    val othersShare = transaction.totalAmount - transaction.amount

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
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
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${categoryName ?: "Uncategorized"} • ${dateFormat.format(transaction.date)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormat.format(transaction.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                    Text(
                        text = "Others: ${currencyFormat.format(othersShare)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onMarkSynced,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Mark Synced")
                }
                Button(
                    onClick = {
                        // Copy amount to clipboard
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Split Amount", transaction.totalAmount.toLong().toString())
                        clipboard.setPrimaryClip(clip)
                        // Open sharing app
                        val savedApp = getSavedSharingApp(context)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            setPackage(savedApp.packageName)
                            putExtra(Intent.EXTRA_TEXT, "${transaction.description} - ${currencyFormat.format(transaction.totalAmount)}")
                        }
                        try {
                            context.startActivity(shareIntent)
                        } catch (e: Exception) {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(savedApp.packageName)
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                            }
                        }
                        // Don't mark as synced - user needs to explicitly click "Mark Synced"
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Split")
                }
            }
        }
    }
}
