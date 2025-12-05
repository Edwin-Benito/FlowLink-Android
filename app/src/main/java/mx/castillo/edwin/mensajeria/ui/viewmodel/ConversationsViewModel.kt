package mx.castillo.edwin.mensajeria.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import mx.castillo.edwin.mensajeria.data.Chat
import mx.castillo.edwin.mensajeria.data.Message
import mx.castillo.edwin.mensajeria.data.User
import mx.castillo.edwin.mensajeria.security.CryptoManager

sealed class ConversationsUiState {
    object Loading : ConversationsUiState()
    data class Success(val chats: List<ChatInfo>) : ConversationsUiState()
    data class Error(val message: String) : ConversationsUiState()
}

data class ChatInfo(
    val chat: Chat,
    val otherUser: User?,
    val chatId: String,
    val unreadMessageCount: Int = 0,
    val isOtherUserTyping: Boolean = false,
    val lastMessageStatus: String = "",
    val displayLastMessage: String = ""
)


class ConversationsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val cryptoManager = CryptoManager()

    private val _uiState = MutableStateFlow<ConversationsUiState>(ConversationsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var mappingJob: Job? = null
    private var conversationsListener: ListenerRegistration? = null
    private var isListenerAttached = false

    fun fetchConversations(context: Context) {
        if (isListenerAttached) return

        try {
            cryptoManager.init(context.applicationContext)
        } catch (e: Exception) {
            _uiState.value = ConversationsUiState.Error("Error de seguridad.")
            return
        }

        val currentUserUid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = ConversationsUiState.Loading
            val currentUser = try {
                db.collection("users").document(currentUserUid).get().await().toObject(User::class.java)
            } catch (e: Exception) { null }
            val currentUserLang = currentUser?.preferredLanguage ?: "es"

            conversationsListener = db.collection("chats")
                .whereArrayContains("participants", currentUserUid)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        _uiState.value = ConversationsUiState.Error("Error al cargar los chats.")
                        return@addSnapshotListener
                    }

                    mappingJob?.cancel()
                    mappingJob = viewModelScope.launch(Dispatchers.IO) {
                        val chatDocuments = snapshot?.documents ?: emptyList()

                        val newChatsInfo = mutableListOf<ChatInfo>()
                        for (doc in chatDocuments) {
                            val chat = doc.toObject(Chat::class.java) ?: continue
                            val otherUserId = chat.participants.firstOrNull { it != currentUserUid }
                            val chatId = doc.id
                            val otherUser = if (otherUserId != null) {
                                try { db.collection("users").document(otherUserId).get().await().toObject(User::class.java) } catch (e: Exception) { null }
                            } else { null }

                            val unreadCount = try {
                                val messagesQuery = db.collection("chats").document(chatId)
                                    .collection("messages").whereNotEqualTo("senderId", currentUserUid).get().await()
                                messagesQuery.documents.mapNotNull { it.toObject(Message::class.java) }.count { !it.readBy.contains(currentUserUid) }
                            } catch (e: Exception) { 0 }

                            var lastMessageStatus = ""
                            if (chat.lastMessageSenderId == currentUserUid) {
                                try {
                                    val lastMessageQuery = db.collection("chats").document(chatId).collection("messages").whereEqualTo("senderId", currentUserUid).orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get().await()
                                    val lastMessage = lastMessageQuery.documents.firstOrNull()
                                    if (otherUserId != null) {
                                        val readBy = lastMessage?.get("readBy") as? List<*>
                                        lastMessageStatus = if (readBy?.contains(otherUserId) == true) "leido" else "enviado"
                                    } else {
                                        lastMessageStatus = "enviado"
                                    }
                                } catch (e: Exception) { lastMessageStatus = "" }
                            }

                            val isTyping = chat.typingUsers.any { it != currentUserUid }

                            // -> INICIO DE BLOQUE DE DIAGNÓSTICO AVANZADO
                            val displayMessage = try {
                                val lastMessageMapManual = doc.get("lastMessage") as? Map<String, String> ?: emptyMap()
                                val encryptedPayload = lastMessageMapManual[currentUserUid]
                                if (encryptedPayload.isNullOrBlank()) {
                                    if (chat.lastMessageSenderId == currentUserUid) "Tú: ..." else "..."
                                } else {
                                    val decryptedJson = cryptoManager.decrypt(encryptedPayload)
                                    val type = object : TypeToken<Map<String, String>>() {}.type
                                    val translations: Map<String, String> = Gson().fromJson(decryptedJson, type)
                                    val finalMsg = translations[currentUserLang] ?: translations.values.firstOrNull() ?: "..."
                                    if (chat.lastMessageSenderId == currentUserUid) "Tú: $finalMsg" else finalMsg
                                }
                            } catch (e: Exception) {
                                "🔒 Mensaje cifrado"
                            }
                            newChatsInfo.add(ChatInfo(chat, otherUser, chatId, unreadCount, isTyping, lastMessageStatus, displayMessage))
                        }

                        withContext(Dispatchers.Main) {
                            _uiState.value = ConversationsUiState.Success(newChatsInfo)
                        }
                    }
                }
            isListenerAttached = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        conversationsListener?.remove()
    }
}