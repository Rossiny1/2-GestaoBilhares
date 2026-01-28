# 📋 DÍVIDA TÉCNICA - GESTÃO BILHARES

> **Gerado em:** 27/01/2026, 21:49  
> **Versão:** 1.0  
> **Projeto:** Gestão de Bilhares (Android Kotlin + Firebase)  
> **Baseline:** Após correção de Security Rules e problema de valores decimais

---

## 📊 RESUMO EXECUTIVO

### Status Geral do Projeto

**Nota Atual:** 7.0/10  
**Nota Potencial (com correções):** 9.0/10

**Distribuição de Dívidas:**
- 🔴 **CRÍTICAS:** 5 itens (ação imediata necessária)
- 🟠 **ALTAS:** 7 itens (resolver em 1-2 semanas)
- 🟡 **MÉDIAS:** 9 itens (resolver em 1 mês)
- 🟢 **BAIXAS:** 6 itens (backlog)

**Total de Dívidas:** 27 itens identificados

---

## 🔴 PRIORIDADE CRÍTICA (Ação Imediata)

### Categoria: Qualidade e Testes

#### 1. ❌ TESTE DE CONVERSÃO DECIMAL AUSENTE

**Problema:**
- Bug de valores decimais multiplicados por 10 foi corrigido hoje (27/01)
- Exemplo: `valor_mesa = 1.50` aparecia como `15.00` na tela de acerto
- **SEM TESTE = Bug pode voltar silenciosamente**

**Impacto:**
- 🔴 **CRÍTICO:** Valores errados = cálculos financeiros incorretos
- 🔴 **CRÍTICO:** Afeta dinheiro real de clientes
- 🔴 **CRÍTICO:** Regressão pode passar despercebida

**Solução:**
```kotlin
@Test
fun `valor_mesa deve ser armazenado como Double em reais`() {
    // Importador: "1,50" → 1.5 (Double em reais)
    val valorImportado = converterValor("1,50")
    assertThat(valorImportado).isEqualTo(1.5)

    // Firestore: Deve armazenar 1.5 (não 150)
    val cliente = Cliente(valor_mesa = 1.5)
    assertThat(cliente.valor_mesa).isEqualTo(1.5)

    // Tela: Deve exibir "R$ 1,50" (não "R$ 15,00")
    val valorFormatado = cliente.valor_mesa.formatarMoeda()
    assertThat(valorFormatado).isEqualTo("R$ 1,50")
}
```

**Esforço:** 1 hora  
**Prazo:** ⏰ Amanhã (28/01) até 12h  
**Responsável:** Dev principal  
**Arquivo:** `app/src/test/java/com/example/gestaobilhares/data/ValorDecimalConverterTest.kt`

---

#### 2. ❌ TESTES DE VIEWMODELS CRÍTICOS AUSENTES

**Problema:**
- **7 testes** encontrados no projeto
- ❌ **FALTAM testes de ViewModels críticos:**
  - `AcertoViewModel` (cálculos financeiros)
  - `SettlementViewModel` (fechamento de acertos)
  - `ClienteViewModel` (cadastro e validações)

**Impacto:**
- 🔴 **CRÍTICO:** Mudanças no código não são validadas automaticamente
- 🔴 **CRÍTICO:** Regressões podem chegar em produção
- 🔴 **CRÍTICO:** Cálculos financeiros sem validação

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
```

**Esforço:** 1 dia (8 horas)  
**Prazo:** ⏰ Esta semana (até 31/01)  
**Responsável:** Dev principal  
**Arquivos necessários:**
- `AcertoViewModelTest.kt`
- `SettlementViewModelTest.kt`
- `ClienteViewModelTest.kt`

---

### Categoria: Infraestrutura

#### 3. ❌ BACKUP AUTOMÁTICO AUSENTE

**Problema:**
- **Zero backup automático** do Firestore
- Dados em produção **sem proteção**
- Recovery manual via Firebase Console (demorado e propenso a erros)

**Impacto:**
- 🔴 **CRÍTICO:** Perda de dados permanente em caso de:
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
  .schedule('0 3 * * *') // Todo dia às 3h AM
  .timeZone('America/Sao_Paulo')
  .onRun(async (context) => {
    const client = new firestore.v1.FirestoreAdminClient();
    const projectId = process.env.GCP_PROJECT || 'gestaobilhares';
    const databaseName = client.databasePath(projectId, '(default)');

    const timestamp = new Date().toISOString().split('T')[0];
    const bucket = `gs://gestaobilhares-backups/backup-${timestamp}`;

    await client.exportDocuments({
      name: databaseName,
      outputUriPrefix: bucket,
      collectionIds: [] // Todas as collections
    });

    console.log(`Backup criado: ${bucket}`);
  });

