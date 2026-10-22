package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.repository.RoomTallyKhataRepository
import com.example.data.model.BusinessProfile
import com.example.data.model.CashEntry
import com.example.data.model.Customer
import com.example.data.model.Transaction
import com.example.data.sync.WebsiteSyncConfig
import com.example.data.sync.WebsiteSyncConfigManager
import com.example.data.sync.WebsiteSyncManager
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.UUID

class TallyKhataRepository(
    private val db: FirebaseFirestore,
    private val context: Context? = null
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        context.applicationContext
    )

    private val auth = Firebase.auth
    private val roomRepo: RoomTallyKhataRepository? by lazy {
        context?.let { RoomTallyKhataRepository(it) }
    }

    private val syncConfigManager: WebsiteSyncConfigManager? by lazy {
        context?.let { WebsiteSyncConfigManager(it) }
    }
    private val websiteSyncManager = WebsiteSyncManager()

    private val cashbookPrefs: SharedPreferences? by lazy {
        context?.getSharedPreferences("tally_local_cashbook", Context.MODE_PRIVATE)
    }

    private val profilePrefs: SharedPreferences? by lazy {
        context?.getSharedPreferences("tally_local_profile", Context.MODE_PRIVATE)
    }

    private val localCashbookFlow = MutableStateFlow<List<CashEntry>>(emptyList())
    private val localProfileFlow = MutableStateFlow<BusinessProfile>(BusinessProfile())

    init {
        loadLocalCashbook()
        loadLocalProfile()
    }

    private suspend fun syncToRemoteMySqlIfEnabled(action: suspend (WebsiteSyncConfig, WebsiteSyncManager) -> Unit) {
        try {
            val config = syncConfigManager?.getConfig() ?: return
            if (config.useWebsiteMySqlAsPrimary && config.websiteUrl.isNotBlank()) {
                action(config, websiteSyncManager)
            }
        } catch (_: Exception) {
            // Keep local changes safe; auto-sync or pull/push will reconcile
        }
    }

    private fun loadLocalCashbook() {
        val json = cashbookPrefs?.getString("entries", "[]") ?: "[]"
        try {
            val array = JSONArray(json)
            val list = mutableListOf<CashEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CashEntry(
                        id = obj.getString("id"),
                        type = obj.getString("type"),
                        amount = obj.getDouble("amount"),
                        category = obj.optString("category", "General"),
                        note = obj.optString("note", ""),
                        entryDate = Timestamp(Date(obj.getLong("entryDate")))
                    )
                )
            }
            localCashbookFlow.value = list.sortedByDescending { it.entryDate.toDate().time }
        } catch (_: Exception) {}
    }

    private fun saveLocalCashbook(list: List<CashEntry>) {
        localCashbookFlow.value = list.sortedByDescending { it.entryDate.toDate().time }
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("type", item.type)
                put("amount", item.amount)
                put("category", item.category)
                put("note", item.note)
                put("entryDate", item.entryDate.toDate().time)
            }
            array.put(obj)
        }
        cashbookPrefs?.edit()?.putString("entries", array.toString())?.apply()
    }

    private fun loadLocalProfile() {
        val name = profilePrefs?.getString("businessName", "My Store") ?: "My Store"
        val owner = profilePrefs?.getString("ownerName", "") ?: ""
        val phone = profilePrefs?.getString("phone", "") ?: ""
        val email = profilePrefs?.getString("email", "") ?: ""
        val currency = profilePrefs?.getString("currency", "$") ?: "$"
        localProfileFlow.value = BusinessProfile(
            id = "settings",
            businessName = name,
            ownerName = owner,
            phone = phone,
            email = email,
            currency = currency
        )
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- Customers ---

    fun observeCustomers(): Flow<List<Customer>> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val path = "users/$uid/customers"
            emitAll(
                db.collection("users").document(uid).collection("customers")
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(Customer::class.java) }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        } else if (roomRepo != null) {
            emitAll(
                roomRepo!!.allCustomers.map { entities ->
                    entities.map { entity ->
                        Customer(
                            id = entity.id,
                            name = entity.name,
                            phone = entity.phone,
                            address = entity.address,
                            type = entity.type,
                            totalGave = entity.totalGave,
                            totalGot = entity.totalGot,
                            netBalance = entity.netBalance,
                            createdAt = Timestamp(Date(entity.createdAt)),
                            updatedAt = Timestamp(Date(entity.updatedAt))
                        )
                    }
                }
            )
        } else {
            throw IllegalStateException("User must be signed in before accessing customer data.")
        }
    }

    suspend fun createCustomer(customer: Customer): Result<String> {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            return try {
                val collectionRef = db.collection("users").document(uid).collection("customers")
                val docRef = collectionRef.document()
                val payload = hashMapOf<String, Any>(
                    "name" to customer.name,
                    "phone" to customer.phone,
                    "address" to customer.address,
                    "type" to customer.type,
                    "totalGave" to customer.totalGave,
                    "totalGot" to customer.totalGot,
                    "netBalance" to customer.netBalance,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(docRef.id)
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, "users/$uid/customers")
                Result.failure(e)
            }
        } else if (roomRepo != null) {
            return try {
                val id = if (customer.id.isNotBlank()) customer.id else UUID.randomUUID().toString()
                val entity = CustomerEntity(
                    id = id,
                    name = customer.name,
                    phone = customer.phone,
                    address = customer.address,
                    type = customer.type,
                    totalGave = customer.totalGave,
                    totalGot = customer.totalGot,
                    netBalance = customer.netBalance,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                roomRepo!!.insertCustomer(entity)
                syncToRemoteMySqlIfEnabled { config, syncMgr ->
                    syncMgr.saveCustomer(config, customer.copy(id = id))
                }
                Result.success(id)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            return Result.failure(IllegalStateException("User session not found"))
        }
    }

    suspend fun updateCustomer(customer: Customer): Result<Unit> {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            return try {
                require(customer.id.isNotBlank()) { "Customer ID must not be blank" }
                val docRef = db.collection("users").document(uid).collection("customers").document(customer.id)
                val payload = hashMapOf<String, Any>(
                    "name" to customer.name,
                    "phone" to customer.phone,
                    "address" to customer.address,
                    "type" to customer.type,
                    "totalGave" to customer.totalGave,
                    "totalGot" to customer.totalGot,
                    "netBalance" to customer.netBalance,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (customer.createdAt != null) {
                    payload["createdAt"] = customer.createdAt
                } else {
                    payload["createdAt"] = FieldValue.serverTimestamp()
                }
                docRef.set(payload).await()
                Result.success(Unit)
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, "users/$uid/customers/${customer.id}")
                Result.failure(e)
            }
        } else if (roomRepo != null) {
            return try {
                val entity = CustomerEntity(
                    id = customer.id,
                    name = customer.name,
                    phone = customer.phone,
                    address = customer.address,
                    type = customer.type,
                    totalGave = customer.totalGave,
                    totalGot = customer.totalGot,
                    netBalance = customer.netBalance,
                    createdAt = customer.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                roomRepo!!.updateCustomer(entity)
                syncToRemoteMySqlIfEnabled { config, syncMgr ->
                    syncMgr.saveCustomer(config, customer)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            return Result.failure(IllegalStateException("User session not found"))
        }
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            return try {
                val docRef = db.collection("users").document(uid).collection("customers").document(customerId)
                docRef.delete().await()
                Result.success(Unit)
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, "users/$uid/customers/$customerId")
                Result.failure(e)
            }
        } else if (roomRepo != null) {
            return try {
                roomRepo!!.deleteCustomer(customerId)
                syncToRemoteMySqlIfEnabled { config, syncMgr ->
                    syncMgr.deleteCustomer(config, customerId)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            return Result.failure(IllegalStateException("User session not found"))
        }
    }

    // --- Transactions ---

    fun observeTransactionsForCustomer(customerId: String): Flow<List<Transaction>> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val path = "users/$uid/transactions"
            emitAll(
                db.collection("users").document(uid).collection("transactions")
                    .whereEqualTo("customerId", customerId)
                    .orderBy("transactionDate", Query.Direction.DESCENDING)
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(Transaction::class.java) }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        } else if (roomRepo != null) {
            emitAll(
                roomRepo!!.getTransactionsForCustomer(customerId).map { entities ->
                    entities.map { entity ->
                        Transaction(
                            id = entity.id,
                            customerId = entity.customerId,
                            customerName = entity.customerName,
                            type = entity.type,
                            amount = entity.amount,
                            note = entity.note,
                            billNumber = entity.billNumber,
                            transactionDate = Timestamp(Date(entity.transactionDate)),
                            createdAt = Timestamp(Date(entity.createdAt)),
                            updatedAt = Timestamp(Date(entity.updatedAt))
                        )
                    }
                }
            )
        } else {
            throw IllegalStateException("User session not found")
        }
    }

    fun observeAllTransactions(): Flow<List<Transaction>> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val path = "users/$uid/transactions"
            emitAll(
                db.collection("users").document(uid).collection("transactions")
                    .orderBy("transactionDate", Query.Direction.DESCENDING)
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(Transaction::class.java) }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        } else if (roomRepo != null) {
            emitAll(
                roomRepo!!.allTransactions.map { entities ->
                    entities.map { entity ->
                        Transaction(
                            id = entity.id,
                            customerId = entity.customerId,
                            customerName = entity.customerName,
                            type = entity.type,
                            amount = entity.amount,
                            note = entity.note,
                            billNumber = entity.billNumber,
                            transactionDate = Timestamp(Date(entity.transactionDate)),
                            createdAt = Timestamp(Date(entity.createdAt)),
                            updatedAt = Timestamp(Date(entity.updatedAt))
                        )
                    }
                }
            )
        } else {
            throw IllegalStateException("User session not found")
        }
    }

    suspend fun recordTransaction(transaction: Transaction, currentCustomer: Customer): Result<String> {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            return try {
                val transDoc = db.collection("users").document(uid).collection("transactions").document()
                val custDoc = db.collection("users").document(uid).collection("customers").document(transaction.customerId)

                val newTotalGave = if (transaction.type == "GAVE") currentCustomer.totalGave + transaction.amount else currentCustomer.totalGave
                val newTotalGot = if (transaction.type == "GOT") currentCustomer.totalGot + transaction.amount else currentCustomer.totalGot
                val newNetBalance = newTotalGave - newTotalGot

                val transPayload = hashMapOf<String, Any>(
                    "customerId" to transaction.customerId,
                    "customerName" to transaction.customerName,
                    "type" to transaction.type,
                    "amount" to transaction.amount,
                    "note" to transaction.note,
                    "billNumber" to transaction.billNumber,
                    "transactionDate" to transaction.transactionDate,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                val custPayload = hashMapOf<String, Any>(
                    "name" to currentCustomer.name,
                    "phone" to currentCustomer.phone,
                    "address" to currentCustomer.address,
                    "type" to currentCustomer.type,
                    "totalGave" to newTotalGave,
                    "totalGot" to newTotalGot,
                    "netBalance" to newNetBalance,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (currentCustomer.createdAt != null) {
                    custPayload["createdAt"] = currentCustomer.createdAt
                } else {
                    custPayload["createdAt"] = FieldValue.serverTimestamp()
                }

                db.runBatch { batch ->
                    batch.set(transDoc, transPayload)
                    batch.set(custDoc, custPayload)
                }.await()

                Result.success(transDoc.id)
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, "users/$uid/transactions")
                Result.failure(e)
            }
        } else if (roomRepo != null) {
            return try {
                val id = if (transaction.id.isNotBlank()) transaction.id else UUID.randomUUID().toString()
                val entity = TransactionEntity(
                    id = id,
                    customerId = transaction.customerId,
                    customerName = transaction.customerName,
                    type = transaction.type,
                    amount = transaction.amount,
                    note = transaction.note,
                    billNumber = transaction.billNumber,
                    transactionDate = transaction.transactionDate.toDate().time,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                val res = roomRepo!!.recordTransaction(entity)
                if (res.isSuccess) {
                    val newTotalGave = if (transaction.type == "GAVE") currentCustomer.totalGave + transaction.amount else currentCustomer.totalGave
                    val newTotalGot = if (transaction.type == "GOT") currentCustomer.totalGot + transaction.amount else currentCustomer.totalGot
                    val updatedCustomer = currentCustomer.copy(
                        totalGave = newTotalGave,
                        totalGot = newTotalGot,
                        netBalance = newTotalGave - newTotalGot
                    )
                    syncToRemoteMySqlIfEnabled { config, syncMgr ->
                        syncMgr.saveTransaction(config, transaction.copy(id = id), updatedCustomer)
                    }
                    Result.success(id)
                } else {
                    Result.failure(res.exceptionOrNull() ?: Exception("Failed"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            return Result.failure(IllegalStateException("User session not found"))
        }
    }

    suspend fun deleteTransaction(transaction: Transaction, currentCustomer: Customer): Result<Unit> {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            return try {
                val transDoc = db.collection("users").document(uid).collection("transactions").document(transaction.id)
                val custDoc = db.collection("users").document(uid).collection("customers").document(transaction.customerId)

                val newTotalGave = if (transaction.type == "GAVE") (currentCustomer.totalGave - transaction.amount).coerceAtLeast(0.0) else currentCustomer.totalGave
                val newTotalGot = if (transaction.type == "GOT") (currentCustomer.totalGot - transaction.amount).coerceAtLeast(0.0) else currentCustomer.totalGot
                val newNetBalance = newTotalGave - newTotalGot

                val custPayload = hashMapOf<String, Any>(
                    "name" to currentCustomer.name,
                    "phone" to currentCustomer.phone,
                    "address" to currentCustomer.address,
                    "type" to currentCustomer.type,
                    "totalGave" to newTotalGave,
                    "totalGot" to newTotalGot,
                    "netBalance" to newNetBalance,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (currentCustomer.createdAt != null) {
                    custPayload["createdAt"] = currentCustomer.createdAt
                } else {
                    custPayload["createdAt"] = FieldValue.serverTimestamp()
                }

                db.runBatch { batch ->
                    batch.delete(transDoc)
                    batch.set(custDoc, custPayload)
                }.await()

                Result.success(Unit)
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, "users/$uid/transactions/${transaction.id}")
                Result.failure(e)
            }
        } else if (roomRepo != null) {
            val res = roomRepo!!.deleteTransaction(transaction.id)
            if (res.isSuccess) {
                val newTotalGave = if (transaction.type == "GAVE") (currentCustomer.totalGave - transaction.amount).coerceAtLeast(0.0) else currentCustomer.totalGave
                val newTotalGot = if (transaction.type == "GOT") (currentCustomer.totalGot - transaction.amount).coerceAtLeast(0.0) else currentCustomer.totalGot
                val updatedCustomer = currentCustomer.copy(
                    totalGave = newTotalGave,
                    totalGot = newTotalGot,
                    netBalance = newTotalGave - newTotalGot
                )
                syncToRemoteMySqlIfEnabled { config, syncMgr ->
                    syncMgr.deleteTransaction(config, transaction, updatedCustomer)
                }
            }
            return res
        } else {
            return Result.failure(IllegalStateException("User session not found"))
        }
    }

    // --- Cashbook ---

    fun observeCashbook(): Flow<List<CashEntry>> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val path = "users/$uid/cashbook"
            emitAll(
                db.collection("users").document(uid).collection("cashbook")
                    .orderBy("entryDate", Query.Direction.DESCENDING)
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(CashEntry::class.java) }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        } else {
            emitAll(localCashbookFlow.asStateFlow())
        }
    }

    suspend fun addCashEntry(entry: CashEntry): Result<String> {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            return try {
                val docRef = db.collection("users").document(uid).collection("cashbook").document()
                val payload = hashMapOf<String, Any>(
                    "type" to entry.type,
                    "amount" to entry.amount,
                    "category" to entry.category,
                    "note" to entry.note,
                    "entryDate" to entry.entryDate,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(docRef.id)
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, "users/$uid/cashbook")
                Result.failure(e)
            }
        } else {
            val id = if (entry.id.isNotBlank()) entry.id else UUID.randomUUID().toString()
            val newEntry = entry.copy(id = id)
            val currentList = localCashbookFlow.value.toMutableList()
            currentList.add(newEntry)
            saveLocalCashbook(currentList)
            syncToRemoteMySqlIfEnabled { config, syncMgr ->
                syncMgr.saveCashEntry(config, newEntry)
            }
            return Result.success(id)
        }
    }

    suspend fun deleteCashEntry(entryId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            return try {
                val docRef = db.collection("users").document(uid).collection("cashbook").document(entryId)
                docRef.delete().await()
                Result.success(Unit)
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, "users/$uid/cashbook/$entryId")
                Result.failure(e)
            }
        } else {
            val currentList = localCashbookFlow.value.filterNot { it.id == entryId }
            saveLocalCashbook(currentList)
            syncToRemoteMySqlIfEnabled { config, syncMgr ->
                syncMgr.deleteCashEntry(config, entryId)
            }
            return Result.success(Unit)
        }
    }

    // --- Profile & Settings ---

    fun observeProfile(): Flow<BusinessProfile?> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val path = "users/$uid/profile/settings"
            emitAll(
                db.collection("users").document(uid).collection("profile").document("settings")
                    .snapshots()
                    .map { snapshot -> snapshot.toObject(BusinessProfile::class.java) }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                        throw error
                    }
            )
        } else {
            emitAll(localProfileFlow.asStateFlow())
        }
    }

    suspend fun saveProfile(profile: BusinessProfile): Result<Unit> {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            return try {
                val docRef = db.collection("users").document(uid).collection("profile").document("settings")
                val payload = hashMapOf<String, Any>(
                    "businessName" to profile.businessName,
                    "ownerName" to profile.ownerName,
                    "phone" to profile.phone,
                    "email" to profile.email,
                    "currency" to profile.currency,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (profile.createdAt != null) {
                    payload["createdAt"] = profile.createdAt
                } else {
                    payload["createdAt"] = FieldValue.serverTimestamp()
                }
                docRef.set(payload).await()
                Result.success(Unit)
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, "users/$uid/profile/settings")
                Result.failure(e)
            }
        } else {
            localProfileFlow.value = profile
            profilePrefs?.edit()
                ?.putString("businessName", profile.businessName)
                ?.putString("ownerName", profile.ownerName)
                ?.putString("phone", profile.phone)
                ?.putString("email", profile.email)
                ?.putString("currency", profile.currency)
                ?.apply()
            syncToRemoteMySqlIfEnabled { config, syncMgr ->
                syncMgr.saveProfile(config, profile)
            }
            return Result.success(Unit)
        }
    }

    // --- Batch Data Access & Restoration for Offline Sync & Backup ---

    suspend fun getAllCustomersOnce(): List<Customer> {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            return try {
                val snapshot = db.collection("users").document(uid).collection("customers")
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .get().await()
                snapshot.toObjects(Customer::class.java)
            } catch (_: Exception) {
                emptyList()
            }
        } else if (roomRepo != null) {
            return roomRepo!!.getAllCustomersOnce().map { entity ->
                Customer(
                    id = entity.id,
                    name = entity.name,
                    phone = entity.phone,
                    address = entity.address,
                    type = entity.type,
                    totalGave = entity.totalGave,
                    totalGot = entity.totalGot,
                    netBalance = entity.netBalance,
                    createdAt = Timestamp(Date(entity.createdAt)),
                    updatedAt = Timestamp(Date(entity.updatedAt))
                )
            }
        }
        return emptyList()
    }

    suspend fun getAllTransactionsOnce(): List<Transaction> {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            return try {
                val snapshot = db.collection("users").document(uid).collection("transactions")
                    .orderBy("transactionDate", Query.Direction.DESCENDING)
                    .get().await()
                snapshot.toObjects(Transaction::class.java)
            } catch (_: Exception) {
                emptyList()
            }
        } else if (roomRepo != null) {
            return roomRepo!!.getAllTransactionsOnce().map { entity ->
                Transaction(
                    id = entity.id,
                    customerId = entity.customerId,
                    customerName = entity.customerName,
                    type = entity.type,
                    amount = entity.amount,
                    note = entity.note,
                    billNumber = entity.billNumber,
                    transactionDate = Timestamp(Date(entity.transactionDate)),
                    createdAt = Timestamp(Date(entity.createdAt)),
                    updatedAt = Timestamp(Date(entity.updatedAt))
                )
            }
        }
        return emptyList()
    }

    fun getAllCashEntriesOnce(): List<CashEntry> {
        return localCashbookFlow.value
    }

    fun getBusinessProfileOnce(): BusinessProfile {
        return localProfileFlow.value
    }

    suspend fun restoreCompleteData(
        customers: List<Customer>,
        transactions: List<Transaction>,
        cashEntries: List<CashEntry>,
        profile: BusinessProfile?
    ): Result<Unit> {
        return try {
            roomRepo?.let { rr ->
                rr.clearAllData()
                val customerEntities = customers.map { c ->
                    CustomerEntity(
                        id = c.id,
                        name = c.name,
                        phone = c.phone,
                        address = c.address,
                        type = c.type,
                        totalGave = c.totalGave,
                        totalGot = c.totalGot,
                        netBalance = c.netBalance,
                        createdAt = c.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                        updatedAt = c.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                    )
                }
                val transactionEntities = transactions.map { t ->
                    TransactionEntity(
                        id = t.id,
                        customerId = t.customerId,
                        customerName = t.customerName,
                        type = t.type,
                        amount = t.amount,
                        note = t.note,
                        billNumber = t.billNumber,
                        transactionDate = t.transactionDate.toDate().time,
                        createdAt = t.createdAt?.toDate()?.time ?: t.transactionDate.toDate().time,
                        updatedAt = t.updatedAt?.toDate()?.time ?: t.transactionDate.toDate().time
                    )
                }
                rr.insertCustomers(customerEntities)
                rr.insertTransactions(transactionEntities)
            }

            if (cashEntries.isNotEmpty()) {
                saveLocalCashbook(cashEntries)
            }

            if (profile != null) {
                saveProfile(profile)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

