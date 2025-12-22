# 4️⃣ ROADMAP & PRODUÇÃO

> **Propósito**: Planejamento estratégico para publicação e evolução do aplicativo em 2026.  
> **Última Atualização**: Janeiro 2025  
> **Versão**: 2.0 (Atualizada)

---

## ✅ CHECKLIST MESTRE DE PRODUÇÃO

### 1. Build e Configuração 🛡️
- [x] **Release Keystore**: Configurado via `keystore.properties`. ✅
- [x] **Minificação (R8/ProGuard)**: Build de release instalado e funcional. ✅
- [x] **Versionamento**: `versionCode` incrementado para 2. ✅

### 2. Segurança e Firebase 🔥
- [ ] **Firestore Rules**: Restringir coleções LEGADO (atualmente muito permissivas). ⚠️ **🔴 CRÍTICO - BLOQUEADOR**
- [x] **EncryptedSharedPreferences**: Implementado e funcionando. ✅ **CONCLUÍDO**
- [x] **Crashlytics/Analytics**: Fluxo de eventos reais verificado no console. ✅
- [x] **Índices Firestore**: Implantados via `firestore.indexes.json`. ✅
- [x] **Multi-tenancy**: Estrutura implementada (`empresas/{empresaId}/entidades/`). ✅

### 3. Qualidade de Código 🧹
- [ ] **Remover Logs**: 20+ arquivos ainda usam `android.util.Log` diretamente. ⚠️ **🟡 IMPORTANTE**
- [x] **printStackTrace()**: Removidas 100% das ocorrências remanescentes. ✅
- [x] **PII em Logs**: Timber configurado para não expor dados sensíveis em produção. ✅

---

## 📅 ROADMAP 2026

### Q1 (Jan-Mar): Segurança e Qualidade (PRIORIDADE CRÍTICA)
*   🔴 **Segurança Crítica**: Corrigir Firestore Rules das coleções LEGADO (BLOQUEADOR).
    *   **Tempo IA**: ~1-2 horas (configurar Custom Claims + atualizar regras)
*   ✅ **Segurança**: EncryptedSharedPreferences já implementado e funcionando.
*   🟡 **Logs**: Substituir todos os `android.util.Log` por Timber (ainda usado em ~10 arquivos).
    *   **Tempo IA**: ~30-60 minutos (substituição em paralelo em todos os arquivos)
*   ✅ **Unit Tests**: 49 testes configurados e passando.
*   🎯 **Cobertura de Testes**: Consolidar múltiplos módulos no JaCoCo (Alvo: 60%).
*   🏗️ **Refatoração**: Completar delegação de `AppRepository` para repositories especializados.
*   ✅ **Distribuição**: Firebase App Distribution configurado e pronto.

> **Nota**: Com implementação via IA, as correções críticas podem ser concluídas em **1.5-3 horas** ao invés de semanas.

### Q2 (Abr-Jun): Compose e Performance
*   🎨 **Compose**: Atingir 60% de migração das telas (atualmente ~35.8%).
*   ⚡ **Performance**: Otimização de queries Room e compressão de imagens.
*   🔗 **Firestore**: Migrar coleções LEGADO para estrutura multi-tenancy completa.
*   🚀 **CI/CD**: Implementar automação via GitHub Actions.

### Q3 (Jul-Set): Acessibilidade e Performance
*   ♿ **A11y**: Validação completa TalkBack e WCAG 2.1 AA.
*   ⚡ **Profiling**: Otimização de queries Room e compressão de imagens.
*   🔍 **LeakCanary**: Auditoria de memory leaks em todo o app.

### Q4 (Out-Dez): Documentação e Polimento
*   📖 **Documentação**: KDoc 100% completo em classes públicas.
*   🧪 **Testes de UI**: Automatizar fluxos críticos com Espresso/Compose Test.
*   🏁 **Finalização Compose**: Tentar atingir 90%+ de migração.

---

## 📦 DISTRIBUIÇÃO DO APP

### Firebase App Distribution
*   **Método de Publicação**: Firebase App Distribution (não Play Store)
*   **Escopo**: Uso interno para até 10 usuários
*   **Configuração**: Já configurado em `app/build.gradle.kts`
*   **Grupos de Testadores**: Configurado via Firebase Console
*   **Build Automático**: Pode ser configurado via CI/CD (GitHub Actions)

### Processo de Distribuição
1. Build de release: `./gradlew assembleRelease`
2. Upload via Firebase App Distribution: `firebase appdistribution:distribute app-release.apk --groups testers`
3. Testadores recebem link de download via email
4. Instalação direta no dispositivo (sem necessidade de Play Store)

## 📊 PROGRESSO ATUAL
A avaliação técnica sênior mantém a nota em **8.0/10**. 
O projeto está quase pronto para publicação, mas requer correções críticas de segurança antes do deploy.
- **Testes**: 49 unitários (100% sucesso). ✅
- **Logs/Traces**: ~10 arquivos ainda usam `android.util.Log` diretamente (SyncRepository, MainActivity, repositories domain, utils). ⚠️
- **Segurança**: Firestore Rules ainda permissivas (fallback). EncryptedSharedPreferences já implementado. 🔴
- **Arquitetura**: Modularização completa, mas AppRepository ainda precisa refatoração. 🟡
- **Distribuição**: Firebase App Distribution configurado. ✅

---

## 🔗 Referências Próximas
*   [5-BOAS-PRATICAS.md](file:///C:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/5-BOAS-PRATICAS.md)
*   [1-STATUS-GERAL.md](file:///C:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/1-STATUS-GERAL.md)
