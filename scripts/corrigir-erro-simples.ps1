# Script SIMPLES para corrigir erros - apenas adiciona imports e função se faltarem
# Uso: .\scripts\corrigir-erro-simples.ps1

$ErrorActionPreference = "Continue"

Write-Host "🔧 Correção Simples de Erros" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

$file = "sync\src\main\java\com\example\gestaobilhares\sync\SyncRepository.kt"

if (-not (Test-Path $file)) {
    Write-Host "❌ Arquivo não encontrado!" -ForegroundColor Red
    exit 1
}

Write-Host "📝 Verificando: $file" -ForegroundColor Yellow

# Ler arquivo
$content = Get-Content $file -Raw
$original = $content
$changed = $false

# 1. Adicionar import DateUtils se não existir
if ($content -notlike "*import com.example.gestaobilhares.core.utils.DateUtils*") {
    Write-Host "➕ Adicionando import DateUtils..." -ForegroundColor Yellow
    
    # Procurar linha com outro import do core.utils
    if ($content -match "(import com\.example\.gestaobilhares\.core\.utils\.[^\r\n]+)") {
        $content = $content -replace "($matches[1])", "`$1`r`nimport com.example.gestaobilhares.core.utils.DateUtils"
    } else {
        # Adicionar após package
        $content = $content -replace "(package com\.example\.gestaobilhares\.sync)", "`$1`r`n`r`nimport com.example.gestaobilhares.core.utils.DateUtils"
    }
    $changed = $true
}

# 2. Adicionar função entityToMap se não existir
if ($content -notlike "*private fun <T> entityToMap*") {
    Write-Host "➕ Adicionando função entityToMap..." -ForegroundColor Yellow
    
    $func = @"

    private fun <T> entityToMap(entity: T): MutableMap<String, Any> {
        val json = gson.toJson(entity)
        @Suppress("UNCHECKED_CAST")
        val map = gson.fromJson(json, Map::class.java) as? Map<String, Any> ?: emptyMap()
        return map.mapKeys { it.key.toString() }.mapValues { entry ->
            val value = entry.value
            when {
                value is Date -> value.time
                value is java.time.LocalDateTime -> value.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                else -> value
            }
        }.toMutableMap()
    }
"@
    
    # Inserir após gson
    if ($content -match "(private val gson: Gson by lazy[^}]+})") {
        $content = $content -replace "($matches[1])", "`$1`r`n$func"
    } else {
        # Inserir após abertura da classe
        $content = $content -replace "(class SyncRepository[^\{]+\{)", "`$1`r`n$func"
    }
    $changed = $true
}

# Salvar se mudou
if ($changed) {
    # Backup
    $backup = "$file.backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
    Copy-Item $file $backup
    Write-Host "💾 Backup: $backup" -ForegroundColor Gray
    
    # Salvar
    $content | Set-Content $file -Encoding UTF8
    Write-Host "✅ Arquivo corrigido!" -ForegroundColor Green
} else {
    Write-Host "✅ Nada a corrigir" -ForegroundColor Green
}

Write-Host ""
Write-Host "💡 Execute: .\gradlew.bat compileDebugKotlin" -ForegroundColor Yellow
Write-Host ""
