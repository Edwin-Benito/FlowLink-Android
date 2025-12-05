package mx.castillo.edwin.mensajeria.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import mx.castillo.edwin.mensajeria.data.User

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    object SuccessFirstTime : LoginUiState() // Estado para nuevos usuarios sociales
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _toastMessage.value = "Por favor, ingresa email y contraseña."
            return
        }
        _uiState.value = LoginUiState.Loading

        // --- INICIO DE CORRECCIÓN ---
        viewModelScope.launch(Dispatchers.IO) { // 1. Lanzar en hilo de IO
            try {
                auth.signInWithEmailAndPassword(email, password).await() // 2. Usar await()

                // 3. Actualizar estado y token (dentro de la corutina)
                updateFcmTokenAfterLogin()

                // 4. Actualizar UI/Toast en el hilo principal
                withContext(Dispatchers.Main) {
                    _toastMessage.value = "Inicio de sesión exitoso"
                    _uiState.value = LoginUiState.Success
                }

            } catch (e: Exception) {
                // 5. Manejar error en el hilo principal
                withContext(Dispatchers.Main) {
                    _toastMessage.value = "Credenciales incorrectas. Verifica tu email y contraseña."
                    _uiState.value = LoginUiState.Error("Credenciales incorrectas")
                }
            }
        }
        // --- FIN DE CORRECCIÓN ---
    }

    fun signInWithCredential(credential: AuthCredential) {
        _uiState.value = LoginUiState.Loading

        // --- INICIO DE CORRECCIÓN ---
        viewModelScope.launch(Dispatchers.IO) { // 1. Lanzar en hilo de IO
            try {
                val authResult = auth.signInWithCredential(credential).await() // 2. Usar await()
                val firebaseUser = authResult.user ?: throw Exception("Usuario no encontrado")
                val userRef = db.collection("users").document(firebaseUser.uid)

                val document = userRef.get().await() // 3. Usar await()

                if (document.exists()) {
                    // CASO 1: El usuario ya tiene un perfil
                    updateFcmTokenAfterLogin()
                    withContext(Dispatchers.Main) {
                        _toastMessage.value = "Inicio de sesión exitoso"
                        _uiState.value = LoginUiState.Success
                    }
                } else {
                    // CASO 2: Es un usuario nuevo, su perfil no existe
                    val newUser = User(
                        uid = firebaseUser.uid,
                        displayName = firebaseUser.displayName ?: "Usuario",
                        email = firebaseUser.email ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString(),
                        username = "" // El username se establecerá en la siguiente pantalla
                    )
                    userRef.set(newUser).await() // 4. Usar await()
                    withContext(Dispatchers.Main) {
                        _uiState.value = LoginUiState.SuccessFirstTime
                    }
                }
            } catch (e: Exception) {
                // 5. Manejar errores
                withContext(Dispatchers.Main) {
                    _toastMessage.value = e.message ?: "Error al iniciar sesión."
                    _uiState.value = LoginUiState.Error(e.message ?: "Error desconocido")
                }
            }
        }
        // --- FIN DE CORRECCIÓN ---
    }

    /**
     * Obtiene el token de FCM más reciente y lo guarda en Firestore.
     * Se debe llamar DESPUÉS de un inicio de sesión exitoso.
     */
    private suspend fun updateFcmTokenAfterLogin() {
        val uid = auth.currentUser?.uid ?: return // Debe haber un usuario
        try {
            // 1. Obtiene el token de FCM del dispositivo
            val token = FirebaseMessaging.getInstance().token.await()

            // 2. Guarda el token en Firestore
            val userRef = db.collection("users").document(uid)
            userRef.update("fcmToken", token).await()
            Log.d("LoginViewModel", "fcmToken actualizado en Firestore.")

        } catch (e: Exception) {
            Log.e("LoginViewModel", "Error al actualizar fcmToken: ${e.message}", e)
            // No bloqueamos al usuario, pero registramos el error
            withContext(Dispatchers.Main) {
                _toastMessage.value = "No se pudo sincronizar el token de notificación."
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _toastMessage.value = "Por favor, ingresa tu correo electrónico."
            return
        }
        _uiState.value = LoginUiState.Loading

        // --- INICIO DE CORRECCIÓN ---
        viewModelScope.launch(Dispatchers.IO) {
            try {
                auth.sendPasswordResetEmail(email).await()
                withContext(Dispatchers.Main) {
                    _toastMessage.value = "Enlace de recuperación enviado. Revisa tu correo."
                    _uiState.value = LoginUiState.Idle
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _toastMessage.value = e.message ?: "No se pudo enviar el correo."
                    _uiState.value = LoginUiState.Error("Fallo al enviar correo")
                }
            }
        }
        // --- FIN DE CORRECCIÓN ---
    }

    fun onToastShown() {
        _toastMessage.value = null
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }

    fun setUiState(newState: LoginUiState) {
        _uiState.value = newState
        if (newState is LoginUiState.Error) {
            _toastMessage.value = newState.message
        }
    }
}