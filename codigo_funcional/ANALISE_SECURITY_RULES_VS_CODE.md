# 🔍 ANÁLISE DETALHADA: SECURITY RULES vs CÓDIGO

## 📊 **DIAGNÓSTICO COMPLETO DE INCOMPATIBILIDADES**

---

## 🎯 **RESUMO EXECUTIVO**

### **🔴 PROBLEMA CRÍTICO IDENTIFICADO**
As Security Rules estão **INCOMPLETAS** e **INCOMPATÍVEIS** com a estrutura de escrita do código para todas as entidades exceto colaboradores.

### **✅ ENTIDADES FUNCIONANDO**
- **Colaboradores**: Paths compatíveis, regras completas ✅

### **❌ ENTIDADES BLOQUEADAS**
- **Clientes**: Sem regras específicas ❌
- **Rotas**: Regras muito restritivas ❌
- **Acertos, Mesas, Despesas**: Sem regras ❌

---

## 📋 **ANÁLISE DETALHADA POR ENTIDADE**

### **1. COLABORADORES ✅ COMPATÍVEL**

#### **🔵 Código escreve em:**
```
empresas/{empresaId}/colaboradores/{uid}
```

#### **🔵 Security Rules permitem:**
```javascript
match /empresas/{empresaId}/colaboradores/{colaboradorId} {
  // ✅ CRIAÇÃO: Autenticado pode se registrar
  allow create: if isAuthenticated() && 
               request.resource.data.firebase_uid == request.auth.uid &&
               request.resource.data.empresa_id == empresaId;
  
  // ✅ LEITURA: Colaboradores da empresa
  allow read: if belongsToCompany(empresaId) || isAdmin(empresaId);
  
  // ✅ ATUALIZAÇÃO: Admin ou próprio usuário
  allow update: if isAdmin(empresaId) || 
               (request.auth.uid == resource.data.firebase_uid && 
                !request.resource.data.diff(resource.data).affectedKeys()
                 .hasAny(['nivel_acesso', 'rotasPermitidas', 'aprovado', 'empresa_id']));
}
```

#### **🔵 Campos validados:**
- `firebase_uid` ✅
- `empresa_id` ✅
- `aprovado` ✅
- `nivel_acesso` ✅

**Status:** ✅ **100% FUNCIONAL**

---

### **2. CLIENTES ❌ BLOQUEADO**

#### **🔴 Código escreve em:**
```
empresas/{empresaId}/entidades/clientes/items/{id}
```

#### **🔴 Security Rules atuais:**
```javascript
match /empresas/{empresaId}/entidades/{collectionName}/items/{itemId} {
  // ❌ NÃO EXISTEM REGRAS ESPECÍFICAS PARA CLIENTES
  // Apenas regra genérica para rotas existe
}
```

#### **🔴 Campos enviados pelo código:**
```kotlin
// BaseSyncHandler prepara:
data["rota_id"] = cliente.rotaId
data["empresa_id"] = empresaId  
data["nome"] = cliente.nome
data["debito_inicial"] = cliente.debitoInicial
```

#### **🔴 Problemas:**
1. **Sem regras específicas** para `clientes/{clienteId}`
2. **Sem validação** de `rota_id` (essencial para multi-tenancy)
3. **Sem validação** de `empresa_id`
4. **Resultado:** `PERMISSION_DENIED`

**Status:** ❌ **COMPLETAMENTE BLOQUEADO**

---

### **3. ROTAS ❌ BLOQUEADO**

#### **🔴 Código escreve em:**
```
empresas/{empresaId}/entidades/rotas/items/{id}
```

#### **🔴 Security Rules atuais:**
```javascript
match /items/{itemId} {
  match /rotas/{rotaId} {
    allow read, write: if collectionName == "rotas" && (
      // ❌ APENAS LEITURA para usuários aprovados
      (request.method == 'get' && isApproved(empresaId)) ||
      // ❌ APENAS ADMINS podem escrever
      (request.method != 'get' && isAdmin(empresaId))
    );
  }
}
```

#### **🔴 Problemas:**
1. **Apenas admins** podem escrever rotas
2. **Usuários aprovados** não podem criar/editar rotas
3. **Sem validação** de campos específicos de rotas
4. **Resultado:** `PERMISSION_DENIED` para não-admins

