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
        // SEC-03: Certificate pin for music-api.gdstudio.xyz — pinned to the intermediate CA.
        // Pinning the CA (rather than the leaf cert) means no app update is needed when
        // music-api.gdstudio.xyz rotates its leaf certificate (~annually). The CA cert
        // itself changes on a multi-year timescale.
        //
        // To verify / refresh:  openssl s_client -connect music-api.gdstudio.xyz:443 \
        //   -servername music-api.gdstudio.xyz -showcerts 2>/dev/null \
        //   | awk '/-----BEGIN CERTIFICATE-----/{n++} n==2{print}' \
        //   | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der 2>/dev/null \
        //   | openssl dgst -sha256 -binary | base64
        buildConfigField("String", "CERT_PIN_CA", "\"sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=\"")
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
