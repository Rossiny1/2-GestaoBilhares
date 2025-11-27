<# normalize-ui-imports.ps1
   -------------------------
   Normaliza imports para usar pacotes corretos do :ui
   Substitui referências antigas do :app para o novo módulo :ui
#>
[CmdletBinding()]
param(
    [string[]]$Targets = @("app"),
    [switch]$Preview
)

$replacements = @{
    "import com\.example\.gestaobilhares\.ui\." = "import com.example.gestaobilhares.ui."
}

function Update-File {
    param([string]$Path)

    $content = Get-Content -Raw -Encoding UTF8 -LiteralPath $Path
    $updated = $content
    $changed = $false

    foreach ($pattern in $replacements.Keys) {
        if ($content -match $pattern) {
            $updated = $updated -replace $pattern, $replacements[$pattern]
            $changed = $true
        }
    }

    if ($changed) {
        if ($Preview) {
            Write-Host "---- PREVIEW: $Path" -ForegroundColor Yellow
        } else {
            $updated | Set-Content -Encoding UTF8 -LiteralPath $Path
            Write-Host "Atualizado: $Path" -ForegroundColor Green
        }
    }
}

foreach ($target in $Targets) {
    if (-Not (Test-Path $target)) {
        Write-Warning "Diretorio '$target' nao encontrado, ignorando."
        continue
    }

    Get-ChildItem -Path $target -Recurse -Include *.kt |
        Where-Object { -Not $_.PSIsContainer } |
        ForEach-Object { Update-File -Path $_.FullName }
}

if ($Preview) {
    Write-Host "`nModo PREVIEW: nenhum arquivo foi gravado." -ForegroundColor Yellow
}

