# Script FINAL para corrigir TODOS os problemas de modularização
$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CORRECAO FINAL DE MODULARIZACAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# ========================================
# PASSO 1: Mover ImageCompressionUtils para :core (ReciboPrinterHelper fica em :app pois usa R)
# ========================================
Write-Host "`n[1/8] Movendo utilitarios para modulo :core..." -ForegroundColor Yellow

# ImageCompressionUtils não usa recursos R, pode ir para :core
$imageSource = "app\src\main\java\com\example\gestaobilhares\utils\ImageCompressionUtils.kt"
$imageDest = "core\src\main\java\com\example\gestaobilhares\core\utils\ImageCompressionUtils.kt"
if (Test-Path $imageSource) {
    if (-not (Test-Path $imageDest)) {
        $destDir = Split-Path $imageDest -Parent
        if (-not (Test-Path $destDir)) {
            New-Item -ItemType Directory -Path $destDir -Force | Out-Null
        }
        Copy-Item -Path $imageSource -Destination $imageDest -Force
        # Atualizar package
        $content = Get-Content $imageDest -Raw -Encoding UTF8
        $content = $content -replace "package com\.example\.gestaobilhares\.utils", "package com.example.gestaobilhares.core.utils"
        Set-Content -Path $imageDest -Value $content -Encoding UTF8 -NoNewline
        Write-Host "  ImageCompressionUtils.kt movido para :core" -ForegroundColor Green
    }
}

# ReciboPrinterHelper usa recursos R do módulo :app, então fica lá ou vai para :ui
# Por enquanto, vamos deixar em :app e ajustar os imports nos arquivos que o usam
Write-Host "  ReciboPrinterHelper.kt permanece em :app (usa recursos R)" -ForegroundColor DarkGray

# ========================================
# PASSO 2: Corrigir estrutura do módulo :ui (caminho correto: ui/c/main/java)
# ========================================
Write-Host "`n[2/8] Corrigindo estrutura do modulo :ui..." -ForegroundColor Yellow
$uiWrongPath = "ui\src\main\java\com\example\gestaobilhares\ui\c\main\java\com\example\gestaobilhares\ui"
$uiCorrectPath = "ui\src\main\java\com\example\gestaobilhares\ui"

if (Test-Path $uiWrongPath) {
    Write-Host "  Movendo arquivos de $uiWrongPath para $uiCorrectPath..." -ForegroundColor Yellow
    Get-ChildItem -Path $uiWrongPath -Recurse -File -ErrorAction SilentlyContinue | ForEach-Object {
        $relPath = $_.FullName.Substring($uiWrongPath.Length + 1)
        $destPath = Join-Path $uiCorrectPath $relPath
        $destDir = Split-Path $destPath -Parent
        
        if (-not (Test-Path $destDir)) {
            New-Item -ItemType Directory -Path $destDir -Force | Out-Null
        }
        
        if (Test-Path $destPath) {
            Remove-Item -Path $destPath -Force -ErrorAction SilentlyContinue
        }
        
        Move-Item -Path $_.FullName -Destination $destPath -Force -ErrorAction SilentlyContinue
    }
    
    # Remover diretório vazio
    Start-Sleep -Seconds 2
    if (Test-Path $uiWrongPath) {
        Remove-Item -Path $uiWrongPath -Recurse -Force -ErrorAction SilentlyContinue
    }
    Write-Host "  Estrutura :ui corrigida!" -ForegroundColor Green
} else {
    Write-Host "  Estrutura :ui ja esta correta" -ForegroundColor DarkGray
}

# ========================================
# PASSO 3: Corrigir estrutura do módulo :sync
# ========================================
Write-Host "`n[3/8] Corrigindo estrutura do modulo :sync..." -ForegroundColor Yellow
$syncWrongPath = "sync\src\main\java\com\example\gestaobilhares\sync\main\java\com\example\gestaobilhares\sync"
$syncCorrectPath = "sync\src\main\java\com\example\gestaobilhares\sync"

