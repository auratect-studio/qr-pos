package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class MonoSetWebhookRequest(
    @Json(name = "webHookUrl") val webHookUrl: String
)

@JsonClass(generateAdapter = true)
data class MonoSetWebhookResponse(
    @Json(name = "status") val status: String = "ok"
)

@JsonClass(generateAdapter = true)
data class MonoStatementItem(
    @Json(name = "id") val id: String,
    @Json(name = "time") val time: Long,
    @Json(name = "description") val description: String,
    @Json(name = "mcc") val mcc: Int? = null,
    @Json(name = "amount") val amount: Long, // in kopecks: 10000 = 100.00 UAH
    @Json(name = "operationAmount") val operationAmount: Long,
    @Json(name = "currencyCode") val currencyCode: Int,
    @Json(name = "commissionRate") val commissionRate: Long? = 0,
    @Json(name = "cashbackAmount") val cashbackAmount: Long? = 0,
    @Json(name = "balance") val balance: Long,
    @Json(name = "hold") val hold: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class MonoClientInfo(
    @Json(name = "clientId") val clientId: String,
    @Json(name = "name") val name: String,
    @Json(name = "webHookUrl") val webHookUrl: String? = null,
    @Json(name = "permissions") val permissions: String? = null,
    @Json(name = "accounts") val accounts: List<MonoAccount> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MonoAccount(
    @Json(name = "id") val id: String,
    @Json(name = "sendId") val sendId: String? = null,
    @Json(name = "balance") val balance: Long,
    @Json(name = "creditLimit") val creditLimit: Long,
    @Json(name = "type") val type: String,
    @Json(name = "currencyCode") val currencyCode: Int,
    @Json(name = "cashbackType") val cashbackType: String? = null,
    @Json(name = "maskedPan") val maskedPan: List<String> = emptyList(),
    @Json(name = "iban") val iban: String? = null
)

interface MonobankApiService {
    @GET("personal/client-info")
    suspend fun getClientInfo(
        @Header("X-Token") token: String
    ): MonoClientInfo

    @GET("personal/statement/{account}/{from}")
    suspend fun getStatement(
        @Header("X-Token") token: String,
        @Path("account") account: String = "0",
        @Path("from") fromTimestampSeconds: Long
    ): List<MonoStatementItem>

    @POST("personal/webhook")
    suspend fun setWebhook(
        @Header("X-Token") token: String,
        @Body body: MonoSetWebhookRequest
    ): Response<MonoSetWebhookResponse>

    companion object {
        private const val BASE_URL = "https://api.monobank.ua/"

        fun create(): MonobankApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(MonobankApiService::class.java)
        }
    }
}
