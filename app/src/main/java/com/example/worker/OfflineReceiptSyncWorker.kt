package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.db.AppDatabase
import com.example.data.db.OfflineReceiptEntity
import com.example.data.preferences.MerchantPreferencesStore
import com.example.data.security.SecureTokenStore
import com.example.network.CheckboxApiService
import com.example.network.CheckboxGoodItem
import com.example.network.CheckboxPayment
import com.example.network.CheckboxReceiptRequest
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class OfflineReceiptSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(applicationContext)
        val dao = database.offlineReceiptDao()

        val pending = dao.getPendingReceipts()
        if (pending.isEmpty()) {
            Log.d(TAG, "No pending offline receipts to synchronize")
            return@withContext Result.success()
        }

        Log.i(TAG, "Found ${pending.size} pending offline receipts to synchronize")

        // 1. Retrieve Checkbox API key from SecureTokenStore or MerchantPreferencesStore
        val tokenStore = SecureTokenStore(applicationContext)
        var apiKey = tokenStore.getCheckboxToken().trim()
        if (apiKey.isBlank()) {
            val prefs = MerchantPreferencesStore(applicationContext).getSavedProfile()
            apiKey = prefs?.checkboxApiKey?.trim() ?: ""
        }

        if (apiKey.isBlank()) {
            Log.w(TAG, "Checkbox API key is not configured. Keeping ${pending.size} receipts buffered offline.")
            return@withContext Result.success()
        }

        val bearer = if (apiKey.startsWith("Bearer ", ignoreCase = true)) apiKey else "Bearer $apiKey"
        val checkboxApi = CheckboxApiService.create()

        var hasFailures = false
        var syncedCount = 0

        for (receipt in pending) {
            try {
                val request = buildCheckboxRequest(receipt)
                val response = checkboxApi.createSellReceipt(bearerToken = bearer, request = request)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val fiscalCode = body.fiscalCode ?: body.id.take(12)
                    val taxUrl = body.taxUrl ?: "https://check.gov.ua/"

                    dao.markAsSynced(receipt.id, fiscalCode, taxUrl)
                    syncedCount++
                    Log.i(TAG, "Receipt ${receipt.id} successfully fiscalized: $fiscalCode")
                } else {
                    val code = response.code()
                    val err = response.errorBody()?.string()?.take(200) ?: "HTTP $code"
                    Log.e(TAG, "Checkbox API returned $code for receipt ${receipt.id}: $err")

                    val newAttempts = receipt.syncAttempts + 1
                    val newStatus = if (newAttempts >= 5) "FAILED" else "PENDING"
                    dao.updateReceipt(
                        receipt.copy(
                            syncAttempts = newAttempts,
                            status = newStatus,
                            lastError = "HTTP $code: $err"
                        )
                    )
                    hasFailures = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during sync of receipt ${receipt.id}: ${e.message}", e)
                val newAttempts = receipt.syncAttempts + 1
                dao.updateReceipt(
                    receipt.copy(
                        syncAttempts = newAttempts,
                        lastError = e.message?.take(100) ?: "Network error"
                    )
                )
                hasFailures = true
            }
        }

        if (syncedCount > 0) {
            try {
                NotificationHelper.showOfflineSyncSuccessNotification(
                    context = applicationContext,
                    syncedCount = syncedCount
                )
            } catch (_: Exception) {}
        }

        if (hasFailures) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    private fun buildCheckboxRequest(receipt: OfflineReceiptEntity): CheckboxReceiptRequest {
        val goods = mutableListOf<CheckboxGoodItem>()

        var parsedFromJson = false
        val trimmedJson = receipt.itemsJson.trim()
        if (trimmedJson.startsWith("[") && trimmedJson.endsWith("]")) {
            try {
                val arr = JSONArray(trimmedJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.optString("name", "Товар ${i + 1}")
                    val code = obj.optString("code", "ITEM-${i + 1}")
                    val price = obj.optDouble("price", 0.0)
                    val qty = obj.optInt("quantity", 1)
                    goods.add(
                        CheckboxGoodItem(
                            name = name,
                            code = code,
                            priceKopecks = Math.round(price * 100),
                            quantityThousandths = qty * 1000L
                        )
                    )
                }
                parsedFromJson = goods.isNotEmpty()
            } catch (_: Exception) {}
        }

        if (!parsedFromJson) {
            val title = receipt.itemsJson.ifBlank { "Послуги / Товари" }
            goods.add(
                CheckboxGoodItem(
                    name = title,
                    code = "ITEM-001",
                    priceKopecks = Math.round(receipt.amount * 100),
                    quantityThousandths = 1000L
                )
            )
        }

        if (receipt.tipsAmount > 0.0) {
            goods.add(
                CheckboxGoodItem(
                    name = "Чайові персоналу",
                    code = "TIPS",
                    priceKopecks = Math.round(receipt.tipsAmount * 100),
                    quantityThousandths = 1000L
                )
            )
        }

        val totalKopecks = Math.round((receipt.amount + receipt.tipsAmount) * 100)
        val payments = listOf(
            CheckboxPayment(
                type = "CASHLESS",
                valueKopecks = totalKopecks,
                label = "Безготівкова оплата (QR-код)"
            )
        )

        return CheckboxReceiptRequest(
            goods = goods,
            payments = payments
        )
    }

    companion object {
        private const val TAG = "OfflineReceiptSync"
        private const val WORK_NAME = "offline_receipt_sync_work"

        fun enqueueSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<OfflineReceiptSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "Offline receipt sync work enqueued with CONNECTED constraint")
        }
    }
}
