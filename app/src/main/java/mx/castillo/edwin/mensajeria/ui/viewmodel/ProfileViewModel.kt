package mx.castillo.edwin.mensajeria.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.castillo.edwin.mensajeria.security.CryptoManager // <-- AÑADE ESTE IMPORT
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import mx.castillo.edwin.mensajeria.R
import mx.castillo.edwin.mensajeria.data.User

class ProfileViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient
    private val cryptoManager = CryptoManager() // <-- AÑADE ESTA INSTANCIA

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    init {
        fetchUserData()
    }

    private fun fetchUserData() {
        val currentUserUid = auth.currentUser?.uid
        if (currentUserUid != null) {
            db.collection("users").document(currentUserUid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        _toastMessage.value = "Error al cargar datos."
                        return@addSnapshotListener
                    }
                    _user.value = snapshot?.toObject(User::class.java)
                }
        }
    }

    fun updateDisplayName(newName: String) {
        if (newName.isBlank() || newName == _user.value?.displayName) {
            return
        }

        viewModelScope.launch {
            val currentUser = auth.currentUser ?: return@launch

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()

            try {
                currentUser.updateProfile(profileUpdates).await()
                db.collection("users").document(currentUser.uid)
                    .update("displayName", newName)
                    .await()
                _toastMessage.value = "Nombre actualizado con éxito."
            } catch (e: Exception) {
                _toastMessage.value = "Error al actualizar el nombre: ${e.message}"
            }
        }
    }

    fun updateBio(newBio: String) {
        if (newBio == _user.value?.bio) {
            return // No hacer nada si la bio no ha cambiado
        }

        viewModelScope.launch {
            val currentUserUid = auth.currentUser?.uid ?: return@launch
            try {
                db.collection("users").document(currentUserUid)
                    .update("bio", newBio)
                    .await()
                _toastMessage.value = "Biografía actualizada."
            } catch (e: Exception) {
                _toastMessage.value = "Error al actualizar la biografía."
            }
        }
    }

    fun sendPasswordReset() {
        viewModelScope.launch {
            val email = auth.currentUser?.email
            if (email == null) {
                _toastMessage.value = "No se pudo encontrar el correo del usuario."
                return@launch
            }
            try {
                auth.sendPasswordResetEmail(email).await()
                _toastMessage.value = "Correo de restablecimiento enviado a $email"
            } catch (e: Exception) {
                _toastMessage.value = "Error al enviar el correo."
            }
        }
    }

    fun onToastShown() {
        _toastMessage.value = null
    }

    // --- LÍNEA CORREGIDA ---
    fun initGoogleSignInClient(context: Context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
        try {
            cryptoManager.init(context)
            uploadPublicKeyIfNeeded()
        } catch (e: Exception) {
            _toastMessage.value = "Error de seguridad: no se pudo inicializar el cifrado."
        }
    }

    private fun uploadPublicKeyIfNeeded() {
        viewModelScope.launch {
            val currentUserUid = auth.currentUser?.uid ?: return@launch
            val userDoc = db.collection("users").document(currentUserUid).get().await()
            val storedPublicKey = userDoc.getString("publicKey")

            val currentPublicKey = cryptoManager.getPublicKeyString()

            if (storedPublicKey == null || storedPublicKey != currentPublicKey) {
                db.collection("users").document(currentUserUid)
                    .update("publicKey", currentPublicKey)
                    .await()
            }
        }
    }

    fun signOut(onSuccess: () -> Unit) {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            onSuccess()
        }
    }
}