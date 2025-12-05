package mx.castillo.edwin.mensajeria.ui.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Reply
// import androidx.compose.material.icons.outlined.StarOutline // Eliminado
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale // --- CORRECCIÓN: IMPORTE AÑADIDO ---
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog // --- CORRECCIÓN: IMPORTE AÑADIDO ---
import androidx.compose.ui.window.DialogProperties // --- CORRECCIÓN: IMPORTE AÑADIDO ---
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import mx.castillo.edwin.mensajeria.data.Message
import mx.castillo.edwin.mensajeria.data.User
import mx.castillo.edwin.mensajeria.ui.components.CustomToast
import mx.castillo.edwin.mensajeria.ui.components.Primary
import mx.castillo.edwin.mensajeria.ui.viewmodel.ChatViewModel
import mx.castillo.edwin.mensajeria.ui.viewmodel.DateSeparator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    chatId: String?,
    isDarkTheme: Boolean = false,
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val context = LocalContext.current

    var showNewMessagesButton by remember { mutableStateOf(false) }
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val viewModelToast by viewModel.toastMessage.collectAsState() // Escucha toasts del VM

    var selectedMessageId by remember { mutableStateOf<String?>(null) }
    var messageToReply by remember { mutableStateOf<Message?>(null) }

    var showDeleteBottomSheet by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<Message?>(null) }

    var selectedImageUrl by remember { mutableStateOf<String?>(null) }


    val replyToSenderName = when (messageToReply?.senderId) {
        currentUserId -> "Tú"
        else -> uiState.otherUser?.displayName
    } ?: "..."

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && chatId != null) {
            viewModel.sendImageMessage(chatId, uri)
        }
    }

    // --- TEMPORIZADOR DE UI (NUEVO) ---
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // Espera 1 segundo
            currentTime = System.currentTimeMillis() // Actualiza la hora
        }
    }
    // --- FIN DEL TEMPORIZADOR ---


    // --- LÓGICA DE FILTRADO Y AGRUPACIÓN (NUEVO) ---
    val groupedMessages by remember(uiState.messages, currentTime) {
        derivedStateOf {
            val now = currentTime // Usa la hora actual del estado
            // 1. Filtrar por tiempo
            val visibleMessages = uiState.messages.filter { msg ->
                msg.deleteAt == null || msg.deleteAt.toDate().time > now
            }
            // 2. Agrupar por fecha
            groupMessagesByDate(visibleMessages)
        }
    }
    // --- FIN LÓGICA DE FILTRADO ---


    // --- Effects ---
    // --- CORRECCIÓN 1: AÑADIDO 'onDispose' ---
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.setScreenActive(true)
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.setScreenActive(false)
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            viewModel.setScreenActive(false)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    // --- FIN CORRECCIÓN ---

    LaunchedEffect(chatId) {
        if (chatId != null) {
            viewModel.loadChat(chatId, context)
        }
    }

    LaunchedEffect(uiState.messages) { // (Scroll automático)
        val messageCount = groupedMessages.count { it is Message } // Cuenta solo mensajes
        if (messageCount > 0) {
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (lastVisibleItemIndex >= groupedMessages.size - 2) { // Compara con el tamaño de la lista agrupada
                scope.launch { listState.animateScrollToItem(groupedMessages.size) } // Scroll al final de la lista agrupada
            } else {
                showNewMessagesButton = true
            }
        }
    }

    LaunchedEffect(listState) { // (Botón de nuevos mensajes)
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { lastIndex ->
                val messageCount = groupedMessages.size
                lastIndex != null && messageCount > 0 && lastIndex >= messageCount - 2
            }
            .collect { showNewMessagesButton = false }
    }

    // Toast handler
    LaunchedEffect(viewModelToast) {
        viewModelToast?.let {
            toastMessage = it
            viewModel.onToastShown()
        }
    }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3000)
            toastMessage = null
        }
    }
    // --- Fin Effects ---

    // --- CORRECCIÓN 1: EL VISOR DE IMAGEN VA AQUÍ, FUERA DEL LAZYCOLUMN ---
    if (selectedImageUrl != null) {
        FullScreenImageViewer(
            imageUrl = selectedImageUrl!!,
            onDismiss = { selectedImageUrl = null }
        )
    }
    // --- FIN CORRECCIÓN 1 ---

    val backgroundColor = if (isDarkTheme) Color(0xFF101C22) else Color(0xFFF5F7F8)

    if (showDeleteBottomSheet && messageToDelete != null) {
        DeleteMessageBottomSheet(
            message = messageToDelete!!,
            onDismiss = { showDeleteBottomSheet = false },
            onDeleteForMe = {
                if (chatId != null) viewModel.deleteMessageForMe(chatId, messageToDelete!!.id)
            },
            onDeleteForEveryone = {
                if (chatId != null) viewModel.deleteMessageForEveryone(chatId, messageToDelete!!.id)
            }
        )
    }

    Scaffold(
        topBar = {
            ChatHeader(
                navController = navController,
                user = uiState.otherUser,
                isTyping = uiState.isOtherUserTyping,
                isDarkTheme = isDarkTheme,
                userStatus = uiState.userStatus,
                chatId = chatId
            )
        },
        bottomBar = {
            ChatInputBar(
                isDarkTheme = isDarkTheme,
                onSendMessage = { messageText, replyMsg ->
                    if (chatId != null) {
                        viewModel.sendMessage(chatId, messageText, replyMsg)
                        messageToReply = null
                    }
                },
                onTyping = { viewModel.onTyping() },
                enabled = uiState.isReady,
                messageToReply = messageToReply,
                replyToSenderName = replyToSenderName,
                onCancelReply = { messageToReply = null },
                onImageSelected = {
                    imagePickerLauncher.launch("image/*")
                },
                duration = uiState.disappearingMessagesDuration
            )
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // --- USA LA NUEVA LISTA AGRUPADA ---
                    items(items = groupedMessages, key = { it.hashCode() }) { item ->
                        when (item) {
                            is Message -> {
                                val isSentByMe = item.senderId == currentUserId
                                val isSelected = item.id == selectedMessageId
                                // --- CORRECCIÓN 1: El visor de imagen YA NO VA AQUÍ ---
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = if (isSentByMe) Arrangement.End else Arrangement.Start
                                ) {
                                    Box {
                                        if (item.isDeleted) {
                                            DeletedMessageBubble(isSentByMe = isSentByMe, isDarkTheme = isDarkTheme)
                                        } else {
                                            MessageBubble(
                                                message = item,
                                                isSentByMe = isSentByMe,
                                                isDarkTheme = isDarkTheme,
                                                otherUser = uiState.otherUser,
                                                currentUser = uiState.currentUser,
                                                isSelected = isSelected,
                                                modifier = Modifier.pointerInput(Unit) {
                                                    detectTapGestures(
                                                        onLongPress = { selectedMessageId = item.id },
                                                        onTap = { selectedMessageId = null }
                                                    )
                                                },
                                                onImageClick = { imageUrl ->
                                                    selectedImageUrl = imageUrl
                                                }

                                            )
                                            // --- MENÚ DE ACCIÓN (SIN "DESTACAR") ---
                                            MessageActionMenu(
                                                expanded = isSelected,
                                                onDismissRequest = { selectedMessageId = null },
                                                message = item,
                                                onDelete = {
                                                    messageToDelete = item
                                                    showDeleteBottomSheet = true
                                                    selectedMessageId = null
                                                },
                                                onReply = { message ->
                                                    messageToReply = message
                                                    selectedMessageId = null
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            is DateSeparator -> {
                                DateSeparatorItem(date = item.date, isDarkTheme = isDarkTheme)
                            }
                        }
                    }
                }

                // --- CORRECCIÓN 2: AnimatedVisibility ESTÁ BIEN, PERO NECESITA EL SCOPE DE Box ---
                // (La llamada en sí está bien, el error del compilador era falso)
                AnimatedVisibility(
                    visible = showNewMessagesButton,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp), // Se alinea dentro del Box
                    enter = fadeIn(), exit = fadeOut()
                ) {
                    Button(
                        onClick = { scope.launch { listState.animateScrollToItem(groupedMessages.size) } }, // Usa groupedMessages.size
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Nuevos mensajes")
                    }
                }
            }
            CustomToast(
                message = toastMessage,
                modifier = Modifier.align(Alignment.BottomEnd).systemBarsPadding().padding(bottom = 1.dp)
            )
        }
    }
}

// --- MENÚ DE ACCIÓN (SIN "DESTACAR") ---
@Composable
fun MessageActionMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    message: Message,
    onDelete: () -> Unit,
    onReply: (Message) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        MenuItem(icon = Icons.Outlined.Reply, text = "Responder") { onReply(message); onDismissRequest() }
        MenuItem(icon = Icons.Outlined.ContentCopy, text = "Copiar") {
            clipboardManager.setText(AnnotatedString(message.text))
            onDismissRequest()
        }
        MenuItem(icon = Icons.Outlined.Delete, text = "Eliminar", isDestructive = true) { onDelete(); onDismissRequest() }
    }
}

