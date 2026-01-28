# 📚 DOCUMENTAÇÃO - GESTÃO DE BILHARES

> **Documentação centralizada do projeto**  
> **Base para trabalho com IA e desenvolvimento**  
> **Atualizada em:** 27/01/2026

---

## 🎯 OBJETIVO

Centralizar todo conhecimento do projeto para desenvolvimento eficiente e debugging científico.

---

## 📋 ESTRUTURA DE DOCUMENTOS

### 🥇 Documentos Principais (Leitura Obrigatória)

#### 1. **AI_GUIDE_FINAL.md** - Protocolo de Trabalho

- **Conteúdo:** Protocolo completo com Gates (0-4), Static/Dynamic Analysis
- **Quando usar:** Para TODAS as tarefas de desenvolvimento
- **Versão:** Final com MCP Integration (27/01/2026)

#### 2. **PROJECT_CONTEXT_FULL.md** - Contexto do Projeto

- **Conteúdo:** 500+ linhas com 34 ViewModels, 27 DAOs, stack completa
- **Quando usar:** Para entender arquitetura e encontrar componentes
- **Versão:** 1.0 (24/01/2026)

#### 3. **GUIA_DIAGNOSTICO_SENIOR_FINAL.md** - Metodologia de Debugging

- **Conteúdo:** Método científico adaptado ao desenvolvimento Android
- **Quando usar:** Para debugging preciso e evitar tentativa-erro
- **Versão:** Final (24/01/2026)

### 🥈 Documentos Complementares

#### 4. **FERRAMENTAS_MCP_HIERARQUIA.md** - Integração MCP

- **Conteúdo:** Hierarquia de ferramentas e fluxo de trabalho otimizado
- **Quando usar:** Para entender uso de Filesystem, Perplexity, GitKraken MCP
- **Versão:** 1.0 (27/01/2026)

#### 5. **README_MIGRACAO_V3.md** - Guia de Migração

- **Conteúdo:** Processos de migração e atualização
- **Quando usar:** Para mudanças estruturais

#### 6. **QUESTIONARIO_ONBOARDING.md** - Onboarding

- **Conteúdo:** Perguntas e respostas para novo desenvolvedor
- **Quando usar:** Para integração de novos membros

---

## 🚀 FLUXO DE TRABALHO RECOMENDADO

### Para IA e Desenvolvedores

```
1. PROBLEMA IDENTIFICADO
   ↓
2. LER AI_GUIDE_FINAL.md (protocolo)
   ↓
3. CONSULTAR PROJECT_CONTEXT_FULL.md (contexto)
   ↓
4. APLICAR GUIA_DIAGNOSTICO_SENIOR.md (método)
   ↓
5. USAR FERRAMENTAS_MCP_HIERARQUIA.md (ferramentas)
```

### Hierarquia de Uso

1. **🥇 Documentação .cursor/rules** (Sempre)
2. **🥈 Comandos** (Apenas quando necessário)
3. **🥉 Ferramentas MCP** (Operacional)

---

## 🎯 BENEFÍCIOS

### ⚡ Eficiência

- **10x mais rápido** encontrar informações
- **Diagnóstico preciso** em 10 minutos
- **Zero tempo perdido** em tentativa-erro

### 🎯 Precisão

- **Conhecimento específico** do projeto
- **Protocolos validados** em casos reais
- **Receitas testadas** para cada bug

### 🚀 Produtividade

- **Máximo 2 builds** por problema
- **Progresso mensurável**
- **Desenvolvimento sustentável**

---

## 📊 ESTATÍSTICAS DO PROJETO

### Stack Tecnológica

- **Kotlin:** 1.9.20
- **Android:** Compile 34, Min 24
- **Arquitetura:** MVVM + Hilt + Room + Firebase
- **Módulos:** 5 (app, core, data, sync, ui)

### Componentes Mapeados

- **ViewModels:** 34
- **DAOs:** 27
- **Entities:** 3+ principais
- **Use Cases:** 4
- **Repositories:** 22
- **Fragments:** 34
- **Adapters:** 33

---

## 🛑 REGRAS FUNDAMENTAIS

### ✅ Sempre

- Começar com AI_GUIDE_FINAL.md
- Consultar PROJECT_CONTEXT_FULL.md para contexto
- Usar GUIA_DIAGNOSTICO_SENIOR.md para debugging
- Seguir hierarquia de ferramentas

### ❌ Nunca

- Pular documentação e ir direto para comandos
- Fazer mais de 2 builds por problema
- Usar tentativa-erro sem diagnóstico
- Ignorar protocolos estabelecidos

---

## 🔄 MANUTENÇÃO

### Atualizações

- **Mensal:** Revisar PROJECT_CONTEXT_FULL.md
- **Trimestral:** Validar protocolos
- **Semestral:** Atualizar stack tecnológica

### Controle de Versão

- **Data:** 27/01/2026
- **Versão:** 1.0
- **Próxima revisão:** 27/02/2026

---

## �️ **ESTRUTURA DO BANCO FIRESTORE**

### **🏗️ Arquitetura Hierárquica Validada**

O Firestore utiliza estrutura hierárquica com multi-tenancy por empresa e rota:

```
empresas/
├── {empresaId}/
│   ├── colaboradores/
│   │   └── {uid}                    # Dados do colaborador (funcional)
│   └── entidades/
│       ├── {collectionName}/        # Nome da coleção: rotas, clientes, acertos, mesas, despesas
│       │   └── items/
│       │       ├── {itemId}         # Documentos reais (ESTRUTURA CORRIGIDA)
│       │       ├── {itemId}
│       │       └── {itemId}
│       └── {collectionName}/
│           └── items/
│               ├── {itemId}
│               └── {itemId}
```

