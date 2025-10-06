# 🚨 BUILD MÍNIMO DE EMERGÊNCIA
# Estratégia: Criar um APK funcional mínimo

Write-Host "🚨 BUILD MÍNIMO DE EMERGÊNCIA..." -ForegroundColor Red

# 1. DESABILITAR TUDO QUE PODE CAUSAR PROBLEMAS
Write-Host "🔧 Desabilitando recursos problemáticos..." -ForegroundColor Yellow

# Criar build.gradle.kts temporário simplificado
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

Write-Host "📝 Build simplificado aplicado" -ForegroundColor Green

# 2. BUILD MÍNIMO
Write-Host "🔨 Executando build mínimo..." -ForegroundColor Yellow
./gradlew assembleDebug --no-daemon

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ BUILD MÍNIMO SUCESSO!" -ForegroundColor Green
    Write-Host "📱 APK mínimo gerado!" -ForegroundColor Green
} else {
    Write-Host "❌ Mesmo o build mínimo falhou" -ForegroundColor Red
    Write-Host "🔧 Restaurando build original..." -ForegroundColor Yellow
    Copy-Item "app/build.gradle.kts.backup" "app/build.gradle.kts"
}
