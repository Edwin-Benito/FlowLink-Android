package mx.castillo.edwin.mensajeria.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import mx.castillo.edwin.mensajeria.data.User
import mx.castillo.edwin.mensajeria.ui.components.AppBottomNavigation
import mx.castillo.edwin.mensajeria.ui.components.CustomToast
import mx.castillo.edwin.mensajeria.ui.components.Primary
import mx.castillo.edwin.mensajeria.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    isDarkTheme: Boolean = false,
    viewModel: ProfileViewModel = viewModel()
) {
    val user by viewModel.user.collectAsState()
    val context = LocalContext.current

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditBioDialog by remember { mutableStateOf(false) } // <-- NUEVO ESTADO PARA EL DIÁLOGO DE BIO
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    val viewModelToast by viewModel.toastMessage.collectAsState()

    LaunchedEffect(viewModelToast) {
        viewModelToast?.let {
            toastMessage = it
            viewModel.onToastShown()
        }
    }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3000)
            toastMessage = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initGoogleSignInClient(context)
    }


    val backgroundColor = if (isDarkTheme) Color(0xFF101C22) else Color(0xFFF5F7F8)
    val cardBackgroundColor = if (isDarkTheme) Color(0xFF182832) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val mutedTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    if (showEditNameDialog) {
        EditNameDialog(
            currentName = user?.displayName ?: "",
            onDismiss = { showEditNameDialog = false },
            onSave = { newName ->
                viewModel.updateDisplayName(newName)
                showEditNameDialog = false
            },
            isDarkTheme = isDarkTheme
        )
    }

    // --- NUEVO: LLAMADA AL DIÁLOGO PARA EDITAR LA BIOGRAFÍA ---
    if (showEditBioDialog) {
        EditBioDialog(
            currentBio = user?.bio ?: "",
            onDismiss = { showEditBioDialog = false },
            onSave = { newBio ->
                viewModel.updateBio(newBio)
                showEditBioDialog = false
            },
            isDarkTheme = isDarkTheme
        )
    }

    if (showPasswordDialog) {
        PasswordResetDialog(
            userEmail = user?.email ?: "",
            onDismiss = { showPasswordDialog = false },
            onConfirm = {
                viewModel.sendPasswordReset()
                showPasswordDialog = false
            },
            isDarkTheme = isDarkTheme
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Perfil", fontWeight = FontWeight.Bold, color = textColor) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = mutedTextColor)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            AppBottomNavigation(navController = navController, currentRoute = "profile")
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfileHeader(user = user, isDarkTheme = isDarkTheme)
                Spacer(modifier = Modifier.height(32.dp))
                InfoSection(
                    user = user,
                    cardBackgroundColor = cardBackgroundColor,
                    textColor = textColor,
                    mutedTextColor = mutedTextColor,
                    onEditNameClick = { showEditNameDialog = true },
                    onEditBioClick = { showEditBioDialog = true } // <-- NUEVA LLAMADA
                )
                Spacer(modifier = Modifier.height(32.dp))
                SecuritySection(cardBackgroundColor = cardBackgroundColor, textColor = textColor, navController = navController, onPasswordClick = { showPasswordDialog = true})
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        viewModel.signOut {
                            navController.navigate("login") { popUpTo(0) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red)
                ) {
                    Text("Cerrar Sesión")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            CustomToast(
                message = toastMessage,
                modifier = Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(bottom = 80.dp)
            )
        }
    }
}

// --- NUEVO DIÁLOGO PARA LA BIOGRAFÍA ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBioDialog(
    currentBio: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    isDarkTheme: Boolean
) {
    var text by remember { mutableStateOf(currentBio) }
    val cardBackgroundColor = if (isDarkTheme) Color(0xFF182832) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar biografía") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Sobre ti...") },
                modifier = Modifier.fillMaxWidth(), // Más alto para la bio
                singleLine = false, // Permite múltiples líneas
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = cardBackgroundColor,
                    unfocusedContainerColor = cardBackgroundColor,
                    disabledContainerColor = cardBackgroundColor,
                    focusedIndicatorColor = Primary,
                    unfocusedIndicatorColor = textColor.copy(alpha = 0.4f),
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = Primary,
                    focusedLabelColor = textColor,
                    unfocusedLabelColor = textColor.copy(alpha = 0.6f)
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(text) },
                enabled = text != currentBio, // Guardar si el texto es diferente
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Primary)
            }
        },
        containerColor = cardBackgroundColor,
        titleContentColor = textColor,
        textContentColor = textColor.copy(alpha = 0.8f)
    )
}

