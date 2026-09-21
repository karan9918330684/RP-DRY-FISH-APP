plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android { namespace = "com.rpdryfish.app"; compileSdk = 36
 defaultConfig { applicationId = "com.rpdryfish.app"; minSdk = 23; targetSdk = 36; versionCode = 3; versionName = "3.0" }
 buildFeatures { compose = true }
}
dependencies {
 val bom = platform("androidx.compose:compose-bom:2025.09.00")
 implementation(bom); implementation("androidx.activity:activity-compose:1.10.1"); implementation("androidx.compose.material3:material3"); implementation("androidx.compose.foundation:foundation"); implementation("androidx.compose.ui:ui"); implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2"); implementation("androidx.core:core-ktx:1.17.0")
}
