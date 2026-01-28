# 🚨 PROMPT DEFINITIVO: RESTAURAR SINCRONIZAÇÃO FIRESTORE (ARQUEOLOGIA DE CÓDIGO)

## 📋 SITUAÇÃO CRÍTICA ATUAL

**Status:** App NÃO sincroniza dados com Firestore (ZERO escritas funcionando).

**Evidências:**
- ✅ Apenas 1 colaborador sincronizado: `rossipys@gmail.com` (super user existente)
- ❌ Colaborador novo criado no app: NÃO aparece no Firestore
- ❌ Rota criada no app: NÃO aparece no Firestore
- ❌ Cliente criado no app: NÃO aparece no Firestore
- ℹ️ Único outro documento: `aNrpdE8HrCIdUISAtZ3LwXZWnrx1` criado pela IA nos testes (artificial)

**Impacto:** App 100% NÃO funcional para produção. Dados não salvam.

**Causa Provável:** Security Rules bloqueando TODAS as escritas do app real (apenas Service Account da IA passou).

**Anexos obrigatórios:**
- `AI_GUIDE_FINAL.md` (protocolo de trabalho - Static Analysis PRIMEIRO)
- `PROJECT_CONTEXT_FULL.md` (contexto completo do projeto)
- `RELATORIO_CORRECAO_SECURITY_RULES_HIERARQUICAS.md` (tentativa anterior)

---

## 🎯 OBJETIVO

**Restaurar sincronização 100% usando CÓDIGO QUE FUNCIONAVA ANTES das Security Rules.**

**Estratégia:** Arqueologia de código (git log) + comparação com rules atuais + correção cirúrgica.

**Meta:** Zero PERMISSION_DENIED para operações do app + Multi-tenancy funcionando.

---

## 🔍 FASE 1: ARQUEOLOGIA - CÓDIGO QUE FUNCIONAVA (20 MIN)

### Tarefa 1.1: Identificar Último Commit Funcional

**Objetivo:** Encontrar quando app AINDA SINCRONIZAVA antes das Security Rules.

**Comandos Git:**
```bash
# Ver histórico recente de commits
git log --oneline --graph --all -20 > git_history.txt

# Ver commits relacionados a Security Rules
git log --all --grep="security\|rules\|firestore" --oneline

# Ver últimas modificações no AppRepository
git log --oneline --follow -- data/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt | head -10

# Ver histórico de firestore.rules
git log --oneline --follow -- firestore.rules
```

**Documentar:**
```markdown
## 📊 COMMITS IDENTIFICADOS

### Último Commit ANTES das Security Rules:
- Hash: [commit hash]
- Data: [data]
- Mensagem: [mensagem]
- Autor: [autor]

### Commit que INTRODUZIU Security Rules:
- Hash: [commit hash]
- Data: 27/01/2026 (hoje)
- Mensagem: [mensagem relacionada a rules]
```

**⚠️ CHECKPOINT:** Confirme que identificou commit ANTES das rules.

---

### Tarefa 1.2: Extrair Código de Escrita Firestore Funcional

**Objetivo:** Ver EXATAMENTE como app gravava dados quando funcionava.

**Comandos:**
```bash
# Ver AppRepository no commit funcional
git show [HASH_COMMIT_FUNCIONAL]:data/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt > AppRepository_FUNCIONAL.kt

# Ver como criava colaboradores
git show [HASH_COMMIT_FUNCIONAL]:data/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt | grep -A 20 "criarColaborador\|insertColaborador\|adicionarColaborador"

# Ver como criava rotas
git show [HASH_COMMIT_FUNCIONAL]:data/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt | grep -A 20 "criarRota\|insertRota\|adicionarRota"

# Ver como criava clientes
git show [HASH_COMMIT_FUNCIONAL]:data/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt | grep -A 20 "criarCliente\|insertCliente\|adicionarCliente"
```

**Salvar arquivos para análise:**
```bash
# Extrair código completo funcional
git show [HASH]:data/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt > codigo_funcional/AppRepository.kt

# Extrair outros arquivos relevantes
git show [HASH]:data/src/main/java/com/example/gestaobilhares/data/entities/Colaborador.kt > codigo_funcional/Colaborador.kt
git show [HASH]:data/src/main/java/com/example/gestaobilhares/data/entities/Rota.kt > codigo_funcional/Rota.kt
```

