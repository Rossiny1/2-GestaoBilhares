# 4️⃣ ROADMAP & PRODUÇÃO

> **Propósito**: Planejamento estratégico para publicação e evolução do aplicativo em 2026.  
> **Última Atualização**: 18 Dezembro 2025  
> **Versão**: 1.0 (Consolidada)

---

## ✅ CHECKLIST MESTRE DE PRODUÇÃO

### 1. Build e Configuração 🛡️
- [x] **Release Keystore**: Configurado via `keystore.properties`. ✅
- [x] **Minificação (R8/ProGuard)**: Build de release instalado e funcional. ✅
- [x] **Versionamento**: `versionCode` incrementado para 2. ✅

### 2. Segurança e Firebase 🔥
- [ ] **Firestore Rules**: Restringir coleções LEGADO (atualmente `if auth != null`). ⚠️ **SEGURANÇA**
- [x] **Crashlytics/Analytics**: Fluxo de eventos reais verificado no console. ✅
- [x] **Índices Firestore**: Implantados via `firestore.indexes.json`. ✅

### 3. Qualidade de Código 🧹
- [x] **Remover Logs**: `android.util.Log` removido de todo o projeto. ✅
- [x] **printStackTrace()**: Removidas 100% das ocorrências remanescentes. ✅
- [x] **PII em Logs**: Garantido que nenhum dado sensível é exposto. ✅

---

## 📅 ROADMAP 2026

### Q1 (Jan-Mar): Qualidade e Base
*   ✅ **Correções de Produção**: 100% dos bloqueadores resolvidos.
*   ✅ **Unit Tests**: 49 testes configurados e passando.
*   🎯 **Cobertura de Testes**: Consolidar múltiplos módulos no JaCoCo (Alvo: 60%).
*   🏗️ **Refatoração**: Iniciar divisão de `AppRepository` em Facades.
*   🚀 **CI/CD**: Implementar automação via GitHub Actions.

### Q2 (Abr-Jun): Segurança e Compose
*   🛡️ **Segurança**: Implementar `EncryptedSharedPreferences` para tokens.
*   🎨 **Compose**: Atingir 60% de migração das telas.
*   🔗 **Firestore**: Trocar regras permissivas por Cloud Functions (Custom Claims).

### Q3 (Jul-Set): Acessibilidade e Performance
*   ♿ **A11y**: Validação completa TalkBack e WCAG 2.1 AA.
*   ⚡ **Profiling**: Otimização de queries Room e compressão de imagens.
*   🔍 **LeakCanary**: Auditoria de memory leaks em todo o app.

### Q4 (Out-Dez): Documentação e Polimento
*   📖 **Documentação**: KDoc 100% completo em classes públicas.
*   🧪 **Testes de UI**: Automatizar fluxos críticos com Espresso/Compose Test.
*   🏁 **Finalização Compose**: Tentar atingir 90%+ de migração.

---

## 📊 PROGRESSO ATUAL
A avaliação técnica sênior elevou a nota para **8.2/10**. 
O projeto está pronto para publicação técnica. Pendências agora são evolutivas (Compose/Arquitetura).
- **Testes**: 49 unitários (100% sucesso).
- **Logs/Traces**: 0 ocorrências de debug/leaks.

---

## 🔗 Referências Próximas
*   [5-BOAS-PRATICAS.md](file:///C:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/5-BOAS-PRATICAS.md)
*   [1-STATUS-GERAL.md](file:///C:/Users/Rossiny/Desktop/2-GestaoBilhares/.cursor/rules/1-STATUS-GERAL.md)
