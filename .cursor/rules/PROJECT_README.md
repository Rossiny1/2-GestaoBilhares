# 📱 GESTÃO DE BILHARES — DOCUMENTAÇÃO DO PROJETO

> App Android nativo para gestão de negócio de aluguel de mesas de bilhar.
>
> **Versão:** 3.0 (Nota 10/10) 🎯  
> **Atualizado em:** 28/01/2026  
> **Arquitetura:** MVVM + Hilt + Room + Firebase (Firestore + Auth)  
> **Package ID:** `com.example.gestaobilhares`

---

## 🤖 MODO IA - ALTA AUTONOMIA

**Esta documentação foi projetada para IAs trabalharem com ALTA AUTONOMIA:**

### Regras de Execução
1. **Parar apenas após entrega completa** da tarefa
2. **Máximo 2 builds** por correção (protocolo Gates)
3. **Zero perguntas desnecessárias** (todas info está aqui)
4. **Build + Testes passando** = tarefa concluída
5. **Usar comandos `.bat`** (evita bloqueio Unix)

### Anti-Loop Enforcement
- **Gate 4:** Parada obrigatória após 2 builds sem sucesso
- **Static Analysis First:** Ler código antes de rodar build
- **Dynamic Analysis:** Apenas se código parecer correto

---

## 📚 Como usar a documentação (3 arquivos)

1. **PROJECT_README.md** (este): visão geral, arquitetura, componentes, métricas e fluxos
2. **DEV_GUIDE.md**: protocolo Gates, receitas de diagnóstico e runbook operacional
3. **TECHNICAL_DEBT.md**: backlog priorizado com código de implementação

---

## 🎯 Visão geral

Sistema Android para gestão completa de negócio de bilhar:
- **Multi-tenancy** por empresa e rota
- **Offline-first** (Room como fonte da verdade) com sincronização Firestore
- **Gestão financeira** (acertos, despesas, ciclos e metas)
- **Inventário/manutenção** (mesas, panos, equipamentos e veículos)
- **Relatórios** e dashboards

---

## 🏗️ Arquitetura e stack

### Stack principal
- **Kotlin:** 1.9.20
- **Android:** Compile SDK 34, Min SDK 24
- **Arquitetura:** MVVM
- **DI:** Hilt
- **Database local:** Room
- **Backend:** Firebase (Firestore + Auth)
- **Async:** Coroutines + StateFlow
- **Navegação:** Navigation Component
- **Build:** Gradle 8.2 (usar `.bat` sempre)

### Módulos
```
gestaobilhares/
├── app/           # Módulo principal e configuração
├── core/          # Utilitários compartilhados
├── data/          # Camada de dados (Room + Firebase)
├── ui/            # Camada de apresentação (Fragments + ViewModels)
└── sync/          # Sincronização offline-first
```

---

## 🔧 Configuração de Ambiente

### Path ADB Customizado (⚠️ IMPORTANTE)
O projeto usa caminho customizado de ADB:

```bash
# Path correto do ADB (NÃO o padrão do Android Studio)
C:\Users\Rossiny\Desktop\2-GestaoBilhares\android-sdk\platform-tools\adb.exe
```

**Problema comum:** IAs procuram no path padrão e falham.

**Solução:** Sempre usar o path completo acima nos comandos.

### Local.properties
```properties
sdk.dir=C:\\Users\\Rossiny\\AppData\\Local\\Android\\Sdk
```

### Secrets (NÃO sincronizado com Git)
```
secrets/
├── google-services.json          # Config Firebase
├── service-account.json           # Service Account p/ scripts
└── keystore.properties            # Assinatura APK release
```

**Path:** `secrets/` na raiz do projeto  
**Status:** Ignorado pelo `.gitignore`

---

## 🧩 Componentes principais

### ViewModels (34 total)

**Críticos (5):**
- `SettlementViewModel` — Fechamento de acertos, troca de panos, cálculos financeiros
- `DashboardViewModel` — Métricas e resumo financeiro do dia/mês
- `RoutesViewModel` — Gestão de rotas e transferências de clientes
- `ClienteViewModel` — Cadastro e validações de clientes
- `AcertoViewModel` — Processamento de acertos

**Gestão de Mesas (10):**
- `GerenciarMesasViewModel`, `CadastroMesaViewModel`, `EditMesaViewModel`
- `RotaMesasViewModel`, `MesasReformadasViewModel`, `MesasDepositoViewModel`
- `HistoricoMesasVendidasViewModel`, `HistoricoManutencaoMesaViewModel`
- `NovaReformaViewModel`

