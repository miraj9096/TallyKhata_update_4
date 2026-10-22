package com.example.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import java.text.SimpleDateFormat
import java.util.Locale

enum class CustomerSearchScope {
    PARTIES,
    TRANSACTIONS
}

@Composable
fun CustomerListScreen(
    viewModel: TallyKhataViewModel,
    onCustomerSelected: (Customer) -> Unit
) {
    val customersState by viewModel.rawCustomers.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentFilter by viewModel.customerFilter.collectAsStateWithLifecycle()
    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()

    val transactionsState by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allTransactions = remember(transactionsState) {
        (transactionsState as? UiState.Success)?.data ?: emptyList()
    }
    var searchScope by remember { mutableStateOf(CustomerSearchScope.PARTIES) }
    val matchingTransactions = remember(allTransactions, searchQuery) {
        viewModel.filterTransactions(allTransactions, searchQuery, "ALL")
    }

    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val websiteConfig by viewModel.websiteConfig.collectAsStateWithLifecycle()

    var showAddCustomerDialog by remember { mutableStateOf(false) }

    // Summary calculations
    val totalToReceive = remember(allCustomers) {
        allCustomers.filter { it.netBalance > 0.01 }.sumOf { it.netBalance }
    }
    val totalToPay = remember(allCustomers) {
        allCustomers.filter { it.netBalance < -0.01 }.sumOf { kotlin.math.abs(it.netBalance) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddCustomerScreen() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Customer")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Party", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Store & Summary Card
            SummaryOverviewCard(
                storeName = profile.businessName,
                currency = profile.currency,
                toReceive = totalToReceive,
                toPay = totalToPay,
                isOnline = isOnline,
                isSyncing = isSyncing,
                websiteConfigured = websiteConfig.websiteUrl.isNotBlank(),
                onSyncClick = {
                    if (websiteConfig.websiteUrl.isNotBlank()) {
                        if (websiteConfig.useWebsiteMySqlAsPrimary) {
                            viewModel.syncWithMySqlNow()
                        } else {
                            viewModel.syncFromOffline()
                        }
                    } else {
                        viewModel.selectTab(MainTab.SETTINGS)
                    }
                },
                onDashboardClick = {
                    viewModel.selectTab(MainTab.DASHBOARD)
                }
            )

            // Active Website phpMyAdmin / MySQL Database Engine Banner
            if (websiteConfig.useWebsiteMySqlAsPrimary) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TallyGotGreen.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TallyGotGreen.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { viewModel.syncWithMySqlNow() }
                        .testTag("active_mysql_db_indicator")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = TallyGotGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (websiteConfig.lastPingDbName.isNotBlank())
                                    "phpMyAdmin / MySQL: ${websiteConfig.lastPingDbName}"
                                else
                                    "Primary DB: Website phpMyAdmin / MySQL",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = TallyGotGreen
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = TallyGotGreen
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Syncing...",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TallyGotGreen
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Sync",
                                    tint = TallyGotGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sync",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TallyGotGreen
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("customer_search_input"),
                placeholder = { Text("Search parties, notes, bill #, amount...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // Search Scope Selector (Parties vs Ledger Entries)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = searchScope == CustomerSearchScope.PARTIES,
                    onClick = { searchScope = CustomerSearchScope.PARTIES },
                    label = { Text("Parties (${allCustomers.size})") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("search_scope_parties")
                )
                FilterChip(
                    selected = searchScope == CustomerSearchScope.TRANSACTIONS,
                    onClick = { searchScope = CustomerSearchScope.TRANSACTIONS },
                    label = {
                        val count = if (searchQuery.isBlank()) allTransactions.size else matchingTransactions.size
                        Text("Ledger Entries ($count)")
                    },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("search_scope_transactions")
                )
            }

            if (searchScope == CustomerSearchScope.PARTIES) {
                // Filter Chips for Customer Status
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = currentFilter == CustomerFilter.ALL,
                            onClick = { viewModel.setCustomerFilter(CustomerFilter.ALL) },
                            label = { Text("All Parties (${allCustomers.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = currentFilter == CustomerFilter.YOU_GET,
                            onClick = { viewModel.setCustomerFilter(CustomerFilter.YOU_GET) },
                            label = { Text("You'll Get (Due)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TallyGaveRedBg,
                                selectedLabelColor = TallyGaveRed
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = currentFilter == CustomerFilter.YOU_GIVE,
                            onClick = { viewModel.setCustomerFilter(CustomerFilter.YOU_GIVE) },
                            label = { Text("You'll Give (Advance)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TallyGotGreenBg,
                                selectedLabelColor = TallyGotGreen
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = currentFilter == CustomerFilter.SETTLED,
                            onClick = { viewModel.setCustomerFilter(CustomerFilter.SETTLED) },
                            label = { Text("Settled ($0)") }
                        )
                    }
                }

                // Customer List
                when (val state = customersState) {
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
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    is UiState.Success -> {
                        val list = state.data
                        if (list.isEmpty()) {
                            EmptyCustomerState(
                                hasSearch = searchQuery.isNotBlank(),
                                matchingTxCount = matchingTransactions.size,
                                onAddClick = { viewModel.openAddCustomerScreen() },
                                onSwitchToTransactions = { searchScope = CustomerSearchScope.TRANSACTIONS },
                                onClearSearch = { viewModel.setSearchQuery("") }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .testTag("customer_list"),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(list, key = { it.id }) { customer ->
                                    CustomerCard(
                                        customer = customer,
                                        currency = profile.currency,
                                        onClick = { onCustomerSelected(customer) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Search Scope: TRANSACTIONS (Across All Customer Ledgers)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (searchQuery.isNotBlank()) {
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
                                    text = "Found ${matchingTransactions.size} matching ledger entries",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Tap entry to open party ledger",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (matchingTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) "No ledger entries recorded yet" else "No ledger entries matching \"$searchQuery\"",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Try searching by bill number, note description, party name, or amount.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                if (searchQuery.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { viewModel.setSearchQuery("") },
                                        modifier = Modifier.testTag("clear_global_ledger_search_button")
                                    ) {
                                        Text("Clear Search")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("global_ledger_search_list"),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(matchingTransactions, key = { it.id }) { item ->
                                GlobalLedgerSearchItemCard(
                                    transaction = item,
                                    currency = profile.currency,
                                    onClick = {
                                        val customer = allCustomers.firstOrNull { it.id == item.customerId }
                                        if (customer != null) {
                                            onCustomerSelected(customer)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone, address, type ->
                viewModel.addCustomer(name, phone, address, type)
                showAddCustomerDialog = false
            }
        )
    }
}

@Composable
fun SummaryOverviewCard(
    storeName: String,
    currency: String,
    toReceive: Double,
    toPay: Double,
    isOnline: Boolean = true,
    isSyncing: Boolean = false,
    websiteConfigured: Boolean = false,
    onSyncClick: () -> Unit = {},
    onDashboardClick: () -> Unit = {}
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = storeName.ifBlank { "My Business Khata" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Sync status chip / button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isSyncing -> MaterialTheme.colorScheme.tertiaryContainer
                        !isOnline -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                        websiteConfigured -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.clickable(onClick = onSyncClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Syncing...",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = when {
                                    !isOnline -> Icons.Default.WifiOff
                                    websiteConfigured -> Icons.Default.CloudSync
                                    else -> Icons.Default.Wifi
                                },
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = when {
                                    !isOnline -> MaterialTheme.colorScheme.onErrorContainer
                                    websiteConfigured -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    !isOnline -> "Offline"
                                    websiteConfigured -> "Sync"
                                    else -> "Local"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    !isOnline -> MaterialTheme.colorScheme.onErrorContainer
                                    websiteConfigured -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // You'll Receive (Paben / Due)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = TallyGaveRedBg),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = TallyGaveRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "You'll Get",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = TallyGaveRed
                            )
                        }
                        Text(
                            text = String.format(Locale.getDefault(), "%s %,.0f", currency, toReceive),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = TallyGaveRed
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // You'll Give (Diben / Advance)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = TallyGotGreenBg),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = TallyGotGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "You'll Give",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = TallyGotGreen
                            )
                        }
                        Text(
                            text = String.format(Locale.getDefault(), "%s %,.0f", currency, toPay),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = TallyGotGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDashboardClick)
                    .testTag("summary_card_view_dashboard_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Financial Dashboard & Charts",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Analytics",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerCard(
    customer: Customer,
    currency: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Initial Circle Avatar
                val initial = if (customer.name.isNotBlank()) customer.name.first().uppercase() else "P"
                val avatarBg = when {
                    customer.netBalance > 0.01 -> TallyGaveRedBg
                    customer.netBalance < -0.01 -> TallyGotGreenBg
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val initialColor = when {
                    customer.netBalance > 0.01 -> TallyGaveRed
                    customer.netBalance < -0.01 -> TallyGotGreen
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = initialColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (customer.type == "SUPPLIER") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "Supplier",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (customer.phone.isNotBlank()) {
                        Text(
                            text = customer.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Balance Display
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                val balance = customer.netBalance
                when {
                    balance > 0.01 -> {
                        Text(
                            text = String.format(Locale.getDefault(), "%s %,.0f", currency, balance),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TallyGaveRed
                        )
                        Text(
                            text = "Due (You'll Get)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TallyGaveRed
                        )
                    }
                    balance < -0.01 -> {
                        Text(
                            text = String.format(Locale.getDefault(), "%s %,.0f", currency, kotlin.math.abs(balance)),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TallyGotGreen
                        )
                        Text(
                            text = "Advance (You'll Give)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TallyGotGreen
                        )
                    }
                    else -> {
                        Text(
                            text = "$currency 0",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Settled",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun EmptyCustomerState(
    hasSearch: Boolean,
    matchingTxCount: Int = 0,
    onAddClick: () -> Unit,
    onSwitchToTransactions: (() -> Unit)? = null,
    onClearSearch: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (hasSearch) Icons.Default.Search else Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasSearch) "No party matches your search" else "No parties added yet",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (hasSearch) "Try searching with a different name, phone number, or address." else "Start adding your customers or suppliers to track credits & debts effortlessly.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))

        if (hasSearch) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (matchingTxCount > 0 && onSwitchToTransactions != null) {
                    Button(
                        onClick = onSwitchToTransactions,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("switch_to_tx_search_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search in Ledger ($matchingTxCount Found)")
                    }
                }
                if (onClearSearch != null) {
                    TextButton(
                        onClick = onClearSearch,
                        modifier = Modifier.testTag("clear_customer_search_button")
                    ) {
                        Text("Clear Search Query")
                    }
                }
            }
        } else {
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add First Party")
            }
        }
    }
}

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, address: String, type: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var partyType by remember { mutableStateOf("CUSTOMER") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Party / Khata", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Party / Customer Name *") },
                    placeholder = { Text("e.g. Rahim Store / John") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_customer_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("e.g. +1 555-0199") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_customer_phone_input")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Remarks") },
                    placeholder = { Text("Shop #4, Market Road") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Party Type",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { partyType = "CUSTOMER" }
                    ) {
                        RadioButton(
                            selected = partyType == "CUSTOMER",
                            onClick = { partyType = "CUSTOMER" }
                        )
                        Text("Customer", style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { partyType = "SUPPLIER" }
                    ) {
                        RadioButton(
                            selected = partyType == "SUPPLIER",
                            onClick = { partyType = "SUPPLIER" }
                        )
                        Text("Supplier", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, phone, address, partyType) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_customer_button")
            ) {
                Text("Save Party")
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
fun GlobalLedgerSearchItemCard(
    transaction: Transaction,
    currency: String,
    onClick: () -> Unit
) {
    val isGave = transaction.type == "GAVE"
    val color = if (isGave) TallyGaveRed else TallyGotGreen
    val bg = if (isGave) TallyGaveRedBg else TallyGotGreenBg
    val label = if (isGave) "Credit (Gave)" else "Payment (Got)"
    val dateStr = remember(transaction.transactionDate) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(transaction.transactionDate.toDate())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("ledger_search_card_${transaction.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Party Initial Circle
                val initial = if (transaction.customerName.isNotBlank()) transaction.customerName.first().uppercase() else "P"
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.customerName.ifBlank { "Party Entry" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = bg
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = color,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        if (transaction.billNumber.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Bill #${transaction.billNumber}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    if (transaction.note.isNotBlank()) {
                        Text(
                            text = transaction.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }

                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%s%s %,.0f", if (isGave) "-" else "+", currency, transaction.amount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = color
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ledger",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Ledger",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
