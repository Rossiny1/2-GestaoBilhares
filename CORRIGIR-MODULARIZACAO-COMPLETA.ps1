# Script COMPLETO para corrigir TODOS os problemas de modularização
$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CORRECAO COMPLETA DE MODULARIZACAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# ========================================
# PASSO 1: Corrigir estrutura do módulo :ui
# ========================================
Write-Host "`n[1/6] Corrigindo estrutura do modulo :ui..." -ForegroundColor Yellow
$uiWrongPath = "ui\src\main\java\com\example\gestaobilhares\ui\rc\main\java\com\example\gestaobilhares\ui"
$uiCorrectPath = "ui\src\main\java\com\example\gestaobilhares\ui"

if (Test-Path $uiWrongPath) {
    # Mover todos os arquivos recursivamente
    Get-ChildItem -Path $uiWrongPath -Recurse -File | ForEach-Object {
        $relPath = $_.FullName.Substring($uiWrongPath.Length + 1)
        $destPath = Join-Path $uiCorrectPath $relPath
        $destDir = Split-Path $destPath -Parent
        
        if (-not (Test-Path $destDir)) {
            New-Item -ItemType Directory -Path $destDir -Force | Out-Null
        }
        
        if (-not (Test-Path $destPath)) {
            Move-Item -Path $_.FullName -Destination $destPath -Force
        } else {
            # Se já existe, substituir
            Remove-Item -Path $destPath -Force
            Move-Item -Path $_.FullName -Destination $destPath -Force
        }
    }
    
    # Remover diretório vazio
    Start-Sleep -Seconds 1
    if (Test-Path $uiWrongPath) {
        Remove-Item -Path $uiWrongPath -Recurse -Force -ErrorAction SilentlyContinue
    }
    Write-Host "  Estrutura :ui corrigida!" -ForegroundColor Green
} else {
    Write-Host "  Estrutura :ui ja esta correta" -ForegroundColor DarkGray
}

# ========================================
# PASSO 2: Corrigir estrutura do módulo :sync
# ========================================
Write-Host "`n[2/6] Corrigindo estrutura do modulo :sync..." -ForegroundColor Yellow
$syncWrongPath = "sync\src\main\java\com\example\gestaobilhares\sync\main\java\com\example\gestaobilhares\sync"
$syncCorrectPath = "sync\src\main\java\com\example\gestaobilhares\sync"

# Copiar SyncManagerV2.kt do módulo :app se não existir no :sync
$syncManagerSource = "app\src\main\java\com\example\gestaobilhares\sync\SyncManagerV2.kt"
$syncManagerDest = "sync\src\main\java\com\example\gestaobilhares\sync\SyncManagerV2.kt"
if ((Test-Path $syncManagerSource) -and -not (Test-Path $syncManagerDest)) {
    Copy-Item -Path $syncManagerSource -Destination $syncManagerDest -Force -ErrorAction SilentlyContinue
    Write-Host "  SyncManagerV2.kt copiado para modulo :sync" -ForegroundColor Green
}

if (Test-Path $syncWrongPath) {
    # Mover arquivos recursivamente
    Get-ChildItem -Path $syncWrongPath -Recurse -File -ErrorAction SilentlyContinue | ForEach-Object {
        $relPath = $_.FullName.Substring($syncWrongPath.Length + 1)
        $destPath = Join-Path $syncCorrectPath $relPath
        $destDir = Split-Path $destPath -Parent
        
        if (-not (Test-Path $destDir)) {
            New-Item -ItemType Directory -Path $destDir -Force | Out-Null
        }
        
        if (Test-Path $_.FullName) {
            if (Test-Path $destPath) {
                Remove-Item -Path $destPath -Force -ErrorAction SilentlyContinue
            }
            Move-Item -Path $_.FullName -Destination $destPath -Force -ErrorAction SilentlyContinue
        }
    }
    
    # Remover diretórios vazios
    Start-Sleep -Seconds 1
    if (Test-Path $syncWrongPath) {
        Remove-Item -Path $syncWrongPath -Recurse -Force -ErrorAction SilentlyContinue
    }
    Write-Host "  Estrutura :sync corrigida!" -ForegroundColor Green
} else {
    Write-Host "  Estrutura :sync ja esta correta ou nao precisa correcao" -ForegroundColor DarkGray
}

