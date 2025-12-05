package mx.castillo.edwin.mensajeria.ui.pages

import android.R.attr.textColor
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import mx.castillo.edwin.mensajeria.R
import mx.castillo.edwin.mensajeria.ui.components.*
import mx.castillo.edwin.mensajeria.ui.viewmodel.LoginUiState
import mx.castillo.edwin.mensajeria.ui.viewmodel.LoginViewModel

private enum class LoadingButton { NONE, EMAIL, GOOGLE, FACEBOOK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    isDarkTheme: Boolean = false,
    viewModel: LoginViewModel = viewModel(),
    showToast: (String) -> Unit
) {
    // --- LÓGICA PARA EL BOTTOM SHEET ---
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()
    var loadingButton by remember { mutableStateOf(LoadingButton.NONE) }


    val backgroundColor = if (isDarkTheme) BackgroundDark else BackgroundLight
    val textColor = if (isDarkTheme) TextDark else TextLight
    val mutedTextColor = if (isDarkTheme) TextMutedDark else TextMutedLight
    val inputBackgroundColor = if (isDarkTheme) InputDark else InputLight
    val dividerColor = if (isDarkTheme) InputDark else InputLight

    val toastMessage by viewModel.toastMessage.collectAsState()
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            showToast(it)
            viewModel.onToastShown()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState !is LoginUiState.Loading) {
            loadingButton = LoadingButton.NONE
        }
        when (uiState) {
            is LoginUiState.Success -> {
                navController.navigate("conversations") { popUpTo("login") { inclusive = true } }
            }
            is LoginUiState.SuccessFirstTime -> {
                navController.navigate("create_username") { popUpTo("login") { inclusive = true } }
            }
            else -> {}
        }
    }

    // --- Si showBottomSheet es true, mostramos el modal ---
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            dragHandle = null
        ) {
            ForgotPasswordSheet(
                viewModel = viewModel,
                textColor = textColor,
                mutedTextColor = mutedTextColor,
                inputBackgroundColor = inputBackgroundColor,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showBottomSheet = false
                        }
                    }
                }
            )
        }
    }

    val context = LocalContext.current
    val oneTapClient = remember { Identity.getSignInClient(context) }
    val signInRequest = remember {
        BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            ).build()
    }
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                credential.googleIdToken?.let { idToken ->
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    viewModel.signInWithCredential(firebaseCredential)
                }
            } catch (e: Exception) {
                viewModel.setUiState(LoginUiState.Error("Error al iniciar con Google."))
            }
        }
    }
    val callbackManager = remember { CallbackManager.Factory.create() }
    val loginManager = remember { LoginManager.getInstance() }
    DisposableEffect(Unit) {
        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                viewModel.signInWithCredential(credential)
            }
            override fun onCancel() { viewModel.setUiState(LoginUiState.Error("Inicio con Facebook cancelado.")) }
            override fun onError(error: FacebookException) { viewModel.setUiState(LoginUiState.Error("Error de Facebook: ${error.message}")) }
        })
        onDispose { loginManager.unregisterCallback(callbackManager) }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Sign in", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MainContent(
                modifier = Modifier.weight(1f),
                navController = navController,
                loadingButton = loadingButton,
                textColor = textColor,
                mutedTextColor = mutedTextColor,
                inputBackgroundColor = inputBackgroundColor,
                dividerColor = dividerColor,
                onForgotPasswordClick = { showBottomSheet = true }, // Acción para abrir el modal
                onGoogleSignInClick = {
                    loadingButton = LoadingButton.GOOGLE
                    oneTapClient.beginSignIn(signInRequest)
                        .addOnSuccessListener { result ->
                            try {
                                launcher.launch(IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
                            } catch (e: Exception) {
                                viewModel.setUiState(LoginUiState.Error("No se pudo iniciar Google Sign-In."))
                            }
                        }
                        .addOnFailureListener {
                            viewModel.setUiState(LoginUiState.Error("Fallo al conectar con servicios de Google."))
                        }
                },
                onFacebookSignInClick = {
                    loadingButton = LoadingButton.FACEBOOK
                    val activity = context as? ComponentActivity
                    activity?.let {
                        loginManager.logInWithReadPermissions(it, callbackManager, listOf("email", "public_profile"))
                    }
                },
                onEmailSignInClick = { email, password ->
                    loadingButton = LoadingButton.EMAIL
                    viewModel.signInWithEmail(email, password)
                }
            )

            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                SignUpNavigation(mutedTextColor = mutedTextColor, navController = navController, isEnabled = (loadingButton == LoadingButton.NONE))
            }
        }
    }
}


