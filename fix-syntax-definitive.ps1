# Script definitivo para corrigir sintaxe quebrada
# Executar: .\fix-syntax-definitive.ps1

Write-Host "CORRECAO DEFINITIVA - RECONSTRUINDO SINTAXE" -ForegroundColor Red

# Lista de arquivos problemáticos
$files = @(
    "app\src\main\java\com\example\gestaobilhares\ui\settlement\SettlementDetailFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\settlement\SettlementFragment.kt"
)

Write-Host "1. Reconstruindo construtores AppRepository..." -ForegroundColor Cyan

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

Write-Host "2. Verificando sintaxe..." -ForegroundColor Cyan

# Verificar se há outras referencias quebradas
foreach ($file in $files) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw -Encoding UTF8
        
        # Corrigir qualquer referencia quebrada restante
        $content = $content -replace 'AppRepository\([^)]*clienteDao[^)]*\)', 'AppRepository(database.clienteDao(), database.acertoDao(), database.mesaDao(), database.rotaDao(), database.despesaDao(), database.colaboradorDao(), database.cicloAcertoDao(), database.acertoMesaDao(), database.contratoLocacaoDao(), database.aditivoContratoDao(), database.assinaturaRepresentanteLegalDao(), database.logAuditoriaAssinaturaDao())'
        
        Set-Content $file -Value $content -Encoding UTF8
    }
}

Write-Host "CORRECAO DEFINITIVA CONCLUIDA!" -ForegroundColor Green
Write-Host "Agora execute: ./gradlew assembleDebug --no-daemon" -ForegroundColor Yellow
