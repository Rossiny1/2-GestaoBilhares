# 📋 ANÁLISE DE PATHS FIRESTORE - CÓDIGO FUNCIONAL vs ATUAL

## 🔍 **DESCOBERTA CRÍTICA: INCOMPATIBILIDADE ESTRUTURAL**

### **📊 RESUMO DAS ESTRUTURAS**

#### **🔴 ESTRUTURA ATUAL (Código App)**
```
empresas/{empresaId}/colaboradores/{uid}                    ✅ ColaboradorSyncHandler
empresas/{empresaId}/entidades/{collectionName}/items/{id}   ❌ BaseSyncHandler (clientes, rotas, etc)
```

#### **🟢 ESTRUTURA EXPECTIVA (Security Rules)**
```
empresas/{empresaId}/colaboradores/{uid}                    ✅
empresas/{empresaId}/entidades/{collectionName}/items/{id}   ✅
```

#### **🔥 PROBLEMA IDENTIFICADO**
- **Colaboradores**: ✅ **COMPATÍVEL** - Path idêntico em código e rules
- **Demais entidades**: ❌ **INCOMPATÍVEL** - Security Rules esperam estrutura hierárquica mas código escreve em paths diferentes

---

## 📂 **MAPEAMENTO EXATO DE OPERAÇÕES DE ESCRITA**

### **1. COLABORADORES ✅ COMPATÍVEL**

#### **Código (ColaboradorFirestoreRepository.kt):**
```kotlin
// Path: empresas/{empresaId}/colaboradores/{uid}
val docRef = firestore
    .collection("empresas")
    .document(empresaId)
    .collection("colaboradores")
    .document(uid)

// Operação: .set(colaboradorMap, SetOptions.merge())
```

#### **Security Rules:**
```javascript
match /empresas/{empresaId}/colaboradores/{colaboradorId} {
  allow create: if isAuthenticated() && 
               request.resource.data.firebase_uid == request.auth.uid &&
               request.resource.data.empresa_id == empresaId;
  allow update: if isAdmin(empresaId) || 
               (request.auth.uid == resource.data.firebase_uid && 
                !request.resource.data.diff(resource.data).affectedKeys()
                 .hasAny(['nivel_acesso', 'rotasPermitidas', 'aprovado', 'empresa_id']));
}
```

**Status:** ✅ **FUNCIONAL** - Paths e campos compatíveis

---

### **2. CLIENTES ❌ INCOMPATÍVEL**

#### **Código (BaseSyncHandler.kt):**
```kotlin
// Path: empresas/{empresaId}/entidades/clientes/items/{id}
protected fun getCollectionReference(
    firestore: FirebaseFirestore,
    collectionName: String,  // "clientes"
    companyId: String
): CollectionReference {
    return firestore
        .collection(COLLECTION_EMPRESAS)           // "empresas"
        .document(companyId)                       // {empresaId}
        .collection("entidades")                   // "entidades"
        .document(collectionName)                  // "clientes"
        .collection("items")                       // "items"
}
```

#### **Security Rules:**
```javascript
match /empresas/{empresaId}/entidades/{collectionName}/items/{itemId} {
  // CLIENTES: Regras específicas ausentes!
  // Apenas regra genérica para rotas existe
}
```

**Status:** ❌ **BLOQUEADO** - Security Rules não têm regras específicas para clientes

---

### **3. ROTAS ❌ INCOMPATÍVEL**

#### **Código (BaseSyncHandler.kt):**
```kotlin
// Path: empresas/{empresaId}/entidades/rotas/items/{id}
// Mesmo padrão de clientes
```

#### **Security Rules:**
```javascript
match /items/{itemId} {
  match /rotas/{rotaId} {
    allow read, write: if collectionName == "rotas" && (
      (request.method == 'get' && isApproved(empresaId)) ||
      (request.method != 'get' && isAdmin(empresaId))
    );
  }
}
```

**Status:** ❌ **BLOQUEADO** - Path correto mas regras muito restritivas (apenas admins podem escrever)

---

### **4. ACERTOS, MESAS, DESPESAS ❌ INCOMPATÍVEL**

#### **Código (BaseSyncHandler.kt):**
```kotlin
// Path: empresas/{empresaId}/entidades/{collectionName}/items/{id}
// Para: acertos, mesas, despesas, etc
```

