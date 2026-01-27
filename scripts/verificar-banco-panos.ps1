# Script para verificar estado do banco de dados de panos
# Versão: 1.0

Write-Host "=== VERIFICAÇÃO DO BANCO DE DADOS DE PANOS ===" -ForegroundColor Yellow

# Caminho do ADB
$ADB = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar dispositivo
$devices = & $ADB devices
if (!($devices -match "device$")) {
    Write-Host "❌ Nenhum dispositivo conectado!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Dispositivo conectado" -ForegroundColor Green

# Comandos para verificar o banco de dados
Write-Host ""
Write-Host "Verificando banco de dados do app..." -ForegroundColor Cyan

# 1. Listar bancos de dados do app
Write-Host ""
Write-Host "1. Listando bancos de dados do app:" -ForegroundColor Yellow
& $ADB shell run-as com.example.gestaobilhares ls -la databases/

# 2. Verificar se o banco existe
Write-Host ""
Write-Host "2. Verificando tabela panos_estoque:" -ForegroundColor Yellow
& $ADB shell run-as com.example.gestaobilhares sqlite3 databases/gestaobilhares.db ".tables panos_estoque"

# 3. Contar panos no banco
Write-Host ""
Write-Host "3. Contando panos no banco:" -ForegroundColor Yellow
& $ADB shell run-as com.example.gestaobilhares sqlite3 databases/gestaobilhares.db "SELECT COUNT(*) FROM panos_estoque;"

# 4. Listar todos os panos
Write-Host ""
Write-Host "4. Listando todos os panos:" -ForegroundColor Yellow
& $ADB shell run-as com.example.gestaobilhares sqlite3 databases/gestaobilhares.db "SELECT id, numero, cor, tamanho, material, disponivel FROM panos_estoque ORDER BY numero;"

# 5. Verificar panos disponíveis
Write-Host ""
Write-Host "5. Verificando panos disponíveis:" -ForegroundColor Yellow
& $ADB shell run-as com.example.gestaobilhares sqlite3 databases/gestaobilhares.db "SELECT COUNT(*) FROM panos_estoque WHERE disponivel = 1;"

# 6. Verificar panos por tamanho
Write-Host ""
Write-Host "6. Panos por tamanho:" -ForegroundColor Yellow
& $ADB shell run-as com.example.gestaobilhares sqlite3 databases/gestaobilhares.db "SELECT tamanho, COUNT(*) FROM panos_estoque GROUP BY tamanho;"

Write-Host ""
Write-Host "VERIFICAÇÃO CONCLUÍDA" -ForegroundColor Green
Write-Host "Se há panos no banco mas não aparecem na UI, o problema é na notificação do Flow" -ForegroundColor Cyan
Write-Host "Se não há panos no banco, o problema está na inserção" -ForegroundColor Cyan
