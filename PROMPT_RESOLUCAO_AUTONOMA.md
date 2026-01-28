# 🚀 PROMPT DE RESOLUÇÃO AUTÔNOMA - PROBLEMAS CRÍTICOS
## Gestão de Bilhares - Android Kotlin

> **OBJETIVO:** Executar autonomamente até APP COMPILADO + TESTES UNITÁRIOS FUNCIONANDO  
> **MODALIDADE:** Desenvolvedor Android Sênior - Execução Autônoma  
> **DATA:** 27/01/2026  
> **NÃO PARAR ATÉ CONCLUSÃO COMPLETA**

---

## 🎯 MISSÃO CRÍTICA

Você é um **Desenvolvedor Android Sênior** com expertise em:
- Kotlin, Android Native, Jetpack Compose
- Firebase (Firestore, Auth, Crashlytics)
- MVVM + Hilt + Room Database
- Testes Unitários (JUnit, Truth, Mockk)
- Diagnóstico científico (Static/Dynamic Analysis)

**SUA META:** Resolver TODOS os problemas críticos até ter:
1. ✅ App compilando sem erros
2. ✅ Testes unitários críticos funcionando (>30% cobertura)
3. ✅ Infraestrutura essencial implementada
4. ✅ Sem regressões conhecidas

**VOCÊ NÃO PODE PARAR ATÉ ATINGIR 100% DA META.**

---

## 📋 PROBLEMAS CRÍTICOS IDENTIFICADOS

### 🔴 CRÍTICO 1: TESTE DE CONVERSÃO DECIMAL AUSENTE
**RISCO:** Bug de valores multiplicados por 10 pode voltar sem detecção  
**IMPACTO:** Cálculos financeiros incorretos, valores de R$ 1,50 aparecem como R$ 15,00  

**SOLUÇÃO OBRIGATÓRIA:**
```kotlin
// Arquivo: app/src/test/java/com/example/gestaobilhares/data/ValorDecimalConverterTest.kt
package com.example.gestaobilhares.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ValorDecimalConverterTest {

    @Test
    fun `valor_mesa deve ser armazenado como Double em reais`() {
        // GIVEN: Importador converte string para Double
        val valorImportado = converterValor("1,50")

        // THEN: Deve ser 1.5 (não 150)
        assertThat(valorImportado).isEqualTo(1.5)
    }

    @Test
    fun `Cliente valor_mesa deve manter valor em reais`() {
        // GIVEN: Cliente criado com valor 1.5
        val cliente = Cliente(
            id = "cliente001",
            nome = "Teste",
            valor_mesa = 1.5
        )

        // THEN: Não deve multiplicar por 100
        assertThat(cliente.valor_mesa).isEqualTo(1.5)
    }

    @Test
    fun `Formatacao monetaria deve exibir corretamente`() {
        // GIVEN: Valor em Double
        val valor = 1.5

        // WHEN: Formatar para moeda
        val valorFormatado = valor.formatarMoeda()

        // THEN: Deve exibir R$ 1,50 (não R$ 15,00)
        assertThat(valorFormatado).isEqualTo("R$ 1,50")
    }

    @Test
    fun `Acerto valor_mesa deve calcular total corretamente`() {
        // GIVEN: Acerto com valor_mesa = 150.0 e comissao = 60.0
        val acerto = Acerto(
            valor_mesa = 150.0,
            comissao = 60.0
        )

        // WHEN: Calcular total
        val total = acerto.calcularTotal()

        // THEN: Deve ser 210.0 (não 2100.0)
        assertThat(total).isEqualTo(210.0)
    }

    // Helper functions
    private fun converterValor(valor: String): Double {
        return valor.replace(",", ".").toDouble()
    }

    private fun Double.formatarMoeda(): String {
        return "R$ %.2f".format(this).replace(".", ",")
    }
}
```

**CRITÉRIOS DE SUCESSO:**
- [ ] Arquivo criado em `app/src/test/java/com/example/gestaobilhares/data/ValorDecimalConverterTest.kt`
- [ ] Todos os 4 testes passando
- [ ] Build rodando: `./gradlew :app:testDebugUnitTest`
- [ ] Output: `BUILD SUCCESSFUL` com 4 testes verdes

---

### 🔴 CRÍTICO 2: TESTES DE VIEWMODELS AUSENTES
**RISCO:** Mudanças no código não são validadas, regressões chegam em produção  
**IMPACTO:** Cálculos financeiros críticos sem validação automática  

**SOLUÇÃO OBRIGATÓRIA:**

#### 2.1 AcertoViewModelTest

