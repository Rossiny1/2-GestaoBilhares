# Script para corrigir todos os problemas do build de uma vez

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CORRECAO COMPLETA DO BUILD" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Parar processos Java e Gradle
Write-Host "[1/5] Parando processos Java e Gradle..." -ForegroundColor Yellow
.\gradlew --stop 2>$null
Start-Sleep -Seconds 2
Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1
Write-Host "OK: Processos parados" -ForegroundColor Green
Write-Host ""

# 2. Restaurar item_expense_type.xml corretamente
Write-Host "[2/5] Restaurando item_expense_type.xml..." -ForegroundColor Yellow
$xmlContent = git show 7feb452b:app/src/main/res/layout/item_expense_type.xml 2>$null
if ($xmlContent) {
    [System.IO.File]::WriteAllText("app\src\main\res\layout\item_expense_type.xml", $xmlContent, [System.Text.Encoding]::UTF8)
    Write-Host "OK: XML restaurado" -ForegroundColor Green
} else {
    Write-Host "ERRO: Nao foi possivel restaurar o XML" -ForegroundColor Red
}
Write-Host ""

# 3. Verificar se ProcuraçãoRepresentante está causando problemas
Write-Host "[3/5] Verificando arquivos ProcuraçãoRepresentante..." -ForegroundColor Yellow
$procDao = "app\src\main\java\com\example\gestaobilhares\data\dao\ProcuraçãoRepresentanteDao.kt"
$procEntity = "app\src\main\java\com\example\gestaobilhares\data\entities\ProcuraçãoRepresentante.kt"

if (Test-Path $procDao) {
    Write-Host "OK: ProcuraçãoRepresentanteDao.kt existe" -ForegroundColor Green
} else {
    Write-Host "AVISO: ProcuraçãoRepresentanteDao.kt nao encontrado" -ForegroundColor Yellow
}

if (Test-Path $procEntity) {
    Write-Host "OK: ProcuraçãoRepresentante.kt existe" -ForegroundColor Green
} else {
    Write-Host "AVISO: ProcuraçãoRepresentante.kt nao encontrado" -ForegroundColor Yellow
}
Write-Host ""

# 4. Limpar cache do build
Write-Host "[4/5] Limpando cache do build..." -ForegroundColor Yellow
Remove-Item -Path "app\build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path ".gradle" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "build" -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "OK: Cache limpo" -ForegroundColor Green
Write-Host ""

# 5. Verificar XML restaurado
Write-Host "[5/5] Verificando XML restaurado..." -ForegroundColor Yellow
$xmlFile = "app\src\main\res\layout\item_expense_type.xml"
if (Test-Path $xmlFile) {
    $firstLine = Get-Content $xmlFile -TotalCount 1
    if ($firstLine -match "^\<\?xml") {
        Write-Host "OK: XML esta correto" -ForegroundColor Green
    } else {
        Write-Host "ERRO: XML ainda esta corrompido" -ForegroundColor Red
    }
} else {
    Write-Host "ERRO: XML nao existe" -ForegroundColor Red
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CORRECAO CONCLUIDA" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Execute: .\gradlew assembleDebug" -ForegroundColor Yellow

