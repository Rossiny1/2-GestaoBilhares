plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.gestaobilhares.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Modulos
    implementation(project(":data"))
    // Removido: implementation(project(":sync")) - causa dependência circular
    
    // Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // Room (para acesso a AppDatabase)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    
    // Firebase (para RepositoryFactory)
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Dependencias para PDF
    implementation("com.itextpdf:kernel:7.1.16")
    implementation("com.itextpdf:io:7.1.16")
    implementation("com.itextpdf:layout:7.1.16")
    
    // Graficos
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    
    // ✅ NOVO: EncryptedSharedPreferences para segurança de dados sensíveis
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Timber
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51")
    ksp("com.google.dagger:hilt-android-compiler:2.51")
    // Testes
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

