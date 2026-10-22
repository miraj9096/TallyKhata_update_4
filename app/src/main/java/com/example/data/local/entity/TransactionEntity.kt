package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.Transaction
import com.google.firebase.Timestamp
import java.util.Date
import java.util.UUID

/**
 * Room Entity representing a Ledger Debit/Credit Transaction for a Customer.
 * - type = "GAVE" (Credit / You gave goods/money on due)
 * - type = "GOT"  (Debit / You received payment from customer)
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["transactionDate"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val customerName: String = "",
    val type: String = "GAVE", // "GAVE" (Credit) or "GOT" (Debit)
    val amount: Double = 0.0,
    val note: String = "",
    val billNumber: String = "",
    val transactionDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// Extension converters to/from Firestore models
fun TransactionEntity.toTransaction(): Transaction {
    return Transaction(
        id = id,
        customerId = customerId,
        customerName = customerName,
        type = type,
        amount = amount,
        note = note,
        billNumber = billNumber,
        transactionDate = Timestamp(Date(transactionDate)),
        createdAt = Timestamp(Date(createdAt)),
        updatedAt = Timestamp(Date(updatedAt))
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id.ifBlank { UUID.randomUUID().toString() },
        customerId = customerId,
        customerName = customerName,
        type = type,
        amount = amount,
        note = note,
        billNumber = billNumber,
        transactionDate = transactionDate.toDate().time,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis(),
        updatedAt = updatedAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}
