package com.money.tracker.ui.screens.settings

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.money.tracker.util.DatabaseBackupManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Upload
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.money.tracker.ui.theme.ExpenseRed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.money.tracker.service.UpiMonitorService

private const val PREFS_NAME = "money_tracker_prefs"
private const val KEY_SHARING_APP_PACKAGE = "sharing_app_package"
private const val KEY_SHARING_APP_NAME = "sharing_app_name"
private const val DEFAULT_SHARING_APP_PACKAGE = "com.Splitwise.SplitwiseMobile"
private const val DEFAULT_SHARING_APP_NAME = "Splitwise"

data class AppInfo(
    val name: String,
    val packageName: String
)

// Curated list of useful sharing apps for split expenses
private val RECOMMENDED_APPS = listOf(
    AppInfo("Splitwise", "com.Splitwise.SplitwiseMobile"),
    AppInfo("PhonePe", "com.phonepe.app"),
    AppInfo("Google Pay", "com.google.android.apps.nbu.paisa.user"),
    AppInfo("Paytm", "net.one97.paytm"),
    AppInfo("WhatsApp", "com.whatsapp"),
    AppInfo("Telegram", "org.telegram.messenger"),
    AppInfo("Google Keep", "com.google.android.keep"),
    AppInfo("Notes", "com.samsung.android.app.notes"),
    AppInfo("OneNote", "com.microsoft.office.onenote"),
)

fun getSharingApps(context: Context): List<AppInfo> {
    // Return only recommended apps that are installed
    return RECOMMENDED_APPS.filter { app ->
        try {
            context.packageManager.getPackageInfo(app.packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}

fun getSavedSharingApp(context: Context): AppInfo {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return AppInfo(
        name = prefs.getString(KEY_SHARING_APP_NAME, DEFAULT_SHARING_APP_NAME) ?: DEFAULT_SHARING_APP_NAME,
        packageName = prefs.getString(KEY_SHARING_APP_PACKAGE, DEFAULT_SHARING_APP_PACKAGE) ?: DEFAULT_SHARING_APP_PACKAGE
    )
}

fun saveSharingApp(context: Context, app: AppInfo) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
        .putString(KEY_SHARING_APP_PACKAGE, app.packageName)
        .putString(KEY_SHARING_APP_NAME, app.name)
        .apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onCategoriesClick: () -> Unit,
    onTagsClick: () -> Unit
) {
    val context = LocalContext.current

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECEIVE_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasSmsPermission = permissions[Manifest.permission.RECEIVE_SMS] == true
        // Request notification permission after SMS permission is granted
        if (hasSmsPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Check if accessibility service is enabled
    fun isAccessibilityServiceEnabled(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == context.packageName &&
            it.resolveInfo.serviceInfo.name == UpiMonitorService::class.java.name
        }
    }

    var isUpiMonitorEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled()) }

    // Check if notification listener is enabled
    fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledListeners.contains(context.packageName)
    }

    var isNotificationListenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled()) }

    // Sharing app preference
    var selectedSharingApp by remember { mutableStateOf(getSavedSharingApp(context)) }
    var showSharingAppPicker by remember { mutableStateOf(false) }
    val availableSharingApps = remember { getSharingApps(context) }

    // Backup/Restore
    val scope = rememberCoroutineScope()
    val backupManager = remember { DatabaseBackupManager(context) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                val result = backupManager.exportToJson(it)
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                val result = backupManager.importFromJson(it)
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Re-check when returning from settings
    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isUpiMonitorEnabled = isAccessibilityServiceEnabled()
                isNotificationListenerEnabled = isNotificationListenerEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Count disabled permissions
    val disabledPermissionsCount = listOf(
        !hasSmsPermission,
        !isUpiMonitorEnabled,
        !isNotificationListenerEnabled
    ).count { it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Settings", fontWeight = FontWeight.Bold)
                        if (disabledPermissionsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(ExpenseRed, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = disabledPermissionsCount.toString(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // SMS Auto-detect setting
            SettingsToggleItem(
                icon = Icons.Default.Sms,
                title = "Auto-detect SMS",
                subtitle = if (hasSmsPermission) "Detecting payments from SMS" else "Enable to detect payments from SMS",
                checked = hasSmsPermission,
                onCheckedChange = { enabled ->
                    if (enabled && !hasSmsPermission) {
                        smsPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.READ_SMS
                            )
                        )
                        // Notification permission will be requested after SMS permission is granted
                    } else if (hasSmsPermission) {
                        // Open app info to allow user to revoke permission
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // UPI App Monitor setting
            SettingsToggleItem(
                icon = Icons.Default.Payments,
                title = "UPI app monitor",
                subtitle = if (isUpiMonitorEnabled) "Prompting after UPI payments" else "Remind to add after using GPay, CRED, etc.",
                checked = isUpiMonitorEnabled,
                onCheckedChange = {
                    // Open accessibility settings
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Notification Listener setting (for incoming payments)
            SettingsToggleItem(
                icon = Icons.Default.Notifications,
                title = "Auto-detect income",
                subtitle = if (isNotificationListenerEnabled) "Detecting incoming UPI payments" else "Auto-add money received via UPI apps",
                checked = isNotificationListenerEnabled,
                onCheckedChange = {
                    // Open notification listener settings
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    context.startActivity(intent)
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsItem(
                icon = Icons.Default.Category,
                title = "Categories",
                subtitle = "Manage spending categories",
                onClick = onCategoriesClick
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Label,
                title = "Tags",
                subtitle = "Manage tags for trips, events & contexts",
                onClick = onTagsClick
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Sharing app setting
            SettingsItem(
                icon = Icons.Default.Share,
                title = "Split sharing app",
                subtitle = selectedSharingApp.name,
                onClick = { showSharingAppPicker = true }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Export data
            SettingsItem(
                icon = Icons.Default.Upload,
                title = "Export data",
                subtitle = "Backup all data to JSON file",
                onClick = {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val fileName = "xpense_backup_${dateFormat.format(Date())}.json"
                    exportLauncher.launch(fileName)
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Import data
            SettingsItem(
                icon = Icons.Default.Download,
                title = "Import data",
                subtitle = "Restore from backup (replaces all data)",
                onClick = {
                    importLauncher.launch(arrayOf("application/json"))
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }

    // Sharing app picker dialog
    if (showSharingAppPicker) {
        AlertDialog(
            onDismissRequest = { showSharingAppPicker = false },
            title = { Text("Select sharing app") },
            text = {
                LazyColumn {
                    items(availableSharingApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = app.packageName == selectedSharingApp.packageName,
                                    onClick = {
                                        selectedSharingApp = app
                                        saveSharingApp(context, app)
                                        showSharingAppPicker = false
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = app.packageName == selectedSharingApp.packageName,
                                onClick = null
                            )
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSharingAppPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val backgroundColor = if (!checked) {
        ExpenseRed.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (!checked) ExpenseRed else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