```kotlin
// Arquivo: app/src/test/java/com/example/gestaobilhares/ui/settlement/AcertoViewModelTest.kt
package com.example.gestaobilhares.ui.settlement

import com.example.gestaobilhares.data.repository.AppRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AcertoViewModelTest {

    private lateinit var viewModel: SettlementViewModel
    private lateinit var repository: AppRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = SettlementViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `deve calcular total de acerto corretamente`() = runTest {
        // GIVEN: Acerto com valores conhecidos
        val acerto = Acerto(
            id = "acerto001",
            valor_mesa = 150.0,
            comissao = 60.0,
            quantidade_fichas = 100
        )

        // WHEN: Calcular total
        val total = viewModel.calcularTotalAcerto(acerto)

        // THEN: Total deve ser soma de valores
        assertThat(total).isEqualTo(210.0)
    }

    @Test
    fun `deve validar acerto antes de salvar`() = runTest {
        // GIVEN: Acerto inválido (valor negativo)
        val acertoInvalido = Acerto(
            id = "acerto002",
            valor_mesa = -10.0,
            comissao = 0.0
        )

        // WHEN: Tentar salvar
        val resultado = viewModel.salvarAcerto(acertoInvalido)

        // THEN: Deve falhar com erro de validação
        assertThat(resultado.isFailure).isTrue()
        assertThat(resultado.exceptionOrNull()?.message)
            .contains("valor_mesa não pode ser negativo")
    }

    @Test
    fun `deve carregar acertos por rota`() = runTest {
        // GIVEN: Repository retorna lista de acertos
        val acertos = listOf(
            Acerto(id = "1", valor_mesa = 100.0),
            Acerto(id = "2", valor_mesa = 200.0)
        )
        coEvery { repository.buscarAcertosPorRota(any()) } returns flowOf(acertos)

        // WHEN: Carregar acertos
        viewModel.carregarAcertos(rotaId = 1L)
        advanceUntilIdle()

        // THEN: Estado deve conter acertos
        assertThat(viewModel.uiState.value.acertos).hasSize(2)
        assertThat(viewModel.uiState.value.acertos).isEqualTo(acertos)
    }

    @Test
    fun `deve registrar troca de pano com sucesso`() = runTest {
        // GIVEN: Dados de troca de pano
        coEvery { repository.registrarTrocaPano(any()) } returns Result.success(Unit)

        // WHEN: Registrar troca
        val resultado = viewModel.registrarTrocaPano(
            mesaId = 1L,
            panoId = 10L,
            usuarioId = 5L
        )
        advanceUntilIdle()

        // THEN: Deve ter sucesso
        assertThat(resultado.isSuccess).isTrue()
    }

    @Test
    fun `deve tratar erro ao salvar acerto`() = runTest {
        // GIVEN: Repository retorna erro
        coEvery { repository.criarAcerto(any()) } returns 
            Result.failure(Exception("Erro de rede"))

        // WHEN: Tentar salvar
        val resultado = viewModel.salvarAcerto(Acerto(id = "3", valor_mesa = 100.0))
        advanceUntilIdle()

        // THEN: Deve propagar erro
        assertThat(resultado.isFailure).isTrue()
        assertThat(viewModel.uiState.value.erro).isNotNull()
    }
}
```

#### 2.2 ClienteViewModelTest

```kotlin
// Arquivo: app/src/test/java/com/example/gestaobilhares/ui/routes/ClienteViewModelTest.kt
package com.example.gestaobilhares.ui.routes

import com.example.gestaobilhares.data.repository.AppRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClienteViewModelTest {

    private lateinit var viewModel: RoutesViewModel
    private lateinit var repository: AppRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = RoutesViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `deve validar campos obrigatorios do cliente`() = runTest {
        // GIVEN: Cliente sem nome
        val clienteInvalido = Cliente(
            id = "cliente001",
            nome = "",
            rota = "037-Salinas"
        )

        // WHEN: Validar
        val resultado = viewModel.validarCliente(clienteInvalido)

        // THEN: Deve falhar
        assertThat(resultado.isFailure).isTrue()
        assertThat(resultado.exceptionOrNull()?.message)
            .contains("Nome é obrigatório")
    }

    @Test
    fun `deve transferir cliente entre rotas`() = runTest {
        // GIVEN: Cliente e nova rota
        val cliente = Cliente(id = "1", nome = "João", rota = "037-Salinas")
        val novaRota = "034-Bonito"
        coEvery { repository.transferirCliente(any(), any()) } returns Result.success(Unit)

        // WHEN: Transferir
        val resultado = viewModel.transferirCliente(cliente, novaRota)
        advanceUntilIdle()

        // THEN: Deve ter sucesso
        assertThat(resultado.isSuccess).isTrue()
    }

    @Test
    fun `deve carregar clientes por rota`() = runTest {
        // GIVEN: Repository retorna clientes
        val clientes = listOf(
            Cliente(id = "1", nome = "João", rota = "037-Salinas"),
            Cliente(id = "2", nome = "Maria", rota = "037-Salinas")
        )
        coEvery { repository.listarClientes() } returns flowOf(clientes)

        // WHEN: Carregar
        viewModel.carregarClientes(rotaId = 1L)
        advanceUntilIdle()

        // THEN: Estado deve conter clientes
        assertThat(viewModel.uiState.value.clientes).hasSize(2)
    }
}
```

**CRITÉRIOS DE SUCESSO:**
- [ ] 2 arquivos de teste criados (AcertoViewModelTest, ClienteViewModelTest)
- [ ] Mínimo 8 testes implementados (5 + 3)
- [ ] Build rodando: `./gradlew :app:testDebugUnitTest`
- [ ] Todos os testes passando
- [ ] Cobertura de código >30% nos ViewModels testados

---

### 🔴 CRÍTICO 3: BACKUP AUTOMÁTICO AUSENTE
**RISCO:** Perda permanente de dados em produção sem possibilidade de recuperação  
**IMPACTO:** Desastre total em caso de corrupção, exclusão acidental ou bug crítico  

**SOLUÇÃO OBRIGATÓRIA:**

#### 3.1 Firebase Functions - Backup Diário

