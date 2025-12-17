# 5️⃣ BEST PRACTICES - Padrões de Qualidade

> **Propósito**: Padrões de qualidade e boas práticas Android 2025  
> **Audiência**: Todos os desenvolvedores  
> **Última Atualização**: Dezembro 2025

---

## 🎯 Android 2025 - Resumo Executivo

### Princípios Fundamentais
1. **Offline-First**: Dados locais (Room) como fonte primária
2. **Reactive**: StateFlow para observação reativa automática
3. **Modern UI**: Jetpack Compose (migrando de View System)
4. **Dependency Injection**: Hilt (único padrão permitido)
5. **Type-Safe**: Navigation Component com SafeArgs
6. **Testing**: Cobertura mínima 60% (ViewModels + Repositories)

---

## ✅ Checklist Antes de Cada Commit

### 1. Build & Compilação
```bash
# ✅ Build deve passar sem erros
./gradlew assembleDebug

# ✅ Sem warnings críticos
./gradlew lint

# ✅ Código Kotlin formatado
./gradlew ktlintFormat
```

### 2. Testes
```bash
# ✅ Todos os testes passando
./gradlew test

# ✅ Cobertura mantida/aumentada
./gradlew testDebugUnitTestCoverage

# Ver relatório: build/reports/coverage/debug/index.html
```

### 3. Funcionalidade
- [ ] App funciona **offline**
- [ ] Estados de loading/error/empty implementados
- [ ] Navegação funciona corretamente
- [ ] Dados persistem após fechar app

### 4. Código
- [ ] Segue padrões (ver abaixo)
- [ ] StateFlow (não LiveData)
- [ ] `repeatOnLifecycle` (não `observe`)
- [ ] Hilt para DI (não manual)
- [ ] KDoc em classes/funções públicas

### 5. Git
- [ ] Mensagem segue Conventional Commits
- [ ] Commits atômicos (uma mudança lógica)
- [ ] Sem arquivos sensíveis (tokens, keys)
- [ ] Branch atualizado com main

**Exemplo de commit**:
```bash
git commit -m "feat(clients): adicionar filtro por débito alto"
```

---

## 📝 Padrões de Código

### ViewModels: ✅ Bom vs ❌ Ruim

#### ✅ BOM: StateFlow + Observação Reativa
```kotlin
@HiltViewModel
class ClientListViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {
    // ✅ MutableStateFlow para IDs
    private val _rotaId = MutableStateFlow<Long?>(null)
    
    // ✅ Flow reativo com flatMapLatest
    val clientes: StateFlow<List<Cliente>> = _rotaId
        .flatMapLatest { id ->
            id?.let { appRepository.obterClientesPorRota(it) }
                ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    // ✅ Apenas atualiza o ID, Room observa automaticamente
    fun loadRota(rotaId: Long) {
        _rotaId.value = rotaId
    }
}
```

#### ❌ RUIM: LiveData + Update Manual
```kotlin
class ClientListViewModel(
    private val repository: AppRepository
) : ViewModel() {
    // ❌ LiveData (legado)
    val clientes = MutableLiveData<List<Cliente>>()
    
    // ❌ Manual, não reativo
    fun loadClientes(rotaId: Long) {
        viewModelScope.launch {
            // ❌ Sem error handling
            val result = repository.obterClientesPorRotaSync(rotaId)
            clientes.value = result
        }
    }
}
```

### Repositories:  ✅ Bom vs ❌ Ruim

#### ✅ BOM: Flow Reativo
```kotlin
class ClienteRepository @Inject constructor(
    private val clienteDao: ClienteDao
) {
    // ✅ Flow reativo do Room (observa mudanças automaticamente)
    fun obterPorRota(rotaId: Long): Flow<List<Cliente>> =
        clienteDao.obterPorRota(rotaId)
    
    // ✅ Suspend functions para operações write
    suspend fun inserir(cliente: Cliente) = withContext(Dispatchers.IO) {
        clienteDao.insert(cliente)
    }
    
    // ✅ Error handling com Result
    suspend fun sincronizar(): Result<Unit> = try {
        // ...
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

#### ❌ RUIM: Listas Estáticas
```kotlin
class ClienteRepository(private val dao: ClienteDao) {
    // ❌ Não reativo (não observa mudanças)
    suspend fun obterPorRota(rotaId: Long): List<Cliente> =
        dao.obterPorRotaSync(rotaId)
    
