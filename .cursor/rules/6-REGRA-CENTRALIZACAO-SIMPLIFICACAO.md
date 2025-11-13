# 6. REGRA PRINCIPAL: ARQUITETURA HÍBRIDA MODULAR (2025)

## 🎯 REGRA FUNDAMENTAL DO PROJETO (ATUALIZADA JANEIRO 2025)

**ARQUITETURA HÍBRIDA MODULAR: AppRepository como Facade centralizado + Repositories especializados por domínio.**

**Contexto:**
- AppRepository atual: ~1.430 linhas, 264 métodos, 17+ DAOs
- 4 agents trabalhando simultaneamente
- Necessidade de evitar conflitos de merge e permitir trabalho paralelo

**Decisão:**
- **AppRepository** mantém-se como **Facade/Coordinator** (compatibilidade preservada)
- **Repositories especializados** por domínio (ClientRepository, AcertoRepository, etc.)
- **AppRepository delega** para repositories especializados
- **ViewModels** continuam usando AppRepository (sem breaking changes)

## 📋 PRINCÍPIOS APLICADOS (ATUALIZADOS 2025)

### **1. FACADE CENTRALIZADO**

- **APPREPOSITORY COMO FACADE**: Ponto único de acesso para ViewModels (compatibilidade preservada)
- **DELEGAÇÃO**: AppRepository delega para repositories especializados
- **UMA BASE**: BaseViewModel para funcionalidades comuns
- **UM PADRÃO**: StateFlow + collect em toda aplicação

### **2. MODULARIDADE POR DOMÍNIO**

- **REPOSITORIES ESPECIALIZADOS**: Um repository por domínio de negócio
- **SEPARAÇÃO DE RESPONSABILIDADES**: Cada repository gerencia seu domínio
- **TRABALHO PARALELO**: Agents podem trabalhar em domínios diferentes
- **FACILIDADE DE MANUTENÇÃO**: Código organizado e fácil de localizar

### **3. SIMPLIFICAÇÃO**

- **MENOS DUPLICAÇÃO**: Reutilizar código existente
- **MENOS COMPLEXIDADE**: Código claro e direto
- **MAIS MANUTENIBILIDADE**: Fácil de entender e modificar
- **COMPATIBILIDADE**: ViewModels não precisam mudar

## ✅ EXEMPLOS DE APLICAÇÃO (ATUALIZADOS 2025)

### **Arquitetura Híbrida Modular**

```kotlin
// ✅ CORRETO: REPOSITORY ESPECIALIZADO POR DOMÍNIO
class ClientRepository(
    private val clienteDao: ClienteDao
) {
    fun obterTodosClientes(): Flow<List<Cliente>> = clienteDao.obterTodos()
    suspend fun obterClientePorId(id: Long) = clienteDao.obterPorId(id)
    suspend fun inserirCliente(cliente: Cliente): Long = clienteDao.inserir(cliente)
    // ... métodos específicos do domínio Cliente
}

// ✅ CORRETO: APPREPOSITORY COMO FACADE (delega para especializados)
class AppRepository(
    private val clientRepository: ClientRepository,
    private val acertoRepository: AcertoRepository,
    // ... outros repositories especializados
) {
    // Delegação para repositories especializados
    fun obterTodosClientes(): Flow<List<Cliente>> = clientRepository.obterTodosClientes()
    suspend fun obterClientePorId(id: Long) = clientRepository.obterClientePorId(id)
    
    // Cache centralizado para performance
    private val _clientesCache = MutableStateFlow<List<Cliente>>(emptyList())
    val clientesCache: StateFlow<List<Cliente>> = _clientesCache.asStateFlow()
}
```

### **BaseViewModel Centralizada**

```kotlin
// ✅ CORRETO: FUNCIONALIDADES CENTRALIZADAS
abstract class BaseViewModel : ViewModel() {
    protected fun showLoading()
    protected fun hideLoading()
    protected fun showError(message: String)
    protected fun showMessage(message: String)
    // Todas as funcionalidades comuns em um local
}
```

### **StateFlow Unificado**

```kotlin
// ✅ CORRETO: PADRÃO CONSISTENTE
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.property.collect { value ->
            // Atualizar UI
        }
    }
}
```

## ❌ EVITAR: FRAGMENTAÇÃO SEM COORDENAÇÃO

### **Repositories Sem Facade**

```kotlin
// ❌ INCORRETO: Repositories sem coordenação centralizada
// ViewModels precisam conhecer múltiplos repositories
class MyViewModel(
    private val clientRepository: ClientRepository,
    private val acertoRepository: AcertoRepository,
    private val mesaRepository: MesaRepository,
    // ... muitos parâmetros
)

// ✅ CORRETO: AppRepository como Facade (ViewModels usam apenas AppRepository)
class MyViewModel(
    private val appRepository: AppRepository // Um único ponto de acesso
)
```

