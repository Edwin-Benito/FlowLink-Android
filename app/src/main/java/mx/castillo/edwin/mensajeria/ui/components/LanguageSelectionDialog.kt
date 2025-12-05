package mx.castillo.edwin.mensajeria.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mx.castillo.edwin.mensajeria.data.supportedLanguages // <-- Importa la nueva lista

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionDialog(
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit // El onDismiss original de tu ConversationsScreen
) {
    Dialog(
        onDismissRequest = onDismiss, // Llama a onDismiss si el usuario toca fuera
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                // Limita la altura al 70% de la pantalla para que no sea gigante
                .fillMaxHeight(0.7f)
        ) {
            Column {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Elige tu idioma preferido",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Selecciona el idioma en el que quieres recibir los mensajes traducidos.",
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }


                // Esta es la nueva lista con scroll
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Ocupa el espacio restante
                ) {
                    items(supportedLanguages) { language ->
                        ListItem(
                            headlineContent = { Text(language.nativeName) }, // ej. "Español"
                            supportingContent = { Text(language.name) },     // ej. "Spanish"
                            modifier = Modifier.clickable {
                                // Llama a la función del ViewModel con el CÓDIGO de idioma
                                onLanguageSelected(language.code)
                            }
                        )
                    }
                }

                // Un botón de "Cancelar" que selecciona "es" por defecto
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Cancelar (Usar Español por defecto)")
                }
            }
        }
    }
}