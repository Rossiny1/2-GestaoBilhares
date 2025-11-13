Write-Host "Corrigindo item_expense_type.xml..." -ForegroundColor Cyan

# Restaurar do commit e remover BOM
$content = git show 7feb452b:app/src/main/res/layout/item_expense_type.xml
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText("app\src\main\res\layout\item_expense_type.xml", $content, $utf8NoBom)

Write-Host "OK: Arquivo restaurado sem BOM" -ForegroundColor Green

# Verificar se o arquivo está correto
$firstLine = Get-Content "app\src\main\res\layout\item_expense_type.xml" -TotalCount 1 -Encoding UTF8
if ($firstLine -match '^\<\?xml') {
    Write-Host "OK: XML valido" -ForegroundColor Green
} else {
    Write-Host "ERRO: XML invalido" -ForegroundColor Red
}

Write-Host "`nExecute: .\gradlew assembleDebug" -ForegroundColor Yellow

