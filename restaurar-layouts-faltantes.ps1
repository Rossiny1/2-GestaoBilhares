# Script para restaurar layouts faltantes do commit 7feb452b

$commitHash = "7feb452b"
$layoutDir = "app\src\main\res\layout"

Write-Host "Restaurando layouts faltantes do commit $commitHash..." -ForegroundColor Cyan

# Lista de layouts para restaurar
$layouts = @(
    "fragment_colaborador_management.xml",
    "item_colaborador.xml"
)

foreach ($layout in $layouts) {
    $layoutPath = "$layoutDir\$layout"
    $gitPath = "app/src/main/res/layout/$layout"
    
    Write-Host ""
    Write-Host "Verificando: $layout" -ForegroundColor Yellow
    
    # Verificar se o arquivo existe no commit
    git cat-file -e "$commitHash`:$gitPath" 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK] Arquivo encontrado no commit" -ForegroundColor Green
        
        # Criar diretório se não existir
        $dir = Split-Path -Parent $layoutPath
        if (-not (Test-Path $dir)) {
            New-Item -ItemType Directory -Path $dir -Force | Out-Null
            Write-Host "  [DIR] Diretorio criado: $dir" -ForegroundColor Gray
        }
        
        # Restaurar arquivo do commit
        try {
            git show "$commitHash`:$gitPath" | Out-File -FilePath $layoutPath -Encoding UTF8 -NoNewline
            Write-Host "  [OK] Restaurado: $layoutPath" -ForegroundColor Green
        } catch {
            Write-Host "  [ERRO] Erro ao restaurar: $_" -ForegroundColor Red
        }
    } else {
        Write-Host "  [AVISO] Arquivo nao encontrado no commit: $gitPath" -ForegroundColor Yellow
    }
}

# Verificar também DetalhesMesaReformadaComHistoricoDialog
Write-Host ""
Write-Host "Verificando dialogs..." -ForegroundColor Yellow
$dialogLayouts = @(
    "dialog_detalhes_mesa_reformada_com_historico.xml"
)

foreach ($dialog in $dialogLayouts) {
    $dialogPath = "$layoutDir\$dialog"
    $gitPath = "app/src/main/res/layout/$dialog"
    
    Write-Host "Verificando: $dialog" -ForegroundColor Yellow
    
    git cat-file -e "$commitHash`:$gitPath" 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK] Arquivo encontrado no commit" -ForegroundColor Green
        
        $dir = Split-Path -Parent $dialogPath
        if (-not (Test-Path $dir)) {
            New-Item -ItemType Directory -Path $dir -Force | Out-Null
        }
        
        try {
            git show "$commitHash`:$gitPath" | Out-File -FilePath $dialogPath -Encoding UTF8 -NoNewline
            Write-Host "  [OK] Restaurado: $dialogPath" -ForegroundColor Green
        } catch {
            Write-Host "  [ERRO] Erro ao restaurar: $_" -ForegroundColor Red
        }
    } else {
        Write-Host "  [AVISO] Arquivo nao encontrado no commit" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "[OK] Processo concluido!" -ForegroundColor Green

