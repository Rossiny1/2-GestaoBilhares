<# remove-app-duplicates.ps1
   -------------------------
   Remove diretórios duplicados do módulo :app após validação
   Remove: data/, sync/, ui/, workers/ (mantém apenas MainActivity, Application, NotificationService)
#>
[CmdletBinding(SupportsShouldProcess)]
param(
    [switch]$DryRun
)

$directoriesToRemove = @(
    "app\src\main\java\com\example\gestaobilhares\data",
    "app\src\main\java\com\example\gestaobilhares\sync",
    "app\src\main\java\com\example\gestaobilhares\ui",
    "app\src\main\java\com\example\gestaobilhares\workers"
)

$filesToKeep = @(
    "app\src\main\java\com\example\gestaobilhares\MainActivity.kt",
    "app\src\main\java\com\example\gestaobilhares\GestaoBilharesApplication.kt",
    "app\src\main\java\com\example\gestaobilhares\notification\NotificationService.kt"
)

$rootPath = Split-Path -Parent $PSScriptRoot

Write-Host "=== REMOCAO DE DIRETORIOS DUPLICADOS DO :app ===" -ForegroundColor Cyan
Write-Host ""

foreach ($dir in $directoriesToRemove) {
    $fullPath = Join-Path $rootPath $dir
    
    if (Test-Path $fullPath) {
        if ($DryRun) {
            Write-Host "DRY-RUN: Removeria: $dir" -ForegroundColor Yellow
        } elseif ($PSCmdlet.ShouldProcess($fullPath, "Remover diretorio duplicado")) {
            Write-Host "Removendo: $dir" -ForegroundColor Yellow
            Remove-Item -Path $fullPath -Recurse -Force
            Write-Host "  OK: Removido com sucesso" -ForegroundColor Green
        }
    } else {
        Write-Host "Ignorando: $dir (nao existe)" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "=== VERIFICACAO DE ARQUIVOS CRITICOS ===" -ForegroundColor Cyan

foreach ($file in $filesToKeep) {
    $fullPath = Join-Path $rootPath $file
    
    if (Test-Path $fullPath) {
        Write-Host "  OK: $file existe" -ForegroundColor Green
    } else {
        Write-Host "  ATENCAO: $file nao encontrado!" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== CONCLUIDO ===" -ForegroundColor Cyan
if ($DryRun) {
    Write-Host "Modo DRY-RUN: nenhum arquivo foi removido." -ForegroundColor Yellow
}

