package com.example.data.backup

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.BusinessProfile
import com.example.data.model.CashEntry
import com.example.data.model.Customer
import com.example.data.model.Transaction
import com.example.data.sync.MySqlScriptHelper
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupFileInfo(
    val fileName: String,
    val filePath: String,
    val formattedDate: String,
    val timestamp: Long,
    val customerCount: Int,
    val transactionCount: Int,
    val sizeBytes: Long,
    val isSql: Boolean = false,
    val isCsv: Boolean = false
)

data class BackupPayload(
    val profile: BusinessProfile,
    val customers: List<Customer>,
    val transactions: List<Transaction>,
    val cashbook: List<CashEntry>
)

class BackupManager(private val context: Context) {

    private val backupDir: File by lazy {
        val dir = File(context.filesDir, "tally_backups")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    suspend fun createLocalBackup(
        storeId: String,
        customers: List<Customer>,
        transactions: List<Transaction>,
        cashEntries: List<CashEntry>,
        profile: BusinessProfile
    ): Result<BackupFileInfo> = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val dateTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(timestamp))
            val jsonFileName = "tally_backup_$dateTag.json"
            val jsonFile = File(backupDir, jsonFileName)

            val rootJson = JSONObject().apply {
                put("version", 1)
                put("storeId", storeId)
                put("timestamp", timestamp)
                put("customerCount", customers.size)
                put("transactionCount", transactions.size)

                // Profile
                put("profile", JSONObject().apply {
                    put("businessName", profile.businessName)
                    put("ownerName", profile.ownerName)
                    put("phone", profile.phone)
                    put("email", profile.email)
                    put("currency", profile.currency)
                })

                // Customers
                val custArr = JSONArray()
                for (c in customers) {
                    custArr.put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("phone", c.phone)
                        put("address", c.address)
                        put("type", c.type)
                        put("totalGave", c.totalGave)
                        put("totalGot", c.totalGot)
                        put("netBalance", c.netBalance)
                        put("createdAt", c.createdAt?.toDate()?.time ?: timestamp)
                        put("updatedAt", c.updatedAt?.toDate()?.time ?: timestamp)
                    })
                }
                put("customers", custArr)

                // Transactions
                val transArr = JSONArray()
                for (t in transactions) {
                    transArr.put(JSONObject().apply {
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
                put("transactions", transArr)

                // Cashbook
                val cashArr = JSONArray()
                for (cb in cashEntries) {
                    cashArr.put(JSONObject().apply {
                        put("id", cb.id)
                        put("type", cb.type)
                        put("amount", cb.amount)
                        put("category", cb.category)
                        put("note", cb.note)
                        put("entryDate", cb.entryDate.toDate().time)
                    })
                }
                put("cashbook", cashArr)
            }

            jsonFile.writeText(rootJson.toString(2))

            // Also write a matching .sql dump alongside for direct MySQL import!
            val sqlFileName = "tally_mysql_$dateTag.sql"
            val sqlFile = File(backupDir, sqlFileName)
            val sqlContent = MySqlScriptHelper.generateSqlDump(
                storeId = storeId,
                customers = customers,
                transactions = transactions,
                cashEntries = cashEntries,
                profile = profile
            )
            sqlFile.writeText(sqlContent)

            val displayDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
            Result.success(
                BackupFileInfo(
                    fileName = jsonFileName,
                    filePath = jsonFile.absolutePath,
                    formattedDate = displayDate,
                    timestamp = timestamp,
                    customerCount = customers.size,
                    transactionCount = transactions.size,
                    sizeBytes = jsonFile.length()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listLocalBackups(): List<BackupFileInfo> = withContext(Dispatchers.IO) {
        val files = backupDir.listFiles { _, name -> name.endsWith(".json") || name.endsWith(".sql") || name.endsWith(".csv") } ?: return@withContext emptyList()
        val list = mutableListOf<BackupFileInfo>()

        for (file in files) {
            val isSql = file.name.endsWith(".sql")
            val isCsv = file.name.endsWith(".csv")
            var custCount = 0
            var transCount = 0
            val timestamp = file.lastModified()

            if (!isSql && !isCsv) {
                try {
                    val text = file.readText()
                    val json = JSONObject(text)
                    custCount = json.optInt("customerCount", json.optJSONArray("customers")?.length() ?: 0)
                    transCount = json.optInt("transactionCount", json.optJSONArray("transactions")?.length() ?: 0)
                } catch (_: Exception) {}
            }

            val displayDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
            list.add(
                BackupFileInfo(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    formattedDate = displayDate,
                    timestamp = timestamp,
                    customerCount = custCount,
                    transactionCount = transCount,
                    sizeBytes = file.length(),
                    isSql = isSql,
                    isCsv = isCsv
                )
            )
        }
        list.sortedByDescending { it.timestamp }
    }

    suspend fun readBackupPayload(fileInfo: BackupFileInfo): Result<BackupPayload> = withContext(Dispatchers.IO) {
        try {
            val file = File(fileInfo.filePath)
            if (!file.exists()) return@withContext Result.failure(Exception("Backup file not found"))

            val text = file.readText()
            val json = JSONObject(text)

            // Profile
            var profile = BusinessProfile()
            if (json.has("profile")) {
                val p = json.getJSONObject("profile")
                profile = BusinessProfile(
                    id = "settings",
                    businessName = p.optString("businessName", "My Store"),
                    ownerName = p.optString("ownerName", ""),
                    phone = p.optString("phone", ""),
                    email = p.optString("email", ""),
                    currency = p.optString("currency", "$")
                )
            }

            // Customers
            val customers = mutableListOf<Customer>()
            val custArr = json.optJSONArray("customers")
            if (custArr != null) {
                for (i in 0 until custArr.length()) {
                    val obj = custArr.getJSONObject(i)
                    customers.add(
                        Customer(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            phone = obj.optString("phone", ""),
                            address = obj.optString("address", ""),
                            type = obj.optString("type", "CUSTOMER"),
                            totalGave = obj.optDouble("totalGave", 0.0),
                            totalGot = obj.optDouble("totalGot", 0.0),
                            netBalance = obj.optDouble("netBalance", 0.0),
                            createdAt = Timestamp(Date(obj.optLong("createdAt", System.currentTimeMillis()))),
                            updatedAt = Timestamp(Date(obj.optLong("updatedAt", System.currentTimeMillis())))
                        )
                    )
                }
            }

            // Transactions
            val transactions = mutableListOf<Transaction>()
            val transArr = json.optJSONArray("transactions")
            if (transArr != null) {
                for (i in 0 until transArr.length()) {
                    val obj = transArr.getJSONObject(i)
                    val tDate = obj.optLong("transactionDate", System.currentTimeMillis())
                    transactions.add(
                        Transaction(
                            id = obj.getString("id"),
                            customerId = obj.getString("customerId"),
                            customerName = obj.optString("customerName", ""),
                            type = obj.getString("type"),
                            amount = obj.getDouble("amount"),
                            note = obj.optString("note", ""),
                            billNumber = obj.optString("billNumber", ""),
                            transactionDate = Timestamp(Date(tDate)),
                            createdAt = Timestamp(Date(obj.optLong("createdAt", tDate))),
                            updatedAt = Timestamp(Date(obj.optLong("updatedAt", tDate)))
                        )
                    )
                }
            }

            // Cashbook
            val cashbook = mutableListOf<CashEntry>()
            val cashArr = json.optJSONArray("cashbook")
            if (cashArr != null) {
                for (i in 0 until cashArr.length()) {
                    val obj = cashArr.getJSONObject(i)
                    cashbook.add(
                        CashEntry(
                            id = obj.getString("id"),
                            type = obj.getString("type"),
                            amount = obj.getDouble("amount"),
                            category = obj.optString("category", "General"),
                            note = obj.optString("note", ""),
                            entryDate = Timestamp(Date(obj.optLong("entryDate", System.currentTimeMillis())))
                        )
                    )
                }
            }

            Result.success(
                BackupPayload(
                    profile = profile,
                    customers = customers,
                    transactions = transactions,
                    cashbook = cashbook
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBackup(fileName: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(backupDir, fileName)
        if (file.exists()) file.delete() else false
    }

    suspend fun getFileContent(fileName: String): String = withContext(Dispatchers.IO) {
        val file = File(backupDir, fileName)
        if (file.exists()) file.readText() else ""
    }
}