    // ❌ Sem error handling
    suspend fun inserir(cliente: Cliente) {
        dao.insert(cliente) // Pode crashar
    }
}
```

### Compose: ✅ Bom vs ❌ Ruim

#### ✅ BOM: collectAsStateWithLifecycle + Key
```kotlin
@Composable
fun ClientListScreen(
    viewModel: ClientListViewModel = hiltViewModel()
) {
    // ✅ Lifecycle-aware collection
    val clientes by viewModel.clientes.collectAsStateWithLifecycle()
    
    LazyColumn {
        items(
            items = clientes,
            key = { it.id } // ✅ Key para recomposição eficiente
        ) { cliente ->
            ClientCard(cliente)
        }
    }
}
```

#### ❌ RUIM: collectAsState + Sem Key
```kotlin
@Composable
fun ClientListScreen(viewModel: ClientListViewModel) {
    // ❌ Não lifecycle-aware (pode vazar)
    val clientes by viewModel.clientes.collectAsState()
    
    LazyColumn {
        // ❌ Sem key (recomposição ineficiente)
        items(clientes) { cliente ->
            ClientCard(cliente)
        }
    }
}
```

### Fragment (Legacy): ✅ Bom vs ❌ Ruim

#### ✅ BOM: repeatOnLifecycle
```kotlin
@AndroidEntryPoint
class ClientListFragment : Fragment() {
    private val viewModel: ClientListViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ Lifecycle-aware, cancela automaticamente
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.clientes.collect { clientes ->
                    adapter.submitList(clientes)
                }
            }
        }
    }
}
```

#### ❌ RUIM: observe (LiveData)
```kotlin
class ClientListFragment : Fragment() {
    private val viewModel: ClientListViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ❌ LiveData (legado)
        viewModel.clientes.observe(viewLifecycleOwner) { clientes ->
            adapter.submitList(clientes)
        }
    }
}
```

---

## 🧪 Estratégia de Testes

### Pirâmide de Testes

```
        /\
       /UI\      10% - Testes de UI (Espresso)
      /____\
     /      \
    /Integr.\   20% - Testes de Integração
   /__________\
  /            \
 /    Unit      \ 70% - Testes Unitários
/________________\
```

**Distribuição**:
- **70% Unit Tests**: ViewModels, Repositories, Utils
- **20% Integration Tests**: Repositories + Room, Sync
- **10% UI Tests**: Fluxos críticos (Login, Acerto)

### Exemplo: Teste de ViewModel

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ClientListViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    // ✅ StandardTestDispatcher para controle manual
    private val testDispatcher = StandardTestDispatcher()
    private val mockRepository = mock<AppRepository>()
    private lateinit var viewModel: ClientListViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ClientListViewModel(mockRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
   fun `quando rota é carregada, clientes são emitidos`() = runTest {
        // Given
        val rotaId = 1L
        val testClientes = listOf(Cliente(id = 1, nome = "Test"))
        whenever(mockRepository.obterClientesPorRota(rotaId))
            .thenReturn(flowOf(testClientes))
        
        // When
        viewModel.loadRota(rotaId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val clientes = viewModel.clientes.value
        assertEquals(testClientes, clientes)
    }
    
    @Test
    fun `quando erro ocorre, estado de erro é emitido`() = runTest {
        // Given
        val exception = Exception("Network error")
        whenever(mockRepository.obterClientesPorRota(any()))
            .thenReturn(flow { throw exception })
        
        // When
        viewModel.loadRota(1L)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.errorState.value != null)
    }
}
```

### Exemplo: Teste de Repository

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ClienteRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ClienteDao
    private lateinit var repository: ClienteRepository
    
    @Before
    fun setup() {
        // ✅ In-memory database para testes
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        dao = database.clienteDao()
        repository = ClienteRepository(dao)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun `inserir cliente e observar via Flow`() = runTest {
        // Given
        val cliente = Cliente(id = 1, nome = "Test", rotaId = 1)
        
        // When
        repository.inserir(cliente)
        
        // Then - Flow reativo emite automaticamente
        repository.obterPorRota(1).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Test", result[0].nome)
        }
    }
}
```

---

## ⚡ Performance e Otimização

### Memory Management

```kotlin
// ✅ BOA PRÁTICA: Cache com limite
class CacheManager<K, V>(private val maxSize: Int = 100) {
    private val cache = object : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?) =
            size > maxSize
    }
    
    fun put(key: K, value: V) = cache.put(key, value)
    fun get(key: K): V? = cache[key]
    fun clear() = cache.clear()
}

