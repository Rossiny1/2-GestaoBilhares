# 1️⃣ STATUS GERAL & SETUP

> **⚠️ IMPORTANTE**: Antes de ler este arquivo, leia PRIMEIRO: `.cursor/rules/0-PERFORMANCE-MAXIMA-OBRIGATORIO.md`  
> **Propósito**: Visão imediata do projeto, saúde técnica e primeiros passos.  
> **Última Atualização**: Janeiro 2026  
> **Versão**: 5.2 (Performance Máxima + Sync Orchestrator + Fix Rotas + Cobertura de Testes)  
> **Status**: ✅ **PRONTO PARA PRODUÇÃO**

---

## 📈 SAÚDE DO PROJETO
**Nota Geral: 9.2/10 ⭐⭐⭐⭐⭐**

| Critério | Nota | Comentário |
| :--- | :--- | :--- |
| **Arquitetura** | 9.8 | Modularização consolidada + Padrão Orchestrator Solidificado. |
| **Sincronização** | 9.8 | ✅ Fix Rotas (Bootstrap) + Padronização GSON (@SerializedName). |
| **Segurança** | 9.5 | Firestore Rules enrijecidas. Custom Claims ativas. |
| **Qualidade** | 9.8 | ✅ Suite de testes unitários robusta para Sync e Financeiro. Cobertura crescente. |
| **Produtividade** | 10.0 | Suporte total via IA com MCP Firebase/Crashlytics. |

---

## 🚀 SETUP RÁPIDO
```powershell
# 🔨 Build e Instalação (Debug)
./gradlew installDebug

# 🧹 Limpeza e Build
./gradlew clean assembleDebug

# 🧪 Testes
./gradlew test
```

## 🛠️ STATUS DOS MÓDULOS
1.  **`:sync`**: ✅ **ESTÁVEL**. Orchestrator e Handlers consolidados. Padronização de entidades concluída.
2.  **`:data`**: ✅ **ESTÁVEL**. AppRepository em processo de delegação. Entidades protegidas com `@SerializedName`.
3.  **`:ui`**: 🟡 **EM TRANSIÇÃO**. ~36% Compose.
4.  **`:core`**: ✅ **ESTÁVEL**. Utilitários e segurança consolidados. `FinancialCalculator` com 100% de cobertura.

## 🔗 MONITORAMENTO
*   [Firebase Console](https://console.firebase.google.com/project/gestaobilhares)
*   **MCP Crashlytics**: Ativo e configurado para análise via assistente.

---
## 📦 PRÓXIMAS FASES (RESUMO)
1. **Refatoração AppRepository & SyncRepository**: Delegar lógicas remanescentes (Meta: SyncRepository < 300 linhas).
2. **Expansão de Testes**: ✅ Handlers críticos (Cliente, Mesa, Acerto, Despesa, Contrato, Rota) cobertos. Resolvida serialização complexa e hierarquias. Estabilidade total garantida.
3. **Migração Compose**: Meta de 60% no Q2/2026.