### **Duplicação de Código**

```kotlin
// ❌ INCORRETO: DUPLICAÇÃO EM CADA VIEWMODEL
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

## 🏗️ ESTRUTURA MODULAR HÍBRIDA (2025)

```
📁 data/
  └── repository/
      ├── AppRepository.kt (✅ FACADE - delega para especializados)
      └── domain/
          ├── ClientRepository.kt (✅ Domínio: Clientes)
          ├── AcertoRepository.kt (✅ Domínio: Acertos)
          ├── MesaRepository.kt (✅ Domínio: Mesas)
          ├── RotaRepository.kt (✅ Domínio: Rotas)
          ├── DespesaRepository.kt (✅ Domínio: Despesas)
          ├── ColaboradorRepository.kt (✅ Domínio: Colaboradores)
          ├── ContratoRepository.kt (✅ Domínio: Contratos)
          └── CicloRepository.kt (✅ Domínio: Ciclos)

📁 ui/
  └── common/
      └── BaseViewModel.kt (✅ FUNCIONALIDADES CENTRALIZADAS)

📁 ui/
  └── [module]/
      ├── [Module]ViewModel.kt (✅ USA AppRepository - sem mudanças)
      └── [Module]Fragment.kt (✅ USA STATEFLOW + COLLECT)
```

## 🎯 BENEFÍCIOS DA ARQUITETURA HÍBRIDA MODULAR

### **Trabalho Paralelo (4 Agents)**

- **Domínios separados**: Agents podem trabalhar em repositories diferentes sem conflitos
- **Merge facilitado**: Mudanças em domínios diferentes não conflitam
- **Produtividade**: Trabalho simultâneo sem bloqueios

### **Manutenibilidade**

- **Código organizado por domínio**: Fácil de encontrar e modificar
- **Padrões unificados**: AppRepository garante consistência
- **Debugging simplificado**: Logs centralizados no AppRepository

### **Performance**

- **Cache centralizado**: AppRepository mantém cache unificado
- **StateFlow eficiente**: Melhor que LiveData
- **Otimização por domínio**: Repositories especializados podem otimizar seus domínios

### **Desenvolvimento**

- **Modularidade**: Código organizado por responsabilidade
- **Reutilização**: AppRepository centraliza funcionalidades comuns
- **Onboarding**: Estrutura clara facilita entendimento
- **Compatibilidade**: ViewModels não precisam mudar

## 📊 MÉTRICAS DE SUCESSO (ATUALIZADAS 2025)

### **Antes da Refatoração Modular**

- ❌ AppRepository com 5.000+ linhas (monolítico)
- ❌ Dificuldade de trabalho paralelo (conflitos de merge)
- ❌ Código difícil de localizar e manter
- ❌ Testes complexos

### **Depois da Arquitetura Híbrida Modular**

- ✅ AppRepository como Facade (~200-300 linhas)
- ✅ 8 repositories especializados (~200-300 linhas cada)
- ✅ Trabalho paralelo sem conflitos (4 agents)
- ✅ Código organizado por domínio
- ✅ Testes mais simples e focados
- ✅ ViewModels sem breaking changes

## 🚀 APLICAÇÃO PRÁTICA (ATUALIZADA 2025)

### **Ao Criar Novas Funcionalidades**

1. **Identificar Domínio**: Determinar qual repository especializado usar
2. **Adicionar ao Repository Especializado**: Implementar método no repository do domínio
3. **Expor via AppRepository**: Adicionar delegação no AppRepository (se necessário)
4. **Usar BaseViewModel**: Herdar funcionalidades comuns
5. **Seguir Padrão StateFlow**: Consistência com resto da app

### **Ao Refatorar Código Existente**

1. **Identificar Domínio**: Determinar qual domínio a funcionalidade pertence
2. **Mover para Repository Especializado**: Extrair código do AppRepository para repository do domínio
3. **Atualizar AppRepository**: Adicionar delegação no AppRepository
4. **Manter Compatibilidade**: ViewModels continuam usando AppRepository
5. **Testar**: Garantir que funcionalidades existentes continuam funcionando
6. **Documentar**: Atualizar documentação conforme mudanças

## 🎯 CONCLUSÃO (ATUALIZADA 2025)

A **ARQUITETURA HÍBRIDA MODULAR** é o princípio fundamental do projeto, garantindo:

- **Trabalho paralelo eficiente** (4 agents sem conflitos)
- **Código organizado por domínio** (fácil de localizar e manter)
- **Compatibilidade preservada** (ViewModels não precisam mudar)
- **Performance otimizada** (cache centralizado no AppRepository)
- **Escalabilidade** (fácil adicionar novos domínios)
- **Testabilidade** (repositories especializados são mais fáceis de testar)

**Esta arquitetura deve ser aplicada em TODAS as decisões de desenvolvimento, permitindo trabalho paralelo harmonioso entre múltiplos agents.**
