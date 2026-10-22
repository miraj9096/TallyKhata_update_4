package com.example.data.local.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.toCustomer
import com.example.data.local.entity.toEntity
import com.example.data.local.entity.toTransaction
import com.example.data.model.Customer
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Clean Architecture Repository interface for Customer operations.
 * Decouples the presentation layer (ViewModel) from the database layer (Room DAOs/Entities),
 * delivering domain models and reactive streams.
 */
interface CustomerRepository {
    /** Reactive stream of all customers as domain models. */
    val allCustomers: Flow<List<Customer>>

    /** Reactive stream of customers directly as Room entities for low-level sync. */
    val customerEntities: Flow<List<CustomerEntity>>

    /** Observes a specific customer by ID as a reactive domain model stream. */
    fun getCustomerById(id: String): Flow<Customer?>

    /** Reads a customer once by ID. */
    suspend fun getCustomerByIdOnce(id: String): Customer?

    /** Searches customers by name, phone, or address. */
    fun searchCustomers(query: String): Flow<List<Customer>>

    /** Filters customers by type (CUSTOMER or SUPPLIER). */
    fun getCustomersByType(type: String): Flow<List<Customer>>

    /** Customers with outstanding dues (>0, "You'll Get"). */
    fun getDebtors(): Flow<List<Customer>>

    /** Customers with advance payments (<0, "You'll Give"). */
    fun getCreditors(): Flow<List<Customer>>

    /** Settled customers with balance == 0. */
    fun getSettledCustomers(): Flow<List<Customer>>

    /** Inserts or replaces a customer domain model. */
    suspend fun saveCustomer(customer: Customer)

    /** Updates an existing customer. */
    suspend fun updateCustomer(customer: Customer)

    /** Deletes a customer and cascades deletion to their transactions. */
    suspend fun deleteCustomer(customerId: String): Result<Unit>

    /** Total outstanding receivable sum across all debtors. */
    val totalReceivable: Flow<Double>

    /** Total payable advance sum across all creditors. */
    val totalPayable: Flow<Double>

    /** Total count of registered parties. */
    val customerCount: Flow<Int>
}

/**
 * Clean Architecture Repository interface for Transaction entries.
 * Manages debit/credit ledger records and ledger calculations.
 */
interface TransactionRepository {
    /** Reactive stream of all ledger entries across all customers. */
    val allTransactions: Flow<List<Transaction>>

    /** Reactive stream of transactions as Room entities. */
    val transactionEntities: Flow<List<TransactionEntity>>

    /** Observes all transactions for a specific customer. */
    fun getTransactionsForCustomer(customerId: String): Flow<List<Transaction>>

    /** Observes a specific transaction by ID. */
    fun getTransactionById(id: String): Flow<Transaction?>

    /** Reads a single transaction entry once. */
    suspend fun getTransactionByIdOnce(id: String): Transaction?

    /** Searches transactions globally across party name, notes, and bill numbers. */
    fun searchTransactions(query: String): Flow<List<Transaction>>

    /** Searches transactions within a specific customer's ledger by note or bill number. */
    fun searchTransactionsForCustomer(customerId: String, query: String): Flow<List<Transaction>>

    /** Filters a customer's ledger entries by type ("GAVE" or "GOT"). */
    fun getTransactionsForCustomerByType(customerId: String, type: String): Flow<List<Transaction>>

    /** Filters a customer's ledger entries within a date range. */
    fun getTransactionsForCustomerBetweenDates(customerId: String, startDate: Long, endDate: Long): Flow<List<Transaction>>

    /**
     * Atomically records a debit or credit entry in Room and updates the customer's ledger totals.
     * - "GAVE": increases totalGave (Credit/Udhaar)
     * - "GOT": increases totalGot (Payment/Jama)
     */
    suspend fun recordTransaction(transaction: Transaction): Result<Unit>

    /**
     * Atomically deletes a transaction entry and recalculates the customer's balance.
     */
    suspend fun deleteTransaction(transactionId: String): Result<Unit>

    /** Total credit given (Udhaar volume). */
    val totalCreditsGave: Flow<Double>

    /** Total payment collected (Jama volume). */
    val totalDebitsGot: Flow<Double>

    /** Total count of ledger transactions. */
    val transactionCount: Flow<Int>
}

/**
 * Unified Ledger Repository interface combining Customer and Transaction abstractions.
 */
interface LedgerRepository : CustomerRepository, TransactionRepository {
    suspend fun clearAllLocalData()
    suspend fun bulkInsertCustomers(customers: List<Customer>)
    suspend fun bulkInsertTransactions(transactions: List<Transaction>)
}

/**
 * Production-ready Room Repository class for Customer and Transaction entities.
 * Handles the abstraction between the Room DAOs and the ViewModel layer,
 * enforcing clean architecture, domain model separation, and atomic transactions.
 */
