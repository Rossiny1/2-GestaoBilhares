@echo off
REM Script batch para commitar e fazer push automaticamente quando build passa
REM Este script será chamado automaticamente após build bem-sucedido

git diff --quiet
if errorlevel 1 goto has_changes

git diff --cached --quiet
if errorlevel 1 goto has_changes

echo ℹ️  Nenhuma mudança para commitar.
exit /b 0

:has_changes
REM Criar mensagem de commit automática
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set datetime=%%I
set COMMIT_MSG=Auto-commit: Correções de build - %datetime:~0,4%-%datetime:~4,2%-%datetime:~6,2% %datetime:~8,2%:%datetime:~10,2%:%datetime:~12,2%

echo 📝 Fazendo commit automático das mudanças...
git add -A

git commit -m "%COMMIT_MSG%"
if errorlevel 1 (
    echo ⚠️  Nenhuma mudança para commitar ou commit falhou.
    exit /b 0
)

echo 📤 Fazendo push para o repositório remoto...
git push origin HEAD
if errorlevel 1 (
    echo ⚠️  Push falhou. Verifique a conexão ou credenciais.
    exit /b 1
)

echo ✅ Mudanças commitadas e enviadas com sucesso!
