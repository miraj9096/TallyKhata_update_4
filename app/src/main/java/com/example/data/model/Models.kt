package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val type: String = "CUSTOMER", // "CUSTOMER" or "SUPPLIER"
    val totalGave: Double = 0.0,
    val totalGot: Double = 0.0,
    val netBalance: Double = 0.0, // totalGave - totalGot
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Transaction(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val type: String = "GAVE", // "GAVE" (Credit / Udhaar) or "GOT" (Debit / Collection)
    val amount: Double = 0.0,
    val note: String = "",
    val billNumber: String = "",
    val transactionDate: Timestamp = Timestamp.now(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CashEntry(
    @DocumentId val id: String = "",
    val type: String = "CASH_IN", // "CASH_IN" or "CASH_OUT"
    val amount: Double = 0.0,
    val category: String = "General",
    val note: String = "",
    val entryDate: Timestamp = Timestamp.now(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BusinessProfile(
    @DocumentId val id: String = "settings",
    val businessName: String = "My Store",
    val ownerName: String = "",
    val phone: String = "",
    val email: String = "",
    val currency: String = "$",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