```javascript
// Arquivo: functions/index.js
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const firestore = require('@google-cloud/firestore');

admin.initializeApp();

// Backup automático diário às 3h AM
exports.backupFirestore = functions.pubsub
  .schedule('0 3 * * *') // Todo dia às 3h AM
  .timeZone('America/Sao_Paulo')
  .onRun(async (context) => {
    try {
      const client = new firestore.v1.FirestoreAdminClient();
      const projectId = process.env.GCP_PROJECT || 'gestaobilhares';
      const databaseName = client.databasePath(projectId, '(default)');

      const timestamp = new Date().toISOString().split('T')[0];
      const bucket = `gs://${projectId}-backups/backup-${timestamp}`;

      console.log(`Iniciando backup para: ${bucket}`);

      const [operation] = await client.exportDocuments({
        name: databaseName,
        outputUriPrefix: bucket,
        collectionIds: [] // Todas as collections
      });

      console.log(`Backup criado com sucesso: ${bucket}`);

      // Log no Firestore
      await admin.firestore().collection('_system').doc('backups').collection('logs').add({
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        bucket: bucket,
        status: 'SUCCESS',
        operationId: operation.name
      });

      return { success: true, bucket };
    } catch (error) {
      console.error('Erro no backup:', error);

      // Log de erro
      await admin.firestore().collection('_system').doc('backups').collection('logs').add({
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        status: 'ERROR',
        error: error.message
      });

      throw error;
    }
  });

// Limpeza de backups antigos (manter últimos 30 dias)
exports.cleanOldBackups = functions.pubsub
  .schedule('0 4 * * *') // Todo dia às 4h AM
  .timeZone('America/Sao_Paulo')
  .onRun(async (context) => {
    const { Storage } = require('@google-cloud/storage');
    const storage = new Storage();
    const projectId = process.env.GCP_PROJECT || 'gestaobilhares';
    const bucketName = `${projectId}-backups`;

    try {
      const [files] = await storage.bucket(bucketName).getFiles();
      const now = Date.now();
      const thirtyDaysAgo = now - (30 * 24 * 60 * 60 * 1000);

      let deletedCount = 0;

      for (const file of files) {
        const [metadata] = await file.getMetadata();
        const createdTime = new Date(metadata.timeCreated).getTime();

        if (createdTime < thirtyDaysAgo) {
          await file.delete();
          console.log(`Backup deletado: ${file.name}`);
          deletedCount++;
        }
      }

      console.log(`Limpeza concluída: ${deletedCount} backups removidos`);
      return { deleted: deletedCount };
    } catch (error) {
      console.error('Erro na limpeza:', error);
      throw error;
    }
  });

// Função para restaurar backup manualmente (chamada via HTTP)
exports.restoreBackup = functions.https.onCall(async (data, context) => {
  // Verificar autenticação e permissões
  if (!context.auth || !context.auth.token.admin) {
    throw new functions.https.HttpsError(
      'permission-denied',
      'Apenas administradores podem restaurar backups'
    );
  }

  const { backupPath } = data;

  if (!backupPath) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'backupPath é obrigatório'
    );
  }

  try {
    const client = new firestore.v1.FirestoreAdminClient();
    const projectId = process.env.GCP_PROJECT || 'gestaobilhares';
    const databaseName = client.databasePath(projectId, '(default)');

    console.log(`Restaurando backup de: ${backupPath}`);

    const [operation] = await client.importDocuments({
      name: databaseName,
      inputUriPrefix: backupPath
    });

    console.log(`Restauração iniciada: ${operation.name}`);

    return { 
      success: true, 
      operationId: operation.name,
      message: 'Restauração iniciada com sucesso'
    };
  } catch (error) {
    console.error('Erro na restauração:', error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});
```

#### 3.2 Package.json para Firebase Functions

```json
{
  "name": "functions",
  "description": "Cloud Functions for Gestão Bilhares",
  "scripts": {
    "serve": "firebase emulators:start --only functions",
    "shell": "firebase functions:shell",
    "start": "npm run shell",
    "deploy": "firebase deploy --only functions",
    "logs": "firebase functions:log"
  },
  "engines": {
    "node": "18"
  },
  "main": "index.js",
  "dependencies": {
    "firebase-admin": "^12.0.0",
    "firebase-functions": "^4.5.0",
    "@google-cloud/firestore": "^7.1.0",
    "@google-cloud/storage": "^7.7.0"
  },
  "devDependencies": {
    "firebase-functions-test": "^3.1.0"
  },
  "private": true
}
```

#### 3.3 Script de Deploy

```bash
#!/bin/bash
# Arquivo: scripts/deploy-backup-functions.sh

echo "🚀 Deploy de Firebase Functions - Backup Automático"
echo "=================================================="

cd functions

echo "📦 Instalando dependências..."
npm install

echo "🔧 Configurando Firebase..."
firebase use gestaobilhares

echo "📤 Fazendo deploy das functions..."
firebase deploy --only functions:backupFirestore,functions:cleanOldBackups,functions:restoreBackup

echo "✅ Deploy concluído!"
echo ""
echo "📊 Verificar logs:"
echo "firebase functions:log --only backupFirestore"
echo ""
echo "🗄️ Verificar backups no Console:"
echo "https://console.cloud.google.com/storage/browser/gestaobilhares-backups"
```

**CRITÉRIOS DE SUCESSO:**
- [ ] Arquivo `functions/index.js` criado
- [ ] Arquivo `functions/package.json` criado
- [ ] Script `scripts/deploy-backup-functions.sh` criado
- [ ] Functions deployadas: `firebase deploy --only functions`
- [ ] Backup rodando diariamente (verificar logs após 24h)
- [ ] Bucket criado no Cloud Storage: `gestaobilhares-backups`

---

### 🔴 CRÍTICO 4: SECURITY RULES SEM TESTES
**RISCO:** Mudanças futuras podem quebrar regras de segurança sem detecção  
**IMPACTO:** Multi-tenancy comprometido, dados expostos entre rotas  

**SOLUÇÃO OBRIGATÓRIA:**

```javascript
// Arquivo: tests/firestore.rules.test.js
const { initializeTestEnvironment, assertSucceeds, assertFails } = 
  require('@firebase/rules-unit-testing');
const fs = require('fs');

