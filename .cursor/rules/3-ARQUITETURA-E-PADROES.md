# 3️⃣ ARQUITETURA E PADRÕES

> **⚠️ IMPORTANTE**: Antes de ler este arquivo, leia PRIMEIRO: `.cursor/rules/0-PERFORMANCE-MAXIMA-OBRIGATORIO.md`  
> **Propósito**: Definição da estrutura técnica, padrões de código e modularização.  
> **Última Atualização**: Janeiro 2026  
> **Versão**: 5.3 (Performance Máxima + Estratégia de Testes Ampliada)

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
*   `SyncRepository`: Orquestra o fluxo global (Pull/Push).
*   `SyncHandlers`: Cada entidade (Mesa, Cliente, Acerto) possui seu próprio handler especializado.
*   `BaseSyncHandler`: Classe base com utilitários como `entityToMap` e filtros de multi-tenancy.

### Sincronização Incremental
*   Uso de `last_modified` do servidor para busca diferencial.
*   Economia de ~98% de dados em sincronizações subsequentes.


---

## 🛡️ QUALIDADE E TESTES
### Estratégia Unitária
*   **Financeiro**: Lógica centralizada em `FinancialCalculator` com 100% de cobertura.
*   **Sincronização**: Cada `SyncHandler` possui testes unitários (`ComprehensiveSyncTest`) validando pull, push e integridade relational.
*   **Repositórios**: `SyncRepositoryTest` valida a orquestração e filtros de rota.

### Cobertura e Regressão
*   **JaCoCo**: Configurado para medir cobertura em módulos críticos.
*   **Cenários de Borda**: Testes incluem simulação de falhas de rede, conflitos de ID e cenários de "Bootstrap" (primeiro login).

---

## 🛠️ STACK TÉCNICO
*   **DI**: Hilt (100% migrado).
*   **UI**: Transição Compose (Híbrida Fragments/Composables).
*   **Data**: Room com Flow support para reatividade real-time local.
*   **Logging**: **Timber** é obrigatório. `android.util.Log` é desencorajado.
*   **Threads**: Kotlin Coroutines & Flow (Suspensão sobre Bloqueio).

---

## 🧹 BOAS PRÁTICAS
1.  **Imutabilidade**: Usar `data class` com `val` sempre que possível.
2.  **Timber**: Usar `Timber.tag(TAG).d()` para debug e `Timber.e()` para erros.
3.  **Encapsulamento**: DAOs e RemoteDataSources nunca devem ser expostos fora do módulo `:data`.
