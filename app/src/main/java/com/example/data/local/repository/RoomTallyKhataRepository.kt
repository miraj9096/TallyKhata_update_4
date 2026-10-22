package com.example.data.local.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Unified Room repository for Tally Khata managing Customers, Debit/Credit Transactions,
 * and maintaining mathematical ledger consistency locally.
 */
class RoomTallyKhataRepository(
    private val database: AppDatabase,
    private val customerDao: CustomerDao = database.customerDao(),
    private val transactionDao: TransactionDao = database.transactionDao()
) {
    constructor(context: Context) : this(AppDatabase.getInstance(context))

    // --- Customers ---

    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    fun getCustomerById(id: String): Flow<CustomerEntity?> = customerDao.getCustomerById(id)

    suspend fun getCustomerByIdOnce(id: String): CustomerEntity? = customerDao.getCustomerByIdOnce(id)

    fun searchCustomers(query: String): Flow<List<CustomerEntity>> = customerDao.searchCustomers(query)

    suspend fun insertCustomer(customer: CustomerEntity) {
        customerDao.insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: CustomerEntity) {
        customerDao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customerId: String) {
        database.withTransaction {
            transactionDao.deleteTransactionsByCustomerId(customerId)
            customerDao.deleteCustomerById(customerId)
        }
    }

    // --- Transactions ---

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsForCustomer(customerId: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsForCustomer(customerId)

    /**
     * Records a debit or credit transaction and recalculates the customer's balance atomically.
     * - type = "GAVE": increases totalGave (You gave credit / Due)
     * - type = "GOT": increases totalGot (You received payment / Jama)
     */
    suspend fun recordTransaction(transaction: TransactionEntity): Result<Unit> {
        return try {
            database.withTransaction {
                val customer = customerDao.getCustomerByIdOnce(transaction.customerId)
                    ?: throw IllegalArgumentException("Customer with ID ${transaction.customerId} not found")

                val newTotalGave = if (transaction.type == "GAVE") {
                    customer.totalGave + transaction.amount
                } else {
                    customer.totalGave
                }

                val newTotalGot = if (transaction.type == "GOT") {
                    customer.totalGot + transaction.amount
                } else {
                    customer.totalGot
                }

                val newNetBalance = newTotalGave - newTotalGot

                transactionDao.insertTransaction(transaction)
                customerDao.updateCustomerTotals(
                    customerId = customer.id,
                    totalGave = newTotalGave,
                    totalGot = newTotalGot,
                    netBalance = newNetBalance,
                    updatedAt = System.currentTimeMillis()
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a transaction and restores the customer's credit/debit balances atomically.
     */
    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return try {
            database.withTransaction {
                val transaction = transactionDao.getTransactionByIdOnce(transactionId)
                    ?: throw IllegalArgumentException("Transaction with ID $transactionId not found")

                val customer = customerDao.getCustomerByIdOnce(transaction.customerId)
                    ?: throw IllegalArgumentException("Customer with ID ${transaction.customerId} not found")

                val newTotalGave = if (transaction.type == "GAVE") {
                    (customer.totalGave - transaction.amount).coerceAtLeast(0.0)
                } else {
                    customer.totalGave
                }

                val newTotalGot = if (transaction.type == "GOT") {
                    (customer.totalGot - transaction.amount).coerceAtLeast(0.0)
                } else {
                    customer.totalGot
                }

                val newNetBalance = newTotalGave - newTotalGot

                transactionDao.deleteTransactionById(transactionId)
                customerDao.updateCustomerTotals(
                    customerId = customer.id,
                    totalGave = newTotalGave,
                    totalGot = newTotalGot,
                    netBalance = newNetBalance,
                    updatedAt = System.currentTimeMillis()
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Batch and Offline Sync Operations ---
    suspend fun getAllCustomersOnce(): List<CustomerEntity> = customerDao.getAllCustomersOnce()

    suspend fun getAllTransactionsOnce(): List<TransactionEntity> = transactionDao.getAllTransactionsOnce()

    suspend fun insertCustomers(customers: List<CustomerEntity>) = customerDao.insertCustomers(customers)

    suspend fun insertTransactions(transactions: List<TransactionEntity>) = transactionDao.insertTransactions(transactions)

    suspend fun clearAllData() {
        database.withTransaction {
            transactionDao.deleteAllTransactions()
            customerDao.deleteAllCustomers()
        }
    }

    // --- Summary Metrics ---

    val totalReceivable: Flow<Double?> = customerDao.getTotalReceivable()
    val totalPayable: Flow<Double?> = customerDao.getTotalPayable()
    val totalCreditsGave: Flow<Double?> = transactionDao.getTotalCreditsGave()
    val totalDebitsGot: Flow<Double?> = transactionDao.getTotalDebitsGot()
}