// Rotação: Manter últimos 30 dias
exports.cleanOldBackups = functions.pubsub
  .schedule('0 4 * * *')
  .onRun(async (context) => {
    // Deletar backups > 30 dias
  });
```

**Esforço:** 2-4 horas  
**Prazo:** ⏰ Esta semana (até 31/01)  
**Responsável:** Dev principal + DevOps  
**Configuração necessária:**
- Cloud Functions habilitadas
- Bucket do Cloud Storage criado
- Permissões configuradas

---

#### 4. ⚠️ MONITORAMENTO SEMANAL NÃO EXECUTADO

**Problema:**
- Checklist `CHECKLIST_MONITORAMENTO_SEMANAL_PRODUCAO.md` **criado**
- **NUNCA foi executado**
- Sem baseline de métricas
- Problemas ocultos não são detectados

**Impacto:**
- 🔴 **ALTO:** Violations no Firestore não monitoradas
- 🔴 **ALTO:** Sincronização pode estar falhando silenciosamente
- 🔴 **ALTO:** Performance não é rastreada

**Solução:**
1. **Primeira execução:** Segunda-feira, 03/02/2026, 9h
2. **Verificar:**
   - Violations no Firebase Console (últimos 7 dias)
   - Logs de sincronização (erros PERMISSION_DENIED)
   - Crashlytics (novos crashes)
   - Performance (queries lentas)
   - Estrutura hierárquica do Firestore
3. **Documentar baseline:**
   - Número de documentos por collection
   - Tempo médio de sync
   - Taxa de sucesso/erro
4. **Gerar relatório:** `monitoramento/relatorios/2026-02-03.md`

**Esforço:** 65 minutos  
**Prazo:** ⏰ Segunda-feira (03/02), 9h  
**Responsável:** Dev principal  
**Frequência:** Semanal (toda segunda-feira)

---

#### 5. 🔒 SECURITY RULES SEM TESTES AUTOMATIZADOS

**Problema:**
- Security Rules foram corrigidas recentemente (27/01)
- **Sem testes automatizados** das regras
- Testes manuais via Service Account (que IGNORA regras)
- Mudanças futuras podem quebrar regras sem detecção

**Impacto:**
- 🔴 **ALTO:** Regras podem ser quebradas em mudanças futuras
- 🔴 **ALTO:** Teste manual é propenso a erros
- 🔴 **ALTO:** Multi-tenancy pode ser comprometido

**Solução:**
```javascript
// tests/firestore.rules.test.js
const { initializeTestEnvironment } = require('@firebase/rules-unit-testing');

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
});
```

**Esforço:** 4 horas  
**Prazo:** ⏰ Esta semana (até 31/01)  
**Responsável:** Dev principal  
**Dependências:**
- `@firebase/rules-unit-testing`
- Java 21+ (para emulador)

---

## 🟠 PRIORIDADE ALTA (1-2 Semanas)

### Categoria: Qualidade e Testes

#### 6. ⚠️ COBERTURA DE TESTES INSUFICIENTE

**Problema:**
- **7 testes** no projeto inteiro
- Cobertura estimada: **~5%**
- ViewModels, Repositories, Use Cases sem testes

**Impacto:**
- 🟠 **ALTO:** Regressões não são detectadas
- 🟠 **ALTO:** Refatorações são arriscadas
- 🟠 **ALTO:** Confiança baixa em mudanças

**Solução:**
- Meta 1: **30% de cobertura** em 2 semanas
- Meta 2: **60% de cobertura** em 1 mês
- Meta 3: **80% de cobertura** em 3 meses

**Prioridade de testes:**
1. Conversão de valores (decimal) ✅ **CRÍTICO**
2. ViewModels críticos (Acerto, Cliente, Settlement)
3. Repositories (AcertoRepository, ClienteRepository)
4. Use Cases (RegistrarTrocaPanoUseCase)
5. Cálculos financeiros (FinancialCalculator)

**Esforço:** 3 dias (24 horas)  
**Prazo:** 2 semanas (até 14/02)  
**Responsável:** Dev principal + QA

---

#### 7. ⚠️ TESTES DE REPOSITORIES AUSENTES

**Problema:**
- **22 Repositories** no projeto
- **Zero testes** de repositories críticos
- Sincronização Firestore sem validação

**Impacto:**
- 🟠 **ALTO:** Queries podem estar incorretas
- 🟠 **ALTO:** Sincronização pode falhar silenciosamente
- 🟠 **ALTO:** Conflitos não são testados

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

    // Verificar Firestore
    val firestoreAcerto = firestore
        .document("empresas/empresa001/entidades/acertos/items/${acerto.id}")
        .get()
        .await()
    assertThat(firestoreAcerto.exists()).isTrue()
}
```

