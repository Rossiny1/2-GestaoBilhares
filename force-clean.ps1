# SCRIPT DE LIMPEZA FORÇADA - GestaoBilhares
# Remove processos e arquivos bloqueados

Write-Host "INICIANDO LIMPEZA FORÇADA..." -ForegroundColor Red

# Parar todos os processos Java/Gradle
Write-Host "Parando processos Java/Gradle..." -ForegroundColor Yellow
try {
    taskkill /f /im java.exe 2>$null
    taskkill /f /im gradle.exe 2>$null
    taskkill /f /im gradlew.exe 2>$null
    Write-Host "Processos Java/Gradle parados" -ForegroundColor Green
} catch {
    Write-Host "Nenhum processo Java/Gradle encontrado" -ForegroundColor Yellow
}

# Parar daemons Gradle
Write-Host "Parando daemons Gradle..." -ForegroundColor Yellow
try {
    ./gradlew --stop 2>$null
    Write-Host "Daemons Gradle parados" -ForegroundColor Green
} catch {
    Write-Host "Nenhum daemon Gradle encontrado" -ForegroundColor Yellow
}

# Aguardar um pouco
Write-Host "Aguardando 3 segundos..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

# Remover diretórios de build manualmente
Write-Host "Removendo diretórios de build..." -ForegroundColor Yellow
try {
    if (Test-Path "app/build") {
        Remove-Item -Path "app/build" -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "Diretório app/build removido" -ForegroundColor Green
    }
    if (Test-Path "build") {
        Remove-Item -Path "build" -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "Diretório build removido" -ForegroundColor Green
    }
    if (Test-Path ".gradle") {
        Remove-Item -Path ".gradle" -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "Diretório .gradle removido" -ForegroundColor Green
    }
} catch {
    Write-Host "Alguns arquivos podem estar bloqueados, continuando..." -ForegroundColor Yellow
}

# Limpar cache do Gradle
Write-Host "Limpando cache do Gradle..." -ForegroundColor Yellow
try {
    ./gradlew clean --no-daemon 2>$null
    Write-Host "Cache Gradle limpo" -ForegroundColor Green
} catch {
    Write-Host "Erro ao limpar cache, continuando..." -ForegroundColor Yellow
}

Write-Host "LIMPEZA FORÇADA CONCLUÍDA!" -ForegroundColor Green
Write-Host "Agora você pode tentar o build novamente" -ForegroundColor Cyan
