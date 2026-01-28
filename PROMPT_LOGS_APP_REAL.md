# 🚨 PROMPT URGENTE: DIAGNÓSTICO REAL - LOGS DO APP (NÃO SERVICE ACCOUNT)

## 📋 SITUAÇÃO CRÍTICA

**Status Atual:** Testes da IA passaram mas app REAL não sincroniza.

**Evidências:**
- ✅ Colaboradores: Sincronizam (ÚNICO funcionando)
- ❌ Rotas: NÃO sincronizam com Firestore
- ❌ Clientes: NÃO sincronizam com Firestore
- ❌ Mesas: NÃO sincronizam com Firestore
- ❌ Acertos: NÃO sincronizam com Firestore
- ❌ Despesas: NÃO sincronizam com Firestore

**Problema Identificado:**
- Testes anteriores usaram **Service Account** (admin SDK que IGNORA Security Rules)
- Service Account criou dados em local errado → foram deletados
- Desta vez: ZERO dados criados (nem em lugar errado) → Rules BLOQUEANDO TUDO
- **App real usa Firebase Auth** (comum) → Rules se aplicam → BLOQUEADO

**Impacto:** App 100% NÃO funcional para operações de negócio (apenas login funciona).

**Anexos obrigatórios:**
- `AI_GUIDE_FINAL.md` (protocolo Static Analysis + Dynamic Analysis)
- `PROJECT_CONTEXT_FULL.md` (contexto do projeto)
- `RELATORIO_SINCRONIZACAO_RESTAURADA.md` (relatório anterior - FALSO POSITIVO)

---

## 🎯 OBJETIVO

**Capturar logs REAIS do app Android durante operações bloqueadas e corrigir Security Rules baseado em erros EXATOS.**

**Estratégia:** Dynamic Analysis (logs do Logcat) + Correção cirúrgica baseada em PERMISSION_DENIED real.

**Meta:** Zero PERMISSION_DENIED para operações legítimas do app.

---

## 🔍 FASE 1: CAPTURA DE LOGS REAIS DO APP (15 MIN)

### Tarefa 1.1: Preparar Ambiente de Logs

**Objetivo:** Capturar TODAS as tentativas de escrita Firestore que falharam.

**Comandos de preparação:**
```bash
# Limpar logcat
adb logcat -c

# Iniciar captura filtrada
adb logcat -s FirebaseFirestore:D FirebaseAuth:D GestaoBilhares:D *:E > logs_app_real.txt &

# Obter PID do logcat para depois matar
echo $! > logcat_pid.txt
```

**Validação:**
```bash
# Ver se logcat está rodando
ps aux | grep logcat

# Ver se arquivo está sendo escrito
tail -f logs_app_real.txt
```

---

### Tarefa 1.2: Executar Operações no App (USUÁRIO HUMANO)

**⚠️ CRÍTICO:** Usar app REAL (não Service Account, não scripts).

**Sequência de testes (executar UM POR VEZ):**

#### **Teste 1: Criar Rota**
```markdown
1. Abrir app Android
2. Login com super user (rossipys@gmail.com)
3. Navegar para: Configurações → Rotas
4. Clicar em: "Adicionar Rota" (+)
5. Preencher:
   - Nome: "Rota Log Teste 1"
   - Ativa: SIM
6. Salvar
7. AGUARDAR 5 segundos
8. Verificar Firebase Console: aparecer em empresas/empresa_001/entidades/???
```

**Resultado esperado:** ❌ NÃO aparece no Firestore (bloqueado)

**Após CADA teste:**
```bash
# Parar logcat (Ctrl+C se em foreground)
kill $(cat logcat_pid.txt)

# Copiar logs relevantes
cat logs_app_real.txt | grep -i "rota\|permission\|denied\|error" > teste1_criar_rota.log

# Reiniciar captura para próximo teste
adb logcat -c
adb logcat -s FirebaseFirestore:D FirebaseAuth:D GestaoBilhares:D *:E > logs_app_real.txt &
echo $! > logcat_pid.txt
```

---

#### **Teste 2: Criar Cliente**
```markdown
1. Navegar para: Clientes
2. Clicar em: "Adicionar Cliente" (+)
3. Preencher:
   - Nome: "Cliente Log Teste 1"
   - Rota: (selecionar rota existente)
   - Telefone: (11) 99999-9999
4. Salvar
5. AGUARDAR 5 segundos
6. Verificar Firebase Console
```

