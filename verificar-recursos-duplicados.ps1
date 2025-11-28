# Script para verificar recursos duplicados entre app e ui
Write-Host "=== VERIFICACAO DE RECURSOS DUPLICADOS ===" -ForegroundColor Cyan
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

$duplicadosEncontrados = @()

# 1. LAYOUTS
Write-Host "[1/7] Verificando layouts..." -ForegroundColor Yellow
$appLayouts = Get-ChildItem -Path "$appBase\layout" -File -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name
$uiLayouts = Get-ChildItem -Path "$uiBase\layout" -File -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name
$layoutDuplicados = $appLayouts | Where-Object { $uiLayouts -contains $_ }
if ($layoutDuplicados.Count -gt 0) {
    Write-Host "  Layouts duplicados: $($layoutDuplicados.Count)" -ForegroundColor Yellow
    $duplicadosEncontrados += @{
        Tipo = "Layouts"
        Quantidade = $layoutDuplicados.Count
        Arquivos = $layoutDuplicados
    }
} else {
    Write-Host "  OK: Nenhum layout duplicado" -ForegroundColor Green
}
Write-Host ""

# 2. NAVIGATION
Write-Host "[2/7] Verificando navigation graphs..." -ForegroundColor Yellow
$appNav = Get-ChildItem -Path "$appBase\navigation" -File -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name
$uiNav = Get-ChildItem -Path "$uiBase\navigation" -File -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name
$navDuplicados = $appNav | Where-Object { $uiNav -contains $_ }
if ($navDuplicados.Count -gt 0) {
    Write-Host "  Navigation graphs duplicados: $($navDuplicados.Count)" -ForegroundColor Yellow
    $duplicadosEncontrados += @{
        Tipo = "Navigation"
        Quantidade = $navDuplicados.Count
        Arquivos = $navDuplicados
    }
} else {
    Write-Host "  OK: Nenhum navigation graph duplicado" -ForegroundColor Green
}
Write-Host ""

# 3. VALUES (strings, colors, styles, etc)
Write-Host "[3/7] Verificando values (strings, colors, styles)..." -ForegroundColor Yellow
$appValues = Get-ChildItem -Path "$appBase\values" -File -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name
$uiValues = Get-ChildItem -Path "$uiBase\values" -File -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name
$valuesDuplicados = $appValues | Where-Object { $uiValues -contains $_ }
if ($valuesDuplicados.Count -gt 0) {
    Write-Host "  Arquivos values duplicados: $($valuesDuplicados.Count)" -ForegroundColor Yellow
    $duplicadosEncontrados += @{
        Tipo = "Values"
        Quantidade = $valuesDuplicados.Count
        Arquivos = $valuesDuplicados
    }
} else {
    Write-Host "  OK: Nenhum arquivo values duplicado" -ForegroundColor Green
}
Write-Host ""

# 4. DRAWABLES
Write-Host "[4/7] Verificando drawables..." -ForegroundColor Yellow
$appDrawables = Get-ChildItem -Path "$appBase\drawable" -File -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name
$uiDrawables = Get-ChildItem -Path "$uiBase\drawable" -File -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name
$drawableDuplicados = $appDrawables | Where-Object { $uiDrawables -contains $_ }
if ($drawableDuplicados.Count -gt 0) {
    Write-Host "  Drawables duplicados: $($drawableDuplicados.Count)" -ForegroundColor Yellow
    $duplicadosEncontrados += @{
        Tipo = "Drawables"
        Quantidade = $drawableDuplicados.Count
        Arquivos = $drawableDuplicados
    }
} else {
    Write-Host "  OK: Nenhum drawable duplicado" -ForegroundColor Green
}
Write-Host ""

