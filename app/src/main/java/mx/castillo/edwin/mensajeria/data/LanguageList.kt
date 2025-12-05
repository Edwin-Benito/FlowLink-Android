package mx.castillo.edwin.mensajeria.data

// Un data class simple para representar un idioma
data class Language(
    val code: String, // ej. "en"
    val name: String,  // ej. "English"
    val nativeName: String
)


val supportedLanguages = listOf(
    Language("es", "Spanish", "Español"),
    Language("en", "English", "English"),
    Language("fr", "French", "Français"),
    Language("de", "German", "Deutsch"),
    Language("it", "Italian", "Italiano"),
    Language("pt", "Portuguese", "Português"),
    Language("ja", "Japanese", "日本語"),
    Language("ko", "Korean", "한국어"),
    Language("zh-CN", "Chinese (Simplified)", "中文 (简体)"),
    Language("ru", "Russian", "Русский"),
    Language("ar", "Arabic", "العربية"),
    Language("hi", "Hindi", "हिन्दी"),
    Language("nl", "Dutch", "Nederlands"),
    Language("sv", "Swedish", "Svenska"),
    Language("tr", "Turkish", "Türkçe"),
    Language("pl", "Polish", "Polski"),
    Language("vi", "Vietnamese", "Tiếng Việt"),
    Language("th", "Thai", "ไทย"),
    Language("el", "Greek", "Ελληνικά"),
    Language("uk", "Ukrainian", "Українська")
    //Language("nah", "Náhuatl de la Huasteca Oriental","náhuatl")

)