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
- **Firebase Firestore**: Backend (configurado e funcionando com SyncRepository)
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
- `MetaColaborador`: Metas por colaborador/rota
- `Equipment`: Equipamentos do estoque
- `PanoEstoque`: Panos em estoque
- `StockItem`: Itens genéricos do estoque
- `SignaturePoint`: Pontos de assinatura
- `SyncMetadata`: Metadados de sincronização (último timestamp por entidade)

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

## 🔄 SINCRONIZAÇÃO (IMPLEMENTADA E OTIMIZADA)

### **Estratégia Offline-first**

1. **Dados Locais**: Sempre disponíveis (Room Database)
2. **Fila de Sincronização**: Operações offline enfileiradas (CREATE, UPDATE, DELETE)
3. **Sincronização Bidirecional**: Pull (servidor → local) + Push (local → servidor)
4. **Resolução de Conflitos**: Comparação de timestamp (última escrita vence)
5. **WorkManager**: Sincronização periódica em background
6. **Sincronização Incremental**: Busca apenas dados novos/atualizados desde última sync
7. **Paginação**: Processa dados em lotes para evitar limites do Firestore
8. **Cache In-Memory**: Reduz queries ao banco durante processamento
9. **Heurística de Background**: `SyncRepository.shouldRunBackgroundSync()` só dispara WorkManager se houver pendências/falhas na fila ou se a última sync global `_global_sync` ocorreu há mais de 6 h, evitando execuções desnecessárias
10. **ACL por Rota**: `shouldSyncRouteData` e `accessibleRouteIdsCache` (Set) garantem que apenas as rotas permitidas sejam sincronizadas; usuários restritos têm queries Firestore filtradas por `rotaId`/`whereIn`
11. **Processamento Completo da Fila**: `processSyncQueue()` processa todas as operações pendentes em loop até esvaziar a fila completamente
12. **Operações DELETE**: Todas as exclusões locais enfileiram operação DELETE que é processada na próxima sincronização, com verificação pós-DELETE para confirmar exclusão no Firestore
13. **Proteção contra Cascade Delete (UPSERT)**: Entidades "pai" (Rota, Cliente, Mesa) utilizam estratégia `UPSERT` (Insert Ignore + Update Manual) no lugar de `REPLACE`. Isso previne que a atualização da entidade pai dispare `ON DELETE CASCADE` no banco de dados, excluindo acidentalmente entidades filhas (Ciclos, Acertos) durante a sincronização.

> **Nota**: O `SyncManager` agenda o WorkManager apenas quando o dispositivo está carregando, com bateria saudável e em rede não medida (Wi‑Fi). Isso reduz impacto em bateria/dados mantendo o comportamento offline-first.

### **Implementação Atual**

```kotlin
// SyncRepository implementado e funcionando com sincronização incremental
class SyncRepository(
    private val context: Context,
    private val appRepository: AppRepository,
    private val syncMetadataDao: SyncMetadataDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun syncPull(): Result<Int> // Sincronizar do servidor
    suspend fun syncPush(): Result<Int> // Enviar para servidor (processa fila de operações)
    suspend fun syncBidirectional(): Result<Int> // Sincronização completa (PUSH → PULL)
    suspend fun processSyncQueue(): Result<Unit> // Processa todas as operações pendentes (CREATE, UPDATE, DELETE)
    suspend fun processSingleSyncOperation(operation: SyncOperationEntity): Result<Unit> // Processa uma operação individual
    
    // Métodos auxiliares para sincronização incremental
    private suspend fun getLastSyncTimestamp(entityType: String): Long
    private suspend fun saveSyncMetadata(entityType: String, ...)
    private suspend fun executePaginatedQuery(query: Query, ...): Int
    private fun createIncrementalQuery(collectionRef: CollectionReference, ...): Query
    private fun resolveCollectionReference(entityType: String): CollectionReference // Mapeia tipo de entidade para coleção Firestore
}
```

**Fila de Sincronização (SyncOperationEntity)**:
- **OperationType**: CREATE, UPDATE, DELETE
- **EntityType**: Tipo da entidade (ex: "Despesa", "Cliente", "Acerto")
- **EntityId**: ID da entidade (usado como documentId no Firestore)
- **EntityData**: Dados JSON (para CREATE/UPDATE) ou "{}" (para DELETE)
- **Status**: PENDING, PROCESSING, COMPLETED, FAILED
- **RetryCount**: Contador de tentativas (máximo configurável)

**Processamento da Fila**:
- `processSyncQueue()` busca operações pendentes em lotes (configurável, padrão: 50)
- Processa cada operação individualmente via `processSingleSyncOperation()`
- Continua processando em loop até não haver mais operações pendentes
- Logs detalhados para rastreamento (enfileiramento, processamento, execução, erros)

