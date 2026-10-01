package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class CheckboxGoodItem(
    @Json(name = "name") val name: String,
    @Json(name = "code") val code: String,
    @Json(name = "price") val priceKopecks: Long, // in kopecks: 100 UAH = 10000
    @Json(name = "quantity") val quantityThousandths: Long = 1000 // 1 item = 1000
)

@JsonClass(generateAdapter = true)
data class CheckboxPayment(
    @Json(name = "type") val type: String = "CASHLESS",
    @Json(name = "value") val valueKopecks: Long,
    @Json(name = "label") val label: String = "Безготівкова оплата (QR-код)"
)

@JsonClass(generateAdapter = true)
data class CheckboxReceiptRequest(
    @Json(name = "goods") val goods: List<CheckboxGoodItem>,
    @Json(name = "payments") val payments: List<CheckboxPayment>,
    @Json(name = "delivery") val delivery: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class CheckboxReceiptResponse(
    @Json(name = "id") val id: String,
    @Json(name = "fiscal_code") val fiscalCode: String? = null,
    @Json(name = "fiscal_date") val fiscalDate: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "tax_url") val taxUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class CheckboxCashierStatus(
    @Json(name = "id") val id: String,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "organization") val organization: String? = null
)

interface CheckboxApiService {
    @GET("api/v1/cashier/me")
    suspend fun getCashierMe(
        @Header("Authorization") bearerToken: String
    ): Response<CheckboxCashierStatus>

    @POST("api/v1/receipts/sell")
    suspend fun createSellReceipt(
        @Header("Authorization") bearerToken: String,
        @Body request: CheckboxReceiptRequest
    ): Response<CheckboxReceiptResponse>

    companion object {
        private const val BASE_URL = "https://api.checkbox.in.ua/"

        fun create(): CheckboxApiService {
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
                .create(CheckboxApiService::class.java)
        }
    }
}
