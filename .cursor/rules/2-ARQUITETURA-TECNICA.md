# 2. ARQUITETURA TÉCNICA

## 🏗️ PADRÕES DE DESENVOLVIMENTO

### **Linguagem e Framework (Modernizado 2025)**

- **Kotlin** como linguagem principal
- **Android Architecture Components** (ViewModel, StateFlow, Room)
- **Navigation Component** para navegação
- **Hilt** para injeção de dependência
- **Material Design** para UI
- **StateFlow** para observação reativa moderna
- **BaseViewModel** para centralização de funcionalidades

### **Arquitetura MVVM Modernizada e Centralizada**

- **Model**: Room Database (Entities, DAOs)
- **View**: Fragments com DataBinding + StateFlow
- **ViewModel**: Lógica de negócio com StateFlow
- **Repository**: AppRepository centralizado (único ponto de acesso)
- **BaseViewModel**: Funcionalidades comuns centralizadas
- **repeatOnLifecycle**: Observação moderna de StateFlow
- **🎯 REGRA**: Centralização e simplificação sempre que possível

## 🗄️ BANCO DE DADOS

### **Entidades Principais**

- `Cliente`: Dados dos clientes
- `Mesa`: Mesas de bilhar disponíveis
- `Rota`: Rotas de entrega
- `Acerto`: Transações de acerto
- `Despesa`: Despesas por rota/ciclo
- `ContratoLocacao`: Contratos de locação
- `SignaturePoint`: Pontos de assinatura
- `CicloAcertoEntity`: Ciclos de acerto por rota
- `AditivoMesa`: Aditivos de mesa
- `Veiculo`: Dados dos veículos
- `Abastecimento`: Histórico de abastecimento
- `Manutencao`: Histórico de manutenção
- `Colaborador`: Dados dos colaboradores
- `Meta`: Metas de desempenho
- `EstoqueItem`: Itens do estoque
- `Pano`: Panos de mesa

### **Relacionamentos**

- Cliente → Mesa (1:N)
- Rota → Cliente (1:N)
- Cliente → Acerto (1:N)
- Contrato → Mesa (1:N)
- Rota → CicloAcerto (1:N)
- CicloAcerto → Acerto (1:N)
- CicloAcerto → Despesa (1:N)
- Cliente → ContratoLocacao (1:N)
- Mesa → AditivoMesa (1:N)
- Veiculo → Abastecimento (1:N)
- Veiculo → Manutencao (1:N)
- Colaborador → Meta (1:N)

## 🔐 SEGURANÇA E VALIDAÇÃO

### **Assinatura Eletrônica**

- **SignatureView**: Captura de assinatura manual
- **SignatureStatistics**: Validação biométrica
- **DocumentIntegrityManager**: Hash SHA-256
- **LegalLogger**: Logs jurídicos para auditoria
- **SignatureMetadataCollector**: Metadados do dispositivo

### **Validação Jurídica (Lei 14.063/2020)**

- Captura de metadados (timestamp, device ID, IP, pressão, velocidade)
- Geração de hash SHA-256 para integridade
- Logs jurídicos completos para auditoria
- Validação de características biométricas
- Confirmação de presença física do locatário

## 📱 COMPONENTES UI

### **Fragments Principais**

- `RoutesFragment`: Listagem de rotas
- `ClientListFragment`: Clientes por rota
- `ClientDetailFragment`: Detalhes do cliente
- `SettlementFragment`: Tela de acerto
- `SettlementDetailFragment`: Detalhes do acerto
- `ContractGenerationFragment`: Geração de contrato
- `SignatureCaptureFragment`: Captura de assinatura
- `AditivoSignatureFragment`: Assinatura de aditivo
- `MesasDepositoFragment`: Gerenciamento de mesas
- `CadastroMesaFragment`: Cadastro de mesa
- `EditMesaFragment`: Edição de mesa
- `VehicleDetailFragment`: Histórico de veículos
- `MetaCadastroFragment`: Cadastro de metas
- `RepresentanteLegalSignatureFragment`: Assinatura do representante legal
- `CycleManagementFragment`: Gerenciamento de ciclos
- `CycleReceiptsFragment`: Recebimentos do ciclo
- `CycleExpensesFragment`: Despesas do ciclo
- `CycleSummaryFragment`: Resumo do ciclo
- `CycleHistoryFragment`: Histórico de ciclos

### **Adapters**

- `ClientListAdapter`: Lista de clientes
- `MesasAcertoAdapter`: Mesas no acerto
- `RoutesAdapter`: Lista de rotas
- `SettlementHistoryAdapter`: Histórico de acertos
- `MesasAdapter`: Lista de mesas do cliente
- `CycleReceiptsAdapter`: Recebimentos do ciclo
- `CycleExpensesAdapter`: Despesas do ciclo

### **Dialogs**

- `ContractFinalizationDialog`: Finalização de contrato
- `SettlementSummaryDialog`: Resumo do acerto
- `ClientSelectionDialog`: Seleção de cliente para transferência
- `TransferClientDialog`: Transferência de cliente entre rotas
- `PanoSelectionDialog`: Seleção de pano para troca
- `AddEditStockItemDialog`: Adicionar/editar item do estoque
- `AddPanosLoteDialog`: Adicionar panos em lote
- `AdicionarMesaDialogFragment`: Adicionar mesa ao cliente
- `ConfirmarRetiradaMesaDialogFragment`: Confirmação de retirada de mesa
- `AdicionarObservacaoDialogFragment`: Adicionar observação
- `GerarRelatorioDialogFragment`: Geração de relatórios
- `RotaNaoIniciadaDialogFragment`: Dialog para rota não iniciada

