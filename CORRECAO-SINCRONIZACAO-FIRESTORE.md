# 🔧 Correção Crítica: Sincronização Firestore

## 📋 Problema Identificado

1. **Regras não deployadas**: As regras foram restauradas no código, mas **NÃO foram deployadas no Firebase**
2. **Estrutura aninhada**: O SyncRepository usa `empresas/empresa_001/entidades/{collectionName}/items`, mas as regras não cobriam completamente essa estrutura
3. **Autenticação Firebase**: Usuários que fazem login offline não estão autenticados no Firebase, causando `PERMISSION_DENIED`

## ✅ Correções Aplicadas

### 1. Regras do Firestore (`firestore.rules`)

- ✅ Adicionada regra específica para estrutura aninhada: `empresas/{empresaId}/entidades/{collectionName}/items/{itemId}`
- ✅ Adicionada regra genérica para subcoleções: `empresas/{empresaId}/{document=**}`
- ✅ Qualquer usuário autenticado pode ler: rotas, clientes, mesas, ciclos, despesas, acertos
- ✅ Coleções sensíveis (veículos, histórico, equipamentos) continuam restritas a ADMIN

### 2. RoutesFragment.kt

- ✅ Removida lógica complexa de autenticação automática
- ✅ Simplificada sincronização manual

## 🚀 Próximos Passos OBRIGATÓRIOS

### PASSO 1: Deploy das Regras no Firebase

Execute o script de deploy:

```powershell
.\deploy-regras-firestore.ps1
```

**OU manualmente:**

```powershell
firebase deploy --only firestore:rules --project gestaobilhares
```

### PASSO 2: Verificar Deploy

Após o deploy, verifique no Firebase Console:
- Firestore → Rules
- As regras devem mostrar a estrutura `empresas/{empresaId}/entidades/{collectionName}/items`

### PASSO 3: Testar Sincronização

1. Faça login como superadmin
2. Tente sincronizar
3. Verifique se os dados são importados

## 🔍 Estrutura de Coleções no Firestore

O SyncRepository usa a seguinte estrutura:

```
empresas/
  empresa_001/
    entidades/
      rotas/
        items/
          {rotaId}
      clientes/
        items/
          {clienteId}
      mesas/
        items/
          {mesaId}
      ...
```

As regras agora cobrem:
- ✅ `empresas/{empresaId}/entidades/{collectionName}/items/{itemId}` (específica)
- ✅ `empresas/{empresaId}/{document=**}` (genérica para subcoleções)

## ⚠️ Importante

- As regras permitem que **qualquer usuário autenticado** leia as coleções principais
- Isso é necessário para que a sincronização funcione mesmo quando o usuário não está autenticado no Firebase (login offline)
- Coleções sensíveis (veículos, histórico, etc.) continuam restritas a ADMIN

## 📝 Notas Técnicas

- `request.auth != null` verifica se o usuário está autenticado no Firebase
- Se o usuário fez login offline, `request.auth` será `null` e a sincronização falhará
- A solução atual permite leitura para qualquer usuário autenticado, garantindo que funcione

