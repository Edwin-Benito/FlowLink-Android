package mx.castillo.edwin.mensajeria.ui.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import mx.castillo.edwin.mensajeria.ui.viewmodel.SignUpUiState
import mx.castillo.edwin.mensajeria.ui.viewmodel.SignUpViewModel
import mx.castillo.edwin.mensajeria.ui.components.*



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    navController: NavController,
    isDarkTheme: Boolean = false,
    viewModel: SignUpViewModel = viewModel(),
    showToast: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is SignUpUiState.Loading

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SignUpUiState.Success -> {
                showToast("¡Registro exitoso! Revisa tu correo para verificar la cuenta.")
                // --- CAMBIA ESTA NAVEGACIÓN ---
                navController.navigate("verify_email") {
                    popUpTo("login") // Vuelve hasta la pantalla de login en el historial
                }
            }
            is SignUpUiState.Error -> {
                showToast(state.message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    val backgroundColor = if (isDarkTheme) BackgroundDark else BackgroundLight
    val textColor = if (isDarkTheme) TextDark else TextLight
    val mutedTextColor = if (isDarkTheme) TextMutedDark else TextMutedLight
    val inputBackgroundColor = if (isDarkTheme) InputDark else InputLight

    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") } // <-- NUEVO CAMPO
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sign up",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textColor
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                "Create an account",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Start your journey with us",
                fontSize = 16.sp,
                color = mutedTextColor,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            AuthInput(
                value = displayName,
                onValueChange = { displayName = it },
                placeholder = "Nombre completo",
                placeholderColor = mutedTextColor,
                backgroundColor = inputBackgroundColor,
                textColor = textColor,
                enabled = !isLoading
            )
            Spacer(Modifier.height(16.dp))
            // Campo para el Nombre de Usuario
            AuthInput(
                value = username,
                onValueChange = { username = it.lowercase().filter { char -> char.isLetterOrDigit() || char == '_' } }, // Solo permite minúsculas, números y _
                placeholder = "Nombre de usuario (ej. juan_perez)",
                placeholderColor = mutedTextColor,
                backgroundColor = inputBackgroundColor,
                textColor = textColor,
                enabled = !isLoading
            )
            Spacer(Modifier.height(16.dp))


            EmailInput(
                value = email,
                onValueChange = { email = it },
                placeholderColor = mutedTextColor,
                backgroundColor = inputBackgroundColor,
                textColor = textColor,
                enabled = !isLoading
            )
            Spacer(Modifier.height(16.dp))

            PasswordInput(
                value = password,
                onValueChange = { password = it },
                placeholderColor = mutedTextColor,
                backgroundColor = inputBackgroundColor,
                textColor = textColor,
                enabled = !isLoading
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.signUpWithEmail(displayName, username, email, password) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White, // <-- CORRECCIÓN: Usa Color de Compose
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(24.dp))

            SignInNavigation(mutedTextColor, navController, !isLoading)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    placeholderColor: Color,
    backgroundColor: Color,
    textColor: Color,
    enabled: Boolean
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        placeholder = { Text(placeholder, color = placeholderColor) },
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = backgroundColor,
            unfocusedContainerColor = backgroundColor,
            disabledContainerColor = backgroundColor,
            focusedIndicatorColor = Primary,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            cursorColor = Primary
        ),
        singleLine = true
    )
}

@Composable
private fun SignInNavigation(
    mutedTextColor: Color,
    navController: NavController,
    isEnabled: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Already have an account?", fontSize = 14.sp, color = mutedTextColor)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Sign in",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isEnabled) Primary else mutedTextColor,
            modifier = Modifier.clickable(enabled = isEnabled) { navController.popBackStack() }
        )
    }
}
