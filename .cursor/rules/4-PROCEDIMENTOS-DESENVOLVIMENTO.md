# 4. PROCEDIMENTOS DE DESENVOLVIMENTO

## 🚀 REGRAS FUNDAMENTAIS

### **Preservação do Progresso**

- **NUNCA** comprometer funcionalidades já implementadas
- **SEMPRE** verificar funcionalidades existentes antes de implementar
- **SEMPRE** fazer builds intermediários para validação
- **SEMPRE** trabalhar em paralelo para otimização

### **🎯 REGRA PRINCIPAL: CENTRALIZAÇÃO E SIMPLIFICAÇÃO**

- **CENTRALIZAR**: Manter funcionalidades relacionadas em um único local
- **SIMPLIFICAR**: Evitar fragmentação desnecessária de código
- **UM ARQUIVO, UMA RESPONSABILIDADE**: Manter coesão alta
- **ELIMINAR DUPLICAÇÃO**: Reutilizar código existente
- **FACILITAR MANUTENÇÃO**: Código organizado e acessível

### **Modernização Incremental (2025)**

- **StateFlow First**: Priorizar StateFlow sobre LiveData
- **BaseViewModel**: Usar funcionalidades centralizadas
- **repeatOnLifecycle**: Padrão moderno de observação
- **Performance**: Otimizar com coroutines e StateFlow
- **ViewModel Initialization**: SEMPRE usar inicialização manual
- **Zero Crashes**: Garantir estabilidade em todas as telas
- **Jetpack Compose**: UI moderna e componentes reutilizáveis
- **Material 3**: Design system consistente
- **Componentes Customizados**: GestaoBilharesButton, GestaoBilharesTextField, GestaoBilharesCard

### **Responsabilidades do Usuário**

- **Builds**: Usuário executa todos os builds e geração de APK (não automatizar no assistente)
- **Testes**: Usuário realiza testes manuais
- **Validação**: Usuário confirma funcionamento antes de prosseguir

## 🔧 COMANDOS E FERRAMENTAS

### **Comandos de Build (Auto-aprovados)**

```bash
gradlew tasks
gradlew clean
gradlew build
gradlew compileDebugKotlin
gradlew assembleDebug
```

### **Comandos de Sistema (Auto-aprovados)**

```bash
dir / ls
Get-ChildItem
tasklist
Select-String
```

### **Comandos de Desenvolvimento (Auto-aprovados)**

- Criar, editar, excluir arquivos `.kt`, `.xml`, `.gradle`
- Comentar/descomentar imports
- Remover dependências problemáticas
- Criar implementações mock
- Operações de limpeza de cache

## 🐛 RESOLUÇÃO DE PROBLEMAS

### **Build Failures**

1. **Diagnóstico**: Usar `--stacktrace` para identificar erros
2. **Limpeza**: `gradlew clean` antes de rebuild
3. **Recovery**: Parar daemons se necessário
4. **Validação**: Build intermediário após correções

### **Recovery de Daemon Kotlin**

```bash
./gradlew --stop
taskkill /f /im java.exe
./gradlew clean --no-daemon
```

### **Logs e Debug**

- **Logcat**: Usar caminho específico do ADB (ver `crash-simples.ps1`)
- **Logs Detalhados**: Adicionar em componentes críticos
- **Análise**: Capturar logs durante testes
- **Tag padrão**: `LOG_CRASH` para diagnósticos críticos
- **Scripts**: Não criar novos scripts; manter e ajustar os existentes; sem Unicode/emoji

## 📱 TESTES E VALIDAÇÃO

### **Fluxo de Testes**

1. **Build**: Gerar APK de debug
2. **Instalação**: Transferir para dispositivo
3. **Teste Manual**: Validar funcionalidades
4. **Logs**: Capturar logs se necessário
5. **Correção**: Ajustar baseado nos resultados

### **Validações Críticas**

- **Login**: Autenticação funcionando
- **Navegação**: Fluxo entre telas
- **Dados**: Persistência no banco
- **Contratos**: Geração e assinatura
- **Relatórios**: PDF e impressão

## 🔄 METODOLOGIA DE TRABALHO

### **Abordagem Sistemática**

- **Análise Profunda**: Entender código existente
- **Implementação Incremental**: Pequenas mudanças
- **Validação Contínua**: Testes após cada alteração
- **Documentação**: Atualizar regras quando necessário

