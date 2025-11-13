# Script para corrigir layouts com BOM - restaurar sem BOM

$commitHash = "7feb452b"
$layoutDir = "app\src\main\res\layout"

Write-Host "Corrigindo layouts com BOM do commit $commitHash..." -ForegroundColor Cyan

# Lista de layouts para restaurar sem BOM
$layouts = @(
    "fragment_colaborador_management.xml",
    "item_colaborador.xml"
)

foreach ($layout in $layouts) {
    $layoutPath = "$layoutDir\$layout"
    $gitPath = "app/src/main/res/layout/$layout"
    
    Write-Host ""
    Write-Host "Corrigindo: $layout" -ForegroundColor Yellow
    
    # Verificar se o arquivo existe no commit
    git cat-file -e "$commitHash`:$gitPath" 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK] Arquivo encontrado no commit" -ForegroundColor Green
        
        # Remover arquivo atual se existir
        if (Test-Path $layoutPath) {
            Remove-Item $layoutPath -Force
            Write-Host "  [OK] Arquivo atual removido" -ForegroundColor Gray
        }
        
        # Criar diretório se não existir
        $dir = Split-Path -Parent $layoutPath
        if (-not (Test-Path $dir)) {
            New-Item -ItemType Directory -Path $dir -Force | Out-Null
        }
        
        # Restaurar arquivo do commit SEM BOM usando UTF8NoBOM
        try {
            $content = git show "$commitHash`:$gitPath"
            # Remover BOM se existir e salvar como UTF8 sem BOM
            $utf8NoBom = New-Object System.Text.UTF8Encoding $false
            $fullPath = Join-Path (Get-Location) $layoutPath
            [System.IO.File]::WriteAllText($fullPath, $content, $utf8NoBom)
            Write-Host "  [OK] Restaurado sem BOM: $layoutPath" -ForegroundColor Green
        } catch {
            Write-Host "  [ERRO] Erro ao restaurar: $_" -ForegroundColor Red
        }
    } else {
        Write-Host "  [AVISO] Arquivo nao encontrado no commit: $gitPath" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "[OK] Processo concluido!" -ForegroundColor Green