### **Sincronização Incremental (PULL e PUSH)**

**Objetivo**: Reduzir uso de dados móveis e melhorar performance (98.6% de redução estimada)

**Implementação PULL**:
- **SyncMetadata**: Entidade Room para armazenar último timestamp de sincronização PULL por entidade
- **Queries Incrementais**: Usa `whereGreaterThan("lastModified", lastSyncTimestamp)` no Firestore
- **Fallback Seguro**: Se índice Firestore não existir ou query falhar, faz sync completo
- **Primeira Sincronização**: Sempre faz sync completo (quando `lastSyncTimestamp == 0L`)
- **Entidades Implementadas**: Todas as 27 entidades (Clientes, Rotas, Mesas, Colaboradores, Ciclos, Acertos, Despesas, Contratos, CategoriasDespesa, TiposDespesa, Metas, ColaboradorRotas, AditivoMesas, ContratoMesas, AssinaturasRepresentanteLegal, LogsAuditoria, PanoEstoque, MesaVendida, StockItem, MesaReformada, HistoricoManutencaoMesa, HistoricoManutencaoVeiculo, HistoricoCombustivelVeiculo, Veiculos, PanoMesa, MetaColaborador, Equipments)

**Implementação PUSH**:
- **SyncMetadata Push**: Usa sufixo `_push` para diferenciar de PULL (`entityType_push`)
- **Filtro Incremental**: Filtra entidades locais cujo timestamp (`dataUltimaAtualizacao`, `dataAtualizacao`, `dataCriacao`, etc.) é maior que `lastPushTimestamp`
- **Atualização de Timestamp**: Após push bem-sucedido, atualiza timestamp local com `lastModified` do servidor
- **Metadata Tracking**: Salva metadata de push (count, duration, bytes uploaded, errors)
- **Entidades Implementadas**: Todas as 27 entidades com lógica específica por tipo de timestamp disponível

**Otimizações de Performance**:
- **Cache In-Memory**: Carrega todos os registros locais uma vez antes de processar documentos do Firestore
- **Paginação**: Processa documentos em lotes de 500 para evitar limite de 1MB do Firestore
- **Queries Eficientes**: Usa índices compostos no Firestore para queries incrementais (quando disponíveis)
- **Fallback Robusto**: Se índice não existir, busca sem orderBy e ordena em memória (funciona sem índices)

**Exemplo de Uso PULL**:
```kotlin
// pullClientes() verifica se é primeira sync ou se há timestamp
val lastSync = getLastSyncTimestamp("clientes")
if (lastSync == 0L) {
    // Primeira sync: busca tudo
    pullClientesFullSync()
} else {
    // Sync incremental: busca apenas novos/atualizados
    val query = createIncrementalQuery(collectionRef, "clientes")
    executePaginatedQuery(query) { batch ->
        // Processa lote de documentos
    }
}
```

**Exemplo de Uso PUSH**:
```kotlin
// pushClientes() filtra apenas entidades modificadas
val lastPush = getLastPushTimestamp("clientes")
val clientesParaEnviar = if (lastPush > 0L) {
    clientesLocais.filter { it.dataUltimaAtualizacao.time > lastPush }
} else {
    clientesLocais // Primeira sync: enviar todos
}

clientesParaEnviar.forEach { cliente ->
    // Enviar para Firestore
    // Após sucesso, timestamp local é atualizado com lastModified do servidor
}
```

### **Controle de Acesso por Rotas**

- `UserSessionManager.getInstance(context)` expõe o nível de acesso e as rotas permitidas do colaborador; o cache (`accessibleRouteIdsCache`) agora é um `Set<Long>` resetado a cada `syncPull`
- `shouldSyncRouteData(...)` centraliza toda a validação de ACL, reutilizando caches de cliente/mesa para descobrir `rotaId` sem reconsultar o banco
- As consultas Firestore aplicam o filtro de rota sempre que o usuário não é admin:
  - Clientes, Despesas, Ciclos, Metas, MetaColaborador e ColaboradorRotas executam `whereEqualTo/whereIn("rotaId", chunk)` em grupos de até 10 IDs
  - Quando o colaborador não tem rotas atribuídas, nenhuma query é executada e apenas os dados locais permanecem disponíveis (offline-first)
- O processamento local continua validando `shouldSyncRouteData` para garantir consistência caso documentos cheguem sem o campo `rotaId`

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

### **Índices Compostos do Firestore (MELHORIA FUTURA)**

**Status**: ⏳ **PENDENTE** - Sistema funciona sem índices, mas performance melhora significativamente com eles

**Objetivo**: Criar índices compostos no Firestore para otimizar queries de busca de acertos por cliente e sincronização incremental.

