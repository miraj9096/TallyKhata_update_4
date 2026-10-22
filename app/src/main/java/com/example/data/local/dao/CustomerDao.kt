package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Customer entities in Room.
 * Provides reactive Flow streams and suspend CRUD operations.
 */
@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers ORDER BY updatedAt DESC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerById(id: String): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerByIdOnce(id: String): CustomerEntity?

    @Query("""
        SELECT * FROM customers 
        WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE type = :type ORDER BY updatedAt DESC")
    fun getCustomersByType(type: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE netBalance > 0.01 ORDER BY netBalance DESC")
    fun getDebtors(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE netBalance < -0.01 ORDER BY netBalance ASC")
    fun getCreditors(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE ABS(netBalance) <= 0.01 ORDER BY updatedAt DESC")
    fun getSettledCustomers(): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: String)

    @Query("""
        UPDATE customers 
        SET totalGave = :totalGave, totalGot = :totalGot, netBalance = :netBalance, updatedAt = :updatedAt 
        WHERE id = :customerId
    """)
    suspend fun updateCustomerTotals(
        customerId: String,
        totalGave: Double,
        totalGot: Double,
        netBalance: Double,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT SUM(totalGave) FROM customers")
    fun getTotalReceivable(): Flow<Double?>

    @Query("SELECT SUM(totalGot) FROM customers")
    fun getTotalPayable(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM customers")
    fun getCustomersCount(): Flow<Int>

    @Query("SELECT * FROM customers ORDER BY updatedAt DESC")
    suspend fun getAllCustomersOnce(): List<CustomerEntity>

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()
}
