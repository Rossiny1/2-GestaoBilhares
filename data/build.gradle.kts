plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.gestaobilhares.data"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
    
    buildFeatures {
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.exportSchema", "false")
    arg("room.generateKotlin", "true")
    arg("room.expandProjection", "false")
    arg("ksp.incremental", "true")
    arg("ksp.incremental.intermodule", "true")
    arg("ksp.verify", "false")
    arg("ksp.parallel", "false")
}

dependencies {
    // Módulo Core
    implementation(project(":core"))
    
    // TODO: Refatorar para mover otimizações (cache, memory, network, workers, ui, database) para módulos apropriados
    // Removida dependência circular: :data não pode depender de :app porque :app depende de :data
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
    
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}

