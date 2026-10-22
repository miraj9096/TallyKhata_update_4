package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer

@Composable
fun MainScreen(
    viewModel: TallyKhataViewModel,
    onSignOutClicked: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedCustomerId by viewModel.selectedCustomerId.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearUserMessage()
        }
    }

    val selectedCustomer = remember(selectedCustomerId, allCustomers) {
        allCustomers.firstOrNull { it.id == selectedCustomerId }
    }

    val isAddingCustomer by viewModel.isAddingCustomer.collectAsStateWithLifecycle()

    if (isAddingCustomer) {
        AddCustomerScreen(
            viewModel = viewModel,
            onBack = { viewModel.closeAddCustomerScreen() }
        )
    } else if (selectedCustomer != null) {
        CustomerDetailScreen(
            customer = selectedCustomer,
            viewModel = viewModel,
            onBack = { viewModel.selectCustomer(null) }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == MainTab.CUSTOMERS,
                        onClick = { viewModel.selectTab(MainTab.CUSTOMERS) },
                        icon = { Icon(Icons.Default.People, contentDescription = "Customers") },
                        label = { Text("Parties") },
                        modifier = Modifier.testTag("nav_tab_customers")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.DASHBOARD,
                        onClick = { viewModel.selectTab(MainTab.DASHBOARD) },
                        icon = { Icon(Icons.Default.Insights, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.CASHBOOK,
                        onClick = { viewModel.selectTab(MainTab.CASHBOOK) },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Cash Book") },
                        label = { Text("Cash Book") },
                        modifier = Modifier.testTag("nav_tab_cashbook")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.HISTORY,
                        onClick = { viewModel.selectTab(MainTab.HISTORY) },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("Timeline") },
                        modifier = Modifier.testTag("nav_tab_history")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.SETTINGS,
                        onClick = { viewModel.selectTab(MainTab.SETTINGS) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Store") },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    MainTab.CUSTOMERS -> {
                        CustomerListScreen(
                            viewModel = viewModel,
                            onCustomerSelected = { customer ->
                                viewModel.selectCustomer(customer)
                            }
                        )
                    }
                    MainTab.DASHBOARD -> {
                        FinancialDashboardScreen(
                            viewModel = viewModel,
                            onNavigateToCustomer = { customer ->
                                viewModel.selectCustomer(customer)
                            },
                            onNavigateToDebtors = {
                                viewModel.setCustomerFilter(CustomerFilter.YOU_GET)
                                viewModel.selectTab(MainTab.CUSTOMERS)
                            }
                        )
                    }
                    MainTab.CASHBOOK -> {
                        CashBookScreen(viewModel = viewModel)
                    }
                    MainTab.HISTORY -> {
                        HistoryScreen(
                            viewModel = viewModel,
                            onCustomerClick = { customerId ->
                                val customer = allCustomers.firstOrNull { it.id == customerId }
                                if (customer != null) {
                                    viewModel.selectCustomer(customer)
                                }
                            }
                        )
                    }
                    MainTab.SETTINGS -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onSignOutClicked = onSignOutClicked
                        )
                    }
                }
            }
        }
    }
}
