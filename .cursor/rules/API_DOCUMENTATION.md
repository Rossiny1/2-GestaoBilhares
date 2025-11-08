# Documentação de APIs e Endpoints

## 📚 Visão Geral

Este documento descreve as principais APIs e endpoints do sistema Gestão Bilhares, incluindo métodos do Repository, sincronização e operações de banco de dados.

## 🗄️ AppRepository

O `AppRepository` é o único ponto de acesso aos dados do sistema. Todos os métodos de leitura/escrita passam por ele.

### Estrutura

```kotlin
class AppRepository(
    private val clienteDao: ClienteDao,
    private val mesaDao: MesaDao,
    private val rotaDao: RotaDao,
    private val acertoDao: AcertoDao,
    // ... outros DAOs
)
```

### Operações CRUD Principais

#### Cliente

```kotlin
// Inserir
suspend fun inserirCliente(cliente: Cliente): Long

// Atualizar
suspend fun atualizarCliente(cliente: Cliente)

// Buscar
suspend fun obterClientePorId(id: Long): Cliente?
fun obterClientesPorRota(rotaId: Long): Flow<List<Cliente>>
fun obterTodosClientes(): Flow<List<Cliente>>

// Deletar
suspend fun deletarCliente(cliente: Cliente)
```

#### Mesa

```kotlin
// Inserir
suspend fun inserirMesa(mesa: Mesa): Long

// Atualizar
suspend fun atualizarMesa(mesa: Mesa)
suspend fun atualizarRelogioMesa(mesaId: Long, relogioInicial: Int, relogioFinal: Int)

// Buscar
suspend fun obterMesaPorId(id: Long): Mesa?
fun buscarMesasPorRota(rotaId: Long): Flow<List<Mesa>>
fun obterTodasMesas(): Flow<List<Mesa>>

// Vincular
suspend fun vincularMesaACliente(mesaId: Long, clienteId: Long)
suspend fun desvincularMesaDeCliente(mesaId: Long)
```

#### Rota

```kotlin
// Inserir
suspend fun inserirRota(rota: Rota): Long

// Atualizar
suspend fun atualizarRota(rota: Rota)
suspend fun atualizarStatusRota(rotaId: Long, status: StatusRota)

// Buscar
fun obterTodasRotas(): Flow<List<Rota>>
fun obterRotasAtivas(): Flow<List<Rota>>
fun getRotasResumoComAtualizacaoTempoReal(): Flow<List<RotaResumo>>

// Deletar
suspend fun deletarRota(rota: Rota)
```

#### Acerto

```kotlin
// Inserir
suspend fun inserirAcerto(acerto: Acerto): Long
suspend fun inserirAcertoComMesas(acerto: Acerto, mesas: List<AcertoMesa>): Long

// Atualizar
suspend fun atualizarAcerto(acerto: Acerto)

// Buscar
suspend fun obterAcertoPorId(id: Long): Acerto?
fun obterAcertosPorCliente(clienteId: Long): Flow<List<Acerto>>
fun obterAcertosPorRota(rotaId: Long): Flow<List<Acerto>>

// Deletar
suspend fun deletarAcerto(acerto: Acerto)
```

#### ContratoLocacao

```kotlin
// Inserir
suspend fun inserirContrato(contrato: ContratoLocacao): Long

// Atualizar
suspend fun atualizarContrato(contrato: ContratoLocacao)

// Buscar
suspend fun obterContratoPorId(id: Long): ContratoLocacao?
suspend fun buscarContratoAtivoPorCliente(clienteId: Long): ContratoLocacao?
fun obterContratosPorCliente(clienteId: Long): Flow<List<ContratoLocacao>>

// Assinatura
suspend fun salvarAssinaturaLocatario(contratoId: Long, assinaturaBase64: String, metadados: SignatureMetadata)
```

### Métodos de Cache

```kotlin
// Rotas com cache (TTL: 2 minutos)
fun buscarRotasComCache(): Flow<List<Rota>>

// Clientes por rota com cache (TTL: 1 minuto)
fun buscarClientesPorRotaComCache(rotaId: Long): Flow<List<Cliente>>
```

### Métodos de Sincronização

```kotlin
// Adicionar operação à fila de sincronização
suspend fun adicionarOperacaoSync(
    entityType: String,
    entityId: Long,
    operation: String,
    payload: String,
    priority: Int = 0
)

// Logar operação de sincronização
suspend fun logarOperacaoSync(
    entityType: String,
    entityId: Long,
    operation: String,
    status: String,
    errorMessage: String? = null,
    payload: String? = null
)
```

## 🔄 SyncManagerV2

O `SyncManagerV2` gerencia a sincronização bidirecional entre o app e o Firestore.

### Estrutura de Sincronização

```
Firestore Structure:
/empresas/{empresaId}/
  ├── rotas/
  ├── clientes/
  ├── mesas/
  ├── acertos/
  ├── contratos/
  └── ... (27 entidades)
```

### Métodos Principais

#### Sincronização Push (App → Firestore)

```kotlin
// Sincronizar todas as operações pendentes
suspend fun syncPush(): SyncResult

// Sincronizar entidade específica
suspend fun syncPushEntity(entityType: String, entityId: Long): Boolean

// Sincronizar operação específica
suspend fun syncPushOperation(operation: SyncQueue): Boolean
```

#### Sincronização Pull (Firestore → App)

```kotlin
// Importar todos os dados do Firestore
suspend fun syncPull(): SyncResult

// Importar entidade específica
suspend fun syncPullEntity(entityType: String): Int

// Forçar atualização de rotas
suspend fun forcarAtualizacaoRotas()
```