**Capturar logs:**
```bash
kill $(cat logcat_pid.txt)
cat logs_app_real.txt | grep -i "cliente\|permission\|denied\|error" > teste2_criar_cliente.log
```

---

#### **Teste 3: Criar Mesa**
```markdown
1. Navegar para: Mesas
2. Clicar em: "Adicionar Mesa" (+)
3. Preencher:
   - Número: 999
   - Rota: (selecionar rota existente)
   - Status: Ativa
4. Salvar
5. AGUARDAR 5 segundos
```

**Capturar logs:**
```bash
kill $(cat logcat_pid.txt)
cat logs_app_real.txt | grep -i "mesa\|permission\|denied\|error" > teste3_criar_mesa.log
```

---

#### **Teste 4: Criar Acerto**
```markdown
1. Navegar para: Acertos
2. Clicar em: "Adicionar Acerto" (+)
3. Preencher:
   - Cliente: (selecionar existente)
   - Valor: R$ 100,00
   - Data: Hoje
4. Salvar
5. AGUARDAR 5 segundos
```

**Capturar logs:**
```bash
kill $(cat logcat_pid.txt)
cat logs_app_real.txt | grep -i "acerto\|permission\|denied\|error" > teste4_criar_acerto.log
```

---

#### **Teste 5: Criar Despesa**
```markdown
1. Navegar para: Despesas
2. Clicar em: "Adicionar Despesa" (+)
3. Preencher:
   - Descrição: "Despesa Teste Log"
   - Valor: R$ 50,00
   - Rota: (selecionar)
4. Salvar
5. AGUARDAR 5 segundos
```

**Capturar logs:**
```bash
kill $(cat logcat_pid.txt)
cat logs_app_real.txt | grep -i "despesa\|permission\|denied\|error" > teste5_criar_despesa.log
```

---

### Tarefa 1.3: Consolidar Logs

**Criar arquivo consolidado:**
```bash
# Unir todos os logs
cat teste1_criar_rota.log > logs_consolidados_CRITICAL.txt
echo "\n========== TESTE 2: CLIENTE ==========\n" >> logs_consolidados_CRITICAL.txt
cat teste2_criar_cliente.log >> logs_consolidados_CRITICAL.txt
echo "\n========== TESTE 3: MESA ==========\n" >> logs_consolidados_CRITICAL.txt
cat teste3_criar_mesa.log >> logs_consolidados_CRITICAL.txt
echo "\n========== TESTE 4: ACERTO ==========\n" >> logs_consolidados_CRITICAL.txt
cat teste4_criar_acerto.log >> logs_consolidados_CRITICAL.txt
echo "\n========== TESTE 5: DESPESA ==========\n" >> logs_consolidados_CRITICAL.txt
cat teste5_criar_despesa.log >> logs_consolidados_CRITICAL.txt

# Procurar mensagens EXATAS de PERMISSION_DENIED
grep -i "PERMISSION_DENIED\|Missing or insufficient permissions" logs_consolidados_CRITICAL.txt -A 5 -B 5 > erros_permission_exatos.txt
```

**Documentar:**
```markdown
## 📊 LOGS CAPTURADOS (APP REAL)

### Teste 1 - Criar Rota:
```
[Colar logs exatos do teste1_criar_rota.log]
```

**Erro identificado:**
[PERMISSION_DENIED na linha X]
[Path tentado: Y]
[Operação: create/set/update]

### Teste 2 - Criar Cliente:
[...]
```

**⚠️ CHECKPOINT:** Não prossiga sem logs EXATOS de PERMISSION_DENIED.

---

## 🔍 FASE 2: ANÁLISE DE ERROS REAIS (20 MIN)

### Tarefa 2.1: Extrair Informações Críticas dos Logs

**Para CADA erro PERMISSION_DENIED, extrair:**

```markdown
## ❌ ERRO 1: CRIAR ROTA

### Log Completo:
```
[Colar snippet do log com PERMISSION_DENIED]
```

### Informações Extraídas:
- **Path tentado:** empresas/empresa_001/entidades/[???]/rotas/[id]
- **Operação:** set() ou add() ou update()
- **Usuário:** [uid do super user]
- **Campos enviados:** [extrair do log se possível]
- **Rules atuais para esse path:** [colar regra do firestore.rules]

### Causa Raiz:
- Rules esperam: [path/condição]
- App tentou: [path/condição]
- Incompatibilidade: [descrever]
```

