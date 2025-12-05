package mx.castillo.edwin.mensajeria.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import mx.castillo.edwin.mensajeria.ui.components.Primary
import mx.castillo.edwin.mensajeria.ui.viewmodel.ContactInfoViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactInfoScreen(
    navController: NavController,
    userId: String?,
    chatId: String?,
    isDarkTheme: Boolean = false, // Pasa el estado del tema
    viewModel: ContactInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDurationDialog by remember { mutableStateOf(false) } // <-- ESTADO PARA EL DIÁLOGO

    LaunchedEffect(userId, chatId) { // <-- OBSERVA AMBOS
        if (userId != null && chatId != null) { // <-- COMPRUEBA AMBOS
            viewModel.loadContactInfo(userId, chatId) // <-- PASA AMBOS
        }
    }

    // --- Definición de Colores ---
    val backgroundColor = if (isDarkTheme) Color(0xFF101C22) else Color(0xFFF5F7F8)
    val cardBackgroundColor = if (isDarkTheme) Color(0xFF1A2A33) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val mutedTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
    val errorColor = MaterialTheme.colorScheme.error
    val iconBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f) // Color fondo iconos settings

    // --- DIÁLOGO PARA SELECCIONAR DURACIÓN ---
    if (showDurationDialog) {
        DisappearingMessagesDialog(
            currentDuration = uiState.disappearingMessagesDuration,
            onDismiss = { showDurationDialog = false },
            onDurationSelected = { duration ->
                viewModel.updateDisappearingMessagesDuration(duration)
                showDurationDialog = false
            },
            isDarkTheme = isDarkTheme // Pasamos el tema
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.user?.displayName ?: "", color = textColor) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = mutedTextColor)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Mostrar menú */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Más opciones", tint = mutedTextColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text(uiState.error!!, color = errorColor, modifier = Modifier.padding(horizontal = 32.dp))
                }
            }
            uiState.user != null -> {
                val user = uiState.user!! // Sabemos que no es nulo aquí

                // El cálculo de chatId ya no es necesario aquí
                // val chatId = ...

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp) // Padding horizontal para todo el contenido
                        .verticalScroll(rememberScrollState()), // Permite el scroll vertical
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // --- Sección de Perfil ---
                    AsyncImage(
                        model = user.photoUrl ?: "https://i.pravatar.cc/300", // Placeholder
                        contentDescription = "Foto de perfil de ${user.displayName}",
                        modifier = Modifier
                            .size(128.dp)
                            .clip(CircleShape)
                            .border(4.dp, if(isDarkTheme) Color(0xFF334155) else Color.White, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(user.displayName ?: "Nombre Desconocido", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text(
                        text = "@${user.username}",
                        fontSize = 16.sp,
                        color = mutedTextColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- Sección Biografía ---
                    InfoCard(backgroundColor = cardBackgroundColor) {
                        InfoItem(
                            title = "Biografía",
                            titleColor = Primary,
                            content = user.bio.ifBlank { "Sin biografía" },
                            contentColor = textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Botones de Acción ---
                    ActionButtonsRow(mutedTextColor = mutedTextColor)

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- Sección de Notificaciones y MÁS ---
                    SettingsCard(backgroundColor = cardBackgroundColor) {
                        NotificationSettingsItem(
                            icon = Icons.Default.Notifications,
                            text = "Silenciar notificaciones",
                            secondaryText = if (uiState.isMuted) "Sí" else "No",
                            showSwitch = true,
                            isChecked = uiState.isMuted,
                            onCheckedChange = { newState ->
                                viewModel.toggleMuteNotifications(user.uid, newState)
                            },
                            iconBackgroundColor = iconBackgroundColor,
                            iconColor = mutedTextColor,
                            textColor = textColor,
                            secondaryTextColor = mutedTextColor
                        )
                        Divider(color = mutedTextColor.copy(alpha = 0.2f), modifier = Modifier.padding(start = 76.dp)) // Ajusta indentación
                        SettingsItem(
                            icon = Icons.Default.MusicNote,
                            text = "Notificaciones personalizadas",
                            onClick = { /* TODO: Navegar a pantalla de notificaciones */ },
                            iconBackgroundColor = iconBackgroundColor,
                            iconColor = mutedTextColor,
                            textColor = textColor,
                            arrowColor = mutedTextColor
                        )

                        // --- BLOQUE COMENTADO PRESERVADO (COMO LO PEDISTE) ---
                        /*
                        Divider(color = mutedTextColor.copy(alpha = 0.2f), modifier = Modifier.padding(start = 76.dp))
                        SettingsItem(
                            icon = Icons.Default.PermMedia,
                            text = "Visibilidad de archivos multimedia",
                            onClick = { /* TODO: Navegar a pantalla de visibilidad */ },
                            iconBackgroundColor = iconBackgroundColor,
                            iconColor = mutedTextColor,
                            textColor = textColor,
                            arrowColor = mutedTextColor
                        )
                        */
                        // --- FIN BLOQUE COMENTADO ---

                        Divider(color = mutedTextColor.copy(alpha = 0.2f), modifier = Modifier.padding(start = 76.dp))

                        // --- Item Mensajes Temporales (Sin cambios) ---
                        SettingsItemWithDetail(
                            icon = Icons.Default.Timer, // Icono de temporizador
                            text = "Mensajes temporales",
                            detail = formatDuration(uiState.disappearingMessagesDuration), // Muestra duración actual
                            onClick = { showDurationDialog = true }, // Abre el diálogo
                            iconBackgroundColor = iconBackgroundColor,
                            iconColor = mutedTextColor,
                            textColor = textColor,
                            detailColor = mutedTextColor,
                            arrowColor = mutedTextColor
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- BLOQUE COMENTADO PRESERVADO (CON 'DESTACADOS' ELIMINADO) ---
                    /*SettingsCard(backgroundColor = cardBackgroundColor) {
                        SettingsItemWithDetail(
                            icon = Icons.Default.FolderOpen,
                            text = "Archivos, enlaces y docs",
                            detail = "0 archivos", // TODO: Cargar esto desde el ViewModel si lo implementas
                            onClick = { /* TODO: Navegar a pantalla de archivos */ },
                            iconBackgroundColor = iconBackgroundColor,
                            iconColor = mutedTextColor,
                            textColor = textColor,
                            detailColor = mutedTextColor,
                            arrowColor = mutedTextColor
                        )
                        // --- "Mensajes Destacados" ha sido eliminado de este bloque ---
                    }*/
                    // --- FIN BLOQUE COMENTADO ---

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Sección de Acciones Destructivas ---
                    SettingsCard(backgroundColor = cardBackgroundColor) {
                        DestructiveSettingsItem(
                            icon = Icons.Default.Block,
                            text = "Bloquear a ${user.displayName}",
                            onClick = { viewModel.blockUser(user.uid) }, // Llama al VM
                            errorColor = errorColor
                        )
                        Divider(color = mutedTextColor.copy(alpha = 0.2f), modifier = Modifier.padding(start = 76.dp))
                        DestructiveSettingsItem(
                            icon = Icons.Default.ThumbDown,
                            text = "Reportar a ${user.displayName}",
                            onClick = { viewModel.reportUser(user.uid) }, // Llama al VM
                            errorColor = errorColor
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp)) // Espacio al final
                }
            }
        }
    }
}


