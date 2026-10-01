package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineReceiptDao {

    @Query("SELECT * FROM offline_receipts WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingReceipts(): List<OfflineReceiptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: OfflineReceiptEntity)

    @Update
    suspend fun updateReceipt(receipt: OfflineReceiptEntity)

    @Query("UPDATE offline_receipts SET status = 'SYNCED', fiscalCode = :fiscalCode, taxUrl = :taxUrl WHERE id = :id")
    suspend fun markAsSynced(id: String, fiscalCode: String? = null, taxUrl: String? = null)

    @Query("UPDATE offline_receipts SET status = 'FAILED', lastError = :error WHERE id = :id")
    suspend fun markAsFailed(id: String, error: String)

    @Query("SELECT COUNT(*) FROM offline_receipts WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM offline_receipts WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("SELECT * FROM offline_receipts ORDER BY createdAt DESC LIMIT 50")
    fun getAllOfflineReceiptsFlow(): Flow<List<OfflineReceiptEntity>>

    @Query("DELETE FROM offline_receipts WHERE status = 'SYNCED' AND createdAt < :olderThanTimestamp")
    suspend fun purgeOldSynced(olderThanTimestamp: Long)
}
