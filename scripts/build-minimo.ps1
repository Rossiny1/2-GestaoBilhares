# BUILD MINIMO DE EMERGENCIA
# Estrategia: Criar um APK funcional minimo

Write-Host "BUILD MINIMO DE EMERGENCIA..." -ForegroundColor Red

# 1. DESABILITAR TUDO QUE PODE CAUSAR PROBLEMAS
Write-Host "Desabilitando recursos problematicos..." -ForegroundColor Yellow

# Criar build.gradle.kts temporario simplificado
$buildGradleSimplificado = @"
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
}
"@

# Backup do build.gradle.kts original
Copy-Item "app/build.gradle.kts" "app/build.gradle.kts.backup"

# Aplicar build simplificado
$buildGradleSimplificado | Out-File -FilePath "app/build.gradle.kts" -Encoding UTF8

Write-Host "Build simplificado aplicado" -ForegroundColor Green

# 2. BUILD MINIMO
Write-Host "Executando build minimo..." -ForegroundColor Yellow
./gradlew assembleDebug --no-daemon

if ($LASTEXITCODE -eq 0) {
    Write-Host "BUILD MINIMO SUCESSO!" -ForegroundColor Green
    Write-Host "APK minimo gerado!" -ForegroundColor Green
} else {
    Write-Host "Mesmo o build minimo falhou" -ForegroundColor Red
    Write-Host "Restaurando build original..." -ForegroundColor Yellow
    Copy-Item "app/build.gradle.kts.backup" "app/build.gradle.kts"
}
