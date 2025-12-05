package mx.castillo.edwin.mensajeria.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import mx.castillo.edwin.mensajeria.ui.components.Primary
import mx.castillo.edwin.mensajeria.ui.viewmodel.SignUpViewModel

@Composable
fun VerifyEmailScreen(
    navController: NavController,
    showToast: (String) -> Unit,
    viewModel: SignUpViewModel = viewModel()
) {
    val isEmailVerified by produceState(initialValue = false) {
        while (true) {
            FirebaseAuth.getInstance().currentUser?.reload()
            value = FirebaseAuth.getInstance().currentUser?.isEmailVerified ?: false
            if (value) break
            delay(3000)
        }
    }

    val toastMessage by viewModel.toastMessage.collectAsState()
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            showToast(it)
            viewModel.onToastShown()
        }
    }

    LaunchedEffect(isEmailVerified) {
        if (isEmailVerified) {
            showToast("¡Correo verificado con éxito! Bienvenido/a.")
            navController.navigate("conversations") {
                popUpTo(0)
            }
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.1f))
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MarkEmailUnread,
                    contentDescription = "Email",
                    tint = Primary,
                    modifier = Modifier.size(52.dp)
                )
            }
            Spacer(Modifier.height(24.dp))

            Text("Revisa tu correo", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Hemos enviado un enlace de verificación a tu correo. " +
                        "Por favor, haz clic en el enlace para continuar.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))

            // --- BOTÓN CORREGIDO ---
            Button(
                onClick = { viewModel.resendVerificationEmail() },
                modifier = Modifier.fillMaxWidth(),
                // Se añaden los colores correctos
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Reenviar correo")
            }
            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Regresar")
            }
        }
    }
}