describe('Security Rules - Multi-tenancy', () => {
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

  afterEach(async () => {
    await testEnv.clearFirestore();
  });

  describe('Clientes - Acesso por Rota', () => {
    test('Usuário pode ler clientes da própria rota', async () => {
      const alice = testEnv.authenticatedContext('alice', {
        rotasPermitidas: ['037-Salinas']
      });

      const clienteRef = alice.firestore()
        .doc('empresas/empresa001/entidades/clientes/items/cliente001');

      // Criar documento de teste
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore()
          .doc('empresas/empresa001/entidades/clientes/items/cliente001')
          .set({ nome: 'João', rota: '037-Salinas' });
      });

      await assertSucceeds(clienteRef.get());
    });

    test('Usuário NÃO pode ler clientes de outra rota', async () => {
      const bob = testEnv.authenticatedContext('bob', {
        rotasPermitidas: ['034-Bonito']
      });

      // Cliente da rota 037-Salinas
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore()
          .doc('empresas/empresa001/entidades/clientes/items/cliente001')
          .set({ nome: 'João', rota: '037-Salinas' });
      });

      const clienteRef = bob.firestore()
        .doc('empresas/empresa001/entidades/clientes/items/cliente001');

      await assertFails(clienteRef.get());
    });

    test('Usuário pode criar cliente na própria rota', async () => {
      const alice = testEnv.authenticatedContext('alice', {
        rotasPermitidas: ['037-Salinas']
      });

      const novoCliente = alice.firestore()
        .doc('empresas/empresa001/entidades/clientes/items/cliente002');

      await assertSucceeds(novoCliente.set({
        nome: 'Maria',
        rota: '037-Salinas',
        createdAt: new Date()
      }));
    });

    test('Usuário NÃO pode criar cliente em outra rota', async () => {
      const alice = testEnv.authenticatedContext('alice', {
        rotasPermitidas: ['037-Salinas']
      });

      const novoCliente = alice.firestore()
        .doc('empresas/empresa001/entidades/clientes/items/cliente003');

      await assertFails(novoCliente.set({
        nome: 'Carlos',
        rota: '034-Bonito', // Rota diferente!
        createdAt: new Date()
      }));
    });
  });

  describe('Acertos - Acesso por Rota', () => {
    test('Usuário pode ler acertos da própria rota', async () => {
      const alice = testEnv.authenticatedContext('alice', {
        rotasPermitidas: ['037-Salinas']
      });

      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore()
          .doc('empresas/empresa001/entidades/acertos/items/acerto001')
          .set({ 
            rotaId: '037-Salinas',
            valor_mesa: 150.0 
          });
      });

      const acertoRef = alice.firestore()
        .doc('empresas/empresa001/entidades/acertos/items/acerto001');

      await assertSucceeds(acertoRef.get());
    });

    test('Usuário NÃO pode modificar acertos de outra rota', async () => {
      const bob = testEnv.authenticatedContext('bob', {
        rotasPermitidas: ['034-Bonito']
      });

      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore()
          .doc('empresas/empresa001/entidades/acertos/items/acerto001')
          .set({ 
            rotaId: '037-Salinas',
            valor_mesa: 150.0 
          });
      });

      const acertoRef = bob.firestore()
        .doc('empresas/empresa001/entidades/acertos/items/acerto001');

      await assertFails(acertoRef.update({ valor_mesa: 200.0 }));
    });
  });

  describe('Usuários não autenticados', () => {
    test('Usuário não autenticado NÃO pode ler nada', async () => {
      const unauthenticated = testEnv.unauthenticatedContext();

      const clienteRef = unauthenticated.firestore()
        .doc('empresas/empresa001/entidades/clientes/items/cliente001');

      await assertFails(clienteRef.get());
    });

    test('Usuário não autenticado NÃO pode escrever nada', async () => {
      const unauthenticated = testEnv.unauthenticatedContext();

      const clienteRef = unauthenticated.firestore()
        .doc('empresas/empresa001/entidades/clientes/items/cliente001');

      await assertFails(clienteRef.set({ nome: 'Hacker' }));
    });
  });
});
```

#### Package.json para testes

```json
{
  "name": "firestore-rules-tests",
  "version": "1.0.0",
  "scripts": {
    "test": "jest --testEnvironment=node",
    "test:watch": "jest --watch"
  },
  "devDependencies": {
    "@firebase/rules-unit-testing": "^3.0.2",
    "jest": "^29.7.0"
  }
}
```

**CRITÉRIOS DE SUCESSO:**
- [ ] Arquivo `tests/firestore.rules.test.js` criado
- [ ] Arquivo `tests/package.json` criado
- [ ] Dependências instaladas: `npm install`
- [ ] Testes rodando: `npm test`
- [ ] Todos os 8 testes passando
- [ ] Java 21+ instalado (necessário para emulador)

---

### 🔴 CRÍTICO 5: MONITORAMENTO NÃO EXECUTADO
**RISCO:** Problemas ocultos em produção não são detectados  
**IMPACTO:** Violations, erros de sincronização e performance degradada passam despercebidos  

**SOLUÇÃO OBRIGATÓRIA:**

#### 5.1 Script de Monitoramento Automático

```kotlin
// Arquivo: app/src/main/java/com/example/gestaobilhares/monitoring/MonitoringManager.kt
package com.example.gestaobilhares.monitoring

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.perf.FirebasePerformance
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitoringManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val crashlytics: FirebaseCrashlytics,
    private val performance: FirebasePerformance
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    fun iniciarMonitoramento() {
        scope.launch {
            try {
                Timber.d("📊 Iniciando monitoramento semanal")

                val relatorio = MonitoringReport(
                    dataExecucao = System.currentTimeMillis(),
                    violations = verificarViolations(),
                    errosSincronizacao = verificarErrosSincronizacao(),
                    metricsPerformance = coletarMetricsPerformance(),
                    estruturaFirestore = validarEstruturaFirestore(),
                    statusSaude = calcularStatusSaude()
                )

                salvarRelatorio(relatorio)
                enviarNotificacao(relatorio)

                Timber.i("✅ Monitoramento concluído: ${relatorio.statusSaude}")
            } catch (e: Exception) {
                Timber.e(e, "❌ Erro no monitoramento")
                crashlytics.recordException(e)
            }
        }
    }

    private suspend fun verificarViolations(): ViolationsData {
        // Verificar violations no Firestore (últimos 7 dias)
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)

        val violations = firestore.collection("_system")
            .document("monitoring")
            .collection("violations")
            .whereGreaterThan("timestamp", sevenDaysAgo)
            .get()
            .await()

        return ViolationsData(
            total = violations.size(),
            porTipo = violations.documents.groupBy { 
                it.getString("tipo") ?: "UNKNOWN" 
            }.mapValues { it.value.size }
        )
    }

    private suspend fun verificarErrosSincronizacao(): SyncErrorsData {
        // Verificar erros PERMISSION_DENIED nos últimos 7 dias
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)

        val erros = firestore.collection("_system")
            .document("monitoring")
            .collection("sync_errors")
            .whereGreaterThan("timestamp", sevenDaysAgo)
            .get()
            .await()

        val permissionDenied = erros.documents.count { 
            it.getString("error")?.contains("PERMISSION_DENIED") == true 
        }

        return SyncErrorsData(
            totalErros = erros.size(),
            permissionDenied = permissionDenied,
            outrosErros = erros.size() - permissionDenied
        )
    }

    private suspend fun coletarMetricsPerformance(): PerformanceMetrics {
        // Coletar métricas de performance das queries principais
        val trace = performance.newTrace("weekly_monitoring")
        trace.start()

        val clientesQueryTime = medirQueryTime { 
            firestore.collection("empresas")
                .document("empresa001")
                .collection("entidades")
                .document("clientes")
                .collection("items")
                .limit(10)
                .get()
                .await()
        }

        val acertosQueryTime = medirQueryTime {
            firestore.collection("empresas")
                .document("empresa001")
                .collection("entidades")
                .document("acertos")
                .collection("items")
                .limit(10)
                .get()
                .await()
        }

        trace.stop()

        return PerformanceMetrics(
            clientesQueryMs = clientesQueryTime,
            acertosQueryMs = acertosQueryTime,
            mediaGeralMs = (clientesQueryTime + acertosQueryTime) / 2
        )
    }

    private suspend fun validarEstruturaFirestore(): EstruturaValidation {
        // Validar estrutura hierárquica do Firestore
        val issues = mutableListOf<String>()

        // Verificar collections principais
        val requiredCollections = listOf("empresas", "_system")
        for (collection in requiredCollections) {
            val exists = firestore.collection(collection).limit(1).get().await().isEmpty.not()
            if (!exists) {
                issues.add("Collection '$collection' não encontrada")
            }
        }

        // Verificar entidades
        val entidades = listOf("clientes", "acertos", "mesas")
        val empresaRef = firestore.collection("empresas").document("empresa001")

        for (entidade in entidades) {
            val exists = empresaRef.collection("entidades")
                .document(entidade)
                .collection("items")
                .limit(1)
                .get()
                .await()
                .isEmpty
                .not()

            if (!exists) {
                issues.add("Entidade '$entidade' sem dados")
            }
        }

        return EstruturaValidation(
            valida = issues.isEmpty(),
            issues = issues
        )
    }

    private fun calcularStatusSaude(
        violations: ViolationsData,
        erros: SyncErrorsData,
        performance: PerformanceMetrics,
        estrutura: EstruturaValidation
    ): HealthStatus {
        var pontos = 100

        // Descontar por violations
        pontos -= violations.total * 5

        // Descontar por erros de sincronização
        pontos -= erros.totalErros * 3
        pontos -= erros.permissionDenied * 10

        // Descontar por performance ruim (> 1000ms)
        if (performance.mediaGeralMs > 1000) {
            pontos -= 20
        } else if (performance.mediaGeralMs > 500) {
            pontos -= 10
        }

        // Descontar por estrutura inválida
        if (!estrutura.valida) {
            pontos -= 30
        }

        return when {
            pontos >= 90 -> HealthStatus.EXCELENTE
            pontos >= 70 -> HealthStatus.BOM
            pontos >= 50 -> HealthStatus.REGULAR
            else -> HealthStatus.CRITICO
        }
    }

    private suspend fun salvarRelatorio(relatorio: MonitoringReport) {
        val dataFormatada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date(relatorio.dataExecucao))

        firestore.collection("_system")
            .document("monitoring")
            .collection("reports")
            .document(dataFormatada)
            .set(relatorio)
            .await()

        Timber.i("📄 Relatório salvo: $dataFormatada")
    }

    private fun enviarNotificacao(relatorio: MonitoringReport) {
        if (relatorio.statusSaude == HealthStatus.CRITICO) {
            // Enviar notificação de alerta
            Timber.w("🚨 Status CRÍTICO detectado! Verificar imediatamente.")
            crashlytics.log("Status saúde: CRÍTICO - Monitoramento semanal")
        }
    }

    private suspend fun medirQueryTime(block: suspend () -> Unit): Long {
        val inicio = System.currentTimeMillis()
        block()
        return System.currentTimeMillis() - inicio
    }
}

