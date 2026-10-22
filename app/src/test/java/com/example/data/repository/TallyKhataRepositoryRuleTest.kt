package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Customer
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TallyKhataRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createCustomer_authenticatedUser_success() = runBlocking {
        signInTestUser("testuser1@example.com")
        val repository = TallyKhataRepository(firestore)

        val customer = Customer(
            name = "Test Merchant",
            phone = "01812345678",
            address = "Dhanmondi, Dhaka",
            type = "CUSTOMER",
            totalGave = 500.0,
            totalGot = 0.0,
            netBalance = 500.0
        )

        val result = withTimeout(10000L) {
            repository.createCustomer(customer)
        }

        assertTrue(result.isSuccess)
        val customerId = result.getOrThrow()
        assertNotNull(customerId)
        assertTrue(customerId.isNotEmpty())

        val customers = withTimeout(10000L) {
            repository.observeCustomers().first { list -> list.any { it.name == "Test Merchant" } }
        }
        val fetched = customers.first { it.name == "Test Merchant" }
        assertEquals(500.0, fetched.totalGave, 0.01)
        assertEquals(500.0, fetched.netBalance, 0.01)
    }

    @Test
    fun recordTransaction_updatesCustomerBalance() = runBlocking {
        signInTestUser("testuser2@example.com")
        val repository = TallyKhataRepository(firestore)

        val customer = Customer(
            name = "Karim Mia",
            type = "CUSTOMER",
            totalGave = 0.0,
            totalGot = 0.0,
            netBalance = 0.0
        )
        val custResult = withTimeout(10000L) { repository.createCustomer(customer) }
        val customerId = custResult.getOrThrow()
        val createdCust = customer.copy(id = customerId)

        val transaction = Transaction(
            customerId = customerId,
            customerName = "Karim Mia",
            type = "GAVE",
            amount = 1200.0,
            note = "Goods on credit"
        )

        val transResult = withTimeout(10000L) {
            repository.recordTransaction(transaction, createdCust)
        }
        assertTrue(transResult.isSuccess)

        val customers = withTimeout(10000L) {
            repository.observeCustomers().first { list -> list.any { it.id == customerId && it.totalGave == 1200.0 } }
        }
        val updatedCust = customers.first { it.id == customerId }
        assertEquals(1200.0, updatedCust.totalGave, 0.01)
        assertEquals(1200.0, updatedCust.netBalance, 0.01)
    }
}
