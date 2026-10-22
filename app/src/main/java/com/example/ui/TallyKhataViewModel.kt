package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupFileInfo
import com.example.data.backup.BackupManager
import com.example.data.export.CsvExporter
import com.example.data.model.BusinessProfile
import com.example.data.model.CashEntry
import com.example.data.model.Customer
import com.example.data.model.Transaction
import com.example.data.repository.TallyKhataRepository
import java.io.File
import com.example.data.sync.MySqlConnectionInfo
import com.example.data.sync.MySqlScriptHelper
import com.example.data.sync.NetworkMonitor
import com.example.data.sync.WebsiteSyncConfig
import com.example.data.sync.WebsiteSyncConfigManager
import com.example.data.sync.WebsiteSyncManager
import com.example.notification.BackupReminderHelper
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class CustomerFilter {
    ALL,
    YOU_GET,  // netBalance > 0 (debtors)
    YOU_GIVE, // netBalance < 0 (creditors/advance)
    SETTLED   // netBalance == 0
}

enum class MainTab {
    CUSTOMERS,
    DASHBOARD,
    CASHBOOK,
    HISTORY,
    SETTINGS
}

class TallyKhataViewModel(
    private val repository: TallyKhataRepository,
    context: Context? = null
) : ViewModel() {

    private val networkMonitor = context?.let { NetworkMonitor(it) }
    private val configManager = context?.let { WebsiteSyncConfigManager(it) }
    private val syncManager = WebsiteSyncManager()
    private val backupManager = context?.let { BackupManager(it) }

    val isOnline: StateFlow<Boolean> = networkMonitor?.isOnlineFlow?.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = networkMonitor.isCurrentlyConnected()
    ) ?: MutableStateFlow(true)

    val websiteConfig: StateFlow<WebsiteSyncConfig> = configManager?.configFlow
        ?: MutableStateFlow(WebsiteSyncConfig())

    private val _dbConnectionInfo = MutableStateFlow<MySqlConnectionInfo?>(null)
    val dbConnectionInfo: StateFlow<MySqlConnectionInfo?> = _dbConnectionInfo.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _localBackups = MutableStateFlow<List<BackupFileInfo>>(emptyList())
    val localBackups: StateFlow<List<BackupFileInfo>> = _localBackups.asStateFlow()

    private val _currentTab = MutableStateFlow(MainTab.CUSTOMERS)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _selectedCustomerId = MutableStateFlow<String?>(null)
    val selectedCustomerId: StateFlow<String?> = _selectedCustomerId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _transactionSearchQuery = MutableStateFlow("")
    val transactionSearchQuery: StateFlow<String> = _transactionSearchQuery.asStateFlow()

    private val _customerFilter = MutableStateFlow(CustomerFilter.ALL)
    val customerFilter: StateFlow<CustomerFilter> = _customerFilter.asStateFlow()

    private val _isAddingCustomer = MutableStateFlow(false)
    val isAddingCustomer: StateFlow<Boolean> = _isAddingCustomer.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        refreshLocalBackups()
        checkInitialMySqlConnection()
    }

    private fun checkInitialMySqlConnection() {
        val config = websiteConfig.value
        if (config.websiteUrl.isNotBlank()) {
            viewModelScope.launch {
                val res = syncManager.testConnectionDetails(config.websiteUrl, config.apiKey, config.storeId)
                if (res.isSuccess) {
                    _dbConnectionInfo.value = res.getOrNull()
                }
            }
        }
    }

    fun openAddCustomerScreen() {
        _isAddingCustomer.value = true
    }

    fun closeAddCustomerScreen() {
        _isAddingCustomer.value = false
    }

    // Customers stream
    val rawCustomers: StateFlow<UiState<List<Customer>>> = repository.observeCustomers()
        .combine(_searchQuery) { list, query -> Pair(list, query) }
        .combine(_customerFilter) { (list, query), filter ->
            filterCustomers(list, query, filter)
        }
        .combine(MutableStateFlow(Unit)) { filteredList, _ ->
            UiState.Success(filteredList) as UiState<List<Customer>>
        }
        .catch { error ->
            emit(UiState.Error(error.localizedMessage ?: "Failed to load customers"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // Unfiltered customers for summary totals
    val allCustomers: StateFlow<List<Customer>> = repository.observeCustomers()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // All transactions for live history
    val allTransactions: StateFlow<UiState<List<Transaction>>> = repository.observeAllTransactions()
        .combine(MutableStateFlow(Unit)) { list, _ ->
            UiState.Success(list) as UiState<List<Transaction>>
        }
        .catch { error ->
            emit(UiState.Error(error.localizedMessage ?: "Failed to load transactions"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // Daily Cashbook stream
    val cashbookEntries: StateFlow<UiState<List<CashEntry>>> = repository.observeCashbook()
        .combine(MutableStateFlow(Unit)) { list, _ ->
            UiState.Success(list) as UiState<List<CashEntry>>
        }
        .catch { error ->
            emit(UiState.Error(error.localizedMessage ?: "Failed to load cashbook"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // Business profile stream
    val businessProfile: StateFlow<BusinessProfile> = repository.observeProfile()
        .combine(MutableStateFlow(Unit)) { profile, _ ->
            profile ?: BusinessProfile()
        }
        .catch { emit(BusinessProfile()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = BusinessProfile()
        )

    // Transactions for selected customer
    private val _selectedCustomerTransactions = MutableStateFlow<UiState<List<Transaction>>>(UiState.Loading)
    val selectedCustomerTransactions: StateFlow<UiState<List<Transaction>>> = _selectedCustomerTransactions.asStateFlow()

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun selectCustomer(customer: Customer?) {
        _selectedCustomerId.value = customer?.id
        _transactionSearchQuery.value = ""
        if (customer != null && customer.id.isNotBlank()) {
            observeSelectedCustomer(customer.id)
        }
    }

    private fun observeSelectedCustomer(customerId: String) {
        viewModelScope.launch {
            _selectedCustomerTransactions.value = UiState.Loading
            repository.observeTransactionsForCustomer(customerId)
                .catch { error ->
                    _selectedCustomerTransactions.value = UiState.Error(error.localizedMessage ?: "Error loading ledger")
                }
                .collect { list ->
                    _selectedCustomerTransactions.value = UiState.Success(list)
                }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTransactionSearchQuery(query: String) {
        _transactionSearchQuery.value = query
    }

    fun setCustomerFilter(filter: CustomerFilter) {
        _customerFilter.value = filter
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    /**
     * Filters a list of customers by search query (name, phone, address, balance)
     * and status filter (ALL, YOU_GET, YOU_GIVE, SETTLED).
     */
    fun filterCustomers(
        customers: List<Customer>,
        query: String,
        filter: CustomerFilter = CustomerFilter.ALL
    ): List<Customer> {
        val trimmed = query.trim()
        val listMatchingQuery = if (trimmed.isBlank()) customers else {
            customers.filter { c ->
                c.name.contains(trimmed, ignoreCase = true) ||
                c.phone.contains(trimmed, ignoreCase = true) ||
                c.address.contains(trimmed, ignoreCase = true) ||
                String.format(java.util.Locale.getDefault(), "%.0f", c.netBalance).contains(trimmed) ||
                String.format(java.util.Locale.getDefault(), "%.2f", c.netBalance).contains(trimmed)
            }
        }
        return when (filter) {
            CustomerFilter.ALL -> listMatchingQuery
            CustomerFilter.YOU_GET -> listMatchingQuery.filter { it.netBalance > 0.01 }
            CustomerFilter.YOU_GIVE -> listMatchingQuery.filter { it.netBalance < -0.01 }
            CustomerFilter.SETTLED -> listMatchingQuery.filter { kotlin.math.abs(it.netBalance) <= 0.01 }
        }
    }

    /**
     * Filters a list of transactions by search query (note, billNumber, customerName, amount, date)
     * and type filter ("ALL", "GAVE", "GOT").
     */
    fun filterTransactions(
        transactions: List<Transaction>,
        query: String,
        typeFilter: String = "ALL"
    ): List<Transaction> {
        val trimmed = query.trim()
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        return transactions.filter { tx ->
            val matchesType = when (typeFilter) {
                "GAVE" -> tx.type == "GAVE"
                "GOT" -> tx.type == "GOT"
                else -> true
            }
            val matchesQuery = if (trimmed.isBlank()) true else {
                tx.customerName.contains(trimmed, ignoreCase = true) ||
                tx.note.contains(trimmed, ignoreCase = true) ||
                tx.billNumber.contains(trimmed, ignoreCase = true) ||
                tx.amount.toString().contains(trimmed) ||
                String.format(java.util.Locale.getDefault(), "%,.0f", tx.amount).contains(trimmed) ||
                String.format(java.util.Locale.getDefault(), "%,.2f", tx.amount).contains(trimmed) ||
                sdf.format(tx.transactionDate.toDate()).contains(trimmed, ignoreCase = true)
            }
            matchesType && matchesQuery
        }
    }

    // --- Actions ---

    fun addCustomer(
        name: String,
        phone: String,
        address: String,
        type: String,
        openingBalance: Double = 0.0,
        openingType: String = "NONE",
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (name.isBlank()) {
            _userMessage.value = "Customer name is required"
            onComplete(false)
            return
        }
        viewModelScope.launch {
            val totalGave = if (openingType == "GAVE") openingBalance else 0.0
            val totalGot = if (openingType == "GOT") openingBalance else 0.0
            val netBalance = totalGave - totalGot

            val customer = Customer(
                name = name.trim(),
                phone = phone.trim(),
                address = address.trim(),
                type = type,
                totalGave = totalGave,
                totalGot = totalGot,
                netBalance = netBalance
            )
            val result = repository.createCustomer(customer)
            if (result.isSuccess) {
                val customerId = result.getOrNull()
                if (openingBalance > 0.0 && !customerId.isNullOrBlank() && openingType != "NONE") {
                    val initialTx = Transaction(
                        customerId = customerId,
                        customerName = customer.name,
                        type = openingType,
                        amount = openingBalance,
                        note = "Opening Balance",
                        transactionDate = Timestamp.now()
                    )
                    repository.recordTransaction(initialTx, customer.copy(id = customerId))
                }
                _userMessage.value = "Added party: $name"
                _isAddingCustomer.value = false
                onComplete(true)
            } else {
                _userMessage.value = "Failed to add party: ${result.exceptionOrNull()?.message}"
                onComplete(false)
            }
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            val result = repository.updateCustomer(customer)
            if (result.isSuccess) {
                _userMessage.value = "Updated party details"
            } else {
                _userMessage.value = "Failed to update: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            val result = repository.deleteCustomer(customer.id)
            if (result.isSuccess) {
                _userMessage.value = "Deleted party: ${customer.name}"
                _selectedCustomerId.value = null
            } else {
                _userMessage.value = "Failed to delete: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun recordTransaction(
        customer: Customer,
        type: String, // "GAVE" or "GOT"
        amount: Double,
        note: String,
        billNumber: String
    ) {
        if (amount <= 0.0) {
            _userMessage.value = "Amount must be greater than 0"
            return
        }
        viewModelScope.launch {
            val transaction = Transaction(
                customerId = customer.id,
                customerName = customer.name,
                type = type,
                amount = amount,
                note = note.trim(),
                billNumber = billNumber.trim(),
                transactionDate = Timestamp.now()
            )
            val result = repository.recordTransaction(transaction, customer)
            if (result.isSuccess) {
                val action = if (type == "GAVE") "Credit entry (+You Gave)" else "Payment received (+You Got)"
                _userMessage.value = "$action of $amount recorded"
            } else {
                _userMessage.value = "Transaction failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteTransaction(transaction: Transaction, customer: Customer) {
        viewModelScope.launch {
            val result = repository.deleteTransaction(transaction, customer)
            if (result.isSuccess) {
                _userMessage.value = "Transaction removed and balance recalculated"
            } else {
                _userMessage.value = "Failed to delete transaction: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun addCashEntry(type: String, amount: Double, category: String, note: String) {
        if (amount <= 0.0) {
            _userMessage.value = "Amount must be greater than 0"
            return
        }
        viewModelScope.launch {
            val entry = CashEntry(
                type = type,
                amount = amount,
                category = category.ifBlank { "General" },
                note = note.trim(),
                entryDate = Timestamp.now()
            )
            val result = repository.addCashEntry(entry)
            if (result.isSuccess) {
                val label = if (type == "CASH_IN") "Cash In" else "Cash Out"
                _userMessage.value = "$label of $amount recorded"
            } else {
                _userMessage.value = "Failed to record cash: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteCashEntry(entryId: String) {
        viewModelScope.launch {
            val result = repository.deleteCashEntry(entryId)
            if (result.isSuccess) {
                _userMessage.value = "Cash entry deleted"
            } else {
                _userMessage.value = "Failed to delete: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun updateProfile(businessName: String, ownerName: String, phone: String, email: String, currency: String) {
        viewModelScope.launch {
            val current = businessProfile.value
            val updated = current.copy(
                businessName = businessName.trim().ifBlank { "My Store" },
                ownerName = ownerName.trim(),
                phone = phone.trim(),
                email = email.trim(),
                currency = currency.trim().ifBlank { "$" }
            )
            val result = repository.saveProfile(updated)
            if (result.isSuccess) {
                _userMessage.value = "Store profile updated"
            } else {
                _userMessage.value = "Failed to update profile: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun syncRegistrationDetails(defaultName: String?, defaultPhone: String?, defaultEmail: String?) {
        viewModelScope.launch {
            val current = businessProfile.value
            if (current.businessName == "My Store" && (defaultName?.isNotBlank() == true || defaultPhone?.isNotBlank() == true || defaultEmail?.isNotBlank() == true)) {
                val updated = current.copy(
                    businessName = defaultName?.ifBlank { "My Store" } ?: current.businessName,
                    phone = defaultPhone?.ifBlank { current.phone } ?: current.phone,
                    email = defaultEmail?.ifBlank { current.email } ?: current.email
                )
                repository.saveProfile(updated)
            }
        }
    }

    // --- Website MySQL Synchronization & Offline Backup ---

    fun refreshLocalBackups() {
        viewModelScope.launch {
            backupManager?.let { bm ->
                _localBackups.value = bm.listLocalBackups()
            }
        }
    }

    fun saveWebsiteConfig(
        websiteUrl: String,
        apiKey: String,
        storeId: String,
        autoSync: Boolean,
        useAsPrimary: Boolean? = null
    ) {
        configManager?.saveConfig(websiteUrl, apiKey, storeId, autoSync, useAsPrimary)
        _userMessage.value = "Website MySQL configuration saved"
        if (websiteUrl.isNotBlank()) {
            viewModelScope.launch {
                val res = syncManager.testConnectionDetails(websiteUrl, apiKey, storeId)
                if (res.isSuccess) {
                    val info = res.getOrNull()!!
                    _dbConnectionInfo.value = info
                    configManager?.updatePingInfo(info.databaseName, "${info.customerCount} customers, ${info.transactionCount} tx")
                }
            }
        }
    }

    fun setUseWebsiteMySqlAsPrimary(enabled: Boolean) {
        configManager?.setUseWebsiteMySqlAsPrimary(enabled)
        _userMessage.value = if (enabled) {
            "Website phpMyAdmin / MySQL set as Primary Database"
        } else {
            "Switched to Device-Local Offline Database"
        }
        if (enabled && websiteConfig.value.websiteUrl.isNotBlank()) {
            syncWithMySqlNow()
        }
    }

    fun testWebsiteConnectionDetailed(
        websiteUrl: String,
        apiKey: String,
        storeId: String,
        onResult: (Boolean, MySqlConnectionInfo?, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = syncManager.testConnectionDetails(websiteUrl, apiKey, storeId)
            if (result.isSuccess) {
                val info = result.getOrNull()!!
                _dbConnectionInfo.value = info
                configManager?.updatePingInfo(info.databaseName, "${info.customerCount} customers, ${info.transactionCount} transactions")
                onResult(true, info, info.message)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to connect to MySQL backend"
                onResult(false, null, err)
            }
        }
    }

    fun testWebsiteConnection(
        websiteUrl: String,
        apiKey: String,
        storeId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        testWebsiteConnectionDetailed(websiteUrl, apiKey, storeId) { success, _, message ->
            onResult(success, message)
        }
    }

    fun getPhpMyAdminSetupSql(): String {
        return MySqlScriptHelper.generatePhpMyAdminImportSql(websiteConfig.value.storeId)
    }

    /**
     * Bidirectional live sync: pushes any local changes, then pulls latest data from MySQL/phpMyAdmin.
     */
    fun syncWithMySqlNow(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val config = websiteConfig.value
        if (config.websiteUrl.isBlank()) {
            _userMessage.value = "Website URL is not configured. Please set it in Settings."
            onComplete(false, "Website URL is not configured")
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            configManager?.updateSyncStatus("SYNCING", "Syncing with website phpMyAdmin / MySQL...")

            val customers = repository.getAllCustomersOnce()
            val transactions = repository.getAllTransactionsOnce()
            val cashEntries = repository.getAllCashEntriesOnce()
            val profile = repository.getBusinessProfileOnce()

            // 1. Push local changes
            val pushRes = syncManager.pushOfflineDataToWebsite(
                config = config,
                customers = customers,
                transactions = transactions,
                cashEntries = cashEntries,
                profile = profile
            )

            // 2. Pull remote updates to ensure local cache is identical to MySQL
            val pullRes = syncManager.pullDataFromWebsite(config)

            _isSyncing.value = false

            if (pullRes.isSuccess) {
                val pullData = pullRes.getOrNull()!!
                repository.restoreCompleteData(
                    customers = pullData.customers,
                    transactions = pullData.transactions,
                    cashEntries = pullData.cashbook,
                    profile = pullData.profile
                )
                val msg = "Synced with MySQL (${pullData.customers.size} parties, ${pullData.transactions.size} transactions)"
                configManager?.updateSyncStatus("SUCCESS", msg)
                _userMessage.value = msg
                onComplete(true, msg)
            } else if (pushRes.isSuccess) {
                val msg = "Pushed data to website MySQL database"
                configManager?.updateSyncStatus("SUCCESS", msg)
                _userMessage.value = msg
                onComplete(true, msg)
            } else {
                val err = pushRes.exceptionOrNull()?.message ?: pullRes.exceptionOrNull()?.message ?: "Sync failed"
                configManager?.updateSyncStatus("FAILED", err)
                _userMessage.value = "MySQL Error: $err"
                onComplete(false, err)
            }
        }
    }

    /**
     * Synchronizes offline Room SQLite ledger data to the website MySQL database.
     */
    fun syncFromOffline(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val config = websiteConfig.value
        if (config.websiteUrl.isBlank()) {
            _userMessage.value = "Website URL is not configured. Please set it in Settings."
            onComplete(false, "Website URL is not configured")
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            configManager?.updateSyncStatus("SYNCING", "Synchronizing offline data to website MySQL...")

            val customers = repository.getAllCustomersOnce()
            val transactions = repository.getAllTransactionsOnce()
            val cashEntries = repository.getAllCashEntriesOnce()
            val profile = repository.getBusinessProfileOnce()

            val result = syncManager.pushOfflineDataToWebsite(
                config = config,
                customers = customers,
                transactions = transactions,
                cashEntries = cashEntries,
                profile = profile
            )

            _isSyncing.value = false
            if (result.isSuccess) {
                val pushRes = result.getOrNull()!!
                val msg = "Synced ${pushRes.syncedCustomers} parties, ${pushRes.syncedTransactions} transactions to MySQL"
                configManager?.updateSyncStatus("SUCCESS", msg)
                _userMessage.value = msg
                onComplete(true, msg)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Synchronization failed"
                configManager?.updateSyncStatus("FAILED", errorMsg)
                _userMessage.value = "Sync Error: $errorMsg"
                onComplete(false, errorMsg)
            }
        }
    }

    /**
     * Pulls remote ledger records from the website MySQL database and restores into local Room SQLite.
     */
    fun pullFromWebsite(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val config = websiteConfig.value
        if (config.websiteUrl.isBlank()) {
            _userMessage.value = "Website URL is not configured."
            onComplete(false, "Website URL is not configured")
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            val result = syncManager.pullDataFromWebsite(config)
            _isSyncing.value = false

            if (result.isSuccess) {
                val pullData = result.getOrNull()!!
                val restoreRes = repository.restoreCompleteData(
                    customers = pullData.customers,
                    transactions = pullData.transactions,
                    cashEntries = pullData.cashbook,
                    profile = pullData.profile
                )
                if (restoreRes.isSuccess) {
                    val msg = "Restored ${pullData.customers.size} parties and ${pullData.transactions.size} transactions from MySQL"
                    configManager?.updateSyncStatus("SUCCESS", msg)
                    _userMessage.value = msg
                    onComplete(true, msg)
                } else {
                    val err = "Database restore failed: ${restoreRes.exceptionOrNull()?.message}"
                    _userMessage.value = err
                    onComplete(false, err)
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Pull failed"
                _userMessage.value = "Pull Error: $errorMsg"
                onComplete(false, errorMsg)
            }
        }
    }

    /**
     * Creates an instant offline JSON and SQL backup stored on device storage.
     */
    fun createOfflineBackup(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            if (backupManager == null) {
                _userMessage.value = "Backup manager unavailable"
                onComplete(false, "Backup manager unavailable")
                return@launch
            }

            val customers = repository.getAllCustomersOnce()
            val transactions = repository.getAllTransactionsOnce()
            val cashEntries = repository.getAllCashEntriesOnce()
            val profile = repository.getBusinessProfileOnce()
            val storeId = websiteConfig.value.storeId

            val result = backupManager.createLocalBackup(
                storeId = storeId,
                customers = customers,
                transactions = transactions,
                cashEntries = cashEntries,
                profile = profile
            )

            if (result.isSuccess) {
                refreshLocalBackups()
                val info = result.getOrNull()!!
                val msg = "Created offline backup with ${info.customerCount} parties & ${info.transactionCount} transactions"
                _userMessage.value = msg
                onComplete(true, msg)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Backup creation failed"
                _userMessage.value = "Backup failed: $err"
                onComplete(false, err)
            }
        }
    }

    /**
     * Restores ledger state from a selected local backup file.
     */
    fun restoreFromBackup(fileInfo: BackupFileInfo, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            if (backupManager == null) return@launch

            val payloadRes = backupManager.readBackupPayload(fileInfo)
            if (payloadRes.isSuccess) {
                val payload = payloadRes.getOrNull()!!
                val restoreRes = repository.restoreCompleteData(
                    customers = payload.customers,
                    transactions = payload.transactions,
                    cashEntries = payload.cashbook,
                    profile = payload.profile
                )
                if (restoreRes.isSuccess) {
                    val msg = "Successfully restored ${payload.customers.size} parties & ${payload.transactions.size} transactions"
                    _userMessage.value = msg
                    onComplete(true, msg)
                } else {
                    val err = restoreRes.exceptionOrNull()?.message ?: "Restore failed"
                    _userMessage.value = err
                    onComplete(false, err)
                }
            } else {
                val err = payloadRes.exceptionOrNull()?.message ?: "Failed to read backup file"
                _userMessage.value = err
                onComplete(false, err)
            }
        }
    }

    fun deleteBackup(fileInfo: BackupFileInfo) {
        viewModelScope.launch {
            backupManager?.deleteBackup(fileInfo.fileName)
            refreshLocalBackups()
            _userMessage.value = "Deleted backup file"
        }
    }

    suspend fun generateSqlDump(): String {
        val customers = repository.getAllCustomersOnce()
        val transactions = repository.getAllTransactionsOnce()
        val cashEntries = repository.getAllCashEntriesOnce()
        val profile = repository.getBusinessProfileOnce()
        val storeId = websiteConfig.value.storeId

        return MySqlScriptHelper.generateSqlDump(
            storeId = storeId,
            customers = customers,
            transactions = transactions,
            cashEntries = cashEntries,
            profile = profile
        )
    }

    fun getPhpScriptTemplate(): String {
        val currentKey = websiteConfig.value.apiKey.ifBlank { "TALLY_SECRET_KEY_12345" }
        return MySqlScriptHelper.getSamplePhpBackendScript(currentKey)
    }

    fun getBackupFileContent(fileName: String, callback: (String) -> Unit) {
        viewModelScope.launch {
            val content = backupManager?.getFileContent(fileName) ?: ""
            callback(content)
        }
    }

    /**
     * Previews a single customer's ledger statement in CSV format.
     */
    fun previewCustomerLedgerCsv(customer: Customer, transactions: List<Transaction>): String {
        val profile = businessProfile.value
        return CsvExporter.generateCustomerLedgerCsv(customer, transactions, profile)
    }

    /**
     * Exports a customer's full ledger history as an RFC 4180 CSV file and updates local backups.
     */
    fun exportCustomerLedgerCsv(
        context: Context,
        customer: Customer,
        transactions: List<Transaction>,
        onComplete: (Boolean, String, File?) -> Unit = { _, _, _ -> }
    ) {
        viewModelScope.launch {
            val profile = businessProfile.value
            val result = CsvExporter.exportCustomerLedgerToFile(context, customer, transactions, profile)
            if (result.isSuccess) {
                val file = result.getOrNull()
                refreshLocalBackups()
                val msg = "Exported CSV statement for ${customer.name}"
                _userMessage.value = msg
                onComplete(true, msg, file)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to export customer CSV"
                _userMessage.value = err
                onComplete(false, err, null)
            }
        }
    }

    /**
     * Exports a summary directory of all parties/customers to CSV.
     */
    fun exportAllCustomersCsv(
        context: Context,
        onComplete: (Boolean, String, File?) -> Unit = { _, _, _ -> }
    ) {
        viewModelScope.launch {
            val customers = repository.getAllCustomersOnce()
            val profile = businessProfile.value
            val result = CsvExporter.exportAllCustomersToFile(context, customers, profile)
            if (result.isSuccess) {
                val file = result.getOrNull()
                refreshLocalBackups()
                BackupReminderHelper.recordExportCompleted(context)
                val msg = "Exported ${customers.size} parties to CSV"
                _userMessage.value = msg
                onComplete(true, msg, file)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to export parties CSV"
                _userMessage.value = err
                onComplete(false, err, null)
            }
        }
    }

    /**
     * Exports all transactions journal across all customers to CSV.
     */
    fun exportAllTransactionsCsv(
        context: Context,
        onComplete: (Boolean, String, File?) -> Unit = { _, _, _ -> }
    ) {
        viewModelScope.launch {
            val transactions = repository.getAllTransactionsOnce()
            val customers = repository.getAllCustomersOnce()
            val profile = businessProfile.value
            val result = CsvExporter.exportAllTransactionsToFile(context, transactions, customers, profile)
            if (result.isSuccess) {
                val file = result.getOrNull()
                refreshLocalBackups()
                BackupReminderHelper.recordExportCompleted(context)
                val msg = "Exported ${transactions.size} transactions to CSV"
                _userMessage.value = msg
                onComplete(true, msg, file)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to export transactions CSV"
                _userMessage.value = err
                onComplete(false, err, null)
            }
        }
    }
}

class TallyKhataViewModelFactory(
    private val repository: TallyKhataRepository,
    private val context: Context? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TallyKhataViewModel::class.java)) {
            return TallyKhataViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
