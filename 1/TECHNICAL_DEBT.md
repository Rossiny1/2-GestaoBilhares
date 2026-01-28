# 🔧 TECHNICAL_DEBT - DÍVIDA TÉCNICA E MELHORIAS

> **Inventário completo de débitos técnicos do projeto**  
> **Versão:** 1.1  
> **Gerado em:** 27/01/2026  
> **Atualizado:** 28/01/2026  
> **Baseline:** Após correção de Security Rules e valores decimais

---

## 📊 RESUMO EXECUTIVO

### Status Geral
**Nota Atual:** 7.0/10  
**Nota Potencial (com correções):** 9.0/10

### Distribuição de Dívidas
- 🔴 **CRÍTICAS:** 5 itens (ação imediata)
- 🟠 **ALTAS:** 7 itens (1-2 semanas)
- 🟡 **MÉDIAS:** 9 itens (1 mês)
- 🟢 **BAIXAS:** 6 itens (backlog)

**Total:** 27 débitos identificados

---

## 🔴 PRIORIDADE CRÍTICA (Ação Imediata)

### 1. ❌ TESTE DE CONVERSÃO DECIMAL AUSENTE

**Problema:**
- Bug de valores decimais (x10) corrigido em 27/01
- Exemplo: `valor_mesa = 1.50` aparecia como `15.00`
- **SEM TESTE = Bug pode voltar silenciosamente**

**Impacto:**
- 🔴 Valores errados = cálculos financeiros incorretos
- 🔴 Afeta dinheiro real de clientes
- 🔴 Regressão passa despercebida

**Solução:**
```kotlin
// ValorDecimalConverterTest.kt
@Test
fun `valor_mesa deve ser armazenado como Double em reais`() {
    // Importador: "1,50" → 1.5
    val valorImportado = converterValor("1,50")
    assertThat(valorImportado).isEqualTo(1.5)

    // Firestore: 1.5 (não 150)
    val cliente = Cliente(valor_mesa = 1.5)
    assertThat(cliente.valor_mesa).isEqualTo(1.5)

    // Tela: "R$ 1,50" (não "R$ 15,00")
    val valorFormatado = cliente.valor_mesa.formatarMoeda()
    assertThat(valorFormatado).isEqualTo("R$ 1,50")
}

@Test
fun `conversao de string com virgula deve gerar Double correto`() {
    val casos = mapOf(
        "1,50" to 1.5,
        "10,00" to 10.0,
        "100,50" to 100.5,
        "0,50" to 0.5
    )

    casos.forEach { (input, expected) ->
        val resultado = converterValor(input)
        assertThat(resultado).isEqualTo(expected)
    }
}
```

**Prazo:** ⏰ 28/01 até 12h  
**Esforço:** 1 hora  
**Arquivo:** `app/src/test/java/.../ValorDecimalConverterTest.kt`  
**Responsável:** Dev principal

---

### 2. ❌ TESTES DE VIEWMODELS CRÍTICOS AUSENTES

**Problema:**
- 7 testes totais no projeto
- Faltam testes de ViewModels críticos:
  - `AcertoViewModel` (cálculos financeiros)
  - `SettlementViewModel` (fechamento)
  - `ClienteViewModel` (validações)

**Impacto:**
- 🔴 Mudanças não validadas automaticamente
- 🔴 Regressões chegam em produção
- 🔴 Cálculos financeiros sem validação

**Solução:**
```kotlin
// AcertoViewModelTest.kt
@Test
fun `deve calcular total de acerto corretamente`() {
    val acerto = Acerto(
        valor_mesa = 150.0,
        comissao = 0.60,
        quantidade_fichas = 100
    )
    val total = viewModel.calcularTotalAcerto(acerto)
    assertThat(total).isEqualTo(210.0) // valor_mesa + comissão
}

@Test
fun `deve validar acerto antes de salvar`() {
    val acertoInvalido = Acerto(valor_mesa = -10.0)
    val resultado = viewModel.salvarAcerto(acertoInvalido)
    assertThat(resultado.isFailure).isTrue()
}

// SettlementViewModelTest.kt
@Test
fun `troca de pano deve atualizar estoque`() = runTest {
    val panoId = "pano001"
    val mesaId = "mesa001"

    viewModel.registrarTrocaPano(mesaId, panoId)

    val estoque = repository.getEstoquePano(panoId).first()
    assertThat(estoque.quantidade).isEqualTo(quantidadeAnterior - 1)
}

// ClienteViewModelTest.kt
@Test
fun `deve validar campos obrigatorios do cliente`() {
    val clienteInvalido = Cliente(nome = "", rota_id = "")
    val resultado = viewModel.salvarCliente(clienteInvalido)
    assertThat(resultado.isFailure).isTrue()
    assertThat(resultado.exceptionOrNull()?.message)
        .contains("obrigatório")
}
```

