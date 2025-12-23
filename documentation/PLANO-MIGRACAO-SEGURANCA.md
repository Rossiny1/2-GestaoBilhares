# 🔒 PLANO DE MIGRAÇÃO DE SEGURANÇA - Firestore Rules

> **Data**: Janeiro 2025  
> **Objetivo**: Enrijecer as regras de segurança do Firestore sem quebrar a sincronização  
> **Status**: 🟡 EM ANDAMENTO

---

## ⚠️ PROBLEMA IDENTIFICADO

As coleções LEGADO (`ciclos`, `despesas`, `acertos`, `mesas`, `rotas`, `clientes`) têm regras com **fallback permissivo** que permite acesso quando o usuário não tem `companyId` no token:

```firestore
!('companyId' in request.auth.token) ||  // ⚠️ FALLBACK PERMISSIVO
```

**Risco**: Qualquer usuário autenticado pode acessar dados de qualquer empresa se não tiver claims configuradas.

---

## 📋 PLANO DE MIGRAÇÃO GRADUAL (3 FASES)

### ✅ FASE 1: PREPARAÇÃO (CONCLUÍDA)

**Objetivo**: Garantir que novas claims sejam criadas automaticamente

- [x] Atualizar `onUserCreated` para incluir `rotasAtribuidas`
- [x] Atualizar `onCollaboratorUpdated` para incluir `rotasAtribuidas`
- [x] Criar trigger `onColaboradorRotaUpdated` para atualizar claims quando rotas mudarem
- [x] Criar função `migrateUserClaims` para migrar usuários existentes
- [x] Criar função `validateUserClaims` para validar antes de remover fallbacks

**Status**: ✅ **CONCLUÍDO**

---

### 🟡 FASE 2: MIGRAÇÃO DE USUÁRIOS EXISTENTES (EM ANDAMENTO)

**Objetivo**: Atualizar claims de todos os usuários existentes

#### Passo 2.1: Deploy das Functions Atualizadas

```bash
cd functions
npm install
npm run build
firebase deploy --only functions
```

#### Passo 2.2: Executar Migração de Claims

**Opção A: Via Firebase Console (Recomendado)**
1. Acesse Firebase Console → Functions
2. Encontre a função `migrateUserClaims`
3. Execute via HTTP callable (pode usar Postman ou curl)

**Opção B: Via Script Node.js**

Criar arquivo `scripts/migrate-claims.js`:

```javascript
const admin = require('firebase-admin');
const serviceAccount = require('../path/to/serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

// Chamar a função migrateUserClaims
// (implementar chamada HTTP ou executar lógica diretamente)
```

#### Passo 2.3: Validar Migração

Execute a função `validateUserClaims` para verificar se todos os usuários têm claims:

```bash
# Via curl (substituir TOKEN pelo token de admin)
curl -X POST https://us-central1-gestaobilhares.cloudfunctions.net/validateUserClaims \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json"
```

**Critério de Sucesso**: 
- ✅ 100% dos usuários ativos devem ter `companyId` nas claims
- ✅ Usuários com rotas atribuídas devem ter `rotasAtribuidas` nas claims

**Tempo Estimado**: 30-60 minutos

---

### 🔴 FASE 3: REMOÇÃO GRADUAL DOS FALLBACKS (APENAS APÓS VALIDAÇÃO)

**⚠️ CRÍTICO**: Só executar esta fase após confirmar que 100% dos usuários têm claims configuradas.

#### Passo 3.1: Atualizar Firestore Rules (Versão Intermediária)

Criar versão intermediária que **adiciona logs** mas mantém fallback temporariamente:

```firestore
function hasCompanyAccess(empresaId) {
  return request.auth != null && (
    isAdmin() ||
    // ✅ FASE 3.1: Manter fallback mas adicionar validação
    (!('companyId' in request.auth.token) && 
     // Log de warning para identificar usuários sem claims
     debug("⚠️ Usuário sem companyId tentando acessar empresa " + empresaId)) ||
    // Se tem claim, verifica se pertence à empresa
    request.auth.token.companyId == empresaId
  );
}
```

**Deploy e Monitoramento**:
```bash
firebase deploy --only firestore:rules
```

