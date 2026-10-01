package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_receipts")
data class OfflineReceiptEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val amount: Double,
    val tipsAmount: Double = 0.0,
    val itemsJson: String,
    val merchantTaxNumber: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, SYNCED, FAILED
    val syncAttempts: Int = 0,
    val lastError: String? = null,
    val fiscalCode: String? = null,
    val taxUrl: String? = null
)
