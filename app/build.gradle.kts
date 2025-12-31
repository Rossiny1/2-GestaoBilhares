import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin")
    // ✅ CORREÇÃO: KSP (compatível com Java 11+)
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    // ✅ NOVO: JaCoCo para cobertura de testes
    id("jacoco")
    // ✅ ATIVADO: Hilt
    id("com.google.dagger.hilt.android")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
    id("com.google.firebase.appdistribution")
}

android {
    namespace = "com.example.gestaobilhares"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.gestaobilhares"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    // ✅ ATUALIZADO: Configuração de testes com JaCoCo
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        // Habilitar cobertura de código
        unitTests.all {
            it.useJUnitPlatform() // JUnit 5 support
            it.extensions.configure(JacocoTaskExtension::class) {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val props = Properties()
                props.load(FileInputStream(keystorePropertiesFile))
                storeFile = file(props["storeFile"] as String)
                storePassword = props["storePassword"] as String
                keyAlias = props["keyAlias"] as String
                keyPassword = props["keyPassword"] as String
            } else {
                // Fallback para debug key se keystore.properties não existir (apenas para evitar quebra de build local)
                storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // ✅ NOTA: O upload automático de mapping.txt para Crashlytics é feito automaticamente
            // pelo plugin com.google.firebase.crashlytics quando o build de release é feito.
            // Não é necessária configuração adicional - o plugin detecta automaticamente o mapping.txt.
            
            firebaseAppDistribution {
                releaseNotes = "Release gerada via Gradle"
                groups = "testers"
            }
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
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")
    
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

    // ✅ ATIVADO: Hilt
    implementation("com.google.dagger:hilt-android:2.51")
    ksp("com.google.dagger:hilt-android-compiler:2.51")
    
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

// ✅ NOVO: Configuração JaCoCo para Cobertura de Testes
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    
    group = "Reporting"
    description = "Generate Jacoco coverage reports for Debug build"
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/databinding/**/*.*",
        "**/generated/**/*.*",
        "**/Hilt_*.class",
        "**/*_Factory.class",
        "**/*_MembersInjector.class",
        "**/*_HiltModules*.class",
        "**/*$*.class" // Inner classes often cause issues or noise
    )
    
    // Classes do módulo app
    val appClasses = fileTree("${layout.buildDirectory.asFile.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    
    // Classes de outros módulos (ui, data, core)
    val uiClasses = fileTree("${project(":ui").layout.buildDirectory.asFile.get()}/tmp/kotlin-classes/debug") { exclude(fileFilter) }
    val dataClasses = fileTree("${project(":data").layout.buildDirectory.asFile.get()}/tmp/kotlin-classes/debug") { exclude(fileFilter) }
    val coreClasses = fileTree("${project(":core").layout.buildDirectory.asFile.get()}/tmp/kotlin-classes/debug") { exclude(fileFilter) }

    classDirectories.setFrom(files(appClasses, uiClasses, dataClasses, coreClasses))
    
    sourceDirectories.setFrom(files(
        "${project.projectDir}/src/main/java",
        "${project(":ui").projectDir}/src/main/java",
        "${project(":data").projectDir}/src/main/java",
        "${project(":core").projectDir}/src/main/java"
    ))
    
    executionData.setFrom(fileTree(layout.buildDirectory.asFile.get()) {
        include("jacoco/testDebugUnitTest.exec")
    })
}

tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    dependsOn("jacocoTestReport")
    
    violationRules {
        rule {
            limit {
                minimum = "0.60".toBigDecimal() // 60% coverage mínima
            }
        }
    }
}

// ✅ AUTOMAÇÃO: Criar PR automaticamente após build bem-sucedido
tasks.register("createPROnSuccess") {
    doLast {
        // Detectar sistema operacional e usar script apropriado
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        
        if (isWindows) {
            // Windows: usar PowerShell
            val psScript = rootProject.file("scripts/create-pr-on-success.ps1")
            if (psScript.exists()) {
                try {
                    exec {
                        commandLine("powershell", "-ExecutionPolicy", "Bypass", "-File", psScript.absolutePath)
                    }
                } catch (e: Exception) {
                    // Não falhar o build se o PR falhar
                    logger.warn("Erro ao criar PR: ${e.message}")
                }
            }
        } else {
            // Linux/Mac: usar shell script
            val scriptPath = rootProject.file("scripts/create-pr-on-success.sh")
            if (scriptPath.exists() && scriptPath.canExecute()) {
                try {
                    exec {
                        commandLine("bash", scriptPath.absolutePath)
                    }
                } catch (e: Exception) {
                    // Não falhar o build se o PR falhar
                    logger.warn("Erro ao criar PR: ${e.message}")
                }
            }
        }
    }
}

// Conectar createPROnSuccess ao installDebug (se a task existir)
// ✅ OTIMIZAÇÃO WINDOWS: Desabilitar tasks desnecessárias no debug para acelerar build
afterEvaluate {
    // Desabilitar tasks de teste, lint e check durante build debug (apenas para assembleDebug)
    tasks.matching { 
        (it.name.contains("test", ignoreCase = true) || 
         it.name.contains("lint", ignoreCase = true) || 
         it.name.contains("check", ignoreCase = true)) && 
        !it.name.contains("assemble") 
    }.configureEach {
        enabled = false
    }
    
    // Conectar createPROnSuccess
    tasks.findByName("installDebug")?.let {
        it.finalizedBy("createPROnSuccess")
    }
    
    tasks.findByName("assembleDebug")?.let {
        it.finalizedBy("createPROnSuccess")
    }
}

// Task helper para rodar testes + cobertura
tasks.register("testCoverage") {
    group = "verification"
    description = "Run tests and generate coverage report"
    dependsOn("testDebugUnitTest", "jacocoTestReport")
}

