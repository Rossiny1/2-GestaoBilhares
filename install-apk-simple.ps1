# Script para instalar APK corrigido do GestaoBilhares
# Corrigido problema do crash na seleção de tipo de despesa

Write-Host "=== INSTALANDO APK CORRIGIDO - GESTAO BILHARES ===" -ForegroundColor Green
Write-Host ""

# Verificar se o APK existe
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apkPath)) {
    Write-Host "ERRO: APK nao encontrado em $apkPath" -ForegroundColor Red
    Write-Host "Execute primeiro: .\gradlew assembleDebug" -ForegroundColor Yellow
    exit 1
}

Write-Host "APK encontrado: $apkPath" -ForegroundColor Green
Write-Host "Tamanho: $((Get-Item $apkPath).Length / 1MB) MB" -ForegroundColor Cyan

# Verificar se há dispositivos conectados
Write-Host ""
Write-Host "Verificando dispositivos conectados..." -ForegroundColor Yellow
$devices = adb devices
Write-Host $devices

# Verificar se há pelo menos um dispositivo
if ($devices -match "device$") {
    Write-Host "Dispositivo encontrado!" -ForegroundColor Green
    
    # Desinstalar versão anterior (se existir)
    Write-Host ""
    Write-Host "Desinstalando versao anterior..." -ForegroundColor Yellow
    adb uninstall com.example.gestaobilhares
    
    # Instalar nova versão
    Write-Host ""
    Write-Host "Instalando nova versao..." -ForegroundColor Yellow
    adb install -r $apkPath
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "INSTALACAO CONCLUIDA COM SUCESSO!" -ForegroundColor Green
        Write-Host ""
        Write-Host "=== CORRECOES APLICADAS ===" -ForegroundColor Cyan
        Write-Host "ID tvTitle adicionado ao layout dialog_select_category.xml" -ForegroundColor Green
        Write-Host "Crash na selecao de tipo de despesa corrigido" -ForegroundColor Green
        Write-Host "Sistema de categoria/tipo funcionando corretamente" -ForegroundColor Green
        Write-Host ""
        Write-Host "Para testar:" -ForegroundColor Yellow
        Write-Host "1. Abra o app GestaoBilhares" -ForegroundColor White
        Write-Host "2. Va para Rotas > Clientes > Detalhes > Despesas" -ForegroundColor White
        Write-Host "3. Tente cadastrar uma despesa selecionando categoria e tipo" -ForegroundColor White
        Write-Host "4. O crash nao deve mais ocorrer" -ForegroundColor White
        Write-Host ""
        Write-Host "APK pronto para testes!" -ForegroundColor Green
    } else {
        Write-Host "ERRO na instalacao!" -ForegroundColor Red
        Write-Host "Verifique se o dispositivo esta conectado e com USB Debug ativo" -ForegroundColor Yellow
    }
} else {
    Write-Host "Nenhum dispositivo encontrado!" -ForegroundColor Red
    Write-Host "Conecte um dispositivo Android via USB e ative o USB Debug" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Pressione qualquer tecla para sair..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown") 