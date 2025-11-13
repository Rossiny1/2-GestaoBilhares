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
- **Jetpack Compose** para UI moderna (implementado 2025)

### **Arquitetura MVVM Modernizada e Centralizada (Híbrida)**

- **Model**: Room Database (Entities, DAOs)
- **View**: Fragments com DataBinding + StateFlow + wrappers `ComposeView`; telas Compose modernas
- **ViewModel**: Lógica de negócio com StateFlow
- **Repository**: AppRepository centralizado (único ponto de acesso)
- **BaseViewModel**: Funcionalidades comuns centralizadas
- **repeatOnLifecycle**: Observação moderna de StateFlow
- **Compose Integration**: Telas modernas com Jetpack Compose
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

### **Relacionamentos**

- Cliente → Mesa (1:N)
- Rota → Cliente (1:N)
- Cliente → Acerto (1:N)
- Contrato → Mesa (1:N)

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

### **Fragments Principais (View System)**

- `RoutesFragment`: Listagem de rotas
- `ClientListFragment`: Clientes por rota
- `ClientDetailFragment`: Detalhes do cliente
- `SettlementFragment`: Tela de acerto
- `ContractGenerationFragment`: Geração de contrato
- `SignatureCaptureFragment`: Captura de assinatura
- `VehicleDetailFragment`: Histórico de veículos
- `MetaCadastroFragment`: Cadastro de metas
- `RepresentanteLegalSignatureFragment`: Assinatura do representante legal

### **Compose Screens (Modernas) - Status Parcial**

- `DashboardScreen`: Tela principal com estatísticas
- `ClientDetailScreen`: Detalhes do cliente (Compose)
- `SettlementScreen`: Tela de acerto (Compose)
- `VehicleDetailScreen`: Histórico de veículos (Compose)
- `StockScreen`: Controle de estoque (Compose)
- `RoutesScreen`: Listagem de rotas (Compose)
- `ClientListScreen`: Clientes por rota (Compose)
- `ClosureReportScreen`: Relatórios de fechamento (Compose)
- `VehiclesScreen`: Listagem de veículos (Compose)
- `ContractManagementScreen`: Gerenciamento de contratos (Compose)
- `MetasScreen`: Gestão de metas (Compose)
- `ColaboradoresScreen`: Gestão de colaboradores (Compose)
- `CiclosScreen`: Gestão de ciclos (Compose)
- `ExpenseRegisterScreen`: Registro de despesas (Compose)
- `MesasDepositoScreen`: Gestão de mesas (Compose)
- `MetaCadastroScreen`: Cadastro de metas (Compose)
- `NovaReformaScreen`: Nova reforma (Compose)

### **Adapters (View System)**

- `ClientListAdapter`: Lista de clientes
- `MesasAcertoAdapter`: Mesas no acerto
- `RoutesAdapter`: Lista de rotas

### **Compose Components (Modernos)**

- `GestaoBilharesButton`: Botão customizado reutilizável
- `GestaoBilharesTextField`: Campo de texto customizado
- `GestaoBilharesCard`: Card customizado
- `GestaoBilharesLoadingIndicator`: Indicador de carregamento
- `ButtonVariant`: Enum para variantes de botão
- `ComposeIntegration`: Integração centralizada de telas Compose

### **Dialogs**

- `ContractFinalizationDialog`: Finalização de contrato
- `SettlementSummaryDialog`: Resumo do acerto
- `ClientSelectionDialog`: Seleção de cliente para transferência
- `TransferClientDialog`: Transferência de cliente entre rotas
- `PanoSelectionDialog`: Seleção de pano para troca
- `AddEditStockItemDialog`: Adicionar/editar item do estoque
- `AddPanosLoteDialog`: Adicionar panos em lote

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

### **Jetpack Compose Migration (2025)**

- **DashboardScreen**: Tela principal com estatísticas modernas
- **ClientDetailScreen**: Detalhes do cliente com UI moderna
- **SettlementScreen**: Tela de acerto com componentes Compose
- **VehicleDetailScreen**: Histórico de veículos modernizado
- **StockScreen**: Controle de estoque com UI fluida
- **RoutesScreen**: Listagem de rotas com Material 3
- **ClientListScreen**: Clientes com design moderno
- **ClosureReportScreen**: Relatórios com interface intuitiva
- **VehiclesScreen**: Gestão de veículos modernizada
- **ContractManagementScreen**: Contratos com UX melhorada
- **MetasScreen**: Metas com componentes reutilizáveis
- **ColaboradoresScreen**: Colaboradores com design consistente
- **CiclosScreen**: Ciclos com interface moderna
- **ExpenseRegisterScreen**: Despesas com formulários otimizados
- **MesasDepositoScreen**: Mesas com componentes customizados
- **MetaCadastroScreen**: Cadastro com validação visual
- **NovaReformaScreen**: Reformas com UX aprimorada

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
- **UI Declarativa**: Compose oferece UI mais fluida e responsiva
- **Componentes Reutilizáveis**: Redução de código duplicado
- **Material 3**: Design system moderno e consistente
- **Testabilidade**: Compose facilita testes de UI
- **Performance UI**: Compose otimiza renderização automaticamente

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

