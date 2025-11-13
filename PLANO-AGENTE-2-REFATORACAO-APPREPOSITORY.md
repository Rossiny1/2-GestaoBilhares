# 📋 PLANO DE TRABALHO - AGENTE 2: REFATORAÇÃO APPREPOSITORY E MIGRAÇÃO CICLOACERTOREPOSITORY

## 🎯 OBJETIVO
Refatorar o AppRepository para delegar para repositories especializados (em vez de usar DAOs diretamente) e migrar o CicloAcertoRepository para usar AppRepository.

## 📍 CONTEXTO
- **Status Atual**: AppRepository usa DAOs diretamente (~1.430 linhas)
- **Meta**: AppRepository como Facade (~200-300 linhas) delegando para repositories especializados
- **Trabalho Paralelo**: Outro agente está criando a estrutura `domain/` e organizando repositories especializados

## ⚠️ REGRAS CRÍTICAS PARA TRABALHO HARMONIOSO

### **NÃO MODIFICAR:**
- ❌ **NÃO modificar** repositories na pasta `domain/` (outro agente está trabalhando neles)
- ❌ **NÃO criar** novos repositories (outro agente está criando)
- ❌ **NÃO mover** repositories (outro agente está movendo)
- ❌ **NÃO modificar** fragments ou ViewModels (já migrados)

### **PODE MODIFICAR:**
- ✅ Modificar `AppRepository.kt` para delegar para repositories especializados
- ✅ Modificar `CicloAcertoRepository.kt` para usar AppRepository
- ✅ Modificar `RepositoryFactory.kt` para criar repositories especializados
- ✅ Atualizar imports nos 5 fragments que ainda usam CicloAcertoRepository

## 📋 TAREFAS DETALHADAS

### **FASE 1: Aguardar Estrutura Domain (PRIORIDADE ALTA)**

**AGUARDAR** até que o outro agente complete:
- ✅ Pasta `domain/` criada
- ✅ Repositories movidos/criados na pasta `domain/`

**Verificar antes de continuar:**
- [ ] Pasta `app/src/main/java/com/example/gestaobilhares/data/repository/domain/` existe
- [ ] Repositories especializados existem em `domain/`

### **FASE 2: Atualizar RepositoryFactory (PRIORIDADE ALTA)**

Atualizar `RepositoryFactory.kt` para criar repositories especializados:

```kotlin
object RepositoryFactory {
    fun getAppRepository(context: Context): AppRepository {
        val database = AppDatabase.getDatabase(context)
        
        // Criar repositories especializados
        val clientRepository = domain.ClienteRepository(database.clienteDao())
        val acertoRepository = domain.AcertoRepository(database.acertoDao(), database.clienteDao())
        val mesaRepository = domain.MesaRepository(database.mesaDao())
        val rotaRepository = domain.RotaRepository(database.rotaDao())
        val despesaRepository = domain.DespesaRepository(database.despesaDao())
        val cicloRepository = domain.CicloRepository(database.cicloAcertoDao())
        val colaboradorRepository = domain.ColaboradorRepository(database.colaboradorDao())
        val contratoRepository = domain.ContratoRepository(
            database.contratoLocacaoDao(),
            database.aditivoContratoDao()
        )
        val acertoMesaRepository = domain.AcertoMesaRepository(database.acertoMesaDao())
        val categoriaDespesaRepository = domain.CategoriaDespesaRepository(database.categoriaDespesaDao())
        
        // Criar AppRepository com repositories especializados
        return AppRepository.create(
            database,
            clientRepository,
            acertoRepository,
            mesaRepository,
            rotaRepository,
            despesaRepository,
            cicloRepository,
            colaboradorRepository,
            contratoRepository,
            acertoMesaRepository,
            categoriaDespesaRepository
        )
    }
}
```

### **FASE 3: Refatorar AppRepository para Delegar (PRIORIDADE ALTA)**

Modificar `AppRepository.kt`:

