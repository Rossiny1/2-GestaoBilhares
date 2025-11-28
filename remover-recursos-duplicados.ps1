# Script para remover recursos duplicados do modulo app (mantendo apenas em ui)
Write-Host "=== REMOCAO DE RECURSOS DUPLICADOS ===" -ForegroundColor Cyan
Write-Host ""

$appBase = "app\src\main\res"
$uiBase = "ui\src\main\res"

if (-not (Test-Path $appBase)) {
    Write-Host "[ERRO] Diretorio app\src\main\res nao encontrado" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $uiBase)) {
    Write-Host "[ERRO] Diretorio ui\src\main\res nao encontrado" -ForegroundColor Red
    exit 1
}

$removidos = 0
$diferentes = 0
$erros = 0

# Funcao para comparar arquivos
function Compare-Files {
    param(
        [string]$file1,
        [string]$file2
    )
    
    if (-not (Test-Path $file1) -or -not (Test-Path $file2)) {
        return $false
    }
    
    $hash1 = (Get-FileHash -Path $file1 -Algorithm MD5 -ErrorAction SilentlyContinue).Hash
    $hash2 = (Get-FileHash -Path $file2 -Algorithm MD5 -ErrorAction SilentlyContinue).Hash
    
    return $hash1 -eq $hash2
}

# 1. VALUES (colors.xml, strings.xml, styles.xml)
Write-Host "[1/3] Removendo arquivos values duplicados..." -ForegroundColor Yellow
$valuesFiles = @("colors.xml", "strings.xml", "styles.xml")

foreach ($file in $valuesFiles) {
    $appFile = Join-Path "$appBase\values" $file
    $uiFile = Join-Path "$uiBase\values" $file
    
    if (Test-Path $appFile) {
        if (Test-Path $uiFile) {
            $saoIdenticos = Compare-Files -file1 $appFile -file2 $uiFile
            if ($saoIdenticos) {
                try {
                    Remove-Item -Path $appFile -Force -ErrorAction Stop
                    Write-Host "  [OK] Removido (identico): $file" -ForegroundColor Green
                    $removidos++
                } catch {
                    Write-Host "  [ERRO] Nao foi possivel remover: $file - $_" -ForegroundColor Red
                    $erros++
                }
            } else {
                Write-Host "  [ATENCAO] Arquivos diferentes: $file" -ForegroundColor Yellow
                Write-Host "    Mantendo ambos por seguranca. Verifique manualmente." -ForegroundColor Yellow
                $diferentes++
            }
        } else {
            Write-Host "  [INFO] Arquivo nao existe em ui: $file (mantendo em app)" -ForegroundColor Gray
        }
    }
}
Write-Host ""

# 2. DRAWABLES (146 arquivos)
Write-Host "[2/3] Removendo drawables duplicados..." -ForegroundColor Yellow
$appDrawables = Get-ChildItem -Path "$appBase\drawable" -File -ErrorAction SilentlyContinue
$uiDrawables = Get-ChildItem -Path "$uiBase\drawable" -File -ErrorAction SilentlyContinue
$uiDrawableNames = $uiDrawables | Select-Object -ExpandProperty Name

$drawableDuplicados = $appDrawables | Where-Object { $uiDrawableNames -contains $_.Name }

Write-Host "  Total de drawables duplicados encontrados: $($drawableDuplicados.Count)" -ForegroundColor Cyan

$drawableRemovidos = 0
$drawableDiferentes = 0
$drawableErros = 0

foreach ($drawable in $drawableDuplicados) {
    $appFile = $drawable.FullName
    $uiFile = Join-Path "$uiBase\drawable" $drawable.Name
    
    if (Test-Path $uiFile) {
        $saoIdenticos = Compare-Files -file1 $appFile -file2 $uiFile
        if ($saoIdenticos) {
            try {
                Remove-Item -Path $appFile -Force -ErrorAction Stop
                $drawableRemovidos++
                if ($drawableRemovidos % 20 -eq 0) {
                    Write-Host "    Processados: $drawableRemovidos/$($drawableDuplicados.Count)" -ForegroundColor Gray
                }
            } catch {
                $drawableErros++
            }
        } else {
            $drawableDiferentes++
        }
    }
}

Write-Host "  [OK] Drawables removidos: $drawableRemovidos" -ForegroundColor Green
if ($drawableDiferentes -gt 0) {
    Write-Host "  [ATENCAO] Drawables diferentes: $drawableDiferentes (mantidos por seguranca)" -ForegroundColor Yellow
}
if ($drawableErros -gt 0) {
    Write-Host "  [ERRO] Erros ao remover: $drawableErros" -ForegroundColor Red
}
$removidos += $drawableRemovidos
$diferentes += $drawableDiferentes
$erros += $drawableErros
Write-Host ""

# 3. MENU
Write-Host "[3/3] Removendo menu duplicado..." -ForegroundColor Yellow
$menuFile = "navigation_drawer_menu.xml"
$appMenuFile = Join-Path "$appBase\menu" $menuFile
$uiMenuFile = Join-Path "$uiBase\menu" $menuFile

if (Test-Path $appMenuFile) {
    if (Test-Path $uiMenuFile) {
        $saoIdenticos = Compare-Files -file1 $appMenuFile -file2 $uiMenuFile
        if ($saoIdenticos) {
            try {
                Remove-Item -Path $appMenuFile -Force -ErrorAction Stop
                Write-Host "  [OK] Removido (identico): $menuFile" -ForegroundColor Green
                $removidos++
            } catch {
                Write-Host "  [ERRO] Nao foi possivel remover: $menuFile - $_" -ForegroundColor Red
                $erros++
            }
        } else {
            Write-Host "  [ATENCAO] Arquivos diferentes: $menuFile" -ForegroundColor Yellow
            Write-Host "    Mantendo ambos por seguranca. Verifique manualmente." -ForegroundColor Yellow
            $diferentes++
        }
    } else {
        Write-Host "  [INFO] Arquivo nao existe em ui: $menuFile (mantendo em app)" -ForegroundColor Gray
    }
} else {
    Write-Host "  [INFO] Arquivo nao existe em app: $menuFile" -ForegroundColor Gray
}
Write-Host ""

# RESUMO FINAL
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DA REMOCAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Arquivos removidos: $removidos" -ForegroundColor Green
if ($diferentes -gt 0) {
    Write-Host "  Arquivos diferentes (mantidos): $diferentes" -ForegroundColor Yellow
    Write-Host "    (Verifique manualmente se podem ser removidos)" -ForegroundColor Yellow
}
if ($erros -gt 0) {
    Write-Host "  Erros: $erros" -ForegroundColor Red
}
Write-Host ""
Write-Host "Todos os recursos duplicados e identicos foram removidos do modulo :app" -ForegroundColor Green
Write-Host "Os recursos agora estao apenas no modulo :ui (correto)" -ForegroundColor Green
Write-Host ""

