package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Transaction entities in Room.
 * Handles credit and debit records tied to customer ledgers.
 */
@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY transactionDate DESC")
    fun getTransactionsForCustomer(customerId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId AND type = :type ORDER BY transactionDate DESC")
    fun getTransactionsForCustomerByType(customerId: String, type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId AND transactionDate BETWEEN :startDate AND :endDate ORDER BY transactionDate DESC")
    fun getTransactionsForCustomerBetweenDates(customerId: String, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE customerName LIKE '%' || :query || '%' 
           OR note LIKE '%' || :query || '%' 
           OR billNumber LIKE '%' || :query || '%'
        ORDER BY transactionDate DESC
    """)
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE customerId = :customerId 
          AND (note LIKE '%' || :query || '%' OR billNumber LIKE '%' || :query || '%')
        ORDER BY transactionDate DESC
    """)
    fun searchTransactionsForCustomer(customerId: String, query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionByIdOnce(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("DELETE FROM transactions WHERE customerId = :customerId")
    suspend fun deleteTransactionsByCustomerId(customerId: String)

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'GAVE'")
    fun getTotalCreditsGave(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'GOT'")
    fun getTotalDebitsGot(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE customerId = :customerId AND type = 'GAVE'")
    suspend fun calculateTotalGaveForCustomer(customerId: String): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE customerId = :customerId AND type = 'GOT'")
    suspend fun calculateTotalGotForCustomer(customerId: String): Double?

    @Query("SELECT COUNT(*) FROM transactions")
    fun getTransactionsCount(): Flow<Int>

    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
