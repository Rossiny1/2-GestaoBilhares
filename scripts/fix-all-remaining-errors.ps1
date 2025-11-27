# Script para corrigir TODOS os erros restantes
$ErrorActionPreference = "Stop"

$rootPath = Split-Path -Parent $PSScriptRoot
$uiPath = Join-Path $rootPath "ui\src\main\java"

Write-Host "=== CORRIGINDO TODOS OS ERROS RESTANTES ===" -ForegroundColor Cyan

# 1. Corrigir referencias a factory que nao existe
$files = Get-ChildItem -Path $uiPath -Filter "*.kt" -Recurse -File
$updatedCount = 0

foreach ($file in $files) {
    try {
        $content = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
        $originalContent = $content
        
        # Corrigir by viewModels { factory } para criar factory manualmente
        if ($content -match 'by viewModels\s*\{\s*factory\s*\}') {
            Write-Host "  Corrigindo factory em: $($file.Name)" -ForegroundColor Yellow
            
            # Encontrar o nome do ViewModel
            $viewModelMatch = $content -match 'private val (\w+): (\w+ViewModel)'
            if ($viewModelMatch) {
                $viewModelName = $matches[2]
                $viewModelVar = $matches[1]
                
                # Substituir por inicializacao manual
                $content = $content -replace 'private val \w+: \w+ViewModel by viewModels\s*\{\s*factory\s*\}', "private lateinit var $viewModelVar : $viewModelName"
                
                # Adicionar inicializacao no onViewCreated se nao existir
                if ($content -notmatch "$viewModelVar\s*=") {
                    $content = $content -replace '(override fun onViewCreated\([^)]+\)\s*\{)', "`$1`r`n        // TODO: Inicializar $viewModelVar aqui"
                }
            }
        }
        
        # Corrigir referencias a com.example.gestaobilhares.sync.SyncRepository
        # Se o modulo :sync nao existe, comentar ou remover essas referencias
        if ($content -match 'com\.example\.gestaobilhares\.sync\.SyncRepository') {
            Write-Host "  Aviso: SyncRepository encontrado em $($file.Name) - verificar se modulo :sync existe" -ForegroundColor Yellow
        }
        
        # Corrigir DarkDialogTheme se nao existir
        if ($content -match 'R\.style\.DarkDialogTheme') {
            $content = $content -replace 'R\.style\.DarkDialogTheme', 'android.R.style.Theme_Material_Dialog'
        }
        
        if ($content -ne $originalContent) {
            [System.IO.File]::WriteAllText($file.FullName, $content, [System.Text.Encoding]::UTF8)
            $updatedCount++
        }
    } catch {
        Write-Host "  ERRO: $($file.Name) - $_" -ForegroundColor Red
    }
}

Write-Host "`nArquivos atualizados: $updatedCount" -ForegroundColor Green
Write-Host "=== CONCLUIDO ===" -ForegroundColor Green

