# 4️⃣ ROADMAP 2026

> **⚠️ IMPORTANTE**: Antes de ler este arquivo, leia PRIMEIRO: `.cursor/rules/0-PERFORMANCE-MAXIMA-OBRIGATORIO.md`  
> **Propósito**: Planejamento estratégico e fases pendentes de evolução.  
> **Última Atualização**: Janeiro 2026  
> **Versão**: 3.1

---

## 📅 FASES PENDENTES

### FASE 1: Consolidação da Arquitetura (Q1 2026) ✅ **CONCLUÍDO**
*   ✅ **Refatoração AppRepository**: Migração de métodos para repositories especializados concluída.
*   ✅ **Orchestrator Sync**: SyncRepository refatorado para Handlers especializados. Padronização de nomes de campos Firestore concluída.
*   ✅ **Testes de Cobertura**: 100% de cobertura em `FinancialCalculator`. Testes unitários para todos os `SyncHandlers`.
*   ✅ **Fix Sincronização**: Resolvido problema de acesso a rotas para não-admins via lógica de Bootstrap.

### FASE 2: Transição de Interface (Q2 2026)
*   **Aceleração Compose**: Migrar as 43 telas restantes de ViewBinding para Jetpack Compose (Meta: 60% concluído).
*   **Performance Visual**: Implementar compressão de imagens adaptativa no upload para o Firestore.

### FASE 3: Estabilidade e Acessibilidade (Q3 2026)
*   **Auditoria de Memória**: Uso do LeakCanary para eliminar vazamentos nos Fragments.
*   **Acessibilidade (A11y)**: Garantir compatibilidade total com TalkBack em fluxos críticos (Acertos e Contratos).

### FASE 4: Documentação e Polimento (Q4 2026)
*   **Documentação Técnica**: 100% das classes públicas com KDoc.
*   **Testes de Interface**: Automação de fluxos de ponta-a-ponta (E2E) com Espresso e Compose Test.

---

## ✅ MARCOS CONCLUÍDOS (DEZEMBRO 2025)
*   ✅ **Sync Engine**: Refatoração de SyncRepository para Handlers especializados e fix de rotas para não-admins.
*   ✅ **Padronização**: Implementação de `@SerializedName` em todas as entidades persistentes.
*   ✅ **Segurança**: Firestore Rules enrijecidas e Multi-tenancy garantido.
*   ✅ **Cálculo Financeiro**: Fix do cálculo de média para relógios com defeito e 100% de cobertura em `FinancialCalculator`.
*   ✅ **Automatização**: Firebase Functions configuradas para gerenciamento de Custom Claims.
*   ✅ **Monitoramento**: Integração total com Crashlytics MCP para análise via IA.
