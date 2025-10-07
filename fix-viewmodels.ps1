# Script para corrigir ViewModels com construtor parametrizado
# Cria Factory para cada ViewModel problemático

Write-Host "=== CORRIGINDO VIEWMODELS PROBLEMÁTICOS ===" -ForegroundColor Green

# Lista de ViewModels problemáticos
$problematicViewModels = @(
    "ContractManagementViewModel",
    "ClientListViewModel", 
    "DashboardViewModel",
    "EditMesaViewModel",
    "RotaMesasViewModel",
    "GerenciarMesasViewModel",
    "RepresentanteLegalSignatureViewModel",
    "AditivoSignatureViewModel",
    "SignatureCaptureViewModel",
    "VehiclesViewModel",
    "VehicleDetailViewModel",
    "StockViewModel",
    "MetaCadastroViewModel",
    "ClosureReportViewModel",
    "MetasViewModel",
    "SettlementViewModel",
    "HistoricoMesasVendidasViewModel",
    "ExpenseRegisterViewModel",
    "GlobalExpensesViewModel"
)

foreach ($viewModel in $problematicViewModels) {
    Write-Host "Criando Factory para $viewModel..." -ForegroundColor Yellow
    
    # Determinar o pacote baseado no ViewModel
    $package = ""
    if ($viewModel -like "*Contract*") { $package = "contracts" }
    elseif ($viewModel -like "*Client*") { $package = "clients" }
    elseif ($viewModel -like "*Dashboard*") { $package = "dashboard" }
    elseif ($viewModel -like "*Mesa*") { $package = "mesas" }
    elseif ($viewModel -like "*Rota*") { $package = "routes" }
    elseif ($viewModel -like "*Signature*") { $package = "contracts" }
    elseif ($viewModel -like "*Vehicle*") { $package = "inventory/vehicles" }
    elseif ($viewModel -like "*Stock*") { $package = "inventory/stock" }
    elseif ($viewModel -like "*Meta*") { $package = "metas" }
    elseif ($viewModel -like "*Closure*") { $package = "reports" }
    elseif ($viewModel -like "*Settlement*") { $package = "settlement" }
    elseif ($viewModel -like "*Expense*") { $package = "expenses" }
    else { $package = "common" }
    
    $factoryContent = @"
package com.example.gestaobilhares.ui.$package

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestaobilhares.data.repository.AppRepository

/**
 * Factory para criar $viewModel com dependências injetadas.
 * Resolve o problema de instanciação do ViewModelProvider.
 */
class $($viewModel)Factory(
    private val repository: AppRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom($viewModel::class.java)) {
            return $viewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: `$`{modelClass.name}")
    }
}
"@
    
    $factoryPath = "app/src/main/java/com/example/gestaobilhares/ui/$package/$($viewModel)Factory.kt"
    $factoryContent | Out-File -FilePath $factoryPath -Encoding UTF8
    
    Write-Host "✅ Factory criada: $factoryPath" -ForegroundColor Green
}

Write-Host "=== CORREÇÃO CONCLUÍDA ===" -ForegroundColor Green
Write-Host "Agora você precisa atualizar os Fragments para usar as Factories" -ForegroundColor Yellow
