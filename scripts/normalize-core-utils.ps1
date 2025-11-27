<# normalize-core-utils.ps1
   ------------------------
   Substitui todas as referências antigas para `com.example.gestaobilhares.utils`
   pelo novo pacote `com.example.gestaobilhares.core.utils`.

   Uso:
       .\scripts\normalize-core-utils.ps1               # usa os diretórios padrão
       .\scripts\normalize-core-utils.ps1 -Targets app,data
       .\scripts\normalize-core-utils.ps1 -Preview      # mostra diff em tela
#>
[CmdletBinding()]
param(
    [string[]]$Targets = @("app","data","sync","ui"),
    [switch]$Preview
)

$old = "com.example.gestaobilhares.utils."
$new = "com.example.gestaobilhares.core.utils."

function Update-File {
    param([string]$Path)

    $content = Get-Content -Raw -Encoding UTF8 -LiteralPath $Path
    if ($content.Contains($old)) {
        $updated = $content.Replace($old, $new)
        if ($Preview) {
            Write-Host "---- PREVIEW: $Path" -ForegroundColor Yellow
            $diff = Compare-Object ($content -split "`n") ($updated -split "`n") -SyncWindow 2
            $diff | ForEach-Object {
                $color = if ($_.SideIndicator -eq "=>") { "Green" } else { "Red" }
                Write-Host ("{0} {1}" -f $_.SideIndicator, $_.InputObject) -ForegroundColor $color
            }
        } else {
            $updated | Set-Content -Encoding UTF8 -LiteralPath $Path
            Write-Host "Atualizado: $Path" -ForegroundColor Green
        }
    }
}

foreach ($target in $Targets) {
    if (-Not (Test-Path $target)) {
        Write-Warning "Diretório '$target' não encontrado, ignorando."
        continue
    }

    Get-ChildItem -Path $target -Recurse -Include *.kt |
        Where-Object { -Not $_.PSIsContainer } |
        ForEach-Object { Update-File -Path $_.FullName }
}

if ($Preview) {
    Write-Host "`nModo PREVIEW: nenhum arquivo foi gravado." -ForegroundColor Yellow
}

