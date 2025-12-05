package mx.castillo.edwin.mensajeria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun AppBottomNavigation(navController: NavController, currentRoute: String?) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 8.dp
    ) {
        // Ítem de Chats
        NavigationItem(
            label = "Chats",
            icon = if (currentRoute == "conversations") Icons.Filled.Chat else Icons.Outlined.Chat,
            isSelected = currentRoute == "conversations",
            onClick = { if (currentRoute != "conversations") navController.navigate("conversations") }
        )

        // Ítem de Contactos
        NavigationItem(
            label = "Contactos",
            icon = if (currentRoute == "contacts") Icons.Filled.Groups else Icons.Outlined.Groups,
            isSelected = currentRoute == "contacts",
            onClick = { if (currentRoute != "contacts") navController.navigate("contacts") }
        )

        // Ítem de Perfil
        NavigationItem(
            label = "Perfil",
            icon = if (currentRoute == "profile") Icons.Filled.Person else Icons.Outlined.Person,
            isSelected = currentRoute == "profile",
            onClick = { if (currentRoute != "profile") navController.navigate("profile") }
        )
    }
}

@Composable
fun RowScope.NavigationItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
        icon = {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Primary.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Icon(icon, contentDescription = label)
                }
            } else {
                Icon(icon, contentDescription = label)
            }
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Primary,
            selectedTextColor = Primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthInput(
    value: String,
    onValueChange: (String) -> Unit, // <-- LÍNEA CORREGIDA
    placeholder: String,
    placeholderColor: Color,
    backgroundColor: Color,
    textColor: Color,
    enabled: Boolean = true
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
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