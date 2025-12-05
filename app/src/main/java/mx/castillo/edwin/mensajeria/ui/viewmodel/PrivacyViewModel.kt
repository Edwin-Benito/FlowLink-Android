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

class PrivacyViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    init {
        fetchUserData()
    }

    private fun fetchUserData() {
        val currentUserUid = auth.currentUser?.uid ?: return
        db.collection("users").document(currentUserUid)
            .addSnapshotListener { snapshot, _ ->
                _user.value = snapshot?.toObject(User::class.java)
            }
    }

    fun updateLastSeenPrivacy(setting: String) {
        viewModelScope.launch {
            val currentUserUid = auth.currentUser?.uid ?: return@launch
            try {
                db.collection("users").document(currentUserUid)
                    .update("lastSeenPrivacy", setting)
                    .await()
            } catch (e: Exception) {
                // Manejar error
            }
        }
    }

    fun updateReadReceipts(isEnabled: Boolean) {
        viewModelScope.launch {
            val currentUserUid = auth.currentUser?.uid ?: return@launch
            try {
                db.collection("users").document(currentUserUid)
                    .update("readReceiptsEnabled", isEnabled)
                    .await()
            } catch (e: Exception) {
                // Manejar error si es necesario
            }
        }
    }
}