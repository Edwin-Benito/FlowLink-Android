package mx.castillo.edwin.mensajeria.data

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.Timestamp // <-- IMPORTANTE: Añade este import
import java.util.Date

data class User(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val preferredLanguage: String = "",
    val fcmToken: String? = null,
    val bio: String = "",
    val readReceiptsEnabled: Boolean = true,
    val lastSeenPrivacy: String = "Todos",
    val publicKey: String? = null
)

data class Chat(
    val participants: List<String> = emptyList(),
    val lastMessage: Map<String, String> = emptyMap(),
    @ServerTimestamp
    val lastMessageTimestamp: Date? = null,
    val typingUsers: List<String> = emptyList(),
    val lastMessageSenderId: String? = null,
    val disappearingMessagesDuration: Long = 0,
    val mutedBy: List<String> = emptyList()

)

data class Message(
    @DocumentId
    val id: String = "",
    val senderId: String = "",
    @ServerTimestamp
    val timestamp: Date? = null,
    val status: String = "enviando",
    val readBy: List<String> = emptyList(),
    val payloads: Map<String, String> = emptyMap(),

    @get:Exclude
    var text: String = "",

    @get:PropertyName("isDeleted")
    val isDeleted: Boolean = false,

    val replyToMessageId: String? = null,
    val replyToMessageText: String? = null,
    val replyToSenderId: String? = null,
    val imageUrl: String? = null,
    val deletedFor: List<String> = emptyList(),
    val participants: List<String> = emptyList(),
    val deleteAt: Timestamp? = null

)