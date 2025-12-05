package mx.castillo.edwin.mensajeria.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import mx.castillo.edwin.mensajeria.data.Chat
import mx.castillo.edwin.mensajeria.data.User

data class ContactInfoUiState(
    val user: User? = null,
    val chat: Chat? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val disappearingMessagesDuration: Long = 0,
    val isMuted: Boolean = false
)

class ContactInfoViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow(ContactInfoUiState())
    val uiState = _uiState.asStateFlow()

    private var currentChatId: String? = null
    private var chatListener: ListenerRegistration? = null

    // --- FUNCIÓN 'loadContactInfo' (MODIFICADA) ---
    // Ahora acepta el 'chatId' real desde la pantalla de chat
    fun loadContactInfo(userId: String, chatId: String) {
        if (chatId == currentChatId) return // Evita recargar si ya está cargado

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            currentChatId = chatId // <-- USA EL CHAT ID REAL

            val currentUserUid = auth.currentUser?.uid
            if (currentUserUid == null) {
                _uiState.value = ContactInfoUiState(isLoading = false, error = "Usuario no autenticado.")
                return@launch
            }

            // --- CÁLCULO DE ID ELIMINADO ---
            // val chatId = if (currentUserUid < userId) "$currentUserUid-$userId" else "$userId-$currentUserUid" // <-- YA NO SE USA

            try {
                // 1. Cargar datos del usuario (esto solo se hace una vez)
                val userDocument = db.collection("users").document(userId).get().await()
                val loadedUser = userDocument.toObject(User::class.java)

                if (loadedUser == null) {
                    _uiState.value = ContactInfoUiState(isLoading = false, error = "Usuario no encontrado.")
                    return@launch
                }

                _uiState.update { it.copy(user = loadedUser) }

                // 2. Escuchar datos del chat (esto se actualiza en tiempo real)
                chatListener?.remove() // Limpia listener anterior
                chatListener = db.collection("chats").document(chatId) // <-- USA EL CHAT ID REAL
                    .addSnapshotListener { chatDocument, error ->

                        if (error != null) {
                            Log.w("ContactInfoVM", "Error al escuchar el chat $chatId", error)
                            _uiState.update { it.copy(isLoading = false, chat = null, disappearingMessagesDuration = 0) }
                            return@addSnapshotListener
                        }

                        var loadedChat: Chat? = null
                        if (chatDocument != null && chatDocument.exists()) {
                            loadedChat = chatDocument.toObject(Chat::class.java)
                        } else {
                            Log.w("ContactInfoVM", "Documento del chat $chatId no existe (aún).")
                        }

                        // 3. Actualizar el estado final
                        _uiState.update {
                            it.copy(
                                user = loadedUser,
                                chat = loadedChat,
                                isLoading = false,
                                disappearingMessagesDuration = loadedChat?.disappearingMessagesDuration ?: 0,
                                isMuted = loadedChat?.mutedBy?.contains(auth.currentUser?.uid) == true
                            )
                        }
                    }

            } catch (e: Exception) {
                Log.e("ContactInfoVM", "Error general al cargar información", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = "No se pudo cargar la información completa.")
            }
        }
    }
    // --- FIN FUNCIÓN MODIFICADA ---


    // --- FUNCIÓN 'updateDisappearingMessagesDuration' (Sin cambios, pero ahora funcionará) ---
    fun updateDisappearingMessagesDuration(durationSeconds: Long) {
        val chatId = currentChatId ?: return
        val currentUserUid = auth.currentUser?.uid ?: return
        val otherUserId = _uiState.value.user?.uid ?: return

        viewModelScope.launch {
            try {
                val chatData = mapOf(
                    "disappearingMessagesDuration" to durationSeconds,
                    "participants" to listOf(currentUserUid, otherUserId)
                )

                // Esta línea ahora escribirá en el ID correcto (ej. wmPg43...)
                db.collection("chats").document(chatId)
                    .set(chatData, SetOptions.merge())
                    .await()

                Log.d("ContactInfoVM", "Duración de mensajes temporales actualizada a $durationSeconds s para chat $chatId")

            } catch (e: Exception) {
                Log.e("ContactInfoVM", "Error al actualizar/crear duración para chat $chatId", e)
                _uiState.update { it.copy(error = "Error al guardar la configuración.") }
            }
        }
    }

    // --- Funciones de acción (sin cambios) ---
    fun blockUser(userIdToBlock: String) {
        Log.d("ContactInfo", "Simulando bloqueo de: $userIdToBlock")
    }
    fun reportUser(userIdToReport: String) {
        Log.d("ContactInfo", "Simulando reporte de: $userIdToReport")
    }
    fun toggleMuteNotifications(userId: String, isMuted: Boolean) {
        val chatId = currentChatId ?: return
        val currentUserUid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val chatRef = db.collection("chats").document(chatId)

                db.runTransaction { transaction ->
                    val snapshot = transaction.get(chatRef)
                    val currentMutedBy = snapshot.get("mutedBy") as? MutableList<String> ?: mutableListOf()

                    if (isMuted) {
                        if (!currentMutedBy.contains(currentUserUid)) currentMutedBy.add(currentUserUid)
                    } else {
                        currentMutedBy.remove(currentUserUid)
                    }

                    transaction.update(chatRef, "mutedBy", currentMutedBy)
                }.await()

                // Actualiza el estado local
                _uiState.update { it.copy(isMuted = isMuted) }

                Log.d("ContactInfoVM", "Silencio actualizado para $chatId: $isMuted")
            } catch (e: Exception) {
                Log.e("ContactInfoVM", "Error al actualizar silencio de notificaciones", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatListener?.remove()
    }
}