// ✅ BOA PRÁTICA: Cancelar coroutines ao destruir ViewModel
class MyViewModel : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel() // Evita leaks
    }
}

// ✅ BOA PRÁTICA: Debounce em buscas
val searchQuery = MutableStateFlow("")
val searchResults = searchQuery
    .debounce(300) // Aguarda 300ms antes de buscar
    .flatMapLatest { query -> repository.search(query) }
```

### Database Optimization

```kotlin
// ✅ BOA PRÁTICA: Índices Room para queries frequentes
@Entity(
    tableName = "clientes",
    indices = [
        Index(value = ["rotaId"]),          // Filtro por rota
        Index(value = ["dataAtualizacao"]), // Sync incremental
        Index(value = ["nome"])             // Busca por nome
    ]
)
data class Cliente(...)

// ✅ BOA PRÁTICA: Paginação com Paging 3
@Dao
interface ClienteDao {
    @Query("SELECT * FROM clientes ORDER BY nome ASC")
    fun getPagedClientes(): PagingSource<Int, Cliente>
}

// ViewModel
val clientes: Flow<PagingData<Cliente>> = Pager(
    config = PagingConfig(pageSize = 20),
    pagingSourceFactory = { dao.getPagedClientes() }
).flow.cachedIn(viewModelScope)
```

### Compose Performance

```kotlin
// ✅ BOA PRÁTICA: remember para cálculos pesados
@Composable
fun ExpensiveComponent(data: List<Item>) {
    val processedData = remember(data) {
        data.sortedBy { it.priority }.take(10)
    }
    
    LazyColumn {
        items(processedData, key = { it.id }) { item ->
            ItemCard(item)
        }
    }
}

// ✅ BOA PRÁTICA: derivedStateOf para evitar recomposições
@Composable
fun ScrollableList(items: List<Item>) {
    val listState = rememberLazyListState()
    
    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 3
        }
    }
    
    if (showButton) {
        FloatingActionButton(onClick = { /* scroll to top */ })
    }
}
```

---

## 🔐 Segurança

### Dados Sensíveis

```kotlin
// ✅ BOA PRÁTICA: EncryptedSharedPreferences
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

### Validação de Entrada

```kotlin
// ✅ BOA PRÁTICA: Validação robusta
fun validarEmail(email: String): Result<String> {
    return when {
        email.isBlank() -> 
            Result.failure(Exception("Email vazio"))
        !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> 
            Result.failure(Exception("Email inválido"))
        else -> 
            Result.success(email.trim())
    }
}

fun validarValorFinanceiro(valor: Double): Result<Double> {
    return when {
        valor < 0 -> 
            Result.failure(Exception("Valor não pode ser negativo"))
        valor > 1_000_000 -> 
            Result.failure(Exception("Valor muito alto"))
        else -> 
            Result.success(valor)
    }
}
```

### ProGuard/R8

```proguard
# Ofuscação para produção
-optimizations !code/simplification/arithmetic
-optimizationpasses 5
-allowaccessmodification

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Firebase
-keep class com.google.firebase.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
```

---

## ♿ Acessibilidade (A11y)

### Content Descriptions

```kotlin
// ✅ Compose
Image(
    painter = painterResource(R.drawable.ic_client),
    contentDescription = "Foto do cliente" // Essencial para TalkBack
)

Button(onClick = { }) {
    Icon(
        imageVector = Icons.Default.Save,
        contentDescription = "Salvar alterações"
    )
}

// ✅ View System
imageView.contentDescription = "Foto do cliente"
```

### Tamanho Mínimo de Toque

```kotlin
// ✅ Mínimo 48dp (recomendação WCAG)
Button(
    onClick = { },
    modifier = Modifier
        .size(48.dp) // ✅ Tamanho adequado
        .semantics { role = Role.Button }
) {
    Icon(Icons.Default.Delete)
}
```

### Contraste de Cores