**Status:** ❌ **BLOQUEADO PARA NÃO-ADMINS**

---

### **4. ACERTOS ❌ BLOQUEADO**

#### **🔴 Código escreve em:**
```
empresas/{empresaId}/entidades/acertos/items/{id}
```

#### **🔴 Security Rules atuais:**
```javascript
// ❌ NÃO EXISTEM REGRAS PARA ACERTOS
// Nenhuma menção a "acertos" nas security rules
```

#### **🔴 Campos enviados:**
```kotlin
data["rota_id"] = acerto.rotaId
data["cliente_id"] = acerto.clienteId  
data["valor"] = acerto.valor
data["data_acerto"] = acerto.dataAcerto
```

#### **🔴 Problemas:**
1. **Ausência total** de regras para acertos
2. **Sem validação** de `rota_id` (multi-tenancy)
3. **Sem validação** de `cliente_id`
4. **Resultado:** `PERMISSION_DENIED`

**Status:** ❌ **COMPLETAMENTE BLOQUEADO**

---

### **5. MESAS ❌ BLOQUEADO**

#### **🔴 Código escreve em:**
```
empresas/{empresaId}/entidades/mesas/items/{id}
```

#### **🔴 Security Rules atuais:**
```javascript
// ❌ NÃO EXISTEM REGRAS PARA MESAS
// Nenhuma menção a "mesas" nas security rules
```

#### **🔴 Problemas:**
1. **Ausência total** de regras para mesas
2. **Sem validação** de campos
3. **Resultado:** `PERMISSION_DENIED`

**Status:** ❌ **COMPLETAMENTE BLOQUEADO**

---

### **6. DESPESAS ❌ BLOQUEADO**

#### **🔴 Código escreve em:**
```
empresas/{empresaId}/entidades/despesas/items/{id}
```

#### **🔴 Security Rules atuais:**
```javascript
// ❌ NÃO EXISTEM REGRAS PARA DESPESAS
// Nenhuma menção a "despesas" nas security rules
```

#### **🔴 Problemas:**
1. **Ausência total** de regras para despesas
2. **Sem validação** de campos
3. **Resultado:** `PERMISSION_DENIED`

**Status:** ❌ **COMPLETAMENTE BLOQUEADO**

---

## 🎯 **ANÁLISE DE HELPERS E VALIDAÇÃO**

### **✅ Helpers disponíveis nas Security Rules:**

```javascript
// ✅ isAuthenticated() - Verifica se usuário está autenticado
function isAuthenticated() {
  return request.auth != null;
}

// ✅ belongsToCompany() - Verifica pertencimento à empresa
function belongsToCompany(empresaId) {
  return isAuthenticated() && 
         exists(/databases/$(database)/documents/empresas/$(empresaId)/colaboradores/$(request.auth.uid));
}

// ✅ belongsToUserRoute() - Verifica acesso à rota
function belongsToUserRoute(empresaId, rotaId) {
  return belongsToCompany(empresaId) &&
         get(/databases/$(database)/documents/empresas/$(empresaId)/colaboradores/$(request.auth.uid))
         .data.rotasPermitidas.hasAny([rotaId]);
}

// ✅ isAdmin() - Verifica se é admin
function isAdmin(empresaId) {
  return belongsToCompany(empresaId) &&
         get(/databases/$(database)/documents/empresas/$(empresaId)/colaboradores/$(request.auth.uid))
         .data.nivel_acesso == "ADMIN";
}

// ✅ isApproved() - Verifica se está aprovado
function isApproved(empresaId) {
  return belongsToCompany(empresaId) &&
         get(/databases/$(database)/documents/empresas/$(empresaId)/colaboradores/$(request.auth.uid))
         .data.aprovado == true;
}
```

### **🔴 Problema: Helpers não estão sendo usados!**

Os helpers existem e estão corretos, mas **não estão sendo aplicados** às regras das entidades não-colaborador.

---

## 🔧 **PLANO DE CORREÇÃO DETALHADO**

### **📋 Estrutura necessária nas Security Rules:**

