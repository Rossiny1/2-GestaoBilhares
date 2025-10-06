# Script para corrigir referencias restantes ao DAO removido
# Executar: .\fix-final-references.ps1

Write-Host "CORRIGINDO REFERENCIAS RESTANTES AO DAO REMOVIDO" -ForegroundColor Yellow

# Lista de arquivos que ainda referenciam o DAO removido
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

Write-Host "1. Removendo referencias ao DAO removido..." -ForegroundColor Cyan

foreach ($file in $files) {
    if (Test-Path $file) {
        Write-Host "Corrigindo: $file" -ForegroundColor Green
        
        # Ler conteudo do arquivo
        $content = Get-Content $file -Raw -Encoding UTF8
        
        # Remover referencias ao DAO removido
        $content = $content -replace 'procura├º├úoRepresentanteDao', 'null'
        $content = $content -replace 'procuraçãoRepresentanteDao', 'null'
        
        # Remover argumentos extras do construtor AppRepository
        $content = $content -replace 'AppRepository\([^)]*procura[^)]*\)', 'AppRepository(clienteDao, acertoDao, mesaDao, rotaDao, despesaDao, colaboradorDao, cicloAcertoDao, acertoMesaDao, contratoLocacaoDao, aditivoContratoDao, assinaturaRepresentanteLegalDao, logAuditoriaAssinaturaDao)'
        
        # Salvar arquivo corrigido
        Set-Content $file -Value $content -Encoding UTF8
    }
}

Write-Host "2. Verificando AppRepository..." -ForegroundColor Cyan

# Verificar se AppRepository ainda tem referencias problemáticas
$appRepoFile = "app\src\main\java\com\example\gestaobilhares\data\repository\AppRepository.kt"
if (Test-Path $appRepoFile) {
    $content = Get-Content $appRepoFile -Raw -Encoding UTF8
    
    # Remover qualquer referencia restante ao DAO
    $content = $content -replace 'procura├º├úoRepresentanteDao', 'null'
    $content = $content -replace 'procuraçãoRepresentanteDao', 'null'
    
    Set-Content $appRepoFile -Value $content -Encoding UTF8
    Write-Host "AppRepository corrigido" -ForegroundColor Green
}

Write-Host "CORRECAO FINAL CONCLUIDA!" -ForegroundColor Green
Write-Host "Agora execute: ./gradlew assembleDebug --no-daemon" -ForegroundColor Yellow
