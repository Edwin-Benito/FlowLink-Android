package mx.castillo.edwin.mensajeria.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserPresenceViewModel : ViewModel() {

    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var databaseRef: DatabaseReference? = null
    private var onlineStatusRef: DatabaseReference? = null
    private var lastSeenRef: DatabaseReference? = null

    init {
        if (currentUser != null) {
            // Obtenemos la referencia a la raíz de la base de datos de presencia
            databaseRef = FirebaseDatabase.getInstance().getReference("/status/${currentUser.uid}")

            // Referencias específicas para 'online' y 'last_seen'
            onlineStatusRef = databaseRef?.child("online")
            lastSeenRef = databaseRef?.child("last_seen")

            // Cuando nos conectamos a Firebase, nos marcamos como online
            val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")
            connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    if (connected) {
                        // Cuando el usuario se CONECTA
                        onlineStatusRef?.setValue(true)
                        // Cuando el usuario se DESCONECTA (cierra la app, pierde internet)
                        // Firebase ejecutará estas acciones automáticamente
                        onlineStatusRef?.onDisconnect()?.setValue(false)
                        lastSeenRef?.onDisconnect()?.setValue(ServerValue.TIMESTAMP)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    // Manejar error si es necesario
                }
            })
        }
    }

    // Llamar a este método cuando la app va a segundo plano
    fun onAppBackgrounded() {
        onlineStatusRef?.setValue(false)
        lastSeenRef?.setValue(ServerValue.TIMESTAMP)
    }

    // Llamar a este método cuando la app vuelve al primer plano
    fun onAppForegrounded() {
        onlineStatusRef?.setValue(true)
    }

}