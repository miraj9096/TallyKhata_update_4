package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.TableChart
import com.example.data.backup.BackupFileInfo
import com.example.data.export.CsvExporter
import com.example.ui.theme.TallyGaveRed
import com.example.ui.theme.TallyGotGreen
import com.example.ui.theme.TallyGotGreenBg
import com.example.ui.theme.TallyNavy
import com.example.ui.theme.TallyTeal
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import com.example.notification.BackupReminderHelper
import com.example.notification.BackupReminderSettings
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CheckCircle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: TallyKhataViewModel,
    onSignOutClicked: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val websiteConfig by viewModel.websiteConfig.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val localBackups by viewModel.localBackups.collectAsStateWithLifecycle()

    var businessName by remember(profile.businessName) { mutableStateOf(profile.businessName) }
    var ownerName by remember(profile.ownerName) { mutableStateOf(profile.ownerName) }
    var phone by remember(profile.phone) { mutableStateOf(profile.phone) }
    var email by remember(profile.email) { mutableStateOf(profile.email) }
    var currency by remember(profile.currency) { mutableStateOf(profile.currency) }

    // Website Sync inputs
    var websiteUrl by remember(websiteConfig.websiteUrl) { mutableStateOf(websiteConfig.websiteUrl) }
    var apiKey by remember(websiteConfig.apiKey) { mutableStateOf(websiteConfig.apiKey) }
    var storeId by remember(websiteConfig.storeId) { mutableStateOf(websiteConfig.storeId) }
    var autoSync by remember(websiteConfig.autoSync) { mutableStateOf(websiteConfig.autoSync) }
    var useAsPrimary by remember(websiteConfig.useWebsiteMySqlAsPrimary) { mutableStateOf(websiteConfig.useWebsiteMySqlAsPrimary) }

    val dbConnectionInfo by viewModel.dbConnectionInfo.collectAsStateWithLifecycle()
    var testConnectionResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    // Dialogs state
    var showPhpScriptDialog by remember { mutableStateOf(false) }
    var showPhpMyAdminSqlDialog by remember { mutableStateOf(false) }
    var showSqlDumpDialog by remember { mutableStateOf(false) }
    var currentSqlDump by remember { mutableStateOf("") }
    var backupToRestore by remember { mutableStateOf<BackupFileInfo?>(null) }
    var showPullConfirmDialog by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notifications enabled for CSV reminders", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied. Scheduled reminders won't appear.", Toast.LENGTH_LONG).show()
        }
    }

    var reminderSettings by remember {
        mutableStateOf(BackupReminderHelper.getSettings(context))
    }

    val commonCurrencies = listOf("$", "₹", "৳", "€", "£", "AED", "SAR", "¥")

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Business Profile Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Business / Store Settings",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Store / Shop Name") },
                        placeholder = { Text("e.g. Al-Madina Super Shop") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_business_name_input")
                    )

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("Owner / Merchant Name") },
                        placeholder = { Text("e.g. Mohammad Ali") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Business Contact Phone") },
                        placeholder = { Text("+880 1711-000000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Business Contact Email") },
                        placeholder = { Text("shop@example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Currency Selector
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = "Currency Symbol",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(commonCurrencies) { sym ->
                                FilterChip(
                                    selected = currency == sym,
                                    onClick = { currency = sym },
                                    label = { Text(sym, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                                    leadingIcon = if (currency == sym) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            viewModel.updateProfile(businessName, ownerName, phone, email, currency)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_settings_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Business Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2. Website phpMyAdmin / MySQL Database Engine Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Website phpMyAdmin / MySQL",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Use website MySQL as your primary database",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Online/Offline status badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isOnline) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isOnline) "Online" else "Offline",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Primary Database Mode Switch Banner
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (useAsPrimary) TallyGotGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = if (useAsPrimary) androidx.compose.foundation.BorderStroke(1.5.dp, TallyGotGreen.copy(alpha = 0.5f)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (useAsPrimary) Icons.Default.Check else Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = if (useAsPrimary) TallyGotGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (useAsPrimary) "Primary Database: Active (MySQL)" else "Website MySQL Database Engine",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (useAsPrimary) TallyGotGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (useAsPrimary)
                                        "All customer records & ledger entries write directly to MySQL with instant offline caching"
                                    else
                                        "Enable to make website MySQL/phpMyAdmin your primary ledger database",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = useAsPrimary,
                                onCheckedChange = {
                                    useAsPrimary = it
                                    viewModel.setUseWebsiteMySqlAsPrimary(it)
                                },
                                modifier = Modifier.testTag("use_mysql_primary_switch")
                            )
                        }
                    }

                    // Quick Setup Assistant Shortcuts
                    Text(
                        text = "phpMyAdmin & Web Hosting Setup",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPhpMyAdminSqlDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("phpmyadmin_sql_importer_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("phpMyAdmin SQL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showPhpScriptDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("php_script_setup_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PHP API Script", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Live Database Info Card (if connected)
                    dbConnectionInfo?.let { info ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = TallyGotGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Connected Database: ${info.databaseName.ifBlank { "MySQL Host" }}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "MySQL Version: ${info.serverVersion.ifBlank { "5.7+ / MariaDB" }} • Store: ${info.storeId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Remote Data: ${info.customerCount} parties • ${info.transactionCount} transactions • ${info.cashbookCount} cashbook entries",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Last Sync Status Banner
                    if (websiteConfig.lastSyncTime > 0L) {
                        val formattedLastSync = remember(websiteConfig.lastSyncTime) {
                            SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(websiteConfig.lastSyncTime))
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when (websiteConfig.lastSyncStatus) {
                                "SUCCESS" -> TallyGotGreen.copy(alpha = 0.12f)
                                "FAILED" -> TallyGaveRed.copy(alpha = 0.12f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (websiteConfig.lastSyncStatus == "SUCCESS") Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (websiteConfig.lastSyncStatus == "SUCCESS") TallyGotGreen else TallyGaveRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Last synced: $formattedLastSync",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (websiteConfig.lastSyncMessage.isNotBlank()) {
                                        Text(
                                            text = websiteConfig.lastSyncMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Server Configuration Fields
                    OutlinedTextField(
                        value = websiteUrl,
                        onValueChange = { websiteUrl = it },
                        label = { Text("Website MySQL API Endpoint URL") },
                        placeholder = { Text("https://yourwebsite.com/api/tally_sync.php") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("website_url_input")
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Secret Key / Token") },
                        placeholder = { Text("e.g. TALLY_SECRET_KEY_12345") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("website_api_key_input")
                    )

                    OutlinedTextField(
                        value = storeId,
                        onValueChange = { storeId = it },
                        label = { Text("Store Identifier (Store ID)") },
                        placeholder = { Text("store_main") },
                        leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Auto-sync row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Sync when Online",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Automatically sync local changes when network connects",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoSync,
                            onCheckedChange = { autoSync = it },
                            modifier = Modifier.testTag("auto_sync_switch")
                        )
                    }

                    // Test connection & Save Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isTestingConnection = true
                                testConnectionResult = null
                                viewModel.testWebsiteConnectionDetailed(websiteUrl, apiKey, storeId) { success, info, msg ->
                                    isTestingConnection = false
                                    testConnectionResult = Pair(success, msg)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("test_website_connection_button"),
                            enabled = !isTestingConnection && websiteUrl.isNotBlank()
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Connection")
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.saveWebsiteConfig(websiteUrl, apiKey, storeId, autoSync, useAsPrimary)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("save_website_config_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Config")
                        }
                    }

                    // Connection test result banner
                    testConnectionResult?.let { (success, message) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (success) TallyGotGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (success) "✓ $message" else "✗ $message",
                                color = if (success) TallyGotGreen else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Primary Live Sync Button
                    Button(
                        onClick = {
                            viewModel.syncWithMySqlNow()
                        },
                        enabled = !isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("sync_from_offline_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Synchronizing with MySQL...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync with phpMyAdmin Now", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Secondary Maintenance Push / Pull
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPullConfirmDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled = !isSyncing
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pull from MySQL", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.syncFromOffline() },
                            modifier = Modifier.weight(1f),
                            enabled = !isSyncing
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Push to MySQL", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 3. Scheduled CSV Backup Reminders Card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("backup_reminders_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header with switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (reminderSettings.isEnabled) TallyGotGreenBg else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (reminderSettings.isEnabled) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = "Backup Reminder",
                                    tint = if (reminderSettings.isEnabled) TallyGotGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Scheduled CSV Backup Reminders",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (reminderSettings.isEnabled) {
                                        "Active • ${BackupReminderHelper.formatFrequencyLabel(reminderSettings.frequencyDays)}"
                                    } else {
                                        "Paused • Tap switch to enable"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (reminderSettings.isEnabled) TallyGotGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = reminderSettings.isEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                                reminderSettings = BackupReminderHelper.saveSettings(context, isChecked, reminderSettings.frequencyDays)
                                val msg = if (isChecked) {
                                    "Backup reminders enabled: ${BackupReminderHelper.formatFrequencyLabel(reminderSettings.frequencyDays)}"
                                } else {
                                    "Backup reminders paused"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("backup_reminder_switch")
                        )
                    }

                    // Notification permission banner if on Android 13+ and missing permission
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Permission Needed",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Notifications are blocked. Grant permission to receive scheduled backup alerts.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.testTag("grant_notification_permission_button")
                                ) {
                                    Text("Grant", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = reminderSettings.isEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Frequency selection chips
                            Column {
                                Text(
                                    text = "Reminder Frequency",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    item {
                                        FilterChip(
                                            selected = reminderSettings.frequencyDays == BackupReminderHelper.FREQUENCY_WEEKLY,
                                            onClick = {
                                                reminderSettings = BackupReminderHelper.saveSettings(
                                                    context,
                                                    true,
                                                    BackupReminderHelper.FREQUENCY_WEEKLY
                                                )
                                            },
                                            label = { Text("Weekly (Rec.)") },
                                            leadingIcon = if (reminderSettings.frequencyDays == BackupReminderHelper.FREQUENCY_WEEKLY) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            modifier = Modifier.testTag("frequency_weekly_chip")
                                        )
                                    }
                                    item {
                                        FilterChip(
                                            selected = reminderSettings.frequencyDays == BackupReminderHelper.FREQUENCY_DAILY,
                                            onClick = {
                                                reminderSettings = BackupReminderHelper.saveSettings(
                                                    context,
                                                    true,
                                                    BackupReminderHelper.FREQUENCY_DAILY
                                                )
                                            },
                                            label = { Text("Daily") },
                                            leadingIcon = if (reminderSettings.frequencyDays == BackupReminderHelper.FREQUENCY_DAILY) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            modifier = Modifier.testTag("frequency_daily_chip")
                                        )
                                    }
                                    item {
                                        FilterChip(
                                            selected = reminderSettings.frequencyDays == BackupReminderHelper.FREQUENCY_BIWEEKLY,
                                            onClick = {
                                                reminderSettings = BackupReminderHelper.saveSettings(
                                                    context,
                                                    true,
                                                    BackupReminderHelper.FREQUENCY_BIWEEKLY
                                                )
                                            },
                                            label = { Text("Bi-Weekly (14d)") },
                                            leadingIcon = if (reminderSettings.frequencyDays == BackupReminderHelper.FREQUENCY_BIWEEKLY) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            modifier = Modifier.testTag("frequency_biweekly_chip")
                                        )
                                    }
                                    item {
                                        FilterChip(
                                            selected = reminderSettings.frequencyDays == BackupReminderHelper.FREQUENCY_MONTHLY,
                                            onClick = {
                                                reminderSettings = BackupReminderHelper.saveSettings(
                                                    context,
                                                    true,
                                                    BackupReminderHelper.FREQUENCY_MONTHLY
                                                )
                                            },
                                            label = { Text("Monthly (30d)") },
                                            leadingIcon = if (reminderSettings.frequencyDays == BackupReminderHelper.FREQUENCY_MONTHLY) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            modifier = Modifier.testTag("frequency_monthly_chip")
                                        )
                                    }
                                }
                            }

                            // Schedule details info box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Next Reminder: ",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = BackupReminderHelper.formatScheduledTime(reminderSettings.nextScheduledMillis),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (reminderSettings.lastExportMillis > 0L) TallyGotGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Last CSV Export: ",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = BackupReminderHelper.formatLastExportTime(reminderSettings.lastExportMillis),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Action buttons: Test Notification & Direct Export
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            val sent = BackupReminderHelper.showBackupNotification(context, isTest = true)
                                            if (sent) {
                                                Toast.makeText(context, "Test notification dispatched! Check your status bar.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Unable to post notification. Check device settings.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("send_test_notification_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Send Test Alert", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.exportAllTransactionsCsv(context) { success, msg, file ->
                                            reminderSettings = BackupReminderHelper.getSettings(context)
                                            if (success && file != null) {
                                                CsvExporter.shareCsvFile(context, file, "All Transactions Journal (CSV)")
                                            } else {
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("quick_export_csv_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TallyGotGreen
                                    )
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export CSV Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Local Data Backups & SQL Dumps Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Offline Data Backups & SQL",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Create local snapshots & export .sql files",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.createOfflineBackup() },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("create_backup_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Backup")
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    currentSqlDump = viewModel.generateSqlDump()
                                    showSqlDumpDialog = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("export_sql_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export .SQL")
                        }
                    }

                    // CSV Spreadsheet Exports Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.exportAllCustomersCsv(context) { success, msg, file ->
                                    reminderSettings = BackupReminderHelper.getSettings(context)
                                    if (success && file != null) {
                                        CsvExporter.shareCsvFile(context, file, "Parties Directory (CSV)")
                                    } else {
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("export_parties_csv_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Parties CSV", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.exportAllTransactionsCsv(context) { success, msg, file ->
                                    reminderSettings = BackupReminderHelper.getSettings(context)
                                    if (success && file != null) {
                                        CsvExporter.shareCsvFile(context, file, "All Transactions Journal (CSV)")
                                    } else {
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("export_journal_csv_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Journal CSV", fontSize = 12.sp)
                        }
                    }

                    // Backups List
                    if (localBackups.isNotEmpty()) {
                        Text(
                            text = "Stored Backups & Exports (${localBackups.size})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            localBackups.take(6).forEach { backup ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val badgeLabel = when {
                                                    backup.isCsv -> "CSV"
                                                    backup.isSql -> "SQL"
                                                    else -> "JSON"
                                                }
                                                val badgeBg = when {
                                                    backup.isCsv -> TallyGotGreenBg
                                                    backup.isSql -> MaterialTheme.colorScheme.tertiaryContainer
                                                    else -> MaterialTheme.colorScheme.primaryContainer
                                                }
                                                val badgeTextColor = when {
                                                    backup.isCsv -> TallyGotGreen
                                                    backup.isSql -> MaterialTheme.colorScheme.onTertiaryContainer
                                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = badgeBg
                                                ) {
                                                    Text(
                                                        text = badgeLabel,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = badgeTextColor,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = backup.formattedDate,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            if (backup.isCsv) {
                                                Text(
                                                    text = "Spreadsheet Ledger • ${(backup.sizeBytes / 1024).coerceAtLeast(1)} KB",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else if (backup.isSql) {
                                                Text(
                                                    text = "MySQL Database Dump • ${(backup.sizeBytes / 1024).coerceAtLeast(1)} KB",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                Text(
                                                    text = "${backup.customerCount} parties • ${backup.transactionCount} transactions • ${(backup.sizeBytes / 1024).coerceAtLeast(1)} KB",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row {
                                            if (!backup.isSql && !backup.isCsv) {
                                                IconButton(onClick = { backupToRestore = backup }) {
                                                    Icon(Icons.Default.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (backup.isCsv) {
                                                        CsvExporter.shareCsvFile(context, File(backup.filePath), backup.fileName)
                                                    } else {
                                                        viewModel.getBackupFileContent(backup.fileName) { content ->
                                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                                type = "text/plain"
                                                                putExtra(Intent.EXTRA_SUBJECT, backup.fileName)
                                                                putExtra(Intent.EXTRA_TEXT, content)
                                                            }
                                                            context.startActivity(Intent.createChooser(sendIntent, "Share Backup"))
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                                            }
                                            IconButton(onClick = { viewModel.deleteBackup(backup) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Account & Sign Out Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val localUser = remember { com.example.data.auth.AuthSessionManager(context).getCurrentUser() }
                    val firebaseUser = Firebase.auth.currentUser
                    val accountTitle = when {
                        firebaseUser != null -> "Google Cloud Account"
                        localUser?.authType == "MOBILE" -> "Mobile Ledger Account"
                        localUser?.authType == "EMAIL" -> "Business Email Account"
                        else -> "Store Account"
                    }
                    val accountSubtitle = when {
                        firebaseUser != null -> firebaseUser.email ?: "Google Account"
                        localUser != null -> localUser.phone ?: localUser.email ?: localUser.storeName
                        else -> "Active Store Session"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (localUser?.authType == "MOBILE") Icons.Default.PhoneAndroid else if (localUser?.authType == "EMAIL") Icons.Default.Email else Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = accountTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = accountSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            signOut(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignOutClicked,
                                scope = coroutineScope
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("sign_out_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                }
            }

            // 5. App Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Tally Khata v1.0",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Daily Khata, Udhaar Ledger & Cash Book with offline backup & website MySQL database synchronization.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Dialog: PHP & MySQL Setup Script
    if (showPhpScriptDialog) {
        val phpScript = remember { viewModel.getPhpScriptTemplate() }
        AlertDialog(
            onDismissRequest = { showPhpScriptDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Website MySQL Backend Script", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "1. Save this script as 'tally_sync.php' and upload to your website hosting (cPanel public_html/api).\n" +
                               "2. Configure your MySQL host, database name, username and password in the script.\n" +
                               "3. Tables will be created automatically on first sync!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        ) {
                            Text(
                                text = phpScript,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(phpScript))
                        Toast.makeText(context, "Copied tally_sync.php to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy PHP Script")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhpScriptDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: phpMyAdmin SQL Importer Script
    if (showPhpMyAdminSqlDialog) {
        val phpMyAdminSql = remember { viewModel.getPhpMyAdminSetupSql() }
        AlertDialog(
            onDismissRequest = { showPhpMyAdminSqlDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("phpMyAdmin SQL Query", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "1. Open phpMyAdmin from your cPanel / web hosting.\n" +
                               "2. Select or create your MySQL database.\n" +
                               "3. Click the 'SQL' tab at the top.\n" +
                               "4. Paste this code and click 'Go' to create tables!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        ) {
                            Text(
                                text = phpMyAdminSql,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(phpMyAdminSql))
                        Toast.makeText(context, "Copied phpMyAdmin SQL query to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy SQL for phpMyAdmin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhpMyAdminSqlDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: SQL Dump Preview
    if (showSqlDumpDialog) {
        AlertDialog(
            onDismissRequest = { showSqlDumpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MySQL .SQL Database Dump", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Ready to import into phpMyAdmin, MySQL Workbench, or your hosting control panel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        ) {
                            Text(
                                text = currentSqlDump,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(currentSqlDump))
                        Toast.makeText(context, "Copied SQL dump to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy SQL")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSqlDumpDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: Confirm Restore from Backup
    backupToRestore?.let { backup ->
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            title = { Text("Restore Offline Backup?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will restore ${backup.customerCount} parties and ${backup.transactionCount} transactions from backup '${backup.formattedDate}'. Existing data will be overwritten.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreFromBackup(backup)
                        backupToRestore = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Confirm Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Confirm Pull from Website MySQL
    if (showPullConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPullConfirmDialog = false },
            title = { Text("Pull from Website MySQL?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will download all ledger records from your website MySQL database and replace your current local SQLite database. Are you sure you want to proceed?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPullConfirmDialog = false
                        viewModel.pullFromWebsite()
                    }
                ) {
                    Text("Pull & Replace")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPullConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
