package mx.castillo.edwin.mensajeria.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CreateUsernameViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun saveUsername(username: String) {
        val currentUserUid = auth.currentUser?.uid
        if (currentUserUid == null) {
            _uiState.value = UiState.Error("No se ha podido verificar el usuario.")
            return
        }
        if (username.length < 3) {
            _uiState.value = UiState.Error("El nombre de usuario debe tener al menos 3 caracteres.")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                // Verificar que el username no esté en uso
                val usernameQuery = db.collection("users").whereEqualTo("username", username).get().await()
                if (!usernameQuery.isEmpty) {
                    _uiState.value = UiState.Error("Ese nombre de usuario ya está en uso.")
                    return@launch
                }

                // Si está libre, actualizar el documento del usuario
                db.collection("users").document(currentUserUid)
                    .update("username", username)
                    .await()

                _uiState.value = UiState.Success

            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Ocurrió un error.")
            }
        }
    }
}