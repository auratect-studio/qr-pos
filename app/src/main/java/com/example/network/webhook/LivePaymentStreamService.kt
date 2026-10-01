package com.example.network.webhook

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

enum class StreamConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Клієнт для підключення до Edge Webhook Gateway через Server-Sent Events (SSE).
 * Забезпечує миттєве сповіщення про зарахування коштів на рахунок без періодичного polling.
 */
class LivePaymentStreamService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Без таймауту для тривалого SSE-потоку
        .retryOnConnectionFailure(true)
        .build(),
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
) {
    private val tag = "LivePaymentStream"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var streamJob: Job? = null

    private val _connectionState = MutableStateFlow(StreamConnectionState.DISCONNECTED)
    val connectionState: StateFlow<StreamConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<BankWebhookEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<BankWebhookEvent> = _events.asSharedFlow()

    private val eventAdapter = moshi.adapter(BankWebhookEvent::class.java)

    var currentGatewayUrl: String = "https://qr-pos-gateway.antigravity.workers.dev"
        private set
    var currentMerchantId: String = "default"
        private set

    /**
     * Запуск постійного прослуховування SSE-потоку.
     * Запобігає спаму запитами, якщо тестовий шлюз офлайн або повертає 404.
     */
    fun startStreaming(gatewayUrl: String, merchantId: String, forceReconnect: Boolean = false) {
        val cleanUrl = gatewayUrl.trim().trimEnd('/')
        val cleanId = merchantId.trim().ifBlank { "default" }

        if (!forceReconnect && streamJob?.isActive == true && currentGatewayUrl == cleanUrl && currentMerchantId == cleanId) {
            // Вже підключено до цього каналу
            return
        }

        stopStreaming()

        currentGatewayUrl = cleanUrl
        currentMerchantId = cleanId

        if (cleanUrl.isBlank()) {
            _connectionState.value = StreamConnectionState.DISCONNECTED
            return
        }

        streamJob = scope.launch {
            var attempt = 0
            val maxAttempts = 2
            while (isActive) {
                var shouldStopRetrying = false
                try {
                    _connectionState.value = StreamConnectionState.CONNECTING
                    val streamEndpoint = "$currentGatewayUrl/events/$currentMerchantId"
                    Log.d(tag, "Connecting to SSE: $streamEndpoint (attempt ${attempt + 1})")

                    val request = Request.Builder()
                        .url(streamEndpoint)
                        .header("Accept", "text/event-stream")
                        .header("Cache-Control", "no-cache")
                        .build()

                    val response: Response = client.newCall(request).execute()

                    if (!response.isSuccessful) {
                        Log.w(tag, "SSE connection failed with HTTP ${response.code}")
                        _connectionState.value = StreamConnectionState.ERROR
                        response.close()
                        // 404/502 означає, що віддалений шлюз не розгорнутий або недосяжний — не повторюємо нескінченно
                        if (response.code == 404 || response.code == 502) {
                            Log.i(tag, "Gateway endpoint not available (${response.code}). Pausing auto-retry.")
                            shouldStopRetrying = true
                        }
                    } else {
                        _connectionState.value = StreamConnectionState.CONNECTED
                        attempt = 0
                        Log.d(tag, "SSE Connected to $streamEndpoint")

                        response.body?.use { body ->
                            val reader = BufferedReader(InputStreamReader(body.byteStream()))
                            var line: String? = null
                            while (isActive && reader.readLine().also { line = it } != null) {
                                val currentLine = line ?: break
                                if (currentLine.startsWith("data:")) {
                                    val dataPayload = currentLine.removePrefix("data:").trim()
                                    if (dataPayload.isNotEmpty() && dataPayload.startsWith("{")) {
                                        try {
                                            val event = eventAdapter.fromJson(dataPayload)
                                            if (event != null) {
                                                Log.i(tag, "Received Bank Webhook Event: ${event.amount} UAH (${event.bank})")
                                                _events.emit(event)
                                            }
                                        } catch (e: Exception) {
                                            Log.w(tag, "Failed to parse SSE event JSON: $dataPayload", e)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        Log.w(tag, "SSE connection interrupted: ${e.message}")
                        _connectionState.value = StreamConnectionState.ERROR
                    }
                }

                if (shouldStopRetrying) {
                    break
                }

                // Затримка перед повторним підключенням (Backoff)
                if (isActive) {
                    attempt++
                    if (attempt >= maxAttempts) {
                        Log.i(tag, "Reached maximum reconnect attempts ($maxAttempts). Pausing auto-reconnect.")
                        break
                    }
                    val backoffSeconds = (attempt * 2L).coerceAtMost(10L)
                    delay(backoffSeconds * 1000L)
                }
            }
            if (_connectionState.value == StreamConnectionState.CONNECTING) {
                _connectionState.value = StreamConnectionState.DISCONNECTED
            }
        }
    }

    fun reconnect() {
        startStreaming(currentGatewayUrl, currentMerchantId, forceReconnect = true)
    }

    fun stopStreaming() {
        streamJob?.cancel()
        streamJob = null
        _connectionState.value = StreamConnectionState.DISCONNECTED
    }

    /**
     * Відправка тестового симуляційного платежу через шлюз (для перевірки коннекту)
     */
    suspend fun triggerTestPayment(
        amount: Double,
        bank: String = "MONOBANK",
        comment: String = "Тест Webhook"
    ): Boolean {
        return try {
            val json = """
                {"amount": $amount, "bank": "$bank", "comment": "$comment"}
            """.trimIndent()
            val reqBody = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$currentGatewayUrl/test-push/$currentMerchantId")
                .post(reqBody)
                .build()

            val response = client.newCall(request).execute()
            val successful = response.isSuccessful
            response.close()
            successful
        } catch (e: Exception) {
            Log.e(tag, "Error triggering test push", e)
            false
        }
    }
}
