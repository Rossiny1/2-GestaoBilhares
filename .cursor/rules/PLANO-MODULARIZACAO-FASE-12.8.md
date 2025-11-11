# PLANO DE MODULARIZAÇÃO - FASE 12.8

> **Documento de Referência** - Plano completo e detalhado para modularização do projeto Gestão Bilhares em 4 módulos: `core`, `data`, `ui`, `sync`.

## 📋 ÍNDICE

1. [Visão Geral](#visão-geral)
2. [Estrutura dos Módulos](#estrutura-dos-módulos)
3. [Mapeamento de Pacotes](#mapeamento-de-pacotes)
4. [Plano de Migração Passo a Passo](#plano-de-migração-passo-a-passo)
5. [Dependências entre Módulos](#dependências-entre-módulos)
6. [Checklist de Migração](#checklist-de-migração)
7. [Riscos e Considerações](#riscos-e-considerações)
8. [Ordem de Execução Recomendada](#ordem-de-execução-recomendada)

---

## 🎯 VISÃO GERAL

### Objetivo
Dividir o projeto monolítico em módulos independentes para:
- ✅ Melhorar manutenibilidade
- ✅ Facilitar testes unitários
- ✅ Reduzir acoplamento
- ✅ Aumentar reutilização de código
- ✅ Otimizar builds incrementais

### Módulos Criados

```
GestaoBilhares/
├── app/          # Módulo principal (Application, MainActivity, Navigation)
├── core/         # Utilitários, entidades, interfaces
├── data/         # Repositories, DAOs, database
├── ui/           # Fragments, ViewModels, adapters
└── sync/         # Sincronização e workers
```

### Hierarquia de Dependências

```
app
 ├── core (sem dependências)
 ├── data (depende de core)
 ├── ui (depende de core + data)
 └── sync (depende de core + data)
```

---

## 📦 ESTRUTURA DOS MÓDULOS

### 1. Módulo `:core`
**Responsabilidade**: Código compartilhado sem dependências Android específicas (ou mínimas)

**Conteúdo**:
- ✅ Utilitários (`utils/`)
- ✅ Entidades (`data/entities/`)
- ✅ Interfaces e contratos
- ✅ Modelos de dados (`data/model/`)
- ✅ Constantes e enums
- ✅ Helpers de validação

**Dependências**:
- AndroidX Core KTX
- Kotlin Coroutines Core
- Gson
- DataStore Preferences
- Timber (Logging)

**Namespace**: `com.example.gestaobilhares.core`

---

### 2. Módulo `:data`
**Responsabilidade**: Camada de acesso a dados

**Conteúdo**:
- ✅ DAOs (`data/dao/`)
- ✅ Database (`data/database/`)
- ✅ Repositories (`data/repository/`)
- ✅ Factory (`data/factory/`)
- ✅ Otimizações de database (`database/`)

**Dependências**:
- Módulo `:core`
- Room (Runtime + KTX + Compiler)
- Firebase Firestore
- Firebase Storage
- Kotlin Coroutines

**Namespace**: `com.example.gestaobilhares.data`

---

### 3. Módulo `:ui`
**Responsabilidade**: Interface do usuário e lógica de apresentação

**Conteúdo**:
- ✅ Fragments (`ui/*/`)
- ✅ ViewModels (`ui/*/`)
- ✅ Adapters (`ui/*/`)
- ✅ Components (`ui/components/`)
- ✅ Dialogs (`ui/dialogs/`)
- ✅ Theme (`ui/theme/`)
- ✅ Utilitários de UI (PDF generators, Chart generators)

**Dependências**:
- Módulo `:core`
- Módulo `:data`
- AndroidX UI (Material, Navigation, RecyclerView, etc.)
- ViewModel e Lifecycle
- iTextPDF
- MPAndroidChart
- Location Services

**Namespace**: `com.example.gestaobilhares.ui`

---

### 4. Módulo `:sync`
**Responsabilidade**: Sincronização bidirecional e processamento em background

**Conteúdo**:
- ✅ SyncManagerV2 (`sync/`)
- ✅ Handlers de sincronização (`sync/handlers/`)
- ✅ Workers (`workers/`)
- ✅ Lógica de sincronização

**Dependências**:
- Módulo `:core`
- Módulo `:data`
- WorkManager
- Firebase Firestore
- Firebase Storage
- Kotlin Coroutines

**Namespace**: `com.example.gestaobilhares.sync`

---

## 🗺️ MAPEAMENTO DE PACOTES

### Pacotes Atuais → Novos Módulos

#### `:core` (Sem dependências Android pesadas)

```
app/src/main/java/com/example/gestaobilhares/
├── utils/                          → core/src/main/java/.../core/utils/
│   ├── AppLogger.kt
│   ├── DataValidator.kt
│   ├── DateUtils.kt
│   ├── StringUtils.kt
│   ├── PasswordHasher.kt
│   ├── DataEncryption.kt
│   ├── NetworkUtils.kt
│   ├── PaginationManager.kt
│   ├── UserSessionManager.kt
│   ├── SignatureMetadataCollector.kt
│   ├── SignatureStatistics.kt
│   ├── DocumentIntegrityManager.kt
│   ├── LegalLogger.kt
│   └── FinancialCalculator.kt
│
├── data/entities/                  → core/src/main/java/.../core/entities/
│   ├── Acerto.kt
│   ├── AcertoMesa.kt
│   ├── Cliente.kt
│   ├── Colaborador.kt
│   ├── ContratoLocacao.kt
│   ├── Despesa.kt
│   ├── Mesa.kt
│   ├── Rota.kt
│   ├── Veiculo.kt
│   └── ... (todas as entidades)
│
└── data/model/                     → core/src/main/java/.../core/model/
    └── EstadosCidades.kt
```

#### `:data` (Camada de dados)

```
app/src/main/java/com/example/gestaobilhares/
├── data/dao/                       → data/src/main/java/.../data/dao/
│   ├── AcertoDao.kt
│   ├── ClienteDao.kt
│   ├── MesaDao.kt
│   └── ... (todos os DAOs)
│
├── data/database/                  → data/src/main/java/.../data/database/
│   ├── AppDatabase.kt
│   └── Converters.kt
│
├── data/repository/                → data/src/main/java/.../data/repository/
│   ├── AppRepository.kt
│   ├── internal/
│   │   ├── ContratoRepositoryInternal.kt
│   │   ├── VeiculoRepositoryInternal.kt
│   │   └── ... (todos os repositories internos)
│   └── ... (outros repositories)
│
├── data/factory/                   → data/src/main/java/.../data/factory/
│   └── RepositoryFactory.kt
│
└── database/                       → data/src/main/java/.../data/database/
    ├── DatabaseConnectionPool.kt
    ├── DatabasePerformanceTuner.kt
    ├── QueryOptimizationManager.kt
    └── TransactionOptimizationManager.kt
```

#### `:ui` (Interface do usuário)

```
app/src/main/java/com/example/gestaobilhares/
├── ui/                             → ui/src/main/java/.../ui/
│   ├── auth/
│   ├── clients/
│   ├── routes/
│   ├── settlement/
│   ├── contracts/
│   ├── mesas/
│   ├── expenses/
│   ├── inventory/
│   ├── metas/
│   ├── cycles/
│   ├── dashboard/
│   ├── reports/
│   ├── dialogs/
│   ├── components/
│   ├── common/
│   └── theme/
│
├── utils/                          → ui/src/main/java/.../ui/utils/
│   ├── ContractPdfGenerator.kt
│   ├── PdfReportGenerator.kt
│   ├── AditivoPdfGenerator.kt
│   ├── ClosureReportPdfGenerator.kt
│   ├── ChartGenerator.kt
│   ├── AuditReportGenerator.kt
│   ├── BluetoothPrinterHelper.kt
│   ├── ReciboPrinterHelper.kt
│   ├── ImageCompressionUtils.kt
│   ├── MoneyTextWatcher.kt
│   └── ApkSizeAnalyzer.kt
│
└── cache/                          → ui/src/main/java/.../ui/cache/
    └── AppCacheManager.kt
```

#### `:sync` (Sincronização)

```
app/src/main/java/com/example/gestaobilhares/
├── sync/                           → sync/src/main/java/.../sync/
│   ├── SyncManagerV2.kt
│   └── handlers/
│       ├── BasePullHandler.kt
│       ├── AcertoPullHandler.kt
│       ├── ClientePullHandler.kt
│       ├── MesaPullHandler.kt
│       ├── RotaPullHandler.kt
│       └── CicloPullHandler.kt
│
└── workers/                        → sync/src/main/java/.../sync/workers/
    ├── SyncWorker.kt
    └── CleanupWorker.kt
```

#### `:app` (Módulo principal - permanece)

```
app/src/main/java/com/example/gestaobilhares/
├── GestaoBilharesApplication.kt    → Permanece no app
├── MainActivity.kt                 → Permanece no app
├── notification/                   → Permanece no app (ou move para :ui)
│   └── NotificationService.kt
├── memory/                         → Move para :core ou :data
│   ├── MemoryOptimizer.kt
│   ├── ObjectPool.kt
│   └── WeakReferenceManager.kt
└── network/                        → Move para :core ou :sync
    ├── BatchOperationsManager.kt
    ├── NetworkCacheManager.kt
    ├── NetworkCompressionManager.kt
    └── RetryLogicManager.kt
```

---

## 📝 PLANO DE MIGRAÇÃO PASSO A PASSO

### ETAPA 1: Preparação e Validação ✅ (CONCLUÍDA)

- [x] Criar estrutura de diretórios dos módulos
- [x] Criar `build.gradle.kts` para cada módulo
- [x] Atualizar `settings.gradle.kts`
- [x] Atualizar `app/build.gradle.kts` (dependências restauradas, módulos comentados)
- [x] Criar `AndroidManifest.xml` para cada módulo
- [x] **VALIDAR BUILD**: Build executado com sucesso ✅

---

### ETAPA 2: Migração do Módulo `:core` (Prioridade ALTA)

**Ordem de migração**:

1. **Entidades** (`data/entities/` → `core/entities/`)
   - Migrar todas as entidades Room
   - Atualizar imports de `com.example.gestaobilhares.data.entities` para `com.example.gestaobilhares.core.entities`
   - **Atenção**: Entidades Room precisam estar no módulo `:data`, não `:core`!
   - **CORREÇÃO**: Entidades ficam em `:data`, não `:core`

2. **Modelos** (`data/model/` → `core/model/`)
   - Migrar `EstadosCidades.kt`
   - Atualizar imports

3. **Utilitários Básicos** (`utils/` → `core/utils/`)
   - Migrar utilitários sem dependências Android pesadas:
     - `AppLogger.kt`
     - `DataValidator.kt`
     - `DateUtils.kt`
     - `StringUtils.kt`
     - `PasswordHasher.kt`
     - `DataEncryption.kt`
     - `NetworkUtils.kt`
     - `PaginationManager.kt`
     - `UserSessionManager.kt`
     - `SignatureMetadataCollector.kt`
     - `SignatureStatistics.kt`
     - `DocumentIntegrityManager.kt`
     - `LegalLogger.kt`
     - `FinancialCalculator.kt`

4. **Atualizar imports em todo o projeto**
   - Buscar e substituir imports antigos por novos
   - Validar compilação após cada substituição

**Checklist**:
- [ ] Migrar modelos de dados
- [ ] Migrar utilitários básicos
- [ ] Atualizar todos os imports
- [ ] Validar build do módulo `:core`
- [ ] Testar funcionalidades básicas

---

### ETAPA 3: Migração do Módulo `:data` (Prioridade ALTA)

**Ordem de migração**:

1. **Entidades** (permanecem em `:data`)
   - Manter todas as entidades Room em `data/entities/`
   - Atualizar namespace para `com.example.gestaobilhares.data.entities`

2. **DAOs** (`data/dao/` → `data/dao/`)
   - Migrar todos os DAOs
   - Atualizar imports de entidades para `com.example.gestaobilhares.data.entities`

3. **Database** (`data/database/` → `data/database/`)
   - Migrar `AppDatabase.kt`
   - Migrar `Converters.kt`
   - Atualizar referências a entidades e DAOs

4. **Repositories** (`data/repository/` → `data/repository/`)
   - Migrar `AppRepository.kt`
   - Migrar todos os repositories internos
   - Atualizar imports de utilitários para `com.example.gestaobilhares.core.utils`
   - Atualizar imports de entidades para `com.example.gestaobilhares.data.entities`

5. **Factory** (`data/factory/` → `data/factory/`)
   - Migrar `RepositoryFactory.kt`
   - Atualizar imports

6. **Otimizações de Database** (`database/` → `data/database/optimization/`)
   - Migrar otimizadores de database
   - Atualizar imports

**Checklist**:
- [ ] Migrar entidades Room
- [ ] Migrar DAOs
- [ ] Migrar Database e Converters
- [ ] Migrar Repositories
- [ ] Migrar Factory
- [ ] Migrar otimizações de database
- [ ] Atualizar todos os imports
- [ ] Validar build do módulo `:data`
- [ ] Testar operações de database

---

### ETAPA 4: Migração do Módulo `:sync` (Prioridade MÉDIA)

**Ordem de migração**:

1. **SyncManagerV2** (`sync/` → `sync/`)
   - Migrar `SyncManagerV2.kt`
   - Atualizar imports de repositories para `com.example.gestaobilhares.data.repository`
   - Atualizar imports de entidades para `com.example.gestaobilhares.data.entities`

2. **Handlers** (`sync/handlers/` → `sync/handlers/`)
   - Migrar todos os handlers de sincronização
   - Atualizar imports

3. **Workers** (`workers/` → `sync/workers/`)
   - Migrar `SyncWorker.kt`
   - Migrar `CleanupWorker.kt`
   - Atualizar imports

**Checklist**:
- [ ] Migrar SyncManagerV2
- [ ] Migrar handlers de sincronização
- [ ] Migrar workers
- [ ] Atualizar todos os imports
- [ ] Validar build do módulo `:sync`
- [ ] Testar sincronização

---

### ETAPA 5: Migração do Módulo `:ui` (Prioridade MÉDIA)

**Ordem de migração**:

1. **Utilitários de UI** (`utils/` → `ui/utils/`)
   - Migrar geradores de PDF
   - Migrar helpers de impressão
   - Migrar compressão de imagens
   - Migrar outros utilitários de UI

2. **Common** (`ui/common/` → `ui/common/`)
   - Migrar `BaseViewModel.kt`
   - Migrar `SignatureView.kt`
   - Atualizar imports

3. **Components** (`ui/components/` → `ui/components/`)
   - Migrar componentes reutilizáveis
   - Atualizar imports

4. **Fragments e ViewModels** (`ui/*/` → `ui/*/`)
   - Migrar por funcionalidade (auth, clients, routes, etc.)
   - Atualizar imports de repositories para `com.example.gestaobilhares.data.repository`
   - Atualizar imports de entidades para `com.example.gestaobilhares.data.entities`
   - Atualizar imports de utilitários para `com.example.gestaobilhares.core.utils` ou `com.example.gestaobilhares.ui.utils`

5. **Dialogs** (`ui/dialogs/` → `ui/dialogs/`)
   - Migrar todos os dialogs
   - Atualizar imports

6. **Theme** (`ui/theme/` → `ui/theme/`)
   - Migrar recursos de tema
   - Atualizar referências

**Checklist**:
- [ ] Migrar utilitários de UI
- [ ] Migrar common
- [ ] Migrar components
- [ ] Migrar fragments e ViewModels (por funcionalidade)
- [ ] Migrar dialogs
- [ ] Migrar theme
- [ ] Atualizar todos os imports
- [ ] Validar build do módulo `:ui`
- [ ] Testar navegação e telas

---

### ETAPA 6: Limpeza e Ajustes Finais (Prioridade BAIXA)

1. **Módulo `:app`**
   - Manter apenas `GestaoBilharesApplication.kt` e `MainActivity.kt`
   - Mover `notification/` para `:ui` (opcional)
   - Mover `memory/` para `:core` ou `:data` (opcional)
   - Mover `network/` para `:core` ou `:sync` (opcional)

2. **Atualizar Navigation**
   - Verificar referências de fragments no `nav_graph.xml`
   - Atualizar IDs de recursos se necessário

3. **Atualizar Resources**
   - Mover recursos compartilhados para módulos apropriados
   - Atualizar referências R.id, R.string, etc.

4. **Testes**
   - Executar todos os testes unitários
   - Executar testes de instrumentação
   - Validar funcionalidades principais

**Checklist**:
- [ ] Limpar módulo `:app`
- [ ] Atualizar navigation
- [ ] Atualizar resources
- [ ] Executar testes
- [ ] Validar build completo
- [ ] Testar app completo

---

## 🔗 DEPENDÊNCIAS ENTRE MÓDULOS

### Diagrama de Dependências

```
┌─────────┐
│   app   │
└────┬────┘
     │
     ├──► :core (sem dependências)
     │
     ├──► :data ──► :core
     │
     ├──► :ui ──► :core
     │     └──► :data
     │
     └──► :sync ──► :core
           └──► :data
```

### Regras de Dependência

1. **`:core`**: Nenhuma dependência de outros módulos do projeto
2. **`:data`**: Pode depender apenas de `:core`
3. **`:ui`**: Pode depender de `:core` e `:data`
4. **`:sync`**: Pode depender de `:core` e `:data`
5. **`:app`**: Pode depender de todos os módulos

### Dependências Externas por Módulo

#### `:core`
- AndroidX Core KTX
- Kotlin Coroutines Core
- Gson
- DataStore Preferences
- Timber

#### `:data`
- Todas as dependências de `:core`
- Room (Runtime + KTX + Compiler)
- Firebase Firestore
- Firebase Storage
- Kotlin Coroutines Play Services

#### `:ui`
- Todas as dependências de `:core` e `:data`
- AndroidX UI (Material, Navigation, RecyclerView, etc.)
- ViewModel e Lifecycle
- iTextPDF
- MPAndroidChart
- Location Services

#### `:sync`
- Todas as dependências de `:core` e `:data`
- WorkManager
- Firebase Firestore
- Firebase Storage

---

## ✅ CHECKLIST DE MIGRAÇÃO

### Fase 1: Preparação ✅
- [x] Estrutura de módulos criada
- [x] Build files configurados
- [x] Settings.gradle.kts atualizado
- [ ] **Build de validação executado**

### Fase 2: Módulo `:core`
- [ ] Modelos migrados
- [ ] Utilitários básicos migrados
- [ ] Imports atualizados
- [ ] Build validado
- [ ] Testes executados

### Fase 3: Módulo `:data`
- [ ] Entidades migradas
- [ ] DAOs migrados
- [ ] Database migrado
- [ ] Repositories migrados
- [ ] Factory migrado
- [ ] Imports atualizados
- [ ] Build validado
- [ ] Testes executados

### Fase 4: Módulo `:sync`
- [ ] SyncManagerV2 migrado
- [ ] Handlers migrados
- [ ] Workers migrados
- [ ] Imports atualizados
- [ ] Build validado
- [ ] Testes executados

### Fase 5: Módulo `:ui`
- [ ] Utilitários de UI migrados
- [ ] Common migrado
- [ ] Components migrados
- [ ] Fragments migrados
- [ ] ViewModels migrados
- [ ] Dialogs migrados
- [ ] Theme migrado
- [ ] Imports atualizados
- [ ] Build validado
- [ ] Testes executados

### Fase 6: Limpeza Final
- [ ] Módulo `:app` limpo
- [ ] Navigation atualizado
- [ ] Resources atualizados
- [ ] Todos os testes passando
- [ ] Build completo validado
- [ ] App testado manualmente

---

## ⚠️ RISCOS E CONSIDERAÇÕES

### Riscos Identificados

1. **Quebra de Imports**
   - **Risco**: Alto
   - **Mitigação**: Atualizar imports gradualmente, validar build após cada etapa

2. **Dependências Circulares**
   - **Risco**: Médio
   - **Mitigação**: Seguir rigorosamente a hierarquia de dependências

3. **Resources Compartilhados**
   - **Risco**: Médio
   - **Mitigação**: Mover resources para módulos apropriados ou manter no `:app`

4. **Room Schema Location**
   - **Risco**: Baixo
   - **Mitigação**: Configurar `room.schemaLocation` no módulo `:data`

5. **Navigation Graph**
   - **Risco**: Médio
   - **Mitigação**: Atualizar referências de fragments após migração do `:ui`

6. **Build Time**
   - **Risco**: Baixo (pode até melhorar)
   - **Mitigação**: Builds incrementais devem ser mais rápidos após modularização

### Considerações Importantes

1. **Entidades Room**: Devem permanecer no módulo `:data`, não em `:core`, pois dependem de anotações Room
2. **Resources**: Recursos compartilhados podem precisar ser duplicados ou movidos para `:app`
3. **Namespace**: Manter consistência nos namespaces para facilitar migração
4. **Testes**: Atualizar caminhos de testes após migração
5. **ProGuard**: Atualizar regras ProGuard se necessário

---

## 🚀 ORDEM DE EXECUÇÃO RECOMENDADA

### Abordagem Incremental (Recomendada)

1. **Validação Inicial** (1 dia)
   - Executar build para validar estrutura
   - Corrigir problemas de configuração

2. **Migração `:core`** (2-3 dias)
   - Migrar modelos e utilitários
   - Validar build e testes

3. **Migração `:data`** (3-4 dias)
   - Migrar entidades, DAOs, database, repositories
   - Validar build e testes
   - **CRÍTICO**: Testar operações de database

4. **Migração `:sync`** (2-3 dias)
   - Migrar SyncManagerV2, handlers, workers
   - Validar build e testes
   - Testar sincronização

5. **Migração `:ui`** (4-5 dias)
   - Migrar por funcionalidade (auth → clients → routes → etc.)
   - Validar build após cada funcionalidade
   - Testar navegação

6. **Limpeza Final** (1-2 dias)
   - Limpar módulo `:app`
   - Atualizar navigation e resources
   - Testes finais

**Tempo Total Estimado**: 2-3 semanas

### Abordagem por Funcionalidade (Alternativa)

Migrar uma funcionalidade completa por vez (ex: auth com core, data, ui, sync relacionados).

**Vantagem**: Funcionalidades completas funcionando
**Desvantagem**: Mais complexo, pode criar dependências temporárias

---

## 📚 REFERÊNCIAS

- [Android Modularization Guide](https://developer.android.com/topic/modularization)
- [Gradle Multi-Project Builds](https://docs.gradle.org/current/userguide/multi_project_builds.html)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Navigation Component](https://developer.android.com/guide/navigation)

---

## 📝 NOTAS DE IMPLEMENTAÇÃO

### Correções Importantes

1. **Entidades Room**: Inicialmente planejado para `:core`, mas devem ficar em `:data` devido a dependências de Room
2. **Resources**: Alguns resources podem precisar ser duplicados ou mantidos no `:app`
3. **Namespace**: Usar `com.example.gestaobilhares.{module}` para cada módulo

### Comandos Úteis

```bash
# Validar build de um módulo específico
./gradlew :core:assembleDebug
./gradlew :data:assembleDebug
./gradlew :ui:assembleDebug
./gradlew :sync:assembleDebug

# Build completo
./gradlew assembleDebug

# Limpar e rebuild
./gradlew clean assembleDebug

# Ver dependências
./gradlew :app:dependencies
```

---

**Última Atualização**: 2025-11-10
**Status**: ✅ Estrutura criada e build validado
**Próximo Passo**: Iniciar migração do módulo `:core` (Etapa 2)