$syncManagerSource = "app\src\main\java\com\example\gestaobilhares\sync\SyncManagerV2.kt"
$syncManagerDest = "sync\src\main\java\com\example\gestaobilhares\sync\SyncManagerV2.kt"
if ((Test-Path $syncManagerSource) -and -not (Test-Path $syncManagerDest)) {
    $destDir = Split-Path $syncManagerDest -Parent
    if (-not (Test-Path $destDir)) {
        New-Item -ItemType Directory -Path $destDir -Force | Out-Null
    }
    Copy-Item -Path $syncManagerSource -Destination $syncManagerDest -Force -ErrorAction SilentlyContinue
    Write-Host "  SyncManagerV2.kt copiado para modulo :sync" -ForegroundColor Green
}

if (Test-Path $syncWrongPath) {
    Get-ChildItem -Path $syncWrongPath -Recurse -File -ErrorAction SilentlyContinue | ForEach-Object {
        $relPath = $_.FullName.Substring($syncWrongPath.Length + 1)
        $destPath = Join-Path $syncCorrectPath $relPath
        $destDir = Split-Path $destPath -Parent
        
        if (-not (Test-Path $destDir)) {
            New-Item -ItemType Directory -Path $destDir -Force | Out-Null
        }
        
        if (Test-Path $destPath) {
            Remove-Item -Path $destPath -Force -ErrorAction SilentlyContinue
        }
        
        Move-Item -Path $_.FullName -Destination $destPath -Force -ErrorAction SilentlyContinue
    }
    
    Start-Sleep -Seconds 1
    if (Test-Path $syncWrongPath) {
        Remove-Item -Path $syncWrongPath -Recurse -Force -ErrorAction SilentlyContinue
    }
    Write-Host "  Estrutura :sync corrigida!" -ForegroundColor Green
} else {
    Write-Host "  Estrutura :sync ja esta correta" -ForegroundColor DarkGray
}

# ========================================
# PASSO 4: Obter lista de arquivos Kotlin no módulo :ui (após mover)
# ========================================
Write-Host "`n[4/8] Preparando lista de arquivos para correcao..." -ForegroundColor Yellow
$uiFiles = Get-ChildItem -Path "ui\src\main\java" -Recurse -File -Filter "*.kt" -ErrorAction SilentlyContinue | Where-Object { 
    $_.FullName -notlike "*\c\*" -and $_.FullName -notlike "*\rc\*" 
}
Write-Host "  Encontrados $($uiFiles.Count) arquivos Kotlin" -ForegroundColor Green

# ========================================
# PASSO 5: Corrigir imports de Gson
# ========================================
Write-Host "`n[5/8] Corrigindo imports de Gson..." -ForegroundColor Yellow
$gsonFixed = 0
foreach ($file in $uiFiles) {
    try {
        $content = Get-Content $file.FullName -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
        if ($null -eq $content) { continue }
        $original = $content
        
        $content = $content -replace "import com\.example\.gestaobilhares\.gson\.", "import com.google.gson."
        $content = $content -replace "import gson\.", "import com.google.gson."
        $content = $content -replace "import com\.example\.gestaobilhares\.gson\b", "import com.google.gson"
        
        if ($content -ne $original) {
            Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline -ErrorAction SilentlyContinue
            $gsonFixed++
        }
    } catch {
        # Ignorar erros
    }
}
Write-Host "  $gsonFixed arquivos corrigidos" -ForegroundColor Green

# ========================================
# PASSO 6: Corrigir imports de utils para core.utils
# ========================================
Write-Host "`n[6/8] Corrigindo imports de utils..." -ForegroundColor Yellow
$utilsFixed = 0
foreach ($file in $uiFiles) {
    try {
        $content = Get-Content $file.FullName -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
        if ($null -eq $content) { continue }
        $original = $content
        
        # Corrigir imports de utils para core.utils (lista específica)
        $content = $content -replace "import com\.example\.gestaobilhares\.utils\.(AppLogger|DataEncryption|FirebaseStorageManager|DateUtils|StringUtils|PasswordHasher|DataValidator|PaginationManager|SignatureStatistics|ImageCompressionUtils)", "import com.example.gestaobilhares.core.utils.`$1"
        
        # ReciboPrinterHelper permanece em :app, então corrigir se estiver tentando importar de core.utils
        $content = $content -replace "import com\.example\.gestaobilhares\.core\.utils\.ReciboPrinterHelper", "import com.example.gestaobilhares.utils.ReciboPrinterHelper"
        
        # Corrigir outros imports genéricos de utils para core.utils (mas não ReciboPrinterHelper)
        if ($content -match "import com\.example\.gestaobilhares\.utils\.([A-Za-z]+)") {
            $matches = [regex]::Matches($content, "import com\.example\.gestaobilhares\.utils\.([A-Za-z]+)")
            foreach ($match in $matches) {
                $className = $match.Groups[1].Value
                if ($className -ne "ReciboPrinterHelper") {
                    $content = $content -replace "import com\.example\.gestaobilhares\.utils\.$className", "import com.example.gestaobilhares.core.utils.$className"
                }
            }
        }
        
        if ($content -ne $original) {
            Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline -ErrorAction SilentlyContinue
            $utilsFixed++
        }
    } catch {
        # Ignorar erros
    }
}
Write-Host "  $utilsFixed arquivos corrigidos" -ForegroundColor Green

