<# normalize-data-imports.ps1
   -------------------------
   Normaliza imports nos módulos para usar pacotes corretos do :data
   Substitui referências antigas do :app para o novo módulo :data
#>
[CmdletBinding()]
param(
    [string[]]$Targets = @("app","ui","sync"),
    [switch]$Preview
)

$replacements = @{
    "import com\.example\.gestaobilhares\.data\.entities\." = "import com.example.gestaobilhares.data.entities."
    "import com\.example\.gestaobilhares\.data\.dao\." = "import com.example.gestaobilhares.data.dao."
    "import com\.example\.gestaobilhares\.data\.repository\." = "import com.example.gestaobilhares.data.repository."
    "import com\.example\.gestaobilhares\.data\.database\." = "import com.example.gestaobilhares.data.database."
    "import com\.example\.gestaobilhares\.data\.factory\." = "import com.example.gestaobilhares.data.factory."
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

