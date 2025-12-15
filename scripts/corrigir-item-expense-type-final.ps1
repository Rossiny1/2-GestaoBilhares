Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CORRECAO: item_expense_type.xml" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# [1/4] Deletar arquivo corrompido
Write-Host "`n[1/4] Removendo arquivo corrompido..." -ForegroundColor Yellow
if (Test-Path "app\src\main\res\layout\item_expense_type.xml") {
    Remove-Item "app\src\main\res\layout\item_expense_type.xml" -Force
    Write-Host "OK: Arquivo removido" -ForegroundColor Green
} else {
    Write-Host "INFO: Arquivo nao existe" -ForegroundColor Yellow
}

# [2/4] Restaurar do commit sem BOM
Write-Host "`n[2/4] Restaurando do commit 7feb452b..." -ForegroundColor Yellow
$content = git show 7feb452b:app/src/main/res/layout/item_expense_type.xml
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText("app\src\main\res\layout\item_expense_type.xml", $content, $utf8NoBom)
Write-Host "OK: Arquivo restaurado sem BOM" -ForegroundColor Green

# [3/4] Verificar se o arquivo está correto
Write-Host "`n[3/4] Verificando XML..." -ForegroundColor Yellow
$firstLine = Get-Content "app\src\main\res\layout\item_expense_type.xml" -TotalCount 1 -Encoding UTF8
if ($firstLine -match '^\<\?xml') {
    Write-Host "OK: XML valido (comeca com <?xml)" -ForegroundColor Green
} else {
    Write-Host "ERRO: XML invalido - primeira linha: $firstLine" -ForegroundColor Red
    exit 1
}

# [4/4] Limpar cache do build
Write-Host "`n[4/4] Limpando cache do build..." -ForegroundColor Yellow
.\gradlew clean --no-daemon 2>&1 | Out-Null
Write-Host "OK: Cache limpo" -ForegroundColor Green

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "CORRECAO CONCLUIDA" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "`nExecute: .\gradlew assembleDebug" -ForegroundColor Yellow