**Financeiro (6):**
- `CycleManagementViewModel`, `CycleExpensesViewModel`, `CycleReceiptsViewModel`
- `ExpenseRegisterViewModel`, `GlobalExpensesViewModel`, `ExpenseHistoryViewModel`

**Metas (3):**
- `MetasViewModel`, `MetaCadastroViewModel`, `MetaHistoricoViewModel`

**Inventário (4):**
- `StockViewModel`, `EquipmentsViewModel`, `VehiclesViewModel`, `VehicleDetailViewModel`

**Outros (6):**
- `BackupViewModel`, `TransferClientViewModel`, `LogViewerViewModel`
- `RotasConfigViewModel`, `RouteManagementViewModel`, `ClientSelectionViewModel`

**Path:** `ui/src/main/java/com/example/gestaobilhares/ui/`

---

### DAOs (27 total)

**Principais:**
- `SyncOperationDao` — **CRÍTICO** (fila de sincronização offline)
- `ClienteDao`, `AcertoDao`, `MesaDao`, `DespesaDao`, `RotaDao`
- `CicloDao`, `MetaDao`, `VeiculoDao`, `EstoqueDao`, `EquipamentoDao`

**Path:** `data/src/main/java/com/example/gestaobilhares/data/dao/`

**Padrão:** `EntidadeDao.kt` (ex: `ClienteDao.kt`, `AcertoDao.kt`)

---

### Repositories (22 total)

**Padrão:** Interface + Implementação com Hilt
- `AcertoRepository`, `ClienteRepository`, `MesaRepository`
- `DespesaRepository`, `RotaRepository`, `CicloRepository`
- `SyncRepository` — **CRÍTICO** (orquestra sincronização)
- `AppRepository` — **FACADE** (ponto central, NÃO refatorar sem análise)

**Path:** `data/src/main/java/com/example/gestaobilhares/data/repository/`

**Padrão:**
- `EntidadeRepository.kt` (interface)
- `EntidadeRepositoryImpl.kt` (implementação)

---

### Use Cases (4 principais)

1. **RegistrarTrocaPanoUseCase** — Lógica completa de troca de pano
   - Validações, histórico, atualização de estoque
   - Usado por `SettlementViewModel`, `NovaReformaViewModel`

**Path:** `data/src/main/java/com/example/gestaobilhares/data/usecases/`

---

## 📊 Métricas do projeto

### Código
- **Linhas:** ~50.000
- **ViewModels:** 34
- **DAOs:** 27
- **Repositories:** 22
- **Fragments:** 34
- **Adapters:** 33
- **Use Cases:** 4
- **Scripts:** 112

### Cobertura de Testes
- **Atual:** 5-7% (~7 testes)
- **Meta 2 semanas:** 30%
- **Meta 1 mês:** 60%
- **Meta 3 meses:** 80%

### Performance
- **Sync esperado:** < 2s (meta)
- **Builds por correção:** Máximo 2 (protocolo Gates)
- **Tempo resolução bug:** 15-30 min (com protocolo)

---

## 🗄️ Firestore (multi-tenancy)

### Hierarquia (padrão obrigatório)
```
empresas/
├── {empresaId}/
│   ├── colaboradores/{uid}
│   └── entidades/
│       ├── rotas/items/{id}
│       ├── clientes/items/{id}
│       ├── mesas/items/{id}
│       ├── acertos/items/{id}
│       └── despesas/items/{id}
```

### Índices Firestore Implementados

**3 índices compostos configurados no Console:**

1. **Clientes por rota + ativo + nome**
   ```
   Collection: empresas/{empresaId}/entidades/clientes/items
   Fields: rota_id (ASC), ativo (ASC), nome (ASC)
   Scope: Collection
   Status: Enabled
   ```

2. **Acertos por cliente + data**
   ```
   Collection: empresas/{empresaId}/entidades/acertos/items
   Fields: cliente_id (ASC), dataAcerto (DESC)
   Scope: Collection
   Status: Enabled
   ```

3. **Mesas por cliente + ativa**
   ```
   Collection: empresas/{empresaId}/entidades/mesas/items
   Fields: cliente_id (ASC), ativa (ASC)
   Scope: Collection
   Status: Enabled
   ```

### Campos obrigatórios (em entidades)
- `empresa_id`: isolamento por empresa
- `rota_id`: isolamento por rota (controle de acesso)
- `dataUltimaAtualizacao`: base para resolução de conflitos