---

### Tarefa 1.3: Mapear Paths Exatos de Escrita

**Objetivo:** Descobrir EXATAMENTE onde app gravava (paths completos).

**Análise do código funcional:**
```kotlin
// Procurar no AppRepository_FUNCIONAL.kt

// EXEMPLO DO QUE PROCURAR:
// firestore.collection("empresas")
//          .document(empresaId)
//          .collection("colaboradores")
//          .document(uid)
//          .set(colaborador)

// OU:
// firestore.collection("empresas")
//          .document(empresaId)
//          .collection("entidades")
//          .document("rotas") // ou collection("rotas")?
//          .collection("items") // ou direto no documento?
//          .document(rotaId)
//          .set(rota)
```

**Documentar paths EXATOS:**
```markdown
## 🗺️ PATHS DE ESCRITA (CÓDIGO FUNCIONAL)

### Colaborador:
- Path: empresas/{empresaId}/colaboradores/{uid}
- Método: set() ou add()?
- Campos obrigatórios: [listar campos]

### Rota:
- Path: empresas/{empresaId}/entidades/[???]/rotas/{rotaId}
- Estrutura: collection("rotas") ou document("rotas")?
- Subcollection "items"? SIM/NÃO

### Cliente:
- Path: empresas/{empresaId}/entidades/[???]/clientes/{clienteId}
- Estrutura: [descrever]

### Acerto:
- Path: empresas/{empresaId}/entidades/[???]/acertos/{acertoId}
- Estrutura: [descrever]
```

**⚠️ CRITICAL:** A estrutura `entidades/rotas` é:
- Opção A: `entidades` (collection) → `rotas` (documento) → `items` (subcollection) → `{id}` (documento)
- Opção B: `entidades` (documento) → `rotas` (subcollection) → `{id}` (documento)
- Opção C: Outra estrutura?

**CONFIRME com o código funcional!**

---

## 🔍 FASE 2: COMPARAR CÓDIGO ATUAL VS FUNCIONAL (15 MIN)

### Tarefa 2.1: Verificar se Código Mudou

**Objetivo:** Confirmar se app AINDA usa mesmos paths ou mudou.

**Comandos:**
```bash
# Ver AppRepository atual
cat data/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt > AppRepository_ATUAL.kt

# Diff entre funcional e atual
diff -u codigo_funcional/AppRepository.kt AppRepository_ATUAL.kt > diff_repository.txt

# Procurar métodos de escrita atuais
rg "fun.*colaborador" --type kt data/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt -A 10
rg "fun.*rota" --type kt data/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt -A 10
```

**Documentar mudanças:**
```markdown
## 🔄 COMPARAÇÃO CÓDIGO

### AppRepository.kt
- Funcional (commit anterior): [linhas relevantes]
- Atual: [linhas relevantes]
- Mudou? [SIM/NÃO]

### Se MUDOU:
- O que mudou: [descrever]
- Impacto: [descrever]

### Se NÃO MUDOU:
- Código idêntico: Problema é 100% nas Security Rules
```

---

### Tarefa 2.2: Validar Estrutura Firestore Real

**Objetivo:** Ver estrutura NO CONSOLE do Firebase (não assumir nada).

**Manual (você ou IA via screenshot):**
1. Abrir Firebase Console: https://console.firebase.google.com/project/gestaobilhares/firestore/data
2. Navegar: `empresas` → `empresa_001`
3. Verificar subcollections existentes:
   - ✅ `colaboradores` existe? (sim, tem rossipys)
   - ❓ `entidades` existe?
   - ❓ Se existe, é collection ou document?
   - ❓ Dentro de entidades: `rotas`, `clientes` são collections ou subcollections de um doc?

**Documentar estrutura REAL observada:**
```markdown
## 🏗️ ESTRUTURA FIRESTORE REAL (CONSOLE)

empresas/
  └─ empresa_001/ (document)
      ├─ colaboradores/ (subcollection) ✅ EXISTE
      │   ├─ ahxpd...x1 (rossipys - super user) ✅
      │   └─ aNrpd...x1 (teste IA) ✅
      └─ entidades/ (???)
          ├─ É collection? [SIM/NÃO]
          ├─ É document? [SIM/NÃO]
          └─ Contém: [listar]
```

