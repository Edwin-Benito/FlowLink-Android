package mx.castillo.edwin.mensajeria.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import mx.castillo.edwin.mensajeria.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController, isDarkTheme: Boolean) {
    val backgroundColor = if (isDarkTheme) Color(0xFF101C22) else Color(0xFFF5F7F8)
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val mutedTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acerca de", color = textColor) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = textColor)
                    }
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Usamos el logo de la app
            Image(
                painter = painterResource(id = R.drawable.logo_app1),
                contentDescription = "Logo de FlowLink",
                modifier = Modifier.size(300.dp)
            )

            Text("La forma perfecta de conectar.", fontSize = 18.sp, color = mutedTextColor)

            Spacer(Modifier.height(48.dp))

            // Tarjetas de información
            InfoCard(title = "Compañia de Desarollo ", content = "SafeSight", isDarkTheme = isDarkTheme)
            Spacer(Modifier.height(16.dp))
            InfoCard(title = "Desarrollado por", content = "Edwin Benito Castillo Hernández", isDarkTheme = isDarkTheme)
            Spacer(Modifier.height(16.dp))
            InfoCard(title = "Versión", content = "1.0.0 (Alpha)", isDarkTheme = isDarkTheme)
        }

        // Footer con texto de derechos de autor
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
            Text("© 2025 FlowLink. Todos los derechos reservados.", fontSize = 12.sp, color = mutedTextColor)
        }
    }
}

@Composable
private fun InfoCard(title: String, content: String, isDarkTheme: Boolean) {
    val cardBackgroundColor = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.White
    val titleColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
    val contentColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBackgroundColor)
            .padding(16.dp)
    ) {
        Text(text = title, fontSize = 14.sp, color = titleColor)
        Text(text = content, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
    }
}