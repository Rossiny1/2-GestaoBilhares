# verificar-e-remover-duplicacoes.ps1
# Script completo para verificar e remover duplicacoes e arquivos desnecessarios

$ErrorActionPreference = "Continue"
$rootPath = $PSScriptRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICACAO DE DUPLICACOES E ARQUIVOS" -ForegroundColor Cyan
Write-Host "DESNECESSARIOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ==================== 1. ARQUIVOS DE BACKUP/TEMPORARIOS ====================
Write-Host "[1/6] Verificando arquivos de backup/temporarios..." -ForegroundColor Yellow

$backupFiles = @()
$backupFiles += Get-ChildItem -Path $rootPath -Recurse -Include "*.backup", "*.bak", "*.old", "*.tmp" -File -ErrorAction SilentlyContinue | Where-Object { $_.FullName -notmatch "\\build\\" -and $_.FullName -notmatch "\\.git\\" }

if ($backupFiles.Count -gt 0) {
    Write-Host "  Encontrados $($backupFiles.Count) arquivos de backup/temporarios:" -ForegroundColor Yellow
    foreach ($file in $backupFiles) {
        $relativePath = $file.FullName.Replace($rootPath + "\", "")
        Write-Host "    - $relativePath" -ForegroundColor Gray
    }
} else {
    Write-Host "  [OK] Nenhum arquivo de backup encontrado" -ForegroundColor Green
}

# ==================== 2. ANDROIDMANIFEST DUPLICADO ====================
Write-Host ""
Write-Host "[2/6] Verificando AndroidManifest duplicado..." -ForegroundColor Yellow

$manifest1 = Join-Path $rootPath "app\src\main\AndroidManifest.xml"
$manifest2 = Join-Path $rootPath "app\src\main\AndroidManifest (1).xml"

if (Test-Path $manifest2) {
    Write-Host "  [ATENCAO] AndroidManifest (1).xml encontrado (duplicado)" -ForegroundColor Yellow
    
    if (Test-Path $manifest1) {
        $hash1 = (Get-FileHash -Path $manifest1 -Algorithm MD5 -ErrorAction SilentlyContinue).Hash
        $hash2 = (Get-FileHash -Path $manifest2 -Algorithm MD5 -ErrorAction SilentlyContinue).Hash
        
        if ($hash1 -eq $hash2) {
            Write-Host "  [OK] Arquivos sao identicos - seguro remover duplicado" -ForegroundColor Green
        } else {
            Write-Host "  [ATENCAO] Arquivos sao diferentes - verificar manualmente" -ForegroundColor Red
        }
    }
} else {
    Write-Host "  [OK] Nenhum AndroidManifest duplicado encontrado" -ForegroundColor Green
}

# ==================== 3. DIRETORIOS VAZIOS ====================
Write-Host ""
Write-Host "[3/6] Verificando diretorios vazios..." -ForegroundColor Yellow

$emptyDirs = @()
$directories = Get-ChildItem -Path $rootPath -Recurse -Directory -ErrorAction SilentlyContinue | Where-Object { 
    $_.FullName -notmatch "\\build\\" -and 
    $_.FullName -notmatch "\\.git\\" -and
    $_.FullName -notmatch "\\.gradle\\" -and
    $_.FullName -notmatch "\\node_modules\\"
}

foreach ($dir in $directories) {
    $files = Get-ChildItem -Path $dir.FullName -File -Recurse -ErrorAction SilentlyContinue
    if ($files.Count -eq 0) {
        $emptyDirs += $dir
    }
}

if ($emptyDirs.Count -gt 0) {
    Write-Host "  Encontrados $($emptyDirs.Count) diretorios vazios:" -ForegroundColor Yellow
    foreach ($dir in $emptyDirs) {
        $relativePath = $dir.FullName.Replace($rootPath + "\", "")
        Write-Host "    - $relativePath" -ForegroundColor Gray
    }
} else {
    Write-Host "  [OK] Nenhum diretorio vazio encontrado" -ForegroundColor Green
}

# ==================== 4. LAYOUTS DUPLICADOS ENTRE APP E UI ====================
Write-Host ""
Write-Host "[4/6] Verificando layouts duplicados entre app e ui..." -ForegroundColor Yellow

$appLayouts = @()
$uiLayouts = @()

$appLayoutPath = Join-Path $rootPath "app\src\main\res\layout"
$uiLayoutPath = Join-Path $rootPath "ui\src\main\res\layout"

if (Test-Path $appLayoutPath) {
    $appLayouts = Get-ChildItem -Path $appLayoutPath -Filter "*.xml" -File -ErrorAction SilentlyContinue | ForEach-Object { $_.Name }
}

if (Test-Path $uiLayoutPath) {
    $uiLayouts = Get-ChildItem -Path $uiLayoutPath -Filter "*.xml" -File -ErrorAction SilentlyContinue | ForEach-Object { $_.Name }
}

$duplicateLayouts = $appLayouts | Where-Object { $uiLayouts -contains $_ }

if ($duplicateLayouts.Count -gt 0) {
    Write-Host "  Encontrados $($duplicateLayouts.Count) layouts duplicados:" -ForegroundColor Yellow
    foreach ($layout in $duplicateLayouts) {
        Write-Host "    - $layout" -ForegroundColor Gray
    }
} else {
    Write-Host "  [OK] Nenhum layout duplicado encontrado" -ForegroundColor Green
}

# ==================== 5. CODIGO KOTLIN DUPLICADO ====================
Write-Host ""
Write-Host "[5/6] Verificando codigo Kotlin duplicado..." -ForegroundColor Yellow

$appKotlinFiles = @()
$uiKotlinFiles = @()

$appKotlinPath = Join-Path $rootPath "app\src\main\java"
$uiKotlinPath = Join-Path $rootPath "ui\src\main\java"

if (Test-Path $appKotlinPath) {
    $appKotlinFiles = Get-ChildItem -Path $appKotlinPath -Filter "*.kt" -Recurse -File -ErrorAction SilentlyContinue | ForEach-Object { 
        $_.FullName.Replace($appKotlinPath + "\", "").Replace("\", "/")
    }
}

if (Test-Path $uiKotlinPath) {
    $uiKotlinFiles = Get-ChildItem -Path $uiKotlinPath -Filter "*.kt" -Recurse -File -ErrorAction SilentlyContinue | ForEach-Object { 
        $_.FullName.Replace($uiKotlinPath + "\", "").Replace("\", "/")
    }
}

# Verificar se há arquivos com mesmo nome e caminho relativo
$duplicateKotlin = $appKotlinFiles | Where-Object { $uiKotlinFiles -contains $_ }

if ($duplicateKotlin.Count -gt 0) {
    Write-Host "  Encontrados $($duplicateKotlin.Count) arquivos Kotlin duplicados:" -ForegroundColor Yellow
    foreach ($file in $duplicateKotlin) {
        Write-Host "    - $file" -ForegroundColor Gray
    }
} else {
    Write-Host "  [OK] Nenhum arquivo Kotlin duplicado encontrado" -ForegroundColor Green
}

# ==================== 6. MENUS DUPLICADOS ====================
Write-Host ""
Write-Host "[6/6] Verificando menus duplicados..." -ForegroundColor Yellow

$appMenus = @()
$uiMenus = @()

$appMenuPath = Join-Path $rootPath "app\src\main\res\menu"
$uiMenuPath = Join-Path $rootPath "ui\src\main\res\menu"

if (Test-Path $appMenuPath) {
    $appMenus = Get-ChildItem -Path $appMenuPath -Filter "*.xml" -File -ErrorAction SilentlyContinue | ForEach-Object { $_.Name }
}

if (Test-Path $uiMenuPath) {
    $uiMenus = Get-ChildItem -Path $uiMenuPath -Filter "*.xml" -File -ErrorAction SilentlyContinue | ForEach-Object { $_.Name }
}

$duplicateMenus = $appMenus | Where-Object { $uiMenus -contains $_ }

if ($duplicateMenus.Count -gt 0) {
    Write-Host "  Encontrados $($duplicateMenus.Count) menus duplicados:" -ForegroundColor Yellow
    foreach ($menu in $duplicateMenus) {
        Write-Host "    - $menu" -ForegroundColor Gray
    }
} else {
    Write-Host "  [OK] Nenhum menu duplicado encontrado" -ForegroundColor Green
}

# ==================== RESUMO ====================
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$totalIssues = $backupFiles.Count + $(if (Test-Path $manifest2) { 1 } else { 0 }) + $emptyDirs.Count + $duplicateLayouts.Count + $duplicateKotlin.Count + $duplicateMenus.Count

if ($totalIssues -eq 0) {
    Write-Host "[OK] Nenhuma duplicacao ou arquivo desnecessario encontrado!" -ForegroundColor Green
} else {
    Write-Host "Total de problemas encontrados: $totalIssues" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Para remover automaticamente, execute:" -ForegroundColor Cyan
    Write-Host "  .\verificar-e-remover-duplicacoes.ps1 -Remove" -ForegroundColor White
}

Write-Host ""

# ==================== REMOCAO AUTOMATICA (se solicitado) ====================
if ($args -contains "-Remove" -or $args -contains "-remove") {
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "REMOCAO AUTOMATICA" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    $removedCount = 0
    
    # Remover arquivos de backup
    foreach ($file in $backupFiles) {
        try {
            Remove-Item -Path $file.FullName -Force -ErrorAction Stop
            Write-Host "  [OK] Removido: $($file.Name)" -ForegroundColor Green
            $removedCount++
        } catch {
            Write-Host "  [ERRO] Falha ao remover $($file.Name) - ${_}" -ForegroundColor Red
        }
    }
    
    # Remover AndroidManifest duplicado (se identico)
    if (Test-Path $manifest2) {
        if (Test-Path $manifest1) {
            $hash1 = (Get-FileHash -Path $manifest1 -Algorithm MD5 -ErrorAction SilentlyContinue).Hash
            $hash2 = (Get-FileHash -Path $manifest2 -Algorithm MD5 -ErrorAction SilentlyContinue).Hash
            
            if ($hash1 -eq $hash2) {
                try {
                    Remove-Item -Path $manifest2 -Force -ErrorAction Stop
                    Write-Host "  [OK] Removido: AndroidManifest (1).xml (duplicado identico)" -ForegroundColor Green
                    $removedCount++
                } catch {
                    Write-Host "  [ERRO] Falha ao remover AndroidManifest (1).xml - ${_}" -ForegroundColor Red
                }
            } else {
                Write-Host "  [ATENCAO] AndroidManifest (1).xml nao removido (arquivos diferentes)" -ForegroundColor Yellow
            }
        }
    }
    
    # Remover diretorios vazios (exceto estrutura necessaria)
    # Ordenar do mais profundo para o mais raso para evitar erros
    $sortedDirs = $emptyDirs | Sort-Object { $_.FullName.Split('\').Count } -Descending
    
    foreach ($dir in $sortedDirs) {
        # Verificar se o diretorio ainda existe (pode ter sido removido como subdiretorio)
        if (-not (Test-Path $dir.FullName)) {
            continue
        }
        
        $relativePath = $dir.FullName.Replace($rootPath + "\", "")
        # Nao remover diretorios que podem ser necessarios para estrutura
        if ($relativePath -notmatch "\\factory\\?$" -and $relativePath -notmatch "\\res\\layout\\?$") {
            try {
                # Verificar se realmente esta vazio (sem arquivos)
                $hasFiles = Get-ChildItem -Path $dir.FullName -Recurse -File -ErrorAction SilentlyContinue
                if ($hasFiles.Count -eq 0) {
                    Remove-Item -Path $dir.FullName -Recurse -Force -ErrorAction Stop
                    Write-Host "  [OK] Removido diretorio vazio: $relativePath" -ForegroundColor Green
                    $removedCount++
                } else {
                    Write-Host "  [ATENCAO] Diretorio nao esta vazio: $relativePath (mantido)" -ForegroundColor Yellow
                }
            } catch {
                # Ignorar erro se o diretorio ja foi removido
                if (Test-Path $dir.FullName) {
                    Write-Host "  [ERRO] Falha ao remover $relativePath - ${_}" -ForegroundColor Red
                }
            }
        }
    }
    
    Write-Host ""
    Write-Host "Total de itens removidos: $removedCount" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICACAO CONCLUIDA" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

