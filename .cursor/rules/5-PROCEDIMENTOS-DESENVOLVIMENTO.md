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

## ⚠️ AVISOS IMPORTANTES

1. **Nunca quebrar compatibilidade**: ViewModels devem continuar usando AppRepository
2. **Offline-first**: Sempre testar offline
3. **Modularização**: Trabalhar em domínios diferentes para evitar conflitos
4. **Commits frequentes**: Facilitar rollback se necessário
5. **Testes antes de commit**: Garantir que build passa

