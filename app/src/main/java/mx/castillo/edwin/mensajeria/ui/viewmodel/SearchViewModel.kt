package mx.castillo.edwin.mensajeria.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import mx.castillo.edwin.mensajeria.data.User
import mx.castillo.edwin.mensajeria.ui.model.UserListUiState

@OptIn(FlowPreview::class) // Necesario para usar debounce
class SearchViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<UserListUiState>(UserListUiState.Idle)
    val uiState = _uiState.asStateFlow()

    // Un flujo para el texto de búsqueda que se actualiza desde la UI
    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(500) // Espera 500ms después de la última letra escrita
                .filter { query ->
                    if (query.length < 3) {
                        // Si la búsqueda es corta, limpia los resultados
                        if (_uiState.value !is UserListUiState.Idle) {
                            _uiState.value = UserListUiState.Idle
                        }
                        return@filter false
                    } else {
                        return@filter true
                    }
                }
                .collect { query ->
                    // Cuando el flujo pasa los filtros, se ejecuta la búsqueda
                    performSearch(query)
                }
        }
    }

    // La UI llamará a esta función en cada cambio de texto
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = UserListUiState.Loading
        try {
            // Consulta de prefijo: busca usernames que empiezan con 'query'
            val result = db.collection("users")
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + '\uf8ff')
                .get()
                .await()

            val usersList = result.toObjects(User::class.java)
            val currentUserUid = auth.currentUser?.uid
            val filteredList = usersList.filter { it.uid != currentUserUid }

            if (filteredList.isEmpty()) {
                _uiState.value = UserListUiState.Error("No se encontraron usuarios.")
            } else {
                _uiState.value = UserListUiState.Success(filteredList)
            }
        } catch (e: Exception) {
            _uiState.value = UserListUiState.Error("Error en la búsqueda: ${e.message}")
        }
    }
}