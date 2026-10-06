package elemsocial.com.core.ws

import elemsocial.com.core.codec.MsgPackCodec
import elemsocial.com.core.crypto.CryptoEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.io.IOException
import java.security.KeyPair
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import kotlin.math.min

class ElementSocketClient(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private data class QueuedRequest(
        val payload: Map<String, Any?>,
        val timeoutMs: Long,
        val deferred: CompletableDeferred<Map<String, Any?>>
    )

    enum class ConnectionState {
        Disconnected,
        Connecting,
        Handshaking,
        Ready,
        Error
    }

    private val httpClient = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var webSocket: WebSocket? = null

    private var keyPair: KeyPair? = null
    private var serverRsaPublicPem: String? = null
    private var clientAesKey: ByteArray? = null
    private var serverAesKey: ByteArray? = null

    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<Map<String, Any?>>>()
    private val queuedRequests = ConcurrentLinkedQueue<QueuedRequest>()
    private val queueProcessing = AtomicBoolean(false)

    private var reconnectJob: Job? = null
    private var autoReconnectEnabled: Boolean = false
    private var reconnectAttempts: Int = 0
    @Volatile
    private var authorizationSessionKey: String? = null
    @Volatile
    private var awaitingSessionAuthorization: Boolean = false

    private var urls: List<String> = emptyList()
    private var urlIndex: Int = 0

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _logs = MutableSharedFlow<String>(extraBufferCapacity = 200)
    val logs: SharedFlow<String> = _logs.asSharedFlow()

    private val _events = MutableSharedFlow<Map<String, Any?>>(extraBufferCapacity = 200)
    val events: SharedFlow<Map<String, Any?>> = _events.asSharedFlow()

    companion object {
        private const val BASE_RECONNECT_DELAY_MS = 1_500L
        private const val MAX_RECONNECT_DELAY_MS = 30_000L
        private const val MAX_QUEUED_REQUESTS = 1_000
        private const val QUEUE_WAIT_GRACE_MS = 120_000L
    }

    fun configureReconnectUrls(wsUrls: List<String>) {
        val prepared = wsUrls
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (prepared.isEmpty()) {
            throw IllegalArgumentException("At least one websocket URL is required")
        }

        urls = prepared
        if (urlIndex !in urls.indices) urlIndex = 0
    }

    fun connect(url: String) = connect(listOf(url))

    fun connect(urls: List<String>) {
        configureReconnectUrls(urls)
        autoReconnectEnabled = true
        reconnectAttempts = 0
        cancelReconnect()

        if (_connectionState.value == ConnectionState.Connecting ||
            _connectionState.value == ConnectionState.Handshaking ||
            _connectionState.value == ConnectionState.Ready
        ) return

        val targetUrl = getCurrentUrl() ?: return
        startSocketConnection(targetUrl)
    }

    fun disconnect(clearQueue: Boolean = false, disableReconnect: Boolean = true) {
        if (disableReconnect) autoReconnectEnabled = false
        cancelReconnect()

        webSocket?.close(1000, "Client disconnect")
        webSocket = null

        cleanupConnectionState(
            state = ConnectionState.Disconnected,
            throwable = IOException("Disconnected by client")
        )

        if (clearQueue || disableReconnect) {
            clearQueuedRequests(IOException("Queue cleared by client"))
        }
    }

    fun setAuthorizationSessionKey(sessionKey: String?) {
        authorizationSessionKey = sessionKey?.trim()?.takeIf { it.isNotBlank() }
    }

    suspend fun connectAndAwaitReady(url: String, timeoutMs: Long = 20_000): Boolean =
        connectAndAwaitReady(listOf(url), timeoutMs)

    suspend fun connectAndAwaitReady(urls: List<String>, timeoutMs: Long = 20_000): Boolean {
        if (isSocketReadyToSend()) return true

        connect(urls)

        val result = withTimeoutOrNull(timeoutMs) {
            while (!isSocketReadyToSend() && _connectionState.value != ConnectionState.Error) {
                delay(120)
            }
            isSocketReadyToSend()
        }

        return result == true
    }

    suspend fun sendRequest(payload: Map<String, Any?>, timeoutMs: Long = 60_000): Map<String, Any?> {
        if (isSocketReadyToSend()) return sendNowAwait(payload, timeoutMs)

        if (!autoReconnectEnabled) {
            throw IllegalStateException("Socket is not ready and auto-reconnect is disabled")
        }

        if (queuedRequests.size >= MAX_QUEUED_REQUESTS) {
            throw IOException("Message queue overflow")
        }

        val queued = QueuedRequest(
            payload = payload.toMap(),
            timeoutMs = timeoutMs,
            deferred = CompletableDeferred()
        )

        queuedRequests.add(queued)
        log("Request queued: type=${payload["type"]}, action=${payload["action"]}")

        if (_connectionState.value == ConnectionState.Disconnected ||
            _connectionState.value == ConnectionState.Error
        ) {
            getCurrentUrl()?.let { startSocketConnection(it) }
        }

        processQueuedRequests()

        return try {
            withTimeout(timeoutMs + QUEUE_WAIT_GRACE_MS) {
                queued.deferred.await()
            }
        } catch (error: TimeoutCancellationException) {
            queued.deferred.cancel(error)
            queuedRequests.remove(queued)
            throw IOException("Timed out while waiting queued request")
        }
    }

    private fun startSocketConnection(url: String) {
        awaitingSessionAuthorization = false
        _connectionState.value = ConnectionState.Connecting
        resetTransportKeys()

        val oldSocket = webSocket
        webSocket = null
        oldSocket?.cancel()

        log("Connecting to $url")

        val request = Request.Builder().url(url).build()
        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!isCurrentSocket(webSocket)) {
                    webSocket.close(1000, "Stale socket")
                    return
                }
                log("Socket connected")
                reconnectAttempts = 0
                runCatching {
                    if (keyPair == null) keyPair = CryptoEngine.generateRsaKeyPair()
                    val publicPem = CryptoEngine.publicKeyToPem(keyPair!!.public)
                    val exchangePayload = JSONObject()
                        .put("type", "key_exchange")
                        .put("key", publicPem)
                        .toString()

                    _connectionState.value = ConnectionState.Handshaking
                    webSocket.send(exchangePayload)
                    log("key_exchange sent")
                }.onFailure { error ->
                    _connectionState.value = ConnectionState.Error
                    log("Handshake init error: ${error.message}")
                    handleTransportLoss(ConnectionState.Error, error)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (!isCurrentSocket(webSocket)) return
                runCatching { handleTextMessage(webSocket, text) }
                    .onFailure { log("Text error (ignored): ${it.message}") }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (!isCurrentSocket(webSocket)) return
                runCatching { handleBinaryMessage(bytes.toByteArray()) }
                    .onFailure { log("Binary error (ignored): ${it.message}") }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (!isCurrentSocket(webSocket)) { webSocket.close(code, reason); return }
                log("Socket closing: [$code] $reason")
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!isCurrentSocket(webSocket)) return
                log("Socket closed: [$code] $reason")
                handleTransportLoss(ConnectionState.Disconnected, IOException("Socket closed"))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!isCurrentSocket(webSocket)) return
                log("Socket failure: ${t.message ?: "unknown"}")
                handleTransportLoss(ConnectionState.Error, t)
            }
        })
    }

    private suspend fun sendNowAwait(payload: Map<String, Any?>, timeoutMs: Long): Map<String, Any?> {
        val socket = webSocket ?: throw IllegalStateException("WebSocket is not connected")
        val outboundAes = serverAesKey ?: throw IllegalStateException("Transport keys are not ready")
        if (_connectionState.value != ConnectionState.Ready) {
            throw IllegalStateException("Socket is not ready")
        }

        val rayId = generateRayId(16)
        val packet = payload.toMutableMap().apply { put("ray_id", rayId) }

        val encoded = MsgPackCodec.encodeMap(packet)
        val encrypted = CryptoEngine.aesEncryptWithIvPrefix(encoded, outboundAes)
        val deferred = CompletableDeferred<Map<String, Any?>>()
        pendingRequests[rayId] = deferred

        if (!socket.send(encrypted.toByteString())) {
            pendingRequests.remove(rayId)
            throw IOException("WebSocket send returned false")
        }

        return try {
            withTimeout(timeoutMs) { deferred.await() }
        } finally {
            pendingRequests.remove(rayId, deferred)
        }
    }

    private suspend fun authorizeSession(sessionKey: String): Boolean {
        val response = sendNowAwait(
            payload = mapOf(
                "type" to "authorization",
                "action" to "connect",
                "S_KEY" to sessionKey
            ),
            timeoutMs = 30_000
        )
        val status = response["status"]?.toString()?.lowercase()
        val message = response["message"]?.toString()?.lowercase().orEmpty()
        val error = response["error"]?.toString()?.lowercase().orEmpty()
        val combined = "$message $error"
        val accountNotFound = combined.contains("account_not_found") ||
            combined.contains("account not found") ||
            combined.contains("аккаунт не найден") ||
            combined.contains("профиль не найден")
        val success = when (status) {
            "success", "ok", "200", null, "" -> true
            else -> false
        }
        return success && !accountNotFound
    }

    private fun handleTextMessage(webSocket: WebSocket, text: String) {
        val json = JSONObject(text)
        val type = json.optString("type")
        if (type != "key_exchange") {
            log("Unexpected text message: $text")
            return
        }

        val serverKey = json.optString("key")
        if (serverKey.isBlank()) error("Server RSA key is empty")

        serverRsaPublicPem = serverKey

        val nextClientAes = CryptoEngine.generateAesKeyBytes()
        clientAesKey = nextClientAes

        val rsaPayload = MsgPackCodec.encodeMap(
            mapOf(
                "type" to "aes_key",
                "key" to CryptoEngine.bytesToBase64(nextClientAes)
            )
        )
        val encryptedPayload = CryptoEngine.rsaEncrypt(rsaPayload, serverKey)
        webSocket.send(encryptedPayload.toByteString())
        log("Client AES key sent via RSA")
    }

    private fun handleBinaryMessage(message: ByteArray) {
        if (serverAesKey == null) {
            val localKeyPair = keyPair ?: error("Local RSA keys are not initialized")
            val decrypted = CryptoEngine.rsaDecrypt(message, localKeyPair.private)
            val payload = MsgPackCodec.decodeToMap(decrypted)

            val type = payload["type"] as? String
            if (type != "aes_key") {
                log("Unexpected RSA payload: $payload")
                return
            }

            val keyB64 = payload["key"] as? String ?: error("Missing server aes_key")
            serverAesKey = CryptoEngine.base64ToBytes(keyB64)
            val sessionKey = authorizationSessionKey
            awaitingSessionAuthorization = !sessionKey.isNullOrBlank()
            _connectionState.value = ConnectionState.Ready
            log("Handshake completed, socket ready")
            if (sessionKey.isNullOrBlank()) {
                processQueuedRequests()
                return
            }

            scope.launch {
                try {
                    val authorized = runCatching { authorizeSession(sessionKey) }.getOrDefault(false)
                    log(if (authorized) "Session authorized" else "Session auth failed")
                } finally {
                    awaitingSessionAuthorization = false
                    processQueuedRequests()
                }
            }
            return
        }

        val inboundAes = clientAesKey ?: error("Client AES key is not initialized")
        val decrypted = CryptoEngine.aesDecryptWithIvPrefix(message, inboundAes)
        val payload = MsgPackCodec.decodeToMap(decrypted)

        resolvePendingByRayId(payload)
        emitEvent(payload)
    }

    private fun resolvePendingByRayId(payload: Map<String, Any?>) {
        val rayId = payload["ray_id"] as? String ?: return
        val deferred = pendingRequests.remove(rayId) ?: return
        deferred.complete(payload)
    }

    private fun emitEvent(payload: Map<String, Any?>) {
        scope.launch { _events.emit(payload) }
    }

    private fun log(text: String) {
        scope.launch { _logs.emit(text) }
    }

    private fun handleTransportLoss(fallbackState: ConnectionState, throwable: Throwable) {
        cleanupConnectionState(fallbackState, throwable)
        if (!autoReconnectEnabled) return
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        if (reconnectJob != null) return
        val targetUrl = getCurrentUrl() ?: run {
            log("Reconnect skipped: no url")
            return
        }

        val exp = 1L shl min(reconnectAttempts, 10)
        val backoff = min(BASE_RECONNECT_DELAY_MS * exp, MAX_RECONNECT_DELAY_MS)
        val jitter = Random.nextLong(0, 801)
        val delayMs = backoff + jitter
        reconnectAttempts += 1

        reconnectJob = scope.launch {
            log("Reconnect in ${delayMs}ms (attempt=$reconnectAttempts)")
            delay(delayMs)
            reconnectJob = null
            nextUrl()
            getCurrentUrl()?.let {
                if (autoReconnectEnabled) startSocketConnection(it)
            }
        }
    }

    private fun cancelReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
    }

    private fun processQueuedRequests() {
        if (!isSocketReadyToSend()) return
        if (!queueProcessing.compareAndSet(false, true)) return

        try {
            while (true) {
                val queued = queuedRequests.poll() ?: break
                dispatchQueuedRequest(queued)
            }
        } finally {
            queueProcessing.set(false)
            if (queuedRequests.isNotEmpty() && isSocketReadyToSend()) processQueuedRequests()
        }
    }

    private fun dispatchQueuedRequest(request: QueuedRequest) {
        scope.launch {
            if (request.deferred.isCancelled || request.deferred.isCompleted) return@launch

            runCatching { sendNowAwait(request.payload, request.timeoutMs) }
                .onSuccess { request.deferred.complete(it) }
                .onFailure { error ->
                    if (isTransientSendError(error) && autoReconnectEnabled && !request.deferred.isCancelled) {
                        queuedRequests.add(request)
                        if (_connectionState.value == ConnectionState.Disconnected ||
                            _connectionState.value == ConnectionState.Error
                        ) {
                            scheduleReconnect()
                        }
                        return@launch
                    }
                    request.deferred.completeExceptionally(error)
                }
        }
    }

    private fun isTransientSendError(error: Throwable): Boolean {
        if (error is IllegalStateException && error.message?.contains("not ready", true) == true) return true
        return error is IOException
    }

    private fun clearQueuedRequests(reason: Throwable) {
        while (true) {
            val request = queuedRequests.poll() ?: break
            if (!request.deferred.isCompleted) request.deferred.completeExceptionally(reason)
        }
    }

    private fun cleanupConnectionState(state: ConnectionState, throwable: Throwable? = null) {
        _connectionState.value = state

        if (state != ConnectionState.Ready) {
            awaitingSessionAuthorization = false
            resetTransportKeys()
        }

        val pendingException = throwable ?: IOException("Connection closed")
        pendingRequests.forEach { (_, deferred) ->
            if (!deferred.isCompleted) deferred.completeExceptionally(pendingException)
        }
        pendingRequests.clear()

        if (state == ConnectionState.Disconnected || state == ConnectionState.Error) {
            webSocket = null
        }
    }

    private fun resetTransportKeys() {
        serverRsaPublicPem = null
        clientAesKey = null
        serverAesKey = null
    }

    private fun isSocketReadyToSend(): Boolean =
        _connectionState.value == ConnectionState.Ready &&
            webSocket != null &&
            serverAesKey != null &&
            clientAesKey != null &&
            !awaitingSessionAuthorization

    private fun isCurrentSocket(socket: WebSocket): Boolean = webSocket === socket

    private fun getCurrentUrl(): String? {
        if (urls.isEmpty()) return null
        if (urlIndex !in urls.indices) urlIndex = 0
        return urls[urlIndex]
    }

    private fun nextUrl() {
        if (urls.isEmpty()) return
        urlIndex = (urlIndex + 1) % urls.size
    }

    private fun generateRayId(length: Int): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return buildString(length) {
            repeat(length) { append(alphabet[Random.nextInt(alphabet.length)]) }
        }
    }
}
