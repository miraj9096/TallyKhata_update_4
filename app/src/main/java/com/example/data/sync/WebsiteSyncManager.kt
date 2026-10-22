package com.example.data.sync

import com.example.data.model.BusinessProfile
import com.example.data.model.CashEntry
import com.example.data.model.Customer
import com.example.data.model.Transaction
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.concurrent.TimeUnit

data class MySqlConnectionInfo(
    val connected: Boolean,
    val message: String,
    val databaseName: String = "",
    val serverVersion: String = "",
    val storeId: String = "",
    val customerCount: Int = 0,
    val transactionCount: Int = 0,
    val cashbookCount: Int = 0,
    val tablesReady: Boolean = true
)

data class SyncPushResult(
    val success: Boolean,
    val message: String,
    val syncedCustomers: Int = 0,
    val syncedTransactions: Int = 0,
    val syncedCashbook: Int = 0
)

data class SyncPullResult(
    val success: Boolean,
    val message: String,
    val customers: List<Customer> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val cashbook: List<CashEntry> = emptyList(),
    val profile: BusinessProfile? = null
)

/**
 * Client to interact with website phpMyAdmin / MySQL database as the primary backend or sync target.
 */
class WebsiteSyncManager {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(
        websiteUrl: String,
        apiKey: String,
        storeId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val details = testConnectionDetails(websiteUrl, apiKey, storeId)
        if (details.isSuccess) {
            val info = details.getOrNull()!!
            Result.success(info.message)
        } else {
            Result.failure(details.exceptionOrNull() ?: Exception("Failed to connect"))
        }
    }