---

## 🛡️ FASE 3: COMPARAR SECURITY RULES VS CÓDIGO (15 MIN)

### Tarefa 3.1: Analisar Rules Atuais vs Paths Reais

**Objetivo:** Identificar EXATAMENTE onde rules bloqueiam escrita do app.

**Analisar firestore.rules atual:**
```javascript
// REGRA ATUAL para colaboradores
match /empresas/{empresaId}/colaboradores/{colaboradorId} {
  allow create: if isAuthenticated() && 
                   request.resource.data.firebase_uid == request.auth.uid &&
                   request.resource.data.empresa_id == empresaId;
}

// PROBLEMA POSSÍVEL 1: Campo empresa_id
// App grava "empresa_id" ou "empresaId"?
// App grava esse campo?

// REGRA ATUAL para rotas
match /empresas/{empresaId}/entidades/{entidadeDoc}/rotas/{rotaId} {
  allow create: if isAdmin(empresaId);
}

// PROBLEMA POSSÍVEL 2: {entidadeDoc}
// App grava em "entidades/rotas/items/{id}" ou "entidades/rotas/{id}"?
// App grava em "entidades/{algumDoc}/rotas/{id}"?

// PROBLEMA POSSÍVEL 3: isAdmin()
// Função verifica campo correto?
// App define isAdmin antes de criar rota?
```

**Verificar campos gravados pelo app:**
```bash
# Ver entidade Colaborador
rg "data class Colaborador" --type kt -A 20

# Ver se tem campo empresa_id ou empresaId
rg "empresa.*id" --type kt data/src/main/java/com/example/gestaobilhares/data/entities/

# Ver entidade Rota
rg "data class Rota" --type kt -A 20
```

**Documentar incompatibilidades:**
```markdown
## ❌ INCOMPATIBILIDADES ENCONTRADAS

### 1. Campos de Colaborador:
- App grava: [campos reais do data class]
- Rules espera: firebase_uid, empresa_id
- Conflito: [SIM/NÃO]

### 2. Path de Rotas:
- App grava em: [path exato do código]
- Rules permitem em: empresas/{empresaId}/entidades/{entidadeDoc}/rotas/{rotaId}
- Match: [SIM/NÃO]

### 3. Função isAdmin:
- Verifica campo: nivel_acesso == "ADMIN"
- App define campo: [campo real]
- Match: [SIM/NÃO]

### 4. Função belongsToUserRoute:
- Verifica campo: rotasPermitidas.hasAny([rotaId])
- App define campo: [campo real]
- Match: [SIM/NÃO]
```

---

## 🔧 FASE 4: CORREÇÃO CIRÚRGICA (30 MIN)

### Tarefa 4.1: Decisão de Correção

**Baseado na análise acima, escolher abordagem:**

#### OPÇÃO A: Corrigir Security Rules (se código está certo)
- Rules estão incompatíveis com código funcional
- Ajustar rules para match com paths/campos reais

#### OPÇÃO B: Corrigir Código (se rules estão corretas)
- Código mudou após commit funcional
- Reverter ou ajustar para match com rules

#### OPÇÃO C: Rollback Completo (se muito complexo)
- Voltar ao commit funcional (git revert/reset)
- Reprojetar rules depois

**🎯 RECOMENDAÇÃO:** OPÇÃO A (corrigir rules) é mais seguro.

---

### Tarefa 4.2: Implementar Correção (OPÇÃO A)

**Baseado nas incompatibilidades identificadas:**

**Correção 1: Path de entidades**

Se app grava em `empresas/{empresaId}/entidades/rotas/items/{id}`:
```javascript
// ANTES (errado)
match /empresas/{empresaId}/entidades/{entidadeDoc}/rotas/{rotaId} {
  allow create: if isAdmin(empresaId);
}

// DEPOIS (correto)
match /empresas/{empresaId}/entidades/rotas/items/{rotaId} {
  allow create: if isAdmin(empresaId);
}
```

Se app grava em `empresas/{empresaId}/entidades/rotas/{id}`:
```javascript
match /empresas/{empresaId}/entidades/rotas/{rotaId} {
  allow create: if isAdmin(empresaId);
}
```

