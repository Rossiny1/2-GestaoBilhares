# 🔍 Script para Analisar Logs do App Android
# Uso: .\scripts\analisar_logs_app_corrigido.ps1

Write-Host "🔍 Analisando logs do app Android..." -ForegroundColor Green

# Verificar se arquivo de logs existe
if (-not (Test-Path "logs_app_real.txt")) {
    Write-Host "❌ Arquivo logs_app_real.txt não encontrado" -ForegroundColor Red
    Write-Host "📋 Execute primeiro: .\scripts\iniciar_captura_logs.ps1" -ForegroundColor Yellow
    exit 1
}

# Ler logs
$logs = Get-Content "logs_app_real.txt"

Write-Host "📊 Estatísticas dos logs:" -ForegroundColor Blue
Write-Host "   Total de linhas: $($logs.Count)" -ForegroundColor White
Write-Host ""

# Extrair erros PERMISSION_DENIED
$permissionErrors = $logs | Select-String -Pattern "PERMISSION_DENIED|Missing|insufficient" -Context 5

Write-Host "❌ Erros PERMISSION_DENIED encontrados: $($permissionErrors.Count)" -ForegroundColor Red

if ($permissionErrors.Count -gt 0) {
    Write-Host ""
    Write-Host "📋 Detalhes dos erros:" -ForegroundColor Yellow
    
    for ($i = 0; $i -lt $permissionErrors.Count; $i++) {
        Write-Host ""
        Write-Host "--- ERRO $($i + 1) ---" -ForegroundColor Red
        Write-Host $permissionErrors[$i] -ForegroundColor White
    }
    
    # Salvar erros filtrados
    $permissionErrors | Out-File -FilePath "erros_permission_denied.txt" -Encoding UTF8
    Write-Host ""
    Write-Host "💾 Erros salvos em: erros_permission_denied.txt" -ForegroundColor Green
}
else {
    Write-Host "✅ Nenhum erro PERMISSION_DENIED encontrado" -ForegroundColor Green
}

# Extrair paths de documentos
$documentPaths = $logs | Select-String -Pattern "projects/.*/documents/.*" | ForEach-Object { $_.Line }

if ($documentPaths.Count -gt 0) {
    Write-Host ""
    Write-Host "📂 Paths de documentos encontrados:" -ForegroundColor Yellow
    
    $uniquePaths = $documentPaths | Select-Object -Unique
    foreach ($path in $uniquePaths) {
        Write-Host "   $path" -ForegroundColor White
    }
    
    # Salvar paths
    $uniquePaths | Out-File -FilePath "document_paths.txt" -Encoding UTF8
    Write-Host "💾 Paths salvos em: document_paths.txt" -ForegroundColor Green
}

# Extrair operações Firestore
$firestoreOps = $logs | Select-String -Pattern "FirebaseFirestore/D" | ForEach-Object { $_.Line }

if ($firestoreOps.Count -gt 0) {
    Write-Host ""
    Write-Host "🔥 Operações Firestore:" -ForegroundColor Yellow
    
    $opsGrouped = $firestoreOps | Group-Object { $_.Split()[2] }
    foreach ($group in $opsGrouped) {
        Write-Host "   $($group.Name): $($group.Count) operações" -ForegroundColor White
    }
}

# Verificar se colaboradores funcionam (controle)
$collaboratorOps = $logs | Select-String -Pattern "colaboradores" | ForEach-Object { $_.Line }

Write-Host ""
Write-Host "👥 Operações de Colaboradores (controle):" -ForegroundColor Yellow
if ($collaboratorOps.Count -gt 0) {
    Write-Host "   ✅ $($collaboratorOps.Count) operações encontradas" -ForegroundColor Green
    foreach ($op in $collaboratorOps) {
        Write-Host "   $op" -ForegroundColor White
    }
}
else {
    Write-Host "   ❌ Nenhuma operação de colaboradores encontrada" -ForegroundColor Red
}

Write-Host ""
Write-Host "🎯 Análise concluída!" -ForegroundColor Green
Write-Host "📋 Próximo passo: Corrigir Security Rules baseado nos erros encontrados" -ForegroundColor Cyan
