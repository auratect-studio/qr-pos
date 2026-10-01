package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.TransactionRecord
import com.example.model.TransactionStatus
import com.example.model.UkrainianBank

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val feeAmount: Double,
    val netAmount: Double,
    val bankId: String,
    val recipientName: String,
    val iban: String,
    val purpose: String,
    val itemsSummary: String,
    val timestamp: Long,
    val status: String = TransactionStatus.PAID.name,
    val tipsAmount: Double = 0.0
) {
    fun toModel(): TransactionRecord {
        val bank = UkrainianBank.entries.firstOrNull { it.id == bankId } ?: UkrainianBank.MONOBANK
        val txStatus = try {
            TransactionStatus.valueOf(status)
        } catch (_: Exception) {
            TransactionStatus.PAID
        }
        return TransactionRecord(
            id = id,
            amount = amount,
            feeAmount = feeAmount,
            netAmount = netAmount,
            bank = bank,
            recipientName = recipientName,
            iban = iban,
            purpose = purpose,
            itemsSummary = itemsSummary,
            timestamp = timestamp,
            status = txStatus,
            tipsAmount = tipsAmount
        )
    }

    companion object {
        fun fromModel(model: TransactionRecord): TransactionEntity = TransactionEntity(
            id = model.id,
            amount = model.amount,
            feeAmount = model.feeAmount,
            netAmount = model.netAmount,
            bankId = model.bank.id,
            recipientName = model.recipientName,
            iban = model.iban,
            purpose = model.purpose,
            itemsSummary = model.itemsSummary,
            timestamp = model.timestamp,
            status = model.status.name,
            tipsAmount = model.tipsAmount
        )
    }
}