**Esforço:** 2 dias (16 horas)  
**Prazo:** 2 semanas (até 14/02)  
**Responsável:** Dev principal

---

### Categoria: Arquitetura

#### 8. ⚠️ DEPENDENCY INJECTION INCONSISTENTE

**Problema:**
- Hilt implementado mas não em todos os módulos
- Alguns ViewModels usam factory manual
- Repositories mistos (Hilt + manual)

**Impacto:**
- 🟠 **MÉDIO:** Código inconsistente
- 🟠 **MÉDIO:** Difícil manutenção
- 🟠 **MÉDIO:** Testes mais difíceis

**Solução:**
1. Migrar todos ViewModels para `@HiltViewModel`
2. Migrar todos Repositories para `@Inject`
3. Remover factories manuais
4. Documentar padrão

**Esforço:** 1 dia (8 horas)  
**Prazo:** 2 semanas (até 14/02)

---

#### 9. ⚠️ MÓDULOS COM ACOPLAMENTO ALTO

**Problema:**
- Módulo `ui` depende de `data` diretamente
- Módulo `sync` acessa DAOs diretamente
- Violação da arquitetura limpa

**Impacto:**
- 🟠 **MÉDIO:** Difícil testar isoladamente
- 🟠 **MÉDIO:** Mudanças propagam em cascata
- 🟠 **MÉDIO:** Reuso de código difícil

**Solução:**
1. Criar interfaces de domínio
2. Inverter dependências (Dependency Inversion)
3. UI → Domain ← Data

**Esforço:** 3 dias (24 horas)  
**Prazo:** 3 semanas (até 21/02)

---

### Categoria: Performance

#### 10. ⚠️ QUERIES FIRESTORE SEM ÍNDICES

**Problema:**
- Queries complexas sem índices compostos
- Possível lentidão em produção
- Warnings no Firebase Console

**Impacto:**
- 🟠 **MÉDIO:** Performance degradada
- 🟠 **MÉDIO:** Custos maiores (reads desnecessários)
- 🟠 **MÉDIO:** UX prejudicada (lentidão)

**Solução:**
1. Auditar queries no Firebase Console
2. Criar índices compostos necessários
3. Otimizar queries com paginação

**Esforço:** 1 dia (8 horas)  
**Prazo:** 2 semanas (até 14/02)

---

#### 11. ⚠️ SINCRONIZAÇÃO EM MAIN THREAD

**Problema:**
- Algumas operações de sync bloqueiam UI
- Falta uso de `withContext(Dispatchers.IO)`

**Impacto:**
- 🟠 **MÉDIO:** UI trava durante sync
- 🟠 **MÉDIO:** ANRs (App Not Responding)
- 🟠 **MÉDIO:** UX ruim

**Solução:**
```kotlin
// ANTES
suspend fun sincronizarDados() {
    firestore.collection("clientes").get() // Main thread!
}

// DEPOIS
suspend fun sincronizarDados() = withContext(Dispatchers.IO) {
    firestore.collection("clientes").get()
}
```