// Data classes
data class MonitoringReport(
    val dataExecucao: Long,
    val violations: ViolationsData,
    val errosSincronizacao: SyncErrorsData,
    val metricsPerformance: PerformanceMetrics,
    val estruturaFirestore: EstruturaValidation,
    val statusSaude: HealthStatus
)

data class ViolationsData(
    val total: Int,
    val porTipo: Map<String, Int>
)

data class SyncErrorsData(
    val totalErros: Int,
    val permissionDenied: Int,
    val outrosErros: Int
)

data class PerformanceMetrics(
    val clientesQueryMs: Long,
    val acertosQueryMs: Long,
    val mediaGeralMs: Long
)

data class EstruturaValidation(
    val valida: Boolean,
    val issues: List<String>
)

enum class HealthStatus {
    EXCELENTE, BOM, REGULAR, CRITICO
}
```

#### 5.2 WorkManager para Execução Semanal

```kotlin
// Arquivo: app/src/main/java/com/example/gestaobilhares/monitoring/MonitoringWorker.kt
package com.example.gestaobilhares.monitoring

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class MonitoringWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val monitoringManager: MonitoringManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            monitoringManager.iniciarMonitoramento()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "MonitoringWeeklyWork"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<MonitoringWorker>(
                repeatInterval = 7,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.HOURS) // Primeira execução em 1h
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }
    }
}
```

**CRITÉRIOS DE SUCESSO:**
- [ ] Arquivo `MonitoringManager.kt` criado
- [ ] Arquivo `MonitoringWorker.kt` criado
- [ ] WorkManager agendado no `Application.onCreate()`
- [ ] Primeira execução manual rodada com sucesso
- [ ] Relatório salvo em `_system/monitoring/reports/[data]`
- [ ] Logs confirmando execução: `adb logcat -s MonitoringManager:D`

---

## 🎯 PROTOCOLO DE EXECUÇÃO

### FASE 1: DIAGNÓSTICO INICIAL (15 minutos)
```bash
# 1. Verificar ambiente
./gradlew --version
java -version
firebase --version