@Composable
private fun MenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    DropdownMenuItem(
        text = { Text(text, color = contentColor) },
        onClick = onClick,
        leadingIcon = { Icon(imageVector = icon, contentDescription = text, tint = contentColor) }
    )
}

// --- MessageBubble (SIN "isStarred") ---
@Composable
fun MessageBubble(
    message: Message,
    isSentByMe: Boolean,
    isDarkTheme: Boolean,
    otherUser: User?,
    currentUser: User?,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onImageClick: (String) -> Unit
) {
    val textToShow = message.text
    val bubbleShape = if (isSentByMe) RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp) else RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)
    val defaultBubbleColor = if (isSentByMe) Primary else MaterialTheme.colorScheme.surfaceVariant
    val selectedBubbleColor = if (isDarkTheme) Primary.copy(alpha = 0.5f) else Primary.copy(alpha = 0.3f)
    val animatedBubbleColor by animateColorAsState(if (isSelected) selectedBubbleColor else defaultBubbleColor, tween(200))
    val animatedScale by animateFloatAsState(if (isSelected) 1.05f else 1.0f, tween(200))
    val bubbleTextColor = if (isSentByMe) Color.White else MaterialTheme.colorScheme.onSurface
    val formattedTime = message.timestamp?.let { formatTimestamp(it) } ?: ""

    Column(
        horizontalAlignment = if (isSentByMe) Alignment.End else Alignment.Start,
        modifier = modifier
            .graphicsLayer(scaleX = animatedScale, scaleY = animatedScale)
            .widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.8f)
    ) {
        if (message.imageUrl != null) {
            ImageBubble(
                imageUrl = message.imageUrl,
                isSentByMe = isSentByMe,
                timestamp = formattedTime,
                onImageClick = onImageClick
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(animatedBubbleColor)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
                    if (message.replyToMessageId != null) {
                        ReplyDisplay(
                            message = message,
                            isSentByMe = isSentByMe,
                            otherUser = otherUser,
                            currentUser = currentUser
                        )
                    }
                    if (message.text.isNotBlank()) {
                        Text(text = textToShow, color = bubbleTextColor)
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val mutedTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
            Text(text = formattedTime, fontSize = 12.sp, color = mutedTextColor)
            if (isSentByMe) {
                Spacer(Modifier.width(4.dp))
                val readReceiptsEnabled = currentUser?.readReceiptsEnabled ?: true
                val isRead = readReceiptsEnabled && otherUser?.uid in message.readBy
                val icon = when {
                    isRead -> Icons.Default.DoneAll
                    message.status == "enviado" -> Icons.Default.Check
                    else -> Icons.Default.Schedule
                }
                Icon(icon, "Estado del mensaje", Modifier.size(16.dp), tint = if (isRead) Color(0xFF53BDEB) else mutedTextColor)
            }
        }
    }
}

// --- ImageBubble (SIN "isStarred") ---
@Composable
fun ImageBubble(
    imageUrl: String,
    isSentByMe: Boolean,
    timestamp: String,
    onImageClick: (String) -> Unit
) {
    val bubbleShape = if (isSentByMe) RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp) else RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)
    Column(horizontalAlignment = if (isSentByMe) Alignment.End else Alignment.Start) {
        Box(
            modifier = Modifier
                .width(LocalConfiguration.current.screenWidthDp.dp * 0.6f)
                .clip(bubbleShape)
                // --- CORRECCIÓN 2: USAR onImageClick EN LUGAR DE selectedImageUrl ---
                .clickable { onImageClick(imageUrl) }
            // --- FIN CORRECCIÓN 2 ---
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Imagen enviada",
                contentScale = ContentScale.Crop,
                modifier = Modifier.aspectRatio(1f)
            )
        }
    }
}


