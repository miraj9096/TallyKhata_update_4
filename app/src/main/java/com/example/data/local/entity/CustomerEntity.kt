package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Customer
import com.google.firebase.Timestamp
import java.util.Date
import java.util.UUID

/**
 * Room Entity representing a Customer or Supplier party in Tally Khata.
 * Tracks customer details, given credit (Due/Udhaar), and received debit/payments (Jama).
 */
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String = "",
    val address: String = "",
    val type: String = "CUSTOMER", // "CUSTOMER" or "SUPPLIER"
    val totalGave: Double = 0.0,   // Total credit/udhaar given to customer
    val totalGot: Double = 0.0,    // Total debit/payment received from customer
    val netBalance: Double = 0.0,  // totalGave - totalGot (>0: You get, <0: You owe)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// Extension converters to/from Firestore models
fun CustomerEntity.toCustomer(): Customer {
    return Customer(
        id = id,
        name = name,
        phone = phone,
        address = address,
        type = type,
        totalGave = totalGave,
        totalGot = totalGot,
        netBalance = netBalance,
        createdAt = Timestamp(Date(createdAt)),
        updatedAt = Timestamp(Date(updatedAt))
    )
}

fun Customer.toEntity(): CustomerEntity {
    return CustomerEntity(
        id = id.ifBlank { UUID.randomUUID().toString() },
        name = name,
        phone = phone,
        address = address,
        type = type,
        totalGave = totalGave,
        totalGot = totalGot,
        netBalance = netBalance,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis(),
        updatedAt = updatedAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}
