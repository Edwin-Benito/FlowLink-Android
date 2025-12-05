package mx.castillo.edwin.mensajeria.ui.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Timer
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    navController: NavController,
    isDarkTheme: Boolean = false,
    viewModel: mx.castillo.edwin.mensajeria.ui.viewmodel.PrivacyViewModel = viewModel()
) {
    val user by viewModel.user.collectAsState()
    var showLastSeenDialog by remember { mutableStateOf(false) } // <-- Añade este estado

    if (showLastSeenDialog) {
        LastSeenDialog(
            currentSelection = user?.lastSeenPrivacy ?: "Todos",
            onDismiss = { showLastSeenDialog = false },
            onSelect = { newSetting ->
                viewModel.updateLastSeenPrivacy(newSetting)
                showLastSeenDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacidad", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Controla quién ve tu información y actividad.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            SettingSwitchRow(
                icon = Icons.Default.Visibility,
                title = "Confirmaciones de lectura",
                description = "Si desactivas esta opción, no podrás ver las confirmaciones de lectura de otras personas.",
                checked = user?.readReceiptsEnabled ?: true,
                onCheckedChange = { isEnabled ->
                    viewModel.updateReadReceipts(isEnabled)
                }
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // --- AÑADE ESTA NUEVA FILA DE AJUSTES ---
            SettingOptionsRow(
                icon = Icons.Default.Timer,
                title = "Últ. vez y En línea",
                currentValue = user?.lastSeenPrivacy ?: "Todos",
                onClick = { showLastSeenDialog = true }
            )
        }
    }
}

@Composable
fun SettingOptionsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    currentValue: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = currentValue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
}

// --- AÑADE ESTE NUEVO DIÁLOGO ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastSeenDialog(
    currentSelection: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val options = listOf("Todos", "Nadie")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Quién puede ver tu últ. vez y estado en línea?") },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option == currentSelection),
                            onClick = { onSelect(option) }
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(text = option)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun SettingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}