**Monitorar por 24-48 horas**:
- Verificar logs do Firestore para identificar usuários sem claims
- Se houver usuários sem claims, executar migração novamente
- Verificar se sincronização continua funcionando normalmente

#### Passo 3.2: Remover Fallback (Versão Final)

**APENAS APÓS** confirmar que não há mais usuários sem claims:

```firestore
function hasCompanyAccess(empresaId) {
  return request.auth != null && (
    isAdmin() ||
    // ✅ FASE 3.2: Remover fallback - exigir companyId obrigatório
    ('companyId' in request.auth.token && 
     request.auth.token.companyId == empresaId)
  );
}
```

**Deploy**:
```bash
firebase deploy --only firestore:rules
```

**Monitoramento Crítico**:
- ⚠️ Monitorar Crashlytics por erros de PERMISSION_DENIED
- ⚠️ Verificar se sincronização continua funcionando
- ⚠️ Se houver problemas, reverter imediatamente para versão anterior

**Tempo Estimado**: 1-2 horas (incluindo monitoramento)

---

## 🧪 CHECKLIST DE VALIDAÇÃO ANTES DE REMOVER FALLBACKS

Antes de executar a Fase 3, verificar:

- [ ] Função `validateUserClaims` executada e mostra 100% de usuários com `companyId`
- [ ] Função `migrateUserClaims` executada com sucesso (0 falhas)
- [ ] Testes manuais de sincronização funcionando normalmente
- [ ] Nenhum erro de PERMISSION_DENIED nos logs do Firestore
- [ ] Todos os usuários ativos testaram login após migração
- [ ] Backup das regras atuais criado

---

## 🔄 PLANO DE REVERSÃO (EM CASO DE PROBLEMAS)

Se após remover fallbacks houver problemas:

### Reversão Imediata

1. **Reverter Firestore Rules**:
```bash
git checkout HEAD~1 firestore.rules
firebase deploy --only firestore:rules
```

2. **Investigar Problemas**:
   - Verificar logs do Firestore
   - Executar `validateUserClaims` novamente
   - Identificar usuários sem claims

3. **Corrigir e Re-executar Migração**:
   - Corrigir usuários sem claims
   - Re-executar `migrateUserClaims`
   - Validar novamente antes de tentar remover fallbacks

---

## 📊 MÉTRICAS DE SUCESSO

### Antes da Migração
- ⚠️ Regras com fallback permissivo
- ⚠️ Usuários podem acessar dados de qualquer empresa

### Após Fase 2 (Migração)
- ✅ 100% dos usuários ativos com `companyId` nas claims
- ✅ Sincronização funcionando normalmente
- ✅ Nenhum erro de PERMISSION_DENIED

### Após Fase 3 (Remoção de Fallbacks)
- ✅ Regras restritivas sem fallbacks permissivos
- ✅ Multi-tenancy garantido nas Security Rules
- ✅ Sincronização funcionando normalmente
- ✅ Zero erros de PERMISSION_DENIED

---

## 🚨 ALERTAS E MONITORAMENTO

### Durante Migração (Fase 2)
- Monitorar logs das Functions (`migrateUserClaims`)
- Verificar se claims estão sendo criadas corretamente
- Validar que usuários conseguem fazer login após migração

### Após Remoção de Fallbacks (Fase 3)
- ⚠️ **CRÍTICO**: Monitorar Crashlytics por erros de PERMISSION_DENIED
- ⚠️ Verificar logs do Firestore para tentativas de acesso negadas
- ⚠️ Testar sincronização manualmente com diferentes usuários
- ⚠️ Estar preparado para reverter imediatamente se necessário

---

## 📝 NOTAS IMPORTANTES

1. **Não pular etapas**: Cada fase deve ser concluída e validada antes de prosseguir
2. **Backup sempre**: Criar backup das regras antes de cada mudança
3. **Testes incrementais**: Testar com usuários reais após cada mudança
4. **Monitoramento contínuo**: Manter monitoramento por pelo menos 48 horas após remoção de fallbacks
5. **Comunicação**: Informar usuários sobre possíveis interrupções durante migração

---

## 🔗 REFERÊNCIAS

- [Firebase Custom Claims](https://firebase.google.com/docs/auth/admin/custom-claims)
- [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started)
- [Functions Documentation](./CONFIGURACAO-MCP-CRASHLYTICS.md)