### Colaboradores (Auth + perfil)
Path: `empresas/{empresaId}/colaboradores/{uid}`

Campos:
- `firebase_uid`, `empresa_id`, `nivel_acesso`, `aprovado`, `rotasPermitidas`

### Security Rules (intenção)
- Usuário só acessa dados das rotas em `rotasPermitidas`
- Escritas exigem `empresa_id` e validações de rota
- Função-chave: `belongsToUserRoute(empresaId, rotaId)`

---

## 🔄 Sincronização offline-first

### Estratégia
1. **Room é a fonte da verdade**
2. Operações geram entrada em `sync_operations`
3. **WorkManager** processa a fila em background (6h)
4. Firestore recebe dados seguindo o path padrão
5. Conflitos resolvidos via `dataUltimaAtualizacao` (Last Writer Wins, com exceções)

### WorkManager Config
```kotlin
// Intervalo: 6 horas
// Constraints: Conectado + Bateria não baixa + Carregando
// Worker: SyncWorker.kt
// Agendado em: GestaoBilharesApplication.onCreate()
```

### Status típicos
- `PENDING`: aguardando
- `PROCESSING`: em execução
- `SUCCESS/COMPLETED`: concluído
- `FAILED/ERROR`: falhou (retry ou erro permanente)

---

## 💼 Regras de Negócio (com código)

### Taxa de Comissão
- **Padrão:** Campo `comissaoFicha` por cliente (ex: 0.6)
- **Cálculo:** `comissao = totalFichas × comissaoFicha`
- **Implementação:** `FinancialCalculator.kt` (linha 42)
- **Validação:** Entre 0.0 e 100.0

### Acerto (Fechamento)
**Definição:** Fechamento financeiro periódico por cliente.

**Cálculo completo:**
```kotlin
// 1. Total de fichas (soma de todas as mesas)
totalFichas = Σ(fichasFinal - fichasInicial)

// 2. Valor bruto
valorBruto = totalFichas × valorFicha

// 3. Comissão
comissao = totalFichas × comissaoFicha

// 4. Valor líquido
valorLiquido = valorBruto - comissao - descontos + debitoAnterior

// 5. Débito atual
debitoAtual = valorLiquido - valorRecebido
```

**Fonte:** `FinancialCalculator.kt` (linhas 38-102)

### Débito/Crédito
- **Débito positivo:** cliente deve à empresa
- **Débito negativo:** empresa deve ao cliente (crédito)
- **Propagação:** Débito atual vira `debitoAnterior` no próximo acerto
- **Limite:** Sem limite hard-coded (apenas alertas UI)

### Estados de Mesa
**Enum:** `EstadoConservacao`
- `NOVO` — Nova (sem uso)
- `BOM` — Bom estado
- `REGULAR` — Uso moderado
- `RUIM` — Precisa reforma

**Status boolean:** `ativa`
- `true`: ATIVA (em operação)
- `false`: REFORMA/VENDIDA/DEPOSITO

**Fonte:** `MesaEntity.kt` (linha 28)

### Troca de Pano
**Regras:**
1. Validar mesa existe e ativa
2. Validar pano em estoque (quantidade > 0)
3. Baixar estoque (nunca negativo)
4. Registrar histórico imutável
5. Atualizar mesa (`panoAtualId`, `dataUltimaTrocaPano`)
6. Gerar SyncOperation

**Invariantes:**
- Estoque ≥ 0
- Histórico não é editado
- Mesa sempre aponta para pano existente

**Implementação:** `RegistrarTrocaPanoUseCase.kt`

---

## 📖 Glossário do domínio

| Termo | Definição | Implementação |
|-------|-----------|---------------|
| **Acerto** | Fechamento financeiro periódico por cliente | `FinancialCalculator.calcularDebitoAtual()` |
| **Rota** | Divisão geográfica com multi-tenancy | `rotasPermitidas` array |
| **Mesa** | Mesa de sinuca com estados BOM/REGULAR/RUIM/NOVO | `EstadoConservacao` enum |
| **Ficha** | Unidade de medida (valor × comissão) | `valorFicha * comissaoFicha` |
| **Pano** | Tecido da mesa com histórico de manutenção | `HistoricoManutencaoMesa.TROCA_PANO` |
| **Comissão** | Valor por ficha definido por cliente | `comissaoFicha` field |
| **Ciclo** | Período de acertos agrupados por rota/ano | `CicloAcertoEntity` |
| **Despesa** | Gastos por categoria vinculados a ciclos | `Despesa` entity |
| **Reforma** | Manutenção registrada em histórico | `TipoManutencao` enum |
| **Depósito** | Mesa em estoque (ativa=false) | `ativo` boolean |