#### **Security Rules:**
```javascript
// Ausência completa de regras para estas coleções
```

**Status:** ❌ **BLOQUEADO** - Sem regras definidas

---

## 🎯 **CAMPOS OBRIGATÓRIOS EXPECTOS**

### **Para Colaboradores ✅**
```javascript
// Campos validados nas Security Rules:
firebase_uid == request.auth.uid    ✅ Presente no código
empresa_id == empresaId             ✅ Presente no código
```

### **Para Demais Entidades ❌**
```javascript
// Campos necessários mas não validados:
rota_id                             ❌ Não validado nas rules
empresa_id                         ❌ Não validado nas rules
```

---

## 🔧 **ANÁLISE DE SYNC HANDLERS**

### **ColaboradorSyncHandler.kt ✅**
- **Path Customizado**: Sobrescreve `getColaboradoresCollectionReference()`
- **Path Direto**: `empresas/{empresaId}/colaboradores/{uid}`
- **Resultado**: ✅ **FUNCIONA**

### **ClienteSyncHandler.kt ❌**
- **Path Base**: Usa `getCollectionReference(COLLECTION_CLIENTES)`
- **Path Resultante**: `empresas/{empresaId}/entidades/clientes/items/{id}`
- **Resultado**: ❌ **BLOQUEADO**

### **RotaSyncHandler.kt ❌**
- **Path Base**: Usa `getCollectionReference(COLLECTION_ROTAS)`
- **Path Resultante**: `empresas/{empresaId}/entidades/rotas/items/{id}`
- **Resultado**: ❌ **BLOQUEADO**

---

## 📋 **PLANO DE CORREÇÃO**

### **OPÇÃO A: Corrigir Security Rules (RECOMENDADO)**

#### **1. Adicionar regras específicas para clientes:**
```javascript
match /items/{itemId} {
  // ═══════════════════════════════════════════════════════
  // CLIENTES
  // ═══════════════════════════════════════════════════════
  match /clientes/{clienteId} {
    allow read: if collectionName == "clientes" && 
                  isApproved(empresaId) && 
                  belongsToUserRoute(empresaId, request.resource.data.rota_id);
    
    allow write: if collectionName == "clientes" && 
                   (isAdmin(empresaId) || 
                    (isApproved(empresaId) && 
                     belongsToUserRoute(empresaId, request.resource.data.rota_id)));
  }
}
```

#### **2. Ajustar regras de rotas para permitir escrita por usuários aprovados:**
```javascript
match /rotas/{rotaId} {
  allow read: if collectionName == "rotas" && isApproved(empresaId);
  allow write: if collectionName == "rotas" && 
               (isAdmin(empresaId) || 
                (isApproved(empresaId) && 
                 belongsToUserRoute(empresaId, rotaId)));
}
```

#### **3. Adicionar regras para demais entidades:**
```javascript
// ACERTOS, MESAS, DESPESAS, etc
match /acertos/{acertoId} {
  allow read, write: if collectionName == "acertos" && 
                      (isAdmin(empresaId) || 
                       (isApproved(empresaId) && 
                        belongsToUserRoute(empresaId, request.resource.data.rota_id)));
}
```

### **OPÇÃO B: Modificar código (NÃO RECOMENDADO)**

#### **Mudar todos os sync handlers para usar paths diretos:**
```kotlin
// Em vez de: empresas/{empresaId}/entidades/clientes/items/{id}
// Usar: empresas/{empresaId}/clientes/{id}
```

**Problema:** Quebra compatibilidade com dados existentes

---

## 🎯 **CONCLUSÃO**

### **Problema Principal:**
1. **Colaboradores**: ✅ Funcionando (paths compatíveis)
2. **Demais entidades**: ❌ Bloqueadas (security rules incompletas/restritivas)

### **Solução Recomendada:**
- **OPÇÃO A**: Completar security rules para todas as entidades
- **Manter estrutura hierárquica** do código
- **Adicionar validação de rota_id** onde necessário
- **Permitir escrita por usuários aprovados** (não apenas admins)

### **Próximos Passos:**
1. Implementar correção das security rules
2. Testar sincronização incremental
3. Validar multi-tenancy por rota
4. Gerar relatório final

---

**Data:** 27/01/2026  
**Status:** 📋 Análise concluída - Pronto para correção cirúrgica
