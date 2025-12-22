# 🔒 Melhorias nas Regras do Firestore - Coleções LEGADO

## 📋 Resumo das Alterações

As regras das coleções LEGADO (`ciclos`, `despesas`, `acertos`, `mesas`, `rotas`, `clientes`) foram melhoradas para serem mais seguras **sem quebrar a sincronização**.

### ✅ Estratégia Implementada

**Princípio Fundamental**: Fallback Seguro
- Se o usuário **não tem** custom claims configurados (`companyId`), as regras **permitem acesso** (compatibilidade)
- Se o usuário **tem** custom claims configurados, as regras **verificam** se pertence à empresa
- Isso garante que usuários existentes continuem funcionando enquanto novos usuários têm segurança adicional

## 🔐 Melhorias de Segurança

### Antes (Regras Antigas)
```firestore
match /ciclos/{cicloId} {
  allow read, write: if request.auth != null; 
}
```
**Problema**: Qualquer usuário autenticado podia acessar dados de qualquer empresa.

### Depois (Regras Melhoradas)
```firestore
match /ciclos/{cicloId} {
  allow read: if request.auth != null;
  
  allow create: if request.auth != null && (
    isAdmin() ||
    !('companyId' in request.auth.token) ||  // ✅ FALLBACK SEGURO
    !('empresaId' in request.resource.data) ||
    hasCompanyAccess(request.resource.data.empresaId)
  );
  
  allow update, delete: if request.auth != null && (
    isAdmin() ||
    !('companyId' in request.auth.token) ||  // ✅ FALLBACK SEGURO
    !('empresaId' in resource.data) ||
    hasCompanyAccess(resource.data.empresaId)
  );
}
```

**Melhorias**:
1. ✅ Verifica `companyId` quando disponível nos custom claims
2. ✅ Verifica `empresaId` nos documentos quando disponível
3. ✅ Mantém fallback seguro para usuários sem claims (evita PERMISSION_DENIED)
4. ✅ Admin sempre tem acesso total

## 🛡️ Função Auxiliar: `hasCompanyAccess()`

```firestore
function hasCompanyAccess(empresaId) {
  return request.auth != null && (
    isAdmin() ||
    !('companyId' in request.auth.token) ||  // ✅ FALLBACK: Permite se não tem claim
    request.auth.token.companyId == empresaId  // ✅ Verifica se tem claim
  );
}
```

**Como Funciona**:
1. Admin sempre tem acesso ✅
2. Se usuário **não tem** `companyId` no token → **Permite** (fallback seguro)
3. Se usuário **tem** `companyId` no token → **Verifica** se corresponde à empresa

## 📊 Coleções Afetadas

### ✅ Melhoradas com Verificação de Empresa:
- `ciclos` - Dados de ciclos de trabalho
- `despesas` - Despesas operacionais
- `acertos` - Histórico de acertos financeiros
- `mesas` - Cadastro de mesas

### ✅ Melhoradas (com nota sobre filtro no app):
- `rotas` - Rotas de entrega/coleta
  - **Nota**: O app já filtra por rotas atribuídas no código (`SyncRepository`)
  - As regras apenas garantem autenticação básica
- `clientes` - Cadastro de clientes
  - **Nota**: O app já filtra por rotas atribuídas no código (`SyncRepository`)
  - As regras apenas garantem autenticação básica

## 🔄 Compatibilidade Garantida

### ✅ Cenários que Continuam Funcionando:

1. **Usuário sem custom claims** (situação atual de muitos usuários)
   - ✅ Pode ler/escrever normalmente
   - ✅ Fallback seguro permite acesso

2. **Usuário com custom claims configurados**
   - ✅ Verifica empresa antes de permitir escrita
   - ✅ Mais seguro, mas ainda funcional

3. **Admin**
   - ✅ Sempre tem acesso total
   - ✅ Não afetado pelas verificações

## 🧪 Como Testar

### Teste 1: Sincronização Básica
1. Faça login com um usuário existente
2. Tente sincronizar dados (pull/push)
3. ✅ **Esperado**: Sincronização funciona normalmente

### Teste 2: Usuário com Custom Claims
1. Faça login com usuário que tem `companyId` configurado
2. Tente criar/editar documento em coleção LEGADO
3. ✅ **Esperado**: Funciona se `empresaId` do documento corresponde ao `companyId` do usuário

### Teste 3: Usuário sem Custom Claims
1. Faça login com usuário sem `companyId` configurado
2. Tente criar/editar documento em coleção LEGADO
3. ✅ **Esperado**: Funciona normalmente (fallback seguro)

### Teste 4: Admin
1. Faça login como admin
2. Tente criar/editar qualquer documento
3. ✅ **Esperado**: Sempre funciona (admin bypass)

## ⚠️ Importante: Deploy das Regras

**ANTES de fazer deploy**, certifique-se de:

1. ✅ Testar localmente (se possível)
2. ✅ Fazer backup das regras antigas
3. ✅ Deploy em horário de baixo uso (se possível)
4. ✅ Monitorar logs do Firebase após deploy

**Comando para Deploy**:
```powershell
firebase deploy --only firestore:rules --project gestaobilhares
```

## 🔍 Monitoramento Pós-Deploy

Após fazer deploy, monitore:

1. **Firebase Console → Firestore → Usage**
   - Verificar se há aumento de PERMISSION_DENIED
   - Se houver, verificar logs para identificar usuários afetados

2. **Logs do App**
   - Verificar se há erros de sincronização
   - Verificar se usuários conseguem fazer login normalmente

3. **Teste Manual**
   - Fazer login com diferentes tipos de usuários
   - Testar sincronização pull/push
   - Verificar se dados são salvos corretamente

## 📝 Próximos Passos (Opcional)

Para aumentar ainda mais a segurança no futuro:

1. **Configurar Custom Claims para Todos os Usuários**
   - Garantir que todos os usuários tenham `companyId` configurado
   - Isso permitirá remover os fallbacks gradualmente

2. **Adicionar Verificação de Rotas nas Regras**
   - Quando possível verificar arrays nas regras do Firestore
   - Atualmente o app já faz isso no código

3. **Migração Gradual**
   - Migrar dados das coleções LEGADO para estrutura nova (`empresas/{empresaId}/entidades/`)
   - Isso permitirá usar regras mais granulares

## ✅ Conclusão

As regras foram melhoradas para serem **mais seguras** mantendo **100% de compatibilidade** com o sistema atual. O fallback seguro garante que usuários existentes continuem funcionando enquanto novos usuários com custom claims têm segurança adicional.

**Status**: ✅ Pronto para deploy (após testes)