**Esforço:** 4 horas  
**Prazo:** 1 semana (até 07/02)

---

#### 12. ⚠️ CACHE LOCAL SUBUTILIZADO

**Problema:**
- Room Database usado apenas como fallback
- Não há estratégia de cache-first
- Sempre busca no Firestore

**Impacto:**
- 🟠 **MÉDIO:** Lentidão desnecessária
- 🟠 **MÉDIO:** Custos maiores (reads Firestore)
- 🟠 **MÉDIO:** Não funciona offline adequadamente

**Solução:**
1. Implementar cache-first strategy
2. Sync em background
3. TTL (Time To Live) por collection

**Esforço:** 2 dias (16 horas)  
**Prazo:** 2 semanas (até 14/02)

---

## 🟡 PRIORIDADE MÉDIA (1 Mês)

### Categoria: Documentação

#### 13. 📚 FLUXOS DE NEGÓCIO NÃO DOCUMENTADOS

**Problema:**
- `PROJECT_CONTEXT_FULL.md` existe ✅
- **FALTA:** `FLUXOS_DE_NEGOCIO.md`
- Como funciona acerto? Como calcular comissão?

**Impacto:**
- 🟡 **MÉDIO:** Onboarding lento (novo dev demora para entender)
- 🟡 **MÉDIO:** Conhecimento concentrado em 1 pessoa
- 🟡 **MÉDIO:** Difícil manutenção futura

**Solução:**
Documentar fluxos críticos:
1. **Fluxo de Acerto** (passo a passo)
2. **Cálculo de Comissão** (fórmulas)
3. **Troca de Pano** (processo completo)
4. **Sincronização** (como funciona)
5. **Multi-tenancy** (controle de acesso)

**Esforço:** 4 horas  
**Prazo:** 1 semana (até 07/02)

---

#### 14. 📚 TROUBLESHOOTING NÃO DOCUMENTADO

**Problema:**
- Problemas comuns não estão documentados
- Soluções ficam apenas em relatórios antigos

**Impacto:**
- 🟡 **MÉDIO:** Tempo perdido resolvendo problemas conhecidos
- 🟡 **MÉDIO:** Conhecimento não é reutilizado

**Solução:**
Criar `TROUBLESHOOTING_COMUM.md`:
```markdown
# Problemas Comuns

## 1. Cliente não sincroniza
**Sintoma:** Cliente criado no app não aparece no Firestore
**Causa:** Security Rules bloqueando escrita
**Solução:** Verificar rotasPermitidas do usuário

## 2. Valor aparece multiplicado por 10
**Sintoma:** R$ 1,50 aparece como R$ 15,00
**Causa:** Importador multiplica por 100, app multiplica novamente
**Solução:** Importador deve salvar como Double em reais (1.5)

## 3. PERMISSION_DENIED
**Sintoma:** Erro ao ler/escrever no Firestore
**Causa:** Security Rules ou falta de permissão
**Solução:** Verificar rotasPermitidas e structure hierárquica
```

**Esforço:** 2 horas  
**Prazo:** 1 semana (até 07/02)

---

#### 15. 📚 ONBOARDING DE DEV AUSENTE

**Problema:**
- Sem guia de setup para novos desenvolvedores
- Configuração de ambiente não documentada

**Impacto:**
- 🟡 **MÉDIO:** Novo dev demora 3-5 dias para começar
- 🟡 **MÉDIO:** Erros de configuração comuns

**Solução:**
Criar `ONBOARDING_DEV.md`:
```markdown
# Setup do Ambiente (10 passos, 30 minutos)

1. Clonar repositório
2. Instalar Android Studio
3. Configurar Firebase (google-services.json)
4. Baixar dependências Gradle
5. Executar primeiro build
6. Rodar testes
7. Deploy de Security Rules
8. Configurar emulador
9. Primeiro run no emulador
10. Verificar Crashlytics funcionando
```

**Esforço:** 2 horas  
**Prazo:** 1 semana (até 07/02)

---

### Categoria: DevOps

