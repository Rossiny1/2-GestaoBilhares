# 5. PROCEDIMENTOS DE DESENVOLVIMENTO

> **Documento de desenvolvimento** - Regras fundamentais, padrões de código, comandos, troubleshooting e boas práticas.

## 🚀 REGRAS FUNDAMENTAIS

### Preservação do Progresso
- **NUNCA** comprometer funcionalidades já implementadas
- **SEMPRE** verificar funcionalidades existentes antes de implementar
- **SEMPRE** fazer builds intermediários para validação
- **SEMPRE** trabalhar em paralelo para otimização

### 🎯 REGRA PRINCIPAL: CENTRALIZAÇÃO E SIMPLIFICAÇÃO
- **CENTRALIZAR**: Manter funcionalidades relacionadas em um único local
- **SIMPLIFICAR**: Evitar fragmentação desnecessária de código
- **UM ARQUIVO, UMA RESPONSABILIDADE**: Manter coesão alta
- **ELIMINAR DUPLICAÇÃO**: Reutilizar código existente
- **FACILITAR MANUTENÇÃO**: Código organizado e acessível

### Modernização Incremental (2025)
- **StateFlow First**: Priorizar StateFlow sobre LiveData
- **BaseViewModel**: Usar funcionalidades centralizadas
- **repeatOnLifecycle**: Padrão moderno de observação
- **Material Design 3**: Usar componentes MD3 e tema dinâmico
- **KSP**: Usar KSP em vez de KAPT (mais rápido)
- **Zero Crashes**: Garantir estabilidade em todas as telas
- **Centralização**: AppRepository como único ponto de acesso
- **Segurança**: Criptografia, sanitização de logs, validação de inputs

### Responsabilidades do Usuário
- **Builds**: Usuário executa todos os builds e geração de APK
- **Testes**: Usuário realiza testes manuais
- **Validação**: Usuário confirma funcionamento antes de prosseguir

## 💻 PADRÕES DE CÓDIGO

### StateFlow Migration

```kotlin
// ❌ PADRÃO ANTIGO: LiveData
private val _data = MutableLiveData<String>()
val data: LiveData<String> = _data

// ✅ PADRÃO MODERNO: StateFlow
private val _data = MutableStateFlow<String>("")
val data: StateFlow<String> = _data.asStateFlow()
```

### Observação Moderna

```kotlin
// ❌ PADRÃO ANTIGO: observe
viewModel.data.observe(viewLifecycleOwner) { value ->
    // Atualizar UI
}

// ✅ PADRÃO MODERNO: collect + repeatOnLifecycle
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.data.collect { value ->
            // Atualizar UI
        }
    }
}
```

### BaseViewModel Usage

```kotlin
// ✅ PADRÃO MODERNO: Herdar de BaseViewModel
class MyViewModel : BaseViewModel() {
    fun doSomething() {
        showLoading()
        try {
            // Lógica de negócio
            showMessage("Sucesso!")
        } catch (e: Exception) {
            showError("Erro: ${e.message}")
        } finally {
            hideLoading()
        }
    }
}
```

### ViewModel Initialization

```kotlin
// ✅ PADRÃO MODERNO: Usar by viewModels() com Hilt
class MyFragment : Fragment() {
    private val viewModel: MyViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }
}

// ⚠️ CASO ESPECIAL: Inicialização manual apenas quando necessário
// (ex: quando ViewModel precisa de parâmetros específicos do Fragment)
class MyFragment : Fragment() {
    private lateinit var viewModel: MyViewModel
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Apenas quando by viewModels() não é suficiente
        val database = AppDatabase.getDatabase(requireContext())
        val repository = Repository(database.dao())
        viewModel = MyViewModel(repository)
        
        setupUI()
        observeViewModel()
    }
}
```

### Repository Pattern

```kotlin
// ✅ CORRETO: Usar suspend functions
suspend fun obterClientePorId(id: Long): Cliente? {
    return clienteDao.obterClientePorId(id)
}

// ✅ CORRETO: Retornar Flow para observação reativa
fun obterClientesPorRota(rotaId: Long): Flow<List<Cliente>> {
    return clienteDao.obterClientesPorRota(rotaId)
}

// ✅ CORRETO: Usar Result para tratamento de erros
suspend fun salvarCliente(cliente: Cliente): Result<Long> {
    return try {
        val id = clienteDao.inserir(cliente)
        Result.success(id)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// ❌ EVITAR: runBlocking (causa ANR)
fun obterClienteSync(id: Long): Cliente? {
    return runBlocking { clienteDao.obterClientePorId(id) }
}
```

### Tratamento de Erros

```kotlin
// ✅ CORRETO: Usar Result ou sealed class para estados
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

// ✅ CORRETO: Logs sanitizados em produção
AppLogger.d("Tag", "Cliente salvo: ${cliente.nome}") // CPF será sanitizado automaticamente
```

## 🔧 COMANDOS E FERRAMENTAS

### Comandos de Build

```bash
./gradlew clean
./gradlew build
./gradlew assembleDebug
./gradlew compileDebugKotlin
```

### Comandos de Sistema

```bash
# Windows PowerShell
taskkill /f /im java.exe
Get-ChildItem
Select-String
```

### Recovery de Daemon Kotlin

```bash
./gradlew --stop
taskkill /f /im java.exe
./gradlew clean --no-daemon
```

## 🐛 RESOLUÇÃO DE PROBLEMAS

### Build Failures
1. **Diagnóstico**: Usar `--stacktrace` para identificar erros
2. **Limpeza**: `gradlew clean` antes de rebuild
3. **Recovery**: Parar daemons se necessário
4. **Validação**: Build intermediário após correções