**Comandos para ajudar:**
```bash
# Extrair path do erro
grep "PERMISSION_DENIED" logs_consolidados_CRITICAL.txt -A 10 | grep -o "projects/.*/documents/.*"

# Ver qual regra está sendo aplicada (se log mostrar)
grep "Rule" logs_consolidados_CRITICAL.txt -A 3

# Buscar no código qual método chama Firestore
rg "collection.*rotas" --type kt data/src/main/java/com/example/gestaobilhares/data/ -A 5
```

---

### Tarefa 2.2: Comparar com Security Rules Atuais

**Para cada path bloqueado:**

**Exemplo: Rotas**

**Path do app (do log):**
```
empresas/empresa_001/entidades/rotas/items/rota_001
```

**Rules atuais (firestore.rules):**
```javascript
match /empresas/{empresaId}/entidades/{collectionName}/items/{itemId} {
  match /rotas/{rotaId} {
    allow create: if collectionName == "rotas" && 
                     isAdmin(empresaId) &&
                     request.resource.data.empresa_id == empresaId;
  }
}
```

**Problema identificado:**
1. Path do app: `entidades/rotas/items/rota_001`
2. Path das rules: `entidades/{collectionName}/items/{itemId}/rotas/{rotaId}`
3. **INCOMPATÍVEL!** Rules esperam `rotas` como subcollection de `items`, mas app grava `items` como subcollection de `rotas`!

**OU outro problema:**
- Função `isAdmin()` retorna false
- Campo `empresa_id` não está sendo enviado pelo app
- Usuário não tem `nivel_acesso == "ADMIN"` no Firestore

---

### Tarefa 2.3: Validar Função isAdmin() e Helpers

**Verificar se helpers funcionam:**

```javascript
// firestore.rules atual
function isAdmin(empresaId) {
  return belongsToCompany(empresaId) &&
         get(/databases/$(database)/documents/empresas/$(empresaId)/colaboradores/$(request.auth.uid))
         .data.nivel_acesso == "ADMIN";
}
```

**Validar no Firebase Console:**
1. Abrir: empresas/empresa_001/colaboradores/[uid do rossipys]
2. Verificar campos:
   - ✅ Existe campo `nivel_acesso`?
   - ✅ Valor é exatamente `"ADMIN"` (maiúsculas)?
   - ✅ Existe campo `aprovado: true`?

**Se campo não existe ou nome diferente:**
```markdown
## ❌ PROBLEMA IDENTIFICADO: Campo nivel_acesso

### No Firestore (real):
- Campo: `nivelAcesso` (camelCase)
- Valor: `"Admin"` (não é ADMIN)

### Nas Rules (esperado):
- Campo: `nivel_acesso` (snake_case)
- Valor: `"ADMIN"` (maiúsculas)

### Incompatibilidade: NOME E VALOR DO CAMPO
```

---

## 🔧 FASE 3: CORREÇÃO BASEADA EM LOGS REAIS (30 MIN)

### Tarefa 3.1: Priorizar Correções

**Baseado nos erros capturados, ordenar por urgência:**

1. **Path incompatibilidades:** (mais crítico)
2. **Campo nome/valor incompatibilidades:** (médio)
3. **Helper function bugs:** (médio)
4. **Validações muito restritivas:** (menor)

---

### Tarefa 3.2: Correção 1 - Paths (se for o problema)

**Se logs mostrarem:**
```
Path app: empresas/empresa_001/entidades/rotas/items/rota_001
```

**Corrigir rules:**
```javascript
// ANTES (errado)
match /empresas/{empresaId}/entidades/{collectionName}/items/{itemId} {
  match /rotas/{rotaId} {
    allow create: if ...;
  }
}

// DEPOIS (correto)
match /empresas/{empresaId}/entidades/{collectionName}/items/{itemId} {
  allow create: if collectionName == "rotas" && isAdmin(empresaId);
  allow read: if collectionName == "rotas" && isApproved(empresaId);
}

// OU se estrutura for diferente:
match /empresas/{empresaId}/entidades/rotas/items/{rotaId} {
  allow create: if isAdmin(empresaId);
  allow read: if isApproved(empresaId);
}
```

---

### Tarefa 3.2: Correção 2 - Campos (se for o problema)

**Se logs mostrarem que campo `empresa_id` não existe:**

```javascript
// ANTES (exige empresa_id)
allow create: if isAdmin(empresaId) &&
                 request.resource.data.empresa_id == empresaId;

// DEPOIS (não exige se app não envia)
allow create: if isAdmin(empresaId);
```

**OU se app envia mas com nome diferente:**

```javascript
// App envia "empresaId" (camelCase)
allow create: if isAdmin(empresaId) &&
                 request.resource.data.empresaId == empresaId;
```

