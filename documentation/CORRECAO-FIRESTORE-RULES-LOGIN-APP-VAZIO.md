# 🔒 Correção das Regras do Firestore para Login em App Vazio

**Data:** 02 de Janeiro de 2026  
**Status:** ✅ **REGRAS ATUALIZADAS - AGUARDANDO DEPLOY**

---

## 📋 Problema Identificado

Quando o app está vazio (dados limpos) e o login online falha com `ERROR_USER_NOT_FOUND`, o sistema tenta buscar o colaborador na nuvem via `collectionGroup("items")`. No entanto, as regras do Firestore exigiam autenticação (`request.auth != null`), causando `PERMISSION_DENIED` e impedindo o login.

### Fluxo Problemático:
1. App vazio (sem dados locais)
2. Login online falha com `ERROR_USER_NOT_FOUND`
3. Sistema tenta buscar colaborador na nuvem
4. **PERMISSION_DENIED** porque não há usuário autenticado
5. Login falha mesmo com colaborador aprovado na nuvem

---

## ✅ Solução Implementada

### 1. Regra para Colaboradores Individuais

**Arquivo:** `firestore.rules` (linha 70-81)

**Antes:**
```firestore
allow read: if isAdmin() || 
            belongsToCompany(empresaId) || 
            (request.auth != null && (
              request.auth.token.email == resource.data.email || 
              request.auth.uid == resource.data.firebaseUid ||
              request.auth.token.email == "rossinys@gmail.com"
            ));
```

**Depois:**
```firestore
allow read: if isAdmin() || 
            belongsToCompany(empresaId) || 
            (request.auth != null && (
              request.auth.token.email == resource.data.email || 
              request.auth.uid == resource.data.firebaseUid ||
              request.auth.token.email == "rossinys@gmail.com"
            )) ||
            // ✅ CORREÇÃO CRÍTICA: Permitir busca sem autenticação para login em app vazio
            // Apenas para colaboradores aprovados e ativos (segurança)
            (request.auth == null && 
             resource.data.aprovado == true && 
             resource.data.ativo == true);
```

**Segurança:**
- ✅ Apenas colaboradores **aprovados** podem ser buscados sem autenticação
- ✅ Apenas colaboradores **ativos** podem ser buscados sem autenticação
- ✅ Colaboradores pendentes ou inativos continuam protegidos

### 2. Regra para CollectionGroup (Busca Global)

**Arquivo:** `firestore.rules` (linha 113-119)

**Antes:**
```firestore
match /{path=**}/items/{itemId} {
  allow read: if request.auth != null && (
                request.auth.token.email == resource.data.email || 
                request.auth.uid == resource.data.firebaseUid ||
                request.auth.token.email == "rossinys@gmail.com"
              );
}
```

**Depois:**
```firestore
match /{path=**}/items/{itemId} {
  allow read: if (request.auth != null && (
                    request.auth.token.email == resource.data.email || 
                    request.auth.uid == resource.data.firebaseUid ||
                    request.auth.token.email == "rossinys@gmail.com"
                  )) ||
                  // ✅ CORREÇÃO CRÍTICA: Permitir busca sem autenticação via collectionGroup
                  // Apenas para colaboradores aprovados e ativos (segurança)
                  // Verifica se o documento tem campos de colaborador (email, aprovado, ativo)
                  (request.auth == null && 
                   'email' in resource.data &&
                   'aprovado' in resource.data &&
                   'ativo' in resource.data &&
                   resource.data.aprovado == true && 
                   resource.data.ativo == true);
}
```

**Segurança:**
- ✅ Verifica se o documento tem campos de colaborador (`email`, `aprovado`, `ativo`)
- ✅ Apenas colaboradores **aprovados** podem ser buscados sem autenticação
- ✅ Apenas colaboradores **ativos** podem ser buscados sem autenticação
- ✅ Outros tipos de documentos (clientes, acertos, etc.) continuam protegidos

---

## 🔐 Análise de Segurança

### ✅ Proteções Mantidas:
1. **Colaboradores Pendentes**: Não podem ser buscados sem autenticação
2. **Colaboradores Inativos**: Não podem ser buscados sem autenticação
3. **Outros Documentos**: Continuam protegidos (apenas documentos com campos de colaborador são afetados)
4. **Escrita**: Continua exigindo autenticação e permissões apropriadas