**Correção 2: Campos de Colaborador**

Se app NÃO grava `empresa_id`:
```javascript
// ANTES (errado)
allow create: if isAuthenticated() && 
                 request.resource.data.firebase_uid == request.auth.uid &&
                 request.resource.data.empresa_id == empresaId;

// DEPOIS (correto)
allow create: if isAuthenticated() && 
                 request.resource.data.firebase_uid == request.auth.uid;
                 // Remover validação de empresa_id se app não envia
```

**Correção 3: Função isAdmin**

Se campo no app é `nivel_acesso` (verificar):
```javascript
function isAdmin(empresaId) {
  return belongsToCompany(empresaId) &&
         get(/databases/$(database)/documents/empresas/$(empresaId)/colaboradores/$(request.auth.uid))
         .data.nivel_acesso == "ADMIN"; // ← Confirmar nome do campo
}
```

**Correção 4: Temporária - Relaxar Validações**

Para testar se paths estão corretos:
```javascript
// TEMPORÁRIO - Para diagnóstico
match /empresas/{empresaId}/colaboradores/{colaboradorId} {
  allow create: if isAuthenticated(); // Remover validações complexas
}

match /empresas/{empresaId}/entidades/{path=**} {
  allow read, write: if isAuthenticated(); // Permitir tudo em entidades
}
```

---

### Tarefa 4.3: Deploy e Teste Incremental

**Deploy rules corrigidas:**
```powershell
firebase deploy --only firestore:rules
```

**Teste 1: Criar Colaborador**
```bash
# 1. Abrir app
# 2. Criar novo usuário: teste2@example.com
# 3. Verificar Firebase Console: aparecer em colaboradores?
# 4. adb logcat | grep -i "permission\|firestore"
```

**Se FALHAR:**
```bash
# Ver logs exatos
adb logcat -s FirestoreSync:D FirebaseFirestore:D | grep -i "denied\|permission"

# Coletar mensagem EXATA do erro
# Ex: "PERMISSION_DENIED: Missing or insufficient permissions"
```

**Teste 2: Criar Rota (se Teste 1 passar)**
```bash
# 1. Login com super user (rossipys)
# 2. Criar rota no app
# 3. Verificar Firebase Console
```

**Teste 3: Criar Cliente (se Teste 2 passar)**

---

## 📊 FASE 5: VALIDAÇÃO COMPLETA (20 MIN)

### Tarefa 5.1: Testes Funcionais End-to-End

**Cenário 1: Fluxo Completo Novo Usuário**
1. Criar usuário novo no app
2. ✅ Aparecer em Firestore: `empresas/empresa_001/colaboradores/{uid}`
3. Admin aprovar usuário (rossipys)
4. ✅ Campo `aprovado: true` atualizado
5. Atribuir `rotasPermitidas: ["rota_001"]`
6. ✅ Campo atualizado

**Cenário 2: Fluxo Completo Rota**
1. Login com admin (rossipys)
2. Criar rota "Rota Teste"
3. ✅ Aparecer em Firestore: `empresas/empresa_001/entidades/.../rotas/{id}`
4. Logout
5. Login com usuário aprovado (teste2)
6. ✅ Rota aparece na lista

**Cenário 3: Fluxo Completo Cliente**
1. Login com usuário aprovado
2. Criar cliente na rota_001
3. ✅ Aparecer em Firestore: `empresas/empresa_001/entidades/.../clientes/{id}`
4. Campo `rotaId: "rota_001"` correto
5. Campo `usuarioCriadorId: {uid}` correto

---

## 📋 FASE 6: RELATÓRIO FINAL (10 MIN)

### Tarefa 6.1: Gerar Relatório Técnico

**Arquivo:** `RELATORIO_SINCRONIZACAO_RESTAURADA.md`

