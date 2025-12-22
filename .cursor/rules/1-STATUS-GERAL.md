# 1️⃣ STATUS GERAL & SETUP

> **Propósito**: Visão imediata do projeto, saúde técnica e primeiros passos.  
> **Última Atualização**: Janeiro 2025  
> **Versão**: 2.0 (Atualizada)  
> **Distribuição**: Firebase App Distribution (uso interno, máximo 10 usuários)

---

## 🚀 SETUP RÁPIDO

### Comandos Essenciais (PowerShell/CMD)
```powershell
# 🔨 Build e Instalação (Debug)
./gradlew installDebug

# 📦 Build de Release (para distribuição)
./gradlew assembleRelease

# 📤 Distribuir via Firebase App Distribution
firebase appdistribution:distribute app/build/outputs/apk/release/app-release.apk --groups testers

# 🧪 Rodar Todos os Testes
./gradlew test

# 📊 Verificar Cobertura
./gradlew testDebugUnitTestCoverage

# 🧹 Limpeza Profunda
./gradlew clean
```

### Links de Monitoramento (Firebase)
*   [Console Firebase](https://console.firebase.google.com/project/gestaobilhares)
*   [Crashlytics](https://console.firebase.google.com/project/gestaobilhares/crashlytics)
*   [Performance Monitoring](https://console.firebase.google.com/project/gestaobilhares/performance)
*   [App Distribution](https://console.firebase.google.com/project/gestaobilhares/appdistribution)

### 📦 Distribuição do App
*   **Método**: Firebase App Distribution (uso interno)
*   **Escopo**: Máximo 10 usuários
*   **Build Release**: `./gradlew assembleRelease`
*   **Upload**: `firebase appdistribution:distribute app-release.apk --groups testers`
*   **Nota**: App não será publicado na Play Store (uso interno apenas)

### Monitoramento via MCP Crashlytics
*   **MCP Configurado**: Servidor MCP do Firebase Crashlytics ativo
*   **Acesso via IA**: O assistente pode consultar crashes e problemas diretamente
*   **Documentação**: Ver `documentation/CONFIGURACAO-MCP-CRASHLYTICS.md`
*   **Exemplos de Uso**:
    - "Quais são os 10 problemas mais críticos no Crashlytics?"
    - "Analise o problema [ISSUE_ID] no Crashlytics"
    - "Mostre crashes da versão 1.0.0"

---

## 📈 SAÚDE DO PROJETO (AVALIAÇÃO SÊNIOR)

### Nota Geral: **8.0/10** ⭐⭐⭐⭐
> **Status**: Quase pronto para produção. Requer correções críticas de segurança antes do deploy.

| Critério | Nota | Comentário |
| :--- | :--- | :--- |
| **Arquitetura** | 9.0 | Modularização completa (5 módulos), Hilt DI, arquitetura híbrida. |
| **Sincronização** | 9.0 | Sistema incremental robusto com WorkManager. |
| **Monitoramento** | 9.0 | Crashlytics e Timber configurados. |
| **Qualidade de Código** | 7.5 | 20+ arquivos ainda usam `android.util.Log` diretamente. |
| **Testes** | 8.5 | 49 testes estáveis (100% sucesso). JaCoCo configurado. |
| **Segurança** | 6.0 | ⚠️ Firestore Rules permissivas em coleções LEGADO. |

### ✅ Pontos Fortes
*   **Modularização completa**: 5 módulos (`app`, `core`, `data`, `ui`, `sync`).
*   **Offline-first**: Room como fonte da verdade, sincronização incremental.
*   **Hilt DI**: 100% migrado, injeção de dependências moderna.
*   **Arquitetura híbrida**: AppRepository como Facade, repositories especializados em `domain/`.
*   **Sincronização**: Sistema incremental com 98% de economia de dados.
*   **Testes**: 49 testes unitários passando (100% sucesso).

### ⚠️ Áreas de Atenção Críticas
1.  **🔴 SEGURANÇA**: Firestore Rules das coleções LEGADO muito permissivas (qualquer usuário autenticado pode acessar dados de qualquer empresa).
2.  **🔴 SEGURANÇA**: Dados sensíveis em `SharedPreferences` padrão (deveria usar `EncryptedSharedPreferences`).
3.  **🟡 Logs**: 20+ arquivos ainda usam `android.util.Log` diretamente (deveria usar apenas Timber).
4.  **🟡 AppRepository**: ~1910 linhas (meta: 200-300 linhas como Facade). Repositories especializados existem mas não estão totalmente integrados.
5.  **🟡 Migração Compose**: ~35.8% concluída (43 telas pendentes).

---

## 🚨 ALERTAS DE PRODUÇÃO (AGORA)

> [!WARNING]
> **STATUS DE RELEASE: 🟡 QUASE PRONTO - REQUER CORREÇÕES CRÍTICAS**
> 
> ### 🔴 BLOQUEADORES CRÍTICOS:
> 1. **Firestore Rules**: Coleções LEGADO (`ciclos`, `despesas`, `acertos`, `mesas`, `rotas`, `clientes`) com regras muito permissivas.
> 2. **Segurança**: Dados sensíveis em `SharedPreferences` padrão (deveria usar `EncryptedSharedPreferences`).
> 
> ### 🟡 IMPORTANTE:
> 3. **Logs**: 20+ arquivos ainda usam `android.util.Log` diretamente.
> 4. **Testes**: 49/49 Passing ✅
> 5. **Stack Traces**: Removidos/substituídos por Timber ✅

---

## 🔗 Referências Próximas
*   [2-REGRAS-NEGOCIO.md](file:///C:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/2-REGRAS-NEGOCIO.md)
*   [3-ARQUITETURA.md](file:///C:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/3-ARQUITETURA.md)
