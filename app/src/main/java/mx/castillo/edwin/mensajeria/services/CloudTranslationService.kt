package mx.castillo.edwin.mensajeria.services

import android.util.Log
        import com.google.gson.Gson
        import kotlinx.coroutines.Dispatchers
        import kotlinx.coroutines.withContext
        import mx.castillo.edwin.mensajeria.BuildConfig
        import java.io.BufferedReader
        import java.io.InputStreamReader
        import java.net.HttpURLConnection
        import java.net.URL
        import java.net.URLEncoder

// Clases de datos para parsear la respuesta JSON de GSON
        data class TranslationResponse(val data: TranslationData)
data class TranslationData(val translations: List<TranslationItem>)
data class TranslationItem(val translatedText: String)

class CloudTranslationService {

    private val apiKey = BuildConfig.TRANSLATION_API_KEY
    private val gson = Gson()
    private val baseUrl = "https://translation.googleapis.com/language/translate/v2"

    /**
     * Traduce un texto a un idioma de destino.
     * Esta es una 'suspend function' para poder llamarla desde una corutina.
     *
     * @param text El texto original a traducir (ej. "Hola").
     * @param targetLanguage El código del idioma destino (ej. "nci" para Náhuatl, "en" para inglés).
     * @return El texto traducido, o null si falla.
     */
    suspend fun translate(text: String, targetLanguage: String): String? {
        // La traducción es una operación de red, la movemos a un hilo de fondo (IO)
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                // 1. Codificar el texto para que sea seguro en una URL
                val encodedText = URLEncoder.encode(text, "UTF-8")

                // 2. Construir la URL completa
                // Usamos la API Key que guardaste en local.properties
                val urlString = "$baseUrl?key=$apiKey&q=$encodedText&target=$targetLanguage"
                val url = URL(urlString)

                // 3. Abrir la conexión
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000 // 5 segundos
                connection.readTimeout = 5000  // 5 segundos

                // 4. Verificar si la respuesta es exitosa (HTTP 200)
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    // 5. Leer la respuesta
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    // 6. Parsear el JSON con Gson
                    val translationResponse = gson.fromJson(response, TranslationResponse::class.java)

                    // 7. Devolver el texto traducido
                    val translatedText = translationResponse.data.translations.firstOrNull()?.translatedText
                    Log.d("CloudTranslationService", "Traducción exitosa: $text -> $translatedText")
                    translatedText
                } else {
                    // 8. Leer la respuesta de error
                    val errorReader = BufferedReader(InputStreamReader(connection.errorStream))
                    val errorResponse = errorReader.readText()
                    errorReader.close()
                    Log.e("CloudTranslationService", "Error de API: ${connection.responseCode} - $errorResponse")
                    null
                }
            } catch (e: Exception) {
                Log.e("CloudTranslationService", "Excepción durante la llamada a la API de translate", e)
                e.printStackTrace()
                null
            } finally {
                connection?.disconnect()
            }
        }
    }
}