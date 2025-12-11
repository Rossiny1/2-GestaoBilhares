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
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    
    // ✅ FIX: Correção para "Filename too long" no Windows
    // Usar diretório de build mais curto
    buildDir = File(rootDir, "b")
    
    // ✅ OTIMIZAÇÃO: Desabilitar tarefas desnecessárias no debug
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
    
    // ✅ OTIMIZAÇÃO: Compilação paralela de módulos
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            freeCompilerArgs += listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-Xjvm-default=all"
            )
        }
    }
    
    // ✅ TESTES: Habilitados para garantir qualidade do código
    // Removido: tasks que desabilitavam testes
}

// ✅ CORREÇÃO: KSP para Room (compatível com Java 11+)
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.exportSchema", "true")
}

dependencies {
    // Modulos
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":ui"))
    implementation(project(":sync"))

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
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    // ✅ CORREÇÃO: Firebase Storage para upload de imagens
    implementation("com.google.firebase:firebase-storage-ktx")
    
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
    
    // ✅ FASE 4: Jetpack Compose Dependencies (Versões Estáveis e Comprovadas)
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.material:material:1.5.4")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // Compose Debug
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")

    // DataStore (Preferences)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // WorkManager (para sincronização em background)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // ✅ REMOVIDO: Hilt (pode causar conflito com Compose)
    // implementation("com.google.dagger:hilt-android:2.51")
    // kapt("com.google.dagger:hilt-android-compiler:2.51")
    
    // ✅ NOVO: Dependências de Teste
    // JUnit 5
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    
    // Mockito para mocks
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    
    // Coroutines Test
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    // Turbine para testar Flows
    testImplementation("app.cash.turbine:turbine:1.0.0")
    
    // Truth para assertions mais legíveis
    testImplementation("com.google.truth:truth:1.1.5")
    
    // Room Testing
    testImplementation("androidx.room:room-testing:2.6.1")
    
    // Core Testing (LiveData testing)
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    
    // AndroidX Test (para testes instrumentados)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
} 