# 2. Verificar estrutura do projeto
ls -R app/src/
ls -R functions/

# 3. Tentar build inicial
./gradlew :app:assembleDebug

# 4. Verificar testes existentes
./gradlew :app:testDebugUnitTest
```

**SAÍDA ESPERADA:**
- Build compilando ou erros mapeados
- Número de testes existentes (deve ser ~7)

---

### FASE 2: IMPLEMENTAÇÃO DOS TESTES (1-2 horas)

#### 2.1 Teste de Conversão Decimal (20 minutos)
```bash
# Criar arquivo
mkdir -p app/src/test/java/com/example/gestaobilhares/data
touch app/src/test/java/com/example/gestaobilhares/data/ValorDecimalConverterTest.kt

# Implementar código do teste (ver CRÍTICO 1)
# ...

# Rodar teste
./gradlew :app:testDebugUnitTest --tests ValorDecimalConverterTest

# CRITÉRIO: 4/4 testes passando
```

#### 2.2 Testes de ViewModels (40-60 minutos)
```bash
# AcertoViewModelTest
mkdir -p app/src/test/java/com/example/gestaobilhares/ui/settlement
touch app/src/test/java/com/example/gestaobilhares/ui/settlement/AcertoViewModelTest.kt

# Implementar (ver CRÍTICO 2.1)
# ...

# Rodar
./gradlew :app:testDebugUnitTest --tests AcertoViewModelTest

# CRITÉRIO: 5/5 testes passando

# ClienteViewModelTest
mkdir -p app/src/test/java/com/example/gestaobilhares/ui/routes
touch app/src/test/java/com/example/gestaobilhares/ui/routes/ClienteViewModelTest.kt

# Implementar (ver CRÍTICO 2.2)
# ...

# Rodar
./gradlew :app:testDebugUnitTest --tests ClienteViewModelTest

# CRITÉRIO: 3/3 testes passando
```

#### 2.3 Verificar Cobertura (10 minutos)
```bash
# Gerar relatório de cobertura
./gradlew :app:testDebugUnitTestCoverage

# Verificar cobertura em:
# app/build/reports/coverage/test/debug/index.html

# CRITÉRIO: >30% de cobertura global
```

**CHECKPOINT 1:**
- [ ] 12 testes implementados e passando (4 + 5 + 3)
- [ ] Build rodando sem erros
- [ ] Cobertura >30%

---

### FASE 3: BACKUP AUTOMÁTICO (1-2 horas)

#### 3.1 Setup Firebase Functions (30 minutos)
```bash
# Criar diretório
mkdir -p functions
cd functions

# Inicializar
npm init -y

# Instalar dependências
npm install firebase-admin firebase-functions @google-cloud/firestore @google-cloud/storage

# Criar index.js (ver CRÍTICO 3.1)
# ...

# Criar package.json (ver CRÍTICO 3.2)
# ...
```

#### 3.2 Deploy Functions (20 minutos)
```bash
# Login no Firebase
firebase login

# Selecionar projeto
firebase use gestaobilhares

# Deploy
firebase deploy --only functions

# CRITÉRIO: Functions deployadas sem erro
```

#### 3.3 Configurar Storage (15 minutos)
```bash
# Criar bucket via Firebase CLI
gsutil mb -p gestaobilhares gs://gestaobilhares-backups

# Ou via Console:
# https://console.cloud.google.com/storage/create-bucket

# CRITÉRIO: Bucket criado
```

#### 3.4 Testar Backup Manual (15 minutos)
```bash
# Trigger manual
firebase functions:shell
> backupFirestore()

# Verificar logs
firebase functions:log --only backupFirestore

# Verificar bucket
gsutil ls gs://gestaobilhares-backups/