**Prazo:** ⏰ Até 31/01  
**Esforço:** 1 dia (8h)  
**Arquivos:**
- `AcertoViewModelTest.kt`
- `SettlementViewModelTest.kt`
- `ClienteViewModelTest.kt`

**Responsável:** Dev principal

---

### 3. ❌ BACKUP AUTOMÁTICO AUSENTE

**Problema:**
- Zero backup automático do Firestore
- Dados em produção sem proteção
- Recovery manual (demorado e propenso a erros)

**Impacto:**
- 🔴 Perda de dados permanente em caso de:
  - Corrupção de dados
  - Exclusão acidental
  - Bug crítico que afeta DB
  - Problema no Firebase

**Solução:**
```javascript
// functions/backup-firestore.js
const functions = require('firebase-functions');
const firestore = require('@google-cloud/firestore');

exports.backupFirestore = functions.pubsub
  .schedule('0 3 * * *') // 3h AM diariamente
  .timeZone('America/Sao_Paulo')
  .onRun(async (context) => {
    const client = new firestore.v1.FirestoreAdminClient();
    const projectId = 'gestaobilhares';
    const databaseName = client.databasePath(projectId, '(default)');

    const timestamp = new Date().toISOString().split('T')[0];
    const bucket = `gs://gestaobilhares-backups/backup-${timestamp}`;

    await client.exportDocuments({
      name: databaseName,
      outputUriPrefix: bucket,
      collectionIds: [] // Todas collections
    });

    console.log(`Backup criado: ${bucket}`);
  });

// Rotação: Manter últimos 30 dias
exports.cleanOldBackups = functions.pubsub
  .schedule('0 4 * * *')
  .onRun(async (context) => {
    const storage = require('@google-cloud/storage')();
    const bucket = storage.bucket('gestaobilhares-backups');

    const [files] = await bucket.getFiles();
    const thirtyDaysAgo = Date.now() - (30 * 24 * 60 * 60 * 1000);

    for (const file of files) {
      const [metadata] = await file.getMetadata();
      const created = new Date(metadata.timeCreated).getTime();

      if (created < thirtyDaysAgo) {
        await file.delete();
        console.log(`Backup antigo deletado: ${file.name}`);
      }
    }
  });
```

**Prazo:** ⏰ Até 31/01  
**Esforço:** 2-4 horas  
**Configuração necessária:**
- Cloud Functions habilitadas
- Bucket do Cloud Storage criado
- Permissões configuradas
- Billing ativado (Blaze plan)

**Responsável:** Dev principal + DevOps

---

### 4. ⚠️ MONITORAMENTO SEMANAL NÃO EXECUTADO

**Problema:**
- Checklist criado mas **NUNCA executado**
- Sem baseline de métricas
- Problemas ocultos não detectados

**Impacto:**
- 🔴 Violations não monitoradas
- 🔴 Sincronização pode falhar silenciosamente
- 🔴 Performance não rastreada

**Solução:**

1. **Primeira execução:** Segunda 03/02/2026, 9h

2. **Executar checklist completo (65min):**
   - Violations (15min)
   - Estrutura Firestore (10min)
   - Performance (10min)
   - Multi-tenancy (10min)
   - Sync E2E (15min)
   - Logs (5min)

3. **Documentar baseline:**
   - Número de documentos por collection
   - Tempo médio de sync
   - Taxa de sucesso/erro
   - Violations típicas

4. **Gerar relatório:**
```markdown
# Monitoramento Semanal - 03/02/2026

