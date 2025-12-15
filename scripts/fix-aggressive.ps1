# Script agressivo para corrigir referencias persistentes
# Executar: .\fix-aggressive.ps1

Write-Host "CORRECAO AGRESSIVA - REMOVENDO REFERENCIAS PERSISTENTES" -ForegroundColor Red

# Lista de arquivos problemáticos
$files = @(
    "app\src\main\java\com\example\gestaobilhares\ui\colaboradores\ColaboradorManagementFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\colaboradores\ColaboradorMetasFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\colaboradores\ColaboradorRegisterFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\contracts\AditivoSignatureFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\contracts\ContractManagementFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\contracts\SignatureCaptureFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\cycles\CycleClientsFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\cycles\CycleExpensesFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\cycles\CycleManagementFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\expenses\ExpenseRegisterFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\mesas\MesasDepositoFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\mesas\VendaMesaDialog.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\settlement\SettlementDetailFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\settlement\SettlementFragment.kt"
)

Write-Host "1. Removendo linhas problemáticas..." -ForegroundColor Cyan

foreach ($file in $files) {
    if (Test-Path $file) {
        Write-Host "Corrigindo: $file" -ForegroundColor Green
        
        # Ler conteudo do arquivo
        $content = Get-Content $file -Raw -Encoding UTF8
        
        # Remover linhas que contem referencias ao DAO
        $lines = $content -split "`n"
        $newLines = @()
        
        foreach ($line in $lines) {
            # Pular linhas que contem referencias problemáticas
            if ($line -match "procura" -and $line -match "Dao") {
                Write-Host "  Removendo linha: $line" -ForegroundColor Yellow
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

Write-Host "2. Corrigindo construtores AppRepository..." -ForegroundColor Cyan

foreach ($file in $files) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw -Encoding UTF8
        
        # Corrigir construtores AppRepository
        $content = $content -replace 'AppRepository\([^)]*\)', 'AppRepository(clienteDao, acertoDao, mesaDao, rotaDao, despesaDao, colaboradorDao, cicloAcertoDao, acertoMesaDao, contratoLocacaoDao, aditivoContratoDao, assinaturaRepresentanteLegalDao, logAuditoriaAssinaturaDao)'
        
        Set-Content $file -Value $content -Encoding UTF8
    }
}

Write-Host "3. Verificando AppRepository..." -ForegroundColor Cyan

# Verificar se AppRepository ainda tem referencias problemáticas
$appRepoFile = "app\src\main\java\com\example\gestaobilhares\data\repository\AppRepository.kt"
if (Test-Path $appRepoFile) {
    $content = Get-Content $appRepoFile -Raw -Encoding UTF8
    
    # Remover qualquer referencia restante ao DAO
    $content = $content -replace 'procura.*Dao', ''
    $content = $content -replace 'procura.*RepresentanteDao', ''
    
    Set-Content $appRepoFile -Value $content -Encoding UTF8
    Write-Host "AppRepository corrigido" -ForegroundColor Green
}

Write-Host "CORRECAO AGRESSIVA CONCLUIDA!" -ForegroundColor Green
Write-Host "Agora execute: ./gradlew assembleDebug --no-daemon" -ForegroundColor Yellow
