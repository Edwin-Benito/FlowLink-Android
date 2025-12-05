package mx.castillo.edwin.mensajeria.ui.pages

import android.content.Context
import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
// import kotlinx.coroutines.delay // Ya no se necesita aquí
import mx.castillo.edwin.mensajeria.R
import mx.castillo.edwin.mensajeria.ui.viewmodel.SplashDestination
import mx.castillo.edwin.mensajeria.ui.viewmodel.SplashViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = viewModel()
) {
    // 1. Observa el destino (destination)
    val destination by viewModel.destination.collectAsState()
    val context = LocalContext.current

    // 2. Llama a initializeApp UNA SOLA VEZ
    // Le ordena al ViewModel que empiece a trabajar.
    LaunchedEffect(key1 = true) {
        viewModel.initializeApp(context)
    }

    // 3. Reacciona a los cambios de destino
    // Se ejecuta CADA VEZ que 'destination' cambia.
    LaunchedEffect(destination) {
        when (destination) {
            is SplashDestination.Home -> {
                // El ViewModel ya revisó auth Y sincronizó la clave.
                // Ahora solo navegamos.
                navController.navigate("conversations") {
                    popUpTo("splash") { inclusive = true }
                }
            }
            is SplashDestination.Login -> {
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            }
            // Mientras está en Loading, no hacemos nada.
            is SplashDestination.Loading -> {}
        }
    }

    // Tu UI está perfecta.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_app1),
                contentDescription = "Logo",
                modifier = Modifier.size(400.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(color = Color.Black)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(navController = rememberNavController())
}