# Script para corrigir erros de build local relacionados a converterTimestampParaDate e entityToMap
# Uso: .\scripts\corrigir-erro-build-local.ps1

$ErrorActionPreference = "Continue"

Write-Host "🔧 Corrigindo Erros de Build Local" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

$syncRepoPath = "sync\src\main\java\com\example\gestaobilhares\sync\SyncRepository.kt"

if (-not (Test-Path $syncRepoPath)) {
    Write-Host "❌ Arquivo não encontrado: $syncRepoPath" -ForegroundColor Red
    exit 1
}

Write-Host "📝 Verificando arquivo: $syncRepoPath" -ForegroundColor Yellow

# Ler o arquivo
$content = Get-Content $syncRepoPath -Raw -Encoding UTF8
$originalContent = $content
$modified = $false

# 1. Verificar se import DateUtils existe
if ($content -notmatch "import com\.example\.gestaobilhares\.core\.utils\.DateUtils") {
    Write-Host "⚠️  Import DateUtils não encontrado, adicionando..." -ForegroundColor Yellow
    
    # Adicionar import após outros imports do core
    if ($content -match "(import com\.example\.gestaobilhares\.core\.utils\.[^\r\n]+)") {
        $content = $content -replace "($matches[1])", "`$1`r`nimport com.example.gestaobilhares.core.utils.DateUtils"
        $modified = $true
    } else {
        # Adicionar após package
        $content = $content -replace "(package com\.example\.gestaobilhares\.sync)", "`$1`r`n`r`nimport com.example.gestaobilhares.core.utils.DateUtils"
        $modified = $true
    }
}

# 2. Corrigir referências sem prefixo DateUtils
$patterns = @(
    @{ Pattern = "([^\.])\bconverterTimestampParaDate\("; Replacement = "`$1DateUtils.converterTimestampParaDate(" }
)

foreach ($pattern in $patterns) {
    if ($content -match $pattern.Pattern) {
        Write-Host "⚠️  Corrigindo referências sem prefixo DateUtils..." -ForegroundColor Yellow
        $content = $content -replace $pattern.Pattern, $pattern.Replacement
        $modified = $true
    }
}

# 3. Verificar se entityToMap está definida
if ($content -notmatch "private fun <T> entityToMap\(entity: T\)") {
    Write-Host "⚠️  Função entityToMap não encontrada, adicionando..." -ForegroundColor Yellow
    
    # Adicionar função após a definição do gson
    $entityToMapFunction = @"

    /**
     * Converte entidade para Map para Firestore.
     * Similar ao método do BaseSyncHandler, mas adaptado para SyncRepository.
     */
    private fun <T> entityToMap(entity: T): MutableMap<String, Any> {
        val json = gson.toJson(entity)
        @Suppress("UNCHECKED_CAST")
        val map = gson.fromJson(json, Map::class.java) as? Map<String, Any> ?: emptyMap()
        
        return map.mapKeys { it.key.toString() }.mapValues { entry ->
            val key = entry.key.lowercase()
            val value = entry.value
            
            when {
                value is Date -> value.time
                value is java.time.LocalDateTime -> value.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                else -> value
            }
        }.toMutableMap()
    }
"@
    
    # Tentar inserir após a definição do gson
    if ($content -match "(private val gson: Gson by lazy[^}]+})") {
        $content = $content -replace "($matches[1])", "`$1`r`n`r`n$entityToMapFunction"
        $modified = $true
    } else {
        # Inserir após a classe começar
        if ($content -match "(class SyncRepository[^\{]+\{[\r\n]+)") {
            $content = $content -replace "($matches[1])", "`$1`r`n$entityToMapFunction`r`n"
            $modified = $true
        }
    }
}

# 4. Corrigir referências entityToMap sem this.
if ($content -match "([^\.])\bentityToMap\(") {
    Write-Host "⚠️  Corrigindo referências entityToMap..." -ForegroundColor Yellow
    $content = $content -replace "([^\.])\bentityToMap\(", "`$1this.entityToMap("
    $modified = $true
}

# Salvar se modificado
if ($modified) {
    Write-Host "💾 Salvando correções..." -ForegroundColor Green
    
    # Fazer backup
    $backupPath = "$syncRepoPath.backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
    Copy-Item $syncRepoPath $backupPath
    Write-Host "   Backup criado: $backupPath" -ForegroundColor Gray
    
    # Salvar arquivo corrigido
    [System.IO.File]::WriteAllText((Resolve-Path $syncRepoPath).Path, $content, [System.Text.Encoding]::UTF8)
    
    Write-Host "✅ Arquivo corrigido!" -ForegroundColor Green
} else {
    Write-Host "✅ Nenhuma correção necessária" -ForegroundColor Green
}

Write-Host ""
Write-Host "════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "💡 Próximos passos:" -ForegroundColor Yellow
Write-Host "   1. Execute o build novamente: .\gradlew.bat compileDebugKotlin" -ForegroundColor Gray
Write-Host "   2. Se ainda houver erros, sincronize via Git: git pull origin main" -ForegroundColor Gray
Write-Host ""
