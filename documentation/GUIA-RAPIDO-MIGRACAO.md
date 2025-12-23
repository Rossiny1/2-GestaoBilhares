# 🚀 GUIA RÁPIDO - Executar Migração de Claims

> **Objetivo**: Migrar claims de todos os usuários existentes  
> **Tempo Estimado**: 5-10 minutos  
> **Status**: ✅ Functions deployadas e prontas

---

## 📋 Passo a Passo

### 1️⃣ Acessar Firebase Console

1. Abra: https://console.firebase.google.com/project/gestaobilhares/functions
2. Você verá a lista de funções deployadas

### 2️⃣ Executar Migração

**Opção A: Via Interface do Console (Recomendado)**

1. **Clique na função `migrateUserClaims`** na lista
2. Isso abrirá a página de detalhes da função
3. Procure por uma das seguintes opções:
   - Aba **"Testing"** ou **"Testar"** no topo
   - Botão **"Testar função"** ou **"Invoke"**
   - Seção **"Testar"** ou **"Testing"** na lateral
4. No campo de dados, digite: `{}`
5. Clique em **"Executar"** ou **"Invoke"**
6. Aguarde a execução (pode levar alguns minutos)

**Opção B: Via Firebase CLI Shell**

```powershell
# Abrir shell
firebase functions:shell --project gestaobilhares

# Quando o shell abrir, digite:
migrateUserClaims({})

# Aguarde a execução
```

### 3️⃣ Validar Migração

Após a migração, execute a validação:

1. **Clique na função `validateUserClaims`** na lista
2. Execute com dados vazios: `{}`
3. Verifique os resultados:
   - ✅ **Sucesso**: `withoutCompanyId: 0` (todos têm claims)
   - ⚠️ **Atenção**: Se `withoutCompanyId > 0`, há usuários sem claims

### 4️⃣ Critérios de Sucesso

Antes de prosseguir para remover fallbacks, confirme:

- ✅ `validateUserClaims` mostra `withoutCompanyId: 0`
- ✅ Todos os usuários ativos têm `companyId` nas claims
- ✅ Sincronização no app funciona normalmente

---

## 🔍 Verificar Resultados

### Resultado Esperado da Migração

```json
{
  "total": 5,
  "success": 5,
  "failed": 0,
  "errors": []
}
```

### Resultado Esperado da Validação

```json
{
  "total": 5,
  "withCompanyId": 5,
  "withoutCompanyId": 0,
  "withoutClaims": 0
}
```

---

## ⚠️ Se Houver Erros

### Erro: "Colaborador sem email ou firebaseUid"

- **Causa**: Colaborador no Firestore sem `firebaseUid` configurado
- **Solução**: O colaborador precisa fazer login pelo menos uma vez para criar o usuário Auth

### Erro: "Usuário Auth não encontrado"

- **Causa**: `firebaseUid` no Firestore não corresponde a um usuário Auth
- **Solução**: Verificar se o email do colaborador corresponde ao email do usuário Auth

### Erro: "Permission denied"

- **Causa**: Função não tem permissão para atualizar claims
- **Solução**: Verificar se está logado como admin no Firebase

---

## 📞 Próximos Passos

Após confirmar que **100% dos usuários têm claims**:

1. ✅ Testar sincronização no app com diferentes usuários
2. ✅ Remover fallbacks permissivos das Firestore Rules
3. ✅ Monitorar por 24-48 horas

---

## 🔗 Referências

- [Plano Completo de Migração](./PLANO-MIGRACAO-SEGURANCA.md)
- [Firebase Console](https://console.firebase.google.com/project/gestaobilhares/functions)

