# Script para corrigir BOM dos layouts - versao sem interacao

$ErrorActionPreference = "Stop"

$layoutDir = "app\src\main\res\layout"
$commitHash = "7feb452b"

Write-Host "Corrigindo BOM dos layouts..." -ForegroundColor Cyan

$layouts = @(
    "fragment_colaborador_management.xml",
    "item_colaborador.xml"
)

foreach ($layout in $layouts) {
    $layoutPath = "$layoutDir\$layout"
    $gitPath = "app/src/main/res/layout/$layout"
    
    Write-Host ""
    Write-Host "Corrigindo: $layout" -ForegroundColor Yellow
    
    try {
        # Ler conteudo do commit
        $content = git show "${commitHash}:${gitPath}" 2>&1
        
        if ($LASTEXITCODE -eq 0 -and $content -notmatch "fatal:") {
            # Remover BOM e salvar
            $utf8NoBom = New-Object System.Text.UTF8Encoding $false
            $fullPath = Join-Path (Get-Location) $layoutPath
            [System.IO.File]::WriteAllText($fullPath, $content, $utf8NoBom)
            Write-Host "  [OK] Arquivo corrigido sem BOM" -ForegroundColor Green
        } else {
            Write-Host "  [ERRO] Arquivo nao encontrado no commit" -ForegroundColor Red
        }
    } catch {
        Write-Host "  [ERRO] Erro ao processar: $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "[OK] Processo concluido!" -ForegroundColor Green
