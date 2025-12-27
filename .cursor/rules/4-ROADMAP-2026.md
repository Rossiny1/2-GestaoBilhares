# 4️⃣ ROADMAP 2026

> **Propósito**: Planejamento estratégico e fases pendentes de evolução.  
> **Última Atualização**: Dezembro 2025  
> **Versão**: 3.0

---

## 📅 FASES PENDENTES

### FASE 1: Consolidação da Arquitetura (Q1 2026) 🟡 **EM PROGRESSO**
*   **Refatoração AppRepository & SyncRepository**: Migrar métodos remanescentes de acesso a dados e sincronização para repositories especializados e Handlers.
*   **Orchestrator Sync**: Reduzir `SyncRepository.kt` de ~3.500 para ~300 linhas, movendo lógicas de Acertos, Clientes e Mesas para Handlers individuais.
*   **Testes de Cobertura**: Configurar JaCoCo consolidado e atingir 60% de cobertura unitária.
*   **Cleanup de Logs**: Finalizar migração de `android.util.Log` para `Timber` na camada UI.

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
*   ✅ **Sync Engine**: Refatoração de SyncRepository para Handlers especializados.
*   ✅ **Segurança**: Firestore Rules enrijecidas e Multi-tenancy garantido.
*   ✅ **Automatização**: Firebase Functions configuradas para gerenciamento de Custom Claims.
*   ✅ **Monitoramento**: Integração total com Crashlytics MCP para análise via IA.
