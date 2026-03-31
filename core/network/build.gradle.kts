plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.xiaoxiao0301.amberplay.core.network"
    compileSdk = 36
    defaultConfig {
        minSdk = 23
        buildConfigField("String", "MUSIC_API_BASE_URL", "\"https://music-api.gdstudio.xyz/\"")
        // SEC-03/SEC-04: Certificate pins for music-api.gdstudio.xyz
        // Leaf cert obtained via: openssl s_client -connect music-api.gdstudio.xyz:443 ... | openssl dgst -sha256 -binary | base64
        // Refresh these pins when the leaf cert is rotated (check expiry date in CERT_PIN_LEAF_EXPIRY).
        buildConfigField("String", "CERT_PIN_LEAF",          "\"sha256/TazM8eBi89wJfvs7+3h1JlEf3z9TTExU2j9XCJwHReA=\"")
        buildConfigField("String", "CERT_PIN_BACKUP_CA",     "\"sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=\"")
        // Approximate leaf cert expiry — update this date when you refresh CERT_PIN_LEAF
        buildConfigField("String", "CERT_PIN_LEAF_EXPIRY",   "\"2026-11-01\"")
    }
    testOptions { unitTests { all { it.useJUnitPlatform() } } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:datastore"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.timber)
    testImplementation(libs.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
}