# CRITÉRIO: Backup criado com sucesso
```

**CHECKPOINT 2:**
- [ ] Functions deployadas
- [ ] Backup manual funcionando
- [ ] Bucket criado e acessível
- [ ] Agendamento configurado (cron)

---

### FASE 4: TESTES DE SECURITY RULES (1 hora)

#### 4.1 Setup Ambiente de Teste (20 minutos)
```bash
# Criar diretório
mkdir -p tests
cd tests

# Inicializar
npm init -y

# Instalar dependências
npm install --save-dev @firebase/rules-unit-testing jest

# Criar jest.config.js
echo 'module.exports = { testEnvironment: "node" };' > jest.config.js

# Criar arquivo de teste (ver CRÍTICO 4)
# ...
```

#### 4.2 Rodar Testes (20 minutos)
```bash
# Instalar Java 21+ (se necessário)
# Ver: https://adoptium.net/

# Rodar testes
npm test

# CRITÉRIO: 8/8 testes passando
```

#### 4.3 CI Integration (20 minutos)
```bash
# Adicionar script no package.json do projeto raiz
echo '{ "scripts": { "test:rules": "cd tests && npm test" } }' > package.json

# Testar
npm run test:rules

# CRITÉRIO: Integrado ao CI (se existir)
```

**CHECKPOINT 3:**
- [ ] 8 testes de Security Rules passando
- [ ] Emulador funcionando
- [ ] Regras validadas

---

### FASE 5: MONITORAMENTO (1 hora)

#### 5.1 Implementar MonitoringManager (30 minutos)
```bash
# Criar arquivo
mkdir -p app/src/main/java/com/example/gestaobilhares/monitoring
touch app/src/main/java/com/example/gestaobilhares/monitoring/MonitoringManager.kt

# Implementar código (ver CRÍTICO 5.1)
# ...

# Compilar
./gradlew :app:assembleDebug

# CRITÉRIO: Compilação sem erros
```

#### 5.2 Implementar MonitoringWorker (20 minutos)
```bash
# Criar arquivo
touch app/src/main/java/com/example/gestaobilhares/monitoring/MonitoringWorker.kt

# Implementar código (ver CRÍTICO 5.2)
# ...

# Agendar no Application
# Adicionar em GestaoBilharesApplication.kt:
# MonitoringWorker.schedule(applicationContext)

# Compilar
./gradlew :app:assembleDebug

# CRITÉRIO: WorkManager agendado
```

#### 5.3 Executar Primeira Vez (10 minutos)
```bash
# Build e instalar
./gradlew :app:installDebug

# Verificar logs
adb logcat -s MonitoringManager:D MonitoringWorker:D

# Verificar Firestore
# _system/monitoring/reports/[data-atual]

# CRITÉRIO: Relatório gerado
```

**CHECKPOINT 4:**
- [ ] MonitoringManager implementado
- [ ] WorkManager agendado
- [ ] Primeira execução com sucesso
- [ ] Relatório salvo no Firestore

---

### FASE 6: VALIDAÇÃO FINAL (30 minutos)

#### 6.1 Build Completo
```bash
# Clean build
./gradlew clean

# Build debug
./gradlew :app:assembleDebug

# Build release
./gradlew :app:assembleRelease

# CRITÉRIO: Todos os builds passando
```

#### 6.2 Todos os Testes
```bash
# Testes unitários
./gradlew :app:testDebugUnitTest

# Testes de Security Rules
npm run test:rules

# CRITÉRIO: 20+ testes passando (12 unit + 8 rules)
```

#### 6.3 Verificar Infraestrutura
```bash
# Verificar functions
firebase functions:list

# Verificar backup
gsutil ls gs://gestaobilhares-backups/

# Verificar monitoramento
# Firestore: _system/monitoring/reports/

# CRITÉRIO: Tudo rodando
```

#### 6.4 Relatório Final
```bash
# Gerar relatório
echo "=== RELATÓRIO FINAL ===" > RESULTADO.md
echo "" >> RESULTADO.md
echo "✅ Build: SUCESSO" >> RESULTADO.md
echo "✅ Testes: $(./gradlew :app:testDebugUnitTest | grep "tests passed")" >> RESULTADO.md
echo "✅ Cobertura: $(grep -r "Total" app/build/reports/coverage/)" >> RESULTADO.md
echo "✅ Functions: $(firebase functions:list | wc -l) deployadas" >> RESULTADO.md
echo "✅ Backup: Configurado" >> RESULTADO.md
echo "✅ Security Rules: Testadas" >> RESULTADO.md
echo "✅ Monitoramento: Ativo" >> RESULTADO.md

