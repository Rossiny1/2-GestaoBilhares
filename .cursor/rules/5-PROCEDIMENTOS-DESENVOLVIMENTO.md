# 5. PROCEDIMENTOS DE DESENVOLVIMENTO

## 🛠️ SETUP E CONFIGURAÇÃO

### **Requisitos**
- Android Studio Hedgehog (2023.1.1) ou superior
- JDK 17 ou superior
- Kotlin 1.9+
- Gradle 8.1+

### **Configuração Inicial**
1. Clonar repositório
2. Abrir projeto no Android Studio
3. Sincronizar Gradle
4. Executar build: `./gradlew assembleDebug`
5. Instalar APK no dispositivo/emulador

## 📝 PADRÕES DE CÓDIGO

### **Kotlin Style Guide**
- Nomes em camelCase
- Classes em PascalCase
- Constantes em UPPER_SNAKE_CASE
- Packages em lowercase

### **Arquitetura**
- **MVVM**: ViewModel + StateFlow + repeatOnLifecycle
- **Repository Pattern**: AppRepository como Facade
- **Offline-first**: Dados sempre locais
- **Modularização**: Repositories por domínio

### **Exemplo de ViewModel (Observação Reativa)**
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyViewModel(
    private val appRepository: AppRepository
) : BaseViewModel() {
    // ✅ RECOMENDADO: Usar MutableStateFlow para IDs e observar com flatMapLatest
    private val _idFlow = MutableStateFlow<Long?>(null)
    
    private val _data = MutableStateFlow<List<Item>>(emptyList())
    val data: StateFlow<List<Item>> = _data.asStateFlow()
    
    init {
        // ✅ Observação reativa: atualiza automaticamente quando há mudanças no banco
        viewModelScope.launch {
            _idFlow
                .flatMapLatest { id ->
                    if (id == null) return@flatMapLatest flowOf(emptyList())
                    appRepository.obterDadosPorId(id) // Flow reativo do Room
                }
                .collect { items ->
                    _data.value = items
                }
        }
    }
    
    // ✅ Apenas atualiza o ID, o init observa automaticamente
    fun loadData(id: Long) {
        _idFlow.value = id
    }
}
```

### **Exemplo de Fragment/Screen**
```kotlin
// Compose
@Composable
fun MyScreen(
    viewModel: MyViewModel = hiltViewModel(),
    navController: NavController
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    when {
        state.isLoading -> LoadingIndicator()
        state.error != null -> ErrorMessage(state.error)
        else -> Content(state.data)
    }
}

// Fragment (Legacy)
class MyFragment : Fragment() {
    private lateinit var viewModel: MyViewModel
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[MyViewModel::class.java]
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    // Atualizar UI
                }
            }
        }
    }
}
```

## 🔄 WORKFLOW DE DESENVOLVIMENTO

### **1. Criar Nova Funcionalidade**
1. Identificar domínio (Client, Acerto, Mesa, etc.)
2. Adicionar método no Repository especializado
3. Expor via AppRepository (se necessário)
4. Criar/atualizar ViewModel
5. Criar/atualizar UI (Compose preferencial)
6. Testar offline
7. Commit e push

### **2. Refatorar Código Existente**
1. Identificar domínio
2. Mover código para Repository especializado (em `domain/`)
3. Repository especializado recebe DAOs no construtor
4. Atualizar AppRepository (delegação para repository especializado)
5. Manter compatibilidade (ViewModels não mudam)
6. Implementar observação reativa com `flatMapLatest` se necessário
7. Testar funcionalidades existentes
8. Commit e push

**Importante**: Repositories especializados NÃO devem ser acessados diretamente por ViewModels. Sempre usar AppRepository como Facade.

### **3. Migrar para Compose**
1. Criar Screen Compose
2. Manter ViewModel existente
3. Preservar UI idêntica
4. Testar funcionalidades
5. Remover Fragment (quando 100% migrado)
6. Commit e push

## 🧪 TESTES

### **Testes Manuais**
1. Testar fluxo completo offline
2. Testar todas as funcionalidades
3. Verificar estados de loading/error/empty
4. Validar cálculos financeiros
5. Testar navegação

### **Testes Automatizados (Futuro)**
- Unit tests para ViewModels
- Integration tests para Repositories
- UI tests para telas críticas

## 📦 BUILD E DEPLOY

### **Build Local**
```bash
# Debug
./gradlew assembleDebug

