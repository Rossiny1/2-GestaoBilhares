# Script para corrigir warnings comuns do Kotlin
# Parâmetros não utilizados -> renomear para _

Write-Host "🔧 Corrigindo warnings do Kotlin..." -ForegroundColor Yellow

# Lista de arquivos e correções
$corrections = @(
    @{
        File = "app/src/main/java/com/example/gestaobilhares/ui/clients/ClientDetailViewModel.kt"
        Old = "fun adicionarObservacao(_clienteId: Long, observacao: String)"
        New = "fun adicionarObservacao(_clienteId: Long, _observacao: String)"
    },
    @{
        File = "app/src/main/java/com/example/gestaobilhares/ui/clients/ClientDetailViewModel.kt"
        Old = "fun removerMesa(mesaId: Long)"
        New = "fun removerMesa(_mesaId: Long)"
    },
    @{
        File = "app/src/main/java/com/example/gestaobilhares/ui/clients/ClientDetailViewModel.kt"
        Old = "fun obterHistoricoAcertos(acertoId: Long)"
        New = "fun obterHistoricoAcertos(_acertoId: Long)"
    },
    @{
        File = "app/src/main/java/com/example/gestaobilhares/ui/clients/ClientListFragment.kt"
        Old = "val database = AppDatabase.getInstance(requireContext())"
        New = "val _database = AppDatabase.getInstance(requireContext())"
    },
    @{
        File = "app/src/main/java/com/example/gestaobilhares/ui/clients/ClientListFragment.kt"
        Old = "private fun onBotaoInativoClick(_botaoInativo: View) {"
        New = "private fun onBotaoInativoClick(_botaoInativo: View) {"
    }
)

foreach ($correction in $corrections) {
    if (Test-Path $correction.File) {
        $content = Get-Content $correction.File -Raw
        if ($content -match [regex]::Escape($correction.Old)) {
            $content = $content -replace [regex]::Escape($correction.Old), $correction.New
            Set-Content $correction.File -Value $content -Encoding UTF8
            Write-Host "✅ Corrigido: $($correction.File)" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Não encontrado: $($correction.Old)" -ForegroundColor Yellow
        }
    } else {
        Write-Host "❌ Arquivo não encontrado: $($correction.File)" -ForegroundColor Red
    }
}

Write-Host "🎉 Correções de warnings concluídas!" -ForegroundColor Green