## Status Geral: 🟢 Saudável

## Métricas Baseline
- Violations: 3 (edge cases normais)
- Documents: 1.245 total
- Sync time: 1.2s média
- Reads: 5.432 (semana)
- Writes: 1.234 (semana)

## Observações
- Multi-tenancy funcionando corretamente
- Sem PERMISSION_DENIED
- Performance adequada
```

Salvar em: `monitoramento/relatorios/2026-02-03.md`

**Prazo:** ⏰ 03/02, 9h  
**Esforço:** 65 minutos  
**Frequência:** Semanal (toda segunda)  
**Responsável:** Dev principal

---

### 5. 🔒 SECURITY RULES SEM TESTES AUTOMATIZADOS

**Problema:**
- Rules corrigidas em 27/01
- **Sem testes automatizados**
- Testes manuais via Service Account (que IGNORA regras)
- Mudanças futuras podem quebrar sem detecção

**Impacto:**
- 🔴 Regras podem quebrar em mudanças futuras
- 🔴 Multi-tenancy pode ser comprometido
- 🔴 Violations não detectadas antes de deploy

**Solução:**
```javascript
// tests/firestore.rules.test.js
const { initializeTestEnvironment } = require('@firebase/rules-unit-testing');
const fs = require('fs');

describe('Security Rules - Clientes', () => {
  let testEnv;

  beforeAll(async () => {
    testEnv = await initializeTestEnvironment({
      projectId: 'gestaobilhares-test',
      firestore: {
        rules: fs.readFileSync('firestore.rules', 'utf8')
      }
    });
  });

  afterAll(async () => {
    await testEnv.cleanup();
  });

  test('Usuário pode ler clientes da própria rota', async () => {
    const alice = testEnv.authenticatedContext('alice', {
      rotasPermitidas: ['037-Salinas']
    });

    const clienteRef = alice.firestore()
      .doc('empresas/empresa001/entidades/clientes/items/cliente001');

    await assertSucceeds(clienteRef.get());
  });

  test('Usuário NÃO pode ler clientes de outra rota', async () => {
    const bob = testEnv.authenticatedContext('bob', {
      rotasPermitidas: ['034-Bonito']
    });

    const clienteRef = bob.firestore()
      .doc('empresas/empresa001/entidades/clientes/items/cliente001'); // rota 037

    await assertFails(clienteRef.get());
  });

  test('Usuário NÃO pode criar cliente sem empresa_id', async () => {
    const alice = testEnv.authenticatedContext('alice', {
      rotasPermitidas: ['037-Salinas']
    });

    const clienteRef = alice.firestore()
      .doc('empresas/empresa001/entidades/clientes/items/novo001');

    await assertFails(clienteRef.set({
      nome: 'Cliente Teste',
      rota_id: '037-Salinas'
      // Falta empresa_id
    }));
  });
});