# 5. MENU
Write-Host "[5/7] Verificando menus..." -ForegroundColor Yellow
$appMenu = Get-ChildItem -Path "$appBase\menu" -File -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name
$uiMenu = Get-ChildItem -Path "$uiBase\menu" -File -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name
$menuDuplicados = $appMenu | Where-Object { $uiMenu -contains $_ }
if ($menuDuplicados.Count -gt 0) {
    Write-Host "  Menus duplicados: $($menuDuplicados.Count)" -ForegroundColor Yellow
    $duplicadosEncontrados += @{
        Tipo = "Menu"
        Quantidade = $menuDuplicados.Count
        Arquivos = $menuDuplicados
    }
} else {
    Write-Host "  OK: Nenhum menu duplicado" -ForegroundColor Green
}
Write-Host ""

# 6. MIPMAP (icones)
Write-Host "[6/7] Verificando mipmaps (icones)..." -ForegroundColor Yellow
$appMipmap = Get-ChildItem -Path "$appBase\mipmap-*" -Directory -ErrorAction SilentlyContinue
$uiMipmap = Get-ChildItem -Path "$uiBase\mipmap-*" -Directory -ErrorAction SilentlyContinue
if ($appMipmap.Count -gt 0 -and $uiMipmap.Count -gt 0) {
    Write-Host "  ATENCAO: Ambos os modulos tem mipmaps" -ForegroundColor Yellow
    $duplicadosEncontrados += @{
        Tipo = "Mipmap"
        Quantidade = "Verificar manualmente"
        Arquivos = @("Mipmaps podem estar duplicados")
    }
} else {
    Write-Host "  OK: Mipmaps nao duplicados" -ForegroundColor Green
}
Write-Host ""

# 7. CÓDIGO KOTLIN (verificar se há código duplicado)
Write-Host "[7/7] Verificando codigo Kotlin duplicado..." -ForegroundColor Yellow
$appKotlin = Get-ChildItem -Path "app\src\main\java" -Recurse -Filter "*.kt" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name
$uiKotlin = Get-ChildItem -Path "ui\src\main\java" -Recurse -Filter "*.kt" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name
$kotlinDuplicados = $appKotlin | Where-Object { $uiKotlin -contains $_ }
if ($kotlinDuplicados.Count -gt 0) {
    Write-Host "  Arquivos Kotlin com mesmo nome: $($kotlinDuplicados.Count)" -ForegroundColor Yellow
    Write-Host "  ATENCAO: Verificar se sao realmente duplicados ou apenas mesmo nome" -ForegroundColor Yellow
    $duplicadosEncontrados += @{
        Tipo = "Kotlin"
        Quantidade = $kotlinDuplicados.Count
        Arquivos = $kotlinDuplicados
    }
} else {
    Write-Host "  OK: Nenhum arquivo Kotlin duplicado" -ForegroundColor Green
}
Write-Host ""

# RESUMO
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DE DUPLICACOES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($duplicadosEncontrados.Count -eq 0) {
    Write-Host "Nenhuma duplicacao encontrada!" -ForegroundColor Green
} else {
    foreach ($duplicado in $duplicadosEncontrados) {
        Write-Host "Tipo: $($duplicado.Tipo)" -ForegroundColor Yellow
        Write-Host "  Quantidade: $($duplicado.Quantidade)" -ForegroundColor White
        if ($duplicado.Arquivos.Count -le 10) {
            Write-Host "  Arquivos:" -ForegroundColor White
            foreach ($arquivo in $duplicado.Arquivos) {
                Write-Host "    - $arquivo" -ForegroundColor Gray
            }
        } else {
            Write-Host "  Arquivos: $($duplicado.Arquivos.Count) arquivos (muitos para listar)" -ForegroundColor Gray
        }
        Write-Host ""
    }
    
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "RECOMENDACAO" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Todos os recursos devem estar apenas no modulo :ui" -ForegroundColor Yellow
    Write-Host "O modulo :app deve conter apenas:" -ForegroundColor Yellow
    Write-Host "  - MainActivity.kt" -ForegroundColor White
    Write-Host "  - GestaoBilharesApplication.kt" -ForegroundColor White
    Write-Host "  - NotificationService.kt" -ForegroundColor White
    Write-Host "  - AndroidManifest.xml" -ForegroundColor White
    Write-Host ""
    Write-Host "Todos os layouts, navigation, values, drawables, menus devem estar em :ui" -ForegroundColor Yellow
    Write-Host ""
}