    suspend fun testConnectionDetails(
        websiteUrl: String,
        apiKey: String,
        storeId: String
    ): Result<MySqlConnectionInfo> = withContext(Dispatchers.IO) {
        try {
            if (websiteUrl.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Website URL is required"))
            }

            val requestJson = JSONObject().apply {
                put("action", "ping")
                put("storeId", storeId)
            }

            val request = Request.Builder()
                .url(websiteUrl)
                .addHeader("X-API-Key", apiKey)
                .addHeader("X-Store-Id", storeId)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        JSONObject(body).optString("message", "HTTP Error ${response.code}")
                    } catch (_: Exception) {
                        "Server returned HTTP ${response.code}"
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val json = JSONObject(body)
                if (json.optString("status") == "ok") {
                    val info = MySqlConnectionInfo(
                        connected = true,
                        message = json.optString("message", "Connected successfully to MySQL database"),
                        databaseName = json.optString("database", ""),
                        serverVersion = json.optString("serverVersion", ""),
                        storeId = json.optString("storeId", storeId),
                        customerCount = json.optInt("customerCount", 0),
                        transactionCount = json.optInt("transactionCount", 0),
                        cashbookCount = json.optInt("cashbookCount", 0),
                        tablesReady = json.optBoolean("tablesReady", true)
                    )
                    Result.success(info)
                } else {
                    Result.failure(Exception(json.optString("message", "Unexpected server response")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Individual Live Record CRUD for Website MySQL Primary DB Mode ---

    suspend fun saveCustomer(
        config: WebsiteSyncConfig,
        customer: Customer
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (config.websiteUrl.isBlank()) return@withContext Result.failure(IllegalStateException("Website URL not configured"))

            val custObj = JSONObject().apply {
                put("id", customer.id)
                put("name", customer.name)
                put("phone", customer.phone)
                put("address", customer.address)
                put("type", customer.type)
                put("totalGave", customer.totalGave)
                put("totalGot", customer.totalGot)
                put("netBalance", customer.netBalance)
                put("createdAt", customer.createdAt?.toDate()?.time ?: System.currentTimeMillis())
                put("updatedAt", customer.updatedAt?.toDate()?.time ?: System.currentTimeMillis())
            }

            val requestJson = JSONObject().apply {
                put("action", "save_customer")
                put("storeId", config.storeId)
                put("customer", custObj)
            }

            val request = Request.Builder()
                .url(config.websiteUrl)
                .addHeader("X-API-Key", config.apiKey)
                .addHeader("X-Store-Id", config.storeId)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                }
                val json = JSONObject(body)
                if (json.optString("status") == "ok") {
                    Result.success(json.optString("id", customer.id))
                } else {
                    Result.failure(Exception(json.optString("message", "Failed to save customer in MySQL")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(
        config: WebsiteSyncConfig,
        customerId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (config.websiteUrl.isBlank()) return@withContext Result.failure(IllegalStateException("Website URL not configured"))

            val requestJson = JSONObject().apply {
                put("action", "delete_customer")
                put("storeId", config.storeId)
                put("id", customerId)
            }

            val request = Request.Builder()
                .url(config.websiteUrl)
                .addHeader("X-API-Key", config.apiKey)
                .addHeader("X-Store-Id", config.storeId)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveTransaction(
        config: WebsiteSyncConfig,
        transaction: Transaction,
        updatedCustomer: Customer
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (config.websiteUrl.isBlank()) return@withContext Result.failure(IllegalStateException("Website URL not configured"))

            val transObj = JSONObject().apply {
                put("id", transaction.id)
                put("customerId", transaction.customerId)
                put("customerName", transaction.customerName)
                put("type", transaction.type)
                put("amount", transaction.amount)
                put("note", transaction.note)
                put("billNumber", transaction.billNumber)
                put("transactionDate", transaction.transactionDate.toDate().time)
                put("createdAt", transaction.createdAt?.toDate()?.time ?: transaction.transactionDate.toDate().time)
                put("updatedAt", transaction.updatedAt?.toDate()?.time ?: transaction.transactionDate.toDate().time)
            }

            val custObj = JSONObject().apply {
                put("id", updatedCustomer.id)
                put("totalGave", updatedCustomer.totalGave)
                put("totalGot", updatedCustomer.totalGot)
                put("netBalance", updatedCustomer.netBalance)
            }

            val requestJson = JSONObject().apply {
                put("action", "save_transaction")
                put("storeId", config.storeId)
                put("transaction", transObj)
                put("customer", custObj)
            }

            val request = Request.Builder()
                .url(config.websiteUrl)
                .addHeader("X-API-Key", config.apiKey)
                .addHeader("X-Store-Id", config.storeId)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                }
                val json = JSONObject(body)
                if (json.optString("status") == "ok") {
                    Result.success(json.optString("id", transaction.id))
                } else {
                    Result.failure(Exception(json.optString("message", "Failed to save transaction in MySQL")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(
        config: WebsiteSyncConfig,
        transaction: Transaction,
        updatedCustomer: Customer
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (config.websiteUrl.isBlank()) return@withContext Result.failure(IllegalStateException("Website URL not configured"))

            val custObj = JSONObject().apply {
                put("id", updatedCustomer.id)
                put("totalGave", updatedCustomer.totalGave)
                put("totalGot", updatedCustomer.totalGot)
                put("netBalance", updatedCustomer.netBalance)
            }

            val requestJson = JSONObject().apply {
                put("action", "delete_transaction")
                put("storeId", config.storeId)
                put("id", transaction.id)
                put("customerId", transaction.customerId)
                put("customer", custObj)
            }

            val request = Request.Builder()
                .url(config.websiteUrl)
                .addHeader("X-API-Key", config.apiKey)
                .addHeader("X-Store-Id", config.storeId)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveCashEntry(
        config: WebsiteSyncConfig,
        cashEntry: CashEntry
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (config.websiteUrl.isBlank()) return@withContext Result.failure(IllegalStateException("Website URL not configured"))

            val cashObj = JSONObject().apply {
                put("id", cashEntry.id)
                put("type", cashEntry.type)
                put("amount", cashEntry.amount)
                put("category", cashEntry.category)
                put("note", cashEntry.note)
                put("entryDate", cashEntry.entryDate.toDate().time)
            }

            val requestJson = JSONObject().apply {
                put("action", "save_cash")
                put("storeId", config.storeId)
                put("cashEntry", cashObj)
            }

            val request = Request.Builder()
                .url(config.websiteUrl)
                .addHeader("X-API-Key", config.apiKey)
                .addHeader("X-Store-Id", config.storeId)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                }
                val json = JSONObject(body)
                if (json.optString("status") == "ok") {
                    Result.success(json.optString("id", cashEntry.id))
                } else {
                    Result.failure(Exception(json.optString("message", "Failed to save cash entry in MySQL")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCashEntry(
        config: WebsiteSyncConfig,
        cashEntryId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (config.websiteUrl.isBlank()) return@withContext Result.failure(IllegalStateException("Website URL not configured"))

            val requestJson = JSONObject().apply {
                put("action", "delete_cash")
                put("storeId", config.storeId)
                put("id", cashEntryId)
            }

            val request = Request.Builder()
                .url(config.websiteUrl)
                .addHeader("X-API-Key", config.apiKey)
                .addHeader("X-Store-Id", config.storeId)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveProfile(
        config: WebsiteSyncConfig,
        profile: BusinessProfile
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (config.websiteUrl.isBlank()) return@withContext Result.failure(IllegalStateException("Website URL not configured"))

            val profObj = JSONObject().apply {
                put("businessName", profile.businessName)
                put("ownerName", profile.ownerName)
                put("phone", profile.phone)
                put("email", profile.email)
                put("currency", profile.currency)
            }

            val requestJson = JSONObject().apply {
                put("action", "save_profile")
                put("storeId", config.storeId)
                put("profile", profObj)
            }

            val request = Request.Builder()
                .url(config.websiteUrl)
                .addHeader("X-API-Key", config.apiKey)
                .addHeader("X-Store-Id", config.storeId)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pushOfflineDataToWebsite(
        config: WebsiteSyncConfig,
        customers: List<Customer>,
        transactions: List<Transaction>,
        cashEntries: List<CashEntry>,
        profile: BusinessProfile
    ): Result<SyncPushResult> = withContext(Dispatchers.IO) {
        try {
            if (config.websiteUrl.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Website URL is not configured. Go to Settings."))
            }

            val rootJson = JSONObject().apply {
                put("action", "push")
                put("storeId", config.storeId)
                put("timestamp", System.currentTimeMillis())

                // Profile
                put("profile", JSONObject().apply {
                    put("businessName", profile.businessName)
                    put("ownerName", profile.ownerName)
                    put("phone", profile.phone)
                    put("email", profile.email)
                    put("currency", profile.currency)
                })

                // Customers
                val custArray = JSONArray()
                for (c in customers) {
                    custArray.put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("phone", c.phone)
                        put("address", c.address)
                        put("type", c.type)
                        put("totalGave", c.totalGave)
                        put("totalGot", c.totalGot)
                        put("netBalance", c.netBalance)
                        put("createdAt", c.createdAt?.toDate()?.time ?: System.currentTimeMillis())
                        put("updatedAt", c.updatedAt?.toDate()?.time ?: System.currentTimeMillis())
                    })
                }
                put("customers", custArray)

                // Transactions
                val transArray = JSONArray()
                for (t in transactions) {
                    transArray.put(JSONObject().apply {
                        put("id", t.id)
                        put("customerId", t.customerId)
                        put("customerName", t.customerName)
                        put("type", t.type)
                        put("amount", t.amount)
                        put("note", t.note)
                        put("billNumber", t.billNumber)
                        put("transactionDate", t.transactionDate.toDate().time)
                        put("createdAt", t.createdAt?.toDate()?.time ?: t.transactionDate.toDate().time)
                        put("updatedAt", t.updatedAt?.toDate()?.time ?: t.transactionDate.toDate().time)
                    })
                }
                put("transactions", transArray)

                // Cashbook
                val cashArray = JSONArray()
                for (cb in cashEntries) {
                    cashArray.put(JSONObject().apply {
                        put("id", cb.id)
                        put("type", cb.type)
                        put("amount", cb.amount)
                        put("category", cb.category)
                        put("note", cb.note)
                        put("entryDate", cb.entryDate.toDate().time)
                    })
                }
                put("cashbook", cashArray)
            }

            val request = Request.Builder()
                .url(config.websiteUrl)
                .addHeader("X-API-Key", config.apiKey)
                .addHeader("X-Store-Id", config.storeId)
                .post(rootJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        JSONObject(body).optString("message", "HTTP Error ${response.code}")
                    } catch (_: Exception) {
                        "Server returned HTTP ${response.code}"
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val json = JSONObject(body)
                if (json.optString("status") == "ok") {
                    val pushResult = SyncPushResult(
                        success = true,
                        message = json.optString("message", "Sync successful"),
                        syncedCustomers = json.optInt("syncedCustomers", customers.size),
                        syncedTransactions = json.optInt("syncedTransactions", transactions.size),
                        syncedCashbook = json.optInt("syncedCashbook", cashEntries.size)
                    )
                    Result.success(pushResult)
                } else {
                    Result.failure(Exception(json.optString("message", "Website sync failed")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pullDataFromWebsite(
        config: WebsiteSyncConfig
    ): Result<SyncPullResult> = withContext(Dispatchers.IO) {
        try {
            if (config.websiteUrl.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Website URL is not configured. Go to Settings."))
            }

            val requestJson = JSONObject().apply {
                put("action", "pull")
                put("storeId", config.storeId)
            }

            val request = Request.Builder()
                .url(config.websiteUrl)
                .addHeader("X-API-Key", config.apiKey)
                .addHeader("X-Store-Id", config.storeId)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        JSONObject(body).optString("message", "HTTP Error ${response.code}")
                    } catch (_: Exception) {
                        "Server returned HTTP ${response.code}"
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val json = JSONObject(body)
                if (json.optString("status") != "ok") {
                    return@withContext Result.failure(Exception(json.optString("message", "Pull failed")))
                }

                // Parse Profile
                var profile: BusinessProfile? = null
                if (json.has("profile") && !json.isNull("profile")) {
                    val pObj = json.getJSONObject("profile")
                    profile = BusinessProfile(
                        id = "settings",
                        businessName = pObj.optString("business_name", pObj.optString("businessName", "My Store")),
                        ownerName = pObj.optString("owner_name", pObj.optString("ownerName", "")),
                        phone = pObj.optString("phone", ""),
                        email = pObj.optString("email", ""),
                        currency = pObj.optString("currency", "$")
                    )
                }

                // Parse Customers
                val customerList = mutableListOf<Customer>()
                val custArray = json.optJSONArray("customers")
                if (custArray != null) {
                    for (i in 0 until custArray.length()) {
                        val obj = custArray.getJSONObject(i)
                        val created = obj.optLong("created_at", obj.optLong("createdAt", System.currentTimeMillis()))
                        val updated = obj.optLong("updated_at", obj.optLong("updatedAt", System.currentTimeMillis()))
                        customerList.add(
                            Customer(
                                id = obj.optString("id"),
                                name = obj.optString("name"),
                                phone = obj.optString("phone", ""),
                                address = obj.optString("address", ""),
                                type = obj.optString("type", "CUSTOMER"),
                                totalGave = obj.optDouble("total_gave", obj.optDouble("totalGave", 0.0)),
                                totalGot = obj.optDouble("total_got", obj.optDouble("totalGot", 0.0)),
                                netBalance = obj.optDouble("net_balance", obj.optDouble("netBalance", 0.0)),
                                createdAt = Timestamp(Date(created)),
                                updatedAt = Timestamp(Date(updated))
                            )
                        )
                    }
                }

                // Parse Transactions
                val transList = mutableListOf<Transaction>()
                val transArray = json.optJSONArray("transactions")
                if (transArray != null) {
                    for (i in 0 until transArray.length()) {
                        val obj = transArray.getJSONObject(i)
                        val tDate = obj.optLong("transaction_date", obj.optLong("transactionDate", System.currentTimeMillis()))
                        val created = obj.optLong("created_at", obj.optLong("createdAt", tDate))
                        val updated = obj.optLong("updated_at", obj.optLong("updatedAt", tDate))
                        transList.add(
                            Transaction(
                                id = obj.optString("id"),
                                customerId = obj.optString("customer_id", obj.optString("customerId", "")),
                                customerName = obj.optString("customer_name", obj.optString("customerName", "")),
                                type = obj.optString("type", "GAVE"),
                                amount = obj.optDouble("amount", 0.0),
                                note = obj.optString("note", ""),
                                billNumber = obj.optString("bill_number", obj.optString("billNumber", "")),
                                transactionDate = Timestamp(Date(tDate)),
                                createdAt = Timestamp(Date(created)),
                                updatedAt = Timestamp(Date(updated))
                            )
                        )
                    }
                }

                // Parse Cashbook
                val cashList = mutableListOf<CashEntry>()
                val cashArray = json.optJSONArray("cashbook")
                if (cashArray != null) {
                    for (i in 0 until cashArray.length()) {
                        val obj = cashArray.getJSONObject(i)
                        val eDate = obj.optLong("entry_date", obj.optLong("entryDate", System.currentTimeMillis()))
                        cashList.add(
                            CashEntry(
                                id = obj.optString("id"),
                                type = obj.optString("type", "CASH_IN"),
                                amount = obj.optDouble("amount", 0.0),
                                category = obj.optString("category", "General"),
                                note = obj.optString("note", ""),
                                entryDate = Timestamp(Date(eDate))
                            )
                        )
                    }
                }

                Result.success(
                    SyncPullResult(
                        success = true,
                        message = "Pulled ${customerList.size} customers, ${transList.size} transactions from MySQL",
                        customers = customerList,
                        transactions = transList,
                        cashbook = cashList,
                        profile = profile
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