@Composable
fun InfoSection(
    user: User?,
    cardBackgroundColor: Color,
    textColor: Color,
    mutedTextColor: Color,
    onEditNameClick: () -> Unit,
    onEditBioClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "INFORMACIÓN",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = mutedTextColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(cardBackgroundColor)
        ) {
            InfoRow("Nombre", user?.displayName ?: "...", textColor, mutedTextColor, onEditClick = onEditNameClick)
            Divider(color = textColor.copy(alpha = 0.1f))
            InfoRow("Biografía", user?.bio?.ifBlank { "Añade una biografía" } ?: "...", textColor, mutedTextColor, onEditClick = onEditBioClick)
            Divider(color = textColor.copy(alpha = 0.1f))
            InfoRow("Correo", user?.email ?: "...", textColor, mutedTextColor) { /* No se puede editar */ }
        }
    }
}

// --- NUEVO DIÁLOGO CON ESTILOS PERSONALIZADOS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    isDarkTheme: Boolean
) {
    var text by remember { mutableStateOf(currentName) }

    // Obtenemos los colores de nuestro tema
    val cardBackgroundColor = if (isDarkTheme) Color(0xFF182832) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar nombre") },
        text = {
            TextField( // Cambiamos a TextField para más personalización de color
                value = text,
                onValueChange = { text = it },
                label = { Text("Nuevo nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = cardBackgroundColor,
                    unfocusedContainerColor = cardBackgroundColor,
                    disabledContainerColor = cardBackgroundColor,
                    focusedIndicatorColor = Primary,
                    unfocusedIndicatorColor = textColor.copy(alpha = 0.4f),
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = Primary,
                    focusedLabelColor = textColor,
                    unfocusedLabelColor = textColor.copy(alpha = 0.6f)
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(text) },
                enabled = text.isNotBlank() && text != currentName,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Primary)
            }
        },
        // Aplicamos los colores al contenedor del diálogo
        containerColor = cardBackgroundColor,
        titleContentColor = textColor,
        textContentColor = textColor.copy(alpha = 0.8f)
    )
}

// --- El resto de tus Composables no necesita cambios ---

@Composable
fun ProfileHeader(user: User?, isDarkTheme: Boolean) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val mutedTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            AsyncImage(
                model = user?.photoUrl ?: "https://i.pravatar.cc/300",
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
            )
            IconButton(
                onClick = { /* TODO: Lógica para cambiar foto */ },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Primary)
                    .size(32.dp)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Cambiar foto", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Text(text = user?.displayName ?: "Cargando...", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = textColor)
        Text(text = "@${user?.username ?: "..."}", fontSize = 16.sp, color = mutedTextColor)
    }
}


@Composable
fun InfoRow(label: String, value: String, textColor: Color, mutedTextColor: Color, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = mutedTextColor)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, color = textColor)
        }
        IconButton(onClick = onEditClick) {
            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = mutedTextColor)
        }
    }
}

@Composable
fun SecuritySection(
    cardBackgroundColor: Color,
    textColor: Color,
    navController: NavController,
    onPasswordClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "SEGURIDAD",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = textColor.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(cardBackgroundColor)
        ) {
            SettingsButton(
                text = "Cambiar contraseña",
                textColor = textColor,
                onClick = onPasswordClick // <-- MODIFICAR ESTO
            )
            Divider(color = textColor.copy(alpha = 0.1f))

            SettingsButton(
                text = "Configuración de privacidad",
                textColor = textColor,
                onClick = { navController.navigate("privacy") } // <-- AÑADE ESTO
            )

            Divider(color = textColor.copy(alpha = 0.1f))

            SettingsButton(
                text = "Acerca de FlowLink",
                textColor = textColor,
                onClick = { navController.navigate("about") }
            )
        }
    }
}


@Composable
fun PasswordResetDialog(
    userEmail: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isDarkTheme: Boolean
) {
    val cardBackgroundColor = if (isDarkTheme) Color(0xFF182832) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Contraseña") },
        text = { Text("Se enviará un enlace para cambiar tu contraseña a:\n\n$userEmail") },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                Text("Enviar Correo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Primary)
            }
        },
        containerColor = cardBackgroundColor,
        titleContentColor = textColor,
        textContentColor = textColor.copy(alpha = 0.8f)
    )
}

@Composable
fun SettingsButton(text: String, textColor: Color, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = text, color = textColor)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = textColor.copy(alpha = 0.4f))
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(rememberNavController())
}