# ========================================
# PASSO 7: Corrigir imports de R, BuildConfig, databinding e SafeArgs
# ========================================
Write-Host "`n[7/8] Corrigindo imports de R, BuildConfig, databinding e SafeArgs..." -ForegroundColor Yellow
$rFixed = 0
foreach ($file in $uiFiles) {
    try {
        $content = Get-Content $file.FullName -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
        if ($null -eq $content) { continue }
        $original = $content
        
        # Corrigir imports de R
        $content = $content -replace "import com\.example\.gestaobilhares\.R\b", "import com.example.gestaobilhares.ui.R"
        
        # Corrigir imports de databinding
        $content = $content -replace "import com\.example\.gestaobilhares\.databinding\.", "import com.example.gestaobilhares.ui.databinding."
        
        # Corrigir imports de BuildConfig
        $content = $content -replace "import com\.example\.gestaobilhares\.BuildConfig\b", "import com.example.gestaobilhares.ui.BuildConfig"
        
        # Corrigir imports de SafeArgs (Args e Directions)
        $content = $content -replace "import com\.example\.gestaobilhares\.([a-zA-Z]+FragmentArgs)\b", "import com.example.gestaobilhares.ui.`$1"
        $content = $content -replace "import com\.example\.gestaobilhares\.([a-zA-Z]+FragmentDirections)\b", "import com.example.gestaobilhares.ui.`$1"
        
        # Corrigir referências a R.string, R.id, R.layout, R.color, R.drawable (sem import)
        $content = $content -replace "\bR\.(string|id|layout|color|drawable|dimen|style|attr)\b", "com.example.gestaobilhares.ui.R.`$1"
        
        if ($content -ne $original) {
            Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline -ErrorAction SilentlyContinue
            $rFixed++
        }
    } catch {
        # Ignorar erros
    }
}
Write-Host "  $rFixed arquivos corrigidos" -ForegroundColor Green

# ========================================
# PASSO 8: Limpar diretórios vazios e verificar estrutura final
# ========================================
Write-Host "`n[8/8] Limpando e verificando estrutura final..." -ForegroundColor Yellow

# Remover diretórios vazios
$dirsToRemove = @(
    "ui\src\main\java\com\example\gestaobilhares\ui\c",
    "ui\src\main\java\com\example\gestaobilhares\ui\rc"
)
foreach ($dir in $dirsToRemove) {
    if (Test-Path $dir) {
        $files = Get-ChildItem -Path $dir -Recurse -File -ErrorAction SilentlyContinue
        if ($files.Count -eq 0) {
            Remove-Item -Path $dir -Recurse -Force -ErrorAction SilentlyContinue
            Write-Host "  Diretorio vazio removido: $dir" -ForegroundColor Green
        }
    }
}

$uiCount = (Get-ChildItem -Path "ui\src\main\java\com\example\gestaobilhares\ui" -Recurse -File -Filter "*.kt" -ErrorAction SilentlyContinue | Where-Object { 
    $_.FullName -notlike "*\c\*" -and $_.FullName -notlike "*\rc\*" 
}).Count
$syncCount = (Get-ChildItem -Path "sync\src\main\java\com\example\gestaobilhares\sync" -Recurse -File -Filter "*.kt" -ErrorAction SilentlyContinue | Where-Object { 
    $_.FullName -notlike "*\main\java\*" 
}).Count

Write-Host "  Arquivos no modulo :ui: $uiCount" -ForegroundColor Green
Write-Host "  Arquivos no modulo :sync: $syncCount" -ForegroundColor Green

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "CORRECAO FINAL CONCLUIDA!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "`nAgora execute: .\gradlew assembleDebug" -ForegroundColor Cyan

