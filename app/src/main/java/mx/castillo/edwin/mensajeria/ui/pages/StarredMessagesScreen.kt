package mx.castillo.edwin.mensajeria.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // <-- Asegúrate de tener este import
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue // <-- CORRECCIÓN 1: Import añadido
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import mx.castillo.edwin.mensajeria.data.Message // <-- Asegúrate de importar tu clase Message
import mx.castillo.edwin.mensajeria.ui.viewmodel.StarredMessagesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredMessagesScreen(
    navController: NavController,
    chatId: String?,
    isDarkTheme: Boolean = false,
    viewModel: StarredMessagesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState() // Funciona gracias al import 'getValue'
    val context = LocalContext.current

    LaunchedEffect(chatId) {
        viewModel.loadStarredMessages(chatId, context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mensajes Destacados") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                }
                // Puedes añadir colores si quieres: colors = TopAppBarDefaults.topAppBarColors(...)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()
                uiState.error != null -> Text(uiState.error!!)
                uiState.starredMessages.isEmpty() -> Text("No hay mensajes destacados en este chat.")
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        // CORRECCIÓN 2: Especificar tipo y añadir key
                        items(items = uiState.starredMessages, key = { it.id }) { message: Message ->
                            // Ahora message.text debería resolverse correctamente
                            Text(message.text, modifier = Modifier.padding(16.dp))
                            Divider()
                        }
                    }
                }
            }
        }
    }
}