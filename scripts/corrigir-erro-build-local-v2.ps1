# Script para corrigir erros de build local - Versão 2 (Mais Robusta)
# Uso: .\scripts\corrigir-erro-build-local-v2.ps1

$ErrorActionPreference = "Stop"

Write-Host "🔧 Corrigindo Erros de Build Local (v2)" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

try {
    $syncRepoPath = "sync\src\main\java\com\example\gestaobilhares\sync\SyncRepository.kt"
    
    if (-not (Test-Path $syncRepoPath)) {
        Write-Host "❌ Arquivo não encontrado: $syncRepoPath" -ForegroundColor Red
        Write-Host "💡 Verifique se você está na raiz do projeto" -ForegroundColor Yellow
        exit 1
    }
    
    Write-Host "📝 Lendo arquivo: $syncRepoPath" -ForegroundColor Yellow
    
    # Ler o arquivo linha por linha (mais seguro)
    $lines = Get-Content $syncRepoPath -Encoding UTF8
    $content = $lines -join "`n"
    $modified = $false
    $newLines = New-Object System.Collections.ArrayList
    
    # 1. Verificar e adicionar import DateUtils se necessário
    $hasDateUtilsImport = $false
    $importInserted = $false
    
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        
        # Verificar se já tem o import
        if ($line -match "import com\.example\.gestaobilhares\.core\.utils\.DateUtils") {
            $hasDateUtilsImport = $true
        }
        
        # Adicionar import após outros imports do core.utils
        if (-not $importInserted -and $hasDateUtilsImport -eq $false) {
            if ($line -match "import com\.example\.gestaobilhares\.core\.utils\.") {
                [void]$newLines.Add($line)
                [void]$newLines.Add("import com.example.gestaobilhares.core.utils.DateUtils")
                $importInserted = $true
                $modified = $true
                Write-Host "✅ Import DateUtils adicionado" -ForegroundColor Green
                continue
            }
        }
        
        [void]$newLines.Add($line)
    }
    
    # Se não encontrou lugar para inserir, adicionar após package
    if (-not $hasDateUtilsImport -and -not $importInserted) {
        $newContent = New-Object System.Collections.ArrayList
        for ($i = 0; $i -lt $newLines.Count; $i++) {
            [void]$newContent.Add($newLines[$i])
            if ($newLines[$i] -match "^package com\.example\.gestaobilhares\.sync$") {
                [void]$newContent.Add("")
                [void]$newContent.Add("import com.example.gestaobilhares.core.utils.DateUtils")
                $modified = $true
                Write-Host "✅ Import DateUtils adicionado após package" -ForegroundColor Green
            }
        }
        $newLines = $newContent
    }
    
    # 2. Corrigir referências converterTimestampParaDate sem prefixo
    $content = $newLines -join "`n"
    $fixedContent = $content
    
    # Procurar por linhas com converterTimestampParaDate sem DateUtils.
    $pattern = '([^\.\s])\s+converterTimestampParaDate\('
    if ($fixedContent -match $pattern) {
        Write-Host "⚠️  Corrigindo referências converterTimestampParaDate..." -ForegroundColor Yellow
        # Substituir apenas as que não têm DateUtils. antes
        $fixedContent = $fixedContent -replace '([^\.])\s+converterTimestampParaDate\(', '$1 DateUtils.converterTimestampParaDate('
        $modified = $true
    }
    
    # 3. Verificar se entityToMap está definida
    $hasEntityToMap = $fixedContent -match 'private\s+fun\s+<T>\s+entityToMap\s*\('
    
    if (-not $hasEntityToMap) {
        Write-Host "⚠️  Função entityToMap não encontrada, adicionando..." -ForegroundColor Yellow
        
        # Encontrar onde inserir (após gson)
        $inserted = $false
        $finalLines = $fixedContent -split "`n"
        $resultLines = New-Object System.Collections.ArrayList
        
        for ($i = 0; $i -lt $finalLines.Count; $i++) {
            [void]$resultLines.Add($finalLines[$i])
            
            # Inserir após a definição do gson
            if (-not $inserted -and $finalLines[$i] -match 'private\s+val\s+gson:\s+Gson') {
                # Encontrar o fim do bloco lazy
                $j = $i + 1
                while ($j -lt $finalLines.Count -and $finalLines[$j] -notmatch '^\s*\}') {
                    [void]$resultLines.Add($finalLines[$j])
                    $j++
                }
                if ($j -lt $finalLines.Count) {
                    [void]$resultLines.Add($finalLines[$j])
                }
                
                # Adicionar função entityToMap
                [void]$resultLines.Add("")
                [void]$resultLines.Add("    /**")
                [void]$resultLines.Add("     * Converte entidade para Map para Firestore.")
                [void]$resultLines.Add("     */")
                [void]$resultLines.Add("    private fun <T> entityToMap(entity: T): MutableMap<String, Any> {")
                [void]$resultLines.Add("        val json = gson.toJson(entity)")
                [void]$resultLines.Add("        @Suppress(\"UNCHECKED_CAST\")")
                [void]$resultLines.Add("        val map = gson.fromJson(json, Map::class.java) as? Map<String, Any> ?: emptyMap()")
                [void]$resultLines.Add("        return map.mapKeys { it.key.toString() }.mapValues { entry ->")
                [void]$resultLines.Add("            val value = entry.value")
                [void]$resultLines.Add("            when {")
                [void]$resultLines.Add("                value is Date -> value.time")
                [void]$resultLines.Add("                value is java.time.LocalDateTime -> value.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()")
                [void]$resultLines.Add("                else -> value")
                [void]$resultLines.Add("            }")
                [void]$resultLines.Add("        }.toMutableMap()")
                [void]$resultLines.Add("    }")
                
                # Pular linhas já processadas
                $i = $j
                $inserted = $true
                $modified = $true
            }
        }
        
        if ($inserted) {
            $fixedContent = $resultLines -join "`n"
            Write-Host "✅ Função entityToMap adicionada" -ForegroundColor Green
        }
    }
    
    # 4. Corrigir referências entityToMap sem this.
    if ($fixedContent -match '([^\.])\s+entityToMap\(') {
        Write-Host "⚠️  Corrigindo referências entityToMap..." -ForegroundColor Yellow
        $fixedContent = $fixedContent -replace '([^\.])\s+entityToMap\(', '$1 this.entityToMap('
        $modified = $true
    }
    
    # Salvar se modificado
    if ($modified) {
        Write-Host ""
        Write-Host "💾 Salvando correções..." -ForegroundColor Green
        
        # Fazer backup
        $backupPath = "$syncRepoPath.backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
        try {
            Copy-Item $syncRepoPath $backupPath -ErrorAction Stop
            Write-Host "   ✅ Backup criado: $backupPath" -ForegroundColor Gray
        } catch {
            Write-Host "   ⚠️  Não foi possível criar backup: $_" -ForegroundColor Yellow
        }
        
        # Salvar arquivo corrigido
        try {
            $fullPath = (Resolve-Path $syncRepoPath).Path
            [System.IO.File]::WriteAllText($fullPath, $fixedContent, [System.Text.Encoding]::UTF8)
            Write-Host "   ✅ Arquivo corrigido e salvo!" -ForegroundColor Green
        } catch {
            Write-Host "   ❌ Erro ao salvar arquivo: $_" -ForegroundColor Red
            throw
        }
    } else {
        Write-Host "✅ Nenhuma correção necessária - arquivo já está correto" -ForegroundColor Green
    }
    
} catch {
    Write-Host ""
    Write-Host "❌ Erro ao processar: $_" -ForegroundColor Red
    Write-Host "   Linha: $($_.InvocationInfo.ScriptLineNumber)" -ForegroundColor Gray
    Write-Host "   Comando: $($_.InvocationInfo.Line)" -ForegroundColor Gray
    exit 1
}

Write-Host ""
Write-Host "════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "💡 Próximos passos:" -ForegroundColor Yellow
Write-Host "   1. Execute o build: .\gradlew.bat compileDebugKotlin" -ForegroundColor Gray
Write-Host "   2. Se ainda houver erros, sincronize: git pull origin main" -ForegroundColor Gray
Write-Host ""
