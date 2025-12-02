package com.money.tracker.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.money.tracker.data.repository.BudgetPreallocationRepository
import com.money.tracker.data.repository.BudgetRepository
import com.money.tracker.data.repository.CategoryRepository
import com.money.tracker.data.repository.SharingAppRepository
import com.money.tracker.data.repository.TransactionRepository
import com.money.tracker.data.repository.UpiReminderRepository
import com.money.tracker.ui.screens.analytics.AnalyticsScreen
import com.money.tracker.ui.screens.analytics.AnalyticsViewModel
import com.money.tracker.ui.screens.home.HomeScreen
import com.money.tracker.ui.screens.home.HomeViewModel
import com.money.tracker.ui.screens.transactions.AddTransactionScreen
import com.money.tracker.ui.screens.transactions.AddTransactionViewModel
import com.money.tracker.ui.screens.transactions.TransactionsScreen
import com.money.tracker.ui.screens.transactions.TransactionsViewModel
import com.money.tracker.ui.screens.transactions.EditTransactionScreen
import com.money.tracker.ui.screens.transactions.EditTransactionViewModel
import com.money.tracker.ui.screens.settings.SettingsScreen
import com.money.tracker.ui.screens.settings.CategoriesScreen
import com.money.tracker.ui.screens.settings.CategoriesViewModel
import com.money.tracker.data.entity.TransactionSource

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Transactions : Screen("transactions")
    data object Analytics : Screen("analytics")
    data object AddTransaction : Screen("add_transaction")
    data object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }
    data object Settings : Screen("settings")
    data object Categories : Screen("categories")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Transactions, "History", Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List),
    BottomNavItem(Screen.Analytics, "Analytics", Icons.Filled.Analytics, Icons.Outlined.Analytics)
)

@Composable
fun MoneyTrackerNavGraph(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    budgetRepository: BudgetRepository,
    upiReminderRepository: UpiReminderRepository,
    sharingAppRepository: SharingAppRepository,
    budgetPreallocationRepository: BudgetPreallocationRepository,
    openAddTransaction: Boolean = false,
    upiReminderId: Long = -1L,
    upiPackageName: String = "",
    onOpenUpiApp: (String) -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Track the UPI reminder if we came from notification
    var currentUpiReminderId by remember { mutableStateOf(-1L) }
    var currentUpiPackageName by remember { mutableStateOf("") }

    // Navigate to add transaction if requested via intent
    androidx.compose.runtime.LaunchedEffect(openAddTransaction, upiReminderId) {
        if (openAddTransaction) {
            currentUpiReminderId = upiReminderId
            currentUpiPackageName = upiPackageName
            navController.navigate(Screen.AddTransaction.route)
        }
    }

    val showBottomBar = currentDestination?.route in listOf(
        Screen.Home.route,
        Screen.Transactions.route,
        Screen.Analytics.route
    )

    val isHomeScreen = currentDestination?.route == Screen.Home.route

    Column(modifier = Modifier.fillMaxSize()) {
        // Content area
        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route
            ) {
            composable(Screen.Home.route) {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(transactionRepository, categoryRepository, budgetRepository, upiReminderRepository, budgetPreallocationRepository)
                )
                HomeScreen(
                    viewModel = viewModel,
                    onTransactionClick = { id ->
                        navController.navigate(Screen.EditTransaction.createRoute(id))
                    },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onOpenUpiApp = onOpenUpiApp
                )
            }

            composable(Screen.Transactions.route) {
                val viewModel: TransactionsViewModel = viewModel(
                    factory = TransactionsViewModel.Factory(transactionRepository, categoryRepository)
                )
                TransactionsScreen(
                    viewModel = viewModel,
                    onTransactionClick = { id ->
                        navController.navigate(Screen.EditTransaction.createRoute(id))
                    }
                )
            }

            composable(Screen.Analytics.route) {
                val viewModel: AnalyticsViewModel = viewModel(
                    factory = AnalyticsViewModel.Factory(transactionRepository, categoryRepository)
                )
                AnalyticsScreen(viewModel = viewModel)
            }

            composable(Screen.AddTransaction.route) {
                val application = LocalContext.current.applicationContext as Application
                val viewModel: AddTransactionViewModel = viewModel(
                    factory = AddTransactionViewModel.Factory(application, transactionRepository, categoryRepository, sharingAppRepository, upiReminderRepository)
                )
                // Map UPI package name to TransactionSource
                val initialSource = when (currentUpiPackageName) {
                    "com.google.android.apps.nbu.paisa.user" -> TransactionSource.GOOGLE_PAY
                    "com.phonepe.app" -> TransactionSource.PHONEPE
                    "com.dreamplug.androidapp" -> TransactionSource.CRED
                    "money.jupiter" -> TransactionSource.JUPITER
                    else -> TransactionSource.UPI
                }
                AddTransactionScreen(
                    viewModel = viewModel,
                    initialSource = initialSource,
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = {
                        if (currentUpiReminderId > 0) {
                            viewModel.deleteUpiReminder(currentUpiReminderId)
                            currentUpiReminderId = -1L
                            currentUpiPackageName = ""
                        }
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.EditTransaction.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
                val application = LocalContext.current.applicationContext as Application
                val viewModel: EditTransactionViewModel = viewModel(
                    factory = EditTransactionViewModel.Factory(application, transactionId, transactionRepository, categoryRepository)
                )
                EditTransactionScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onCategoriesClick = { navController.navigate(Screen.Categories.route) }
                )
            }

            composable(Screen.Categories.route) {
                val viewModel: CategoriesViewModel = viewModel(
                    factory = CategoriesViewModel.Factory(categoryRepository, transactionRepository)
                )
                CategoriesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        }

        // Bottom bar
        if (showBottomBar) {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                // Add Transaction Button - only on Home screen
                if (isHomeScreen) {
                    Button(
                        onClick = { navController.navigate(Screen.AddTransaction.route) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Transaction")
                    }
                }

                // Navigation Bar
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    }
}