class CustomerTransactionRepository(
    private val database: AppDatabase,
    private val customerDao: CustomerDao = database.customerDao(),
    private val transactionDao: TransactionDao = database.transactionDao()
) : LedgerRepository {

    constructor(context: Context) : this(AppDatabase.getInstance(context))

    // =========================================================================
    // Customer Repository Implementation
    // =========================================================================

    override val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers().map { entities ->
        entities.map { it.toCustomer() }
    }

    override val customerEntities: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    override fun getCustomerById(id: String): Flow<Customer?> = customerDao.getCustomerById(id).map { it?.toCustomer() }

    override suspend fun getCustomerByIdOnce(id: String): Customer? = customerDao.getCustomerByIdOnce(id)?.toCustomer()

    override fun searchCustomers(query: String): Flow<List<Customer>> = customerDao.searchCustomers(query).map { entities ->
        entities.map { it.toCustomer() }
    }

    override fun getCustomersByType(type: String): Flow<List<Customer>> = customerDao.getCustomersByType(type).map { entities ->
        entities.map { it.toCustomer() }
    }

    override fun getDebtors(): Flow<List<Customer>> = customerDao.getDebtors().map { entities ->
        entities.map { it.toCustomer() }
    }

    override fun getCreditors(): Flow<List<Customer>> = customerDao.getCreditors().map { entities ->
        entities.map { it.toCustomer() }
    }

    override fun getSettledCustomers(): Flow<List<Customer>> = customerDao.getSettledCustomers().map { entities ->
        entities.map { it.toCustomer() }
    }

    override suspend fun saveCustomer(customer: Customer) {
        customerDao.insertCustomer(customer.toEntity())
    }

    override suspend fun updateCustomer(customer: Customer) {
        customerDao.updateCustomer(customer.toEntity())
    }

    override suspend fun deleteCustomer(customerId: String): Result<Unit> {
        return try {
            database.withTransaction {
                transactionDao.deleteTransactionsByCustomerId(customerId)
                customerDao.deleteCustomerById(customerId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override val totalReceivable: Flow<Double> = customerDao.getTotalReceivable().map { it ?: 0.0 }

    override val totalPayable: Flow<Double> = customerDao.getTotalPayable().map { it ?: 0.0 }

    override val customerCount: Flow<Int> = customerDao.getCustomersCount()

    // =========================================================================
    // Transaction Repository Implementation
    // =========================================================================

    override val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions().map { entities ->
        entities.map { it.toTransaction() }
    }

    override val transactionEntities: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    override fun getTransactionsForCustomer(customerId: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsForCustomer(customerId).map { entities ->
            entities.map { it.toTransaction() }
        }

    override fun getTransactionById(id: String): Flow<Transaction?> = transactionDao.getTransactionById(id).map { it?.toTransaction() }

    override suspend fun getTransactionByIdOnce(id: String): Transaction? = transactionDao.getTransactionByIdOnce(id)?.toTransaction()

    override fun searchTransactions(query: String): Flow<List<Transaction>> = transactionDao.searchTransactions(query).map { entities ->
        entities.map { it.toTransaction() }
    }

    override fun searchTransactionsForCustomer(customerId: String, query: String): Flow<List<Transaction>> =
        transactionDao.searchTransactionsForCustomer(customerId, query).map { entities ->
            entities.map { it.toTransaction() }
        }

    override fun getTransactionsForCustomerByType(customerId: String, type: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsForCustomerByType(customerId, type).map { entities ->
            entities.map { it.toTransaction() }
        }

    override fun getTransactionsForCustomerBetweenDates(
        customerId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>> =
        transactionDao.getTransactionsForCustomerBetweenDates(customerId, startDate, endDate).map { entities ->
            entities.map { it.toTransaction() }
        }

    override suspend fun recordTransaction(transaction: Transaction): Result<Unit> {
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

                transactionDao.insertTransaction(transaction.toEntity())
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

    override suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return try {
            database.withTransaction {
                val txEntity = transactionDao.getTransactionByIdOnce(transactionId)
                    ?: throw IllegalArgumentException("Transaction with ID $transactionId not found")

                val customer = customerDao.getCustomerByIdOnce(txEntity.customerId)
                    ?: throw IllegalArgumentException("Customer with ID ${txEntity.customerId} not found")

                val newTotalGave = if (txEntity.type == "GAVE") {
                    (customer.totalGave - txEntity.amount).coerceAtLeast(0.0)
                } else {
                    customer.totalGave
                }

                val newTotalGot = if (txEntity.type == "GOT") {
                    (customer.totalGot - txEntity.amount).coerceAtLeast(0.0)
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

    override val totalCreditsGave: Flow<Double> = transactionDao.getTotalCreditsGave().map { it ?: 0.0 }

    override val totalDebitsGot: Flow<Double> = transactionDao.getTotalDebitsGot().map { it ?: 0.0 }

    override val transactionCount: Flow<Int> = transactionDao.getTransactionsCount()

    // =========================================================================
    // Bulk and Maintenance Operations
    // =========================================================================

    override suspend fun clearAllLocalData() {
        database.withTransaction {
            transactionDao.deleteAllTransactions()
            customerDao.deleteAllCustomers()
        }
    }

    override suspend fun bulkInsertCustomers(customers: List<Customer>) {
        customerDao.insertCustomers(customers.map { it.toEntity() })
    }

    override suspend fun bulkInsertTransactions(transactions: List<Transaction>) {
        transactionDao.insertTransactions(transactions.map { it.toEntity() })
    }
}