// --- FUNCIONES AUXILIARES (AHORA NECESARIAS EN CHATSCREEN) ---

fun groupMessagesByDate(messages: List<Message>): List<Any> {
    val groupedList = mutableListOf<Any>()
    var lastDate: Calendar? = null
    messages.forEach { message ->
        val messageDate = Calendar.getInstance().apply { time = message.timestamp ?: Date() }
        if (lastDate == null || !isSameDay(lastDate!!, messageDate)) {
            groupedList.add(DateSeparator(messageDate.time))
        }
        groupedList.add(message)
        lastDate = messageDate
    }
    return groupedList
}

private fun formatDateForSeparator(date: Date): String {
    val messageCalendar = Calendar.getInstance().apply { time = date }
    val nowCalendar = Calendar.getInstance()
    return when {
        isSameDay(messageCalendar, nowCalendar) -> "HOY"
        isSameDay(messageCalendar, nowCalendar.apply { add(Calendar.DAY_OF_YEAR, -1) }) -> "AYER"
        else -> SimpleDateFormat("dd 'de' MMMM", Locale("es", "ES")).format(date).uppercase()
    }
}

private fun formatTimestamp(timestamp: Date): String {
    val messageCalendar = Calendar.getInstance().apply { time = timestamp }
    val nowCalendar = Calendar.getInstance()
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    return if (isSameDay(messageCalendar, nowCalendar)) {
        timeFormat.format(timestamp)
    } else {
        timeFormat.format(timestamp) // Mantenemos el formato de hora simple
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

// --- Resto de Composables (sin cambios) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteMessageBottomSheet(
    message: Message,
    onDismiss: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
    val timeLimitMillis = 3_600_000
    val messageTimestamp = message.timestamp?.time ?: 0
    val isWithinTimeLimit = (System.currentTimeMillis() - messageTimestamp) < timeLimitMillis
    val canDeleteForEveryone = message.senderId == currentUserUid && isWithinTimeLimit

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text("Eliminar mensaje", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            ListItem(
                headlineContent = { Text("Eliminar para mí") },
                leadingContent = { Icon(Icons.Default.PersonRemove, contentDescription = "Eliminar para mí") },
                modifier = Modifier.clickable {
                    onDeleteForMe()
                    onDismiss()
                }
            )
            if (canDeleteForEveryone) {
                ListItem(
                    headlineContent = { Text("Eliminar para todos", color = MaterialTheme.colorScheme.error) },
                    leadingContent = {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Eliminar para todos", tint = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier.clickable {
                        onDeleteForEveryone()
                        onDismiss()
                    }
                )
            }
            ListItem(
                headlineContent = { Text("Cancelar") },
                leadingContent = { Icon(Icons.Default.Cancel, contentDescription = "Cancelar") },
                modifier = Modifier.clickable { onDismiss() }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DeletedMessageBubble(isSentByMe: Boolean, isDarkTheme: Boolean) {
    val bubbleColor = if (isDarkTheme) Color(0xFF2D3748) else Color(0xFFE2E8F0)
    val textColor = if (isDarkTheme) Color(0xFF718096) else Color(0xFF718096)
    val bubbleShape = if (isSentByMe) RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp) else RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(bubbleShape).background(bubbleColor).padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Icon(Icons.Default.Block, "Mensaje eliminado", tint = textColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("Mensaje eliminado", fontStyle = FontStyle.Italic, color = textColor, fontSize = 14.sp)
    }
}

@Composable
fun ReplyDisplay(message: Message, isSentByMe: Boolean, otherUser: User?, currentUser: User?) {
    val senderName = if (message.replyToSenderId == currentUser?.uid) "Tú" else otherUser?.displayName ?: "..."
    val replyBackgroundColor = if (isSentByMe) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.06f)
    val senderNameColor = if (isSentByMe) Color(0xFF81D4FA) else Primary
    val replyTextColor = if (isSentByMe) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    val separatorColor = if (isSentByMe) senderNameColor else Primary

    Row(
        modifier = Modifier
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(replyBackgroundColor)
            .padding(start = 8.dp)
    ) {
        Divider(Modifier.height(38.dp).width(2.dp).clip(RoundedCornerShape(1.dp)), color = separatorColor)
        Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)) {
            Text(senderName, fontWeight = FontWeight.Bold, color = senderNameColor, fontSize = 13.sp)
            Text(message.replyToMessageText ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis, color = replyTextColor, fontSize = 12.sp)
        }
    }
}