# Release (quando configurado)
./gradlew assembleRelease
```

### **APK Location**
```
app/build/outputs/apk/debug/app-debug.apk
```

### **Instalação Manual**
1. Transferir APK para dispositivo
2. Habilitar "Fontes desconhecidas"
3. Instalar APK
4. Testar funcionalidades

## 🔍 DEBUGGING

### **Logs**
- Tag padrão: `LOG_CRASH`
- Usar `android.util.Log.d()` para debug
- Usar `android.util.Log.e()` para erros

### **Script de Logs**
```powershell
# crash-simples.ps1
adb logcat -s LOG_CRASH:* RoutesScreen:* UserSessionManager:*
```

### **Pontos de Atenção**
- Sessão do usuário
- Carregamento de dados
- Navegação entre telas
- Cálculos financeiros
- Sincronização (quando implementada)

## 🚀 IMPLEMENTAÇÕES RECENTES

### **✅ Sincronização (CONCLUÍDA)**
1. ✅ `SyncRepository` especializado implementado
2. ✅ Integração com Firebase Firestore completa
3. ✅ Fila de sincronização offline-first implementada
4. ✅ WorkManager configurado
5. ✅ Estrutura Firestore corrigida (`empresas/empresa_001/entidades/{collectionName}/items`)
6. ✅ Conversão de tipos corrigida (Despesa, LocalDateTime)
7. ✅ Observação reativa implementada em ViewModels (flatMapLatest, stateIn)
8. ✅ Histórico de veículos (abastecimento/manutenção) funcionando
9. ✅ Equipment: Entidade completa com sincronização push/pull
10. ✅ MetaColaborador: Sincronização push/pull implementada
11. ✅ EquipmentsViewModel: Usando Flow reativo corretamente
12. ✅ **Fila de Sincronização Completa (Janeiro 2025)**:
    - ✅ Processamento completo da fila: `processSyncQueue()` processa todas as operações pendentes em loop
    - ✅ Operações DELETE: Todas as exclusões locais enfileiram operação DELETE
    - ✅ Logs detalhados: Sistema completo de rastreamento de operações
    - ✅ Regras Firestore: Permissões de DELETE para usuários autenticados
    - ✅ Verificação pós-DELETE: Confirmação de exclusão no Firestore

### **Prioridade MÉDIA: Migração Compose**
1. Migrar Core Business (Settlement, ClientList)
2. Migrar Ciclos (CycleManagement)
3. Migrar Despesas
4. Migrar Mesas
5. Migrar Gestão

### **Prioridade BAIXA: Otimizações**
1. Performance
2. Testes automatizados
3. Documentação
4. Acessibilidade

## 📚 RECURSOS

### **Documentação**
- Status: `1-STATUS-ATUAL-PROJETO.md`
- Arquitetura: `2-ARQUITETURA-TECNICA.md`
- Regras: `3-REGRAS-NEGOCIO.md`
- Fluxo: `4-FLUXO-PRINCIPAL-APLICACAO.md`

### **Referências Externas**
- [Android Developer](https://developer.android.com)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [StateFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)

## ✅ CHECKLIST DE QUALIDADE E BOAS PRÁTICAS

### **Antes de Cada Commit**

#### **1. Build e Compilação**
- [ ] Build passa sem erros: `./gradlew assembleDebug`
- [ ] Sem warnings críticos no Lint
- [ ] Código Kotlin segue convenções (ktlint)
- [ ] ProGuard rules atualizadas (se necessário)

#### **2. Testes (PRIORIDADE ALTA)**
- [ ] Testes unitários passando: `./gradlew test`
- [ ] Cobertura de código mantida ou aumentada
- [ ] Testes novos para funcionalidades novas
- [ ] Mocks atualizados se necessário

```bash
# Executar todos os testes
./gradlew test

# Executar testes com cobertura
./gradlew testDebugUnitTestCoverage

# Ver relatório de cobertura
# build/reports/coverage/debug/index.html
```

#### **3. Performance**
- [ ] Sem leaks de memória (LeakCanary)
- [ ] Listas grandes usando LazyColumn/RecyclerView
- [ ] Images otimizadas (< 500KB cada)
- [ ] Queries Room com índices apropriados

```kotlin
// ✅ CHECKLIST: Otimizações essenciais
class MyViewModel {
    // ✅ Cancelar coroutines ao destruir ViewModel
    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel() // Evita leaks
    }
    
    // ✅ Cache com limite
    private val cache = LruCache<String, Data>(100)
    
    // ✅ Debounce em buscas
    val searchQuery = MutableStateFlow("")
    val searchResults = searchQuery
        .debounce(300) // Aguarda 300ms
        .flatMapLatest { repository.search(it) }
}
```

#### **4. Segurança**
- [ ] Dados sensíveis criptografados
- [ ] Validação de entrada do usuário
- [ ] Permissões Android justificadas
- [ ] Tokens não versionados (gitignore)

```kotlin
// ❗ NÃO FAÇA: Hardcoded secrets
val apiKey = "sk_live_123456789" // ❌ ERRADO

// ✅ FAÇA: Use BuildConfig ou arquivo seguro
val apiKey = BuildConfig.API_KEY // ✅ CORRETO
```

#### **5. Accessibility**
- [ ] Content descriptions em imagens
- [ ] Tamanhos de toque >= 48dp
- [ ] Contraste de cores adequado
- [ ] Testado com TalkBack

```kotlin
// ✅ CHECKLIST A11y
// 1. Imagens
Image(
    painter = painterResource(R.drawable.ic_save),
    contentDescription = "Salvar alterações" // ✅ Sempre inclua
)

