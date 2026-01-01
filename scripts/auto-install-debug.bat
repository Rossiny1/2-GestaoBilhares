@echo off
REM Script batch para verificar mudanças remotas e instalar automaticamente o app
REM Uso: scripts\auto-install-debug.bat

echo 🔄 Verificando mudanças remotas...

git fetch origin
if errorlevel 1 (
    echo ❌ Erro ao fazer fetch
    exit /b 1
)

git pull origin
if errorlevel 1 (
    echo ❌ Erro ao fazer pull
    exit /b 1
)

echo 🔨 Compilando e instalando app...
call gradlew.bat installDebug

if errorlevel 1 (
    echo ❌ Erro ao instalar app
    exit /b 1
) else (
    echo ✅ App instalado com sucesso no dispositivo conectado!
)
