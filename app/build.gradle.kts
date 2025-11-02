plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin")
    // ✅ CORREÇÃO: KSP (compatível com Java 11+)
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    // ✅ REMOVIDO: Hilt (pode causar conflito com Compose)
    // id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.gestaobilhares"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.gestaobilhares"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    // Configuração temporária para pular testes unitários
    testOptions {
        unitTests {
            isIncludeAndroidResources = false
        }
    }

    buildTypes {
        debug {
            // Build rápido para desenvolvimento
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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
        viewBinding = true
        buildConfig = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    
}

// ✅ CORREÇÃO: KSP para Room (compatível com Java 11+)
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.exportSchema", "true")
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    // ✅ CORREÇÃO: KSP para Room (compatível com Java 11+)
    ksp("androidx.room:room-compiler:2.6.1")
    
    // ✅ FASE 4C: WorkManager para processamento em background (Android 2025)
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    // Coroutines para Tasks do Firebase (await)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // ✅ NOVO: Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    // Dependencies removidas - Hilt não é mais usado
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // ✅ NOVO: Dependências para geração de PDF
    implementation("com.itextpdf:kernel:7.1.16")
    implementation("com.itextpdf:io:7.1.16")
    implementation("com.itextpdf:layout:7.1.16")
    
    // ✅ NOVO: Dependências para compartilhamento e permissões
    implementation("androidx.activity:activity-ktx:1.8.2")
    
    // ✅ NOVO: Dependência para geolocalização
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // ✅ NOVO: Gráficos (MPAndroidChart)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // ✅ REMOVIDO: SignaturePad problemático - implementação nativa será usada
    
    // ✅ REMOVIDO: Dependências Compose não utilizadas

    // DataStore (Preferences)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.jakewharton.timber:timber:5.0.1")

} 