// package.json
{
  "scripts": {
    "test:rules": "jest tests/firestore.rules.test.js"
  },
  "devDependencies": {
    "@firebase/rules-unit-testing": "^2.0.0",
    "jest": "^29.0.0"
  }
}
```

**Prazo:** ⏰ Até 31/01  
**Esforço:** 4 horas  
**Dependências:**
- `@firebase/rules-unit-testing`
- Java 21+ (para emulador)
- Jest

**Responsável:** Dev principal

---

## 🟠 PRIORIDADE ALTA (1-2 Semanas)

### 6. ⚠️ COBERTURA DE TESTES INSUFICIENTE

**Problema:**
- 7 testes no projeto inteiro
- Cobertura estimada: ~5%
- ViewModels, Repositories, Use Cases sem testes

**Impacto:**
- 🟠 Regressões não detectadas
- 🟠 Refatorações arriscadas
- 🟠 Baixa confiança em mudanças

**Meta:**
- **2 semanas:** 30% cobertura
- **1 mês:** 60% cobertura
- **3 meses:** 80% cobertura

**Prioridade de testes:**
1. ✅ Conversão de valores (decimal) - CRÍTICO
2. ViewModels críticos (Acerto, Settlement, Cliente)
3. Repositories (AcertoRepository, ClienteRepository)
4. Use Cases (RegistrarTrocaPanoUseCase)
5. Cálculos financeiros (FinancialCalculator)
6. Mappers (Entity ↔ Domain)
7. Validações de negócio

**Prazo:** 14/02  
**Esforço:** 3 dias (24h)  
**Responsável:** Dev principal + QA

---

### 7. ⚠️ TESTES DE REPOSITORIES AUSENTES

**Problema:**
- 22 Repositories no projeto
- Zero testes de repositories críticos
- Sincronização Firestore sem validação

**Impacto:**
- 🟠 Queries podem estar incorretas
- 🟠 Sincronização falha silenciosamente
- 🟠 Conflitos não testados

**Solução:**
```kotlin
// AcertoRepositoryTest.kt
@Test
fun `deve criar acerto no Firestore e local DB`() = runTest {
    val acerto = Acerto(
        id = "acerto001",
        clienteId = "cliente001",
        valor_mesa = 150.0,
        data = System.currentTimeMillis()
    )

    val resultado = repository.criarAcerto(acerto)

    // Verificar local DB
    val localAcerto = dao.buscarPorId(acerto.id)
    assertThat(localAcerto).isNotNull()

    // Verificar Firestore (mock ou emulador)
    val firestoreDoc = firestore
        .document("empresas/empresa001/entidades/acertos/items/${acerto.id}")
        .get().await()
    assertThat(firestoreDoc.exists()).isTrue()
}

@Test
fun `deve resolver conflito por timestamp mais recente`() = runTest {
    val acertoLocal = Acerto(
        id = "acerto001",
        valor_mesa = 100.0,
        dataUltimaAtualizacao = 1000L
    )

    val acertoFirestore = Acerto(
        id = "acerto001",
        valor_mesa = 150.0,
        dataUltimaAtualizacao = 2000L // Mais recente
    )

    val resultado = repository.resolverConflito(acertoLocal, acertoFirestore)

    // Deve manter o mais recente
    assertThat(resultado.valor_mesa).isEqualTo(150.0)
}
```

**Prazo:** 14/02  
**Esforço:** 2 dias (16h)  
**Responsável:** Dev principal

---

### 8. ⚠️ DEPENDENCY INJECTION INCONSISTENTE

**Problema:**
- Hilt implementado mas não em todos módulos
- Alguns ViewModels usam factory manual
- Repositories mistos (Hilt + manual)

**Impacto:**
- 🟠 Código inconsistente
- 🟠 Difícil manutenção
- 🟠 Testes mais difíceis

**Solução:**

1. **Migrar ViewModels para @HiltViewModel:**
```kotlin
// ANTES
class MeuViewModel(
    private val repository: Repository
) : ViewModel()

class MeuViewModelFactory(
    private val repository: Repository
) : ViewModelProvider.Factory {
    // Código boilerplate...
}

// DEPOIS
@HiltViewModel
class MeuViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel()
```

2. **Migrar Repositories para @Inject:**
```kotlin
// Interface permanece igual
interface MeuRepository {
    suspend fun getData(): List<Data>
}

// Implementação usa Hilt
@Singleton
class MeuRepositoryImpl @Inject constructor(
    private val dao: Dao,
    private val firestore: FirebaseFirestore
) : MeuRepository {
    // Implementação...
}
```

3. **Module de binding:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMeuRepository(
        impl: MeuRepositoryImpl
    ): MeuRepository
}
```

**Prazo:** 14/02  
**Esforço:** 1 dia (8h)  
**Responsável:** Dev principal

---

### 9. ⚠️ MÓDULOS COM ACOPLAMENTO ALTO

**Problema:**
- Módulo `ui` depende de `data` diretamente
- Módulo `sync` acessa DAOs diretamente
- Violação da arquitetura limpa

**Impacto:**
- 🟠 Difícil testar isoladamente
- 🟠 Mudanças propagam em cascata
- 🟠 Reuso difícil

**Solução:**

