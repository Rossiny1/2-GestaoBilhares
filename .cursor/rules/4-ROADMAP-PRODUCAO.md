# 4️⃣ ROADMAP & PRODUÇÃO

> **Propósito**: Planejamento estratégico para publicação e evolução do aplicativo em 2026.  
> **Última Atualização**: Dezembro 2025  
> **Versão**: 3.0 (Atualizada - Segurança Crítica Resolvida)

---

## ✅ CHECKLIST MESTRE DE PRODUÇÃO

### 1. Build e Configuração 🛡️
- [x] **Release Keystore**: Configurado via `keystore.properties`. ✅
- [x] **Minificação (R8/ProGuard)**: Build de release instalado e funcional. ✅
- [x] **Versionamento**: `versionCode` incrementado para 2. ✅

### 2. Segurança e Firebase 🔥
- [x] **Firestore Rules**: Coleções LEGADO enrijecidas - fallbacks permissivos removidos. ✅ **CONCLUÍDO**
- [x] **Custom Claims**: Todos os usuários ativos migrados. Firebase Functions automatizam para novos usuários. ✅ **CONCLUÍDO**
- [x] **EncryptedSharedPreferences**: Implementado e funcionando. ✅ **CONCLUÍDO**
- [x] **Crashlytics/Analytics**: Fluxo de eventos reais verificado no console. ✅
- [x] **Índices Firestore**: Implantados via `firestore.indexes.json`. ✅
- [x] **Multi-tenancy**: Estrutura implementada e regras de segurança garantidas (`empresas/{empresaId}/entidades/`). ✅

### 3. Qualidade de Código 🧹
- [x] **Migração Timber**: Arquivos principais migrados (MainActivity, Repositories, Utils core). ✅ **CONCLUÍDO**
- [ ] **Logs UI**: Alguns arquivos UI ainda usam `android.util.Log` diretamente. ⚠️ **🟡 BAIXA PRIORIDADE**
- [x] **printStackTrace()**: Removidas 100% das ocorrências remanescentes. ✅
- [x] **PII em Logs**: Timber configurado para não expor dados sensíveis em produção. ✅
- [x] **Crashes Críticos**: Corrigidos (AditivoDialog, TypeToken/ProGuard, Crashlytics reporting). ✅

---

## 📅 ROADMAP 2026

### Q1 (Jan-Mar): Segurança e Qualidade ✅ **CONCLUÍDO**
*   ✅ **Segurança Crítica**: Firestore Rules das coleções LEGADO corrigidas. Custom Claims configuradas. ✅ **CONCLUÍDO**
    *   **Implementado**: Dezembro 2025 via MCP Firebase Auth
    *   **Firebase Functions**: Deployadas e automatizando claims para novos usuários
*   ✅ **Segurança**: EncryptedSharedPreferences já implementado e funcionando.
*   ✅ **Logs**: Migração para Timber concluída nos arquivos principais (MainActivity, Repositories, Utils core).
*   🟡 **Logs UI**: Alguns arquivos UI ainda pendentes (baixa prioridade, não crítico para produção).
*   ✅ **Unit Tests**: 49 testes configurados e passando.
*   🎯 **Cobertura de Testes**: Consolidar múltiplos módulos no JaCoCo (Alvo: 60%).
*   🏗️ **Refatoração**: Completar delegação de `AppRepository` para repositories especializados.
*   ✅ **Distribuição**: Firebase App Distribution configurado e pronto.

> **✅ Nota**: Correções críticas de segurança foram concluídas em Dezembro 2025. App agora está pronto para produção.

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
A avaliação técnica sênior atualizada para **9.0/10**. 
O projeto está **✅ PRONTO PARA PRODUÇÃO**. Todas as correções críticas de segurança foram implementadas.
- **Testes**: 49 unitários (100% sucesso). ✅
- **Logs/Traces**: ✅ Arquivos principais migrados para Timber. Alguns arquivos UI ainda pendentes (não crítico). ✅
- **Segurança**: ✅ Firestore Rules enrijecidas. Custom Claims configuradas. Multi-tenancy garantido. ✅
- **Arquitetura**: Modularização completa, mas AppRepository ainda precisa refatoração (melhoria futura). 🟡
- **Distribuição**: Firebase App Distribution configurado. ✅
- **Automação**: ✅ Firebase Functions deployadas para gerenciar claims automaticamente. ✅

---

## 🔗 Referências Próximas
*   [5-BOAS-PRATICAS.md](file:///C:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/5-BOAS-PRATICAS.md)
*   [1-STATUS-GERAL.md](file:///C:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/1-STATUS-GERAL.md)
