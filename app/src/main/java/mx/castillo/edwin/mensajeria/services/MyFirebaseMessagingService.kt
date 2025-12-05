package mx.castillo.edwin.mensajeria.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import mx.castillo.edwin.mensajeria.MainActivity // <-- 1. IMPORTA TU ACTIVIDAD PRINCIPAL
import mx.castillo.edwin.mensajeria.R // <-- 2. IMPORTA TUS RECURSOS (PARA EL ÍCONO)

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // --- ESTA PARTE QUEDA IGUAL ---
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        saveTokenToFirestore(token)
    }

    private fun saveTokenToFirestore(token: String) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            val userDocRef = FirebaseFirestore.getInstance().collection("users").document(currentUserUid)
            userDocRef.update("fcmToken", token)
                .addOnSuccessListener { Log.d("FCM", "Token saved to Firestore") }
                .addOnFailureListener { e -> Log.w("FCM", "Error saving token", e) }
        }
    }

    // --- ESTA ES LA PARTE NUEVA Y MÁS IMPORTANTE ---
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM", "Mensaje recibido desde: ${remoteMessage.from}")

        // Los datos vendrán en el payload "data" que enviaremos desde la Cloud Function
        remoteMessage.data.let { data ->
            val title = data["title"]
            val body = data["body"]
            val chatId = data["chatId"] // <-- Esencial para abrir el chat correcto

            if (title != null && body != null && chatId != null) {
                Log.d("FCM", "Mostrando notificación para el chat: $chatId")
                sendNotification(title, body, chatId)
            } else {
                Log.w("FCM", "Payload de la notificación incompleto: $data")
            }
        }
    }

    /**
     * Construye y muestra la notificación en la barra de estado.
     */
    private fun sendNotification(title: String, messageBody: String, chatId: String) {
        // 1. Define qué pasa cuando el usuario TOCA la notificación
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Añadimos el chatId para que la MainActivity sepa qué chat abrir
            putExtra("chat_id_from_notification", chatId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Construye la notificación
        val channelId = getString(R.string.default_notification_channel_id) // Necesitarás añadir esto a tus strings.xml
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_icon) // <-- ⚠️ DEBES CREAR ESTE ÍCONO
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true) // La notificación desaparece al tocarla
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // 3. (Obligatorio en Android 8+) Crea el Canal de Notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Mensajes Nuevos", // Nombre visible para el usuario
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 4. Muestra la notificación
        val notificationId = (0..10000).random() // Un ID al azar para la notificación
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}