**Arquivos Preparados**:
- ✅ `firestore.indexes.json`: Configuração de todos os índices necessários
- ✅ `deploy-indices-firestore.ps1`: Script PowerShell para deploy automático via Firebase CLI
- ✅ `GUIA-CRIACAO-INDICES-FIRESTORE.md`: Guia completo com 3 opções de criação

**Índices Necessários**:
1. **Busca de Acertos por Cliente**:
   - `items_clienteId_dataAcerto`: `clienteId` (ASC) + `dataAcerto` (DESC)
   - `items_cliente_id_dataAcerto`: Fallback para formato antigo
   - `items_clienteID_dataAcerto`: Fallback para formato alternativo

2. **Sincronização Incremental**:
   - `items_lastModified`: `lastModified` (ASC) - Aplica a todas as entidades

**Benefícios Esperados**:
- ⚡ **Performance**: Queries até 10x mais rápidas com índices
- 📉 **Custo**: Redução de leituras do Firestore (menos custo)
- 🚀 **Escalabilidade**: Suporta grandes volumes de dados sem degradação

**Como Implementar**:
1. **Opção 1 (Recomendada)**: Deploy via Firebase CLI
   ```powershell
   npm install -g firebase-tools
   .\deploy-indices-firestore.ps1
   ```

2. **Opção 2**: Criação manual no Firebase Console
   - Acesse: https://console.firebase.google.com/project/gestaobilhares/firestore/indexes
   - Siga o guia: `GUIA-CRIACAO-INDICES-FIRESTORE.md`

3. **Opção 3**: Usar links dos logs (quando app tentar query sem índice)

**Nota Importante**: 
- O sistema **já funciona sem índices** usando fallback robusto (busca sem orderBy e ordena em memória)
- Os índices são uma **otimização opcional** que melhora performance, mas não é obrigatória
- Consulte `GUIA-CRIACAO-INDICES-FIRESTORE.md` para detalhes completos

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
- ✅ Veículos, Metas, MetaColaborador, Histórico de Combustível
- ✅ Histórico de Manutenção, Contratos
- ✅ Panos, Stock Items, Equipment, e demais entidades

## 🎯 MELHORES PRÁTICAS ANDROID 2025

### **1. Arquitetura e Estrutura de Código**

#### **MVVM com Observação Reativa**
- ✅ **Jetpack Compose**: Priorizar para novas telas (moderno, declarativo)
- ✅ **StateFlow**: Usar em vez de LiveData (melhor para coroutines)
- ✅ **repeatOnLifecycle**: Observação segura de StateFlow (evita leaks)
- ✅ **Offline-first**: Dados sempre disponíveis localmente (Room como fonte primária)
- ✅ **Modularização**: Código organizado por domínio (escalabilidade)
- ✅ **Type-safe Navigation**: Navigation Component com SafeArgs
- ✅ **Material Design 3**: Componentes modernos e consistentes
- ✅ **WorkManager**: Background tasks confiáveis (respeitando sistema)

```kotlin
// ✅ PADRÃO RECOMENDADO 2025: ViewModel com StateFlow
@HiltViewModel // Injeção automática
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {
    // Estado UI como StateFlow (imutável externamente)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        loadData() // Carregar dados ao iniciar
    }
    
    private fun loadData() {
        viewModelScope.launch {
            // Observa Flow do Repository (reativo)
            repository.getData()
                .catch { e -> _uiState.value = UiState.Error(e.message) }
                .collect { data -> _uiState.value = UiState.Success(data) }
        }
    }
}

// Estado UI com sealed class (type-safe)
sealed class UiState {
    object Loading : UiState()
    data class Success(val data: List<Item>) : UiState()
    data class Error(val message: String?) : UiState()
}
```

### **2. Testes Automatizados (PRIORIDADE ALTA)**

#### **Estratégia de Testes**

**Pirâmide de Testes**:
- 70% Unit Tests (ViewModels, Repositories, Utils)
- 20% Integration Tests (Repositories + Room)
- 10% UI Tests (fluxos críticos)

**Ferramentas**:
- ✅ JUnit 5 (framework principal)
- ✅ Mockito/MockK (mocking)
- ✅ Turbine (testar Flows)
- ✅ Truth (assertions legíveis)
- ✅ Robolectric (testes Android sem emulador)

```kotlin
// ✅ EXEMPLO: Teste de ViewModel
class MyViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val mainDispatcher = StandardTestDispatcher()
    private val mockRepository = mock<MyRepository>()
    private lateinit var viewModel: MyViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcher)
        viewModel = MyViewModel(mockRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `quando dados são carregados, estado muda para Success`() = runTest {
        // Given
        val testData = listOf(Item("test"))
        whenever(mockRepository.getData()).thenReturn(flowOf(testData))
        
        // When
        viewModel.loadData()
        mainDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(testData, (state as UiState.Success).data)
    }
}
```

