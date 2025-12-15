Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESTAURAR: AppRepository do commit 589da71" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# [1/4] Backup do AppRepository atual
Write-Host "`n[1/4] Fazendo backup do AppRepository atual..." -ForegroundColor Yellow
$backupPath = "app\src\main\java\com\example\gestaobilhares\data\repository\AppRepository.kt.backup-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
if (Test-Path "app\src\main\java\com\example\gestaobilhares\data\repository\AppRepository.kt") {
    Copy-Item "app\src\main\java\com\example\gestaobilhares\data\repository\AppRepository.kt" -Destination $backupPath -Force
    Write-Host "OK: Backup criado em $backupPath" -ForegroundColor Green
} else {
    Write-Host "AVISO: AppRepository atual nao encontrado" -ForegroundColor Yellow
}

# [2/4] Restaurar do commit
Write-Host "`n[2/4] Restaurando AppRepository do commit 589da71..." -ForegroundColor Yellow
$targetPath = "app\src\main\java\com\example\gestaobilhares\data\repository\AppRepository.kt"
$gitPath = "app/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt"

try {
    $content = git show "589da71:$gitPath" 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERRO: Nao foi possivel restaurar do commit" -ForegroundColor Red
        Write-Host $content -ForegroundColor Red
        exit 1
    }
    
    # Salvar com encoding UTF-8 sem BOM
    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($targetPath, $content, $utf8NoBom)
    
    Write-Host "OK: AppRepository restaurado" -ForegroundColor Green
} catch {
    Write-Host "ERRO: Falha ao restaurar: $_" -ForegroundColor Red
    exit 1
}

# [3/4] Verificar se o arquivo foi restaurado
Write-Host "`n[3/4] Verificando arquivo restaurado..." -ForegroundColor Yellow
if (Test-Path $targetPath) {
    $fileSize = (Get-Item $targetPath).Length
    $lineCount = (Get-Content $targetPath).Count
    Write-Host "OK: Arquivo existe" -ForegroundColor Green
    Write-Host "  Tamanho: $fileSize bytes" -ForegroundColor Gray
    Write-Host "  Linhas: $lineCount" -ForegroundColor Gray
} else {
    Write-Host "ERRO: Arquivo nao foi criado" -ForegroundColor Red
    exit 1
}

# [4/4] Adicionar método faltante buscarMesaVendidaPorId
Write-Host "`n[4/4] Adicionando metodo faltante buscarMesaVendidaPorId..." -ForegroundColor Yellow
$content = Get-Content $targetPath -Raw
if ($content -notmatch "suspend fun buscarMesaVendidaPorId") {
    # Procurar a seção MESA VENDIDA e adicionar o método
    $mesaVendidaSection = "// ==================== MESA VENDIDA ===================="
    if ($content -match $mesaVendidaSection) {
        $newMethod = @"

    suspend fun buscarMesaVendidaPorId(id: Long) = mesaVendidaDao?.buscarPorId(id)
"@
        $content = $content -replace "($mesaVendidaSection[^\n]*)", "`$1`n$newMethod"
        [System.IO.File]::WriteAllText($targetPath, $content, $utf8NoBom)
        Write-Host "OK: Metodo buscarMesaVendidaPorId adicionado" -ForegroundColor Green
    } else {
        Write-Host "AVISO: Secao MESA VENDIDA nao encontrada, metodo nao adicionado" -ForegroundColor Yellow
    }
} else {
    Write-Host "OK: Metodo buscarMesaVendidaPorId ja existe" -ForegroundColor Green
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "RESTAURACAO CONCLUIDA" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "`nPROXIMO PASSO:" -ForegroundColor Yellow
Write-Host "1. Verificar se o RepositoryFactory.create() precisa ser atualizado" -ForegroundColor Yellow
Write-Host "2. Testar o build: .\gradlew assembleDebug" -ForegroundColor Yellow
Write-Host "`nSe houver problemas, o backup esta em: $backupPath" -ForegroundColor Gray

Write-Host "RESTAURAR: AppRepository do commit 589da71" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# [1/4] Backup do AppRepository atual
Write-Host "`n[1/4] Fazendo backup do AppRepository atual..." -ForegroundColor Yellow
$backupPath = "app\src\main\java\com\example\gestaobilhares\data\repository\AppRepository.kt.backup"
if (Test-Path "app\src\main\java\com\example\gestaobilhares\data\repository\AppRepository.kt") {
    Copy-Item "app\src\main\java\com\example\gestaobilhares\data\repository\AppRepository.kt" -Destination $backupPath -Force
    Write-Host "OK: Backup criado em $backupPath" -ForegroundColor Green
} else {
    Write-Host "AVISO: AppRepository atual nao encontrado" -ForegroundColor Yellow
}

# [2/4] Restaurar do commit
Write-Host "`n[2/4] Restaurando AppRepository do commit 589da71..." -ForegroundColor Yellow
$targetPath = "app\src\main\java\com\example\gestaobilhares\data\repository\AppRepository.kt"
$gitPath = "app/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt"

try {
    $content = git show "589da71:$gitPath" 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERRO: Nao foi possivel restaurar do commit" -ForegroundColor Red
        Write-Host $content -ForegroundColor Red
        exit 1
    }
    
    # Salvar com encoding UTF-8 sem BOM
    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($targetPath, $content, $utf8NoBom)
    
    Write-Host "OK: AppRepository restaurado" -ForegroundColor Green
} catch {
    Write-Host "ERRO: Falha ao restaurar: $_" -ForegroundColor Red
    exit 1
}

# [3/4] Verificar se o arquivo foi restaurado
Write-Host "`n[3/4] Verificando arquivo restaurado..." -ForegroundColor Yellow
if (Test-Path $targetPath) {
    $fileSize = (Get-Item $targetPath).Length
    $lineCount = (Get-Content $targetPath).Count
    Write-Host "OK: Arquivo existe" -ForegroundColor Green
    Write-Host "  Tamanho: $fileSize bytes" -ForegroundColor Gray
    Write-Host "  Linhas: $lineCount" -ForegroundColor Gray
} else {
    Write-Host "ERRO: Arquivo nao foi criado" -ForegroundColor Red
    exit 1
}

# [4/4] Verificar DAOs no arquivo restaurado
Write-Host "`n[4/4] Verificando DAOs no arquivo restaurado..." -ForegroundColor Yellow
$content = Get-Content $targetPath -Raw
$daos = @(
    "panoEstoqueDao",
    "mesaVendidaDao",
    "historicoManutencaoMesaDao",
    "mesaReformadaDao",
    "categoriaDespesaDao",
    "tipoDespesaDao",
    "panoMesaDao",
    "veiculoDao",
    "historicoManutencaoVeiculoDao",
    "historicoCombustivelVeiculoDao"
)

$daosEncontrados = 0
foreach ($dao in $daos) {
    if ($content -match $dao) {
        Write-Host "  OK: $dao encontrado" -ForegroundColor Green
        $daosEncontrados++
    } else {
        Write-Host "  FALTANDO: $dao" -ForegroundColor Yellow
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "RESTAURACAO CONCLUIDA" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DAOs encontrados: $daosEncontrados de $($daos.Count)" -ForegroundColor Cyan
Write-Host "`nPROXIMO PASSO:" -ForegroundColor Yellow
Write-Host "1. Verificar se o RepositoryFactory.create() precisa ser atualizado" -ForegroundColor Yellow
Write-Host "2. Testar o build: .\gradlew assembleDebug" -ForegroundColor Yellow
Write-Host "`nSe houver problemas, o backup esta em: $backupPath" -ForegroundColor Gray