1. **Atualizar construtor** para receber repositories especializados:
```kotlin
class AppRepository constructor(
    private val clientRepository: domain.ClienteRepository,
    private val acertoRepository: domain.AcertoRepository,
    private val mesaRepository: domain.MesaRepository,
    private val rotaRepository: domain.RotaRepository,
    private val despesaRepository: domain.DespesaRepository,
    private val cicloRepository: domain.CicloRepository,
    private val colaboradorRepository: domain.ColaboradorRepository,
    private val contratoRepository: domain.ContratoRepository,
    private val acertoMesaRepository: domain.AcertoMesaRepository,
    private val categoriaDespesaRepository: domain.CategoriaDespesaRepository,
    // DAOs opcionais apenas para casos especiais
    private val tipoDespesaDao: TipoDespesaDao? = null,
    private val panoEstoqueDao: PanoEstoqueDao? = null,
    private val stockItemDao: StockItemDao? = null,
    private val mesaReformadaDao: MesaReformadaDao? = null,
    private val mesaVendidaDao: MesaVendidaDao? = null,
    private val historicoManutencaoMesaDao: HistoricoManutencaoMesaDao? = null
) {
    companion object {
        fun create(
            database: AppDatabase,
            clientRepository: domain.ClienteRepository,
            acertoRepository: domain.AcertoRepository,
            mesaRepository: domain.MesaRepository,
            rotaRepository: domain.RotaRepository,
            despesaRepository: domain.DespesaRepository,
            cicloRepository: domain.CicloRepository,
            colaboradorRepository: domain.ColaboradorRepository,
            contratoRepository: domain.ContratoRepository,
            acertoMesaRepository: domain.AcertoMesaRepository,
            categoriaDespesaRepository: domain.CategoriaDespesaRepository
        ): AppRepository {
            return AppRepository(
                clientRepository,
                acertoRepository,
                mesaRepository,
                rotaRepository,
                despesaRepository,
                cicloRepository,
                colaboradorRepository,
                contratoRepository,
                acertoMesaRepository,
                categoriaDespesaRepository,
                database.tipoDespesaDao(),
                database.panoEstoqueDao(),
                database.stockItemDao(),
                database.mesaReformadaDao(),
                database.mesaVendidaDao(),
                database.historicoManutencaoMesaDao()
            )
        }
    }
}
```

2. **Substituir métodos para delegar** (exemplo):
```kotlin
// ❌ ANTES: Usa DAO diretamente
fun obterTodosClientes(): Flow<List<Cliente>> = clienteDao.obterTodos()

// ✅ DEPOIS: Delega para repository especializado
fun obterTodosClientes(): Flow<List<Cliente>> = clientRepository.obterTodosClientes()
```

3. **Manter cache centralizado** no AppRepository:
```kotlin
// Cache continua no AppRepository
private val _clientesCache = MutableStateFlow<List<Cliente>>(emptyList())
val clientesCache: StateFlow<List<Cliente>> = _clientesCache.asStateFlow()
```

4. **Reduzir AppRepository** de ~1.430 linhas para ~200-300 linhas:
   - Remover implementações que agora estão nos repositories especializados
   - Manter apenas delegações e cache
   - Manter métodos de coordenação entre domínios

### **FASE 4: Migrar CicloAcertoRepository (PRIORIDADE ALTA)**

Modificar `CicloAcertoRepository.kt`:

1. **Atualizar construtor** para receber AppRepository:
```kotlin
class CicloAcertoRepository constructor(
    private val appRepository: AppRepository
) {
    // Remover dependências de DespesaRepository, AcertoRepository, ClienteRepository
}
```

2. **Substituir chamadas** para usar AppRepository:
```kotlin
// ❌ ANTES
private val despesaRepository: DespesaRepository
despesaRepository.buscarPorCicloId(cicloId)

// ✅ DEPOIS
appRepository.buscarDespesasPorCicloId(cicloId)
```

3. **Manter métodos públicos** que são usados pelos fragments
4. **Remover anotação @Deprecated** após migração completa

### **FASE 5: Atualizar Fragments que Usam CicloAcertoRepository (PRIORIDADE ALTA)**

Atualizar os 5 fragments que ainda instanciam repositories:

1. **CycleHistoryFragment.kt**
2. **SettlementDetailFragment.kt**
3. **CycleReceiptsFragment.kt**
4. **CycleManagementFragment.kt**
5. **CycleClientsFragment.kt**

**Padrão de atualização:**
```kotlin
// ❌ ANTES
val database = AppDatabase.getDatabase(requireContext())
val appRepository = RepositoryFactory.getAppRepository(requireContext())
val cicloAcertoRepository = CicloAcertoRepository(
    database.cicloAcertoDao(),
    DespesaRepository(database.despesaDao()),
    AcertoRepository(database.acertoDao(), database.clienteDao()),
    ClienteRepository(database.clienteDao(), appRepository),
    database.rotaDao()
)

// ✅ DEPOIS
val appRepository = RepositoryFactory.getAppRepository(requireContext())
val cicloAcertoRepository = CicloAcertoRepository(appRepository)
```

### **FASE 6: Remover Imports Não Utilizados (PRIORIDADE MÉDIA)**

Após migração:
1. Remover imports de `DespesaRepository`, `AcertoRepository`, `ClienteRepository` dos fragments
2. Remover imports de DAOs do AppRepository (se não forem mais usados)

### **FASE 7: Validação e Testes (PRIORIDADE ALTA)**

1. Verificar build sem erros
2. Verificar que AppRepository tem ~200-300 linhas
3. Verificar que todos os métodos ainda funcionam
4. Verificar que não há regressões

## 📝 PADRÃO DE DELEGAÇÃO NO APPREPOSITORY

```kotlin
// ==================== CLIENTE ====================
// ✅ DELEGAÇÃO: AppRepository delega para ClientRepository
fun obterTodosClientes(): Flow<List<Cliente>> = clientRepository.obterTodosClientes()
suspend fun obterClientePorId(id: Long) = clientRepository.obterClientePorId(id)
suspend fun inserirCliente(cliente: Cliente): Long = clientRepository.inserirCliente(cliente)

// Cache centralizado continua no AppRepository
private val _clientesCache = MutableStateFlow<List<Cliente>>(emptyList())
val clientesCache: StateFlow<List<Cliente>> = _clientesCache.asStateFlow()
```

## ✅ CHECKLIST DE VALIDAÇÃO

Antes de considerar completo, verificar:

- [ ] RepositoryFactory atualizado para criar repositories especializados
- [ ] AppRepository construtor atualizado para receber repositories especializados
- [ ] AppRepository métodos delegando para repositories especializados
- [ ] AppRepository reduzido para ~200-300 linhas
- [ ] CicloAcertoRepository migrado para usar AppRepository
- [ ] CicloAcertoRepository sem dependências de outros repositories
- [ ] 5 fragments atualizados para usar CicloAcertoRepository simplificado
- [ ] Imports não utilizados removidos
- [ ] Build passa sem erros
- [ ] Nenhuma regressão funcional

## 🚨 IMPORTANTE

- **AGUARDAR** outro agente completar estrutura `domain/` antes de começar
- **NÃO modificar** repositories na pasta `domain/` (outro agente criou)
- **Focar** em refatorar AppRepository e migrar CicloAcertoRepository
- **Testar** após cada fase para garantir que não quebrou nada

## 🎯 RESULTADO ESPERADO

Após completar este plano:

- ✅ AppRepository como Facade (~200-300 linhas) delegando para especializados
- ✅ CicloAcertoRepository usando AppRepository (sem dependências diretas)
- ✅ Fragments simplificados (sem instanciar múltiplos repositories)
- ✅ Arquitetura híbrida modular completa e funcional

## 📌 ORDEM DE EXECUÇÃO

1. ⏳ **AGUARDAR** outro agente completar estrutura `domain/`
2. Atualizar RepositoryFactory
3. Refatorar AppRepository para delegar
4. Migrar CicloAcertoRepository
5. Atualizar 5 fragments
6. Remover imports não utilizados
7. Validar build e funcionalidades

---

**Status**: Aguardando comando para iniciar
**Prioridade**: ALTA
**Tempo estimado**: 3-4 horas
**Risco de conflito**: BAIXO (trabalha em AppRepository e CicloAcertoRepository, não modifica domain/)

