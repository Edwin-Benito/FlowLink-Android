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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import mx.castillo.edwin.mensajeria.data.Message
import mx.castillo.edwin.mensajeria.data.User
import mx.castillo.edwin.mensajeria.security.CryptoManager

// Estado para la UI de Mensajes Destacados
data class StarredMessagesUiState(
    val starredMessages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class StarredMessagesViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val cryptoManager = CryptoManager()

    private val _uiState = MutableStateFlow(StarredMessagesUiState())
    val uiState = _uiState.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null
    private var processingJob: Job? = null

    fun loadStarredMessages(chatId: String?, context: Context) {
        if (chatId == null) {
            _uiState.value = StarredMessagesUiState(isLoading = false, error = "ID de chat inválido.")
            return
        }

        val currentUserUid = auth.currentUser?.uid
        Log.d("AuthCheck", "StarredMessagesVM: Current User UID = $currentUserUid")
        if (currentUserUid == null) {
            _uiState.value = StarredMessagesUiState(isLoading = false, error = "Usuario no autenticado.")
            return
        }

        // Inicializa CryptoManager (importante manejar errores aquí también)
        try {
            cryptoManager.init(context.applicationContext)
        } catch (e: Exception) {
            _uiState.value = StarredMessagesUiState(isLoading = false, error = "Error de seguridad al inicializar.")
            return
        }

        viewModelScope.launch {
            // Obtén el idioma preferido del usuario actual una sola vez
            val currentUser = try {
                db.collection("users").document(currentUserUid).get().await().toObject(User::class.java)
            } catch (e: Exception) { null }
            val currentUserLanguage = currentUser?.preferredLanguage ?: "es" // Idioma por defecto 'es'

            _uiState.value = StarredMessagesUiState(isLoading = true)

            // Limpia el listener anterior si existe
            listenerRegistration?.remove()

            // Crea la consulta a Firestore
            listenerRegistration = db.collection("chats").document(chatId)
                .collection("messages")
                .whereEqualTo("isStarred", true) // Filtra solo los destacados
                .orderBy("timestamp", Query.Direction.DESCENDING) // Muestra los más recientes primero
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _uiState.value = StarredMessagesUiState(isLoading = false, error = "Error al cargar mensajes.")
                        return@addSnapshotListener
                    }

                    // Cancela el trabajo de procesamiento anterior si llega una nueva actualización
                    processingJob?.cancel()
                    processingJob = viewModelScope.launch(Dispatchers.IO) { // Usa un hilo de fondo
                        val messagesFromDb = snapshot?.toObjects(Message::class.java) ?: emptyList()

                        // Filtra mensajes eliminados para el usuario actual
                        val visibleMessages = messagesFromDb.filter { msg ->
                            !msg.deletedFor.contains(currentUserUid)
                        }

                        // Procesa (desencripta y traduce) los mensajes visibles
                        val processedMessages = visibleMessages.mapNotNull { msg ->
                            // No procesa mensajes eliminados globalmente o imágenes (ya que no tienen texto en payloads)
                            if (msg.isDeleted || msg.imageUrl != null) {
                                msg // Devuelve el mensaje tal cual si es una imagen o está eliminado
                            } else {
                                val encryptedPayload = msg.payloads[currentUserUid]
                                if (encryptedPayload.isNullOrBlank()) {
                                    // Si no hay payload para el usuario, no se puede mostrar
                                    null // Opcional: podrías devolver msg.copy(text = "...") si prefieres
                                } else {
                                    try {
                                        val decryptedJson = cryptoManager.decrypt(encryptedPayload)
                                        val type = object : TypeToken<Map<String, String>>() {}.type
                                        val translations: Map<String, String> = Gson().fromJson(decryptedJson, type)
                                        val displayText = translations[currentUserLanguage] ?: translations.values.firstOrNull() ?: "Traducción no disponible"
                                        msg.copy(text = displayText) // Actualiza el campo 'text' con el desencriptado/traducido
                                    } catch (e: Exception) {
                                        Log.e("StarredVM", "Fallo al descifrar mensaje destacado ID: ${msg.id}", e)
                                        msg.copy(text = "🔒 Error al descifrar") // Muestra error si falla
                                    }
                                }
                            }
                        }

                        // Actualiza la UI en el hilo principal
                        withContext(Dispatchers.Main) {
                            _uiState.value = StarredMessagesUiState(
                                starredMessages = processedMessages,
                                isLoading = false
                            )
                        }
                    }
                }
        }
    }

    // Limpia el listener cuando el ViewModel se destruye
    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
        processingJob?.cancel() // Asegura que el trabajo de procesamiento también se cancele
    }
}