1. **Criar camada de domínio:**
```
projeto/
├── domain/  # NOVO - Interfaces e modelos
│   ├── model/
│   │   ├── Cliente.kt
│   │   └── Acerto.kt
│   └── repository/
│       ├── ClienteRepository.kt (interface)
│       └── AcertoRepository.kt (interface)
├── data/  # Implementações
├── ui/  # Usa domain, não data
└── sync/  # Usa domain, não data
```

2. **Inverter dependências:**
```kotlin
// UI depende de abstração
class ClienteViewModel @Inject constructor(
    private val repository: ClienteRepository // Interface do domain
)

// Data implementa abstração
class ClienteRepositoryImpl @Inject constructor(
    private val dao: ClienteDao
) : ClienteRepository // Do domain
```

**Prazo:** 21/02  
**Esforço:** 3 dias (24h)  
**Responsável:** Dev principal

---

### 10. ⚠️ QUERIES FIRESTORE SEM ÍNDICES

**Problema:**
- Queries complexas sem índices compostos
- Possível lentidão em produção
- Warnings no Firebase Console

**Impacto:**
- 🟠 Performance degradada
- 🟠 Custos maiores (reads desnecessários)
- 🟠 UX prejudicada (lentidão)

**Solução:**

1. **Auditar queries no código:**
```bash
rg "whereEqualTo.*whereEqualTo" --type kt
rg "orderBy.*whereEqualTo" --type kt
```

2. **Verificar Firebase Console:**
```
Console → Firestore → Indexes → (warnings)
```

3. **Criar índices necessários:**
```javascript
// firestore.indexes.json
{
  "indexes": [
    {
      "collectionGroup": "clientes",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "empresa_id", "order": "ASCENDING" },
        { "fieldPath": "rota_id", "order": "ASCENDING" },
        { "fieldPath": "nome", "order": "ASCENDING" }
      ]
    },
    {
      "collectionGroup": "acertos",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "empresa_id", "order": "ASCENDING" },
        { "fieldPath": "data", "order": "DESCENDING" }
      ]
    }
  ]
}
```

4. **Deploy:**
```bash
firebase deploy --only firestore:indexes
```

**Prazo:** 14/02  
**Esforço:** 1 dia (8h)  
**Responsável:** Dev principal

---

### 11. ⚠️ SINCRONIZAÇÃO EM MAIN THREAD

**Problema:**
- Algumas operações de sync bloqueiam UI
- Falta uso de `withContext(Dispatchers.IO)`

**Impacto:**
- 🟠 UI trava durante sync
- 🟠 ANRs (App Not Responding)
- 🟠 UX ruim

**Solução:**
```kotlin
// ANTES - BLOQUEANTE
suspend fun sincronizarDados() {
    firestore.collection("clientes").get() // Main thread!
}

// DEPOIS - NÃO BLOQUEANTE
suspend fun sincronizarDados() = withContext(Dispatchers.IO) {
    firestore.collection("clientes").get()
}

// Para múltiplas operações
suspend fun sincronizarTudo() = withContext(Dispatchers.IO) {
    val clientes = async { sincronizarClientes() }
    val acertos = async { sincronizarAcertos() }
    val mesas = async { sincronizarMesas() }

    awaitAll(clientes, acertos, mesas)
}
```

**Auditoria:**
```bash
# Buscar operações sem Dispatchers.IO
rg "firestore\.collection" --type kt -C 5 | grep -v "Dispatchers.IO"
```

**Prazo:** 07/02  
**Esforço:** 4 horas  
**Responsável:** Dev principal

---

### 12. ⚠️ CACHE LOCAL SUBUTILIZADO

**Problema:**
- Room usado apenas como fallback
- Não há estratégia cache-first
- Sempre busca no Firestore primeiro

**Impacto:**
- 🟠 Lentidão desnecessária
- 🟠 Custos maiores (reads Firestore)
- 🟠 Não funciona offline adequadamente

