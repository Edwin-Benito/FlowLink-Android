package mx.castillo.edwin.mensajeria.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log
import mx.castillo.edwin.mensajeria.data.Chat
import mx.castillo.edwin.mensajeria.data.User
import mx.castillo.edwin.mensajeria.ui.model.UserListUiState

class ContactsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // --- Declaración ÚNICA Y CORRECTA ---
    private val _uiState = MutableStateFlow<UserListUiState>(UserListUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    init {
        fetchUsers()
    }

    private fun fetchUsers() {
        viewModelScope.launch {
            val currentUser = auth.currentUser ?: return@launch
            _uiState.value = UserListUiState.Loading

            db.collection("users").document(currentUser.uid)
                .collection("contacts")
                .addSnapshotListener { contactIdsSnapshot, e ->
                    if (e != null) {
                        _uiState.value = UserListUiState.Error("Error al cargar IDs de contactos.")
                        return@addSnapshotListener
                    }

                    val contactIds = contactIdsSnapshot?.documents?.map { it.id }

                    if (contactIds.isNullOrEmpty()) {
                        _uiState.value = UserListUiState.Success(emptyList())
                    } else {
                        db.collection("users")
                            .whereIn("uid", contactIds)
                            .get()
                            .addOnSuccessListener { usersSnapshot ->
                                val usersList = usersSnapshot.toObjects(User::class.java)
                                _uiState.value = UserListUiState.Success(usersList)
                            }
                            .addOnFailureListener {
                                _uiState.value = UserListUiState.Error("Error al cargar perfiles de contactos.")
                            }
                    }
                }
        }
    }

    fun findOrCreateChat(contactUid: String, onSuccess: (chatId: String) -> Unit) {
        val currentUserUid = auth.currentUser?.uid
        
        // --- ✅ VALIDACIÓN TEMPRANA ---
        if (currentUserUid == null) {
            Log.e("ContactsVM", "findOrCreateChat: Usuario no autenticado")
            _toastMessage.value = "Error: Usuario no autenticado"
            return
        }

        if (contactUid.isBlank()) {
            Log.e("ContactsVM", "findOrCreateChat: contactUid está vacío")
            _toastMessage.value = "Error: ID de contacto inválido"
            return
        }

        // --- ✅ LÓGICA ENVUELTA EN TRY-CATCH ---
        viewModelScope.launch {
            try {
                // 1. Buscar chat existente
                val querySnapshot = db.collection("chats")
                    .whereArrayContains("participants", currentUserUid)
                    .get()
                    .await() // <-- Usar await() para manejo síncrono de errores

                // 2. Filtrar en el cliente
                val existingChat = querySnapshot.documents.find { doc ->
                    val participants = doc.get("participants") as? List<*>
                    participants?.contains(contactUid) == true && participants.size == 2
                }

                if (existingChat != null) {
                    // Chat encontrado, devolver su ID
                    Log.d("ContactsVM", "Chat existente encontrado: ${existingChat.id}")
                    onSuccess(existingChat.id)
                } else {
                    // 3. Crear nuevo chat
                    Log.d("ContactsVM", "Creando nuevo chat entre $currentUserUid y $contactUid")
                    
                    val newChatData = hashMapOf(
                        "participants" to listOf(currentUserUid, contactUid),
                        "lastMessage" to emptyMap<String, String>(),
                        "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                        "typingUsers" to emptyList<String>(),
                        "lastMessageSenderId" to null
                    )

                    val docRef = db.collection("chats").add(newChatData).await()
                    
                    Log.d("ContactsVM", "Nuevo chat creado con ID: ${docRef.id}")
                    onSuccess(docRef.id)
                }

            } catch (e: Exception) {
                // --- ✅ MANEJO DE ERRORES ROBUSTO ---
                Log.e("ContactsVM", "Error en findOrCreateChat para contacto $contactUid", e)
                
                // Mensaje específico según el tipo de error
                val errorMessage = when {
                    e.message?.contains("PERMISSION_DENIED") == true -> 
                        "No tienes permisos para crear chats. Verifica tu conexión."
                    e.message?.contains("network") == true || e.message?.contains("UNAVAILABLE") == true -> 
                        "Sin conexión a Internet. Inténtalo de nuevo."
                    else -> 
                        "Error al iniciar el chat: ${e.message}"
                }
                
                _toastMessage.value = errorMessage
                
                // NO llamamos a onSuccess, dejamos que la UI maneje el estado
                // La navegación solo ocurre si todo sale bien
            }
        }
    }


    fun addContact(contactUid: String, onSuccess: () -> Unit) {
        val currentUserUid = auth.currentUser?.uid ?: return
        db.collection("users").document(currentUserUid)
            .collection("contacts").document(contactUid)
            .set(mapOf("addedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener {
                _toastMessage.value = "Contacto añadido con éxito."
                onSuccess()
            }
            .addOnFailureListener { e ->
                _toastMessage.value = "Error al añadir el contacto: ${e.message}"
            }
    }

    fun onToastShown() {
        _toastMessage.value = null
    }
}