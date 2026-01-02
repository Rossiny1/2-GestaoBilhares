# 3️⃣ ARQUITETURA E PADRÕES

> **Propósito**: Definição da estrutura técnica, padrões de código e modularização.  
> **Última Atualização**: 02 de Janeiro de 2026  
> **Versão**: 6.0 (Correções Crashlytics + Testes + Deploy Release)

---

## 📐 ARQUITETURA HÍBRIDA (Modular)
O projeto é dividido em 5 módulos Gradle para eficiência e isolamento:
*   **`:app`**: Ponto de entrada e configuração global.
*   **`:ui`**: Camada visual (Compose + ViewBinding) e ViewModels.
*   **`:data`**: Persistência local (Room) e Repositories (MVVM).
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
*   ✅ **Status**: Todos os testes unitários passando (corrigidos 3 testes recentemente).

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

## 🧹 BOAS PRÁTICAS
1.  **Imutabilidade**: Usar `data class` com `val` sempre que possível.
2.  **Timber**: Usar `Timber.tag(TAG).d()` para debug e `Timber.e()` para erros.
3.  **Encapsulamento**: DAOs e RemoteDataSources nunca devem ser expostos fora do módulo `:data`.