### Logs e Debug
- **Logcat**: Usar caminho específico do ADB
- **Logs Detalhados**: Adicionar em componentes críticos
- **Análise**: Capturar logs durante testes

## 📱 TESTES E VALIDAÇÃO

### Testes Unitários (2025)

```kotlin
// ✅ CORRETO: Usar Turbine para testar Flows
@Test
fun `deve emitir clientes quando carregar`() = runTest {
    val flow = viewModel.clientes.testIn(this)
    
    viewModel.carregarClientes(rotaId = 1L)
    
    assertEquals(emptyList<Cliente>(), flow.awaitItem())
    // ... mais asserções
}

// ✅ CORRETO: Usar MockK para mocks
@Test
fun `deve chamar repository ao salvar`() = runTest {
    val repository = mockk<AppRepository>()
    val viewModel = MyViewModel(repository)
    
    coEvery { repository.inserirCliente(any()) } returns 1L
    
    viewModel.salvarCliente(cliente)
    
    coVerify { repository.inserirCliente(cliente) }
}
```

### Testes Instrumentados

```kotlin
// ✅ CORRETO: Usar Espresso para testes de UI
@Test
fun testLogin() {
    onView(withId(R.id.emailEditText))
        .perform(typeText("test@example.com"))
    onView(withId(R.id.passwordEditText))
        .perform(typeText("password"))
    onView(withId(R.id.loginButton))
        .perform(click())
    
    onView(withId(R.id.routesFragment))
        .check(matches(isDisplayed()))
}
```

### Fluxo de Testes
1. **Build**: Gerar APK de debug
2. **Testes Automatizados**: Executar testes unitários e instrumentados
3. **Instalação**: Transferir para dispositivo
4. **Teste Manual**: Validar funcionalidades críticas
5. **Logs**: Capturar logs se necessário
6. **Correção**: Ajustar baseado nos resultados

### Validações Críticas
- **Login**: Autenticação funcionando (online/offline)
- **Navegação**: Fluxo entre telas
- **Dados**: Persistência no banco
- **Sincronização**: App ↔ Firestore
- **Contratos**: Geração e assinatura
- **Relatórios**: PDF e impressão

## 🎯 EXEMPLOS DE CENTRALIZAÇÃO

### ✅ CORRETO: Centralização

```kotlin
// ✅ UM REPOSITORY CENTRALIZADO
@Singleton
class AppRepository @Inject constructor(...) {
    // Todas as operações de dados em um local
    fun obterTodosClientes(): Flow<List<Cliente>>
    fun obterTodasRotas(): Flow<List<Rota>>
    fun obterTodasMesas(): Flow<List<Mesa>>
    
    // Cache centralizado
    private val _clientesCache = MutableStateFlow<List<Cliente>>(emptyList())
    val clientesCache: StateFlow<List<Cliente>> = _clientesCache.asStateFlow()
}
```

### ❌ INCORRETO: Fragmentação Desnecessária

```kotlin
// ❌ MÚLTIPLOS REPOSITORIES FRAGMENTADOS
class ClientRepository @Inject constructor(...)
class RouteRepository @Inject constructor(...)
class MesaRepository @Inject constructor(...)
// ... mais 10 repositories
```

## ⚠️ CUIDADOS ESPECIAIS

### Evitar Loops
- **Não repetir** verificações desnecessárias
- **Focar** no problema principal
- **Usar** ferramentas de diagnóstico adequadas

### Preservar Funcionalidades
- **Verificar** dependências antes de remover
- **Manter** compatibilidade com código existente
- **Testar** funcionalidades relacionadas

### Eficiência
- **Trabalhar em paralelo** quando possível
- **Usar** ferramentas apropriadas para cada tarefa
- **Otimizar** tempo de desenvolvimento

## 🔒 SEGURANÇA E PRIVACIDADE (2025)

### Boas Práticas de Segurança

```kotlin
// ✅ CORRETO: Criptografar dados sensíveis
val cpfCriptografado = DataEncryption.encrypt(cpf)

// ✅ CORRETO: Sanitizar logs
AppLogger.d("Tag", "Cliente: ${cliente.nome}") // CPF será sanitizado

// ✅ CORRETO: Validar inputs
if (!DataValidator.validarCPF(cpf)) {
    return Result.failure(IllegalArgumentException("CPF inválido"))
}

// ✅ CORRETO: Usar Android Keystore para chaves
val keyStore = KeyStore.getInstance("AndroidKeyStore")
```

### Permissões
- **Mínimas Necessárias**: Solicitar apenas permissões essenciais
- **Runtime Permissions**: Sempre verificar permissões em runtime
- **Justificativa**: Explicar ao usuário por que a permissão é necessária

## 📚 IMPORTS NECESSÁRIOS

```kotlin
// Para StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Para repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch

// Para ViewModel (Hilt)
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint

// Para testes
import kotlinx.coroutines.test.runTest
import app.cash.turbine.test
import io.mockk.mockk
import io.mockk.coEvery
import io.mockk.coVerify
```

## 🎨 MATERIAL DESIGN 3

### Tema e Cores

```kotlin
// ✅ CORRETO: Usar atributos de tema
android:textColor="?attr/colorOnSurface"
android:background="?attr/colorSurface"

// ✅ CORRETO: Usar Material3 components
<com.google.android.material.button.MaterialButton
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    style="@style/Widget.Material3.Button" />
```

### Componentes Modernos
- **MaterialAlertDialogBuilder**: Em vez de AlertDialog.Builder
- **Material3 TextInputLayout**: Widget.Material3.TextInputLayout.OutlinedBox
- **Material3 Cards**: Usar cardCornerRadius e elevation do tema

---

**Última atualização**: 2025-01-09

