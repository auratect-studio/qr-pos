package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.OfflineReceiptDao
import com.example.data.db.OfflineReceiptEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfflineReceiptQueueTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: OfflineReceiptDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.offlineReceiptDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndRetrievePendingReceipts() = runBlocking {
        val receipt1 = OfflineReceiptEntity(
            id = "rec-001",
            transactionId = "tx-001",
            amount = 150.0,
            tipsAmount = 15.0,
            itemsJson = """[{"name":"Кава","code":"ITEM-1","price":150.0,"quantity":1}]""",
            merchantTaxNumber = "3123456789",
            createdAt = 1000L,
            status = "PENDING"
        )
        val receipt2 = OfflineReceiptEntity(
            id = "rec-002",
            transactionId = "tx-002",
            amount = 300.0,
            tipsAmount = 0.0,
            itemsJson = "Послуги",
            merchantTaxNumber = "3123456789",
            createdAt = 2000L,
            status = "PENDING"
        )

        dao.insertReceipt(receipt1)
        dao.insertReceipt(receipt2)

        val pending = dao.getPendingReceipts()
        assertEquals(2, pending.size)
        assertEquals("rec-001", pending[0].id)
        assertEquals("rec-002", pending[1].id)

        val count = dao.getPendingCount()
        assertEquals(2, count)

        val countFlowValue = dao.getPendingCountFlow().first()
        assertEquals(2, countFlowValue)
    }

    @Test
    fun testMarkAsSyncedRemovesFromPending() = runBlocking {
        val receipt = OfflineReceiptEntity(
            id = "rec-sync-test",
            transactionId = "tx-sync-test",
            amount = 220.0,
            tipsAmount = 20.0,
            itemsJson = "2x Американо",
            merchantTaxNumber = "3123456789",
            status = "PENDING"
        )

        dao.insertReceipt(receipt)
        assertEquals(1, dao.getPendingCount())

        // Mark as synced with fiscal code
        dao.markAsSynced(
            id = "rec-sync-test",
            fiscalCode = "ЧЕК-987654321",
            taxUrl = "https://check.gov.ua/check/987654321"
        )

        // Pending count is now 0
        val pendingAfter = dao.getPendingReceipts()
        assertTrue(pendingAfter.isEmpty())
        assertEquals(0, dao.getPendingCount())

        // Verified record exists with status SYNCED
        val all = dao.getAllOfflineReceiptsFlow().first()
        assertEquals(1, all.size)
        val synced = all.first()
        assertEquals("SYNCED", synced.status)
        assertEquals("ЧЕК-987654321", synced.fiscalCode)
        assertEquals("https://check.gov.ua/check/987654321", synced.taxUrl)
    }

    @Test
    fun testMarkAsFailedStoresError() = runBlocking {
        val receipt = OfflineReceiptEntity(
            id = "rec-fail-test",
            transactionId = "tx-fail-test",
            amount = 100.0,
            tipsAmount = 0.0,
            itemsJson = "Товар",
            merchantTaxNumber = "3123456789",
            status = "PENDING"
        )

        dao.insertReceipt(receipt)
        dao.markAsFailed("rec-fail-test", "HTTP 401: Unauthorized Checkbox Token")

        val all = dao.getAllOfflineReceiptsFlow().first()
        val failed = all.first { it.id == "rec-fail-test" }
        assertEquals("FAILED", failed.status)
        assertEquals("HTTP 401: Unauthorized Checkbox Token", failed.lastError)
    }

    @Test
    fun testPurgeOldSyncedPreservesPending() = runBlocking {
        val oldSynced = OfflineReceiptEntity(
            id = "old-synced",
            transactionId = "tx-old",
            amount = 50.0,
            tipsAmount = 0.0,
            itemsJson = "Старий чек",
            merchantTaxNumber = "123",
            createdAt = 1000L,
            status = "SYNCED"
        )
        val newPending = OfflineReceiptEntity(
            id = "new-pending",
            transactionId = "tx-new",
            amount = 80.0,
            tipsAmount = 0.0,
            itemsJson = "Новий чек",
            merchantTaxNumber = "123",
            createdAt = 5000L,
            status = "PENDING"
        )

        dao.insertReceipt(oldSynced)
        dao.insertReceipt(newPending)

        // Purge synced older than 3000L
        dao.purgeOldSynced(3000L)

        val remaining = dao.getAllOfflineReceiptsFlow().first()
        assertEquals(1, remaining.size)
        assertEquals("new-pending", remaining[0].id)
    }
}
