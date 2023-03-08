plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
}
// TODO: 更新到 2.15 时删除 top.mrxiaom.mirai.aoki.mirai.ProtocolHotfix
val miraiVersion = "2.14.0"

android {
    namespace = "top.mrxiaom.mirai.aoki"
    compileSdk = 32

    defaultConfig {
        applicationId = "top.mrxiaom.mirai.aoki"
        minSdk = 26
        targetSdk = 32
        versionCode = 3
        versionName = "1.1.2"

        buildConfigField("String", "miraiVersion", "\"$miraiVersion\"")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    packagingOptions {
        resources.excludes.add("META-INF/*")
    }
    kotlinOptions.jvmTarget = "1.8"
    buildFeatures.viewBinding = true
}

dependencies {
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.annotation:annotation:1.3.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.4.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.4.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.4.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    implementation("net.mamoe:mirai-core:$miraiVersion")
    implementation("net.mamoe:mirai-core-utils:$miraiVersion")

    implementation("org.bouncycastle:bcprov-jdk15on:1.56")

}