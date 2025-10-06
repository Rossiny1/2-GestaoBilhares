# Script final para corrigir TODOS os arquivos com sintaxe quebrada
# Executar: .\fix-all-syntax-final.ps1

Write-Host "CORRECAO FINAL - TODOS OS ARQUIVOS COM SINTAXE QUEBRADA" -ForegroundColor Red

# Lista COMPLETA de arquivos problemáticos
$files = @(
    "app\src\main\java\com\example\gestaobilhares\ui\expenses\ExpenseRegisterFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\mesas\MesasDepositoFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\mesas\VendaMesaDialog.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\settlement\SettlementDetailFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\settlement\SettlementFragment.kt"
)

Write-Host "1. Corrigindo TODOS os arquivos com sintaxe quebrada..." -ForegroundColor Cyan

foreach ($file in $files) {
    if (Test-Path $file) {
        Write-Host "Corrigindo: $file" -ForegroundColor Green
        
        # Ler conteudo do arquivo
        $content = Get-Content $file -Raw -Encoding UTF8
        
        # Remover linhas quebradas e reconstruir
        $lines = $content -split "`n"
        $newLines = @()
        $skipNext = $false
        
        foreach ($line in $lines) {
            # Pular linhas quebradas
            if ($line -match "AppRepository\(" -and $line -match "clienteDao" -and $line -match "acertoDao") {
                Write-Host "  Reconstruindo construtor quebrado" -ForegroundColor Yellow
                $newLines += "            AppRepository("
                $newLines += "                database.clienteDao(),"
                $newLines += "                database.acertoDao(),"
                $newLines += "                database.mesaDao(),"
                $newLines += "                database.rotaDao(),"
                $newLines += "                database.despesaDao(),"
                $newLines += "                database.colaboradorDao(),"
                $newLines += "                database.cicloAcertoDao(),"
                $newLines += "                database.acertoMesaDao(),"
                $newLines += "                database.contratoLocacaoDao(),"
                $newLines += "                database.aditivoContratoDao(),"
                $newLines += "                database.assinaturaRepresentanteLegalDao(),"
                $newLines += "                database.logAuditoriaAssinaturaDao()"
                $newLines += "            )"
                $skipNext = $true
                continue
            }
            
            if ($skipNext -and $line -match "^\s*\)") {
                $skipNext = $false
                continue
            }
            
            if ($skipNext) {
                continue
            }
            
            $newLines += $line
        }
        
        # Reconstruir conteudo
        $newContent = $newLines -join "`n"
        
        # Salvar arquivo corrigido
        Set-Content $file -Value $newContent -Encoding UTF8
    }
}

Write-Host "2. Verificando sintaxe em todos os arquivos..." -ForegroundColor Cyan

# Verificar se há outras referencias quebradas
foreach ($file in $files) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw -Encoding UTF8
        
        # Corrigir qualquer referencia quebrada restante
        $content = $content -replace 'AppRepository\([^)]*clienteDao[^)]*\)', 'AppRepository(database.clienteDao(), database.acertoDao(), database.mesaDao(), database.rotaDao(), database.despesaDao(), database.colaboradorDao(), database.cicloAcertoDao(), database.acertoMesaDao(), database.contratoLocacaoDao(), database.aditivoContratoDao(), database.assinaturaRepresentanteLegalDao(), database.logAuditoriaAssinaturaDao())'
        
        Set-Content $file -Value $content -Encoding UTF8
    }
}

Write-Host "3. Limpando referencias quebradas restantes..." -ForegroundColor Cyan

# Limpar qualquer referencia quebrada restante
foreach ($file in $files) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw -Encoding UTF8
        
        # Remover linhas que contem referencias quebradas
        $lines = $content -split "`n"
        $newLines = @()
        
        foreach ($line in $lines) {
            # Pular linhas que contem referencias quebradas
            if ($line -match "clienteDao" -and $line -match "acertoDao" -and $line -match "mesaDao" -and $line -match "rotaDao" -and $line -match "despesaDao" -and $line -match "colaboradorDao" -and $line -match "cicloAcertoDao" -and $line -match "acertoMesaDao" -and $line -match "contratoLocacaoDao" -and $line -match "aditivoContratoDao" -and $line -match "assinaturaRepresentanteLegalDao" -and $line -match "logAuditoriaAssinaturaDao") {
                Write-Host "  Removendo linha quebrada: $line" -ForegroundColor Yellow
                continue
            }
            
            $newLines += $line
        }
        
        # Reconstruir conteudo
        $newContent = $newLines -join "`n"
        
        # Salvar arquivo corrigido
        Set-Content $file -Value $newContent -Encoding UTF8
    }
}

Write-Host "CORRECAO FINAL CONCLUIDA!" -ForegroundColor Green
Write-Host "Agora execute: ./gradlew assembleDebug --no-daemon" -ForegroundColor Yellow