#### Métodos por Entidade

```kotlin
// Rotas
suspend fun syncPushRota(rota: Rota): Boolean
suspend fun syncPullRotas(): Int

// Clientes
suspend fun syncPushCliente(cliente: Cliente): Boolean
suspend fun syncPullClientes(): Int

// Mesas
suspend fun syncPushMesa(mesa: Mesa): Boolean
suspend fun syncPullMesas(): Int

// Acertos
suspend fun syncPushAcerto(acerto: Acerto): Boolean
suspend fun syncPullAcertos(): Int

// Contratos
suspend fun syncPushContrato(contrato: ContratoLocacao): Boolean
suspend fun syncPullContratos(): Int
```

### Resolução de Conflitos

O sistema usa **timestamp** para resolver conflitos:
- Dados mais recentes (maior timestamp) vencem
- Implementado automaticamente em todas as operações

## 🔐 Segurança e Criptografia

### Métodos de Criptografia

```kotlin
// Criptografar dados sensíveis
private fun encryptCliente(cliente: Cliente): Cliente
private fun encryptContratoLocacao(contrato: ContratoLocacao): ContratoLocacao
private fun encryptColaborador(colaborador: Colaborador): Colaborador

// Descriptografar dados sensíveis
private fun decryptCliente(cliente: Cliente?): Cliente?
private fun decryptContratoLocacao(contrato: ContratoLocacao?): ContratoLocacao?
private fun decryptColaborador(colaborador: Colaborador?): Colaborador?
```

### Dados Criptografados

- **CPF/CNPJ**: Em Cliente, Colaborador, MesaVendida
- **Senhas**: Hash PBKDF2 (não armazenadas em texto)
- **Assinaturas**: Base64 criptografado
- **Tokens**: Criptografados no Android Keystore

## 📊 Operações de Relatório

### Geração de PDFs

```kotlin
// Contrato
fun generateContractPdf(
    contrato: ContratoLocacao,
    mesas: List<Mesa>,
    assinaturaRepresentante: String? = null
): Pair<File, String?> // Retorna arquivo e hash

// Distrato
fun generateDistratoPdf(
    contrato: ContratoLocacao,
    mesas: List<Mesa>,
    fechamento: FechamentoResumo,
    confissaoDivida: Pair<Double, Date?>? = null,
    assinaturaRepresentante: String? = null
): File

// Relatório de Fechamento
fun generateClosureReport(
    cicloId: Long,
    rotaId: Long
): File
```

### Cálculos Financeiros

```kotlin
// Calcular débito atual do cliente
suspend fun calcularDebitoAtualCliente(clienteId: Long): Double

// Calcular valor acertado por rota
suspend fun calcularValorAcertadoPorRotaECiclo(rotaId: Long, cicloId: Long): Double

// Calcular percentual de clientes acertados
suspend fun calcularPercentualClientesAcertados(
    rotaId: Long,
    cicloId: Long,
    totalClientes: Int
): Int
```

## 🔍 Queries Otimizadas

### Queries com Índices

```kotlin
// Buscar clientes por rota (índice: idx_cliente_rota)
fun obterClientesPorRota(rotaId: Long): Flow<List<Cliente>>

// Buscar mesas por cliente (índice: idx_mesa_cliente)
fun obterMesasPorCliente(clienteId: Long): Flow<List<Mesa>>

// Buscar acertos por ciclo (índice: idx_acerto_ciclo)
fun obterAcertosPorCiclo(cicloId: Long): Flow<List<Acerto>>
```

### Queries com Range

```kotlin
// Buscar despesas por período
fun buscarDespesasPorPeriodo(
    dataInicio: Long,
    dataFim: Long
): Flow<List<Despesa>>

// Buscar histórico por ano
fun buscarHistoricoPorAno(ano: String): Flow<List<Historico>>
```

## 📱 WorkManager

### Workers de Background

```kotlin
// SyncWorker: Sincronização automática
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params)

// CleanupWorker: Limpeza de dados antigos
class CleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params)
```

### Agendamento

```kotlin
// Sincronização periódica (a cada 15 minutos)
val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
    .setConstraints(Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build())
    .build()

WorkManager.getInstance(context).enqueue(syncRequest)
```

## 🎯 Endpoints Firestore

### Estrutura de Coleções

```
/empresas/{empresaId}/
  ├── rotas/{rotaId}
  ├── clientes/{clienteId}
  ├── mesas/{mesaId}
  ├── acertos/{acertoId}
  ├── contratos/{contratoId}
  ├── ciclos/{cicloId}
  ├── despesas/{despesaId}
  └── ... (27 entidades)
```

### Regras de Segurança

```javascript
// Exemplo de regras (configurar no Firebase Console)
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /empresas/{empresaId}/{document=**} {
      allow read, write: if request.auth != null 
        && request.auth.uid != null;
    }
  }
}
```

## 📝 Notas Importantes

### Performance

- Use **Flow** para observação reativa
- Use **cache** para dados frequentemente acessados
- Use **índices** para queries complexas
- Use **transações** para operações atômicas

### Segurança

- Dados sensíveis são **criptografados automaticamente**
- Logs são **sanitizados** em produção
- Senhas nunca são armazenadas em texto

### Sincronização

- Sincronização é **automática** em background
- Conflitos são resolvidos por **timestamp**
- Dados funcionam **100% offline**

## 🔗 Referências

- [Room Database](https://developer.android.com/training/data-storage/room)
- [Firebase Firestore](https://firebase.google.com/docs/firestore)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [StateFlow](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/)

---

**Última atualização**: 2025-01-08

