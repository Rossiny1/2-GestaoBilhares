@echo off
echo === ANALISE DE LOGS - CICLO 4 ===
echo.

REM Verificar se foi passado um arquivo como parametro
if "%~1"=="" (
    echo ❌ Uso: analisar-logs-ciclo-4.bat "caminho\para\logfile.txt"
    echo.
    echo 💡 Exemplo: analisar-logs-ciclo-4.bat "logs_ciclo_4_20241202_143022.txt"
    echo.
    echo 📁 Procurando arquivos de log recentes...
    dir logs_ciclo_4_*.txt /b /o-d 2>nul
    if errorlevel 1 (
        echo    Nenhum arquivo de log encontrado
    ) else (
        echo.
        echo 💡 Execute o comando novamente com o nome do arquivo desejado
    )
    pause
    exit /b 1
)

REM Verificar se o arquivo existe
if not exist "%~1" (
    echo ❌ Arquivo nao encontrado: %~1
    pause
    exit /b 1
)

echo 📄 Analisando arquivo: %~1
echo 📅 Data de criacao: %~t1
echo 📏 Tamanho: %~z1 bytes
echo.

echo 🔍 PROCURANDO CICLO 4 NOS LOGS...
echo ========================================
echo.

echo 🎯 1. CICLO 4 SENDO PROCESSADO:
echo ----------------------------------
findstr /C:"Ciclo ID=4" "%~1" 2>nul || echo    ℹ️  Nenhum log encontrado
findstr /C:"numeroCiclo=4" "%~1" 2>nul || echo    ℹ️  Nenhum log encontrado
findstr /C:"Inserindo novo ciclo" "%~1" 2>nul || echo    ℹ️  Nenhum log encontrado
echo.

echo 🔄 2. ATUALIZACAO DA ROTA COM CICLO 4:
echo ---------------------------------------
findstr /C:"atualizada com ciclo 4" "%~1" 2>nul || echo    ℹ️  Nenhum log encontrado
findstr /C:"cicloAcertoAtual=4" "%~1" 2>nul || echo    ℹ️  Nenhum log encontrado
findstr /C:"Rota ID=1" "%~1" 2>nul || echo    ℹ️  Nenhum log encontrado
echo.

echo ✅ 3. VERIFICACAO APOS ATUALIZACAO:
echo ----------------------------------
findstr /C:"Rota verificada" "%~1" 2>nul || echo    ℹ️  Nenhum log encontrado
findstr /C:"Timestamp.*atualizado" "%~1" 2>nul || echo    ℹ️  Nenhum log encontrado
echo.

echo 🔄 4. SINCRONIZACAO CONCLUIDA:
echo ----------------------------
findstr /C:"Sincroniza.*conclu" "%~1" 2>nul || echo    ℹ️  Nenhum log encontrado
findstr /C:"synchronized" "%~1" 2>nul || echo    ℹ️  Nenhum log encontrado
findstr /C:"sync=4" "%~1" 2>nul || echo    ℹ️  Nenhum log encontrado
echo.

echo 📊 5. TOTAL DE CICLOS APOS SYNC:
echo -------------------------------
findstr /C:"Total de ciclos" "%~1" 2>nul || echo    ℹ️  Nenhum log encontrado
echo.

echo 🔧 6. MECANISMOS DE FALLBACK:
echo ----------------------------
findstr /C:"verificarCiclosNaoExibidos" "%~1" 2>nul || echo    ℹ️  Nenhum log encontrado
findstr /C:"forcarAtualizacaoCicloRota" "%~1" 2>nul || echo    ℹ️  Nenhum log encontrado
findstr /C:"Mecanismo de fallback" "%~1" 2>nul || echo    ℹ️  Nenhum log encontrado
echo.

echo ❌ 7. ERROS ENCONTRADOS:
echo -----------------------
findstr /C:"ERRO" "%~1" 2>nul || echo    ℹ️  Nenhum erro encontrado
findstr /C:"ERROR" "%~1" 2>nul || echo    ℹ️  Nenhum erro encontrado
findstr /C:"Exception" "%~1" 2>nul || echo    ℹ️  Nenhum erro encontrado
echo.

echo 📋 8. RESUMO COMPLETO DOS LOGS:
echo ================================
echo.
echo 📄 Total de linhas no arquivo:
find /c "" "%~1"
echo.

echo 🎯 Eventos relacionados ao ciclo 4:
findstr /C:"4" "%~1" | find /c "4"
echo.

echo 🔄 Eventos de sincronizacao:
findstr /C:"sync" "%~1" | find /c "sync"
echo.

echo ⚠️  Warnings encontrados:
findstr /C:"WARN" "%~1" | find /c "WARN"
echo.

echo 💡 DICAS PARA ANALISE:
echo =====================
echo.
echo 🔍 Se o ciclo 4 NAO aparece nos logs:
echo    • Verifique se a sincronizacao foi executada
echo    • Execute novamente o teste de ciclo 4
echo    • Verifique conexao com internet
echo.
echo 🔍 Se o ciclo 4 aparece mas NAO eh exibido:
echo    • Problema no refresh da interface
echo    • Verifique os logs de RoutesViewModel
echo    • Execute o metodo forcarRefreshDados()
echo.
echo 🔍 Se ha erros de sincronizacao:
echo    • Verifique autenticacao Firebase
echo    • Execute sincronizacao manual
echo    • Verifique permissoes do banco
echo.

echo ✅ Analise concluida!
echo.
pause
