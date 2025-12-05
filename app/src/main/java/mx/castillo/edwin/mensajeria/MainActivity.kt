package mx.castillo.edwin.mensajeria

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.delay
import mx.castillo.edwin.mensajeria.ui.components.CustomToast
import mx.castillo.edwin.mensajeria.ui.pages.*
// Asegúrate de que este import exista (creaste el archivo en el paso anterior):
import mx.castillo.edwin.mensajeria.ui.pages.MaintenanceScreen
import mx.castillo.edwin.mensajeria.ui.theme.MensajeriaTheme
import mx.castillo.edwin.mensajeria.ui.viewmodel.UserPresenceViewModel

class MainActivity : ComponentActivity(), LifecycleEventObserver {

    private val userPresenceViewModel: UserPresenceViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Manejo de permisos
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Inicialización de Firebase
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        askNotificationPermission()
        lifecycle.addObserver(this)

        setContent {
            //val isDarkTheme = isSystemInDarkTheme()
            val isDarkTheme = false

            var isAppEnabled by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                // USAMOS LA INSTANCIA DIRECTA (SIN KTX)
                val remoteConfig = FirebaseRemoteConfig.getInstance()

                val configSettings = FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(0) // 0 para pruebas, 3600 para producción
                    .build()

                remoteConfig.setConfigSettingsAsync(configSettings)

                // Valores por defecto
                val defaultDefaults = mapOf("app_enabled" to true)
                remoteConfig.setDefaultsAsync(defaultDefaults)

                // Buscar cambios
                remoteConfig.fetchAndActivate()
                    .addOnCompleteListener(this@MainActivity) { task ->
                        if (task.isSuccessful) {
                            val updated = task.result
                            isAppEnabled = remoteConfig.getBoolean("app_enabled")
                            Log.d("RemoteConfig", "Config params updated: $updated. Enabled: $isAppEnabled")
                        } else {
                            Log.e("RemoteConfig", "Fetch failed")
                        }
                    }
            }
            // ---------------------------------------------

            MensajeriaTheme(darkTheme = isDarkTheme) {
                val systemUiController = rememberSystemUiController()
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        darkIcons = !isDarkTheme
                    )
                }

                // --- AQUÍ APLICAMOS EL BLOQUEO ---
                if (!isAppEnabled) {
                    // Si Firebase dice FALSE, mostramos pantalla de mantenimiento
                    MaintenanceScreen()
                } else {
                    // Si Firebase dice TRUE (o error de red), mostramos la App normal
                    MainAppContent(isDarkTheme)
                }
            }
        }
    }

    // He movido todo tu contenido de navegación aquí para limpiar el onCreate
    @Composable
    fun MainAppContent(isDarkTheme: Boolean) {
        val navController = rememberNavController()
        var toastMessage by remember { mutableStateOf<String?>(null) }
        val showToast: (String) -> Unit = { message ->
            toastMessage = message
        }

        LaunchedEffect(toastMessage) {
            if (toastMessage != null) {
                delay(3000)
                toastMessage = null
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = "splash") {
                composable("splash") { SplashScreen(navController) }
                composable("login") { LoginScreen(navController, isDarkTheme, showToast = showToast) }
                composable("signup") { SignUpScreen(navController, isDarkTheme, showToast = showToast) }
                composable("conversations") { ConversationsScreen(navController, isDarkTheme) }
                composable("contacts") { ContactsScreen(navController, isDarkTheme, showToast = showToast) }
                composable("profile") { ProfileScreen(navController, isDarkTheme) }

                composable("chat/{chatId}") { backStackEntry ->
                    val chatId = backStackEntry.arguments?.getString("chatId")
                    ChatScreen(navController = navController, chatId = chatId, isDarkTheme = isDarkTheme)
                }

                composable(
                    route = "contactInfo/{userId}/{chatId}",
                    arguments = listOf(
                        navArgument("userId") { type = NavType.StringType; nullable = true },
                        navArgument("chatId") { type = NavType.StringType; nullable = true }
                    )
                ) { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId")
                    val chatId = backStackEntry.arguments?.getString("chatId")

                    ContactInfoScreen(
                        navController = navController,
                        userId = userId,
                        chatId = chatId,
                        isDarkTheme = isDarkTheme
                    )
                }

                composable(
                    route = "starredMessages/{chatId}",
                    arguments = listOf(navArgument("chatId") { type = NavType.StringType; nullable = true })
                ) { backStackEntry ->
                    val chatId = backStackEntry.arguments?.getString("chatId")
                    StarredMessagesScreen(
                        navController = navController,
                        chatId = chatId,
                        isDarkTheme = isDarkTheme
                    )
                }

                composable("search_users") { SearchUsersScreen(navController, isDarkTheme, showToast = showToast) }
                composable("create_username") { CreateUsernameScreen(navController) }
                composable("verify_email") { VerifyEmailScreen(navController, showToast = showToast) }
                composable("about") { AboutScreen(navController, isDarkTheme) }
                composable("privacy") {
                    PrivacyScreen(navController = navController, isDarkTheme = isDarkTheme)
                }
            }

            CustomToast(
                message = toastMessage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .systemBarsPadding()
            )
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                userPresenceViewModel.onAppForegrounded()
            }
            Lifecycle.Event.ON_STOP -> {
                userPresenceViewModel.onAppBackgrounded()
            }
            else -> {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(this)
    }
}