package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.data.model.Transaction
import com.example.ui.theme.TallyGaveRed
import com.example.ui.theme.TallyGaveRedBg
import com.example.ui.theme.TallyGotGreen
import com.example.ui.theme.TallyGotGreenBg
import com.example.ui.theme.TallyNavy
import com.example.ui.theme.TallyTeal
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.export.CsvExporter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customer: Customer,
    viewModel: TallyKhataViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    val transactionsState by viewModel.selectedCustomerTransactions.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()

    // Always get the most up-to-date customer object from list
    val currentCustomer = remember(allCustomers, customer.id) {
        allCustomers.firstOrNull { it.id == customer.id } ?: customer
    }

    val clipboardManager = LocalClipboardManager.current
    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionType by remember { mutableStateOf("GAVE") } // "GAVE" or "GOT"
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    var showExportCsvDialog by remember { mutableStateOf(false) }
    var exportedCsvFile by remember { mutableStateOf<File?>(null) }
    var exportedCsvPreview by remember { mutableStateOf("") }
    var isExportingCsv by remember { mutableStateOf(false) }

    val transactionSearchQuery by viewModel.transactionSearchQuery.collectAsStateWithLifecycle()
    var transactionTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "GAVE", "GOT"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentCustomer.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        if (currentCustomer.phone.isNotBlank()) {
                            Text(
                                text = currentCustomer.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Export CSV statement icon button
                    IconButton(
                        onClick = {
                            val transactions = (transactionsState as? UiState.Success)?.data ?: emptyList()
                            exportedCsvPreview = viewModel.previewCustomerLedgerCsv(currentCustomer, transactions)
                            showExportCsvDialog = true
                        },
                        modifier = Modifier.testTag("export_csv_top_button")
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = "Export CSV Statement")
                    }

                    // Call button
                    if (currentCustomer.phone.isNotBlank()) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${currentCustomer.phone}")
                            }
                            context.startActivity(intent)
                        }) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = "Call Customer")
                        }
                    }
                    // More options menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export Statement (CSV)") },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    val transactions = (transactionsState as? UiState.Success)?.data ?: emptyList()
                                    exportedCsvPreview = viewModel.previewCustomerLedgerCsv(currentCustomer, transactions)
                                    showExportCsvDialog = true
                                },
                                modifier = Modifier.testTag("menu_export_csv_item")
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Party") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showEditDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Party", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirm = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Sticky Action Buttons: YOU GAVE vs YOU GOT
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // YOU GAVE (Credit/Udhaar) -> Red
                    Button(
                        onClick = {
                            transactionType = "GAVE"
                            showTransactionDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("you_gave_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TallyGaveRed,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "YOU GAVE (Credit)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // YOU GOT (Debit/Payment) -> Green
                    Button(
                        onClick = {
                            transactionType = "GOT"
                            showTransactionDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("you_got_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TallyGotGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "YOU GOT (Payment)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Balance Card
            CustomerBalanceSummaryCard(
                customer = currentCustomer,
                currency = profile.currency,
                storeName = profile.businessName,
                onSendReminder = {
                    val message = "Dear ${currentCustomer.name}, your outstanding balance at ${profile.businessName} is ${profile.currency} ${String.format(Locale.getDefault(), "%,.0f", currentCustomer.netBalance)}. Please clear it at your convenience. Thank you!"
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, message)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Send Payment Reminder"))
                },
                onExportCsv = {
                    val transactions = (transactionsState as? UiState.Success)?.data ?: emptyList()
                    exportedCsvPreview = viewModel.previewCustomerLedgerCsv(currentCustomer, transactions)
                    showExportCsvDialog = true
                }
            )

            // Ledger Entries Header & Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRANSACTION ENTRIES",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Ledger Search Bar
            OutlinedTextField(
                value = transactionSearchQuery,
                onValueChange = { viewModel.setTransactionSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("transaction_search_input"),
                placeholder = { Text("Search by note, bill #, amount, date...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (transactionSearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setTransactionSearchQuery("") },
                            modifier = Modifier.testTag("clear_transaction_search")
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Ledger List & Filtered Results
            when (val state = transactionsState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is UiState.Success -> {
                    val transactions = state.data
                    val filteredTransactions = remember(transactions, transactionSearchQuery, transactionTypeFilter) {
                        viewModel.filterTransactions(transactions, transactionSearchQuery, transactionTypeFilter)
                    }
                    val totalGaveInFilter = remember(filteredTransactions) {
                        filteredTransactions.filter { it.type == "GAVE" }.sumOf { it.amount }
                    }
                    val totalGotInFilter = remember(filteredTransactions) {
                        filteredTransactions.filter { it.type == "GOT" }.sumOf { it.amount }
                    }

                    // Filter Chips Row
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = transactionTypeFilter == "ALL",
                                onClick = { transactionTypeFilter = "ALL" },
                                label = { Text("All (${transactions.size})") },
                                modifier = Modifier.testTag("filter_all_transactions")
                            )
                        }
                        item {
                            FilterChip(
                                selected = transactionTypeFilter == "GAVE",
                                onClick = { transactionTypeFilter = "GAVE" },
                                label = { Text("Credit (${transactions.count { it.type == "GAVE" }})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TallyGaveRedBg,
                                    selectedLabelColor = TallyGaveRed
                                ),
                                modifier = Modifier.testTag("filter_gave_transactions")
                            )
                        }
                        item {
                            FilterChip(
                                selected = transactionTypeFilter == "GOT",
                                onClick = { transactionTypeFilter = "GOT" },
                                label = { Text("Payment (${transactions.count { it.type == "GOT" }})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TallyGotGreenBg,
                                    selectedLabelColor = TallyGotGreen
                                ),
                                modifier = Modifier.testTag("filter_got_transactions")
                            )
                        }
                    }

                    // Active Search / Filter Stats Bar
                    if (transactionSearchQuery.isNotBlank() || transactionTypeFilter != "ALL") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Found ${filteredTransactions.size} of ${transactions.size} entries",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (filteredTransactions.isNotEmpty()) {
                                    Text(
                                        text = "Gave: ${profile.currency}${String.format(Locale.getDefault(), "%,.0f", totalGaveInFilter)} | Got: ${profile.currency}${String.format(Locale.getDefault(), "%,.0f", totalGotInFilter)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (transactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No transactions yet",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap 'YOU GAVE' to add a credit sale or 'YOU GOT' to record payment.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else if (filteredTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "No entries matching \"$transactionSearchQuery\"",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Try searching by bill number, note/item name, or exact amount.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        viewModel.setTransactionSearchQuery("")
                                        transactionTypeFilter = "ALL"
                                    },
                                    modifier = Modifier.testTag("clear_transaction_search_button")
                                ) {
                                    Text("Clear Search & Filters")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("customer_ledger_transaction_list"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredTransactions, key = { it.id }) { item ->
                                TransactionItemCard(
                                    transaction = item,
                                    currency = profile.currency,
                                    onDeleteClick = { transactionToDelete = item }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Transaction Dialog
    if (showTransactionDialog) {
        RecordTransactionDialog(
            type = transactionType,
            currency = profile.currency,
            customerName = currentCustomer.name,
            onDismiss = { showTransactionDialog = false },
            onConfirm = { amount, note, billNumber ->
                viewModel.recordTransaction(
                    customer = currentCustomer,
                    type = transactionType,
                    amount = amount,
                    note = note,
                    billNumber = billNumber
                )
                showTransactionDialog = false
            }
        )
    }

    // Confirm Delete Transaction Dialog
    transactionToDelete?.let { trans ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Entry?") },
            text = { Text("Are you sure you want to delete this ${trans.type} entry of ${profile.currency} ${trans.amount}? This will recalculate the party's net balance.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(trans, currentCustomer)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm Delete Party Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Party?") },
            text = { Text("Deleting ${currentCustomer.name} will remove their record from your customer list.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(currentCustomer)
                        showDeleteConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Party")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Party Dialog
    if (showEditDialog) {
        EditCustomerDialog(
            customer = currentCustomer,
            onDismiss = { showEditDialog = false },
            onConfirm = { updated ->
                viewModel.updateCustomer(updated)
                showEditDialog = false
            }
        )
    }

    // Export Statement CSV Dialog
    if (showExportCsvDialog) {
        val transactions = (transactionsState as? UiState.Success)?.data ?: emptyList()
        ExportCsvDialog(
            customerName = currentCustomer.name,
            csvContent = exportedCsvPreview,
            isExporting = isExportingCsv,
            onDismiss = {
                showExportCsvDialog = false
                exportedCsvFile = null
            },
            onShareFile = {
                isExportingCsv = true
                viewModel.exportCustomerLedgerCsv(
                    context = context,
                    customer = currentCustomer,
                    transactions = transactions
                ) { success, msg, file ->
                    isExportingCsv = false
                    if (success && file != null) {
                        exportedCsvFile = file
                        CsvExporter.shareCsvFile(
                            context = context,
                            file = file,
                            subject = "${currentCustomer.name} - Ledger Statement"
                        )
                    } else {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onCopyText = {
                clipboardManager.setText(AnnotatedString(exportedCsvPreview))
                Toast.makeText(context, "CSV copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun CustomerBalanceSummaryCard(
    customer: Customer,
    currency: String,
    storeName: String,
    onSendReminder: () -> Unit,
    onExportCsv: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                customer.netBalance > 0.01 -> TallyGaveRedBg
                customer.netBalance < -0.01 -> TallyGotGreenBg
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            val balance = customer.netBalance
            val balanceLabel = when {
                balance > 0.01 -> "Net Balance: Due (You'll Get)"
                balance < -0.01 -> "Net Balance: Advance (You'll Give)"
                else -> "Net Balance: Settled"
            }
            val balanceColor = when {
                balance > 0.01 -> TallyGaveRed
                balance < -0.01 -> TallyGotGreen
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Text(
                text = balanceLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = balanceColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = String.format(Locale.getDefault(), "%s %,.0f", currency, kotlin.math.abs(balance)),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp
                ),
                color = balanceColor
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-totals row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Credit Given:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%s %,.0f", currency, customer.totalGave),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TallyGaveRed
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Received:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%s %,.0f", currency, customer.totalGot),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TallyGotGreen
                    )
                }
            }

            // Card Action Buttons: Share Reminder & Export CSV
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (balance > 0.01) {
                    FilledTonalButton(
                        onClick = onSendReminder,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("send_reminder_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reminder", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                OutlinedButton(
                    onClick = onExportCsv,
                    modifier = Modifier
                        .then(if (balance > 0.01) Modifier.weight(1f) else Modifier.fillMaxWidth())
                        .testTag("card_export_csv_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export CSV", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun TransactionItemCard(
    transaction: Transaction,
    currency: String,
    onDeleteClick: () -> Unit
) {
    val isGave = transaction.type == "GAVE"
    val amountColor = if (isGave) TallyGaveRed else TallyGotGreen
    val typeLabel = if (isGave) "You Gave (Credit)" else "You Got (Payment)"
    val dateStr = remember(transaction.transactionDate) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val date = transaction.transactionDate.toDate()
        sdf.format(date)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(amountColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = amountColor
                    )
                }

                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (transaction.billNumber.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Bill: #${transaction.billNumber}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.getDefault(), "%s%s %,.0f", if (isGave) "-" else "+", currency, transaction.amount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = amountColor
                )

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete entry",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RecordTransactionDialog(
    type: String, // "GAVE" or "GOT"
    currency: String,
    customerName: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String, billNumber: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var billNumber by remember { mutableStateOf("") }

    val isGave = type == "GAVE"
    val title = if (isGave) "Record Credit (+You Gave)" else "Record Payment (+You Got)"
    val buttonColor = if (isGave) TallyGaveRed else TallyGotGreen

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = title, fontWeight = FontWeight.Bold)
                Text(
                    text = "For: $customerName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Amount ($currency) *") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_amount_input")
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Details / Note") },
                    placeholder = { Text(if (isGave) "e.g. 5 sacks of rice, credit" else "e.g. Cash received, partial payment") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_note_input")
                )

                OutlinedTextField(
                    value = billNumber,
                    onValueChange = { billNumber = it },
                    label = { Text("Bill / Invoice # (Optional)") },
                    placeholder = { Text("e.g. INV-2024") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amount = amountText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { onConfirm(amount, note, billNumber) },
                enabled = amount > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                modifier = Modifier.testTag("confirm_transaction_button")
            ) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditCustomerDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onConfirm: (Customer) -> Unit
) {
    var name by remember { mutableStateOf(customer.name) }
    var phone by remember { mutableStateOf(customer.phone) }
    var address by remember { mutableStateOf(customer.address) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Party Details") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Party Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(customer.copy(name = name.trim(), phone = phone.trim(), address = address.trim()))
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExportCsvDialog(
    customerName: String,
    csvContent: String,
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onShareFile: () -> Unit,
    onCopyText: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Export Ledger (CSV)",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Statement for $customerName formatted according to RFC 4180 standard (compatible with Excel, Google Sheets, Tally, and accounting suites).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        Text(
                            text = csvContent.ifBlank { "No transactions to export yet." },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onCopyText,
                        modifier = Modifier.testTag("copy_csv_preview_button")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Raw CSV", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onShareFile,
                enabled = !isExporting && csvContent.isNotBlank(),
                modifier = Modifier.testTag("share_csv_action_button")
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exporting...")
                } else {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share CSV File")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_csv_dialog_button")
            ) {
                Text("Close")
            }
        }
    )
}

