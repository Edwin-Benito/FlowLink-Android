package mx.castillo.edwin.mensajeria.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import mx.castillo.edwin.mensajeria.data.Chat
import mx.castillo.edwin.mensajeria.data.Message
import mx.castillo.edwin.mensajeria.data.User
import mx.castillo.edwin.mensajeria.security.CryptoManager
// IMPORTACIÓN DE TU NUEVO SERVICIO (PRESERVADA)
import mx.castillo.edwin.mensajeria.services.CloudTranslationService
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// --- ESTADO (CON CAMBIO MENOR) ---
data class ChatUiState(
    val messages: List<Message> = emptyList(),
    // val groupedMessages: List<Any> = emptyList(), // Eliminado, ChatScreen ya hace esto
    val currentUser: User? = null,
    val otherUser: User? = null,
    val isLoading: Boolean = true,
    val isOtherUserTyping: Boolean = false,
    val isReady: Boolean = false,
    val userStatus: String = "Cargando...",
    val disappearingMessagesDuration: Long = 0,
    val error: String? = null
)

data class DateSeparator(val date: Date) // ChatScreen usa esto, pero el VM no necesita groupedMessages

class ChatViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val rtdb = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val cryptoManager = CryptoManager()

    // Listeners
    private var statusListener: ValueEventListener? = null
    private var userStatusRef: DatabaseReference? = null
    private var chatListenerRegistration: ListenerRegistration? = null
    private var messagesListenerRegistration: ListenerRegistration? = null // <-- RESTAURADO

    // INSTANCIA DE TU NUEVO SERVICIO (PRESERVADA)
    private val cloudTranslationService = CloudTranslationService()

    // Estado
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    // Auxiliares
    private var currentChatId: String? = null
    private var typingJob: Job? = null
    private val isScreenActive = MutableStateFlow(false)

    // Flujo del usuario actual (optimizado)
    private val currentUserFlow = flow {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")
        val userDoc = db.collection("users").document(uid).get().await()
        emit(userDoc.toObject(User::class.java) ?: throw IllegalStateException("Usuario no encontrado en DB"))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)


    fun onToastShown() {
        _toastMessage.value = null
    }

    // --- FUNCIÓN 'loadChat' (LÓGICA SECUENCIAL RESTAURADA) ---
    fun loadChat(chatId: String, context: Context) {
        // 1. Guardia de recarga ELIMINADA para forzar la actualización de la duración
        // if (chatId == currentChatId && _uiState.value.isReady) return // <-- ELIMINADO

        Log.d("ChatVM", "loadChat: Iniciando carga para chat $chatId")
        currentChatId = chatId
        // 2. Resetea el estado (SINCRONO)
        _uiState.value = ChatUiState(isLoading = true)

        // 3. Inicializa CryptoManager
        try {
            cryptoManager.init(context.applicationContext)
        } catch (e: Exception) {
            Log.e("ChatVM", "Error inicializando CryptoManager", e)
            _toastMessage.value = "Error de seguridad inicializando cifrado."
            // 4. Actualización de error (SINCRONA)
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Error de seguridad.")
            return
        }

        // 5. Inicia la carga secuencial
        viewModelScope.launch {
            try {
                // --- FASE 1: OBTENER TODOS LOS DATOS (SECUENCIAL) ---
                val currentUser = currentUserFlow.filterNotNull().first()
                val otherUser = getOtherUserData(chatId, currentUser) // Función suspendida
                val duration = try {
                    val chatDoc = db.collection("chats").document(chatId).get().await()
                    if (chatDoc.exists()) chatDoc.getLong("disappearingMessagesDuration") ?: 0L else 0L
                } catch (e: Exception) {
                    Log.w("ChatVM", "No se pudo obtener duración, asumiendo 0", e)
                    0L
                }
                // (La configuración del traductor ya no es necesaria aquí)

                // --- FASE 2: ACTUALIZAR EL ESTADO PREVIO (SINCRONO) ---
                _uiState.value = _uiState.value.copy(
                    currentUser = currentUser,
                    otherUser = otherUser,
                    disappearingMessagesDuration = duration
                )
                Log.d("ChatVM", "Fase 2 completa. Duración en estado: $duration")

                // --- FASE 3: INICIAR LISTENERS (AHORA SEGUROS) ---
                if (otherUser != null) {
                    listenForUserStatus(otherUser.uid)
                }
                listenForTypingStatus(chatId)
                listenForMessages(chatId, currentUser) // Esta función activará isReady = true

            } catch (e: Exception) {
                Log.e("ChatVM", "Error al cargar datos del chat", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = "No se pudo cargar el chat.")
            }
        }
    }

    // --- 'getOtherUserData' (FUNCIÓN SUSPENDIDA RESTAURADA) ---
    private suspend fun getOtherUserData(chatId: String, currentUser: User): User? {
        return try {
            val document = db.collection("chats").document(chatId).get().await()
            val participants = document.get("participants") as? List<*>
            val otherUserId = participants?.firstOrNull { it != currentUser.uid } as? String

            if (otherUserId != null) {
                db.collection("users").document(otherUserId).get().await().toObject(User::class.java)
            } else {
                Log.w("ChatVM", "No se encontró otro participante en el chat $chatId")
                null
            }
        } catch (e: Exception) {
            Log.e("ChatVM", "Error al getOtherUserData $chatId", e)
            _uiState.value = _uiState.value.copy(error = "No se pudo cargar al contacto.")
            null
        }
    }

    private fun listenForMessages(chatId: String, currentUser: User) {
        val currentUserUid = currentUser.uid
        val currentUserLanguage = currentUser.preferredLanguage

        messagesListenerRegistration?.remove()
        messagesListenerRegistration = db.collection("chats").document(chatId)
            .collection("messages").orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e("ChatVM", "Error en listener de mensajes para chat $chatId", error)
                    _uiState.value = _uiState.value.copy(isLoading = false, isReady = true, error = "Error al cargar mensajes.")
                    return@addSnapshotListener
                }
                val messagesFromDb = querySnapshot?.toObjects(Message::class.java) ?: emptyList()

                viewModelScope.launch(Dispatchers.IO) {
                    val visibleMessages = messagesFromDb.filter { msg ->
                        !msg.deletedFor.contains(currentUserUid)
                    }

                    val processedMessages = visibleMessages.map {
                        val encryptedPayload = it.payloads[currentUserUid]
                        if (it.isDeleted || it.imageUrl != null) {
                            it
                        } else if (encryptedPayload.isNullOrBlank()) {
                            Log.w("ChatVM", "Mensaje ${it.id} sin payload para $currentUserUid.")
                            it.copy(text = "...")
                        } else {
                            // --- ✅ CORRECCIÓN: TRY-CATCH ROBUSTO ---
                            try {
                                val decryptedJson = cryptoManager.decrypt(encryptedPayload)
                                val type = object : TypeToken<Map<String, String>>() {}.type
                                val translations: Map<String, String> = Gson().fromJson(decryptedJson, type)
                                val displayText = translations[currentUserLanguage] 
                                    ?: translations.values.firstOrNull() 
                                    ?: "Traducción no encontrada"
                                it.copy(text = displayText)
                            } catch (e: Exception) {
                                // --- PREVIENE EL CRASH ---
                                Log.e("DecryptionError", "Fallo al descifrar mensaje ID: ${it.id}. Razón: ${e.message}", e)
                                // Devuelve un mensaje seguro sin romper la UI
                                it.copy(text = "🔒 Mensaje ilegible (error de clave)")
                            }
                            // --- FIN CORRECCIÓN ---
                        }
                    }

                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            messages = processedMessages,
                            isLoading = false,
                            isReady = true,
                            error = null
                        )

                        if (isScreenActive.value) markMessagesAsRead(chatId)
                    }
                }
            }
    }

    // --- 'sendMessage' (LÓGICA DE TRANSLATE API PRESERVADA) ---
    fun sendMessage(chatId: String, text: String, messageToReply: Message? = null) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val currentUser = currentState.currentUser ?: return@launch
            val otherUser = currentState.otherUser ?: return@launch
            val currentDuration = currentState.disappearingMessagesDuration // <-- Leerá el valor correcto

            Log.d("SendMessage", "Enviando mensaje. Duración leída del estado: $currentDuration segundos")

            try {
                // Traducción
                val translations = mutableMapOf<String, String>()
                val sourceLang = currentUser.preferredLanguage
                val targetLang = otherUser.preferredLanguage
                translations[sourceLang] = text

                // --- INICIO: LÓGICA DE CLOUD TRANSLATE API (PRESERVADA) ---
                if (sourceLang.isNotBlank() && targetLang.isNotBlank() && sourceLang != targetLang) {
                    try {
                        val translatedText = cloudTranslationService.translate(text, targetLang)

                        if (translatedText != null) {
                            translations[targetLang] = translatedText
                            Log.d("ChatVM_Translate", "Traducción exitosa: $text -> $translatedText")
                        } else {
                            Log.e("TranslateError", "Error al traducir con Cloud API (devuelve null)")
                            _toastMessage.value = "Error al traducir. Enviando original."
                        }
                    } catch (e: Exception) {
                        Log.e("TranslateError", "Excepción al traducir con Cloud API", e)
                        _toastMessage.value = "Error al traducir. Enviando original."
                    }
                }
                // --- FIN: LÓGICA DE CLOUD TRANSLATE API ---

                val jsonTranslations = Gson().toJson(translations)

                // Encriptación (PRESERVADA)
                val recipientPublicKey = otherUser.publicKey
                val senderPublicKey = currentUser.publicKey
                if (recipientPublicKey.isNullOrBlank() || senderPublicKey.isNullOrBlank()) {
                    _toastMessage.value = "Error de claves. No se puede enviar."
                    return@launch
                }
                val encryptedPayloads = mutableMapOf<String, String>()
                encryptedPayloads[otherUser.uid] = cryptoManager.encrypt(jsonTranslations, recipientPublicKey)
                encryptedPayloads[currentUser.uid] = cryptoManager.encrypt(jsonTranslations, senderPublicKey)

                // --- LÓGICA DE MENSAJES TEMPORALES (PRESERVADA) ---
                val deleteAtTimestamp: Timestamp? = if (currentDuration > 0) {
                    val nowMillis = System.currentTimeMillis()
                    val deleteMillis = nowMillis + (currentDuration * 1000)
                    Timestamp(Date(deleteMillis))
                } else {
                    null
                }
                // --- FIN LÓGICA DE MENSAJES TEMPORALES ---

                // Crear objeto Message (PRESERVADO)
                val message = Message(
                    senderId = currentUser.uid,
                    payloads = encryptedPayloads,
                    status = "enviando",
                    readBy = listOf(currentUser.uid),
                    replyToMessageId = messageToReply?.id,
                    replyToMessageText = messageToReply?.text,
                    replyToSenderId = messageToReply?.senderId,
                    participants = listOf(currentUser.uid, otherUser.uid),
                    deleteAt = deleteAtTimestamp // <-- VALOR CORRECTO
                )

                // Guardar en Firestore (PRESERVADO)
                val messagesCollection = db.collection("chats").document(chatId).collection("messages")
                val docRef = messagesCollection.add(message).await()
                messagesCollection.document(docRef.id).update("status", "enviado").await()
                db.collection("chats").document(chatId).update(mapOf(
                    "lastMessage" to encryptedPayloads,
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "lastMessageSenderId" to currentUser.uid
                )).await()

            } catch (e: Exception) {
                Log.e("SendMessageError", "Error al enviar mensaje cifrado y traducido", e)
                _toastMessage.value = "No se pudo enviar el mensaje."
            }
        }
    }

    // --- 'sendImageMessage' (LÓGICA DE MENSAJES TEMPORALES RESTAURADA) ---
    fun sendImageMessage(chatId: String, imageUri: Uri) {
        viewModelScope.launch {
            // Lee el estado síncronamente
            val currentState = _uiState.value
            val currentUserUid = currentState.currentUser?.uid ?: return@launch
            val otherUser = currentState.otherUser
            val otherUserId = otherUser?.uid ?: ""
            val currentDuration = currentState.disappearingMessagesDuration // <-- RESTAURADO

            val fileName = "${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child("chat_images/$chatId/$fileName")

            if (otherUserId.isBlank()) {
                Log.e("SendImageError", "No se encontró el ID del otro usuario.")
                _toastMessage.value = "Error: No se pudo identificar al destinatario."
                return@launch
            }

            try {
                storageRef.putFile(imageUri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                // --- LÓGICA DE MENSAJES TEMPORALES (RESTAURADA) ---
                val deleteAtTimestamp: Timestamp? = if (currentDuration > 0) {
                    val nowMillis = System.currentTimeMillis()
                    val deleteMillis = nowMillis + (currentDuration * 1000)
                    Timestamp(Date(deleteMillis))
                } else {
                    null
                }
                // --- FIN LÓGICA ---

                val message = Message(
                    senderId = currentUserUid,
                    text = "Imagen",
                    status = "enviando",
                    readBy = listOf(currentUserUid),
                    imageUrl = downloadUrl,
                    participants = listOf(currentUserUid, otherUserId),
                    deleteAt = deleteAtTimestamp // <-- RESTAURADO
                )

                val messagesCollection = db.collection("chats").document(chatId).collection("messages")
                val docRef = messagesCollection.add(message).await()
                messagesCollection.document(docRef.id).update("status", "enviado").await()

                val lastMessageSummary = mapOf(
                    currentUserUid to "📷 Imagen",
                    otherUserId to "📷 Imagen"
                )

                db.collection("chats").document(chatId)
                    .update(mapOf(
                        "lastMessage" to lastMessageSummary,
                        "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                        "lastMessageSenderId" to currentUserUid
                    )).await()

            } catch (e: Exception) {
                Log.e("SendImageError", "Error al enviar imagen", e)
                _toastMessage.value = "Error al enviar la imagen"
            }
        }
    }

    // --- 'listenForTypingStatus' (CORREGIDO A SÍNCRONO) ---
    private fun listenForTypingStatus(chatId: String) {
        val currentUserUid = auth.currentUser?.uid ?: return
        chatListenerRegistration?.remove()
        chatListenerRegistration = db.collection("chats").document(chatId)
            .addSnapshotListener { snapshot, _ ->
                val typingUsers = snapshot?.get("typingUsers") as? List<*> ?: emptyList<String>()
                val isOtherUserTyping = typingUsers.any { it != currentUserUid }
                _uiState.value = _uiState.value.copy(isOtherUserTyping = isOtherUserTyping)
            }
    }

    // --- 'listenForUserStatus' (CORREGIDO A SÍNCRONO) ---
    private fun listenForUserStatus(userId: String) {
        statusListener?.let { userStatusRef?.removeEventListener(it) }

        userStatusRef = rtdb.getReference("/status/$userId")
        statusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnline = snapshot.child("online").getValue(Boolean::class.java) ?: false
                if (isOnline) {
                    _uiState.value = _uiState.value.copy(userStatus = "En línea")
                } else {
                    val lastSeenTimestamp = snapshot.child("last_seen").getValue(Long::class.java)
                    val statusText = if (lastSeenTimestamp != null) {
                        "Últ. vez ${formatLastSeen(lastSeenTimestamp)}"
                    } else {
                        "Desconectado"
                    }
                    _uiState.value = _uiState.value.copy(userStatus = statusText)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                _uiState.value = _uiState.value.copy(userStatus = "Desconectado")
            }
        }
        userStatusRef?.addValueEventListener(statusListener!!)
    }

    // --- FUNCIONES ELIMINADAS ---
    // 'loadCurrentUser' (ahora integrado en currentUserFlow)
    // 'listenForChatSettings' (ahora integrado en loadChat)
    // 'setupTranslator' (ya no se usa)

    // --- OTRAS FUNCIONES (PRESERVADAS) ---

    fun onTyping() {
        typingJob?.cancel()
        val currentUserUid = auth.currentUser?.uid ?: return
        val chatId = currentChatId ?: return
        typingJob = viewModelScope.launch {
            try {
                db.collection("chats").document(chatId).update("typingUsers", FieldValue.arrayUnion(currentUserUid)).await()
                delay(2000)
                db.collection("chats").document(chatId).update("typingUsers", FieldValue.arrayRemove(currentUserUid)).await()
            } catch (e: Exception) {
                Log.w("ChatVM", "Error actualizando estado 'typing'", e)
            }
        }
    }

    fun markMessagesAsRead(chatId: String) {
        val currentUser = _uiState.value.currentUser
        if (currentUser?.readReceiptsEnabled == false) return
        val currentUserUid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            val currentMessages = _uiState.value.messages
            val unreadMessages = currentMessages.filter { message ->
                message.senderId != currentUserUid && !message.readBy.contains(currentUserUid)
            }
            if (unreadMessages.isEmpty()) return@launch

            val batch = db.batch()
            unreadMessages.forEach { message ->
                if (message.id.isNotBlank()) {
                    val messageRef = db.collection("chats").document(chatId).collection("messages").document(message.id)
                    batch.update(messageRef, "readBy", FieldValue.arrayUnion(currentUserUid))
                }
            }
            try {
                batch.commit().await()
                Log.d("MarkAsRead", "${unreadMessages.size} mensajes marcados como leídos en chat $chatId")
            } catch (e: Exception) {
                Log.e("MarkAsRead", "Error al actualizar mensajes como leídos", e)
            }
        }
    }

    fun deleteMessageForMe(chatId: String, messageId: String) {
        val currentUserUid = auth.currentUser?.uid ?: return
        if (chatId.isBlank() || messageId.isBlank()) return
        val messageRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
        messageRef.update("deletedFor", FieldValue.arrayUnion(currentUserUid))
            .addOnFailureListener { e -> Log.w("DeleteForMe", "Error al eliminar mensaje para mí", e) }
    }

    fun deleteMessageForEveryone(chatId: String, messageId: String) {
        if (chatId.isBlank() || messageId.isBlank()) return
        val messageRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
        val updates = mapOf(
            "isDeleted" to true,
            "payloads" to emptyMap<String, String>(),
            "imageUrl" to null,
            "text" to "Mensaje eliminado"
        )
        messageRef.update(updates)
            .addOnFailureListener { e -> Log.w("DeleteForEveryone", "Error al eliminar mensaje para todos", e) }
    }

    fun starMessage(chatId: String, messageId: String, isCurrentlyStarred: Boolean) {
        if (chatId.isBlank() || messageId.isBlank()) return
        val messageRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
        messageRef.update("isStarred", !isCurrentlyStarred)
            .addOnFailureListener { e -> Log.w("StarMessage", "Error al destacar/desdestacar mensaje", e) }
    }

    // Esta función ya no es necesaria en el ViewModel si ChatScreen la maneja
    /*
    private fun groupMessagesByDate(messages: List<Message>): List<Any> {
        val groupedList = mutableListOf<Any>()
        var lastDate: Calendar? = null
        messages.forEach { message ->
            val messageDate = Calendar.getInstance().apply { time = message.timestamp ?: Date() }
            if (lastDate == null || !isSameDay(lastDate!!, messageDate)) {
                groupedList.add(DateSeparator(messageDate.time))
            }
            groupedList.add(message)
            lastDate = messageDate
        }
        return groupedList
    }
    */

    private fun formatLastSeen(timestamp: Long): String {
        val messageCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCalendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        return if (isSameDay(messageCalendar, nowCalendar)) {
            "hoy a las ${timeFormat.format(Date(timestamp))}"
        } else if (isSameDay(messageCalendar, nowCalendar.apply { add(Calendar.DAY_OF_YEAR, -1) })) {
            "ayer a las ${timeFormat.format(Date(timestamp))}"
        } else {
            "el ${dateFormat.format(Date(timestamp))}"
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun setScreenActive(isActive: Boolean) {
        isScreenActive.value = isActive
        if (isActive && currentChatId != null) {
            markMessagesAsRead(currentChatId!!)
        }
    }

    override fun onCleared() {
        super.onCleared()
        statusListener?.let { userStatusRef?.removeEventListener(it) }
        chatListenerRegistration?.remove()
        messagesListenerRegistration?.remove() // <-- RESTAURADO
        typingJob?.cancel()
        Log.d("ChatVM", "ChatViewModel cleared for chat: $currentChatId")
    }
}