package mx.castillo.edwin.mensajeria.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import mx.castillo.edwin.mensajeria.ui.components.AuthInput
import mx.castillo.edwin.mensajeria.ui.viewmodel.CreateUsernameViewModel

@Composable
fun CreateUsernameScreen(
    navController: NavController,
    viewModel: CreateUsernameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var username by remember { mutableStateOf("") }

    // Navega a la pantalla principal cuando el username se guarda con éxito
    LaunchedEffect(uiState) {
        if (uiState is CreateUsernameViewModel.UiState.Success) {
            navController.navigate("conversations") {
                // Limpia todo el historial para que no pueda volver aquí
                popUpTo(0)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("¡Casi listo!", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(
            "Elige un nombre de usuario único para tu cuenta.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        AuthInput(
            value = username,
            onValueChange = { username = it.lowercase().filter { char -> char.isLetterOrDigit() || char == '_' } },
            placeholder = "@nombredeusuario",
            placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            textColor = MaterialTheme.colorScheme.onSurface,
            enabled = uiState !is CreateUsernameViewModel.UiState.Loading
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.saveUsername(username) },
            enabled = uiState !is CreateUsernameViewModel.UiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState is CreateUsernameViewModel.UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("Continuar")
            }
        }

        if (uiState is CreateUsernameViewModel.UiState.Error) {
            Text(
                text = (uiState as CreateUsernameViewModel.UiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}