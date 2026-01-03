# 📖 3️⃣ ARQUITETURA E PADRÕES

> **LEIA TERCEIRO** - Entenda a estrutura técnica e padrões antes de implementar.  
> **Propósito**: Definição da estrutura técnica, padrões de código e modularização.  
> **Última Atualização**: Janeiro 2026 (Refatoração ColaboradorRepository)  
> **Versão**: 6.0 (Correções Crashlytics + Testes + Deploy Release)

---

## 📐 ARQUITETURA HÍBRIDA (Modular)
O projeto é dividido em 5 módulos Gradle para eficiência e isolamento:
*   **`:app`**: Ponto de entrada e configuração global.
*   **`:ui`**: Camada visual (Compose + ViewBinding) e ViewModels.
*   **`:data`**: Persistência local (Room) e Repositories (MVVM). ✅ **Refatorado**: AppRepository delega para repositories especializados (ColaboradorRepository, ColaboradorFirestoreRepository, ColaboradorAuthService).
*   **`:sync`**: Motor de sincronização e handlers Firestore.
*   **`:core`**: Lógica compartilhada, segurança e utilitários.

---

## 🔄 PADRÕES DE SINCRONIZAÇÃO (Sync Engine)
### Padrão Orchestrator
Para evitar arquivos massivos, o módulo `:sync` utiliza o padrão **Orchestrator + Handlers**:
*   `SyncRepository`: Orquestra o fluxo global (Pull/Push). ⚠️ **Ainda com 3644 linhas** - refatoração pendente.
*   `SyncHandlers`: Cada entidade (Mesa, Cliente, Acerto, Ciclo, Despesa, Rota, Colaborador, Contrato) possui seu próprio handler especializado.
*   `BaseSyncHandler`: Classe base com utilitários como `entityToMap`, filtros de multi-tenancy e paginação.

### Sincronização Incremental
*   Uso de `last_modified` do servidor para busca diferencial.
*   Economia de ~98% de dados em sincronizações subsequentes.
*   ✅ **CancellationException**: Tratamento correto implementado em todos os handlers para propagar cancelamento de corrotinas.


---

## 🛡️ QUALIDADE E TESTES
### Estratégia Unitária
*   **Financeiro**: Lógica centralizada em `FinancialCalculator` com 100% de cobertura.
*   **Sincronização**: Cada `SyncHandler` possui testes unitários (`ComprehensiveSyncTest`, `ConflictResolutionTest`) validando pull, push, integridade relational e resolução de conflitos.
*   **Repositórios**: `SyncRepositoryTest` valida a orquestração e filtros de rota.
*   **Colaboradores**: `ColaboradorRepositoryTest` e `ColaboradorAuthServiceTest` cobrem criação, aprovação, sincronização e resolução de conflitos (28 testes passando).
*   ✅ **Status**: Todos os testes unitários passando (28 testes para Colaborador implementados recentemente).

### Cobertura e Regressão
*   **JaCoCo**: Configurado para medir cobertura em módulos críticos.
*   **Cenários de Borda**: Testes incluem simulação de falhas de rede, conflitos de ID, cenários de "Bootstrap" (primeiro login) e paginação Firestore.
*   **Mocks**: Cadeia completa de queries Firestore mockada corretamente (`whereEqualTo` → `limit` → `startAfter`).

---

## 🛠️ STACK TÉCNICO
*   **DI**: Hilt (100% migrado).
*   **UI**: ViewBinding (51 Fragments + 27 Dialogs). Compose: 0% (meta: 60% Q2/2026).
*   **Data**: Room com Flow support para reatividade real-time local.
*   **Logging**: **Timber** é obrigatório. `android.util.Log` é desencorajado.
*   **Threads**: Kotlin Coroutines & Flow (Suspensão sobre Bloqueio). ✅ CancellationException tratado corretamente.
*   **Build**: ProGuard/R8 ativado em release. Mapping.txt gerado automaticamente.

---

## 🏗️ REPOSITORIES ESPECIALIZADOS (Padrão de Delegação)

### Arquitetura de Repositories
O `AppRepository` atua como **Facade** delegando para repositories especializados:

#### Colaborador (✅ Refatorado - Janeiro 2026)
*   **`ColaboradorRepository`**: Operações locais (Room) - busca, inserção, atualização, criação de pendentes.
*   **`ColaboradorFirestoreRepository`**: Operações Firestore - busca por UID, criação, atualização de status de aprovação, sincronização completa.
*   **`ColaboradorAuthService`**: Coordena o fluxo de autenticação - processa colaborador durante login, preserva status de aprovação, resolve conflitos (local vs Firestore).

**Benefícios**:
- ✅ Lógica de aprovação centralizada e testável
- ✅ Preservação automática de status de aprovação durante login
- ✅ Resolução de conflitos entre local e Firestore
- ✅ 28 testes unitários cobrindo cenários críticos

#### Outros Repositories Especializados
*   **`ClienteRepository`**: Operações de clientes (local)
*   **`AcertoRepository`**: Operações de acertos (local)
*   **`RotaRepository`**: Operações de rotas (local)
*   **`DespesaRepository`**: Operações de despesas (local)
*   **`MesaRepository`**: Operações de mesas (local)
*   **`CicloRepository`**: Operações de ciclos (local)
*   **`MetaRepository`**: Operações de metas (local)
*   **`VeiculoRepository`**: Operações de veículos (local)
*   **`ContratoRepository`**: Operações de contratos (local)
*   **`PanoRepository`**: Operações de panos (local)

## 🧹 BOAS PRÁTICAS
1.  **Imutabilidade**: Usar `data class` com `val` sempre que possível.
2.  **Timber**: Usar `Timber.tag(TAG).d()` para debug e `Timber.e()` para erros.
3.  **Encapsulamento**: DAOs e RemoteDataSources nunca devem ser expostos fora do módulo `:data`.
4.  **Delegação**: Novos métodos devem ser implementados em repositories especializados, não diretamente no `AppRepository`.

## 🛠️ FERRAMENTAS DE DESENVOLVIMENTO

### Cursor Cloud (Ambiente Principal)
- **Uso**: Ambiente remoto principal para desenvolvimento e implementações
- **Vantagens**:
  - Integração nativa com Firebase CLI e MCP
  - Acesso direto ao Crashlytics para análise de erros
  - Assistente de IA com contexto completo do projeto
  - Ambiente consistente (sem problemas de setup local)
- **Localização**: `/workspace` na VM

### Firebase CLI
- **Autenticação**: Via `firebase login:ci` (token armazenado em `FIREBASE_TOKEN`)
- **Uso**: Deploy de releases, análise de logs, gerenciamento de projeto
- **Integração**: Total com Cursor Cloud via MCP

### GitHub
- **Repositório**: `https://github.com/Rossiny1/2-GestaoBilhares`
- **Sincronização**: Automática entre VM (Cursor Cloud) e máquinas locais
- **Branches**: `main` (produção), `develop` (desenvolvimento), `feature/*` (features)

### Workflow Recomendado
1. **Desenvolvimento**: Cursor Cloud (VM) para features principais
2. **Testes Locais**: Máquina do desenvolvedor para validação rápida
3. **Deploy**: Cursor Cloud (VM) para builds de release
4. **Sincronização**: GitHub como fonte única da verdade
