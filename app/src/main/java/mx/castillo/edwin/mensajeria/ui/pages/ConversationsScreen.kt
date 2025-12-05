package mx.castillo.edwin.mensajeria.ui.pages

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import mx.castillo.edwin.mensajeria.ui.components.AppBottomNavigation
import mx.castillo.edwin.mensajeria.ui.components.LanguageSelectionDialog
import mx.castillo.edwin.mensajeria.ui.components.Primary
import mx.castillo.edwin.mensajeria.ui.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    navController: NavController,
    isDarkTheme: Boolean = false,
    languageViewModel: LanguageViewModel = viewModel(),
    conversationsViewModel: ConversationsViewModel = viewModel() // <-- Inyectamos el nuevo ViewModel
) {
    // La lógica del diálogo de idioma se mantiene igual
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val showLanguageDialog = remember { mutableStateOf(!prefs.getBoolean("language_dialog_shown", false)) }



    if (showLanguageDialog.value) {
        LanguageSelectionDialog(
            onLanguageSelected = { langCode ->
                languageViewModel.saveLanguagePreference(langCode, prefs)
                showLanguageDialog.value = false
            },
            onDismiss = {
                languageViewModel.saveLanguagePreference("es", prefs)
                showLanguageDialog.value = false
            }
        )
    }

    LaunchedEffect(Unit) {
        conversationsViewModel.fetchConversations(context)
    }

    // Obtenemos el estado de la UI desde el nuevo ViewModel
    val uiState by conversationsViewModel.uiState.collectAsState()
    val backgroundColor = if (isDarkTheme) Color(0xFF101C22) else Color(0xFFF5F7F8)
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val mutedTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Chats", fontWeight = FontWeight.Bold, color = textColor) },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Nuevo mensaje", tint = mutedTextColor)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = backgroundColor.copy(alpha = 0.8f))
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("contacts") }) {
                Icon(Icons.Default.Add, contentDescription = "Iniciar nuevo chat")
            }
        },
        bottomBar = { AppBottomNavigation(navController, "conversations") },
        containerColor = backgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            // Reaccionamos a los diferentes estados de la UI
            when (val state = uiState) {
                is ConversationsUiState.Loading -> CircularProgressIndicator()
                is ConversationsUiState.Error -> Text(text = state.message, color = textColor)
                is ConversationsUiState.Success -> {
                    if (state.chats.isEmpty()) {
                        Text("No tienes conversaciones.\nPulsa '+' para iniciar un chat.", color = textColor, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(state.chats) { chatInfo ->
                                // Usamos el nuevo ChatItem que recibe ChatInfo
                                RealChatItem(
                                    chatInfo = chatInfo,
                                    isDarkTheme = isDarkTheme,
                                    navController = navController
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// He renombrado el Composable a 'RealChatItem' para diferenciarlo del de ejemplo
@Composable
fun RealChatItem(chatInfo: ChatInfo, isDarkTheme: Boolean, navController: NavController) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val mutedTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    val simpleDateFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = chatInfo.chat.lastMessageTimestamp?.let { simpleDateFormat.format(it) } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("chat/${chatInfo.chatId}") }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = chatInfo.otherUser?.photoUrl ?: "https://i.pravatar.cc/150",
            contentDescription = "Avatar de ${chatInfo.otherUser?.displayName}",
            modifier = Modifier.size(56.dp).clip(CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = chatInfo.otherUser?.displayName ?: "Usuario Desconocido",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textColor
                )
                Text(text = formattedTime, fontSize = 12.sp, color = mutedTextColor)
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- NUEVO: Icono de estado del último mensaje ---
                if (chatInfo.lastMessageStatus.isNotEmpty()) {
                    val icon = if (chatInfo.lastMessageStatus == "leido") Icons.Default.DoneAll else Icons.Default.Check
                    val tint = if (chatInfo.lastMessageStatus == "leido") Primary else mutedTextColor
                    Icon(
                        imageVector = icon,
                        contentDescription = "Estado del mensaje",
                        modifier = Modifier.size(16.dp),
                        tint = tint
                    )
                    Spacer(Modifier.width(4.dp))
                }

                // --- NUEVO: Muestra "Escribiendo..." o el último mensaje ---
                Text(
                    text = if (chatInfo.isOtherUserTyping) "Escribiendo..." else chatInfo.displayLastMessage,
                    fontSize = 14.sp,
                    color = if (chatInfo.isOtherUserTyping) Primary else mutedTextColor,
                    fontWeight = if (chatInfo.isOtherUserTyping) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // --- NUEVO: Contador de mensajes no leídos ---
                if (chatInfo.unreadMessageCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chatInfo.unreadMessageCount.toString(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun ConversationsScreenPreview() {
    ConversationsScreen(navController = rememberNavController())
}