# Script para corrigir imports após migração dos módulos
$ErrorActionPreference = "Stop"

Write-Host "Corrigindo imports nos módulos migrados..." -ForegroundColor Cyan

# Função para substituir imports em um arquivo
function Fix-ImportsInFile {
    param($filePath, $oldPackage, $newPackage)
    
    if (-not (Test-Path $filePath)) { return }
    
    $content = Get-Content $filePath -Raw -Encoding UTF8
    $originalContent = $content
    
    # Substituir imports
    $content = $content -replace "import $oldPackage", "import $newPackage"
    
    # Substituir referências no código (package declarations já estão corretos)
    # Não precisamos fazer nada aqui pois os packages já estão corretos
    
    if ($content -ne $originalContent) {
        Set-Content -Path $filePath -Value $content -Encoding UTF8 -NoNewline
        Write-Host "Corrigido: $filePath" -ForegroundColor DarkGreen
    }
}

# Corrigir imports no módulo :sync
Write-Host "Corrigindo imports no módulo :sync..." -ForegroundColor Yellow
$syncFiles = Get-ChildItem -Path "sync\src\main\java" -Recurse -File -Filter "*.kt" -ErrorAction SilentlyContinue
foreach ($file in $syncFiles) {
    # Imports de data já estão corretos (com.example.gestaobilhares.data)
    # Imports de core já estão corretos (com.example.gestaobilhares.core)
    # Não precisa mudar nada
}

# Corrigir imports no módulo :ui
Write-Host "Corrigindo imports no módulo :ui..." -ForegroundColor Yellow
$uiFiles = Get-ChildItem -Path "ui\src\main\java" -Recurse -File -Filter "*.kt" -ErrorAction SilentlyContinue
foreach ($file in $uiFiles) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $originalContent = $content
    
    # Corrigir imports de utils que foram para core
    $content = $content -replace "import com\.example\.gestaobilhares\.utils\.(AppLogger|DataEncryption|FirebaseStorageManager|DateUtils|StringUtils|PasswordHasher|DataValidator|PaginationManager|SignatureStatistics)", "import com.example.gestaobilhares.core.utils.`$1"
    
    # Corrigir imports de data
    $content = $content -replace "import com\.example\.gestaobilhares\.data\.", "import com.example.gestaobilhares.data."
    
    # Corrigir imports de sync
    $content = $content -replace "import com\.example\.gestaobilhares\.sync\.", "import com.example.gestaobilhares.sync."
    
    # Corrigir imports de workers
    $content = $content -replace "import com\.example\.gestaobilhares\.workers\.", "import com.example.gestaobilhares.sync.workers."
    
    if ($content -ne $originalContent) {
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
        Write-Host "Corrigido: $($file.Name)" -ForegroundColor DarkGreen
    }
}

# Corrigir imports no módulo :app que referenciam módulos migrados
Write-Host "Corrigindo imports no módulo :app..." -ForegroundColor Yellow
$appFiles = Get-ChildItem -Path "app\src\main\java" -Recurse -File -Filter "*.kt" -ErrorAction SilentlyContinue | Where-Object { $_.FullName -notlike "*\sync\*" -and $_.FullName -notlike "*\workers\*" -and $_.FullName -notlike "*\ui\*" }
foreach ($file in $appFiles) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $originalContent = $content
    
    # Corrigir imports de sync
    $content = $content -replace "import com\.example\.gestaobilhares\.sync\.", "import com.example.gestaobilhares.sync."
    
    # Corrigir imports de workers
    $content = $content -replace "import com\.example\.gestaobilhares\.workers\.", "import com.example.gestaobilhares.sync.workers."
    
    # Corrigir imports de ui
    $content = $content -replace "import com\.example\.gestaobilhares\.ui\.", "import com.example.gestaobilhares.ui."
    
    if ($content -ne $originalContent) {
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
        Write-Host "Corrigido: $($file.Name)" -ForegroundColor DarkGreen
    }
}

Write-Host "✅ Correção de imports concluída!" -ForegroundColor Green

