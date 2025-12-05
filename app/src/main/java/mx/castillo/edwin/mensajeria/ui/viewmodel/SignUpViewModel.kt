package mx.castillo.edwin.mensajeria.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import mx.castillo.edwin.mensajeria.data.User

// Estado de la UI para la pantalla de registro
sealed class SignUpUiState {
    object Idle : SignUpUiState()
    object Loading : SignUpUiState()
    object Success : SignUpUiState()
    data class Error(val message: String) : SignUpUiState()
}

class SignUpViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    fun onToastShown() {
        _toastMessage.value = null
    }

    fun signUpWithEmail(displayName: String, username: String, email: String, password: String) {
        // ... (Validaciones iniciales se mantienen igual) ...

        _uiState.value = SignUpUiState.Loading
        viewModelScope.launch {
            try {
                // Paso 1: Verificar si el nombre de usuario ya existe
                val usernameQuery = db.collection("users").whereEqualTo("username", username).get().await()
                if (!usernameQuery.isEmpty) {
                    _uiState.value = SignUpUiState.Error("El nombre de usuario ya está en uso. Por favor, elige otro.")
                    return@launch
                }

                // Paso 2: Si el username está libre, crear el usuario en Firebase Auth
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    // --- CORRECCIÓN CLAVE ---
                    // Se envía el correo de verificación al usuario recién creado
                    firebaseUser.sendEmailVerification().await()

                    // Paso 3: Crear el objeto User y guardarlo en Firestore
                    val newUser = User(
                        uid = firebaseUser.uid,
                        displayName = displayName,
                        username = username,
                        email = email
                    )
                    saveUserToFirestore(newUser)
                } else {
                    _uiState.value = SignUpUiState.Error("No se pudo crear el usuario.")
                }

            } catch (e: Exception) {
                _uiState.value = SignUpUiState.Error(e.message ?: "Ocurrió un error desconocido.")
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            try {
                val firebaseUser = auth.currentUser
                firebaseUser?.sendEmailVerification()?.await()
                _toastMessage.value = "Correo de verificación reenviado."
            } catch (e: Exception) {
                _toastMessage.value = e.message ?: "Error al reenviar el correo."
            }
        }
    }

    private suspend fun saveUserToFirestore(user: User) {
        try {
            db.collection("users").document(user.uid).set(user).await()
            _uiState.value = SignUpUiState.Success
        } catch (e: Exception) {
            _uiState.value = SignUpUiState.Error("Error al guardar los datos del perfil.")
        }
    }

    fun resetState() {
        _uiState.value = SignUpUiState.Idle
    }
}