@Composable
fun DeleteMessageDialog(
    message: Message,
    onDismiss: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit
) {
    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
    val timeLimitMillis = 3_600_000
    val messageTimestamp = message.timestamp?.time ?: 0
    val isWithinTimeLimit = (System.currentTimeMillis() - messageTimestamp) < timeLimitMillis
    val canDeleteForEveryone = message.senderId == currentUserUid && isWithinTimeLimit

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar mensaje") },
        text = {
            Column {
                TextButton(onClick = {
                    onDeleteForMe()
                    onDismiss()
                }) {
                    Text("Eliminar para mí")
                }
                if (canDeleteForEveryone) {
                    TextButton(onClick = {
                        onDeleteForEveryone()
                        onDismiss()
                    }) {
                        Text("Eliminar para todos")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(
    isDarkTheme: Boolean,
    onSendMessage: (String, Message?) -> Unit,
    onTyping: () -> Unit,
    enabled: Boolean,
    messageToReply: Message?,
    replyToSenderName: String,
    onCancelReply: () -> Unit,
    onImageSelected: () -> Unit,
    duration: Long
) {
    var text by remember { mutableStateOf("") }
    val surfaceColor = if (isDarkTheme) Color(0xFF182832) else Color.White

    Surface(color = surfaceColor, tonalElevation = 8.dp) {
        Column {
            AnimatedVisibility(visible = messageToReply != null) {
                ReplyPreview(message = messageToReply, senderName = replyToSenderName, onCancel = onCancelReply)
            }
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(visible = duration > 0, enter = fadeIn(), exit = fadeOut()) {
                    Icon(
                        imageVector = Icons.Default.Timer, // El ícono que ya usas
                        contentDescription = "Mensajes temporales activos",
                        tint = Color.Gray,
                        modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                    )
                }
                TextField(
                    value = text,
                    onValueChange = { text = it; if (it.isNotEmpty()) onTyping() },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe un mensaje...") },
                    shape = CircleShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                )
                IconButton(onClick = onImageSelected) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Adjuntar imagen", tint = Color.Gray)
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { if (text.isNotBlank()) { onSendMessage(text, messageToReply); text = "" } },
                    enabled = enabled,
                    modifier = Modifier.clip(CircleShape).background(Primary).size(48.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ReplyPreview(message: Message?, senderName: String, onCancel: () -> Unit) {
    if (message == null) return
    val previewText = message.text.ifBlank { "Mensaje" }

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(Modifier.height(36.dp).width(4.dp).clip(RoundedCornerShape(2.dp)), color = Primary)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text("Respondiendo a $senderName", fontWeight = FontWeight.Bold, color = Primary, fontSize = 14.sp)
            Text(previewText, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
        }
        IconButton(onClick = onCancel) {
            Icon(Icons.Default.Close, contentDescription = "Cancelar respuesta")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHeader(
    navController: NavController,
    user: User?,
    isTyping: Boolean,
    userStatus: String,
    isDarkTheme: Boolean,
    chatId: String?
) {
    val surfaceColor = if (isDarkTheme) Color(0xFF182832) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val mutedTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    enabled = user != null,
                    onClick = {
                        user?.uid?.let { userId ->
                            if (chatId != null) { // 'chatId' es el parámetro que recibe tu ChatScreen
                                navController.navigate("contactInfo/$userId/$chatId")
                            }
                        }
                    }
                )
            ) {
                AsyncImage(
                    model = user?.photoUrl ?: "https://i.pravatar.cc/100",
                    contentDescription = "Avatar de ${user?.displayName}",
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(user?.displayName ?: "Cargando...", fontWeight = FontWeight.Bold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (isTyping) "Escribiendo..." else userStatus, fontSize = 12.sp, color = if (isTyping) Primary else mutedTextColor)
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = mutedTextColor)
            }
        },
        actions = {
            IconButton(onClick = { /*TODO*/ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Más opciones", tint = mutedTextColor)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceColor)
    )
}


@Composable
fun FullScreenImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        // Propiedades para que ocupe casi toda la pantalla
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                // Tocar en cualquier parte del fondo cierra la imagen
                .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
            contentAlignment = Alignment.Center
        ) {
            // La imagen
            AsyncImage(
                model = imageUrl,
                contentDescription = "Imagen en pantalla completa",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), // Un padding para que no toque los bordes
                contentScale = ContentScale.Fit // Asegura que se vea completa
            )

            // Botón de cerrar (opcional, pero útil)
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
            }
        }
    }
}

@Composable
fun DateSeparatorItem(date: Date, isDarkTheme: Boolean) {
    val textColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
    val backgroundColor = if (isDarkTheme) Color(0xFF182832) else Color(0xFFE2E8F0)

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formatDateForSeparator(date),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(backgroundColor).padding(vertical = 4.dp, horizontal = 8.dp)
        )
    }
}