---

## 🚀 Comandos Essenciais (SEMPRE `.bat`)

### Build
```bash
# Build debug (SEMPRE usar .bat)
.\gradlew.bat :app:assembleDebug

# Build release
.\gradlew.bat :app:assembleRelease

# Com cache e paralelismo
.\gradlew.bat :app:assembleDebug --build-cache --parallel

# Instalar APK
C:\Users\Rossiny\Desktop\2-GestaoBilhares\android-sdk\platform-tools\adb.exe install app/build/outputs/apk/debug/app-debug.apk
```

### Testes
```bash
# Testes unitários
.\gradlew.bat testDebugUnitTest

# Testes instrumentados
.\gradlew.bat connectedDebugAndroidTest
```

### Logs (ADB customizado)
```bash
# Path ADB customizado
set ADB_PATH=C:\Users\Rossiny\Desktop\2-GestaoBilhares\android-sdk\platform-tools\adb.exe

# Logs por tag
%ADB_PATH% logcat -s [DIAGNOSTICO]:D -v time

# Múltiplas tags
%ADB_PATH% logcat -s [DIAGNOSTICO]:D [SYNC]:D [CONFLICT]:D *:E

# Salvar em arquivo
%ADB_PATH% logcat > logs.txt
```

### Git
```bash
# Status
git status

# Commit
git add .
git commit -m "feat: implementa funcionalidade X"

# Push
git push origin main
```

### Database Inspector
```
Android Studio → View → Tool Windows → App Inspection → Database Inspector
```

---

## 📦 Deploy e Distribuição

### Firebase App Distribution
```bash
# Via script PowerShell (requer secrets/)
.\scripts\deploy-app-distribution.ps1

# Ou manual
.\gradlew.bat :app:assembleDebug
firebase appdistribution:distribute app/build/outputs/apk/debug/app-debug.apk \
  --app 1:XXXXX:android:XXXXX \
  --groups testers \
  --release-notes "Versão teste X"
```

### Security Rules Deploy
```bash
# Via Node.js (automatizado)
node import-data/deploy-security-rules-v2.js

# Via PowerShell (manual)
.\scripts\deploy-regras-firestore.ps1
```

---

## 🔐 Regras do projeto (leis)

1. Multi-tenancy por rota é **obrigatório**
2. Offline-first é **obrigatório**
3. UI não acessa DAO/Firebase diretamente
4. StateFlow (não LiveData)
5. Valores em reais (Double), nunca centavos
6. **Usar comandos `.bat`** (evita bloqueio Unix)
7. **ADB path customizado** (não o padrão)
8. **Secrets em `secrets/`** (nunca no Git)

---

## 📖 Referência rápida

### Encontrar componentes
- **DAOs:** `data/src/main/java/.../data/dao/EntidadeDao.kt`
- **Repositories:** `data/src/main/java/.../data/repository/EntidadeRepository[Impl].kt`
- **ViewModels:** `ui/src/main/java/.../ui/<funcionalidade>/ViewModel.kt`

### Busca no código
```bash
rg "SettlementViewModel" --type kt -n
rg "sync_operations" --type kt -C 5
```

---

## 🚀 Onboarding rápido

### Para humanos (1-2 horas)
1. Ler este arquivo (30 min)
2. Ler DEV_GUIDE.md (20 min)
3. Explorar Firebase Console (10 min)
4. Rodar app em debug (20 min)
5. Testar fluxo de acerto (20 min)

### Para IA (5 min)
1. Carregar PROJECT_README.md
2. Carregar DEV_GUIDE.md
3. Carregar TECHNICAL_DEBT.md
4. **Começar tarefa com ALTA AUTONOMIA**

---

## ✅ Checklist de Validação (para IAs)

Antes de considerar tarefa concluída:

- [ ] Build passa (`.\gradlew.bat :app:assembleDebug`)
- [ ] Testes passam (`.\gradlew.bat testDebugUnitTest`)
- [ ] Código segue padrão MVVM + Hilt
- [ ] Multi-tenancy respeitado (rota_id presente)
- [ ] Offline-first mantido (Room primeiro, sync depois)
- [ ] Logs estruturados adicionados ([TAG])
- [ ] Máximo 2 builds usados
- [ ] Zero regressões introduzidas

---

*Documentação viva — mantida pela equipe de desenvolvimento e otimizada para IAs.*