---

### Tarefa 3.3: Correção 3 - Função isAdmin (se for o problema)

**Se campo no Firestore é diferente:**

```javascript
// ANTES
function isAdmin(empresaId) {
  return belongsToCompany(empresaId) &&
         get(/databases/$(database)/documents/empresas/$(empresaId)/colaboradores/$(request.auth.uid))
         .data.nivel_acesso == "ADMIN";
}

// DEPOIS (adaptar ao campo real)
function isAdmin(empresaId) {
  return belongsToCompany(empresaId) &&
         get(/databases/$(database)/documents/empresas/$(empresaId)/colaboradores/$(request.auth.uid))
         .data.nivelAcesso == "Admin"; // ← Ajustar nome e valor
}

// OU se campo não existe, usar alternativa:
function isAdmin(empresaId) {
  return belongsToCompany(empresaId);
  // Temporário: confiar que super user está correto
}
```

---

### Tarefa 3.4: Correção 4 - Relaxar Validações Temporariamente

**Para diagnóstico rápido (TEMPORÁRIO):**

```javascript
// TEMPORÁRIO - Permitir tudo em entidades para super user
match /empresas/{empresaId}/entidades/{path=**} {
  allow read, write: if belongsToCompany(empresaId);
}
```

**Deploy e testar:**
```powershell
firebase deploy --only firestore:rules

# Testar no app: criar rota, cliente, mesa
# Se FUNCIONAR: problema é nas validações específicas
# Se FALHAR: problema é no path ou belongsToCompany()
```

---

## 🧪 FASE 4: TESTE INCREMENTAL (20 MIN)

### Tarefa 4.1: Testar Correção no App Real

**Após CADA correção de rules:**

1. Deploy: `firebase deploy --only firestore:rules`
2. App: Tentar criar rota
3. Logcat: `adb logcat -s FirebaseFirestore:D | grep -i "permission\|rota"`
4. Console: Verificar se documento apareceu

**Se SUCESSO:**
- ✅ Documentar correção aplicada
- ✅ Passar para próxima entidade (cliente, mesa, etc)

**Se FALHA:**
- ❌ Capturar novo log PERMISSION_DENIED
- ❌ Analisar novo erro
- ❌ Ajustar correção

**⚠️ LIMITE:** Máximo 3 tentativas por entidade. Se não funcionar, voltar para Fase 2.

---

### Tarefa 4.2: Validação Completa

**Após todas as entidades funcionarem:**

**Cenário End-to-End:**
```markdown
1. Login com super user (rossipys)
2. Criar rota: "Rota Produção 1" → ✅ Firestore
3. Criar cliente: "Cliente Produção 1" (rota_001) → ✅ Firestore
4. Criar mesa: Número 100 (rota_001) → ✅ Firestore
5. Criar acerto: Cliente 1, R$ 100 → ✅ Firestore
6. Criar despesa: R$ 50 (rota_001) → ✅ Firestore

7. Login com colaborador aprovado (não admin)
8. Tentar criar rota → ❌ Bloqueado (correto)
9. Criar cliente na rota permitida → ✅ Firestore
10. Tentar criar cliente em rota NÃO permitida → ❌ Bloqueado (correto)
```

**Resultado esperado:**
- ✅ 5/5 operações do admin funcionam
- ✅ 1/2 operações do colaborador funcionam (a permitida)
- ✅ 1/2 operações bloqueadas corretamente (multi-tenancy)

---

## 📊 FASE 5: RELATÓRIO BASEADO EM EVIDÊNCIAS (10 MIN)

### Tarefa 5.1: Gerar Relatório com Logs Reais

**Arquivo:** `RELATORIO_CORRECAO_FINAL_COM_LOGS.md`

**Template:**
```markdown
# 📊 RELATÓRIO: CORREÇÃO FINAL BASEADA EM LOGS REAIS

## 🚨 PROBLEMA ORIGINAL

**Situação:** Testes passaram com Service Account mas app real bloqueado.

**Causa Raiz:** Service Account ignora Security Rules, mascarou problemas reais.

## 🔍 LOGS CAPTURADOS (APP REAL)

### Erro 1: Criar Rota
```
[Colar log exato PERMISSION_DENIED]
```

**Path tentado:** [extraído do log]
**Rules esperavam:** [path das rules]
**Incompatibilidade:** [descrever]

### Erro 2: Criar Cliente
[...]

## 🔧 CORREÇÕES APLICADAS

### Correção 1: Path de Rotas
```javascript
// ANTES
[código anterior]