// ✅ PADRÃO COMPOSE: Screen Moderna (sem alterar aparência)
@Composable
fun MyScreen(
    onNavigateBack: () -> Unit,
    viewModel: MyViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Título") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Conteúdo da tela
        }
    }
}

// ✅ PADRÃO COMPOSE: Componente Reutilizável
@Composable
fun GestaoBilharesButton(
    text: String,
    onClick: () -> Unit,
    variant: ButtonVariant = ButtonVariant.Primary,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = when (variant) {
                ButtonVariant.Primary -> Color(0xFF2C3E50)
                ButtonVariant.Secondary -> Color(0xFF7F8C8D)
                ButtonVariant.Success -> Color(0xFF27AE60)
                ButtonVariant.Danger -> Color(0xFFE74C3C)
                ButtonVariant.Error -> Color(0xFFE74C3C)
            }
        )
    ) {
        Text(text = text, color = Color.White)
    }
}
```

## 🎯 REGRA PRINCIPAL: ARQUITETURA HÍBRIDA MODULAR (2025)

### **Decisão Arquitetural: Estrutura Híbrida para Trabalho Paralelo**

**Análise realizada (Janeiro 2025):**
- AppRepository atual: ~1.430 linhas, 264 métodos, 17+ DAOs
- Contexto: 4 agents trabalhando simultaneamente
- Necessidade: Evitar conflitos de merge e permitir trabalho paralelo

**Decisão: Arquitetura Híbrida Modular**
- **AppRepository** mantém-se como **Facade/Coordinator** centralizado
- **Repositories especializados** por domínio (ClientRepository, AcertoRepository, etc.)
- **AppRepository delega** para repositories especializados
- **Benefício**: Agents podem trabalhar em domínios diferentes sem conflitos

### **Princípios Arquiteturais (Atualizados 2025)**

1. **FACADE CENTRALIZADO**: AppRepository como ponto único de acesso (mantém compatibilidade)
2. **REPOSITORIES ESPECIALIZADOS**: Um repository por domínio de negócio
3. **DELEGAÇÃO**: AppRepository delega para repositories especializados
4. **BASEVIEWMODEL CENTRALIZADA**: Funcionalidades comuns em um local
5. **MODULARIDADE**: Separação por domínio facilita trabalho paralelo
6. **COMPATIBILIDADE**: ViewModels continuam usando AppRepository (sem breaking changes)

### **Estrutura Modular Híbrida (2025)**

```
📁 data/
  └── repository/
      ├── AppRepository.kt (✅ FACADE - delega para especializados)
      ├── domain/
      │   ├── ClientRepository.kt (✅ Domínio: Clientes)
      │   ├── AcertoRepository.kt (✅ Domínio: Acertos)
      │   ├── MesaRepository.kt (✅ Domínio: Mesas)
      │   ├── RotaRepository.kt (✅ Domínio: Rotas)
      │   ├── DespesaRepository.kt (✅ Domínio: Despesas)
      │   ├── ColaboradorRepository.kt (✅ Domínio: Colaboradores)
      │   ├── ContratoRepository.kt (✅ Domínio: Contratos)
      │   └── CicloRepository.kt (✅ Domínio: Ciclos)

📁 ui/
  └── common/
      └── BaseViewModel.kt (✅ FUNCIONALIDADES CENTRALIZADAS)

📁 ui/
  └── [module]/
      ├── [Module]ViewModel.kt (✅ USA AppRepository - sem mudanças)
      └── [Module]Fragment.kt (✅ USA STATEFLOW + COLLECT)
```

### **Benefícios da Arquitetura Híbrida**

- **Trabalho Paralelo**: 4 agents podem trabalhar em domínios diferentes sem conflitos
- **Manutenibilidade**: Código organizado por domínio, fácil de localizar
- **Performance**: Cache centralizado no AppRepository
- **Consistência**: Padrões unificados via AppRepository
- **Compatibilidade**: ViewModels não precisam mudar (usam AppRepository)
- **Escalabilidade**: Fácil adicionar novos domínios sem afetar existentes
- **Testabilidade**: Repositories especializados são mais fáceis de testar
