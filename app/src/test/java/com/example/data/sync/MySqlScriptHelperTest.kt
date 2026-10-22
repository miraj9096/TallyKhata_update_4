package com.example.data.sync

import com.example.data.model.BusinessProfile
import com.example.data.model.CashEntry
import com.example.data.model.Customer
import com.example.data.model.Transaction
import com.google.firebase.Timestamp
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class MySqlScriptHelperTest {

    @Test
    fun testGenerateSqlDump_containsTablesAndData() {
        val profile = BusinessProfile(
            businessName = "Test Mart",
            ownerName = "Rahim",
            phone = "12345",
            email = "rahim@test.com",
            currency = "$"
        )
        val customers = listOf(
            Customer(
                id = "cust_1",
                name = "Ahmed Store",
                phone = "01700000000",
                address = "Dhaka",
                type = "CUSTOMER",
                totalGave = 500.0,
                totalGot = 200.0,
                netBalance = 300.0,
                createdAt = Timestamp(Date()),
                updatedAt = Timestamp(Date())
            )
        )
        val transactions = listOf(
            Transaction(
                id = "tx_1",
                customerId = "cust_1",
                customerName = "Ahmed Store",
                type = "GAVE",
                amount = 500.0,
                note = "Rice & Flour supply",
                billNumber = "INV-001",
                transactionDate = Timestamp(Date())
            )
        )
        val cashEntries = listOf(
            CashEntry(
                id = "cash_1",
                type = "CASH_IN",
                amount = 1000.0,
                category = "Sales",
                note = "Counter sales",
                entryDate = Timestamp(Date())
            )
        )

        val sql = MySqlScriptHelper.generateSqlDump(
            storeId = "store_test",
            customers = customers,
            transactions = transactions,
            cashEntries = cashEntries,
            profile = profile
        )

        assertNotNull(sql)
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `tally_customers`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `tally_transactions`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `tally_cashbook`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `tally_profile`"))
        assertTrue(sql.contains("Ahmed Store"))
        assertTrue(sql.contains("INV-001"))
        assertTrue(sql.contains("Rice & Flour supply"))
        assertTrue(sql.contains("Counter sales"))
    }

    @Test
    fun testGeneratePhpMyAdminImportSql_containsCleanDdl() {
        val sql = MySqlScriptHelper.generatePhpMyAdminImportSql("my_store_123")
        assertNotNull(sql)
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `tally_profile`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `tally_customers`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `tally_transactions`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `tally_cashbook`"))
        assertTrue(sql.contains("my_store_123"))
        assertTrue(sql.contains("SET SQL_MODE = \"NO_AUTO_VALUE_ON_ZERO\""))
        assertTrue(sql.contains("utf8mb4"))
    }

    @Test
    fun testGetSamplePhpBackendScript_containsEndpointsAndAuth() {
        val script = MySqlScriptHelper.getSamplePhpBackendScript("CUSTOM_SECRET_KEY")
        assertNotNull(script)
        assertTrue(script.contains("CUSTOM_SECRET_KEY"))
        assertTrue(script.contains("case 'push':"))
        assertTrue(script.contains("case 'pull':"))
        assertTrue(script.contains("case 'ping':"))
        assertTrue(script.contains("case 'save_customer':"))
        assertTrue(script.contains("case 'save_transaction':"))
        assertTrue(script.contains("case 'delete_customer':"))
        assertTrue(script.contains("case 'delete_transaction':"))
        assertTrue(script.contains("case 'save_cash':"))
        assertTrue(script.contains("case 'save_profile':"))
        assertTrue(script.contains("tally_customers"))
        assertTrue(script.contains("tally_transactions"))
    }
}
