package mx.castillo.edwin.mensajeria.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import mx.castillo.edwin.mensajeria.security.CryptoManager

sealed class SplashDestination {
    object Loading : SplashDestination()
    object Home : SplashDestination()
    object Login : SplashDestination()
}

class SplashViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val cryptoManager = CryptoManager()
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow() // <-- AÑADIDO (para que puedas mostrar toasts)

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination = _destination.asStateFlow()

    // --- INICIO: CÓDIGO MODIFICADO ---
    // El 'init' se elimina, ya que necesitamos el 'context' de la UI.

    /**
     * Esta es la NUEVA función de arranque.
     * Debe ser llamada desde la UI (SplashScreen) con el Contexto.
     */
    fun initializeApp(context: Context) {
        // Evita que se ejecute varias veces si ya está cargando
        if (_destination.value != SplashDestination.Loading) return

        viewModelScope.launch {
            if (auth.currentUser != null) {
                try {
                    // 1. Intenta sincronizar la clave pública
                    checkAndSyncPublicKey(context.applicationContext)
                    
                    // 2. Si todo sale bien, navega a Home
                    _destination.value = SplashDestination.Home
                    
                } catch (e: Exception) {
                    // --- ✅ CORRECCIÓN CRÍTICA ---
                    Log.e("SplashVM", "Error al sincronizar claves públicas", e)
                    val errorMessage = when {
                        e.message?.contains("UNAVAILABLE") == true -> 
                            "Sin conexión. Algunas funciones pueden no estar disponibles."
                        e.message?.contains("cifrado") == true -> 
                            "Error de seguridad. Tus mensajes podrían no descifrarse correctamente."
                        else -> 
                            "Advertencia: No se pudo verificar la seguridad. Continuando..."
                    }
                    _toastMessage.value = errorMessage
                    
                    // DECISIÓN: Permitir continuar a Home aunque falle la sincronización
                    // El usuario podrá usar la app, pero puede tener problemas de descifrado
                    // Alternativa: Cerrar sesión y enviar a Login
                    
                    // OPCIÓN A: Continuar a Home (recomendado para no bloquear al usuario)
                    //_destination.value = SplashDestination.Home
                    
                    // OPCIÓN B: Cerrar sesión y enviar a Login (descomenta si prefieres esta opción)
                     auth.signOut()
                     _destination.value = SplashDestination.Login
                    // --- FIN CORRECCIÓN ---
                }
            } else {
                // 3. Si no hay usuario autenticado, va a Login
                _destination.value = SplashDestination.Login
            }
        }
    }
    // --- FIN: CÓDIGO MODIFICADO ---


    // Esta función está perfecta, solo la llamamos desde 'initializeApp'
    fun checkAndSyncPublicKey(context: Context) {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")
                val userRef = db.collection("users").document(uid)

                // --- ✅ MANEJO DE ERROR EN INIT ---
                try {
                    cryptoManager.init(context)
                } catch (e: Exception) {
                    Log.e("CryptoInit", "Error al inicializar CryptoManager", e)
                    throw Exception("No se pudo inicializar el sistema de cifrado: ${e.message}", e)
                }
                // --- FIN CORRECCIÓN ---

                val currentDevicePublicKey = cryptoManager.getPublicKeyString()
                val userDocument = userRef.get().await()
                val storedPublicKey = userDocument.getString("publicKey")

                // Compara la clave del dispositivo con la de Firestore
                if (storedPublicKey != currentDevicePublicKey || !userDocument.exists()) {
                    Log.d("PublicKeySync", "La clave pública es diferente. Actualizando Firestore...")

                    val keyData = mapOf("publicKey" to currentDevicePublicKey)
                    userRef.set(keyData, com.google.firebase.firestore.SetOptions.merge()).await()

                    Log.d("PublicKeySync", "Clave pública sincronizada para el usuario $uid")
                } else {
                    Log.d("PublicKeySync", "La clave pública ya está sincronizada.")
                }
            } catch (e: Exception) {
                Log.e("PublicKeySync", "Error al sincronizar la clave pública", e)
                _toastMessage.value = "Error al verificar la seguridad de la cuenta."
                // --- ✅ CORRECCIÓN: PROPAGA LA EXCEPCIÓN ---
                // Esto permite que 'initializeApp' la capture y decida qué hacer
                throw e
                // --- FIN CORRECCIÓN ---
            }
        }
    }
}