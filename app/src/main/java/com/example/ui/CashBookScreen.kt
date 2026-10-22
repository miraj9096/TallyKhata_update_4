package com.example.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CashEntry
import com.example.ui.theme.TallyGaveRed
import com.example.ui.theme.TallyGaveRedBg
import com.example.ui.theme.TallyGotGreen
import com.example.ui.theme.TallyGotGreenBg
import com.example.ui.theme.TallyTeal
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CashBookScreen(
    viewModel: TallyKhataViewModel
) {
    val cashEntriesState by viewModel.cashbookEntries.collectAsStateWithLifecycle()
    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()

    var showAddCashDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("CASH_IN") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "CASH_IN", "CASH_OUT"
    var searchQuery by remember { mutableStateOf("") }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = cashEntriesState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is UiState.Success -> {
                    val entries = state.data
                    val totalIn = remember(entries) {
                        entries.filter { it.type == "CASH_IN" }.sumOf { it.amount }
                    }
                    val totalOut = remember(entries) {
                        entries.filter { it.type == "CASH_OUT" }.sumOf { it.amount }
                    }
                    val netBalance = totalIn - totalOut

                    val filteredEntries = remember(entries, selectedFilter, searchQuery) {
                        val typeFiltered = when (selectedFilter) {
                            "CASH_IN" -> entries.filter { it.type == "CASH_IN" }
                            "CASH_OUT" -> entries.filter { it.type == "CASH_OUT" }
                            else -> entries
                        }
                        if (searchQuery.isBlank()) typeFiltered
                        else {
                            val q = searchQuery.trim()
                            typeFiltered.filter {
                                it.note.contains(q, ignoreCase = true) ||
                                it.category.contains(q, ignoreCase = true) ||
                                it.amount.toString().contains(q) ||
                                String.format(Locale.getDefault(), "%,.0f", it.amount).contains(q)
                            }
                        }
                    }

                    // Cash Balance Header Card
                    CashSummaryCard(
                        currency = profile.currency,
                        totalIn = totalIn,
                        totalOut = totalOut,
                        netCash = netBalance,
                        onAddCashIn = {
                            selectedType = "CASH_IN"
                            showAddCashDialog = true
                        },
                        onAddCashOut = {
                            selectedType = "CASH_OUT"
                            showAddCashDialog = true
                        }
                    )

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        placeholder = { Text("Search cash entries by note, category, amount...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Filter chips row
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == "ALL",
                                onClick = { selectedFilter = "ALL" },
                                label = { Text("All (${entries.size})") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == "CASH_IN",
                                onClick = { selectedFilter = "CASH_IN" },
                                label = { Text("Cash In (Sales)") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == "CASH_OUT",
                                onClick = { selectedFilter = "CASH_OUT" },
                                label = { Text("Cash Out (Expenses)") }
                            )
                        }
                    }

                    // List of entries
                    if (filteredEntries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) "No cash transactions recorded" else "No entries matching \"$searchQuery\"",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (searchQuery.isBlank()) "Record daily cash counter sales or business expenses." else "Try searching by category or note keywords.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                if (searchQuery.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(onClick = { searchQuery = "" }) {
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
                                .testTag("cashbook_list"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredEntries, key = { it.id }) { item ->
                                CashEntryRow(
                                    entry = item,
                                    currency = profile.currency,
                                    onDelete = { viewModel.deleteCashEntry(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCashDialog) {
        AddCashEntryDialog(
            initialType = selectedType,
            currency = profile.currency,
            onDismiss = { showAddCashDialog = false },
            onConfirm = { type, amount, category, note ->
                viewModel.addCashEntry(type, amount, category, note)
                showAddCashDialog = false
            }
        )
    }
}

@Composable
fun CashSummaryCard(
    currency: String,
    totalIn: Double,
    totalOut: Double,
    netCash: Double,
    onAddCashIn: () -> Unit,
    onAddCashOut: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "CASH IN HAND (DAILY HISAB)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = String.format(Locale.getDefault(), "%s %,.0f", currency, netCash),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp
                ),
                color = if (netCash >= 0) TallyTeal else TallyGaveRed
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Cash In Total
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = TallyGotGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Total Cash In",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = String.format(Locale.getDefault(), "%s %,.0f", currency, totalIn),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TallyGotGreen
                    )
                }

                // Cash Out Total
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = TallyGaveRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Total Cash Out",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = String.format(Locale.getDefault(), "%s %,.0f", currency, totalOut),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TallyGaveRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAddCashIn,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("cash_in_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TallyGotGreen)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Cash In", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAddCashOut,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("cash_out_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TallyGaveRed)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("- Cash Out", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CashEntryRow(
    entry: CashEntry,
    currency: String,
    onDelete: () -> Unit
) {
    val isIn = entry.type == "CASH_IN"
    val color = if (isIn) TallyGotGreen else TallyGaveRed
    val dateStr = remember(entry.entryDate) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(entry.entryDate.toDate())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isIn) TallyGotGreenBg else TallyGaveRedBg
                    ) {
                        Text(
                            text = if (isIn) "CASH IN" else "CASH OUT",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (entry.category.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = entry.category,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (entry.note.isNotBlank()) {
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(Locale.getDefault(), "%s%s %,.0f", if (isIn) "+" else "-", currency, entry.amount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = color
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete entry",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddCashEntryDialog(
    initialType: String,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (type: String, amount: Double, category: String, note: String) -> Unit
) {
    var type by remember { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(if (initialType == "CASH_IN") "Sales" else "Expense") }
    var note by remember { mutableStateOf("") }

    val isIn = type == "CASH_IN"

    val categoriesIn = listOf("Daily Sales", "Counter Cash", "Customer Due Payment", "Owner Capital", "Other Inflow")
    val categoriesOut = listOf("Shop Rent", "Electricity / Bill", "Staff Salary", "Tea / Snacks", "Stock Purchase", "Transport", "Misc")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isIn) "Record Cash In (Counter Sales)" else "Record Cash Out (Expense)",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            type = "CASH_IN"
                            category = "Daily Sales"
                        }
                    ) {
                        RadioButton(selected = isIn, onClick = {
                            type = "CASH_IN"
                            category = "Daily Sales"
                        })
                        Text("Cash In (+)", color = TallyGotGreen, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            type = "CASH_OUT"
                            category = "Tea / Snacks"
                        }
                    ) {
                        RadioButton(selected = !isIn, onClick = {
                            type = "CASH_OUT"
                            category = "Tea / Snacks"
                        })
                        Text("Cash Out (-)", color = TallyGaveRed, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Amount ($currency) *") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cash_amount_input")
                )

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val list = if (isIn) categoriesIn else categoriesOut
                    items(list) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Remarks / Note (Optional)") },
                    placeholder = { Text("e.g. Afternoon counter tally") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amount = amountText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { onConfirm(type, amount, category, note) },
                enabled = amount > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isIn) TallyGotGreen else TallyGaveRed
                )
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
