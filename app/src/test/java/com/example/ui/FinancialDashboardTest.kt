package com.example.ui

import com.example.data.model.Customer
import com.example.data.model.Transaction
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date
import kotlin.math.abs

class FinancialDashboardTest {

    @Test
    fun testFinancialTotalsAndNetBalance() {
        val customers = listOf(
            Customer(id = "1", name = "Alice", totalGave = 12000.0, totalGot = 8000.0, netBalance = 4000.0),
            Customer(id = "2", name = "Bob", totalGave = 5000.0, totalGot = 5000.0, netBalance = 0.0),
            Customer(id = "3", name = "Charlie", totalGave = 1000.0, totalGot = 1500.0, netBalance = -500.0),
            Customer(id = "4", name = "Diana", totalGave = 6000.0, totalGot = 1000.0, netBalance = 5000.0)
        )

        val totalCredits = customers.sumOf { it.totalGave }
        val totalDebits = customers.sumOf { it.totalGot }
        val netBalance = customers.sumOf { it.netBalance }

        assertEquals(24000.0, totalCredits, 0.001)
        assertEquals(15500.0, totalDebits, 0.001)
        assertEquals(8500.0, netBalance, 0.001)

        val totalReceivable = customers.filter { it.netBalance > 0.01 }.sumOf { it.netBalance }
        val totalPayable = customers.filter { it.netBalance < -0.01 }.sumOf { abs(it.netBalance) }

        assertEquals(9000.0, totalReceivable, 0.001) // Alice 4000 + Diana 5000
        assertEquals(500.0, totalPayable, 0.001) // Charlie 500
    }

    @Test
    fun testCollectionRateCalculation() {
        val totalCredits = 20000.0
        val totalDebits = 15000.0

        val collectionRate = if (totalCredits > 0.0) {
            ((totalDebits / totalCredits) * 100.0).coerceIn(0.0, 100.0)
        } else 100.0

        assertEquals(75.0, collectionRate, 0.01)

        // Case when credits are zero
        val zeroCreditRate = if (0.0 > 0.0) 0.0 else 100.0
        assertEquals(100.0, zeroCreditRate, 0.01)
    }

    @Test
    fun testCustomerBalanceRiskSegmentation() {
        val customers = listOf(
            Customer(id = "1", name = "High Due", netBalance = 8000.0),
            Customer(id = "2", name = "Moderate Due 1", netBalance = 3500.0),
            Customer(id = "3", name = "Moderate Due 2", netBalance = 1200.0),
            Customer(id = "4", name = "Low Due", netBalance = 450.0),
            Customer(id = "5", name = "Settled", netBalance = 0.0),
            Customer(id = "6", name = "Advance", netBalance = -300.0)
        )

        val highDue = customers.filter { it.netBalance >= 5000.0 }
        val mediumDue = customers.filter { it.netBalance in 1000.0..4999.99 }
        val lowDue = customers.filter { it.netBalance in 0.01..999.99 }
        val settled = customers.filter { abs(it.netBalance) <= 0.01 }
        val advance = customers.filter { it.netBalance < -0.01 }

        assertEquals(1, highDue.size)
        assertEquals(2, mediumDue.size)
        assertEquals(1, lowDue.size)
        assertEquals(1, settled.size)
        assertEquals(1, advance.size)
    }

    @Test
    fun testTopDebtorsRanking() {
        val customers = listOf(
            Customer(id = "1", name = "Party A", netBalance = 2000.0),
            Customer(id = "2", name = "Party B", netBalance = 15000.0),
            Customer(id = "3", name = "Party C", netBalance = 8000.0),
            Customer(id = "4", name = "Party D", netBalance = -500.0)
        )

        val debtors = customers.filter { it.netBalance > 0.01 }
            .sortedByDescending { it.netBalance }
            .take(5)

        assertEquals(3, debtors.size)
        assertEquals("Party B", debtors[0].name)
        assertEquals("Party C", debtors[1].name)
        assertEquals("Party A", debtors[2].name)
    }

    @Test
    fun testTimeframeTransactionFiltering() {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - 24 * 60 * 60 * 1000L
        val fortyDaysAgo = now - 40L * 24 * 60 * 60 * 1000L

        val transactions = listOf(
            Transaction(id = "t1", amount = 500.0, type = "GAVE", transactionDate = Timestamp(Date(now))),
            Transaction(id = "t2", amount = 300.0, type = "GOT", transactionDate = Timestamp(Date(oneDayAgo))),
            Transaction(id = "t3", amount = 1000.0, type = "GAVE", transactionDate = Timestamp(Date(fortyDaysAgo)))
        )

        val thirtyDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -30)
        }.timeInMillis

        val last30DaysTx = transactions.filter { it.transactionDate.toDate().time >= thirtyDaysAgo }

        assertEquals(2, last30DaysTx.size)
        assertTrue(last30DaysTx.any { it.id == "t1" })
        assertTrue(last30DaysTx.any { it.id == "t2" })
    }
}
