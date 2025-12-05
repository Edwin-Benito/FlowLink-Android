package mx.castillo.edwin.mensajeria.ui.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import mx.castillo.edwin.mensajeria.data.User
import mx.castillo.edwin.mensajeria.ui.components.Primary
import mx.castillo.edwin.mensajeria.ui.model.UserListUiState
import mx.castillo.edwin.mensajeria.ui.viewmodel.ContactsViewModel
import mx.castillo.edwin.mensajeria.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchUsersScreen(
    navController: NavController,
    isDarkTheme: Boolean = false,
    searchViewModel: SearchViewModel = viewModel(),
    contactsViewModel: ContactsViewModel = viewModel(),
    showToast: (String) -> Unit
) {
    val uiState by searchViewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var addedContactIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val backgroundColor = if (isDarkTheme) Color(0xFF101C22) else Color(0xFFF5F7F8)
    if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF0F172A)

    // Observamos los toasts del ContactsViewModel para saber cuándo se añade un contacto
    val toastMessage by contactsViewModel.toastMessage.collectAsState()
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            showToast(it)
            contactsViewModel.onToastShown()
        }
    }

    Scaffold(
        containerColor = backgroundColor
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            stickyHeader {
                SearchHeader(
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        searchViewModel.onSearchQueryChange(it)
                    },
                    navController = navController,
                    isDarkTheme = isDarkTheme
                )
            }

            item {
                if (uiState is UserListUiState.Success || uiState is UserListUiState.Error) {
                    val title = if(uiState is UserListUiState.Success) "Resultados de la búsqueda" else "Información"
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (val state = uiState) {
                        is UserListUiState.Idle -> Text("Busca usuarios por su @nombredeusuario")
                        is UserListUiState.Loading -> CircularProgressIndicator()
                        is UserListUiState.Error -> Text(text = state.message)
                        is UserListUiState.Success -> {
                            // La lista se muestra en la sección de 'items'
                        }
                    }
                }
            }

            if (uiState is UserListUiState.Success) {
                items((uiState as UserListUiState.Success).users) { user ->
                    UserSearchRow(
                        user = user,
                        isAdded = user.uid in addedContactIds,
                        onAddClick = {
                            contactsViewModel.addContact(user.uid) {
                                addedContactIds = addedContactIds + user.uid
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    navController: NavController,
    isDarkTheme: Boolean
) {
    val headerColor = (if (isDarkTheme) Color(0xFF101C22) else Color(0xFFF5F7F8)).copy(alpha = 0.9f)
    val inputBackgroundColor = if (isDarkTheme) Color(0xFF1F2937) else Color(0xFFE5E7EB)

    Column(modifier = Modifier.background(headerColor)) {
        // ... Barra de título ...
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Regresar")
            }
            Text(
                "Añadir Contacto",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(48.dp)) // Espacio para centrar el título
        }

        // ... Barra de búsqueda ...
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Buscar por @nombredeusuario") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = inputBackgroundColor,
                unfocusedContainerColor = inputBackgroundColor,
                disabledContainerColor = inputBackgroundColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            singleLine = true
        )
    }
}

@Composable
fun UserSearchRow(user: User, isAdded: Boolean, onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.photoUrl ?: "https://i.pravatar.cc/150?u=${user.uid}",
            contentDescription = "Avatar de ${user.displayName}",
            modifier = Modifier.size(48.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = user.displayName, fontWeight = FontWeight.Bold)
            Text(text = "@${user.username}", style = MaterialTheme.typography.bodyMedium)
        }
        Button(
            onClick = onAddClick,
            enabled = !isAdded,
            colors = if (isAdded) ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Primary)
            else ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            if(isAdded) Icon(Icons.Default.Check, contentDescription = "Añadido")
            Text(if (isAdded) "Añadido" else "Añadir")
        }
    }
}