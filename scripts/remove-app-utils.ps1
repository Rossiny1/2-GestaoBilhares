<# remove-app-utils.ps1
   --------------------
   Remove o diretório `app/src/main/java/com/example/gestaobilhares/utils`
   após a migração dos utilitários para o módulo :core.

   Uso:
       .\scripts\remove-app-utils.ps1
       .\scripts\remove-app-utils.ps1 -WhatIf   # apenas mostra o que seria removido
#>
[CmdletBinding(SupportsShouldProcess=$true)]
param()

$utilsPath = "app/src/main/java/com/example/gestaobilhares/utils"

if (-Not (Test-Path $utilsPath)) {
    Write-Warning "Diretório '$utilsPath' não existe ou já foi removido."
    return
}

$files = Get-ChildItem -Path $utilsPath -Recurse -File
Write-Host ("Encontrados {0} arquivos para remoção." -f $files.Count)

foreach ($file in $files) {
    if ($PSCmdlet.ShouldProcess($file.FullName, "Remove-Item")) {
        Remove-Item -LiteralPath $file.FullName -Force -WhatIf:$WhatIfPreference
    }
}

if ($PSCmdlet.ShouldProcess($utilsPath, "Remove-Item (diretório)")) {
    Remove-Item -LiteralPath $utilsPath -Recurse -Force -WhatIf:$WhatIfPreference
}
Write-Host "Diretório '$utilsPath' removido." -ForegroundColor Green