**Solução:**
```kotlin
// Cache-first strategy
suspend fun getClientes(): Flow<List<Cliente>> = flow {
    // 1. Emitir dados locais imediatamente
    emitAll(dao.getAllClientes())

    // 2. Sync em background (se online)
    if (networkStatus.isConnected) {
        withContext(Dispatchers.IO) {
            try {
                val remotos = firestore.collection("clientes").get().await()
                dao.insertAll(remotos.toClientes())
            } catch (e: Exception) {
                // Falha silenciosa, usa cache
            }
        }
    }
}

// TTL (Time To Live) por collection
data class CacheConfig(
    val collection: String,
    val ttlMillis: Long
)

val cacheTTL = mapOf(
    "clientes" to 5.minutes.inWholeMilliseconds,
    "mesas" to 10.minutes.inWholeMilliseconds,
    "rotas" to 1.hours.inWholeMilliseconds
)
```

**Prazo:** 14/02  
**Esforço:** 2 dias (16h)  
**Responsável:** Dev principal

---

## 🟡 PRIORIDADE MÉDIA (1 Mês)

### 13. 📚 FLUXOS DE NEGÓCIO NÃO DOCUMENTADOS

**Problema:**
- Falta documentação de fluxos críticos
- Como funciona acerto? Cálculo de comissão?
- Conhecimento está apenas na cabeça do dev

**Impacto:**
- 🟡 Onboarding lento (novo dev demora)
- 🟡 Conhecimento concentrado em 1 pessoa
- 🟡 Difícil manutenção futura

**Solução:**

Criar `BUSINESS_FLOWS.md` documentando:

**1. Fluxo de Acerto:**
```markdown
1. Usuário seleciona cliente
2. Sistema busca mesas do cliente
3. Para cada mesa:
   - Informar valor da mesa
   - Informar quantidade de fichas
   - Calcular comissão (se aplicável)
4. Opcionalmente: Registrar troca de pano
5. Calcular total do acerto
6. Confirmar e salvar
7. Gerar comprovante
```

**2. Cálculo de Comissão:**
```markdown
Fórmula: comissao = (quantidade_fichas * valor_ficha) * percentual_comissao

Exemplo:
- Fichas vendidas: 100
- Valor por ficha: R$ 0,60
- Percentual: 60%
- Comissão = (100 * 0.60) * 0.60 = R$ 36,00
```

**3. Troca de Pano:**
```markdown
1. Validar estoque disponível
2. Registrar histórico:
   - Mesa origem
   - Pano anterior
   - Pano novo
   - Usuário responsável
   - Data/hora
   - Origem (acerto/reforma/manutenção)
3. Atualizar mesa (panoId atual)
4. Decrementar estoque pano novo
5. Incrementar estoque pano usado
```

**Prazo:** 07/02  
**Esforço:** 4 horas  
**Responsável:** Dev principal

---

### 14. 📚 TROUBLESHOOTING NÃO DOCUMENTADO

**Problema:**
- Problemas comuns não documentados
- Soluções ficam apenas em relatórios antigos
- Cada dev resolve do zero

**Impacto:**
- 🟡 Tempo perdido resolvendo problemas conhecidos
- 🟡 Conhecimento não reutilizado

**Solução:**

Criar `TROUBLESHOOTING.md`:

