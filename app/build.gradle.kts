import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    // [NUEVO] Plugin para reportes de errores
    id("com.google.firebase.crashlytics")
}

// AÑADE ESTAS LÍNEAS PARA CARGAR TUS PROPIEDADES LOCALES
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val requireSecrets = gradle.startParameter.taskNames.any { taskName ->
    val normalizedTask = taskName.substringAfterLast(":").lowercase()
    listOf("assemble", "bundle", "build", "compile", "connected", "install", "lint", "package", "test").any {
        normalizedTask.startsWith(it)
    }
}
/**
 * Carga un secreto requerido desde local.properties (prioridad) o variables de entorno.
 * La validación solo aplica cuando se ejecutan tareas de build/lint/test/assemble/bundle/package.
 */
fun requiredSecret(name: String): String {
    val value = localProperties.getProperty(name)?.trim().orEmpty()
        .ifBlank { System.getenv(name)?.trim().orEmpty() }
    if (requireSecrets && value.isBlank()) {
        throw GradleException(
            "Falta el secreto requerido $name. Configúralo en ./local.properties o en variables de entorno."
        )
    }
    return value
}

val translationApiKey = requiredSecret("TRANSLATION_API_KEY")
val facebookAppId = requiredSecret("FACEBOOK_APP_ID")
val facebookClientToken = requiredSecret("FACEBOOK_CLIENT_TOKEN")
val facebookLoginScheme = "fb$facebookAppId"

android {
    namespace = "mx.castillo.edwin.mensajeria"
    compileSdk = 36

    defaultConfig {
        applicationId = "mx.castillo.edwin.mensajeria"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        resValue("string", "facebook_app_id", facebookAppId)
        resValue("string", "facebook_client_token", facebookClientToken)
        resValue("string", "fb_login_protocol_scheme", facebookLoginScheme)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // SINTAXIS KOTLIN DSL CORRECTA:
            buildConfigField(
                type = "String",
                name = "TRANSLATION_API_KEY",
                value = "\"$translationApiKey\""
            )
        }

        debug {
            // SINTAXIS KOTLIN DSL CORRECTA:
            buildConfigField(
                type = "String",
                name = "TRANSLATION_API_KEY",
                value = "\"$translationApiKey\""
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    //Firebase BOM (Gestiona las versiones)
    implementation(platform("com.google.firebase:firebase-bom:34.2.0"))

    // [EXISTENTE] Analytics
    implementation("com.google.firebase:firebase-analytics")

    // [NUEVO] Crashlytics (Reporte de fallos)
    implementation("com.google.firebase:firebase-crashlytics")

    // [NUEVO] Remote Config (Para el "Kill Switch" / Apagado remoto)
    implementation("com.google.firebase:firebase-config")

    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    //Firebase Realtime
    implementation("com.google.firebase:firebase-database")

    //Fireabse Storage
    implementation("com.google.firebase:firebase-storage")

    //Firebase Messaging
    implementation("com.google.firebase:firebase-messaging")

    implementation("com.google.firebase:firebase-appcheck")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")

    //Google think
    implementation("com.google.crypto.tink:tink-android:1.14.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Google Sign-In para Compose
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // SplashScreen API (Android 12+)
    implementation("androidx.core:core-splashscreen:1.0.1")

    //Json
    implementation("com.google.code.gson:gson:2.10.1")

    //Coil
    implementation("io.coil-kt:coil-compose:2.4.0")

    //SDK Facebook
    implementation("com.facebook.android:facebook-login:17.0.0")

    // Íconos extendidos
    implementation("androidx.compose.material:material-icons-extended-android:1.6.8")

    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
    implementation(libs.car.ui.lib)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
