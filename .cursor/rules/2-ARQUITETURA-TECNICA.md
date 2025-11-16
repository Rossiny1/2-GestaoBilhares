# 2. ARQUITETURA TÉCNICA (Android 2025)

## 🏗️ PADRÕES DE DESENVOLVIMENTO

### **Stack Tecnológico (Modernizado 2025)**

- **Kotlin**: Linguagem principal (100%)
- **Jetpack Compose**: UI moderna (35.8% implementado)
- **Material Design 3**: Tema e componentes modernos
- **Android Architecture Components**: ViewModel, StateFlow, Room
- **Navigation Component**: Navegação type-safe
- **Room Database**: Persistência local offline-first
- **StateFlow**: Observação reativa moderna (substitui LiveData)
- **WorkManager**: Background tasks (sincronização)
- **Firebase Firestore**: Backend (configurado, aguardando SyncManagerV2)
- **RepositoryFactory**: Injeção de dependência simples (Hilt pode ser adicionado futuramente)

### **Arquitetura MVVM Modernizada (Híbrida)**

```
┌─────────────────────────────────────────────────────────┐
│                    UI LAYER                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Compose    │  │   Fragments  │  │   Activities │  │
│  │   Screens    │  │   (Legacy)   │  │              │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                 │                  │          │
│         └─────────────────┼──────────────────┘          │
│                           │                             │
│                    ┌──────▼──────┐                      │
│                    │  ViewModels │                      │
│                    │  (StateFlow)│                      │
│                    └──────┬──────┘                      │
└───────────────────────────┼─────────────────────────────┘
                            │
┌───────────────────────────┼─────────────────────────────┐
│                    DOMAIN LAYER                          │
│                    ┌──────▼──────┐                      │
│                    │ AppRepository│                      │
│                    │   (Facade)   │                      │
│                    └──────┬──────┘                      │
│                           │                             │
│         ┌─────────────────┼─────────────────┐           │
│         │                 │                 │           │
│  ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐    │
│  │   Client    │  │   Acerto    │  │    Mesa     │    │
│  │ Repository  │  │ Repository  │  │ Repository  │    │
│  └─────────────┘  └─────────────┘  └─────────────┘    │
│                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │    Rota     │  │  Despesa    │  │ Colaborador │    │
│  │ Repository  │  │ Repository  │  │ Repository  │    │
│  └─────────────┘  └─────────────┘  └─────────────┘    │
│                                                         │
│  ┌─────────────┐  ┌─────────────┐                      │
│  │  Contrato   │  │    Ciclo    │                      │
│  │ Repository  │  │ Repository  │                      │
│  └─────────────┘  └─────────────┘                      │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────┼─────────────────────────────┐
│                    DATA LAYER                            │
│                    ┌──────▼──────┐                      │
│                    │     DAOs    │                      │
│                    └──────┬──────┘                      │
│                           │                             │
│                    ┌──────▼──────┐                      │
│                    │ Room Database│                     │
│                    │  (Local SQL) │                     │
│                    └──────────────┘                     │
└─────────────────────────────────────────────────────────┘
```

### **Arquitetura Híbrida Modular (2025)**

**Princípio**: AppRepository como Facade centralizado + Repositories especializados por domínio

**Estrutura**:
- `AppRepository`: Facade que delega para repositories especializados
- `domain/`: Repositories especializados (ClienteRepository, DespesaRepository, VeiculoRepository, etc.)
- Repositories especializados recebem DAOs no construtor
- ViewModels usam apenas AppRepository (sem acesso direto a DAOs)

**Benefícios**:
- ✅ Trabalho paralelo sem conflitos (4+ agents)
- ✅ Código organizado por domínio
- ✅ Compatibilidade preservada (ViewModels não mudam)
- ✅ Performance otimizada (cache centralizado)
- ✅ Escalabilidade (fácil adicionar novos domínios)
- ✅ Observação reativa com Flows funcionando corretamente

## 🗄️ BANCO DE DADOS

### **Room Database (Offline-first)**

**Entidades Principais**:
- `Cliente`: Dados dos clientes
- `Mesa`: Mesas de bilhar disponíveis
- `Rota`: Rotas de entrega
- `Acerto`: Transações de acerto
- `Despesa`: Despesas por rota/ciclo (usa LocalDateTime)
- `CicloAcerto`: Ciclos de acerto
- `ContratoLocacao`: Contratos de locação
- `Colaborador`: Colaboradores do sistema
- `Veiculo`: Veículos da frota
- `HistoricoCombustivelVeiculo`: Histórico de abastecimento
- `HistoricoManutencaoVeiculo`: Histórico de manutenção
- `Meta`: Metas de colaboradores
- `PanoEstoque`: Panos em estoque
- `SignaturePoint`: Pontos de assinatura

**Relacionamentos**:
- Cliente → Mesa (1:N)
- Rota → Cliente (1:N)
- Cliente → Acerto (1:N)
- Contrato → Mesa (1:N)
- Ciclo → Acerto (1:N)

## 📱 COMPONENTES UI

### **Jetpack Compose (35.8% implementado)**

**Telas Compose Implementadas**:
- `RoutesScreen`, `DashboardScreen`, `ClientListScreen`, `ClientDetailScreen`
- `SettlementScreen`, `SettlementDetailScreen`, `ClosureReportScreen`
- `VehiclesScreen`, `VehicleDetailScreen`, `StockScreen`
- `ContractManagementScreen`, `SignatureCaptureScreen`
- `MetasScreen`, `ColaboradoresScreen`, `CiclosScreen`
- `ExpenseRegisterScreen`, `MesasDepositoScreen`, `NovaReformaScreen`
- `LoginScreen`

