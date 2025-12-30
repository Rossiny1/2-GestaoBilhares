@echo off
REM Script batch para sincronizar todas as mudanças (GitHub e local)
REM Uso: scripts\sync-all-changes.bat

echo 🔄 Sincronizando todas as mudanças...
echo.

echo 📊 Verificando status do repositório...
git status --short

echo.
echo 📝 Verificando mudanças locais...
git add -A
git diff --cached --quiet
if errorlevel 1 (
    echo 📝 Mudanças locais detectadas. Fazendo commit...
    for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set datetime=%%I
    set COMMIT_MSG=Auto-sync: Salvando mudanças locais - %datetime:~0,4%-%datetime:~4,2%-%datetime:~6,2% %datetime:~8,2%:%datetime:~10,2%:%datetime:~12,2%
    git commit -m "%COMMIT_MSG%"
    if errorlevel 1 (
        echo ❌ Erro ao fazer commit
    ) else (
        echo ✅ Mudanças locais commitadas!
    )
)

echo.
echo 📥 Buscando mudanças do GitHub...
git fetch origin

echo.
echo 📥 Fazendo pull...
git pull origin
if errorlevel 1 (
    echo ⚠️  Erro ao fazer pull
) else (
    echo ✅ Mudanças do GitHub baixadas!
)

echo.
echo 📤 Verificando commits locais não enviados...
git push origin HEAD
if errorlevel 1 (
    echo ⚠️  Erro ao fazer push
) else (
    echo ✅ Todos os commits foram enviados para o GitHub!
)

echo.
echo ════════════════════════════════════════
echo ✅ Sincronização completa!
echo.
echo 📝 Últimos 3 commits:
git log --oneline -3
echo.
