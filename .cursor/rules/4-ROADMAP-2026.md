# 4️⃣ ROADMAP 2026

> **Propósito**: Planejamento estratégico e fases pendentes de evolução.  
> **Última Atualização**: 02 de Janeiro de 2026  
> **Versão**: 4.0 (Release 1.0.1 Deployado)

---

## 📅 FASES PENDENTES

### FASE 1: Consolidação da Arquitetura (Q1 2026) 🟡 **PARCIALMENTE CONCLUÍDO**
*   ✅ **Refatoração AppRepository**: Migração de métodos para repositories especializados concluída.
*   ⚠️ **Orchestrator Sync**: Handlers especializados criados, mas **SyncRepository ainda com 3644 linhas** (meta: < 300). Refatoração pendente.
*   ✅ **Testes de Cobertura**: 100% de cobertura em `FinancialCalculator`. Testes unitários para todos os `SyncHandlers`. Todos testes passando.
*   ✅ **Fix Sincronização**: Resolvido problema de acesso a rotas para não-admins via lógica de Bootstrap.
*   ✅ **Correções Crashlytics**: 4 erros corrigidos. Release 1.0.1 deployado.

### FASE 2: Transição de Interface (Q2 2026)
*   **Aceleração Compose**: Migrar 51 Fragments + 27 Dialogs de ViewBinding para Jetpack Compose (Meta: 60% concluído). Status atual: 0%.
*   **Performance Visual**: Implementar compressão de imagens adaptativa no upload para o Firestore.

### FASE 3: Estabilidade e Acessibilidade (Q3 2026)
*   **Auditoria de Memória**: ⚠️ **NÃO INICIADO** - Implementar LeakCanary para detectar e eliminar vazamentos nos Fragments (51 Fragments ativos).
*   **Acessibilidade (A11y)**: Garantir compatibilidade total com TalkBack em fluxos críticos (Acertos e Contratos).

### FASE 4: Documentação e Polimento (Q4 2026)
*   **Documentação Técnica**: ⚠️ **NÃO INICIADO** - 100% das classes públicas com KDoc. Status atual: Parcial.
*   **Testes de Interface**: ⚠️ **NÃO INICIADO** - Automação de fluxos de ponta-a-ponta (E2E) com Espresso e Compose Test. Espresso já nas dependências, mas sem testes implementados.

---

## ✅ MARCOS CONCLUÍDOS

### Dezembro 2025
*   ✅ **Sync Engine**: Refatoração de SyncRepository para Handlers especializados e fix de rotas para não-admins.
*   ✅ **Padronização**: Implementação de `@SerializedName` em todas as entidades persistentes (174 campos).
*   ✅ **Segurança**: Firestore Rules enrijecidas e Multi-tenancy garantido.
*   ✅ **Cálculo Financeiro**: Fix do cálculo de média para relógios com defeito e 100% de cobertura em `FinancialCalculator`.
*   ✅ **Automatização**: Firebase Functions configuradas para gerenciamento de Custom Claims.
*   ✅ **Monitoramento**: Integração total com Crashlytics MCP para análise via IA.

### Janeiro 2026
*   ✅ **Crashlytics**: 4 erros corrigidos (DialogAditivoEquipamentosBinding, AditivoDialog, SyncRepository.mapType, JobCancellationException).
*   ✅ **Testes Unitários**: Todos os testes passando. 3 testes corrigidos (ConflictResolutionTest, ComprehensiveSyncTest).
*   ✅ **CancellationException**: Tratamento correto implementado em todos os 9 handlers principais.
*   ✅ **Release 1.0.1 (3)**: Deploy realizado com sucesso no Firebase App Distribution.
*   ✅ **Mapping.txt**: Gerado automaticamente no build de release para desofuscação de erros.
