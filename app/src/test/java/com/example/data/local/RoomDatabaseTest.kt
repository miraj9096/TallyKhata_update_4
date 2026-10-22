package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.repository.RoomTallyKhataRepository
import com.example.data.model.Customer
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RoomDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var customerDao: CustomerDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var repository: RoomTallyKhataRepository
    private lateinit var cleanRepository: com.example.data.local.repository.CustomerTransactionRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        customerDao = database.customerDao()
        transactionDao = database.transactionDao()
        repository = RoomTallyKhataRepository(database, customerDao, transactionDao)
        cleanRepository = com.example.data.local.repository.CustomerTransactionRepository(database, customerDao, transactionDao)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndReadCustomer() = runBlocking {
        val customer = CustomerEntity(
            id = "c1",
            name = "Bismillah Traders",
            phone = "01700000001",
            address = "Dhaka Sadarghat",
            type = "CUSTOMER",
            totalGave = 5000.0,
            totalGot = 2000.0,
            netBalance = 3000.0
        )
        customerDao.insertCustomer(customer)

        val retrieved = customerDao.getCustomerByIdOnce("c1")
        assertNotNull(retrieved)
        assertEquals("Bismillah Traders", retrieved?.name)
        assertEquals(3000.0, retrieved?.netBalance ?: 0.0, 0.001)

        val all = customerDao.getAllCustomers().first()
        assertEquals(1, all.size)
        assertEquals("c1", all[0].id)
    }

    @Test
    fun insertAndReadTransaction() = runBlocking {
        val customer = CustomerEntity(id = "c2", name = "Rahim Store")
        customerDao.insertCustomer(customer)

        val transaction = TransactionEntity(
            id = "t1",
            customerId = "c2",
            customerName = "Rahim Store",
            type = "GAVE",
            amount = 1500.0,
            note = "Goods delivery"
        )
        transactionDao.insertTransaction(transaction)

        val customerTransactions = transactionDao.getTransactionsForCustomer("c2").first()
        assertEquals(1, customerTransactions.size)
        assertEquals("t1", customerTransactions[0].id)
        assertEquals(1500.0, customerTransactions[0].amount, 0.001)
    }

    @Test
    fun repository_recordTransaction_updatesBalancesAtomically() = runBlocking {
        val customer = CustomerEntity(
            id = "c3",
            name = "Modina General Store",
            totalGave = 0.0,
            totalGot = 0.0,
            netBalance = 0.0
        )
        repository.insertCustomer(customer)

        // 1. Give credit (Udhaar) of 3000
        val gaveTx = TransactionEntity(
            id = "tx1",
            customerId = "c3",
            customerName = "Modina General Store",
            type = "GAVE",
            amount = 3000.0,
            note = "Dry goods wholesale"
        )
        val gaveResult = repository.recordTransaction(gaveTx)
        assertTrue(gaveResult.isSuccess)

        var updatedCustomer = repository.getCustomerByIdOnce("c3")
        assertNotNull(updatedCustomer)
        assertEquals(3000.0, updatedCustomer?.totalGave ?: 0.0, 0.001)
        assertEquals(0.0, updatedCustomer?.totalGot ?: 0.0, 0.001)
        assertEquals(3000.0, updatedCustomer?.netBalance ?: 0.0, 0.001)

        // 2. Customer pays back (Debit/Jama) 1000
        val gotTx = TransactionEntity(
            id = "tx2",
            customerId = "c3",
            customerName = "Modina General Store",
            type = "GOT",
            amount = 1000.0,
            note = "Cash partial payment"
        )
        val gotResult = repository.recordTransaction(gotTx)
        assertTrue(gotResult.isSuccess)

        updatedCustomer = repository.getCustomerByIdOnce("c3")
        assertNotNull(updatedCustomer)
        assertEquals(3000.0, updatedCustomer?.totalGave ?: 0.0, 0.001)
        assertEquals(1000.0, updatedCustomer?.totalGot ?: 0.0, 0.001)
        assertEquals(2000.0, updatedCustomer?.netBalance ?: 0.0, 0.001)
    }

    @Test
    fun repository_deleteTransaction_recalculatesBalances() = runBlocking {
        val customer = CustomerEntity(id = "c4", name = "Al-Amin Store")
        repository.insertCustomer(customer)

        val tx = TransactionEntity(
            id = "tx3",
            customerId = "c4",
            type = "GAVE",
            amount = 800.0
        )
        repository.recordTransaction(tx)

        val beforeDelete = repository.getCustomerByIdOnce("c4")
        assertEquals(800.0, beforeDelete?.totalGave ?: 0.0, 0.001)

        val deleteResult = repository.deleteTransaction("tx3")
        assertTrue(deleteResult.isSuccess)

        val afterDelete = repository.getCustomerByIdOnce("c4")
        assertEquals(0.0, afterDelete?.totalGave ?: 0.0, 0.001)
        assertEquals(0.0, afterDelete?.netBalance ?: 0.0, 0.001)
    }

    @Test
    fun customerDao_searchAndFilterQueries() = runBlocking {
        val c1 = CustomerEntity(id = "c5", name = "Kamal Enterprise", phone = "01811223344", address = "Chittagong Port", netBalance = 500.0)
        val c2 = CustomerEntity(id = "c6", name = "Jamal Supplies", phone = "01922334455", address = "Agrabad CDA", netBalance = -200.0)
        val c3 = CustomerEntity(id = "c7", name = "Hasan Traders", phone = "01733445566", address = "Khatunganj", netBalance = 0.0)

        customerDao.insertCustomers(listOf(c1, c2, c3))

        // Search by name
        val searchByName = customerDao.searchCustomers("Kamal").first()
        assertEquals(1, searchByName.size)
        assertEquals("c5", searchByName[0].id)

        // Search by phone
        val searchByPhone = customerDao.searchCustomers("0192233").first()
        assertEquals(1, searchByPhone.size)
        assertEquals("c6", searchByPhone[0].id)

        // Search by address
        val searchByAddress = customerDao.searchCustomers("Khatunganj").first()
        assertEquals(1, searchByAddress.size)
        assertEquals("c7", searchByAddress[0].id)

        // Balance status queries
        val debtors = customerDao.getDebtors().first()
        assertEquals(1, debtors.size)
        assertEquals("c5", debtors[0].id)

        val creditors = customerDao.getCreditors().first()
        assertEquals(1, creditors.size)
        assertEquals("c6", creditors[0].id)

        val settled = customerDao.getSettledCustomers().first()
        assertEquals(1, settled.size)
        assertEquals("c7", settled[0].id)
    }

    @Test
    fun transactionDao_searchAndFilterQueries() = runBlocking {
        val customer = CustomerEntity(id = "c8", name = "City Hardware")
        customerDao.insertCustomer(customer)

        val t1 = TransactionEntity(
            id = "tx10",
            customerId = "c8",
            customerName = "City Hardware",
            type = "GAVE",
            amount = 1200.0,
            note = "Paint bucket 20L",
            billNumber = "BILL-901",
            transactionDate = 1000000L
        )
        val t2 = TransactionEntity(
            id = "tx11",
            customerId = "c8",
            customerName = "City Hardware",
            type = "GOT",
            amount = 500.0,
            note = "Partial payment cash",
            billNumber = "REC-102",
            transactionDate = 2000000L
        )
        transactionDao.insertTransactions(listOf(t1, t2))

        // Search by note
        val searchNote = transactionDao.searchTransactions("Paint").first()
        assertEquals(1, searchNote.size)
        assertEquals("tx10", searchNote[0].id)

        // Search by bill number
        val searchBill = transactionDao.searchTransactions("REC-102").first()
        assertEquals(1, searchBill.size)
        assertEquals("tx11", searchBill[0].id)

        // Filter by type
        val gaveList = transactionDao.getTransactionsForCustomerByType("c8", "GAVE").first()
        assertEquals(1, gaveList.size)
        assertEquals("tx10", gaveList[0].id)

        val gotList = transactionDao.getTransactionsForCustomerByType("c8", "GOT").first()
        assertEquals(1, gotList.size)
        assertEquals("tx11", gotList[0].id)

        // Filter by date range
        val rangeList = transactionDao.getTransactionsForCustomerBetweenDates("c8", 500000L, 1500000L).first()
        assertEquals(1, rangeList.size)
        assertEquals("tx10", rangeList[0].id)
    }

    @Test
    fun customerTransactionRepository_domainModelCRUD_andSearch() = runBlocking {
        val domainCustomer = Customer(
            id = "c100",
            name = "Meghna Traders",
            phone = "01755554444",
            address = "Kawran Bazar, Dhaka",
            type = "CUSTOMER",
            totalGave = 1000.0,
            totalGot = 200.0,
            netBalance = 800.0
        )

        // Save domain model
        cleanRepository.saveCustomer(domainCustomer)

        // Read single domain model
        val retrieved = cleanRepository.getCustomerByIdOnce("c100")
        assertNotNull(retrieved)
        assertEquals("Meghna Traders", retrieved?.name)
        assertEquals("01755554444", retrieved?.phone)
        assertEquals(800.0, retrieved?.netBalance ?: 0.0, 0.001)

        // Read reactive stream
        val all = cleanRepository.allCustomers.first()
        assertEquals(1, all.size)
        assertEquals("Meghna Traders", all[0].name)

        // Search by query
        val searchResults = cleanRepository.searchCustomers("Kawran").first()
        assertEquals(1, searchResults.size)
        assertEquals("c100", searchResults[0].id)

        // Filter debtors
        val debtors = cleanRepository.getDebtors().first()
        assertEquals(1, debtors.size)
        assertEquals("c100", debtors[0].id)

        // Creditors should be empty
        val creditors = cleanRepository.getCreditors().first()
        assertEquals(0, creditors.size)

        // Totals
        val totalReceivable = cleanRepository.totalReceivable.first()
        assertEquals(1000.0, totalReceivable, 0.001)
        val totalPayable = cleanRepository.totalPayable.first()
        assertEquals(200.0, totalPayable, 0.001)
        val customerCount = cleanRepository.customerCount.first()
        assertEquals(1, customerCount)
    }

    @Test
    fun customerTransactionRepository_recordTransaction_atomicallyUpdatesBalances() = runBlocking {
        val domainCustomer = Customer(
            id = "c101",
            name = "Padma Enterprise",
            phone = "01866665555",
            totalGave = 0.0,
            totalGot = 0.0,
            netBalance = 0.0
        )
        cleanRepository.saveCustomer(domainCustomer)

        // 1. Record GAVE transaction (Credit)
        val gaveTx = Transaction(
            id = "tx101",
            customerId = "c101",
            customerName = "Padma Enterprise",
            type = "GAVE",
            amount = 5000.0,
            note = "Rice 50 bags wholesale",
            billNumber = "INV-001"
        )
        val result1 = cleanRepository.recordTransaction(gaveTx)
        assertTrue(result1.isSuccess)

        // Verify customer totals updated atomically
        val customerAfterGave = cleanRepository.getCustomerByIdOnce("c101")
        assertNotNull(customerAfterGave)
        assertEquals(5000.0, customerAfterGave?.totalGave ?: 0.0, 0.001)
        assertEquals(0.0, customerAfterGave?.totalGot ?: 0.0, 0.001)
        assertEquals(5000.0, customerAfterGave?.netBalance ?: 0.0, 0.001)

        // 2. Record GOT transaction (Payment)
        val gotTx = Transaction(
            id = "tx102",
            customerId = "c101",
            customerName = "Padma Enterprise",
            type = "GOT",
            amount = 2000.0,
            note = "Cash partial payment",
            billNumber = "REC-001"
        )
        val result2 = cleanRepository.recordTransaction(gotTx)
        assertTrue(result2.isSuccess)

        val customerAfterGot = cleanRepository.getCustomerByIdOnce("c101")
        assertNotNull(customerAfterGot)
        assertEquals(5000.0, customerAfterGot?.totalGave ?: 0.0, 0.001)
        assertEquals(2000.0, customerAfterGot?.totalGot ?: 0.0, 0.001)
        assertEquals(3000.0, customerAfterGot?.netBalance ?: 0.0, 0.001)

        // Check transaction streams
        val customerTxs = cleanRepository.getTransactionsForCustomer("c101").first()
        assertEquals(2, customerTxs.size)

        val searchTx = cleanRepository.searchTransactions("Rice").first()
        assertEquals(1, searchTx.size)
        assertEquals("tx101", searchTx[0].id)

        // 3. Delete GOT transaction and verify recalculation
        val deleteResult = cleanRepository.deleteTransaction("tx102")
        assertTrue(deleteResult.isSuccess)

        val customerAfterDelete = cleanRepository.getCustomerByIdOnce("c101")
        assertNotNull(customerAfterDelete)
        assertEquals(5000.0, customerAfterDelete?.totalGave ?: 0.0, 0.001)
        assertEquals(0.0, customerAfterDelete?.totalGot ?: 0.0, 0.001)
        assertEquals(5000.0, customerAfterDelete?.netBalance ?: 0.0, 0.001)
    }

    @Test
    fun customerTransactionRepository_deleteCustomer_cascadesTransactions() = runBlocking {
        val customer = Customer(id = "c102", name = "Jamuna Store")
        cleanRepository.saveCustomer(customer)

        val tx = Transaction(
            id = "tx103",
            customerId = "c102",
            type = "GAVE",
            amount = 1500.0
        )
        cleanRepository.recordTransaction(tx)

        // Confirm both exist
        assertNotNull(cleanRepository.getCustomerByIdOnce("c102"))
        val txsBefore = cleanRepository.getTransactionsForCustomer("c102").first()
        assertEquals(1, txsBefore.size)

        // Delete customer
        val deleteRes = cleanRepository.deleteCustomer("c102")
        assertTrue(deleteRes.isSuccess)

        // Verify customer and transactions are both removed
        assertNull(cleanRepository.getCustomerByIdOnce("c102"))
        val txsAfter = cleanRepository.getTransactionsForCustomer("c102").first()
        assertEquals(0, txsAfter.size)
    }
}