# ========================================
# PASSO 3: Corrigir imports de Gson
# ========================================
Write-Host "`n[3/6] Corrigindo imports de Gson..." -ForegroundColor Yellow
$uiFiles = Get-ChildItem -Path "ui\src\main\java" -Recurse -File -Filter "*.kt" -ErrorAction SilentlyContinue | Where-Object { $_.FullName -notlike "*\rc\*" }
$gsonFixed = 0
foreach ($file in $uiFiles) {
    try {
        $content = Get-Content $file.FullName -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
        if ($null -eq $content) { continue }
        $original = $content
        
        # Corrigir imports de gson
        $content = $content -replace "import com\.example\.gestaobilhares\.gson\.", "import com.google.gson."
        $content = $content -replace "import gson\.", "import com.google.gson."
        $content = $content -replace "import com\.example\.gestaobilhares\.gson\b", "import com.google.gson"
        
        if ($content -ne $original) {
            Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline -ErrorAction SilentlyContinue
            $gsonFixed++
        }
    } catch {
        # Ignorar erros de leitura
    }
}
Write-Host "  $gsonFixed arquivos corrigidos" -ForegroundColor Green

# ========================================
# PASSO 4: Corrigir imports de utils para core.utils
# ========================================
Write-Host "`n[4/6] Corrigindo imports de utils..." -ForegroundColor Yellow
$utilsFixed = 0
foreach ($file in $uiFiles) {
    try {
        $content = Get-Content $file.FullName -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
        if ($null -eq $content) { continue }
        $original = $content
        
        # Corrigir imports de utils para core.utils
        $content = $content -replace "import com\.example\.gestaobilhares\.utils\.(AppLogger|DataEncryption|FirebaseStorageManager|DateUtils|StringUtils|PasswordHasher|DataValidator|PaginationManager|SignatureStatistics|ImageCompressionUtils|ReciboPrinterHelper)", "import com.example.gestaobilhares.core.utils.`$1"
        
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
# PASSO 5: Corrigir imports de R, BuildConfig e databinding
# ========================================
Write-Host "`n[5/6] Corrigindo imports de R, BuildConfig e databinding..." -ForegroundColor Yellow
$rFixed = 0
foreach ($file in $uiFiles) {
    try {
        $content = Get-Content $file.FullName -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
        if ($null -eq $content) { continue }
        $original = $content
        
        # Corrigir imports de R - módulo :ui deve usar seu próprio R
        $content = $content -replace "import com\.example\.gestaobilhares\.R\b", "import com.example.gestaobilhares.ui.R"
        $content = $content -replace "import com\.example\.gestaobilhares\.databinding\.", "import com.example.gestaobilhares.ui.databinding."
        
        # Corrigir imports de BuildConfig
        $content = $content -replace "import com\.example\.gestaobilhares\.BuildConfig\b", "import com.example.gestaobilhares.ui.BuildConfig"
        
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
# PASSO 6: Verificar estrutura final e limpar diretórios vazios
# ========================================
Write-Host "`n[6/6] Verificando estrutura final..." -ForegroundColor Yellow

# Remover diretório rc vazio se existir
$rcPath = "ui\src\main\java\com\example\gestaobilhares\ui\rc"
if (Test-Path $rcPath) {
    $filesInRc = Get-ChildItem -Path $rcPath -Recurse -File -ErrorAction SilentlyContinue
    if ($filesInRc.Count -eq 0) {
        Remove-Item -Path $rcPath -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "  Diretorio 'rc' vazio removido" -ForegroundColor Green
    }
}

$uiCount = (Get-ChildItem -Path "ui\src\main\java\com\example\gestaobilhares\ui" -Recurse -File -Filter "*.kt" -ErrorAction SilentlyContinue | Where-Object { $_.FullName -notlike "*\rc\*" }).Count
$syncCount = (Get-ChildItem -Path "sync\src\main\java\com\example\gestaobilhares\sync" -Recurse -File -Filter "*.kt" -ErrorAction SilentlyContinue | Where-Object { $_.FullName -notlike "*\main\java\*" }).Count

Write-Host "  Arquivos no modulo :ui: $uiCount" -ForegroundColor Green
Write-Host "  Arquivos no modulo :sync: $syncCount" -ForegroundColor Green

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "CORRECAO CONCLUIDA COM SUCESSO!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "`nAgora execute: .\gradlew assembleDebug" -ForegroundColor Cyan