// --- NUEVO COMPOSABLE: DIÁLOGO DE SELECCIÓN ---
@Composable
fun DisappearingMessagesDialog(
    currentDuration: Long,
    onDismiss: () -> Unit,
    onDurationSelected: (Long) -> Unit,
    isDarkTheme: Boolean // Para aplicar colores del tema
) {
    val options = mapOf(
        "Desactivado" to 0L,
        "5 segundos" to 5L,
        "1 minuto" to 60L,
        "1 hora" to 3600L,
        "1 día" to 86400L,
        "7 días" to 604800L
    )
    var selectedOption by remember { mutableStateOf(currentDuration) }

    // Colores basados en el tema
    val dialogBackgroundColor = if (isDarkTheme) Color(0xFF1A2A33) else Color.White
    val dialogTextColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val radioSelectedColor = Primary
    val radioUnselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mensajes temporales") },
        text = {
            Column {
                Text("Cuando están activados, los mensajes nuevos desaparecerán de este chat después del tiempo seleccionado.", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                // Genera las opciones con RadioButtons
                options.forEach { (label, duration) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (duration == selectedOption),
                                onClick = { selectedOption = duration }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (duration == selectedOption),
                            onClick = { selectedOption = duration },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = radioSelectedColor,
                                unselectedColor = radioUnselectedColor
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDurationSelected(selectedOption) },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Primary)
            }
        },
        containerColor = dialogBackgroundColor, // Aplica color de fondo
        titleContentColor = dialogTextColor,   // Aplica color de texto
        textContentColor = dialogTextColor.copy(alpha = 0.8f) // Aplica color de texto secundario
    )
}

