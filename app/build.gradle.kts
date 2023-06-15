@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
}
val miraiVersion = "2.15.0-dev-98"

android {
    namespace = "top.mrxiaom.mirai.aoki"
    compileSdk = 33

    defaultConfig {
        applicationId = "top.mrxiaom.mirai.aoki"
        minSdk = 26
        targetSdk = 33
        versionCode = 8
        versionName = "1.2.1-pre2"

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
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.annotation:annotation:1.6.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.0")
    implementation("androidx.preference:preference:1.2.0")
    implementation("androidx.preference:preference-ktx:1.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    implementation("net.mamoe:mirai-core:$miraiVersion")
    implementation("net.mamoe:mirai-core-utils:$miraiVersion")

    implementation("com.youbenzi:MDTool:1.2.4")
    implementation(project(":tlv544_enc"))

    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
}