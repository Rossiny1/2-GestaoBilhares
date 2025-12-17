# 1️⃣ GUIA RÁPIDO - Gestão Bilhares

> **Propósito**: Onboarding rápido + Referência diária para desenvolvimento  
> **Audiência**: Desenvolvedores novos ou retornando ao projeto  
> **Última Atualização**: Dezembro 2025

---

## ⚡ Setup em 5 Minutos

### Requisitos
- **Android Studio**: Hedgehog (2023.1.1) ou superior
- **JDK**: 17+
- **Kotlin**: 1.9+
- **Gradle**: 8.1+

### Quick Start
```bash
# 1. Clone
git clone [repository-url]
cd 2-GestaoBilhares

# 2. Build
./gradlew assembleDebug

# 3. Install
./gradlew installDebug

# 4. Verificar
adb logcat -s LOG_CRASH:* | Select-String "GestaoBilhares"
```

**✅ Sucesso**: App instalado e funcional offline-first

---

## 🎯 Comandos Essenciais

### Build & Install
```bash
# Debug build
./gradlew assembleDebug

# Release build (requer keystore)
./gradlew assembleRelease

# Install no dispositivo
./gradlew installDebug

# Uninstall
adb uninstall com.example.gestaobilhares
```

### Testes
```bash
# Todos os testes
./gradlew test

# Com cobertura
./gradlew testDebugUnitTestCoverage

# Ver relatório: build/reports/coverage/debug/index.html

# Testes específicos
./gradlew :ui:testDebugUnitTest
```

### Logs
```bash
# Logs gerais
adb logcat -s LOG_CRASH:*

# Sync logs
adb logcat -s SyncRepository:* SyncWorker:*

# Limpar logs
adb logcat -c

# Salvar logs
adb logcat > logcat-$(Get-Date -Format "yyyyMMdd-HHmmss").txt
```

### Limpeza
```bash
# Clean build
./gradlew clean

# Limpar cache Gradle
./gradlew cleanBuildCache

# Invalidar cache Android Studio
# File → Invalidate Caches / Restart
```

---

## 📁 Estrutura do Projeto

```
2-GestaoBilhares/
├── :app                    # MainActivity, Application, DI setup
├── :core                   # Utils, Extensions, Constants
├── :data                   # Entities, DAOs, Repositories
├── :ui                     # Fragments, ViewModels, Compose Screens
├── :sync                   # SyncRepository, Workers, Queue
├── build.gradle.kts        # Root build config
└── settings.gradle.kts     # Módulos
```

### Módulos Gradle

| Módulo | Responsabilidade | Dependências |
|--------|------------------|--------------|
| `:app` | Entry point, DI | core, data, ui, sync |
| `:core` | Utils, extensions | - |
| `:data` | Room, Repositories | core |
| `:ui` | ViewModels, Screens | core, data |
| `:sync` | Sincronização | core, data |

---

## 🔄 Workflow de Desenvolvimento

### 1. Criar Nova Feature
```
Feature branch → Implementar → Testar → Commit → PR
```

**Passos**:
1. Identificar domínio (Cliente, Acerto, Mesa, etc.)
2. Adicionar método no Repository especializado
3. Expor via `AppRepository` (se necessário)
4. Criar/atualizar ViewModel
5. Criar/atualizar UI (Compose preferencial)
6. **Testar offline** ✅
7. Commit (seguir checklist abaixo)

### 2. Checklist Antes de Commit

#### Build & Testes
- [ ] `./gradlew assembleDebug` passa
- [ ] `./gradlew test` passa
- [ ] Sem warnings críticos no Lint
- [ ] App funciona offline

#### Código
- [ ] Segue padrões (ver [BEST-PRACTICES.md](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/5-BEST-PRACTICES.md))
- [ ] StateFlow (não LiveData)
- [ ] `repeatOnLifecycle` (não `observe`)
- [ ] Hilt para DI (não manual)
- [ ] KDoc em classes públicas

#### Git
- [ ] Mensagem segue Conventional Commits
- [ ] Commits atômicos (uma mudança lógica)
- [ ] Sem arquivos sensíveis (tokens, keys)

**Exemplo de commit**:
```bash
git commit -m "feat(clients): adicionar filtro por débito alto"
```

### 3. Padrão de Implementação

