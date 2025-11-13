Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANALISE: Commit 589da71 - AppRepository" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Verificar se o commit existe
Write-Host "`n[1/4] Verificando commit 589da71..." -ForegroundColor Yellow
$commitInfo = git show 589da71 --oneline --no-patch 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Commit nao encontrado" -ForegroundColor Red
    exit 1
}
Write-Host "OK: Commit encontrado" -ForegroundColor Green
Write-Host "  $commitInfo" -ForegroundColor Gray

# Verificar estrutura do AppRepository no commit
Write-Host "`n[2/4] Verificando AppRepository no commit..." -ForegroundColor Yellow
$appRepoCommit = git show 589da71:app/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: AppRepository nao encontrado no commit" -ForegroundColor Red
    exit 1
}

# Contar métodos e DAOs
$daosNoCommit = ($appRepoCommit | Select-String "private val.*Dao:").Count
$metodosNoCommit = ($appRepoCommit | Select-String "^\s*(suspend )?fun ").Count

Write-Host "OK: AppRepository encontrado" -ForegroundColor Green
Write-Host "  DAOs no construtor: $daosNoCommit" -ForegroundColor Gray
Write-Host "  Metodos encontrados: $metodosNoCommit" -ForegroundColor Gray

# Verificar DAOs específicos
Write-Host "`n[3/4] Verificando DAOs especificos..." -ForegroundColor Yellow
$daosEspecificos = @(
    "panoEstoqueDao",
    "mesaVendidaDao",
    "historicoManutencaoMesaDao",
    "mesaReformadaDao",
    "categoriaDespesaDao",
    "tipoDespesaDao"
)

foreach ($dao in $daosEspecificos) {
    if ($appRepoCommit -match $dao) {
        Write-Host "  OK: $dao encontrado" -ForegroundColor Green
    } else {
        Write-Host "  FALTANDO: $dao" -ForegroundColor Red
    }
}

# Verificar métodos importantes
Write-Host "`n[4/4] Verificando metodos importantes..." -ForegroundColor Yellow
$metodosImportantes = @(
    "inserirMesaVendida",
    "buscarMesaVendidaPorId",
    "inserirPanoEstoque",
    "obterTodosPanosEstoque",
    "inserirHistoricoManutencaoMesa",
    "buscarRotaIdPorCliente",
    "obterClientesPorRotaComDebitoAtual",
    "finalizarCicloAtualComDados"
)

foreach ($metodo in $metodosImportantes) {
    if ($appRepoCommit -match "fun.*$metodo") {
        Write-Host "  OK: $metodo encontrado" -ForegroundColor Green
    } else {
        Write-Host "  FALTANDO: $metodo" -ForegroundColor Yellow
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "ANALISE CONCLUIDA" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "`nRECOMENDACAO:" -ForegroundColor Yellow
Write-Host "Se o commit 589da71 tem mais DAOs e metodos, considere restaurar o AppRepository desse commit" -ForegroundColor Yellow
Write-Host "`nPara restaurar: git show 589da71:app/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt > app/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt" -ForegroundColor Cyan