```javascript
match /empresas/{empresaId}/entidades/{collectionName}/items/{itemId} {
  
  // ═══════════════════════════════════════════════════════
  // CLIENTES
  // ═══════════════════════════════════════════════════════
  match /clientes/{clienteId} {
    allow read: if collectionName == "clientes" && 
                  isApproved(empresaId) && 
                  belongsToUserRoute(empresaId, request.resource.data.rota_id);
    
    allow create, update: if collectionName == "clientes" && 
                           (isAdmin(empresaId) || 
                            (isApproved(empresaId) && 
                             belongsToUserRoute(empresaId, request.resource.data.rota_id)));
    
    allow delete: if collectionName == "clientes" && isAdmin(empresaId);
  }
  
  // ═══════════════════════════════════════════════════════
  // ROTAS
  // ═══════════════════════════════════════════════════════
  match /rotas/{rotaId} {
    allow read: if collectionName == "rotas" && isApproved(empresaId);
    
    allow create, update: if collectionName == "rotas" && 
                          (isAdmin(empresaId) || 
                           (isApproved(empresaId) && 
                            belongsToUserRoute(empresaId, rotaId)));
    
    allow delete: if collectionName == "rotas" && isAdmin(empresaId);
  }
  
  // ═══════════════════════════════════════════════════════
  // ACERTOS
  // ═══════════════════════════════════════════════════════
  match /acertos/{acertoId} {
    allow read: if collectionName == "acertos" && 
                  isApproved(empresaId) && 
                  belongsToUserRoute(empresaId, request.resource.data.rota_id);
    
    allow create, update: if collectionName == "acertos" && 
                          (isAdmin(empresaId) || 
                           (isApproved(empresaId) && 
                            belongsToUserRoute(empresaId, request.resource.data.rota_id)));
    
    allow delete: if collectionName == "acertos" && isAdmin(empresaId);
  }
  
  // ═══════════════════════════════════════════════════════
  // MESAS
  // ═══════════════════════════════════════════════════════
  match /mesas/{mesaId} {
    allow read: if collectionName == "mesas" && 
                  isApproved(empresaId) && 
                  belongsToUserRoute(empresaId, request.resource.data.rota_id);
    
    allow create, update: if collectionName == "mesas" && 
                          (isAdmin(empresaId) || 
                           (isApproved(empresaId) && 
                            belongsToUserRoute(empresaId, request.resource.data.rota_id)));
    
    allow delete: if collectionName == "mesas" && isAdmin(empresaId);
  }
  
  // ═══════════════════════════════════════════════════════
  // DESPESAS
  // ═══════════════════════════════════════════════════════
  match /despesas/{despesaId} {
    allow read: if collectionName == "despesas" && 
                  isApproved(empresaId) && 
                  belongsToUserRoute(empresaId, request.resource.data.rota_id);
    
    allow create, update: if collectionName == "despesas" && 
                          (isAdmin(empresaId) || 
                           (isApproved(empresaId) && 
                            belongsToUserRoute(empresaId, request.resource.data.rota_id)));
    
    allow delete: if collectionName == "despesas" && isAdmin(empresaId);
  }
}
```

---

## 🎯 **CAMPOS OBRIGATÓRIOS PARA VALIDAÇÃO**

### **Para todas as entidades (exceto colaboradores):**
- `rota_id` ✅ (enviado pelo código, precisa ser validado)
- `empresa_id` ✅ (enviado pelo código, precisa ser validado)

### **Para clientes:**
- `nome` ✅
- `debito_inicial` ✅

### **Para acertos:**
- `cliente_id` ✅
- `valor` ✅
- `data_acerto` ✅

---

## 📋 **CONCLUSÃO DA ANÁLISE**

### **🔍 DIAGNÓSTICO FINAL:**

1. **Código está CORRETO** ✅
   - Paths consistentes
   - Campos adequados
   - Estrutura hierárquica mantida

2. **Security Rules estão INCOMPLETAS** ❌
   - Apenas colaboradores têm regras completas
   - Demais entidades sem regras específicas
   - Helpers existentes mas não utilizados

3. **Multi-tenancy não está sendo enforceado** ❌
   - `rota_id` não validado
   - `empresa_id` não validado

### **🎯 SOLUÇÃO:**
Completar as Security Rules com regras específicas para cada entidade, utilizando os helpers existentes e validando os campos obrigatórios.

---

**Data:** 27/01/2026  
**Status:** 📋 Análise concluída - Pronto para correção cirúrgica  
**Prioridade:** 🔴 **CRÍTICA** - Bloqueia toda sincronização exceto colaboradores
