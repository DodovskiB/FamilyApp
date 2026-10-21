import java.util.Properties
import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localConfig = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val projectUrl = localConfig.getProperty("supabase.url", "").trim()
val publicKey = localConfig.getProperty("supabase.publishableKey", "").trim()
require(projectUrl.isEmpty() || projectUrl.matches(Regex("https://[a-z0-9-]+\\.supabase\\.co/?"))) {
    "supabase.url must be the HTTPS project URL."
}
require(publicKey.isEmpty() || publicKey.startsWith("sb_publishable_") || runCatching {
    val payload = String(Base64.getUrlDecoder().decode(publicKey.split('.')[1]))
    Regex("\"role\"\\s*:\\s*\"anon\"").containsMatchIn(payload)
}.getOrDefault(false)) { "Use only the Supabase publishable/anon key, never a secret or service_role key." }
localConfig.setProperty("supabase.url", projectUrl.removeSuffix("/"))
localConfig.setProperty("supabase.publishableKey", publicKey)
fun configString(name: String): String = "\"" +
    (localConfig.getProperty(name) ?: "").replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.example.familyapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.familyapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.1.1-preview"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", configString("supabase.url"))
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", configString("supabase.publishableKey"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Official per-app locale persistence/recreation, including Android 12 and lower.
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation(platform("io.github.jan-tennert.supabase:bom:3.2.6"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.ktor:ktor-client-android:3.3.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.compose.material:material-icons-extended")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
}
