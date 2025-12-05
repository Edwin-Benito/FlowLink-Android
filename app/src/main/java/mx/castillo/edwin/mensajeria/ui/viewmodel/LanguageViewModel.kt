package mx.castillo.edwin.mensajeria.ui.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class LanguageViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Guarda la preferencia de idioma y marca que el diálogo ya fue mostrado.
     * @param langCode El código del idioma (ej. "es", "en").
     * @param prefs La instancia de SharedPreferences para guardar la bandera.
     */
    fun saveLanguagePreference(langCode: String, prefs: SharedPreferences) {
        val currentUserUid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            // Prepara los datos que quieres guardar
            val languageData = mapOf("preferredLanguage" to langCode)

            // Usa .set() con SetOptions.merge()
            // Esto CREARÁ el documento si no existe, o
            // ACTUALIZARÁ solo el campo 'preferredLanguage' si sí existe.
            db.collection("users").document(currentUserUid)
                .set(languageData, com.google.firebase.firestore.SetOptions.merge()) // <-- LÍNEA CORREGIDA
                .addOnSuccessListener {
                    prefs.edit().putBoolean("language_dialog_shown", true).apply()
                }
                .addOnFailureListener {
                    // Manejar error
                    Log.e("LanguageViewModel", "No se pudo guardar el idioma", it)
                }
        }
    }
}