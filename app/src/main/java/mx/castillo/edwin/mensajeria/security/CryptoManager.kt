// Copia y pega para reemplazar TODO el contenido de CryptoManager.kt

package mx.castillo.edwin.mensajeria.security

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.HybridDecrypt
import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
//import com.google.crypto.tink.android.AndroidKeysetManager
import com.google.crypto.tink.hybrid.HybridConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException

class CryptoManager {

    companion object {
        private const val KEYSET_NAME = "e2ee_keyset"
        private const val PREFERENCE_FILE = "e2ee_preference"
        private const val MASTER_KEY_URI = "android-keystore://e2ee_master_key"
    }

    private lateinit var privateKeysetHandle: KeysetHandle
    private lateinit var publicKeysetHandle: KeysetHandle

    fun init(context: Context) {
        HybridConfig.register()
        try {
            privateKeysetHandle = AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, PREFERENCE_FILE)
                .withKeyTemplate(KeyTemplates.get("ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM"))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle

            publicKeysetHandle = privateKeysetHandle.publicKeysetHandle
        } catch (e: GeneralSecurityException) {
            throw RuntimeException("No se pudieron inicializar las claves criptográficas", e)
        }
    }

    fun getPublicKeyString(): String {
        val outputStream = ByteArrayOutputStream()
        publicKeysetHandle.writeNoSecret(BinaryKeysetWriter.withOutputStream(outputStream))
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    fun encrypt(plainText: String, recipientPublicKeyString: String): String {
        val recipientPublicKeyHandle = stringToKeysetHandle(recipientPublicKeyString)
        val hybridEncrypt = recipientPublicKeyHandle.getPrimitive(HybridEncrypt::class.java)
        val ciphertext = hybridEncrypt.encrypt(plainText.toByteArray(StandardCharsets.UTF_8), null)
        return Base64.encodeToString(ciphertext, Base64.DEFAULT)
    }

    fun decrypt(ciphertextString: String): String {
        val hybridDecrypt = privateKeysetHandle.getPrimitive(HybridDecrypt::class.java)
        val ciphertext = Base64.decode(ciphertextString, Base64.DEFAULT)
        val plainText = hybridDecrypt.decrypt(ciphertext, null)
        return String(plainText, StandardCharsets.UTF_8)
    }

    private fun stringToKeysetHandle(encodedKeyset: String): KeysetHandle {
        val decodedKeyset = Base64.decode(encodedKeyset, Base64.DEFAULT)
        return KeysetHandle.readNoSecret(BinaryKeysetReader.withInputStream(ByteArrayInputStream(decodedKeyset)))
    }
}