```kotlin
// 1. Repository (data/)
class ClienteRepository @Inject constructor(
    private val clienteDao: ClienteDao
) {
    fun obterPorRota(rotaId: Long): Flow<List<Cliente>> = 
        clienteDao.obterPorRota(rotaId)
}

// 2. AppRepository (data/) - Facade
class AppRepository @Inject constructor(
    private val clienteRepository: ClienteRepository,
    // ... outros repositories
) {
    fun obterClientesPorRota(rotaId: Long) = 
        clienteRepository.obterPorRota(rotaId)
}

// 3. ViewModel (ui/)
@HiltViewModel
class ClientListViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {
    private val _rotaId = MutableStateFlow<Long?>(null)
    
    val clientes: StateFlow<List<Cliente>> = _rotaId
        .flatMapLatest { id ->
            id?.let { appRepository.obterClientesPorRota(it) }
                ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

// 4. UI (ui/)
@Composable
fun ClientListScreen(viewModel: ClientListViewModel = hiltViewModel()) {
    val clientes by viewModel.clientes.collectAsStateWithLifecycle()
    
    LazyColumn {
        items(clientes, key = { it.id }) { cliente ->
            ClientCard(cliente)
        }
    }
}
```

---

## 🔧 Troubleshooting

### Build Failures

#### Erro: "Unresolved reference: Hilt"
```bash
# Reconstruir projeto
./gradlew clean build
# File → Invalidate Caches / Restart
```

#### Erro: "Room schema export"
```bash
# Atualizar schema
./gradlew :data:kspDebugKotlin --rerun-tasks
```

#### Erro: "Duplicate class"
```bash
# Limpar build
./gradlew clean
rm -r .gradle
./gradlew assembleDebug
```

### Sincronização

#### Fila não processa
```bash
# Logs de sync
adb logcat -s SyncRepository:* SyncWorker:*

# Forçar sync manual (no app)
# Configurações → Sincronizar Agora
```

#### Firestore permission denied
- Verificar autenticação: `FirebaseAuth.getInstance().currentUser`
- Verificar regras: `firestore.rules`
- Logs: `adb logcat -s FirebaseFirestore:*`

### Performance

#### App lento
1. Verificar memória: Android Studio Profiler
2. Verificar queries Room: adicionar índices
3. Verificar cache: limitar tamanho (LruCache)

#### Lista lenta
- Usar `LazyColumn` (Compose) ou `RecyclerView`
- Adicionar `key` para recomposição eficiente
- Paginar grandes datasets (Paging 3)

---

## 📚 Referências Rápidas

### Documentação do Projeto
1. **[GUIA-RAPIDO.md](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/1-GUIA-RAPIDO.md)** ← Você está aqui
2. **[ARQUITETURA-REFERENCIA.md](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/2-ARQUITETURA-REFERENCIA.md)** - Referência técnica completa
3. **[REGRAS-NEGOCIO.md](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/3-REGRAS-NEGOCIO.md)** - Lógica de negócio
4. **[STATUS-ROADMAP.md](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/4-STATUS-ROADMAP.md)** - Status e planejamento
5. **[BEST-PRACTICES.md](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/5-BEST-PRACTICES.md)** - Qualidade e padrões

### Links Externos
- [Android Developer](https://developer.android.com)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- [Room](https://developer.android.com/training/data-storage/room)
- [StateFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)

---

## 🎯 Próximos Passos

**Novo no projeto?**
1. ✅ Completar setup (acima)
2. 📚 Ler [ARQUITETURA-REFERENCIA.md](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/2-ARQUITETURA-REFERENCIA.md)
3. 💼 Ler [REGRAS-NEGOCIO.md](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/3-REGRAS-NEGOCIO.md)
4. ✅ Revisar [BEST-PRACTICES.md](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/5-BEST-PRACTICES.md)
5. 🚀 Começar desenvolvimento!

**Implementando feature?**
1. Revisar [ARQUITETURA-REFERENCIA.md](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/2-ARQUITETURA-REFERENCIA.md) (padrões)
2. Verificar [REGRAS-NEGOCIO.md](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/3-REGRAS-NEGOCIO.md) (lógica)
3. Seguir workflow acima
4. Checklist antes de commit

**Planejando sprint?**
1. Ver [STATUS-ROADMAP.md](file:///c:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/4-STATUS-ROADMAP.md) (pendências)
2. Priorizar itens críticos
3. Estimar esforço