### **3. Performance e Otimização (PRIORIDADE MÉDIA)**

#### **Memory Management**

```kotlin
// ✅ BOA PRÁTICA: Cache com limite de tamanho
class CacheManager<K, V>(private val maxSize: Int = 100) {
    private val cache = LinkedHashMap<K, V>(maxSize, 0.75f, true)
    
    fun put(key: K, value: V) {
        if (cache.size >= maxSize) {
            cache.remove(cache.keys.first()) // Remove oldest
        }
        cache[key] = value
    }
    
    fun get(key: K): V? = cache[key]
}

// ✅ BOA PRÁTICA: LazyColumn para listas grandes (Compose)
@Composable
fun MyLazyList(items: List<Item>) {
    LazyColumn {
        items(
            items = items,
            key = { it.id } // Recomposição eficiente
        ) { item ->
            ItemCard(item)
        }
    }
}
```

#### **Database Optimization**

```kotlin
// ✅ BOA PRÁTICA: Índices Room para queries frequentes
@Entity(
    tableName = "clientes",
    indices = [
        Index(value = ["rotaId"]),           // Filtro por rota
        Index(value = ["dataAtualizacao"]),  // Sync incremental
        Index(value = ["nome"])              // Busca por nome
    ]
)
data class Cliente(...)

// ✅ BOA PRÁTICA: Paginação com Paging 3
@Dao
interface ClienteDao {
    @Query("SELECT * FROM clientes ORDER BY nome ASC")
    fun getPagedClientes(): PagingSource<Int, Cliente>
}
```

#### **Métricas de Performance**

| Métrica | Target | Como Medir |
|---------|--------|------------|
| **Frame Rate** | 60 FPS (16ms/frame) | Android Studio Profiler |
| **Cold Start** | < 2s | Logcat timestamps |
| **Memória** | < 100MB normal | Profiler Memory |
| **APK Size** | < 50MB | Build output |
| **Battery** | < 5%/hora | Battery Historian |

### **4. Segurança (PRIORIDADE MÉDIA)**

#### **Dados Sensíveis**

```kotlin
// ✅ BOA PRÁTICA: EncryptedSharedPreferences para dados sensíveis
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

// Armazenar token de forma segura
encryptedPrefs.edit().putString("auth_token", token).apply()
```

#### **Validação de Entrada**

```kotlin
// ✅ BOA PRÁTICA: Validação robusta
fun validarEmail(email: String): Result<String> {
    return when {
        email.isBlank() -> Result.failure(Exception("Email vazio"))
        !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> 
            Result.failure(Exception("Email inválido"))
        else -> Result.success(email.trim())
    }
}

// Sanitização para evitar SQL injection (Room já protege, mas para raw queries)
fun sanitizeQuery(input: String): String {
    return input.replace("'", "''")  // Escape single quotes
        .replace("--", "")           // Remove SQL comments
}
```

#### **ProGuard/R8 para Produção**

```proguard
# Ofuscação agressiva
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Mantém classes essenciais
-keep class com.example.gestaobilhares.data.entities.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Firebase
-keep class com.google.firebase.** { *; }
```

### **5. Accessibility (A11y) (PRIORIDADE BAIXA)**

```kotlin
// ✅ BOA PRÁTICA: Content descriptions para imagens
Image(
    painter = painterResource(R.drawable.ic_client),
    contentDescription = "Foto do cliente" // Essencial para TalkBack
)

// ✅ BOA PRÁTICA: Tamanho mínimo de toque (48dp)
Button(
    onClick = { },
    modifier = Modifier.size(48.dp) // Mínimo recomendado
) {
    Text("Ação")
}

// ✅ BOA PRÁTICA: Contraste de cores (WCAG 2.1 AA)
val textColor = if (isHighContrast) Color.Black else Color.DarkGray
val backgroundColor = Color.White
// Razão de contraste mínima: 4.5:1 para texto normal
```

### **6. Documentação KDoc (PRIORIDADE MÉDIA)**

```kotlin
/**
 * Repository para gerenciar operações de [Cliente].
 * 
 * Este repository implementa o padrão offline-first, onde:
 * - Dados locais (Room) são a fonte primária
 * - Sincronização com Firestore ocorre em background
 * - Conflitos são resolvidos por timestamp (last-write-wins)
 * 
 * @property dao DAO para acess

## 📚 REFERÊNCIAS

- [Android Developer - Architecture](https://developer.android.com/topic/architecture)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [StateFlow vs LiveData](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Firebase Firestore](https://firebase.google.com/docs/firestore)