### **📋 Paths Exatos do App**

#### **✅ Colaboradores (Funcionando)**

```
Path: empresas/{empresaId}/colaboradores/{uid}
Campos: firebase_uid, empresa_id, nivel_acesso, aprovado, rotasPermitidas
Status: 100% funcional
```

#### **✅ Entidades (Corrigido e Funcional)**

```
Path: empresas/{empresaId}/entidades/{collectionName}/items/{itemId}

Onde {collectionName} pode ser:
- rotas     → empresas/empresa_001/entidades/rotas/items/abc123
- clientes  → empresas/empresa_001/entidades/clientes/items/def456
- acertos   → empresas/empresa_001/entidades/acertos/items/ghi789
- mesas     → empresas/empresa_001/entidades/mesas/items/jkl012
- despesas  → empresas/empresa_001/entidades/despesas/items/mno345
```

### **🔐 Security Rules - Estrutura Corrigida**

As Security Rules foram corrigidas para corresponder à estrutura real:

```javascript
match /empresas/{empresaId}/entidades/{collectionName}/items/{itemId} {
  // ROTAS - Admin pode tudo, colaboradores aprovados podem ler
  allow read, write: if collectionName == "rotas" && (
    (request.method == 'get' && isApproved(empresaId)) ||
    (request.method in ['create', 'update', 'delete'] && isAdmin(empresaId))
  );
  
  // CLIENTES - Multi-tenancy por rota
  allow read: if collectionName == "clientes" && 
               isApproved(empresaId) && 
               belongsToUserRoute(empresaId, resource.data.rota_id);
  
  allow create: if collectionName == "clientes" && 
                 isApproved(empresaId) && 
                 belongsToUserRoute(empresaId, request.resource.data.rota_id) &&
                 request.resource.data.empresa_id == empresaId;
  
  // [Regras similares para acertos, mesas, despesas]
}
```

### **🎯 Particularidades Importantes**

#### **Multi-tenancy por Rota**

- **Campo obrigatório:** `rota_id` em todas as entidades (exceto colaboradores)
- **Validação:** `belongsToUserRoute(empresaId, rotaId)` verifica se rota está em `rotasPermitidas`
- **Isolamento:** Usuários veem apenas dados das rotas permitidas

#### **Campos Obrigatórios**

- **`empresa_id`:** Validado em todas as entidades para garantir isolamento
- **`rota_id`:** Necessário para controle de acesso por rota
- **`dataUltimaAtualizacao`:** Timestamp para resolução de conflitos

#### **Estrutura vs App**

- **App usa:** `BaseSyncHandler.getCollectionReference()` → constrói path correto
- **Rules esperam:** `collectionName` dinâmico + validações específicas
- **Compatibilidade:** 100% após correção estrutural

### **📊 Status Final (27/01/2026)**

| Entidade | Path | Security Rules | Status |
|----------|-------|-----------------|---------|
| Colaboradores | `empresas/{id}/colaboradores/{uid}` | ✅ Funcional | 100% OK |
| Rotas | `empresas/{id}/entidades/rotas/items/{id}` | ✅ Corrigido | 100% OK |
| Clientes | `empresas/{id}/entidades/clientes/items/{id}` | ✅ Corrigido | 100% OK |
| Acertos | `empresas/{id}/entidades/acertos/items/{id}` | ✅ Corrigido | 100% OK |
| Mesas | `empresas/{id}/entidades/mesas/items/{id}` | ✅ Corrigido | 100% OK |
| Despesas | `empresas/{id}/entidades/despesas/items/{id}` | ✅ Corrigido | 100% OK |

---

## �🛡️ **DEPLOY DE SECURITY RULES FIRESTORE**

### **📋 Métodos Disponíveis:**

- **`scripts/deploy-regras-firestore.ps1`** - PowerShell (requer login manual)
- **`import-data/deploy-security-rules-v2.js`** - Node.js (automatizado com Service Account)

### **🔧 Como Usar:**

```bash
# Método 1: PowerShell (requer login manual)
.\scripts\deploy-regras-firestore.ps1

# Método 2: Node.js (automatizado com Service Account)
node import-data/deploy-security-rules-v2.js
```

### **📝 Estrutura dos Scripts:**

- **Verificação** do Firebase CLI
- **Verificação** do login (PowerShell) / Service Account (Node.js)
- **Seleção** do projeto `gestaobilhares`
- **Deploy** das regras do arquivo `firestore.rules`
- **Criação** automática do `firebase.json` se não existir
- **Backup** automático das regras atuais (Node.js)

### **⚠️ Importante:**

- Ambos os métodos usam o projeto `gestaobilhares` automaticamente
- Requer arquivo `firestore.rules` na raiz do projeto
- Método PowerShell faz login automático se não estiver autenticado
- Método Node.js usa Service Account (mesmo do importador)
- Deploy apenas das regras (não afeta outros recursos)

### **🎯 Recomendação:**

- **Use Node.js** para consistência com o importador e automação completa
- **Use PowerShell** para deploy manual rápido
- **Ambos são 100% funcionais** e testados

---

## 🏆 OBJETIVO FINAL

> **"Documentação centralizada + protocolo científico = desenvolvimento eficiente e sustentável"**

Esta estrutura garante que qualquer IA ou desenvolvedor possa trabalhar no projeto com máximo conhecimento e mínima frustração.

---

*Documentação viva - Mantida pela equipe e IA*
