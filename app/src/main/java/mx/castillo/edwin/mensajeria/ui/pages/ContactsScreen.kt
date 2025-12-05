package mx.castillo.edwin.mensajeria.ui.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import mx.castillo.edwin.mensajeria.data.User
import mx.castillo.edwin.mensajeria.ui.components.AppBottomNavigation
import mx.castillo.edwin.mensajeria.ui.components.InputDark
import mx.castillo.edwin.mensajeria.ui.components.InputLight
import mx.castillo.edwin.mensajeria.ui.model.UserListUiState
import mx.castillo.edwin.mensajeria.ui.viewmodel.ContactsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    navController: NavController,
    isDarkTheme: Boolean = false,
    viewModel: ContactsViewModel = viewModel(),
    showToast: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val backgroundColor = if (isDarkTheme) Color(0xFF101C22) else Color(0xFFF5F7F8)

    val toastMessage by viewModel.toastMessage.collectAsState()
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            showToast(it)
            viewModel.onToastShown()
        }
    }

    Scaffold(
        bottomBar = {
            AppBottomNavigation(navController = navController, currentRoute = "contacts")
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        when (val state = uiState) {
            is UserListUiState.Loading -> { // Usamos el nuevo nombre
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UserListUiState.Error -> { // Usamos el nuevo nombre
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message)
                }
            }
            is UserListUiState.Success -> { // Usamos el nuevo nombre
                ContactsListContainer(
                    users = state.users,
                    isDarkTheme = isDarkTheme,
                    navController = navController,
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            // Añadimos el estado Idle por si acaso, aunque no debería ocurrir aquí
            is UserListUiState.Idle -> {}
        }
    }
}

// --- Esta es la ÚNICA versión de esta función ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsListContainer(
    users: List<User>,
    isDarkTheme: Boolean,
    navController: NavController,
    viewModel: ContactsViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredUsers = users.filter { it.displayName.contains(searchQuery, ignoreCase = true) }

    val backgroundColor = if (isDarkTheme) Color(0xFF101C22) else Color(0xFFF5F7F8)
    val textColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val mutedTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    LazyColumn(modifier = modifier.fillMaxSize()) {
        stickyHeader {
            Column(modifier = Modifier.background(backgroundColor.copy(alpha = 0.95f))) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Spacer(Modifier.width(48.dp))
                    Text(
                        text = "Contactos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textColor,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = { navController.navigate("search_users") }) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir contacto", tint = textColor)
                    }
                }
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    isDarkTheme = isDarkTheme
                )
            }
        }

        if (users.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxHeight(0.7f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No tienes contactos añadidos.", color = textColor)
                        Text("Pulsa el botón '+' para buscar amigos.", color = mutedTextColor)
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "MIS CONTACTOS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = mutedTextColor,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            items(filteredUsers) { user ->
                ContactRow(user = user, isDarkTheme = isDarkTheme, navController = navController, viewModel = viewModel)
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, isDarkTheme: Boolean) {
    val inputBackgroundColor = if (isDarkTheme) InputDark else InputLight
    val textColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val placeholderColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Buscar", color = placeholderColor) },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Icono de búsqueda",
                tint = placeholderColor
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = inputBackgroundColor,
            unfocusedContainerColor = inputBackgroundColor,
            disabledContainerColor = inputBackgroundColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = textColor,
            cursorColor = textColor
        ),
        singleLine = true
    )
}

@Composable
fun ContactRow(
    user: User,
    isDarkTheme: Boolean,
    navController: NavController,
    viewModel: ContactsViewModel
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val mutedTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {

                viewModel.findOrCreateChat(user.uid) { chatId ->

                    navController.navigate("chat/$chatId")
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.photoUrl ?: "https://i.pravatar.cc/150?u=${user.uid}",
            contentDescription = "Avatar de ${user.displayName}",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = user.displayName, fontWeight = FontWeight.Bold, color = textColor)
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = mutedTextColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContactsScreenPreview() {
    // Para la preview, no podemos pasar el showToast, así que creamos una función vacía
    ContactsScreen(navController = rememberNavController(), showToast = {})
}