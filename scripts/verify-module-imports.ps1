<# verify-module-imports.ps1
   -------------------------
   Verifica se há imports quebrados ou referências aos pacotes antigos do :app
#>
[CmdletBinding()]
param(
    [string[]]$Targets = @("app","data","sync","ui")
)

Write-Host "=== VERIFICACAO DE IMPORTS NOS MODULOS ===" -ForegroundColor Cyan
Write-Host ""

$issues = @()

foreach ($target in $Targets) {
    if (-Not (Test-Path $target)) {
        Write-Warning "Diretorio '$target' nao encontrado, ignorando."
        continue
    }

    Write-Host "Verificando: $target" -ForegroundColor Yellow
    
    Get-ChildItem -Path $target -Recurse -Include *.kt |
        Where-Object { -Not $_.PSIsContainer } |
        ForEach-Object {
            $content = Get-Content -Raw -Encoding UTF8 -LiteralPath $_.FullName
            $relativePath = $_.FullName.Replace((Get-Location).Path + "\", "")
            
            # Verifica imports antigos do :app que deveriam apontar para módulos
            if ($content -match "import com\.example\.gestaobilhares\.data\." -and $target -ne "data") {
                # OK - usando módulo :data
            }
            elseif ($content -match "import com\.example\.gestaobilhares\.sync\." -and $target -ne "sync") {
                # OK - usando módulo :sync
            }
            elseif ($content -match "import com\.example\.gestaobilhares\.ui\." -and $target -ne "ui") {
                # OK - usando módulo :ui
            }
            elseif ($content -match "import com\.example\.gestaobilhares\.core\.utils\.") {
                # OK - usando módulo :core
            }
            elseif ($content -match "import com\.example\.gestaobilhares\.utils\.") {
                $issues += "ANTIGO: $relativePath - usa pacote antigo com.example.gestaobilhares.utils"
            }
        }
}

Write-Host ""
if ($issues.Count -eq 0) {
    Write-Host "=== SUCESSO: Nenhum problema encontrado! ===" -ForegroundColor Green
} else {
    Write-Host "=== PROBLEMAS ENCONTRADOS: ===" -ForegroundColor Red
    foreach ($issue in $issues) {
        Write-Host "  $issue" -ForegroundColor Yellow
    }
}