### **Comunicação**

- **Explicações Detalhadas**: Para desenvolvedor iniciante
- **Código Comentado**: Facilitar compreensão
- **Logs Claros**: Sem jargão técnico
- **Visualização**: Explicações fáceis de visualizar

## ⚠️ CUIDADOS ESPECIAIS

### **Evitar Loops**

- **Não repetir** verificações desnecessárias
- **Focar** no problema principal
- **Usar** ferramentas de diagnóstico adequadas

### **Preservar Funcionalidades**

- **Verificar** dependências antes de remover
- **Manter** compatibilidade com código existente
- **Testar** funcionalidades relacionadas

### **Eficiência**

- **Trabalhar em paralelo** quando possível
- **Usar** ferramentas apropriadas para cada tarefa
- **Otimizar** tempo de desenvolvimento

## 🎨 MIGRAÇÃO JETPACK COMPOSE - LIÇÕES APRENDIDAS

### **Ícones Material Icons - REGRAS CRÍTICAS**

#### **✅ ÍCONES QUE FUNCIONAM (Já testados no projeto):**

```kotlin
// ✅ ÍCONES CONFIRMADOS FUNCIONANDO:
Icons.Default.ArrowBack    // Navegação
Icons.Default.Search       // Busca/Câmera
Icons.Default.Menu         // Menu
Icons.Default.Refresh      // Atualizar
Icons.Default.Add          // Adicionar
Icons.Default.Check        // Salvar/Confirmar
Icons.Default.Edit         // Editar
Icons.Default.Delete       // Excluir
Icons.Default.List         // Lista
Icons.Default.Settings     // Configurações
Icons.Default.Star         // Favorito
Icons.Default.Summarize    // Resumo
Icons.Default.Description  // Documento
Icons.Default.NoteAdd      // Adicionar nota
Icons.Default.Visibility   // Visualizar
```

#### **❌ ÍCONES QUE NÃO EXISTEM (Evitar):**

```kotlin
// ❌ ÍCONES INEXISTENTES - CAUSAM BUILD FAILURE:
Icons.Default.Save         // Não existe
Icons.Default.Camera       // Não existe
Icons.Default.PhotoCamera  // Não existe
Icons.Default.Photo        // Não existe
Icons.Default.Image        // Não existe
Icons.Default.CameraAlt    // Não existe
Icons.Default.CameraEnhance // Não existe
Icons.Default.History     // Não existe
Icons.Default.FilterList   // Não existe
```

#### **🔧 ESTRATÉGIA PARA ÍCONES:**

1. **SEMPRE** usar ícones que já funcionam no projeto (evitar ícones inexistentes que quebram o build)
2. **NUNCA** tentar ícones duvidosos
3. **CONSULTAR** código existente antes de escolher ícones
4. **TESTAR** imediatamente após mudança
5. **EVITAR** loops de tentativas com ícones inexistentes

### **Compose Migration - Padrões Estabelecidos**

#### **✅ ESTRUTURA PADRÃO PARA TELAS COMPOSE:**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinhaTelaScreen(
    // Parâmetros de navegação
    onNavigateBack: () -> Unit,
    onNavigateToNext: () -> Unit,
    viewModel: MinhaTelaViewModel
) {
    // Estados do ViewModel (usar remember para mock data)
    val dados by remember { mutableStateOf(listOf("Item 1", "Item 2")) }
    val isLoading by remember { mutableStateOf(false) }
    val error by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Título da Tela") },
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
                .padding(16.dp)
        ) {
            item {
                GestaoBilharesCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Título do Card",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    // Conteúdo do card
                }
            }
        }
    }
}
```

#### **✅ IMPORTS PADRÃO PARA COMPOSE:**

```kotlin
// Imports essenciais para Compose
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gestaobilhares.ui.compose.components.ButtonVariant
import com.example.gestaobilhares.ui.compose.components.GestaoBilharesButton
import com.example.gestaobilhares.ui.compose.components.GestaoBilharesTextField
import com.example.gestaobilhares.ui.compose.components.GestaoBilharesCard
import com.example.gestaobilhares.ui.compose.components.GestaoBilharesLoadingIndicator
```

#### **✅ COMPONENTES COMPOSE IMPLEMENTADOS:**

```kotlin
// ✅ COMPONENTES REUTILIZÁVEIS CRIADOS:
@Composable
fun GestaoBilharesButton(
    text: String,
    onClick: () -> Unit,
    variant: ButtonVariant = ButtonVariant.Primary,
    modifier: Modifier = Modifier
)