// 2. Botões com área de toque adequada
IconButton(
    onClick = { },
    modifier = Modifier.size(48.dp) // ✅ Mínimo
) { Icon(...) }

// 3. Contraste de cores
Text(
    text = "Importante",
    color = Color(0xFF000000), // Preto
    background = Color(0xFFFFFFFF) // Branco
    // Razão de contraste: 21:1 (WCAG AAA) ✅
)
```

### **Padrões de Código (Code Review)**

#### **ViewModels**
```kotlin
// ✅ BOM: StateFlow + Observação reativa
class GoodViewModel(private val repo: Repo) : ViewModel() {
    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()
    
    init {
        viewModelScope.launch {
            repo.getData()
                .catch { _state.value = State.Error(it) }
                .collect { _state.value = State.Success(it) }
        }
    }
}

// ❌ RUIM: LiveData + update manual
class BadViewModel(private val repo: Repo) : ViewModel() {
    val data = MutableLiveData<Data>()
    
    fun loadData() { // ❌ Manual, não reativo
        viewModelScope.launch {
            data.value = repo.getData() // ❌ Sem error handling
        }
    }
}
```

#### **Repositories**
```kotlin
// ✅ BOM: Flow reativo do Room
class GoodRepository(private val dao: Dao) {
    fun getData(): Flow<List<Item>> = dao.getAll() // ✅ Reativo
    
    suspend fun insert(item: Item) {
        withContext(Dispatchers.IO) {
            dao.insert(item)
        }
    }
}

// ❌ RUIM: Listas estáticas
class BadRepository(private val dao: Dao) {
    suspend fun getData(): List<Item> = dao.getAllSync() // ❌ Não reativo
}
```

### **Testes de Regressão (Manual)**

Antes de release, testar manualmente:

**Fluxos Críticos**:
1. ✅ Login/Logout
2. ✅ Criação de cliente
3. ✅ Acerto (settlement)
4. ✅ Geração de relatório
5. ✅ Sincronização (offline → online)

**Cenários Edge**:
- [ ] App funciona offline
- [ ] Recriação de configuração (rotação de tela)
- [ ] Memória baixa (background apps)
- [ ] Rede lenta/instável
- [ ] Dados inválidos/edge cases

### **Métricas de Qualidade**

| Métrica | Target | Como Verificar |
|---------|--------|----------------|
| **Cobertura de Testes** | > 60% | `./gradlew testDebugUnitTestCoverage` |
| **Warnings** | 0 críticos | Android Studio Lint |
| **Memory Leaks** | 0 | LeakCanary em debug |
| **Crash Rate** | < 1% | Firebase Crashlytics (produção) |
| **Build Time** | < 5min | Gradle build scan |
| **APK Size** | < 50MB | `app/build/outputs/apk/` |

### **Documentação KDoc**

```kotlin
/**
 * ViewModel para gerenciar acertos de clientes.
 * 
 * **Responsabilidades**:
 * - Carregar lista de acertos via [AcertoRepository]
 * - Calcular totais (fichas, valores)
 * - Filtrar por período/cliente
 * 
 * **Estados**:
 * - [Loading]: Carregando dados
 * - [Success]: Dados disponíveis
 * - [Error]: Erro ao carregar
 * 
 * @property repository Fonte de dados de acertos
 * @constructor Cria ViewModel com injeção de [AcertoRepository]
 * 
 * @see AcertoRepository
 * @see Acerto
 * 
 * @sample
 * ```kotlin
 * val viewModel = SettlementViewModel(repository)
 * viewModel.state.collect { state ->
 *     when (state) {
 *         is Loading -> showLoading()
 *         is Success -> showData(state.acertos)
 *         is Error -> showError(state.message)
 *     }
 * }
 * ```
 */
@HiltViewModel
class SettlementViewModel @Inject constructor(
    private val repository: AcertoRepository
) : BaseViewModel() { ... }
```

### **Git Commit Messages**

Seguir convenção Conventional Commits:

```bash
# Formato:
<type>(<scope>): <subject>

# Exemplos:
feat(clients): adicionar filtro por rota
fix(sync): corrigir timestamp após push
test(settlement): adicionar testes para cálculo de total
refactor(repository): extrair lógica para ClienteRepository
docs(readme): atualizar instruções de setup
perf(database): adicionar índices para queries frequentes

# Types:
# feat: Nova funcionalidade
# fix: Correção de bug
# refactor: Refatoração sem mudança de comportamento
# test: Adição/correção de testes
# docs: Documentação
# perf: Otimização de performance
# chore: Manutenção (build, deps, etc)
```

---

## ⚠️ AVISOS IMPORTANTES

1. **Nunca quebrar compatibilidade**: ViewModels devem continuar usando AppRepository
2. **Offline-first**: Sempre testar offline
3. **Modularização**: Trabalhar em domínios diferentes para evitar conflitos
4. **Commits frequentes**: Facilitar rollback se necessário
5. **Testes antes de commit**: Garantir que build passa

