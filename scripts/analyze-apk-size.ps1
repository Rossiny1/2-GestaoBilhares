# ✅ FASE 12.10: Script de Análise de Tamanho do APK
# Analisa o tamanho do APK e identifica oportunidades de otimização

param(
    [string]$ApkPath = "app\build\outputs\apk\debug\app-debug.apk"
)

Write-Host "=== ANALISE DE TAMANHO DO APK ===" -ForegroundColor Cyan
Write-Host ""

# Verificar se o APK existe
if (-not (Test-Path $ApkPath)) {
    Write-Host "ERRO: APK nao encontrado em: $ApkPath" -ForegroundColor Red
    Write-Host "Execute o build primeiro: ./gradlew assembleDebug" -ForegroundColor Yellow
    exit 1
}

# Obter tamanho do APK
$apkFile = Get-Item $ApkPath
$apkSizeMB = [math]::Round($apkFile.Length / 1MB, 2)
$apkSizeKB = [math]::Round($apkFile.Length / 1KB, 2)

Write-Host "Tamanho do APK: $apkSizeMB MB ($apkSizeKB KB)" -ForegroundColor Green
Write-Host ""

# Verificar se bundletool está disponível (para análise detalhada)
$bundletoolAvailable = $false
try {
    $bundletoolVersion = java -jar bundletool.jar --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        $bundletoolAvailable = $true
    }
} catch {
    $bundletoolAvailable = $false
}

if (-not $bundletoolAvailable) {
    Write-Host "INFO: bundletool nao encontrado. Analise basica apenas." -ForegroundColor Yellow
    Write-Host ""
}

# Análise de recursos
Write-Host "=== ANALISE DE RECURSOS ===" -ForegroundColor Cyan

# Verificar tamanho de imagens
$resDir = "app\src\main\res"
if (Test-Path $resDir) {
    $imageDirs = @("drawable", "drawable-hdpi", "drawable-xhdpi", "drawable-xxhdpi", "drawable-xxxhdpi", "mipmap", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi")
    
    $totalImageSize = 0
    $imageCount = 0
    
    foreach ($dir in $imageDirs) {
        $fullPath = Join-Path $resDir $dir
        if (Test-Path $fullPath) {
            $images = Get-ChildItem -Path $fullPath -Include *.png,*.jpg,*.jpeg,*.webp -Recurse -ErrorAction SilentlyContinue
            foreach ($img in $images) {
                $totalImageSize += $img.Length
                $imageCount++
            }
        }
    }
    
    if ($imageCount -gt 0) {
        $imageSizeMB = [math]::Round($totalImageSize / 1MB, 2)
        Write-Host "Imagens encontradas: $imageCount" -ForegroundColor White
        Write-Host "Tamanho total das imagens: $imageSizeMB MB" -ForegroundColor White
        Write-Host ""
        
        if ($imageSizeMB -gt 5) {
            Write-Host "AVISO: Tamanho de imagens muito grande ($imageSizeMB MB)" -ForegroundColor Yellow
            Write-Host "Recomendacao: Comprimir imagens ou usar WebP" -ForegroundColor Yellow
            Write-Host ""
        }
    }
}

# Verificar tamanho de bibliotecas
Write-Host "=== ANALISE DE DEPENDENCIAS ===" -ForegroundColor Cyan

$gradleFile = "app\build.gradle.kts"
if (Test-Path $gradleFile) {
    $content = Get-Content $gradleFile -Raw
    
    # Contar dependências
    $implementationCount = ([regex]::Matches($content, "implementation\(")).Count
    $testImplementationCount = ([regex]::Matches($content, "testImplementation\(")).Count
    $androidTestImplementationCount = ([regex]::Matches($content, "androidTestImplementation\(")).Count
    
    Write-Host "Dependencias de implementacao: $implementationCount" -ForegroundColor White
    Write-Host "Dependencias de teste: $testImplementationCount" -ForegroundColor White
    Write-Host "Dependencias de teste Android: $androidTestImplementationCount" -ForegroundColor White
    Write-Host ""
    
    if ($implementationCount -gt 30) {
        Write-Host "AVISO: Muitas dependencias ($implementationCount)" -ForegroundColor Yellow
        Write-Host "Recomendacao: Revisar dependencias desnecessarias" -ForegroundColor Yellow
        Write-Host ""
    }
}

# Recomendações
Write-Host "=== RECOMENDACOES ===" -ForegroundColor Cyan

$recommendations = @()

if ($apkSizeMB -gt 50) {
    $recommendations += "APK muito grande ($apkSizeMB MB). Considere usar Android App Bundle (AAB)"
}

if ($apkSizeMB -gt 30) {
    $recommendations += "Habilitar ProGuard/R8 para reduzir tamanho"
    $recommendations += "Remover recursos não utilizados (shrinkResources)"
}

if ($recommendations.Count -eq 0) {
    Write-Host "Tamanho do APK esta dentro do esperado!" -ForegroundColor Green
} else {
    foreach ($rec in $recommendations) {
        Write-Host "- $rec" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "=== ANALISE CONCLUIDA ===" -ForegroundColor Cyan

