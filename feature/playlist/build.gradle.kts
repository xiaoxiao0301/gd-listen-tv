plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt)
}
android {
    namespace = "com.xiaoxiao0301.amberplay.feature.playlist"
    compileSdk = 36; defaultConfig { minSdk = 23 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
    kotlinOptions { jvmTarget = "11" }; buildFeatures { compose = true }
    testOptions { unitTests { all { it.useJUnitPlatform() } } }
}
dependencies {
    implementation(project(":domain")); implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:media"))
    implementation(project(":core:datastore"))
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui); implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android); implementation(libs.hilt.navigation.compose); ksp(libs.hilt.compiler)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