**Fragments Legacy (64.2% pendente)**:
- `SettlementFragment`, `ClientListFragment`, `CycleManagementFragment`
- `ExpenseHistoryFragment`, `GerenciarMesasFragment`
- E mais 38 telas...

### **Padrão StateFlow e Observação Reativa**

```kotlin
// ✅ CORRETO: Observação moderna com repeatOnLifecycle (Fragment)
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.property.collect { value ->
            // Atualizar UI
        }
    }
}

// ✅ CORRETO: ViewModel usando flatMapLatest para observação reativa
class MyViewModel(
    private val appRepository: AppRepository
) : ViewModel() {
    private val _idFlow = MutableStateFlow<Long?>(null)
    private val _data = MutableStateFlow<List<Item>>(emptyList())
    val data: StateFlow<List<Item>> = _data.asStateFlow()
    
    init {
        viewModelScope.launch {
            _idFlow
                .flatMapLatest { id ->
                    if (id == null) return@flatMapLatest flowOf(emptyList())
                    appRepository.obterDadosPorId(id) // Flow reativo
                }
                .collect { items ->
                    _data.value = items
                }
        }
    }
    
    fun loadData(id: Long) {
        _idFlow.value = id // Atualiza o Flow, dispara observação automática
    }
}
```

**Padrão Recomendado para ViewModels**:
- Usar `flatMapLatest` com `MutableStateFlow` para IDs
- Observar diretamente Flows do Repository (não filtrar manualmente)
- Room Flows emitem automaticamente quando há mudanças no banco
- Exemplo: `CycleExpensesViewModel`, `CycleReceiptsViewModel`, `VehicleDetailViewModel`

## 🔐 SEGURANÇA E VALIDAÇÃO

### **Assinatura Eletrônica (Lei 14.063/2020)**

- **SignatureView**: Captura de assinatura manual
- **SignatureStatistics**: Validação biométrica
- **DocumentIntegrityManager**: Hash SHA-256
- **LegalLogger**: Logs jurídicos para auditoria
- **SignatureMetadataCollector**: Metadados do dispositivo

**Validações**:
- Captura de metadados (timestamp, device ID, IP, pressão, velocidade)
- Geração de hash SHA-256 para integridade
- Logs jurídicos completos para auditoria
- Validação de características biométricas
- Confirmação de presença física do locatário

## 🔄 SINCRONIZAÇÃO (IMPLEMENTADA)

### **Estratégia Offline-first**

1. **Dados Locais**: Sempre disponíveis (Room Database)
2. **Fila de Sincronização**: Operações offline enfileiradas
3. **Sincronização Bidirecional**: Pull (servidor → local) + Push (local → servidor)
4. **Resolução de Conflitos**: Comparação de timestamp (última escrita vence)
5. **WorkManager**: Sincronização periódica em background

### **Implementação Atual**

```kotlin
// SyncRepository implementado e funcionando
class SyncRepository(
    private val context: Context,
    private val appRepository: AppRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun syncPull(): Result<Int> // Sincronizar do servidor
    suspend fun syncPush(): Result<Int> // Enviar para servidor
    suspend fun syncBidirectional(): Result<Int> // Sincronização completa (PUSH → PULL)
}
```

### **Estrutura Firestore**

```
empresas/
  └── empresa_001/
      └── entidades/
          ├── clientes/
          │   └── items/
          │       └── {documentId}
          ├── despesas/
          │   └── items/
          │       └── {documentId}
          └── ... (outras entidades)
```

### **Padrões de Observação Reativa**

```kotlin
// ✅ CORRETO: ViewModel usando flatMapLatest (como CycleExpensesViewModel)
class VehicleDetailViewModel(
    private val appRepository: AppRepository
) : ViewModel() {
    private val _vehicleIdFlow = MutableStateFlow<Long?>(null)
    
    init {
        viewModelScope.launch {
            _vehicleIdFlow
                .flatMapLatest { vehicleId ->
                    if (vehicleId == null) return@flatMapLatest flowOf(emptyList())
                    appRepository.obterHistoricoCombustivelPorVeiculo(vehicleId)
                }
                .collect { fuelList ->
                    _fuelHistory.value = fuelList
                }
        }
    }
}
```

### **Entidades Sincronizadas**

Todas as entidades principais estão sendo sincronizadas:
- ✅ Clientes, Rotas, Mesas, Acertos
- ✅ Despesas, Ciclos, Colaboradores
- ✅ Veículos, Metas, Histórico de Combustível
- ✅ Histórico de Manutenção, Contratos
- ✅ Panos, Stock Items, e demais entidades

## 🎯 MELHORES PRÁTICAS ANDROID 2025

1. **Jetpack Compose**: Priorizar para novas telas
2. **StateFlow**: Usar em vez de LiveData
3. **repeatOnLifecycle**: Observação segura de StateFlow
4. **Offline-first**: Dados sempre disponíveis localmente
5. **Modularização**: Código organizado por domínio
6. **Type-safe Navigation**: Navigation Component
7. **Material Design 3**: Componentes modernos
8. **WorkManager**: Background tasks confiáveis

## 📚 REFERÊNCIAS

- [Android Developer - Architecture](https://developer.android.com/topic/architecture)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [StateFlow vs LiveData](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Firebase Firestore](https://firebase.google.com/docs/firestore)