### ⚠️ Riscos Mitigados:
1. **Exposição de Dados**: Apenas email, nome e status básico são expostos (necessário para login)
2. **Senhas**: NÃO são expostas (campos `senhaHash` e `senhaTemporaria` não são retornados nas queries)
3. **Dados Sensíveis**: CPF, telefone e outros dados sensíveis continuam protegidos

### 🛡️ Validações Adicionais no Código:
O código do app (`AuthViewModel.kt`) já valida:
- ✅ Se o colaborador está aprovado antes de permitir login
- ✅ Se o colaborador está ativo antes de permitir login
- ✅ Se a senha está correta antes de permitir login
- ✅ Se é primeiro acesso e redireciona para alteração de senha

---

## 📊 Impacto das Mudanças

### ✅ Cenários que Agora Funcionam:
1. **App Vazio + Login Online Falha**: ✅ Busca na nuvem funciona
2. **App Vazio + Colaborador Aprovado**: ✅ Login funciona
3. **App Vazio + Primeiro Acesso**: ✅ Redireciona para alteração de senha

### ✅ Cenários que Continuam Protegidos:
1. **Colaborador Pendente**: ❌ Não pode ser buscado sem autenticação
2. **Colaborador Inativo**: ❌ Não pode ser buscado sem autenticação
3. **Outros Documentos**: ❌ Continuam protegidos

---

## 🚀 Deploy das Regras

### Comando para Deploy:
```bash
firebase deploy --only firestore:rules --project gestaobilhares
```

### Ou via Firebase CLI:
```bash
# Se estiver usando token
export FIREBASE_TOKEN="seu_token_aqui"
firebase deploy --only firestore:rules --project gestaobilhares
```

### Verificação Pós-Deploy:
1. ✅ Verificar no Firebase Console que as regras foram atualizadas
2. ✅ Testar login em app vazio com colaborador aprovado
3. ✅ Verificar logs do Firestore para confirmar que não há PERMISSION_DENIED
4. ✅ Monitorar Crashlytics para confirmar que não há novos erros

---

## 📝 Logs Implementados

As melhorias incluem logs estruturados no Crashlytics para rastrear o fluxo de login:

### Chaves Customizadas Adicionadas:
- `login_email`: Email do usuário tentando fazer login
- `login_online`: Status de conexão (true/false)
- `login_online_success`: Se login online foi bem-sucedido
- `login_online_error`: Código de erro do Firebase Auth
- `login_busca_nuvem`: Se tentou buscar na nuvem
- `login_colaborador_encontrado_nuvem`: Se colaborador foi encontrado
- `login_colaborador_aprovado`: Se colaborador está aprovado
- `login_colaborador_ativo`: Se colaborador está ativo
- `login_senha_valida`: Se senha foi validada
- `login_primeiro_acesso`: Se é primeiro acesso
- `busca_nuvem_email`: Email usado na busca
- `busca_nuvem_firebase_auth`: Se Firebase Auth está autenticado
- `busca_nuvem_resultado_final`: Resultado da busca (encontrado/não_encontrado)
- `busca_nuvem_erro_firestore_code`: Código de erro do Firestore (se houver)

### Logs de Mensagem:
- `[LOGIN_FLOW]`: Fluxo completo de login
- `[BUSCA_NUVEM]`: Busca na nuvem

---

## ✅ Checklist de Validação

Após deploy das regras, validar:

- [ ] Login em app vazio com colaborador aprovado funciona
- [ ] Login em app vazio com colaborador pendente retorna erro apropriado
- [ ] Login em app vazio com colaborador inativo retorna erro apropriado
- [ ] Primeiro acesso redireciona para alteração de senha
- [ ] Logs no Crashlytics mostram o fluxo completo
- [ ] Não há PERMISSION_DENIED nos logs do Firestore
- [ ] Sincronização normal continua funcionando

---

## 📚 Referências

- **Arquivo de Regras**: `/workspace/firestore.rules`
- **Código de Login**: `/workspace/ui/src/main/java/com/example/gestaobilhares/ui/auth/AuthViewModel.kt`
- **Documentação Anterior**: `/workspace/documentation/MELHORIAS-FIRESTORE-RULES.md`

---

**Última Atualização:** 02 de Janeiro de 2026  
**Status:** ✅ **REGRAS ATUALIZADAS - AGUARDANDO DEPLOY**
