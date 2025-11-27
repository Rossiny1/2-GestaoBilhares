# Script para atualizar core/build.gradle.kts com dependencias necessarias
Write-Host "=== ATUALIZANDO core/build.gradle.kts ===" -ForegroundColor Cyan

$buildFile = "core\build.gradle.kts"
$content = Get-Content $buildFile -Raw -Encoding UTF8

# Adicionar dependencias necessarias para utils
$newDependencies = @"
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Dependencias para PDF
    implementation("com.itextpdf:kernel:7.1.16")
    implementation("com.itextpdf:io:7.1.16")
    implementation("com.itextpdf:layout:7.1.16")
    
    // Graficos
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Android
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
}
"@

# Substituir a seção de dependencies
$content = $content -replace '(?s)dependencies \{.*?\}', $newDependencies

Set-Content -Path $buildFile -Value $content -Encoding UTF8 -NoNewline
Write-Host "core/build.gradle.kts atualizado" -ForegroundColor Green
Write-Host "=== core/build.gradle.kts ATUALIZADO ===" -ForegroundColor Green

Write-Host "=== ATUALIZANDO core/build.gradle.kts ===" -ForegroundColor Cyan

$buildFile = "core\build.gradle.kts"
$content = Get-Content $buildFile -Raw -Encoding UTF8

# Adicionar dependencias necessarias para utils
$newDependencies = @"
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Dependencias para PDF
    implementation("com.itextpdf:kernel:7.1.16")
    implementation("com.itextpdf:io:7.1.16")
    implementation("com.itextpdf:layout:7.1.16")
    
    // Graficos
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Android
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
}
"@

# Substituir a seção de dependencies
$content = $content -replace '(?s)dependencies \{.*?\}', $newDependencies

Set-Content -Path $buildFile -Value $content -Encoding UTF8 -NoNewline
Write-Host "core/build.gradle.kts atualizado" -ForegroundColor Green
Write-Host "=== core/build.gradle.kts ATUALIZADO ===" -ForegroundColor Green