#### 16. 🚀 CI/CD AUSENTE

**Problema:**
- **100% deploy manual**
- Build manual do APK
- Deploy manual de Security Rules
- Propenso a erros humanos

**Impacto:**
- 🟡 **MÉDIO:** Deploy lento e propenso a erros
- 🟡 **MÉDIO:** Sem garantia de qualidade
- 🟡 **MÉDIO:** Rollback difícil

**Solução:**
```yaml
# .github/workflows/deploy.yml
name: Deploy Production

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup Java
        uses: actions/setup-java@v3
      - name: Run tests
        run: ./gradlew test

  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - name: Build APK
        run: ./gradlew assembleRelease
      - name: Upload APK
        uses: actions/upload-artifact@v3

  deploy-rules:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Deploy Security Rules
        run: firebase deploy --only firestore:rules
```

**Esforço:** 6 horas  
**Prazo:** 2 semanas (até 14/02)

---

#### 17. 🚀 VERSIONAMENTO SEMÂNTICO AUSENTE

**Problema:**
- Sem versionamento claro do app
- `versionCode` e `versionName` não seguem padrão

**Impacto:**
- 🟡 **MÉDIO:** Difícil rastrear versões em produção
- 🟡 **MÉDIO:** Rollback complicado

**Solução:**
Adotar Semantic Versioning:
- `MAJOR.MINOR.PATCH` (ex: 1.2.3)
- MAJOR: Breaking changes
- MINOR: Features novas
- PATCH: Bug fixes

**Esforço:** 1 hora  
**Prazo:** 1 semana (até 07/02)

---

### Categoria: Código

#### 18. 🔧 NULL SAFETY INCONSISTENTE

**Problema:**
- Mistura de `!!` (force unwrap) e `?.let`
- Falta padronização

**Impacto:**
- 🟡 **MÉDIO:** Crashes por NullPointerException
- 🟡 **MÉDIO:** Código inconsistente

**Solução:**
1. Auditar uso de `!!`
2. Substituir por `?.let` ou `?:`
3. Documentar padrão

**Esforço:** 2 dias (16 horas)  
**Prazo:** 3 semanas (até 21/02)

---

#### 19. 🔧 STRINGS HARDCODED

**Problema:**
- Strings em código (não em `strings.xml`)
- Dificulta internacionalização

**Impacto:**
- 🟡 **BAIXO:** Difícil traduzir app
- 🟡 **BAIXO:** Manutenção mais difícil

**Solução:**
1. Migrar strings para `strings.xml`
2. Usar `R.string.nome_da_string`

**Esforço:** 1 dia (8 horas)  
**Prazo:** 1 mês (até 28/02)

---

#### 20. 🔧 MAGIC NUMBERS

**Problema:**
- Números mágicos no código (ex: `* 100`, `/ 1000`)
- Sem constantes nomeadas

**Impacto:**
- 🟡 **BAIXO:** Difícil entender intenção
- 🟡 **BAIXO:** Manutenção mais difícil

**Solução:**
```kotlin
// ANTES
val valorCentavos = valor * 100

// DEPOIS
const val REAIS_TO_CENTAVOS = 100
val valorCentavos = valor * REAIS_TO_CENTAVOS
```

**Esforço:** 4 horas  
**Prazo:** 1 mês (até 28/02)

---

#### 21. 🔧 FUNÇÕES MUITO LONGAS

**Problema:**
- Funções com 100+ linhas
- Difícil testar e entender

**Impacto:**
- 🟡 **MÉDIO:** Manutenção difícil
- 🟡 **MÉDIO:** Bugs escondidos

**Solução:**
1. Extrair funções menores
2. Single Responsibility Principle
3. Máximo 30 linhas por função

**Esforço:** 3 dias (24 horas)  
**Prazo:** 1 mês (até 28/02)

---

## 🟢 PRIORIDADE BAIXA (Backlog)

### Categoria: Melhorias Futuras

#### 22. 💡 MIGRATION PARA JETPACK COMPOSE

**Problema:**
- UI usa XML (Views tradicionais)
- Jetpack Compose é mais moderno