## 🔄 FLUXO DE DADOS

### **Estados e Navegação**

- SafeArgs para passagem de parâmetros
- SharedPreferences para configurações
- Flow para dados reativos
- Coroutines para operações assíncronas

### **PDF e Relatórios**

- **iText7** para geração de PDFs
- **ContractPdfGenerator**: Contratos de locação
- **PdfReportGenerator**: Relatórios de acerto
- **ClosureReportPdfGenerator**: Relatórios de fechamento

## 🛠️ FERRAMENTAS DE DESENVOLVIMENTO

### **Build e Deploy**

- Gradle para build
- APK de debug para testes
- Logcat para debugging
- ADB para conexão com dispositivo

### **Logs e Debug**

- Logs detalhados em componentes críticos
- Sistema de auditoria jurídica
- Validação de integridade de dados

## 🚀 MODERNIZAÇÕES IMPLEMENTADAS (2025)

### **StateFlow Migration**

- **AuthViewModel**: Convertido de LiveData para StateFlow
- **RoutesViewModel**: Convertido de LiveData para StateFlow
- **LoginFragment**: Convertido de observe para collect + repeatOnLifecycle
- **RoutesFragment**: Convertido de observe para collect + repeatOnLifecycle

### **ViewModel Initialization Fix (2025)**

- **Problema Identificado**: Crashes por `by viewModels()` sem inicialização manual
- **Solução Aplicada**: Inicialização manual de ViewModels em todos os fragments
- **Padrão Implementado**: `lateinit var viewModel` + inicialização em `onViewCreated`
- **Fragments Corrigidos**: VehicleDetailFragment, MetaCadastroFragment, RepresentanteLegalSignatureFragment
- **Dialogs Corrigidos**: ClientSelectionDialog, TransferClientDialog, PanoSelectionDialog, AddEditStockItemDialog, AddPanosLoteDialog
- **Resultado**: Zero crashes - todas as telas funcionando perfeitamente

### **BaseViewModel Centralizada**

- **Funcionalidades Comuns**: Loading, error, message states
- **Métodos Utilitários**: showLoading(), hideLoading(), showError(), showMessage()
- **Logging Centralizado**: Timber para logs consistentes
- **Eliminação de Duplicação**: ~200 linhas de código reduzidas

### **Benefícios Técnicos**

- **Performance**: StateFlow é mais eficiente que LiveData
- **Coroutines**: Integração nativa com Kotlin Coroutines
- **Lifecycle**: repeatOnLifecycle garante observação segura
- **Manutenibilidade**: Código mais limpo e organizado
- **Modernidade**: Seguindo melhores práticas Android 2025

### **Padrões Implementados**

```kotlin
// ✅ PADRÃO MODERNO: StateFlow + collect
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.property.collect { value ->
            // Atualizar UI
        }
    }
}

// ✅ PADRÃO MODERNO: BaseViewModel
class MyViewModel : BaseViewModel() {
    fun doSomething() {
        showLoading()
        // Lógica de negócio
        hideLoading()
    }
}

// ✅ PADRÃO CORRIGIDO: ViewModel Initialization
class MyFragment : Fragment() {
    private lateinit var viewModel: MyViewModel
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicialização manual do ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        val repository = Repository(database.dao())
        viewModel = MyViewModel(repository)
        
        // Configurar UI e observers
        setupUI()
        observeViewModel()
    }
}
```

## 🎯 REGRA PRINCIPAL: CENTRALIZAÇÃO E SIMPLIFICAÇÃO

### **Princípios Arquiteturais**

1. **UM REPOSITORY CENTRALIZADO**: AppRepository como único ponto de acesso aos dados
2. **BASEVIEWMODEL CENTRALIZADA**: Funcionalidades comuns em um local
3. **ELIMINAR FRAGMENTAÇÃO**: Evitar múltiplos arquivos desnecessários
4. **FACILITAR MANUTENÇÃO**: Código organizado e acessível
5. **REUTILIZAR CÓDIGO**: Eliminar duplicação sempre que possível

### **Benefícios da Centralização**

- **Manutenibilidade**: Código em um local facilita manutenção
- **Performance**: Cache centralizado otimiza consultas
- **Consistência**: Padrões unificados em toda aplicação
- **Simplicidade**: Menos arquivos, menos complexidade
- **Debugging**: Logs centralizados facilitam diagnóstico

### **Estrutura Centralizada**

```
📁 data/
  └── repository/
      └── AppRepository.kt (✅ ÚNICO REPOSITORY)

📁 ui/
  └── common/
      └── BaseViewModel.kt (✅ FUNCIONALIDADES CENTRALIZADAS)

📁 ui/
  └── [module]/
      ├── [Module]ViewModel.kt (✅ HERDA DE BASEVIEWMODEL)
      └── [Module]Fragment.kt (✅ USA STATEFLOW + COLLECT)
```