```markdown
## PERMISSION_DENIED no Firestore

**Sintoma:** App não sincroniza, logcat mostra PERMISSION_DENIED

**Causas comuns:**
1. Path fora da estrutura (não em `entidades/*/items/`)
2. Campo `empresa_id` ou `rota_id` faltando
3. Usuário não aprovado (`aprovado: false`)
4. Rota não está em `rotasPermitidas`

**Diagnóstico:**
```bash
adb logcat -s FirebaseFirestore:D | grep "PERMISSION_DENIED" -A 5
```

**Solução:**
1. Verificar path do documento
2. Verificar campos obrigatórios
3. Verificar permissions do usuário
4. Testar com `node import-data/testar-security-rules.js`
```

**Prazo:** 14/02  
**Esforço:** 2 horas

---

### 15-21. OUTROS DÉBITOS MÉDIOS

**(Resumidos para economizar espaço)**

**15. Diagramas de arquitetura ausentes**  
**16. Logs excessivos em produção**  
**17. Error handling inconsistente**  
**18. Validação de entrada insuficiente**  
**19. UI/UX melhorias**  
**20. Performance de animações**  
**21. Acessibilidade**

---

## 🟢 PRIORIDADE BAIXA (Backlog)

### 22-27. DÉBITOS DE BACKLOG

**22. Internacionalização (i18n)**  
**23. Testes de UI (Espresso)**  
**24. CI/CD pipeline**  
**25. Dark mode**  
**26. Widget de dashboard**  
**27. Exportação avançada (Excel, PDF)**

---

## 📊 MÉTRICAS DE PROGRESSO

### Cobertura de Testes
- **Atual:** ~5% (7 testes)
- **Meta 2 semanas:** 30%
- **Meta 1 mês:** 60%
- **Meta 3 meses:** 80%

### Tempo de Correção de Bugs
- **Antes:** 2 horas média
- **Com protocolo:** 15-30 minutos
- **Redução:** 75%

### Builds por Problema
- **Antes:** 10+ builds
- **Com Gates:** Máximo 2 builds
- **Redução:** 80%

### Regressões
- **Antes:** Frequentes
- **Com testes:** Próximo a zero
- **Meta:** < 1 por mês

---

## 🔄 PLANO DE AÇÃO

### Semana 1 (28/01 - 03/02)
- [x] Correção bug valores decimais
- [ ] Teste conversão decimal
- [ ] Testes ViewModels críticos
- [ ] Backup automático Firestore
- [ ] Security Rules testes
- [ ] Primeira execução monitoramento

### Semanas 2-3 (04/02 - 14/02)
- [ ] Cobertura 30%
- [ ] Testes Repositories
- [ ] DI consistente
- [ ] Queries com índices
- [ ] Sync em background thread
- [ ] Cache-first strategy
- [ ] Documentar fluxos de negócio
- [ ] Troubleshooting guide

### Mês 2 (15/02 - 15/03)
- [ ] Desacoplar módulos
- [ ] Cobertura 60%
- [ ] Diagramas de arquitetura
- [ ] Error handling unificado
- [ ] UI/UX melhorias

### Trimestre 1 (Jan-Mar/2026)
- [ ] Cobertura 80%
- [ ] CI/CD básico
- [ ] Performance otimizada
- [ ] Documentação completa

---

## 📞 RESPONSÁVEIS E TRACKING

**Dev Principal:** Responsável por itens críticos e altos  
**QA:** Suporte em testes e validação  
**DevOps:** Backup, monitoramento, CI/CD

**Tracking:**
- Issues no GitHub/Jira com labels de prioridade
- Review semanal (toda segunda junto com monitoramento)
- Sprint planning quinzenal

**Comunicação:**
- Daily: Progresso dos itens críticos
- Semanal: Review completo + monitoramento
- Mensal: Revisão de metas e ajuste de prioridades

---

## 🏆 OBJETIVO FINAL

**Meta:** Projeto com nota **9.0/10**

**Critérios de sucesso:**
- ✅ Testes automatizados robustos (80% cobertura)
- ✅ Arquitetura limpa e desacoplada
- ✅ Monitoramento contínuo semanal
- ✅ Documentação completa e atualizada
- ✅ Performance otimizada (sync < 1s)
- ✅ Zero regressões críticas
- ✅ Backup automático funcionando
- ✅ CI/CD pipeline operacional

---

## 📈 DASHBOARD DE PROGRESSO

### Status Atual (28/01/2026)
```
Críticos:    ░░░░░ 0/5 (0%)
Altos:       ░░░░░░░ 0/7 (0%)
Médios:      ░░░░░░░░░ 0/9 (0%)
Baixos:      ░░░░░░ 0/6 (0%)
```

### Meta Semana 1 (03/02/2026)
```
Críticos:    ████░ 4/5 (80%)
Altos:       ░░░░░░░ 0/7 (0%)
```

### Meta Semana 3 (14/02/2026)
```
Críticos:    █████ 5/5 (100%)
Altos:       ██████░ 6/7 (85%)
Médios:      ██░░░░░░░ 2/9 (22%)
```

---

*Documento vivo - Atualizado conforme débitos são resolvidos*  
*Próxima revisão: 03/02/2026 (primeira execução monitoramento)*
