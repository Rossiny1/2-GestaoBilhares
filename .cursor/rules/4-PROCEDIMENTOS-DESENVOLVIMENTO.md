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
- **Navegação Robusta**: OnBackPressedCallback e controle de stack
- **Centralização**: AppRepository como único ponto de acesso

### **Estratégia de Otimização de Banco (2025)**

- **Incremental**: Uma melhoria por vez, testada individualmente
- **Baixo Risco Primeiro**: Índices essenciais antes de queries complexas
- **Validação Contínua**: Build e teste após cada mudança
- **Preparação para Sync**: Estrutura pronta para sincronização online
- **Performance First**: Otimizações antes da implementação online

### **Implementação Cuidadosa de Sincronização (2025)**

- **Melhores Práticas Android 2025**: Seguindo diretrizes oficiais
- **Índices Otimizados**: Performance sem quebrar funcionalidade
- **Teste Incremental**: Cada entidade testada individualmente
- **Rollback Rápido**: Possibilidade de reverter facilmente
- **Estrutura Preparatória**: Base sólida para sincronização futura
- ✅ **Entidades Seguras**: SyncLog, SyncQueue, SyncConfig com índices estratégicos - CONCLUÍDO

### **Processamento em Background (Fase 4C - CONCLUÍDA)**

- **WorkManager 2.9.1**: Versão mais recente Android 2025
- **CoroutineWorker**: Uso de coroutines nativas
- **Constraints Inteligentes**: NetworkType.CONNECTED, BatteryNotLow
- **BackoffPolicy.EXPONENTIAL**: Retry inteligente
- **Centralização Total**: Workers integrados no AppRepository
- **Agendamento Automático**: Sincronização a cada 15min, limpeza diária às 2:00
- **Inicialização na Application**: Workers iniciados automaticamente
- ✅ **Implementação Completa**: SyncWorker e CleanupWorker funcionais - CONCLUÍDO

### **Planejamento de Implementação Online/Sync**

**CRÍTICO**: Melhorias de banco devem ser feitas ANTES da implementação online:

1. **Fase 6: Otimização de Banco (CONCLUÍDA)**
   - ✅ Índices essenciais (baixo risco) - CONCLUÍDO
   - ✅ Queries otimizadas (médio risco) - CONCLUÍDO
   - ✅ Estrutura para sync (alto risco) - CONCLUÍDO
   - ✅ DAOs e migração 42→43 - CONCLUÍDO
   - ✅ Testes incrementais - CONCLUÍDO

2. **Fase 4C: Processamento em Background (CONCLUÍDA)**
   - ✅ WorkManager 2.9.1 - CONCLUÍDO
   - ✅ CoroutineWorker - CONCLUÍDO
   - ✅ Constraints Inteligentes - CONCLUÍDO
   - ✅ BackoffPolicy.EXPONENTIAL - CONCLUÍDO
   - ✅ Workers Centralizados - CONCLUÍDO
   - ✅ Agendamento Automático - CONCLUÍDO
   - ✅ Inicialização na Application - CONCLUÍDO

3. **Fase 7: Implementação Online/Sync (FUTURO)**
   - API endpoints
   - Sincronização offline-first
   - Resolução de conflitos
   - Testes de cenários complexos

4. **Fase 4D: Otimizações Avançadas (EM ANDAMENTO)**
   - 🔄 Otimização de Memória - WeakReference, object pooling
   - 🔄 Otimização de Rede - Compressão, batch operations
   - 🔄 Otimização de UI - ViewStub, ViewHolder pattern
   - 🔄 Otimização de Banco - Connection pooling

5. **Fase 8: Otimizações Avançadas (FUTURO)**
   - Performance avançada
   - Material Design 3
   - Testes automatizados

**Benefícios desta abordagem:**

- ✅ Menos complexidade na implementação do sync
- ✅ Melhor performance durante sincronização
- ✅ Estrutura preparada para dados online
- ✅ Menos bugs e problemas futuros
- ✅ Manutenção mais fácil

### **Responsabilidades do Usuário**

- **Builds**: Usuário executa todos os builds e geração de APK
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

- **Logcat**: Usar caminho específico do ADB
- **Logs Detalhados**: Adicionar em componentes críticos
- **Análise**: Capturar logs durante testes

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
