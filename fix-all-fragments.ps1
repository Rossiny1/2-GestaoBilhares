# Script para corrigir TODOS os Fragments com problemas de AppRepository
# Corrige ordem de parâmetros e adiciona DAOs faltando

Write-Host "🔧 CORREÇÃO EM MASSA - TODOS OS FRAGMENTS" -ForegroundColor Yellow

# Lista de arquivos que precisam ser corrigidos
$files = @(
    "app/src/main/java/com/example/gestaobilhares/ui/mesas/MesasDepositoFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/mesas/NovaReformaFragment.kt", 
    "app/src/main/java/com/example/gestaobilhares/ui/mesas/RotaMesasFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/metas/MetaCadastroFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/metas/MetasFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/reports/ClosureReportFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/routes/ClientSelectionDialog.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/routes/RoutesFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/routes/TransferClientDialog.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/settlement/SettlementDetailFragment.kt"
)

# Padrão de correção para AppRepository
$oldPattern = @"
AppRepository(
    database.clienteDao(),
    database.acertoDao(),
    database.mesaDao(),
    database.rotaDao(),
    database.despesaDao(),
    database.colaboradorDao(),
    database.cicloAcertoDao(),
    database.acertoMesaDao(),
    database.contratoLocacaoDao(),
    database.aditivoContratoDao(),
    database.assinaturaRepresentanteLegalDao(),
    database.logAuditoriaAssinaturaDao(),
    database.panoEstoqueDao()
)
"@

$newPattern = @"
AppRepository(
    database.clienteDao(),
    database.acertoDao(),
    database.mesaDao(),
    database.rotaDao(),
    database.despesaDao(),
    database.categoriaDespesaDao(),
    database.tipoDespesaDao(),
    database.colaboradorDao(),
    database.cicloAcertoDao(),
    database.acertoMesaDao(),
    database.contratoLocacaoDao(),
    database.aditivoContratoDao(),
    database.assinaturaRepresentanteLegalDao(),
    database.logAuditoriaAssinaturaDao(),
    database.panoEstoqueDao(),
    database.historicoManutencaoVeiculoDao(),
    database.historicoCombustivelVeiculoDao(),
    database.historicoManutencaoMesaDao()
)
"@

$successCount = 0
$errorCount = 0

foreach ($file in $files) {
    if (Test-Path $file) {
        try {
            Write-Host "🔧 Corrigindo: $file" -ForegroundColor Cyan
            
            $content = Get-Content $file -Raw -Encoding UTF8
            
            if ($content -match "AppRepository\(") {
                $newContent = $content -replace [regex]::Escape($oldPattern), $newPattern
                
                if ($newContent -ne $content) {
                    Set-Content $file -Value $newContent -Encoding UTF8
                    Write-Host "✅ Corrigido: $file" -ForegroundColor Green
                    $successCount++
                } else {
                    Write-Host "⚠️  Nenhuma alteração necessária: $file" -ForegroundColor Yellow
                }
            } else {
                Write-Host "⚠️  AppRepository não encontrado: $file" -ForegroundColor Yellow
            }
        }
        catch {
            Write-Host "❌ Erro ao corrigir $file : $($_.Exception.Message)" -ForegroundColor Red
            $errorCount++
        }
    } else {
        Write-Host "❌ Arquivo não encontrado: $file" -ForegroundColor Red
        $errorCount++
    }
}

Write-Host "`n📊 RESUMO DA CORREÇÃO:" -ForegroundColor Yellow
Write-Host "✅ Arquivos corrigidos: $successCount" -ForegroundColor Green
Write-Host "❌ Erros: $errorCount" -ForegroundColor Red
Write-Host "📁 Total processados: $($files.Count)" -ForegroundColor Cyan

if ($errorCount -eq 0) {
    Write-Host "`n🎉 TODOS OS FRAGMENTS CORRIGIDOS COM SUCESSO!" -ForegroundColor Green
} else {
    Write-Host "`n⚠️  Alguns arquivos tiveram problemas. Verifique os erros acima." -ForegroundColor Yellow
}