**Impacto:**
- 🟢 **BAIXO:** App funciona bem com XML
- 🟢 **BAIXO:** Compose seria mais produtivo

**Solução:**
Migração gradual:
1. Novas telas em Compose
2. Reescrever telas críticas
3. Migração completa (6 meses)

**Esforço:** 3 meses (480 horas)  
**Prazo:** Backlog (avaliação em 6 meses)

---

#### 23. 💡 DARK MODE

**Problema:**
- Apenas tema light
- Usuários pedem dark mode

**Impacto:**
- 🟢 **BAIXO:** Funciona sem dark mode
- 🟢 **BAIXO:** UX seria melhor

**Solução:**
1. Criar tema dark
2. Implementar switch
3. Salvar preferência

**Esforço:** 2 dias (16 horas)  
**Prazo:** Backlog

---

#### 24. 💡 NOTIFICAÇÕES PUSH

**Problema:**
- Sem notificações push
- Usuários não são alertados de eventos importantes

**Impacto:**
- 🟢 **BAIXO:** App funciona sem push
- 🟢 **BAIXO:** Engagement seria maior

**Solução:**
1. Firebase Cloud Messaging
2. Notificações de:
   - Acerto criado
   - Meta atingida
   - Manutenção vencida

**Esforço:** 1 semana (40 horas)  
**Prazo:** Backlog

---

#### 25. 💡 ANALYTICS AVANÇADO

**Problema:**
- Firebase Analytics básico
- Falta eventos de negócio detalhados

**Impacto:**
- 🟢 **BAIXO:** Crashlytics funciona
- 🟢 **BAIXO:** Mais dados = melhores decisões

**Solução:**
1. Mapear eventos de negócio
2. Implementar tracking
3. Criar dashboards

**Esforço:** 1 semana (40 horas)  
**Prazo:** Backlog

---

#### 26. 💡 OFFLINE-FIRST COMPLETO

**Problema:**
- Offline parcial (Room + Firestore)
- Sync automático às vezes falha

**Impacto:**
- 🟢 **BAIXO:** App funciona com internet
- 🟢 **BAIXO:** Offline seria mais robusto

**Solução:**
1. Implementar WorkManager
2. Sync periódico em background
3. Conflict resolution automático

**Esforço:** 2 semanas (80 horas)  
**Prazo:** Backlog

---

#### 27. 💡 RELATÓRIOS AVANÇADOS

**Problema:**
- Relatórios básicos
- Falta gráficos e exportação

**Impacto:**
- 🟢 **BAIXO:** Relatórios funcionam
- 🟢 **BAIXO:** Mais insights = melhores decisões

**Solução:**
1. Gráficos (MPAndroidChart)
2. Exportar PDF
3. Enviar por email/WhatsApp

**Esforço:** 1 semana (40 horas)  
**Prazo:** Backlog

---

## 📊 ESTATÍSTICAS DA DÍVIDA TÉCNICA

### Distribuição por Prioridade

| Prioridade | Quantidade | % Total | Esforço Total |
|------------|-----------|---------|---------------|
| 🔴 CRÍTICA | 5 itens | 18.5% | ~3 dias (24h) |
| 🟠 ALTA | 7 itens | 25.9% | ~2 semanas (80h) |
| 🟡 MÉDIA | 9 itens | 33.3% | ~2 semanas (80h) |
| 🟢 BAIXA | 6 itens | 22.2% | ~5 meses (800h) |
| **TOTAL** | **27 itens** | **100%** | **~6 meses (984h)** |

### Distribuição por Categoria

| Categoria | Quantidade | % Total |
|-----------|-----------|---------|
| Qualidade e Testes | 6 itens | 22.2% |
| Infraestrutura | 4 itens | 14.8% |
| Arquitetura | 3 itens | 11.1% |
| Performance | 4 itens | 14.8% |
| Documentação | 3 itens | 11.1% |
| DevOps | 2 itens | 7.4% |
| Código | 4 itens | 14.8% |
| Melhorias Futuras | 6 itens | 22.2% |

### Impacto vs Esforço

