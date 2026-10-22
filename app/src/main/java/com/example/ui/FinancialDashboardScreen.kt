package com.example.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.export.CsvExporter
import com.example.data.model.Customer
import com.example.data.model.Transaction
import com.example.ui.theme.TallyAmber
import com.example.ui.theme.TallyAmberLight
import com.example.ui.theme.TallyGaveRed
import com.example.ui.theme.TallyGaveRedBg
import com.example.ui.theme.TallyGotGreen
import com.example.ui.theme.TallyGotGreenBg
import com.example.ui.theme.TallyNavy
import com.example.ui.theme.TallyNavyLight
import com.example.ui.theme.TallyTeal
import com.example.ui.theme.TallyTealLight
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2

enum class DashboardTimeframe(val title: String) {
    ALL_TIME("All Time"),
    THIS_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_WEEK("This Week")
}

@Composable
fun FinancialDashboardScreen(
    viewModel: TallyKhataViewModel,
    onNavigateToCustomer: (Customer) -> Unit,
    onNavigateToDebtors: () -> Unit
) {
    val context = LocalContext.current
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val transactionsState by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allTransactions = remember(transactionsState) {
        (transactionsState as? UiState.Success)?.data ?: emptyList()
    }
    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()

    var selectedTimeframe by remember { mutableStateOf(DashboardTimeframe.ALL_TIME) }
    var selectedChartSegment by remember { mutableIntStateOf(-1) } // -1: none, 0: Credit, 1: Debit

    // Filter transactions based on selected timeframe
    val filteredTransactions = remember(allTransactions, selectedTimeframe) {
        val now = Calendar.getInstance()
        when (selectedTimeframe) {
            DashboardTimeframe.ALL_TIME -> allTransactions
            DashboardTimeframe.THIS_MONTH -> {
                val startOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                allTransactions.filter { it.transactionDate.toDate().time >= startOfMonth }
            }
            DashboardTimeframe.LAST_30_DAYS -> {
                val thirtyDaysAgo = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                }.timeInMillis
                allTransactions.filter { it.transactionDate.toDate().time >= thirtyDaysAgo }
            }
            DashboardTimeframe.THIS_WEEK -> {
                val startOfWeek = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                allTransactions.filter { it.transactionDate.toDate().time >= startOfWeek }
            }
        }
    }

    // Financial calculations
    val totalCredits = remember(allCustomers, filteredTransactions, selectedTimeframe) {
        if (selectedTimeframe == DashboardTimeframe.ALL_TIME) {
            allCustomers.sumOf { it.totalGave }
        } else {
            filteredTransactions.filter { it.type == "GAVE" }.sumOf { it.amount }
        }
    }

    val totalDebits = remember(allCustomers, filteredTransactions, selectedTimeframe) {
        if (selectedTimeframe == DashboardTimeframe.ALL_TIME) {
            allCustomers.sumOf { it.totalGot }
        } else {
            filteredTransactions.filter { it.type == "GOT" }.sumOf { it.amount }
        }
    }

    // Net balance across all customers (Receivable is positive, Payable is negative)
    val netBalance = remember(totalCredits, totalDebits, allCustomers, selectedTimeframe) {
        if (selectedTimeframe == DashboardTimeframe.ALL_TIME) {
            allCustomers.sumOf { it.netBalance }
        } else {
            totalCredits - totalDebits
        }
    }

    val totalReceivableDues = remember(allCustomers) {
        allCustomers.filter { it.netBalance > 0.01 }.sumOf { it.netBalance }
    }
    val totalPayableAdvance = remember(allCustomers) {
        allCustomers.filter { it.netBalance < -0.01 }.sumOf { abs(it.netBalance) }
    }

    val customersWithDue = remember(allCustomers) {
        allCustomers.filter { it.netBalance > 0.01 }
    }
    val customersSettled = remember(allCustomers) {
        allCustomers.filter { abs(it.netBalance) <= 0.01 }
    }
    val customersInAdvance = remember(allCustomers) {
        allCustomers.filter { it.netBalance < -0.01 }
    }

    // Financial Health Score & Ratio
    val collectionRate = remember(totalCredits, totalDebits) {
        if (totalCredits > 0.0) {
            ((totalDebits / totalCredits) * 100.0).coerceIn(0.0, 100.0)
        } else if (totalDebits > 0.0) {
            100.0
        } else {
            100.0
        }
    }

    val topDebtors = remember(customersWithDue) {
        customersWithDue.sortedByDescending { it.netBalance }.take(5)
    }

    val currency = profile.currency.ifBlank { "৳" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. Dashboard Header
        item {
            DashboardHeader(
                storeName = profile.businessName,
                currency = currency,
                totalCustomers = allCustomers.size,
                selectedTimeframe = selectedTimeframe,
                onTimeframeChange = { selectedTimeframe = it },
                onExportClick = {
                    viewModel.exportAllCustomersCsv(context) { success, _, file ->
                        if (success && file != null) {
                            CsvExporter.shareCsvFile(context, file)
                        }
                    }
                }
            )
        }

        // 2. Financial Health Diagnostic Gauge
        item {
            FinancialHealthDiagnosticCard(
                collectionRate = collectionRate,
                netBalance = netBalance,
                totalReceivable = totalReceivableDues,
                totalPayable = totalPayableAdvance,
                currency = currency,
                debtorsCount = customersWithDue.size,
                settledCount = customersSettled.size,
                totalCustomers = allCustomers.size,
                onViewDebtors = onNavigateToDebtors
            )
        }

        // 3. Core KPI Summary Cards (Total Credits, Total Debits, Net Balance)
        item {
            KpiSummarySection(
                currency = currency,
                totalCredits = totalCredits,
                totalDebits = totalDebits,
                netBalance = netBalance,
                timeframe = selectedTimeframe,
                creditTxCount = if (selectedTimeframe == DashboardTimeframe.ALL_TIME)
                    allTransactions.count { it.type == "GAVE" }
                else
                    filteredTransactions.count { it.type == "GAVE" },
                debitTxCount = if (selectedTimeframe == DashboardTimeframe.ALL_TIME)
                    allTransactions.count { it.type == "GOT" }
                else
                    filteredTransactions.count { it.type == "GOT" }
            )
        }

        // 4. Interactive Donut Ring Chart: Credits Given vs Debits Received
        item {
            CreditVsDebitDonutCard(
                currency = currency,
                totalCredits = totalCredits,
                totalDebits = totalDebits,
                netBalance = netBalance,
                selectedSegment = selectedChartSegment,
                onSegmentSelected = { selectedChartSegment = it }
            )
        }

        // 5. Customer Balance & Risk Distribution Bar Chart
        item {
            CustomerBalanceDistributionCard(
                currency = currency,
                customers = allCustomers,
                onFilterClick = onNavigateToDebtors
            )
        }

        // 6. Cash Flow & Credit Activity Comparison (Recent Days/Activity)
        if (allTransactions.isNotEmpty()) {
            item {
                CreditDebitFlowTimelineCard(
                    currency = currency,
                    transactions = allTransactions
                )
            }
        }

        // 7. Top Debtors Risk Watchlist
        if (topDebtors.isNotEmpty()) {
            item {
                TopDebtorsRiskCard(
                    currency = currency,
                    topDebtors = topDebtors,
                    totalReceivable = totalReceivableDues,
                    onCustomerClick = onNavigateToCustomer,
                    onViewAllDebtors = onNavigateToDebtors
                )
            }
        }

        // 8. Quick Action Footer
        item {
            DashboardActionFooter(
                onExportReport = {
                    viewModel.exportAllCustomersCsv(context) { success, _, file ->
                        if (success && file != null) {
                            CsvExporter.shareCsvFile(context, file)
                        }
                    }
                },
                onViewDebtors = onNavigateToDebtors
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Dashboard Header with Timeframe selector
// -----------------------------------------------------------------------------
@Composable
private fun DashboardHeader(
    storeName: String,
    currency: String,
    totalCustomers: Int,
    selectedTimeframe: DashboardTimeframe,
    onTimeframeChange: (DashboardTimeframe) -> Unit,
    onExportClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TallyNavy),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Financial Dashboard",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (storeName.isNotBlank()) storeName else "Business Health & Ledger Insights",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Export Report Button
                OutlinedButton(
                    onClick = onExportClick,
                    modifier = Modifier.testTag("dashboard_export_btn"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Export CSV",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Report", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timeframe selection chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DashboardTimeframe.values()) { timeframe ->
                    val isSelected = timeframe == selectedTimeframe
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTimeframeChange(timeframe) },
                        label = {
                            Text(
                                text = timeframe.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("timeframe_chip_${timeframe.name.lowercase()}")
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Financial Health Diagnostic Card (Score + Recommendations)
// -----------------------------------------------------------------------------
@Composable
private fun FinancialHealthDiagnosticCard(
    collectionRate: Double,
    netBalance: Double,
    totalReceivable: Double,
    totalPayable: Double,
    currency: String,
    debtorsCount: Int,
    settledCount: Int,
    totalCustomers: Int,
    onViewDebtors: () -> Unit
) {
    val isGoodHealth = collectionRate >= 70.0 || (totalReceivable <= 0.01 && totalCustomers > 0)
    val isModerate = collectionRate in 45.0..69.99

    val healthTitle = when {
        totalCustomers == 0 -> "Welcome to Tally Khata"
        isGoodHealth -> "Healthy Financial Condition"
        isModerate -> "Moderate Credit Exposure"
        else -> "High Credit Risk Alert"
    }

    val healthBadgeColor = when {
        totalCustomers == 0 -> MaterialTheme.colorScheme.primary
        isGoodHealth -> TallyGotGreen
        isModerate -> TallyAmber
        else -> TallyGaveRed
    }

    val healthBadgeBg = when {
        totalCustomers == 0 -> MaterialTheme.colorScheme.primaryContainer
        isGoodHealth -> TallyGotGreenBg
        isModerate -> TallyAmberLight
        else -> TallyGaveRedBg
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("financial_health_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(healthBadgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isGoodHealth -> Icons.Default.CheckCircle
                                isModerate -> Icons.Default.Info
                                else -> Icons.Default.Warning
                            },
                            contentDescription = null,
                            tint = healthBadgeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = healthTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Collection Efficiency: ${String.format(Locale.US, "%.1f", collectionRate)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Health Badge Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = healthBadgeBg
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.0f", collectionRate)}/100",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = healthBadgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (collectionRate / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = healthBadgeColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Insights text
            Text(
                text = when {
                    totalCustomers == 0 ->
                        "Start by adding customers to record credit and cash transactions to view live analytics."
                    isGoodHealth ->
                        "Excellent recovery! Business is collecting over 70% of credit. $debtorsCount parties have pending dues ($currency${String.format(Locale.US, "%,.2f", totalReceivable)})."
                    isModerate ->
                        "Balanced cash flow. Consider sending reminders to $debtorsCount customers with outstanding balances."
                    else ->
                        "Caution: More than half of credit extended remains uncollected ($currency${String.format(Locale.US, "%,.2f", totalReceivable)}). Prioritize collection."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (debtorsCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Review $debtorsCount Debtors →",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(onClick = onViewDebtors)
                            .testTag("review_debtors_link")
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: 3 Key Financial Metric KPI Cards
// -----------------------------------------------------------------------------
@Composable
private fun KpiSummarySection(
    currency: String,
    totalCredits: Double,
    totalDebits: Double,
    netBalance: Double,
    timeframe: DashboardTimeframe,
    creditTxCount: Int,
    debitTxCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = "FINANCIAL TOTALS (${timeframe.title.uppercase()})",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Row of Credits & Debits
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Total Credits Given (Gave)
            ElevatedCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_total_credits"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = TallyGaveRedBg
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Credit",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TallyGaveRed
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(TallyGaveRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = TallyGaveRed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "$currency${String.format(Locale.US, "%,.2f", totalCredits)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TallyGaveRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$creditTxCount entries (Given)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TallyGaveRed.copy(alpha = 0.8f)
                    )
                }
            }

            // Total Debits Collected (Got)
            ElevatedCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_total_debits"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = TallyGotGreenBg
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Received",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TallyGotGreen
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(TallyGotGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = TallyGotGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "$currency${String.format(Locale.US, "%,.2f", totalDebits)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TallyGotGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$debitTxCount entries (Got)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TallyGotGreen.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Net Balance Card
        val isNetReceivable = netBalance > 0.01
        val isNetPayable = netBalance < -0.01

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("kpi_net_balance"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = when {
                    isNetReceivable -> TallyNavyLight
                    isNetPayable -> TallyAmberLight
                    else -> TallyGotGreenBg
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isNetReceivable -> "Net Balance (You Will Get)"
                            isNetPayable -> "Net Advance (You Will Give)"
                            else -> "Net Balance (Fully Settled)"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = when {
                            isNetReceivable -> TallyNavy
                            isNetPayable -> TallyAmber
                            else -> TallyGotGreen
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            isNetReceivable -> "+$currency${String.format(Locale.US, "%,.2f", netBalance)}"
                            isNetPayable -> "-$currency${String.format(Locale.US, "%,.2f", abs(netBalance))}"
                            else -> "$currency 0.00"
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = when {
                            isNetReceivable -> TallyNavy
                            isNetPayable -> TallyAmber
                            else -> TallyGotGreen
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isNetReceivable -> TallyNavy.copy(alpha = 0.15f)
                                isNetPayable -> TallyAmber.copy(alpha = 0.15f)
                                else -> TallyGotGreen.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isNetReceivable -> Icons.Default.TrendingUp
                            isNetPayable -> Icons.Default.TrendingDown
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = when {
                            isNetReceivable -> TallyNavy
                            isNetPayable -> TallyAmber
                            else -> TallyGotGreen
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Recharts-Style Interactive Donut Chart (Credit vs Debit)
// -----------------------------------------------------------------------------
@Composable
private fun CreditVsDebitDonutCard(
    currency: String,
    totalCredits: Double,
    totalDebits: Double,
    netBalance: Double,
    selectedSegment: Int,
    onSegmentSelected: (Int) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("donut_chart_card"),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Credit vs Debit Ratio",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Tap slice to inspect",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val totalVolume = totalCredits + totalDebits
            val creditRatio = if (totalVolume > 0) (totalCredits / totalVolume).toFloat() else 0.5f
            val debitRatio = if (totalVolume > 0) (totalDebits / totalVolume).toFloat() else 0.5f

            // Donut Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(190.dp)
                        .testTag("donut_chart_canvas")
                        .pointerInput(totalVolume) {
                            detectTapGestures { offset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = offset.x - center.x
                                val dy = offset.y - center.y
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f

                                // Starts from -90 deg (top)
                                val normalizedAngle = (angle + 90f) % 360f
                                val creditSweep = creditRatio * 360f

                                if (normalizedAngle <= creditSweep) {
                                    onSegmentSelected(0) // Credit
                                } else {
                                    onSegmentSelected(1) // Debit
                                }
                            }
                        }
                ) {
                    val strokeWidth = 28.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                    if (totalVolume <= 0.0) {
                        // Empty ring placeholder
                        drawArc(
                            color = Color(0xFFE2E8F0),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth)
                        )
                    } else {
                        val creditSweep = (creditRatio * 360f)
                        val debitSweep = (debitRatio * 360f)

                        // 1. Credit Arc (Red)
                        val isCreditHighlighted = selectedSegment == 0
                        drawArc(
                            color = if (isCreditHighlighted) TallyGaveRed else TallyGaveRed.copy(alpha = 0.85f),
                            startAngle = -90f,
                            sweepAngle = creditSweep.coerceAtLeast(4f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = if (isCreditHighlighted) strokeWidth * 1.15f else strokeWidth,
                                cap = StrokeCap.Butt
                            )
                        )

                        // 2. Debit Arc (Green)
                        val isDebitHighlighted = selectedSegment == 1
                        drawArc(
                            color = if (isDebitHighlighted) TallyGotGreen else TallyGotGreen.copy(alpha = 0.85f),
                            startAngle = -90f + creditSweep,
                            sweepAngle = debitSweep.coerceAtLeast(4f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = if (isDebitHighlighted) strokeWidth * 1.15f else strokeWidth,
                                cap = StrokeCap.Butt
                            )
                        )
                    }
                }

                // Inner Center Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = when (selectedSegment) {
                            0 -> "Credit Given"
                            1 -> "Debit Got"
                            else -> "Net Balance"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = when (selectedSegment) {
                            0 -> TallyGaveRed
                            1 -> TallyGotGreen
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when (selectedSegment) {
                            0 -> "$currency${String.format(Locale.US, "%,.0f", totalCredits)}"
                            1 -> "$currency${String.format(Locale.US, "%,.0f", totalDebits)}"
                            else -> {
                                if (netBalance >= 0)
                                    "$currency${String.format(Locale.US, "%,.0f", netBalance)}"
                                else
                                    "-$currency${String.format(Locale.US, "%,.0f", abs(netBalance))}"
                            }
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = when (selectedSegment) {
                            0 -> TallyGaveRed
                            1 -> TallyGotGreen
                            else -> if (netBalance >= 0) TallyNavy else TallyAmber
                        }
                    )
                    Text(
                        text = when (selectedSegment) {
                            0 -> "${String.format(Locale.US, "%.1f", creditRatio * 100)}% of total"
                            1 -> "${String.format(Locale.US, "%.1f", debitRatio * 100)}% of total"
                            else -> if (netBalance > 0) "Receivable" else if (netBalance < 0) "Payable" else "Settled"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart Interactive Legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Legend 1: Credit
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSegmentSelected(if (selectedSegment == 0) -1 else 0) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(TallyGaveRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Credits (Given)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$currency${String.format(Locale.US, "%,.2f", totalCredits)} (${String.format(Locale.US, "%.1f", creditRatio * 100)}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TallyGaveRed
                        )
                    }
                }

                // Legend 2: Debit
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSegmentSelected(if (selectedSegment == 1) -1 else 1) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(TallyGotGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Debits (Received)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$currency${String.format(Locale.US, "%,.2f", totalDebits)} (${String.format(Locale.US, "%.1f", debitRatio * 100)}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TallyGotGreen
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Customer Balance Distribution Bar Chart (Recharts-style breakdown)
// -----------------------------------------------------------------------------
@Composable
private fun CustomerBalanceDistributionCard(
    currency: String,
    customers: List<Customer>,
    onFilterClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("balance_distribution_card"),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Customer Balance Segments",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${customers.size} Parties",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Distribution of customer dues across credit risk brackets",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Buckets
            val highDueCustomers = remember(customers) { customers.filter { it.netBalance >= 5000.0 } }
            val mediumDueCustomers = remember(customers) { customers.filter { it.netBalance in 1000.0..4999.99 } }
            val lowDueCustomers = remember(customers) { customers.filter { it.netBalance in 0.01..999.99 } }
            val settledCustomers = remember(customers) { customers.filter { abs(it.netBalance) <= 0.01 } }
            val advanceCustomers = remember(customers) { customers.filter { it.netBalance < -0.01 } }

            val totalCount = customers.size.coerceAtLeast(1)

            DistributionBarRow(
                label = "High Due (≥ $currency 5,000)",
                count = highDueCustomers.size,
                totalAmount = highDueCustomers.sumOf { it.netBalance },
                fraction = highDueCustomers.size.toFloat() / totalCount,
                barColor = Color(0xFFDC2626), // Strong Red
                currency = currency
            )

            Spacer(modifier = Modifier.height(10.dp))

            DistributionBarRow(
                label = "Moderate Due ($currency 1,000 - 5,000)",
                count = mediumDueCustomers.size,
                totalAmount = mediumDueCustomers.sumOf { it.netBalance },
                fraction = mediumDueCustomers.size.toFloat() / totalCount,
                barColor = Color(0xFFF59E0B), // Amber
                currency = currency
            )

            Spacer(modifier = Modifier.height(10.dp))

            DistributionBarRow(
                label = "Low Due (< $currency 1,000)",
                count = lowDueCustomers.size,
                totalAmount = lowDueCustomers.sumOf { it.netBalance },
                fraction = lowDueCustomers.size.toFloat() / totalCount,
                barColor = Color(0xFF3B82F6), // Blue
                currency = currency
            )

            Spacer(modifier = Modifier.height(10.dp))

            DistributionBarRow(
                label = "Zero Balance (Settled)",
                count = settledCustomers.size,
                totalAmount = 0.0,
                fraction = settledCustomers.size.toFloat() / totalCount,
                barColor = Color(0xFF10B981), // Green
                currency = currency
            )

            Spacer(modifier = Modifier.height(10.dp))

            DistributionBarRow(
                label = "In Advance (Customer Credit)",
                count = advanceCustomers.size,
                totalAmount = advanceCustomers.sumOf { abs(it.netBalance) },
                fraction = advanceCustomers.size.toFloat() / totalCount,
                barColor = Color(0xFF8B5CF6), // Purple
                currency = currency
            )
        }
    }
}

@Composable
private fun DistributionBarRow(
    label: String,
    count: Int,
    totalAmount: Double,
    fraction: Float,
    barColor: Color,
    currency: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$count parties",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = barColor
                )
                if (totalAmount > 0.0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "($currency${String.format(Locale.US, "%,.0f", totalAmount)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Animated Bar Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(barColor.copy(alpha = 0.7f), barColor)
                        )
                    )
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Credit vs Debit Recent Cashflow Timeline Comparison
// -----------------------------------------------------------------------------
@Composable
private fun CreditDebitFlowTimelineCard(
    currency: String,
    transactions: List<Transaction>
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("cashflow_timeline_card"),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recent Cash Flow Comparison",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = "Dual-bar comparison: Extended credit (Red) vs Collected cash (Green)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Group recent 5 days
            val recentDays = remember(transactions) {
                val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                val calendar = Calendar.getInstance()
                val list = mutableListOf<String>()
                for (i in 4 downTo 0) {
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                    list.add(dateFormat.format(cal.time))
                }
                list
            }

            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            val dailyCredits = remember(transactions, recentDays) {
                recentDays.associateWith { day ->
                    transactions.filter {
                        it.type == "GAVE" && dateFormat.format(it.transactionDate.toDate()) == day
                    }.sumOf { it.amount }
                }
            }
            val dailyDebits = remember(transactions, recentDays) {
                recentDays.associateWith { day ->
                    transactions.filter {
                        it.type == "GOT" && dateFormat.format(it.transactionDate.toDate()) == day
                    }.sumOf { it.amount }
                }
            }

            val maxDailyAmount = remember(dailyCredits, dailyDebits) {
                maxOf(
                    dailyCredits.values.maxOrNull() ?: 1.0,
                    dailyDebits.values.maxOrNull() ?: 1.0,
                    1.0
                )
            }

            // Dual Bar Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                recentDays.forEach { day ->
                    val credit = dailyCredits[day] ?: 0.0
                    val debit = dailyDebits[day] ?: 0.0

                    val creditHeightFraction = (credit / maxDailyAmount).toFloat().coerceIn(0.05f, 1f)
                    val debitHeightFraction = (debit / maxDailyAmount).toFloat().coerceIn(0.05f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier
                                .height(100.dp)
                                .fillMaxWidth()
                        ) {
                            // Credit bar (Red)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(creditHeightFraction)
                                    .height((100 * creditHeightFraction).dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (credit > 0) TallyGaveRed else Color(0xFFE2E8F0))
                            )

                            // Debit bar (Green)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(debitHeightFraction)
                                    .height((100 * debitHeightFraction).dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (debit > 0) TallyGotGreen else Color(0xFFE2E8F0))
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(TallyGaveRed))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Credit Given", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(TallyGotGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Payment Got", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Top Debtors Risk Watchlist Card
// -----------------------------------------------------------------------------
@Composable
private fun TopDebtorsRiskCard(
    currency: String,
    topDebtors: List<Customer>,
    totalReceivable: Double,
    onCustomerClick: (Customer) -> Unit,
    onViewAllDebtors: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("top_debtors_card"),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = TallyGaveRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Highest Outstanding Parties",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onViewAllDebtors)
                )
            }

            Text(
                text = "Parties holding the largest share of unpaid store credit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            topDebtors.forEachIndexed { index, customer ->
                val sharePercent = if (totalReceivable > 0) {
                    (customer.netBalance / totalReceivable) * 100.0
                } else {
                    0.0
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onCustomerClick(customer) }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .testTag("top_debtor_row_$index"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank badge
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == 0) TallyGaveRedBg else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${index + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (index == 0) TallyGaveRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = customer.name.ifBlank { "Unnamed Party" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (customer.phone.isNotBlank()) customer.phone else "No phone",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${String.format(Locale.US, "%.1f", sharePercent)}% of dues",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = TallyGaveRed
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$currency${String.format(Locale.US, "%,.2f", customer.netBalance)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TallyGaveRed
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View party ledger",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (index < topDebtors.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Dashboard Action Footer
// -----------------------------------------------------------------------------
@Composable
private fun DashboardActionFooter(
    onExportReport: () -> Unit,
    onViewDebtors: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onExportReport,
                modifier = Modifier
                    .weight(1f)
                    .testTag("footer_export_report_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export CSV")
            }

            Button(
                onClick = onViewDebtors,
                modifier = Modifier
                    .weight(1f)
                    .testTag("footer_view_debtors_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("View Debtors")
            }
        }
    }
}
