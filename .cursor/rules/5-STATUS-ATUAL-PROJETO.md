# 5. STATUS ATUAL DO PROJETO

## ✅ VISÃO GERAL

- **Build**: passando e estável (o usuário executa o build localmente) [[memory:8892811]] [[memory:8654066]
- **Arquitetura**: MVVM + Room + Navigation; migração parcial para Jetpack Compose (arquitetura híbrida View + Compose)
- **Offline-first**: priorizado; integrações online não bloqueiam o uso [[memory:9444462]
- **Sessão e Acesso**: `UserSessionManager` com fallback a `SharedPreferences` e `StateFlow` reativo
- **Logs**: padronização via tag `LOG_CRASH` + script `crash-simples.ps1` atualizado

## 🔧 COMPONENTES TÉCNICOS

- **MVVM + StateFlow**: ViewModels com `StateFlow`/`repeatOnLifecycle` nos Fragments principais
- **Room**: DAOs operando corretamente; AppRepository centralizado
- **Navigation Component**: `nav_graph.xml` como fonte de verdade para fluxos; sem deep links externos
- **Compose**: Tema M3 configurado; screens Compose integradas via wrappers/Activities/Fragments existentes
- **Assinaturas & Jurídico**: captura nativa, metadados, hash de integridade, logs de auditoria preservados

## 🎨 MIGRAÇÃO JETPACK COMPOSE (STATUS ATUALIZADO - DEZEMBRO 2024)

### ✅ Telas Compose Implementadas (24 telas)

**Core Business:**
- `RoutesScreen`, `DashboardScreen`, `ClientListScreen`, `ClientDetailScreen`
- `SettlementScreen`, `SettlementDetailScreen`, `ClosureReportScreen`

**Inventário:**
- `VehiclesScreen`, `VehicleDetailScreen`, `StockScreen`

**Gestão:**
- `ContractManagementScreen`, `ContractGenerationScreen`, `SignatureCaptureScreen`
- `RepresentanteLegalSignatureScreen`, `MetasScreen`, `MetaCadastroScreen`
- `ColaboradoresScreen`, `CiclosScreen`

**Despesas:**
- `ExpenseRegisterScreen`, `ExpenseTypesScreen`, `ExpenseCategoriesScreen`

**Mesas:**
- `MesasDepositoScreen`, `NovaReformaScreen`

**Autenticação:**
- `LoginScreen`

### 🔄 Fragments Híbridos (Wrapper + Compose)

Estes Fragments já usam `ComposeView` e carregam telas Compose:
- `ClientDetailFragment` → `ClientDetailScreen`
- `ExpenseRegisterFragment` → `ExpenseRegisterScreen`
- `SettlementFragment` → `SettlementScreen` (parcial)

### ❌ Fragments Pendentes de Migração (43 telas)

**Prioridade ALTA - Core Business:**
- `SettlementFragment` (completo), `SettlementDetailFragment`
- `ClientListFragment`, `ClientRegisterFragment`, `CycleHistoryFragment`

**Prioridade ALTA - Ciclos (Crítico):**
- `CycleManagementFragment` (ViewPager2 principal)
- `CycleReceiptsFragment`, `CycleExpensesFragment`, `CycleSummaryFragment`, `CycleClientsFragment`

**Prioridade MÉDIA - Inventário:**
- `OthersInventoryFragment`, `EquipmentsFragment`

**Prioridade MÉDIA - Despesas:**
- `ExpenseHistoryFragment`, `GlobalExpensesFragment`, `ExpenseCategoriesFragment`, `ExpenseTypesFragment`

**Prioridade MÉDIA - Mesas:**
- `GerenciarMesasFragment`, `RotaMesasFragment`, `EditMesaFragment`
- `HistoricoManutencaoMesaFragment`, `HistoricoMesasVendidasFragment`, `MesasReformadasFragment`
- `CadastroMesaFragment`

**Prioridade BAIXA - Gestão:**
- `ColaboradorManagementFragment`, `ColaboradorRegisterFragment`, `ColaboradorMetasFragment`
- `MetasFragment`, `AditivoSignatureFragment`

**Prioridade BAIXA - Sistema:**
- `LoginFragment`, `MainFragment`, `LogViewerFragment`
- `RouteManagementFragment`

### 📊 Estatísticas da Migração

- **Compose Implementado**: 24 telas (35.8%)
- **Fragments Pendentes**: 43 telas (64.2%)
- **Híbridos (Wrapper)**: 3 telas
- **Total Analisado**: 67 telas

## 🧭 DIRETRIZES DE MIGRAÇÃO (CONSERVADORA)

- **Não mudar aparência**: manter UI idêntica (cores, espaçamentos, hierarquias)
- **Preservar lógica**: manter ViewModels, Repositórios e validações existentes
- **Inicialização manual**: manter padrão de inicialização explícita de ViewModels nos Fragments enquanto híbrido
- **Navegação progressiva**: substituir Fragments por Composables por fluxo, evitando mudanças amplas de uma só vez
- **Ícones**: reutilizar os ícones já usados pré-Compose, evitando regressões

## ✅ CORREÇÕES CRÍTICAS JÁ ENTREGUES

- Menu principal para admin/super-admin estabilizado (verificações em `RoutesScreen`, `RoutesFragment`, `UserSessionManager`)
- Assinatura: distinção entre rolagem e desenho; validação de traçado; salvamento confiável
- `CycleReceiptsFragment`: adicionados logs abrangentes e carregamento via `first()` para evitar coletas infinitas
- `crash-simples.ps1`: filtros atualizados para `LOG_CRASH`, `RoutesScreen`, `UserSessionManager`

## 📋 PLANO DE MIGRAÇÃO COMPOSE (ETAPAS DETALHADAS)

### 🎯 FASE 1: Core Business (Prioridade CRÍTICA)
**Objetivo**: Finalizar fluxo principal de acertos e clientes

**1.1 SettlementFragment Completo**
- Migrar lógica restante de `SettlementFragment` para `SettlementScreen`
- Implementar captura de foto, validações financeiras, salvamento
- Testes: fluxo completo de acerto

**1.2 ClientListFragment**
- Migrar `ClientListFragment` para usar `ClientListScreen` nativo
- Remover wrapper Fragment, navegação direta Compose
- Testes: listagem, busca, filtros

**1.3 SettlementDetailFragment**
- Migrar `SettlementDetailFragment` para `SettlementDetailScreen`
- Implementar visualização de acertos salvos
- Testes: histórico, impressão, compartilhamento

### 🎯 FASE 2: Ciclos (Prioridade ALTA)
**Objetivo**: Migrar sistema de ciclos para Compose

**2.1 CycleManagementFragment**
- Criar `CycleManagementScreen` com `TabRow` Compose
- Substituir `ViewPager2` por navegação Compose nativa
- Manter estado entre abas

**2.2 Abas de Ciclos**
- `CycleReceiptsFragment` → `CycleReceiptsScreen`
- `CycleExpensesFragment` → `CycleExpensesFragment`
- `CycleSummaryFragment` → `CycleSummaryScreen`
- `CycleClientsFragment` → `CycleClientsScreen`

**2.3 Integração**
- Conectar abas com `CycleManagementScreen`
- Testes: navegação entre abas, persistência de dados

### 🎯 FASE 3: Inventário (Prioridade MÉDIA)
**Objetivo**: Completar módulo de inventário

**3.1 OthersInventoryFragment**
- Criar `OthersInventoryScreen`
- Implementar CRUD de equipamentos diversos

**3.2 EquipmentsFragment**
- Criar `EquipmentsScreen`
- Gerenciamento de equipamentos específicos

### 🎯 FASE 4: Despesas (Prioridade MÉDIA)
**Objetivo**: Finalizar módulo de despesas

**4.1 ExpenseHistoryFragment**
- Criar `ExpenseHistoryScreen`
- Histórico e relatórios de despesas

**4.2 GlobalExpensesFragment**
- Criar `GlobalExpensesScreen`
- Visão consolidada de despesas

**4.3 ExpenseCategoriesFragment/ExpenseTypesFragment**
- Migrar para usar `ExpenseCategoriesScreen`/`ExpenseTypesScreen` nativos
- Remover wrappers Fragment

### 🎯 FASE 5: Mesas (Prioridade MÉDIA)
**Objetivo**: Completar gestão de mesas

**5.1 GerenciarMesasFragment**
- Criar `GerenciarMesasScreen`
- Dashboard de gestão de mesas

**5.2 Fragmentos de Histórico**
- `HistoricoManutencaoMesaFragment` → `HistoricoManutencaoMesaScreen`
- `HistoricoMesasVendidasFragment` → `HistoricoMesasVendidasScreen`
- `MesasReformadasFragment` → `MesasReformadasScreen`

**5.3 Fragmentos de Edição**
- `EditMesaFragment` → `EditMesaScreen`
- `CadastroMesaFragment` → `CadastroMesaScreen`
- `RotaMesasFragment` → `RotaMesasScreen`

### 🎯 FASE 6: Gestão (Prioridade BAIXA)
**Objetivo**: Finalizar módulos administrativos

**6.1 Colaboradores**
- `ColaboradorManagementFragment` → usar `ColaboradoresScreen` nativo
- `ColaboradorRegisterFragment` → `ColaboradorRegisterScreen`
- `ColaboradorMetasFragment` → `ColaboradorMetasScreen`

**6.2 Contratos**
- `AditivoSignatureFragment` → `AditivoSignatureScreen`
- Finalizar validações jurídicas em Compose

### 🎯 FASE 7: Sistema (Prioridade BAIXA)
**Objetivo**: Migrar componentes de sistema

**7.1 Autenticação**
- `LoginFragment` → usar `LoginScreen` nativo
- Remover wrapper Fragment

**7.2 Sistema**
- `MainFragment` → migração para Activity principal
- `LogViewerFragment` → `LogViewerScreen`
- `RouteManagementFragment` → `RouteManagementScreen`

### 🎯 FASE 8: Otimização e Limpeza
**Objetivo**: Consolidar e otimizar

**8.1 Componentes Reutilizáveis**
- Extrair componentes Compose comuns (Cards, Buttons, TextFields)
- Remover duplicações de código
- Padronizar temas e estilos

**8.2 Navegação**
- Migrar `nav_graph.xml` para Navigation Compose
- Implementar navegação type-safe
- Remover Fragments obsoletos

**8.3 Testes e Validação**
- Testes de integração por módulo
- Validação de performance
- Documentação final

## 🔎 MONITORAMENTO E LOGS

- Adotar tag única `LOG_CRASH` para diagnóstico em pontos chave (navegação, sessão, carregamento de dados)
- `crash-simples.ps1` deve permanecer sem Unicode/emoji e sem criar novos scripts

## 🧪 QUALIDADE E ESTABILIDADE

- Build: estável; quando falhar, aplicar funil de erros e comparar com telas Compose já aprovadas
- Sessão: quedas mitigadas por fallback a `SharedPreferences` e logs explícitos
- PDF/Impressão/WhatsApp: componentes preservados; sem regressões funcionais

## 🏗️ REFATORAÇÃO ARQUITETURAL: MODULARIZAÇÃO DO REPOSITORY (2025)

### **Decisão Arquitetural**

**Análise realizada:**
- AppRepository atual: ~1.430 linhas, 264 métodos, 17+ DAOs
- Contexto: 4 agents trabalhando simultaneamente
- Necessidade: Evitar conflitos de merge e permitir trabalho paralelo

**Decisão: Arquitetura Híbrida Modular**
- **AppRepository** mantém-se como **Facade/Coordinator** (compatibilidade preservada)
- **Repositories especializados** por domínio (ClientRepository, AcertoRepository, etc.)
- **AppRepository delega** para repositories especializados
- **ViewModels** continuam usando AppRepository (sem breaking changes)

### **Domínios Identificados para Modularização**

1. **ClientRepository** - Domínio: Clientes (obter, inserir, atualizar, deletar clientes)
2. **AcertoRepository** - Domínio: Acertos (transações de acerto, cálculos)
3. **MesaRepository** - Domínio: Mesas (gestão de mesas, vinculação, relógios)
4. **RotaRepository** - Domínio: Rotas (gestão de rotas, ciclos, status)
5. **DespesaRepository** - Domínio: Despesas (gestão de despesas, categorias, tipos)
6. **ColaboradorRepository** - Domínio: Colaboradores (gestão de colaboradores, metas)
7. **ContratoRepository** - Domínio: Contratos (contratos, aditivos, assinaturas)
8. **CicloRepository** - Domínio: Ciclos (ciclos de acerto, cálculos de ciclo)

## 🎯 PRÓXIMAS IMPLEMENTAÇÕES (CRONOGRAMA SUGERIDO)

### 📅 SEMANA 1-2: FASE 1 - Core Business
- **Dia 1-3**: `SettlementFragment` completo → `SettlementScreen`
- **Dia 4-5**: `ClientListFragment` → `ClientListScreen` nativo
- **Dia 6-7**: `SettlementDetailFragment` → `SettlementDetailScreen`
- **Testes**: Fluxo completo de acertos e clientes

### 📅 SEMANA 3-4: FASE 2 - Ciclos (Crítico)
- **Dia 1-2**: `CycleManagementScreen` com `TabRow`
- **Dia 3-4**: `CycleReceiptsScreen` e `CycleExpensesScreen`
- **Dia 5-6**: `CycleSummaryScreen` e `CycleClientsScreen`
- **Dia 7**: Integração e testes de navegação entre abas

### 📅 SEMANA 5-6: FASE 3-4 - Inventário e Despesas
- **Dia 1-2**: `OthersInventoryScreen` e `EquipmentsScreen`
- **Dia 3-4**: `ExpenseHistoryScreen` e `GlobalExpensesScreen`
- **Dia 5-6**: Migração de wrappers Fragment para Compose nativo
- **Dia 7**: Testes de integração

### 📅 SEMANA 7-8: FASE 5 - Mesas
- **Dia 1-2**: `GerenciarMesasScreen` e `RotaMesasScreen`
- **Dia 3-4**: Fragmentos de histórico (`HistoricoManutencaoMesaScreen`, etc.)
- **Dia 5-6**: Fragmentos de edição (`EditMesaScreen`, `CadastroMesaScreen`)
- **Dia 7**: Testes completos do módulo de mesas

### 📅 SEMANA 9-10: FASE 6-7 - Gestão e Sistema
- **Dia 1-2**: Colaboradores e Contratos
- **Dia 3-4**: Autenticação e Sistema
- **Dia 5-6**: Limpeza de Fragments obsoletos
- **Dia 7**: Testes de regressão

### 📅 SEMANA 11-12: FASE 8 - Otimização
- **Dia 1-3**: Componentes reutilizáveis e padronização
- **Dia 4-5**: Navegação Compose type-safe
- **Dia 6-7**: Testes finais e documentação

## 👥 DIVISÃO DE TAREFAS PARA 4 AGENTS (TRABALHO PARALELO)

### **AGENT 1: Domínios Core Business (Clientes e Acertos)**

**Responsabilidades:**
- Criar `ClientRepository.kt` (extrair métodos de Cliente do AppRepository)
- Criar `AcertoRepository.kt` (extrair métodos de Acerto do AppRepository)
- Atualizar `AppRepository.kt` para delegar para ClientRepository e AcertoRepository
- Testar funcionalidades de clientes e acertos

**Arquivos a modificar:**
- `data/repository/domain/ClientRepository.kt` (NOVO)
- `data/repository/domain/AcertoRepository.kt` (NOVO)
- `data/repository/AppRepository.kt` (atualizar delegações)

**Métodos a migrar:**
- ClientRepository: obterTodosClientes, obterClientePorId, inserirCliente, atualizarCliente, deletarCliente, obterDebitoAtual, etc.
- AcertoRepository: obterAcertosPorCliente, obterAcertoPorId, inserirAcerto, buscarUltimoAcertoPorCliente, etc.

### **AGENT 2: Domínios de Gestão (Mesas e Rotas)**

**Responsabilidades:**
- Criar `MesaRepository.kt` (extrair métodos de Mesa do AppRepository)
- Criar `RotaRepository.kt` (extrair métodos de Rota do AppRepository)
- Atualizar `AppRepository.kt` para delegar para MesaRepository e RotaRepository
- Testar funcionalidades de mesas e rotas

**Arquivos a modificar:**
- `data/repository/domain/MesaRepository.kt` (NOVO)
- `data/repository/domain/RotaRepository.kt` (NOVO)
- `data/repository/AppRepository.kt` (atualizar delegações)

**Métodos a migrar:**
- MesaRepository: obterMesaPorId, obterMesasPorCliente, inserirMesa, vincularMesaACliente, etc.
- RotaRepository: obterTodasRotas, obterRotasAtivas, inserirRota, getRotasResumoComAtualizacaoTempoReal, etc.

### **AGENT 3: Domínios Financeiros (Despesas e Ciclos)**

**Responsabilidades:**
- Criar `DespesaRepository.kt` (extrair métodos de Despesa do AppRepository)
- Criar `CicloRepository.kt` (extrair métodos de Ciclo do AppRepository)
- Atualizar `AppRepository.kt` para delegar para DespesaRepository e CicloRepository
- Testar funcionalidades de despesas e ciclos

**Arquivos a modificar:**
- `data/repository/domain/DespesaRepository.kt` (NOVO)
- `data/repository/domain/CicloRepository.kt` (NOVO)
- `data/repository/AppRepository.kt` (atualizar delegações)

**Métodos a migrar:**
- DespesaRepository: obterTodasDespesas, inserirDespesa, calcularTotalPorRota, buscarDespesasPorCicloId, etc.
- CicloRepository: obterTodosCiclos, inserirCicloAcerto, buscarCicloAtualPorRota, calcularComissoesPorCiclo, etc.

### **AGENT 4: Domínios Administrativos (Colaboradores e Contratos)**

**Responsabilidades:**
- Criar `ColaboradorRepository.kt` (extrair métodos de Colaborador do AppRepository)
- Criar `ContratoRepository.kt` (extrair métodos de Contrato do AppRepository)
- Atualizar `AppRepository.kt` para delegar para ColaboradorRepository e ContratoRepository
- Atualizar `RepositoryFactory.kt` para criar repositories especializados
- Testar funcionalidades de colaboradores e contratos

**Arquivos a modificar:**
- `data/repository/domain/ColaboradorRepository.kt` (NOVO)
- `data/repository/domain/ContratoRepository.kt` (NOVO)
- `data/repository/AppRepository.kt` (atualizar delegações)
- `data/factory/RepositoryFactory.kt` (atualizar criação)

**Métodos a migrar:**
- ColaboradorRepository: obterTodosColaboradores, inserirColaborador, aprovarColaborador, obterMetasPorColaborador, etc.
- ContratoRepository: buscarContratosPorCliente, inserirContrato, buscarAditivosPorContrato, etc.

### **Regras de Trabalho Paralelo**

1. **Cada Agent trabalha em domínios diferentes** (sem conflitos de merge)
2. **AppRepository é atualizado por todos** (mas em seções diferentes)
3. **Testes devem ser executados após cada migração** (garantir compatibilidade)
4. **Commits frequentes** (facilitar merge e rollback se necessário)
5. **Comunicação sobre mudanças no AppRepository** (evitar conflitos)

### **Ordem de Implementação Recomendada**

**FASE 1 (Semana 1-2):**
- Agent 1: ClientRepository
- Agent 2: MesaRepository
- Agent 3: DespesaRepository
- Agent 4: ColaboradorRepository

**FASE 2 (Semana 3-4):**
- Agent 1: AcertoRepository
- Agent 2: RotaRepository
- Agent 3: CicloRepository
- Agent 4: ContratoRepository

**FASE 3 (Semana 5):**
- Todos: Refinar AppRepository (remover código duplicado, otimizar delegações)
- Todos: Testes de integração completos
- Todos: Documentação final

## 🧭 CONCLUSÃO

O projeto está estável com 35.8% das telas já migradas para Compose. A refatoração arquitetural para modularização do repository permitirá trabalho paralelo eficiente entre 4 agents, evitando conflitos de merge e facilitando manutenção. O plano detalhado em 8 fases prioriza o core business (acertos e clientes) e o sistema crítico de ciclos. A estratégia conservadora preserva funcionalidades existentes enquanto migra incrementalmente para uma arquitetura Compose-first e modular. O cronograma de 12 semanas permite entregas incrementais com validação contínua.