@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    navController: NavController,
    loadingButton: LoadingButton,
    textColor: Color,
    mutedTextColor: Color,
    inputBackgroundColor: Color,
    dividerColor: Color,
    onForgotPasswordClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onFacebookSignInClick: () -> Unit,
    onEmailSignInClick: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isAnyButtonLoading = loadingButton != LoadingButton.NONE

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LoginHeader(textColor, mutedTextColor)
        Spacer(Modifier.height(32.dp))

        EmailInput(email, { email = it }, mutedTextColor, inputBackgroundColor, textColor, !isAnyButtonLoading)
        Spacer(Modifier.height(16.dp))
        PasswordInput(password, { password = it }, mutedTextColor, inputBackgroundColor, textColor, !isAnyButtonLoading)
        Spacer(Modifier.height(8.dp))
        ForgotPasswordText(isEnabled = !isAnyButtonLoading, onClick = onForgotPasswordClick)
        Spacer(Modifier.height(24.dp))

        SignInButton(
            isLoading = loadingButton == LoadingButton.EMAIL,
            onClick = { onEmailSignInClick(email, password) }
        )
        Spacer(Modifier.height(32.dp))

        OrDivider(mutedTextColor, dividerColor)
        Spacer(Modifier.height(32.dp))

        SocialLoginButtons(
            textColor = textColor,
            buttonBackgroundColor = inputBackgroundColor,
            isGoogleLoading = loadingButton == LoadingButton.GOOGLE,
            isFacebookLoading = loadingButton == LoadingButton.FACEBOOK,
            onGoogleClick = onGoogleSignInClick,
            onFacebookClick = onFacebookSignInClick
        )
    }
}

@Composable
private fun LoginHeader(textColor: Color, mutedTextColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Welcome back",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sign in to continue",
            fontSize = 16.sp,
            color = mutedTextColor
        )
    }
}

@Composable
private fun ForgotPasswordText(isEnabled: Boolean, onClick: () -> Unit) {
    Text(
        text = "Forgot password?",
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = if(isEnabled) Primary else TextMutedLight,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.End)
            .clickable(enabled = isEnabled, onClick = onClick)
    )
}
@Composable
fun ForgotPasswordSheet(
    viewModel: LoginViewModel,
    onDismiss: () -> Unit,
    textColor: Color,
    mutedTextColor: Color,
    inputBackgroundColor: Color
) {
    var email by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is LoginUiState.Loading

    val toastMessage by viewModel.toastMessage.collectAsState()
    LaunchedEffect(toastMessage) {
        if (toastMessage == "Enlace de recuperación enviado. Revisa tu correo.") {
            onDismiss()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(mutedTextColor.copy(alpha = 0.4f))
        )
        Spacer(Modifier.height(24.dp))

        Text("Recuperar Contraseña", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(Modifier.height(8.dp))
        Text(
            "Ingresa tu correo para recibir un enlace de recuperación.",
            textAlign = TextAlign.Center,
            color = mutedTextColor
        )
        Spacer(Modifier.height(24.dp))

        // --- AJUSTE 3: Usamos los colores correctos en el EmailInput ---
        EmailInput(
            value = email,
            onValueChange = { email = it },
            placeholderColor = mutedTextColor,
            backgroundColor = inputBackgroundColor,
            textColor = textColor,
            enabled = !isLoading
        )
        Spacer(Modifier.height(16.dp))

        // --- AJUSTE 4: Usamos el color primario de la app en el botón ---
        Button(
            onClick = { viewModel.sendPasswordReset(email) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary) // Color correcto
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text("Enviar Enlace")
            }
        }
    }
}

@Composable
private fun SignInButton(onClick: () -> Unit, isLoading: Boolean) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Primary)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text("Sign in", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun OrDivider(textColor: Color, dividerColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Divider(modifier = Modifier.weight(1f), color = dividerColor)
        Text(
            text = "OR",
            color = textColor,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Divider(modifier = Modifier.weight(1f), color = dividerColor)
    }
}

@Composable
private fun SocialButtonContent(iconRes: Int, text: String, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = text,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
    }
}

@Composable
private fun SocialLoginButtons(
    textColor: Color,
    buttonBackgroundColor: Color,
    isGoogleLoading: Boolean,
    isFacebookLoading: Boolean,
    onGoogleClick: () -> Unit,
    onFacebookClick: () -> Unit
) {
    val isAnySocialLoading = isGoogleLoading || isFacebookLoading

    Column {
        Button(
            onClick = onGoogleClick,
            enabled = !isAnySocialLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonBackgroundColor)
        ) {
            if (isGoogleLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = textColor,
                    strokeWidth = 2.dp
                )
            } else {
                SocialButtonContent(R.drawable.ic_google_logo, "Iniciar con Google", textColor)
            }
        }
        Spacer(Modifier.height(16.dp))

        /*Button(
            onClick = onFacebookClick,
            enabled = !isAnySocialLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonBackgroundColor)
        ) {
            if (isFacebookLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = textColor,
                    strokeWidth = 2.dp
                )
            } else {
                SocialButtonContent(R.drawable.ic_facebook, "Iniciar con Facebook", textColor)
            }
        }*/
    }
}

@Composable
private fun SignUpNavigation(mutedTextColor: Color, navController: NavController, isEnabled: Boolean) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Don't have an account?", fontSize = 14.sp, color = mutedTextColor)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Sign up",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isEnabled) Primary else mutedTextColor,
            modifier = Modifier.clickable(enabled = isEnabled) {
                navController.navigate("signup")
            }
        )
    }
}