@Composable
fun GestaoBilharesTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
)

@Composable
fun GestaoBilharesCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)

@Composable
fun GestaoBilharesLoadingIndicator()

// ✅ ENUM PARA VARIANTS DE BOTÃO:
enum class ButtonVariant {
    Primary, Secondary, Success, Danger, Error
}
```

#### **✅ REGRAS CRÍTICAS PARA COMPOSE:**

1. **NUNCA** usar parâmetro `title` em `GestaoBilharesCard` - usar `Text` interno
2. **SEMPRE** usar `remember { mutableStateOf(...) }` para mock data
3. **SEMPRE** usar ícones confirmados que funcionam
4. **SEMPRE** incluir `@OptIn(ExperimentalMaterial3Api::class)`
5. **SEMPRE** usar `Scaffold` com `TopAppBar` para navegação
6. **SEMPRE** usar `LazyColumn` para listas
7. **SEMPRE** usar `paddingValues` do Scaffold

## 🚀 PADRÕES MODERNOS DE DESENVOLVIMENTO (2025)

### **StateFlow Migration**

```kotlin
// ❌ PADRÃO ANTIGO: LiveData
private val _data = MutableLiveData<String>()
val data: LiveData<String> = _data

// ✅ PADRÃO MODERNO: StateFlow
private val _data = MutableStateFlow<String>("")
val data: StateFlow<String> = _data.asStateFlow()
```

### **Observação Moderna**

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

### **BaseViewModel Usage**

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

### **Imports Necessários**

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
```

## 🎯 EXEMPLOS DE CENTRALIZAÇÃO E SIMPLIFICAÇÃO

### **✅ CORRETO: Centralização**

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

### **❌ INCORRETO: Fragmentação Desnecessária**

```kotlin
// ❌ MÚLTIPLOS REPOSITORIES FRAGMENTADOS
class ClientRepository @Inject constructor(...)
class RouteRepository @Inject constructor(...)
class MesaRepository @Inject constructor(...)
class SettlementRepository @Inject constructor(...)
// ... mais 10 repositories
```

### **✅ CORRETO: BaseViewModel Centralizada**

```kotlin
// ✅ FUNCIONALIDADES COMUNS CENTRALIZADAS
abstract class BaseViewModel : ViewModel() {
    protected fun showLoading()
    protected fun hideLoading()
    protected fun showError(message: String)
    protected fun showMessage(message: String)
    // Todas as funcionalidades comuns em um local
}
```

### **✅ CORRETO: ViewModel Initialization**

```kotlin
// ✅ PADRÃO CORRIGIDO: Inicialização Manual
class MyFragment : Fragment() {
    private lateinit var viewModel: MyViewModel
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ SEMPRE inicializar manualmente
        val database = AppDatabase.getDatabase(requireContext())
        val repository = Repository(database.dao())
        viewModel = MyViewModel(repository)
        
        setupUI()
        observeViewModel()
    }
}
```

### **❌ INCORRETO: by viewModels() sem inicialização**

```kotlin
// ❌ CAUSA CRASH: by viewModels() sem inicialização
class MyFragment : Fragment() {
    private val viewModel: MyViewModel by viewModels() // ❌ CRASH!
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ViewModel não inicializado = CRASH
    }
}
```

### **❌ INCORRETO: Duplicação de Código**

```kotlin
// ❌ DUPLICAÇÃO EM CADA VIEWMODEL
class AuthViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    // ... duplicação
}

class RoutesViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    // ... mesma duplicação
}
```

### **🎯 PRINCÍPIOS APLICADOS:**

1. **UM ARQUIVO, UMA RESPONSABILIDADE**: AppRepository para dados, BaseViewModel para estados
2. **ELIMINAR DUPLICAÇÃO**: Funcionalidades comuns centralizadas
3. **FACILITAR MANUTENÇÃO**: Código organizado e acessível
4. **CENTRALIZAR**: Funcionalidades relacionadas em um local
5. **SIMPLIFICAR**: Evitar fragmentação desnecessária
