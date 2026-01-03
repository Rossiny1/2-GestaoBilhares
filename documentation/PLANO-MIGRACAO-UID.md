# Plano de Migração: Schema Firestore por UID

## Problema Atual
- Colaboradores estão em `empresas/{empresaId}/entidades/colaboradores/items/{idAleatorio}`
- Busca por email via `collectionGroup("items")` é lenta e pode falhar
- Quando colaborador não é encontrado, o app faz `signOut()` automaticamente
- Desserialização do Firestore pode retornar null por problemas de mapeamento

## Solução Proposta

### Novo Schema
**Estrutura recomendada:**
```
empresas/{empresaId}/colaboradores/{firebaseUid}
```

**Vantagens:**
- Lookup direto por UID (O(1) ao invés de query)
- Não precisa de collectionGroup queries
- Mais eficiente e escalável
- Alinhado com Firebase best practices

### Estratégia de Migração

#### Fase 1: Suporte Dual (Compatibilidade)
- Manter schema antigo funcionando
- Adicionar novo schema em paralelo
- Durante login, tentar novo schema primeiro, fallback para antigo

#### Fase 2: Migração de Dados
- Script para migrar colaboradores existentes:
  - Ler de `items/{id}` 
  - Escrever em `colaboradores/{firebaseUid}`
  - Manter ambos durante período de transição

#### Fase 3: Remoção do Schema Antigo
- Após período de estabilidade, remover código do schema antigo
- Manter apenas `colaboradores/{uid}`

## Implementação

### 1. Novo Método: `buscarColaboradorPorUid(uid, empresaId)`
- Lookup direto: `firestore.collection("empresas").document(empresaId).collection("colaboradores").document(uid).get()`
- Retorna `Colaborador?` ou erro estruturado

### 2. Novo Método: `getOrCreateColaborador(uid, email, empresaId)`
- Tenta buscar por UID
- Se não existir, cria com dados mínimos:
  - `email`, `firebaseUid`, `nome` (do Firebase Auth), `aprovado: false`, `ativo: true`
- Usa transação ou `set()` com merge para evitar race conditions

### 3. Refatorar `login()`
- Após `signInWithEmailAndPassword` bem-sucedido:
  - Pegar UID do `result.user`
  - Chamar `getOrCreateColaborador(uid, email, empresaId)`
  - Se retornar colaborador, prosseguir com login
  - **NUNCA fazer signOut() se colaborador não existir** - criar automaticamente

### 4. Corrigir Desserialização
- Sempre logar `doc.reference.path` e `doc.data` antes de converter
- Usar `doc.toObject(Colaborador::class.java)` com cuidado
- Se falhar, usar Gson como fallback
- Retornar erro estruturado (sealed class) ao invés de null

### 5. Regras do Firestore
```javascript
match /empresas/{empresaId}/colaboradores/{uid} {
  // Usuário só pode ler/escrever seu próprio documento
  allow read: if request.auth != null && request.auth.uid == uid;
  allow write: if request.auth != null && request.auth.uid == uid;
  
  // Admin pode ler/escrever qualquer colaborador
  allow read, write: if isAdmin();
  
  // Permitir criação para onboarding (usuário autenticado pode criar seu próprio perfil)
  allow create: if request.auth != null && 
                 request.auth.uid == uid &&
                 request.resource.data.firebase_uid == uid;
}
```

## Benefícios
1. **Performance**: Lookup O(1) ao invés de query
2. **Confiabilidade**: Não depende de collectionGroup queries
3. **UX**: Não faz logout automático - cria perfil se necessário
4. **Manutenibilidade**: Código mais simples e direto
5. **Escalabilidade**: Alinhado com Firebase best practices
