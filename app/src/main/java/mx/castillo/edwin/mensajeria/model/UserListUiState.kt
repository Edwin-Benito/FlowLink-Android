package mx.castillo.edwin.mensajeria.ui.model

import mx.castillo.edwin.mensajeria.data.User

sealed class UserListUiState {
    object Idle : UserListUiState()
    object Loading : UserListUiState()
    data class Success(val users: List<User>) : UserListUiState()
    data class Error(val message: String) : UserListUiState()
}