```
        Alto Impacto
            |
    Teste   |   Backup
    Decimal |   Auto
    ViewModels  Monitoring
    --------|--------
    Dark    |   Offline
    Mode    |   First
            |
        Baixo Impacto

    Baixo   |   Alto
        Esforço
```

---

## 🎯 ROADMAP DE RESOLUÇÃO

### Semana 1 (27/01 - 02/02)

**🔴 CRÍTICO:**
1. ✅ Criar teste de conversão decimal (1h)
2. ✅ Implementar backup automático (4h)
3. ✅ Testes de ViewModels críticos (8h)

**Total:** 13 horas

---

### Semana 2 (03/02 - 09/02)

**🔴 CRÍTICO:**
4. ✅ Executar monitoramento semanal (1h)
5. ✅ Testes de Security Rules (4h)

**🟠 ALTA:**
6. ✅ Testes de Repositories (16h)

**Total:** 21 horas

---

### Semana 3 (10/02 - 16/02)

**🟠 ALTA:**
7. ✅ Cobertura de testes 30% (8h)
8. ✅ DI consistente (8h)
9. ✅ Queries Firestore otimizadas (8h)

**Total:** 24 horas

---

### Semana 4 (17/02 - 23/02)

**🟠 ALTA:**
10. ✅ Módulos desacoplados (24h)

**🟡 MÉDIA:**
11. ✅ Documentação completa (8h)

**Total:** 32 horas

---

### Mês 2 (Março/2026)

**🟡 MÉDIA:**
- CI/CD básico
- Versionamento semântico
- Null safety consistente
- Strings localizadas
- Refatoração de funções longas

**Total:** 80 horas

---

### Mês 3+ (Abril/2026+)

**🟢 BAIXA:**
- Melhorias futuras (backlog)
- Avaliação conforme prioridade de negócio

---

## 📈 MÉTRICAS DE PROGRESSO

### Metas Trimestrais (Q1 2026)

**Janeiro:**
- ✅ Resolver 5 itens críticos
- ✅ Nota: 7.0 → 8.0

**Fevereiro:**
- ✅ Resolver 7 itens alta prioridade
- ✅ Cobertura de testes: 5% → 30%
- ✅ Nota: 8.0 → 8.5

**Março:**
- ✅ Resolver 9 itens média prioridade
- ✅ Cobertura de testes: 30% → 60%
- ✅ Nota: 8.5 → 9.0

---

## 🚨 RISCOS E DEPENDÊNCIAS

### Riscos Identificados

**1. Teste de Decimal não criado → Bug volta**
- Probabilidade: ALTA
- Impacto: CRÍTICO
- Mitigação: Criar teste até amanhã

**2. Backup ausente → Perda de dados**
- Probabilidade: BAIXA
- Impacto: CATASTRÓFICO
- Mitigação: Implementar esta semana

**3. Testes insuficientes → Regressões**
- Probabilidade: ALTA
- Impacto: ALTO
- Mitigação: 30% coverage em 2 semanas

### Dependências Externas

- Java 21+ (testes de Security Rules)
- Cloud Functions habilitadas (backup)
- Budget para Firestore reads/writes

---

## ✅ CONCLUSÃO

### Situação Atual

**Nota Geral:** 7.0/10

**Pontos Fortes:**
- ✅ Produção funcional e estável
- ✅ Logging excelente (Timber em 95 arquivos)
- ✅ Crashlytics 100% funcional
- ✅ Arquitetura MVVM sólida

**Pontos Críticos:**
- ❌ Teste de conversão decimal ausente
- ❌ Backup automático ausente
- ❌ Cobertura de testes 5%

### Situação Após Resolução

**Nota Potencial:** 9.0/10

**Melhorias:**
- ✅ Testes robustos (60% coverage)
- ✅ Backup automático diário
- ✅ CI/CD implementado
- ✅ Documentação completa
- ✅ Performance otimizada

---

## 📞 CONTATO E MANUTENÇÃO

**Responsável:** Dev Principal  
**Última atualização:** 27/01/2026, 21:49  
**Próxima revisão:** Segunda-feira, 03/02/2026  
**Frequência:** Semanal (toda segunda-feira)

---

**FIM DO DOCUMENTO** 📋