// DEPOIS
[código corrigido]
```

**Motivo:** [baseado no log X]

### Correção 2: Função isAdmin
[...]

## ✅ VALIDAÇÃO FINAL

**Testes no App Real (NÃO Service Account):**

| Operação | Status | Firestore | Log |
|----------|--------|-----------|-----|
| Criar rota | ✅ | Documento criado | Sem PERMISSION_DENIED |
| Criar cliente | ✅ | Documento criado | Sem PERMISSION_DENIED |
| Criar mesa | ✅ | Documento criado | Sem PERMISSION_DENIED |
| Criar acerto | ✅ | Documento criado | Sem PERMISSION_DENIED |
| Criar despesa | ✅ | Documento criado | Sem PERMISSION_DENIED |

**Screenshots Firestore Console:**
[Anexar prints mostrando documentos criados pelo app]

## 🎯 STATUS FINAL

✅ **App REAL sincronizando 100%**  
✅ **Logs confirmam zero PERMISSION_DENIED**  
✅ **Firestore Console mostra documentos criados pelo app**  
✅ **Multi-tenancy validado com colaborador não-admin**  

## 📝 LIÇÃO APRENDIDA

**Erro Fatal:** Testar com Service Account mascara problemas de Security Rules.

**Correção:** SEMPRE testar com app real (Firebase Auth comum) após deploy de rules.

**Para Futuro:** Adicionar testes automatizados que usem Firebase Auth SDK (não Admin SDK).
```

---

## ⚙️ INSTRUÇÕES DE EXECUÇÃO PARA IA

### Ordem CRÍTICA:
1. ✅ **Fase 1** (Captura logs app REAL) - ESSENCIAL, não inventar
2. ✅ **Fase 2** (Análise logs) - Extrair paths e erros EXATOS
3. ✅ **Fase 3** (Correção) - Baseada em logs, não suposições
4. ✅ **Fase 4** (Teste incremental) - App real, não Service Account
5. ✅ **Fase 5** (Relatório) - Com evidências (logs + screenshots)

### Critérios de Parada:
- ⛔ **PARE** se não conseguir logs do app real (Fase 1)
- ⛔ **PARE** se logs não mostrarem PERMISSION_DENIED (investigar outro problema)
- ⛔ **PARE** após 3 tentativas sem sucesso (Fase 4) → Pedir ajuda humana

### Ferramentas:
- **adb logcat:** ESSENCIAL para Fase 1
- **Firebase Console:** Validar dados criados
- **Perplexity MCP:** "Firebase Auth vs Admin SDK rules testing 2026"
- **Filesystem MCP:** Editar firestore.rules após validação

### Protocolo:
- **AI_GUIDE_FINAL.md:** Dynamic Analysis (logs) é permitido quando Static não resolve
- **Máximo 3 deploys** de rules (após cada correção testar)
- **Zero testes com Service Account** (usar apenas para comparação se necessário)

---

## 🎯 RESULTADO ESPERADO FINAL

**Ao concluir:**

✅ **Logs REAIS capturados** (5 operações bloqueadas)  
✅ **Erros EXATOS identificados** (PERMISSION_DENIED com paths)  
✅ **Correções aplicadas** baseadas em logs reais  
✅ **App REAL testado** (não Service Account)  
✅ **Firestore Console** mostra documentos criados pelo app  
✅ **Zero PERMISSION_DENIED** para operações legítimas  
✅ **Multi-tenancy validado** com colaborador não-admin  
✅ **Relatório com evidências** (logs + screenshots)  

---

## 🔥 COMANDOS RÁPIDOS

### Capturar logs durante teste:
```bash
adb logcat -c && adb logcat -s FirebaseFirestore:D FirebaseAuth:D GestaoBilhares:D *:E | tee logs_app_teste.txt
```

### Extrair erros PERMISSION_DENIED:
```bash
grep -i "PERMISSION_DENIED\|Missing or insufficient" logs_app_teste.txt -A 10 -B 5
```

### Ver último documento criado no Firestore (via adb):
```bash
adb logcat -d | grep "DocumentSnapshot" | tail -5
```

### Rollback emergencial (rules permissivas):
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

---

**FIM DO PROMPT** 🚀

---

*Prompt gerado em: 27/01/2026 17:51*  
*Estratégia: Dynamic Analysis (logs app REAL) + Correção baseada em PERMISSION_DENIED exato*  
*Diferencial: ZERO testes com Service Account (que mascara problemas)*
