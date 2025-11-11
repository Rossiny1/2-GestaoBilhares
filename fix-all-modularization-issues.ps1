# Script completo para corrigir todos os problemas de modularização
$ErrorActionPreference = "Stop"

Write-Host "=== CORRECAO COMPLETA DE MODULARIZACAO ===" -ForegroundColor Cyan

# PASSO 1: Corrigir estrutura de diretórios do módulo :ui
Write-Host "`n[1/4] Corrigindo estrutura de diretorios do modulo :ui..." -ForegroundColor Yellow
$wrongPath = "ui\src\main\java\com\example\gestaobilhares\ui\rc\main\java\com\example\gestaobilhares\ui"
$correctPath = "ui\src\main\java\com\example\gestaobilhares\ui"

if (Test-Path $wrongPath) {
    # Criar diretório temporário
    $tempPath = "ui\src\main\java\com\example\gestaobilhares\ui\_temp"
    if (Test-Path $tempPath) {
        Remove-Item -Path $tempPath -Recurse -Force
    }
    New-Item -ItemType Directory -Path $tempPath -Force | Out-Null
    
    # Mover conteúdo do caminho errado para temporário
    Get-ChildItem -Path $wrongPath | ForEach-Object {
        Move-Item -Path $_.FullName -Destination $tempPath -Force
    }
    
    # Mover do temporário para o correto
    Get-ChildItem -Path $tempPath | ForEach-Object {
        $dest = Join-Path $correctPath $_.Name
        if (Test-Path $dest) {
            # Se já existe, mesclar conteúdo
            if ($_.PSIsContainer) {
                Get-ChildItem -Path $_.FullName -Recurse | ForEach-Object {
                    $relPath = $_.FullName.Substring($tempPath.Length + $_.Name.Length + 1)
                    $finalDest = Join-Path $dest $relPath
                    $finalDir = Split-Path $finalDest -Parent
                    if (-not (Test-Path $finalDir)) {
                        New-Item -ItemType Directory -Path $finalDir -Force | Out-Null
                    }
                    if (-not $_.PSIsContainer) {
                        Move-Item -Path $_.FullName -Destination $finalDest -Force -ErrorAction SilentlyContinue
                    }
                }
            }
        } else {
            Move-Item -Path $_.FullName -Destination $dest -Force
        }
    }
    
    # Limpar
    Remove-Item -Path $tempPath -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item -Path $wrongPath -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "  Estrutura corrigida!" -ForegroundColor Green
} else {
    Write-Host "  Estrutura ja esta correta" -ForegroundColor DarkGray
}

# PASSO 2: Corrigir imports de gson
Write-Host "`n[2/4] Corrigindo imports de Gson..." -ForegroundColor Yellow
$uiFiles = Get-ChildItem -Path "ui\src\main\java" -Recurse -File -Filter "*.kt" -ErrorAction SilentlyContinue
$gsonFixed = 0
foreach ($file in $uiFiles) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $original = $content
    
    # Corrigir imports de gson
    $content = $content -replace "import com\.example\.gestaobilhares\.gson\.", "import com.google.gson."
    $content = $content -replace "import gson\.", "import com.google.gson."
    
    if ($content -ne $original) {
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
        $gsonFixed++
    }
}
Write-Host "  $gsonFixed arquivos corrigidos" -ForegroundColor Green

# PASSO 3: Corrigir imports de utils
Write-Host "`n[3/4] Corrigindo imports de utils..." -ForegroundColor Yellow
$utilsFixed = 0
foreach ($file in $uiFiles) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $original = $content
    
    # Corrigir imports de utils para core.utils
    $content = $content -replace "import com\.example\.gestaobilhares\.utils\.(AppLogger|DataEncryption|FirebaseStorageManager|DateUtils|StringUtils|PasswordHasher|DataValidator|PaginationManager|SignatureStatistics|ImageCompressionUtils|ReciboPrinterHelper)", "import com.example.gestaobilhares.core.utils.`$1"
    
    if ($content -ne $original) {
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
        $utilsFixed++
    }
}
Write-Host "  $utilsFixed arquivos corrigidos" -ForegroundColor Green

# PASSO 4: Corrigir imports de R e databinding
Write-Host "`n[4/4] Corrigindo imports de R e databinding..." -ForegroundColor Yellow
$rFixed = 0
foreach ($file in $uiFiles) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $original = $content
    
    # Corrigir imports de R - módulo :ui deve usar seu próprio R
    $content = $content -replace "import com\.example\.gestaobilhares\.R", "import com.example.gestaobilhares.ui.R"
    $content = $content -replace "import com\.example\.gestaobilhares\.databinding\.", "import com.example.gestaobilhares.ui.databinding."
    
    # Corrigir imports de BuildConfig
    $content = $content -replace "import com\.example\.gestaobilhares\.BuildConfig", "import com.example.gestaobilhares.ui.BuildConfig"
    
    if ($content -ne $original) {
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
        $rFixed++
    }
}
Write-Host "  $rFixed arquivos corrigidos" -ForegroundColor Green

Write-Host "`n=== CORRECAO CONCLUIDA ===" -ForegroundColor Green