```kotlin
// ✅ Contraste mínimo 4.5:1 para texto normal (WCAG AA)
Text(
    text = "Texto importante",
    color = Color(0xFF000000), // Preto
    modifier = Modifier.background(Color(0xFFFFFFFF)) // Branco
    // Razão de contraste: 21:1 (WCAG AAA) ✅
)

// ⚠️ Verificar contraste com ferramenta:
// https://webaim.org/resources/contrastchecker/
```

---

## 📋 Git Conventions

### Conventional Commits

```bash
# Formato: <type>(<scope>): <subject>

# Types:
feat      # Nova funcionalidade
fix       # Correção de bug
refactor  # Refatoração sem mudança de comportamento
test      # Adição/correção de testes
docs      # Documentação
perf      # Otimização de performance
chore     # Manutenção (build, deps)
style     # Formatação de código

# Exemplos:
git commit -m "feat(clients): adicionar filtro por débito alto"
git commit -m "fix(sync): corrigir timestamp após push"
git commit -m "test(settlement): adicionar testes para cálculo de total"
git commit -m "refactor(repository): extrair lógica para ClienteRepository"
git commit -m "docs(readme): atualizar instruções de setup"
git commit -m "perf(database): adicionar índices para queries frequentes"
```

### Branch Naming

```bash
# Formato: <type>/<description>

# Exemplos:
feature/client-debt-filter
fix/sync-timestamp-bug
refactor/app-repository-split
test/settlement-viewmodel
docs/architecture-update
```

---

## 📊 Métricas de Qualidade

| Métrica | Target | Como Verificar |
|---------|--------|----------------|
| **Cobertura de Testes** | >60% | `./gradlew testDebugUnitTestCoverage` |
| **Warnings** | 0 críticos | Android Studio Lint |
| **Memory Leaks** | 0 | LeakCanary em debug |
| **Crash Rate** | <1% | Firebase Crashlytics |
| **Build Time** | <5min | Gradle build scan |
| **APK Size** | <50MB | `app/build/outputs/apk/` |
| **Frame Rate** | 60 FPS | Android Studio Profiler |
| **Cold Start** | <2s | Logcat timestamps |

---

## ⚠️ Avisos Críticos

### NÃO FAÇA

```kotlin
// ❌ NÃO: Hardcoded secrets
val apiKey = "sk_live_123456789"

// ❌ NÃO: Logs de debug em produção
if (BuildConfig.DEBUG) {
    Log.d("TAG", "Debug info")
    Timber.plant(Timber.DebugTree())
}

// ❌ NÃO: LiveData (usar StateFlow)
val data = MutableLiveData<List<Item>>()

// ❌ NÃO: GlobalScope (usar viewModelScope)
GlobalScope.launch { }

// ❌ NÃO: Suspend em Main thread sem coroutine
runBlocking { }

// ❌ NÃO: Injeção manual (usar Hilt)
val repository = AppRepository(dao, context)
```

### SEMPRE FAÇA

```kotlin
// ✅ SIM: BuildConfig ou arquivo seguro
val apiKey = BuildConfig.API_KEY

// ✅ SIM: Timber com CrashlyticsTree em produção
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
} else {
    Timber.plant(CrashlyticsTree())
}

// ✅ SIM: StateFlow
val data = MutableStateFlow<List<Item>>(emptyList())

// ✅ SIM: viewModelScope
viewModelScope.launch { }

// ✅ SIM: withContext para IO
suspend fun fetch() = withContext(Dispatchers.IO) { }

// ✅ SIM: Hilt
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel()
```

---

## 🔗 Referências

### Documentação do Projeto
- [GUIA-RAPIDO.md](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/1-GUIA-RAPIDO.md) - Setup e comandos
- [ARQUITETURA-REFERENCIA.md](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/2-ARQUITETURA-REFERENCIA.md) - Detalhes técnicos
- [REGRAS-NEGOCIO.md](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/3-REGRAS-NEGOCIO.md) - Lógica de negócio
- [STATUS-ROADMAP.md](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/4-STATUS-ROADMAP.md) - Status e planejamento

### Links Externos
- [Android Best Practices](https://developer.android.com/topic/architecture/recommendations)
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Jetpack Compose Guidelines](https://developer.android.com/jetpack/compose/performance)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [OWASP Mobile Top 10](https://owasp.org/www-project-mobile-top-10/)