**Template:**
```markdown
# 📊 RELATÓRIO: SINCRONIZAÇÃO FIRESTORE RESTAURADA

## 🔍 ARQUEOLOGIA DE CÓDIGO

### Commit Funcional Identificado:
- Hash: [hash]
- Data: [data]
- Última modificação AppRepository: [data]

### Código de Escrita (FUNCIONAL):
```kotlin
[Colar métodos de escrita do commit funcional]
```

### Paths Confirmados:
- Colaborador: [path]
- Rota: [path]
- Cliente: [path]

## ❌ PROBLEMAS IDENTIFICADOS

### Incompatibilidade 1: [descrever]
- Rules esperavam: [path/campo]
- Código gravava: [path/campo]
- Correção: [descrever]

### Incompatibilidade 2: [descrever]
[...]

## ✅ CORREÇÕES APLICADAS

### firestore.rules:
```javascript
[Colar diff das correções]
```

## 🧪 TESTES REALIZADOS

| Teste | Status | Evidência |
|-------|--------|-----------|
| Criar colaborador | ✅ | Documento em Firestore |
| Criar rota | ✅ | Documento em Firestore |
| Criar cliente | ✅ | Documento em Firestore |
| Multi-tenancy | ✅ | Bloqueio funcionando |

## 🎯 STATUS FINAL

✅ **App sincronizando 100%**  
✅ **Paths corretos validados**  
✅ **Security Rules alinhadas com código**  
✅ **Multi-tenancy funcionando**  

## 📝 LIÇÃO APRENDIDA

**Erro Original:** Security Rules criadas sem analisar código funcional anterior.

**Correção:** Sempre fazer arqueologia de código (git log) ANTES de criar rules.

**Para Futuro:** Adicionar testes automatizados que validam rules vs código.
```

---

## ⚙️ INSTRUÇÕES DE EXECUÇÃO PARA IA

### Ordem OBRIGATÓRIA:
1. ✅ **Fase 1** (Arqueologia) - NUNCA pule, é a base de tudo
2. ✅ **Fase 2** (Comparação) - Confirmar código não mudou
3. ✅ **Fase 3** (Análise Rules) - Identificar incompatibilidades
4. ✅ **Fase 4** (Correção) - Aplicar apenas após Fases 1-3
5. ✅ **Fase 5** (Validação) - Testar tudo
6. ✅ **Fase 6** (Relatório) - Documentar

### Critérios de Parada:
- ⛔ **PARE** se não encontrar commit funcional (Fase 1)
- ⛔ **PARE** se paths do código funcional forem diferentes dos esperados (Fase 1)
- ⛔ **PARE** se código atual for diferente do funcional (Fase 2) → Confirmar estratégia
- ⛔ **PARE** após 2 deploys de rules sem sucesso → Pedir ajuda humana

### Ferramentas:
- **Git:** `git log`, `git show`, `git diff` (ESSENCIAL para Fase 1)
- **Perplexity MCP:** "Firestore security rules path matching 2026"
- **Filesystem MCP:** Editar firestore.rules após validação
- **Comandos:** `rg`, `firebase deploy`, `adb logcat`

### Protocolo:
- **Siga AI_GUIDE_FINAL.md:** Static Analysis PRIMEIRO (git log é static)
- **Máximo 2 builds:** Deploy rules apenas após confirmar paths
- **Zero suposições:** Tudo baseado em código real ou Console Firestore

---

## 🎯 RESULTADO ESPERADO FINAL

**Ao concluir este prompt:**

✅ Código funcional anterior identificado (commit hash)  
✅ Paths EXATOS de escrita mapeados do código  
✅ Incompatibilidades rules vs código documentadas  
✅ Security Rules corrigidas e deployadas  
✅ App sincronizando 100% (colaborador, rota, cliente)  
✅ Multi-tenancy funcionando por empresa + rota  
✅ Zero PERMISSION_DENIED para operações legítimas  
✅ Relatório técnico completo com evidências  

---

## 🔥 COMANDOS RÁPIDOS DE EMERGÊNCIA

### Se tudo falhar após Fase 4:

**Rollback para rules permissivas:**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

**Deploy emergencial:**
```powershell
firebase deploy --only firestore:rules
```

**Testar app:** Deve sincronizar 100%.

**Próximo passo:** Repetir Fases 1-3 com mais cuidado.

---

**FIM DO PROMPT** 🚀

---

*Prompt gerado em: 27/01/2026 14:03*  
*Estratégia: Arqueologia de código (git) + Comparação + Correção cirúrgica*  
*Baseado em: Falha total de sincronização após Security Rules*
