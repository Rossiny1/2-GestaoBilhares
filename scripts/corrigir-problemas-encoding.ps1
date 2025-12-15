# Script para corrigir problemas de encoding e arquivos faltantes

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CORRECAO DE ENCODING E ARQUIVOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar e mover arquivos ProcuraçãoRepresentante se existirem
Write-Host "[1/4] Verificando arquivos ProcuraçãoRepresentante..." -ForegroundColor Yellow
$daoPath = "app\src\main\java\com\example\gestaobilhares\data\dao\ProcuraçãoRepresentanteDao.kt"
$entityPath = "app\src\main\java\com\example\gestaobilhares\data\entities\ProcuraçãoRepresentante.kt"
$backupDir = "app\src\main\java\com\example\gestaobilhares\data\_backup_encoding"

if (-not (Test-Path $backupDir)) {
    New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
}

if (Test-Path $daoPath) {
    $backupDao = Join-Path $backupDir "ProcuraçãoRepresentanteDao.kt"
    Move-Item -Path $daoPath -Destination $backupDao -Force -ErrorAction SilentlyContinue
    Write-Host "OK: DAO movido para backup" -ForegroundColor Green
} else {
    Write-Host "INFO: DAO nao existe (ja removido ou comentado)" -ForegroundColor Gray
}

if (Test-Path $entityPath) {
    $backupEntity = Join-Path $backupDir "ProcuraçãoRepresentante.kt"
    Move-Item -Path $entityPath -Destination $backupEntity -Force -ErrorAction SilentlyContinue
    Write-Host "OK: Entity movida para backup" -ForegroundColor Green
} else {
    Write-Host "INFO: Entity nao existe (ja removida ou comentada)" -ForegroundColor Gray
}
Write-Host ""

# 2. Garantir que item_expense_type.xml está correto
Write-Host "[2/4] Verificando item_expense_type.xml..." -ForegroundColor Yellow
$xmlPath = "app\src\main\res\layout\item_expense_type.xml"
if (Test-Path $xmlPath) {
    $firstLine = Get-Content $xmlPath -TotalCount 1 -ErrorAction SilentlyContinue
    if ($firstLine -and $firstLine -match "^\<\?xml") {
        Write-Host "OK: XML esta correto" -ForegroundColor Green
    } else {
        Write-Host "AVISO: XML pode estar corrompido, restaurando..." -ForegroundColor Yellow
        git checkout 7feb452b -- "app/src/main/res/layout/item_expense_type.xml" 2>&1 | Out-Null
        Write-Host "OK: XML restaurado" -ForegroundColor Green
    }
} else {
    Write-Host "ERRO: XML nao existe, restaurando..." -ForegroundColor Red
    git checkout 7feb452b -- "app/src/main/res/layout/item_expense_type.xml" 2>&1 | Out-Null
    Write-Host "OK: XML restaurado" -ForegroundColor Green
}
Write-Host ""

# 3. Limpar cache do build
Write-Host "[3/4] Limpando cache do build..." -ForegroundColor Yellow
Remove-Item -Path "app\build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path ".gradle" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "build" -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "OK: Cache limpo" -ForegroundColor Green
Write-Host ""

# 4. Parar processos Java/Gradle
Write-Host "[4/4] Parando processos..." -ForegroundColor Yellow
.\gradlew --stop 2>&1 | Out-Null
Start-Sleep -Seconds 2
Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1
Write-Host "OK: Processos parados" -ForegroundColor Green
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CORRECAO CONCLUIDA" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Execute: .\gradlew assembleDebug" -ForegroundColor Yellow
Write-Host ""