cat RESULTADO.md
```

**CHECKPOINT FINAL:**
- [ ] ✅ App compilando (debug + release)
- [ ] ✅ 20+ testes passando
- [ ] ✅ Cobertura >30%
- [ ] ✅ 3 Firebase Functions deployadas
- [ ] ✅ Backup automático funcionando
- [ ] ✅ Security Rules testadas
- [ ] ✅ Monitoramento ativo
- [ ] ✅ Sem regressões conhecidas

---

## 🛑 REGRAS DE OURO

### 1. Metodologia Científica SEMPRE
- ❌ Nunca adivinhar
- ✅ Sempre diagnosticar antes de corrigir
- ✅ Usar Static Analysis primeiro
- ✅ Logs apenas quando necessário
- ✅ Máximo 2 builds por problema

### 2. Seguir Arquitetura do Projeto
- ✅ MVVM + Hilt + Room + Firebase
- ✅ Multi-tenancy por `rotasPermitidas`
- ✅ Offline-First (Room = fonte verdade)
- ❌ Não tocar em `AppRepository` (Facade)
- ❌ Não adicionar `empresaId` (usar rota)

### 3. Testes Primeiro
- ✅ Escrever teste antes de corrigir bug
- ✅ Validar com teste após correção
- ✅ Nunca commitar sem testes passando
- ✅ Meta: >30% cobertura inicial

### 4. Documentar Tudo
- ✅ Commits descritivos
- ✅ Logs claros e objetivos
- ✅ Comentários apenas onde necessário
- ✅ README atualizado

### 5. Segurança NUNCA É NEGOCIÁVEL
- ✅ Security Rules testadas
- ✅ Backup automático ativo
- ✅ Dados sensíveis nunca em logs
- ✅ Multi-tenancy funcionando

---

## 📊 CRITÉRIOS DE CONCLUSÃO

### ✅ MISSÃO COMPLETA quando:

1. **Build Funcionando**
   - [ ] `./gradlew :app:assembleDebug` → SUCCESS
   - [ ] `./gradlew :app:assembleRelease` → SUCCESS
   - [ ] Sem warnings críticos

2. **Testes Passando**
   - [ ] ValorDecimalConverterTest: 4/4 ✅
   - [ ] AcertoViewModelTest: 5/5 ✅
   - [ ] ClienteViewModelTest: 3/3 ✅
   - [ ] Security Rules Tests: 8/8 ✅
   - [ ] Total: 20+ testes ✅

3. **Cobertura Adequada**
   - [ ] Cobertura global >30%
   - [ ] ViewModels críticos >60%
   - [ ] Conversores >80%

4. **Infraestrutura Rodando**
   - [ ] 3 Firebase Functions deployadas
   - [ ] Backup diário agendado (3h AM)
   - [ ] Limpeza de backups agendada (4h AM)
   - [ ] Bucket criado e acessível

5. **Monitoramento Ativo**
   - [ ] MonitoringManager implementado
   - [ ] WorkManager agendado (semanal)
   - [ ] Primeira execução bem-sucedida
   - [ ] Relatório salvo no Firestore

6. **Sem Regressões**
   - [ ] Conversão decimal testada
   - [ ] Cálculos financeiros validados
   - [ ] Multi-tenancy funcionando
   - [ ] Sincronização sem erros

---

## 🚨 PROTOCOLO DE EMERGÊNCIA

### Se encontrar problema bloqueante:

1. **PARE IMEDIATAMENTE**
2. **Documente o problema:**
   ```markdown
   ## 🚨 PROBLEMA BLOQUEANTE

   **Sintoma:** [descrição exata]
   **Erro:** [mensagem de erro completa]
   **Contexto:** [o que estava fazendo]
   **Tentativas:** [o que já tentou]
   **Logs:** [logs relevantes]
   ```
3. **Tente Static Analysis:**
   ```bash
   rg "[termo relacionado]" --type kt -C 10
   ```
4. **Se não resolver em 30 minutos:** Documente e siga para próximo problema
5. **Volte depois** com análise mais profunda

### Se 3+ problemas bloqueantes:
- Gere relatório de progresso
- Destaque o que foi concluído
- Liste bloqueios pendentes
- **NÃO DESISTA** - Documente e continue

---

## 📝 TEMPLATE DE RELATÓRIO DE PROGRESSO

```markdown
# 📊 RELATÓRIO DE EXECUÇÃO AUTÔNOMA
## Data: [data/hora]

### ✅ CONCLUÍDO
- [ ] Problema 1: [descrição] - SUCESSO
- [ ] Problema 2: [descrição] - SUCESSO
- [ ] ...

### 🚧 EM ANDAMENTO
- [ ] Problema X: [descrição] - 70% completo
  - [detalhes do que falta]

### ❌ BLOQUEADO
- [ ] Problema Y: [descrição] - BLOQUEADO
  - **Motivo:** [razão específica]
  - **Tentativas:** [X tentativas]
  - **Solução proposta:** [próximos passos]

### 📈 MÉTRICAS
- **Testes criados:** X
- **Testes passando:** Y
- **Cobertura:** Z%
- **Build:** SUCESSO/FALHA
- **Functions:** X/3 deployadas
- **Tempo decorrido:** X horas

### 🎯 PRÓXIMOS PASSOS
1. [ação 1]
2. [ação 2]
3. ...
```

---

## 🎓 MINDSET DE DESENVOLVEDOR SÊNIOR

### ✅ FAÇA:
- Diagnostique antes de corrigir
- Leia o código primeiro (Static Analysis)
- Escreva testes que provam o problema
- Corrija cirurgicamente
- Valide com testes
- Documente decisões

### ❌ NÃO FAÇA:
- Tentativa e erro
- Múltiplos builds sem diagnóstico
- Correções sem testes
- Mudanças arquiteturais sem necessidade
- Ignorar warnings
- Código sem documentação

---

## 🏁 INÍCIO DA EXECUÇÃO

**VOCÊ ESTÁ PRONTO PARA COMEÇAR.**

**Sua primeira ação deve ser:**
```bash
# 1. Verificar ambiente
./gradlew --version

# 2. Tentar build inicial
./gradlew :app:assembleDebug

# 3. Verificar testes existentes
./gradlew :app:testDebugUnitTest

# 4. Começar pela FASE 1: DIAGNÓSTICO INICIAL
```

**LEMBRE-SE:**
- Não pare até conclusão completa
- Documente tudo
- Testes primeiro
- Segurança sempre
- Qualidade > Velocidade

**BOA SORTE, DESENVOLVEDOR SÊNIOR! 🚀**

---

*Prompt gerado em: 27/01/2026*  
*Versão: 1.0*  
*Projeto: Gestão de Bilhares - Android Kotlin + Firebase*