// --- NUEVA FUNCIÓN HELPER ---
// Convierte segundos a un texto legible
fun formatDuration(seconds: Long): String {
    return when (seconds) {
        0L -> "Desactivado"
        5L -> "5 segundos"
        60L -> "1 minuto"
        3600L -> "1 hora"
        86400L -> "1 día"
        604800L -> "7 días"
        else -> "Personalizado" // O maneja otros casos si los permites
    }
}


// --- Componentes Auxiliares (Asegúrate de tenerlos definidos) ---
// (No los repito aquí, pero deben estar en tu archivo como antes)

@Composable
fun InfoCard(
    backgroundColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            content()
        }
    }
}

@Composable
fun InfoItem(
    title: String,
    titleColor: Color,
    content: String,
    contentColor: Color
) {
    Text(title, color = titleColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(4.dp))
    Text(content, color = contentColor, fontSize = 16.sp)
}

@Composable
fun ActionButtonsRow(mutedTextColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        ActionButton(icon = Icons.Default.Search, text = "Buscar", onClick = { /*TODO*/ }, textColor = mutedTextColor)
        //ActionButton(icon = Icons.Default.Call, text = "Llamada", onClick = { /*TODO*/ }, textColor = mutedTextColor)
        //ActionButton(icon = Icons.Default.Videocam, text = "Video", onClick = { /*TODO*/ }, textColor = mutedTextColor)
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    textColor: Color
) {
    val buttonColor = Primary.copy(alpha = 0.15f)
    val iconColor = Primary

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(buttonColor)
        ) {
            Icon(icon, contentDescription = text, tint = iconColor, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text, fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SettingsCard(
    backgroundColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    iconBackgroundColor: Color,
    iconColor: Color,
    textColor: Color,
    arrowColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBackgroundColor)
                .padding(12.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, modifier = Modifier.weight(1f), color = textColor)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = arrowColor)
    }
}

@Composable
fun SettingsItemWithDetail(
    icon: ImageVector,
    text: String,
    detail: String,
    onClick: () -> Unit,
    iconBackgroundColor: Color,
    iconColor: Color,
    textColor: Color,
    detailColor: Color,
    arrowColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBackgroundColor)
                .padding(12.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text, fontSize = 16.sp, color = textColor)
            Text(detail, fontSize = 14.sp, color = detailColor)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = arrowColor)
    }
}

@Composable
fun NotificationSettingsItem(
    icon: ImageVector,
    text: String,
    secondaryText: String,
    showSwitch: Boolean,
    isChecked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
    iconBackgroundColor: Color,
    iconColor: Color,
    textColor: Color,
    secondaryTextColor: Color
) {
    var checkedState by remember { mutableStateOf(isChecked) }

    // Actualiza el estado local si el valor del ViewModel cambia (para reflejar cargas iniciales)
    LaunchedEffect(isChecked) {
        checkedState = isChecked
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBackgroundColor)
                    .padding(12.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text, fontSize = 16.sp, color = textColor)
                Text(secondaryText, fontSize = 14.sp, color = secondaryTextColor)
            }
        }
        if (showSwitch) {
            Switch(
                checked = checkedState,
                onCheckedChange = {
                    checkedState = it
                    onCheckedChange(it) // Llama a la lambda del ViewModel
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White, // Color del círculo cuando está activo
                    checkedTrackColor = Primary, // Color del fondo cuando está activo
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = Color.Transparent // Sin borde cuando está inactivo
                )
            )
        }
    }
}

@Composable
fun DestructiveSettingsItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    errorColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = errorColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(32.dp)) // Aumenta un poco el espacio para alinear mejor
        Text(text, color = errorColor, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
    }
}