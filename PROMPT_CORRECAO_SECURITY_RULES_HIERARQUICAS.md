# 🚨 PROMPT CRÍTICO: CORREÇÃO SECURITY RULES - ESTRUTURA HIERÁRQUICA

## 📋 CONTEXTO DO PROBLEMA

**Status Atual:** Security Rules implementadas bloqueando sincronização do app.

**Causa Raiz:** Rules criadas para collections flat (raiz) mas app usa estrutura hierárquica Firestore.

**Evidências:**
- Usuário criado no app NÃO aparece no Firestore
- Rota criada no app NÃO aparece no Firestore
- Collections de teste criadas INCORRETAMENTE na raiz: `clientes/`, `acertos/`, `rotas/`, `usuarios/`
- Estrutura CORRETA do projeto: `empresas/empresa_001/colaboradores/`, `empresas/empresa_001/entidades/rotas/`

**Impacto:** App 100% não funcional para operações de escrita (PERMISSION_DENIED).

**Anexos obrigatórios:**
- `AI_GUIDE_FINAL.md` (protocolo de trabalho)
- `PROJECT_CONTEXT_FULL.md` (contexto do projeto)
- `RELATORIO_SECURITY_RULES_IMPLEMENTACAO.md` (implementação incorreta)

---

## 🎯 OBJETIVO

Corrigir Security Rules para estrutura hierárquica real do projeto e restaurar funcionamento 100% do app.

**Meta:** Zero PERMISSION_DENIED para operações legítimas + Multi-tenancy funcionando.

---

## 🚀 FASE 1: ROLLBACK EMERGENCIAL (EXECUTAR IMEDIATAMENTE - 5 MIN)

### Tarefa 1.1: Criar Rules Permissivas Temporárias

**Arquivo:** `firestore.rules` (substituir conteúdo atual)

**Conteúdo:**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // ═══════════════════════════════════════════════════════
    // MODO EMERGÊNCIA: Restaurar funcionalidade do app
    // Permite TUDO para usuários autenticados
    // TEMPORÁRIO - Será substituído em 1 hora
    // ═══════════════════════════════════════════════════════

    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

**Deploy:**
```powershell
# Deploy emergencial
firebase deploy --only firestore:rules

# Validação
# Deve aparecer: "+ firestore: released rules firestore.rules to cloud.firestore"
```

**Critério de sucesso:**
- Deploy OK sem erros
- Backup automático das rules antigas disponível no Firebase Console

---

## 🔍 FASE 2: ANÁLISE DA ESTRUTURA REAL (15 MIN)

### Tarefa 2.1: Mapear Paths do Firestore no Código

**Comandos de busca:**
```bash
# Buscar onde app acessa Firestore
rg 'collection\("empresas"\)' --type kt -C 5 > estrutura_empresas.txt
rg 'collection\("colaboradores"\)' --type kt -C 5 > estrutura_colaboradores.txt
rg 'collection\("entidades"\)' --type kt -C 5 > estrutura_entidades.txt
rg 'collection\("rotas"\)' --type kt -C 5 > estrutura_rotas.txt
rg 'collection\("clientes"\)' --type kt -C 5 > estrutura_clientes.txt
rg 'collection\("acertos"\)' --type kt -C 5 > estrutura_acertos.txt
rg 'collection\("mesas"\)' --type kt -C 5 > estrutura_mesas.txt

# Consolidar
cat estrutura_*.txt > ANALISE_ESTRUTURA_FIRESTORE.txt
```

**Analisar e documentar:**
```markdown
## 📊 ESTRUTURA MAPEADA

### Path Completo para Colaboradores:
- Código: [colar linha exata do código]
- Path esperado: empresas/{empresaId}/colaboradores/{userId}

### Path Completo para Rotas:
- Código: [colar linha exata do código]
- Path esperado: empresas/{empresaId}/entidades/rotas/{rotaId}

### Path Completo para Clientes:
- Código: [colar linha exata do código]
- Path esperado: empresas/{empresaId}/entidades/clientes/{clienteId}

[... continuar para todas as collections]
```

**⚠️ IMPORTANTE:** Não prossiga para Fase 3 sem confirmar paths EXATOS.

---

## 🧹 FASE 3: LIMPEZA DE DADOS INCORRETOS (10 MIN)

### Tarefa 3.1: Remover Collections Criadas na Raiz

**Opção A: Firebase Console (Manual - RECOMENDADO)**
1. Abrir: https://console.firebase.google.com/project/gestaobilhares/firestore/data
2. Deletar collections na raiz (se existirem):
   - `clientes/` → Delete collection
   - `acertos/` → Delete collection
   - `mesas/` → Delete collection
   - `rotas/` → Delete collection
   - `usuarios/` → Delete collection

**Opção B: Script Node.js (Automático)**

**Arquivo:** `scripts/limpar-dados-teste-incorretos.js`

```javascript
const admin = require('firebase-admin');
const serviceAccount = require('../import-data/serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function deletarCollectionRaiz(collectionName) {
  const collectionRef = db.collection(collectionName);
  const snapshot = await collectionRef.get();

  if (snapshot.empty) {
    console.log(`✅ Collection ${collectionName} não existe ou já está vazia`);
    return;
  }

  console.log(`🗑️ Deletando ${snapshot.size} documentos de ${collectionName}...`);

  const batch = db.batch();
  snapshot.docs.forEach(doc => {
    batch.delete(doc.ref);
  });

  await batch.commit();
  console.log(`✅ Collection ${collectionName} deletada`);
}

async function limparDadosIncorretos() {
  console.log('🧹 Iniciando limpeza de dados de teste incorretos...\n');

  const collectionsParaDeletar = [
    'clientes',
    'acertos',
    'mesas',
    'rotas',
    'usuarios',
    'historico_manutencao'
  ];

  for (const collection of collectionsParaDeletar) {
    await deletarCollectionRaiz(collection);
  }

  console.log('\n✅ Limpeza concluída!');
  console.log('⚠️ Dados em empresas/empresa_001/* foram PRESERVADOS');
}

limparDadosIncorretos()
  .then(() => process.exit(0))
  .catch(error => {
    console.error('❌ Erro na limpeza:', error);
    process.exit(1);
  });
```

**Executar:**
```powershell
node scripts/limpar-dados-teste-incorretos.js
```

**Validação:**
- Abrir Firebase Console
- Verificar que collections na raiz foram removidas
- Verificar que `empresas/empresa_001/*` ainda existe (SE HOUVER DADOS)

---

## 🛡️ FASE 4: CRIAR SECURITY RULES HIERÁRQUICAS CORRETAS (30 MIN)

### Tarefa 4.1: Implementar Rules com Estrutura Completa

**Arquivo:** `firestore.rules` (substituir conteúdo emergencial)

**Conteúdo:**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // ═══════════════════════════════════════════════════════
    // HELPERS (Funções auxiliares)
    // ═══════════════════════════════════════════════════════

    function isAuthenticated() {
      return request.auth != null;
    }

    function belongsToCompany(empresaId) {
      // Verificar se usuário pertence à empresa
      return isAuthenticated() && 
             exists(/databases/$(database)/documents/empresas/$(empresaId)/colaboradores/$(request.auth.uid));
    }

    function belongsToUserRoute(empresaId, rotaId) {
      // Verificar se rota está nas permitidas do colaborador
      return belongsToCompany(empresaId) &&
             get(/databases/$(database)/documents/empresas/$(empresaId)/colaboradores/$(request.auth.uid))
             .data.rotasPermitidas.hasAny([rotaId]);
    }

    function isAdmin(empresaId) {
      // Verificar se colaborador é admin da empresa
      return belongsToCompany(empresaId) &&
             get(/databases/$(database)/documents/empresas/$(empresaId)/colaboradores/$(request.auth.uid))
             .data.nivel_acesso == "ADMIN";
    }

    function isApproved(empresaId) {
      // Verificar se colaborador está aprovado
      return belongsToCompany(empresaId) &&
             get(/databases/$(database)/documents/empresas/$(empresaId)/colaboradores/$(request.auth.uid))
             .data.aprovado == true;
    }

    // ═══════════════════════════════════════════════════════
    // COLLECTION: empresas (RAIZ)
    // ═══════════════════════════════════════════════════════

    match /empresas/{empresaId} {
      // Leitura da empresa: qualquer colaborador aprovado
      allow read: if isApproved(empresaId);

      // Escrita da empresa: apenas admins
      allow write: if isAdmin(empresaId);

      // ───────────────────────────────────────────────────
      // SUBCOLLECTION: colaboradores
      // ───────────────────────────────────────────────────

      match /colaboradores/{colaboradorId} {
        // Leitura: colaboradores da mesma empresa aprovados
        allow read: if belongsToCompany(empresaId) || isAdmin(empresaId);

        // Criação: qualquer autenticado pode se registrar (primeiro acesso)
        // O app cria com aprovado=false, admin aprova depois
        allow create: if isAuthenticated() && 
                         request.resource.data.firebase_uid == request.auth.uid &&
                         request.resource.data.empresa_id == empresaId;

        // Atualização: admin pode tudo, colaborador só pode alterar dados próprios (exceto campos críticos)
        allow update: if isAdmin(empresaId) || 
                         (request.auth.uid == resource.data.firebase_uid && 
                          !request.resource.data.diff(resource.data).affectedKeys().hasAny(['nivel_acesso', 'rotasPermitidas', 'aprovado', 'empresa_id']));

        // Deleção: apenas admins
        allow delete: if isAdmin(empresaId);
      }

      // ───────────────────────────────────────────────────
      // SUBCOLLECTION: entidades
      // ───────────────────────────────────────────────────

      match /entidades/{entidadeDoc} {

        // Leitura do documento entidades: colaboradores aprovados
        allow read: if isApproved(empresaId);

        // ─────────────────────────────────────────────
        // SUB-SUBCOLLECTION: rotas
        // ─────────────────────────────────────────────

        match /rotas/{rotaId} {
          // Leitura: colaborador aprovado com acesso à rota
          allow read: if (isApproved(empresaId) && belongsToUserRoute(empresaId, rotaId)) || 
                         isAdmin(empresaId);

          // Escrita: apenas admins podem criar/editar/deletar rotas
          allow create, update, delete: if isAdmin(empresaId);
        }

        // ─────────────────────────────────────────────
        // SUB-SUBCOLLECTION: clientes
        // ─────────────────────────────────────────────

        match /clientes/{clienteId} {
          // Leitura: colaborador aprovado com acesso à rota do cliente
          allow read: if isApproved(empresaId) && 
                         belongsToUserRoute(empresaId, resource.data.rotaId);

          // Criação: colaborador aprovado com acesso à rota
          allow create: if isApproved(empresaId) && 
                           belongsToUserRoute(empresaId, request.resource.data.rotaId) &&
                           request.resource.data.usuarioCriadorId == request.auth.uid;

          // Atualização: colaborador com acesso à rota E é o criador OU admin
          allow update: if (isApproved(empresaId) && 
                            belongsToUserRoute(empresaId, resource.data.rotaId) &&
                            resource.data.usuarioCriadorId == request.auth.uid) ||
                           isAdmin(empresaId);

          // Deleção: apenas admins
          allow delete: if isAdmin(empresaId);
        }

        // ─────────────────────────────────────────────
        // SUB-SUBCOLLECTION: acertos
        // ─────────────────────────────────────────────

        match /acertos/{acertoId} {
          // Leitura: colaborador aprovado com acesso à rota
          allow read: if isApproved(empresaId) && 
                         belongsToUserRoute(empresaId, resource.data.rotaId);

          // Criação: colaborador aprovado com acesso à rota
          allow create: if isApproved(empresaId) && 
                           belongsToUserRoute(empresaId, request.resource.data.rotaId) &&
                           request.resource.data.usuarioId == request.auth.uid;

          // Atualização: colaborador com acesso à rota E é o criador OU admin
          allow update: if (isApproved(empresaId) && 
                            belongsToUserRoute(empresaId, resource.data.rotaId) &&
                            resource.data.usuarioId == request.auth.uid) ||
                           isAdmin(empresaId);

          // Deleção: apenas admins
          allow delete: if isAdmin(empresaId);
        }

        // ─────────────────────────────────────────────
        // SUB-SUBCOLLECTION: mesas
        // ─────────────────────────────────────────────

        match /mesas/{mesaId} {
          // Leitura: colaborador aprovado com acesso à rota
          allow read: if isApproved(empresaId) && 
                         belongsToUserRoute(empresaId, resource.data.rotaId);

          // Escrita: colaborador aprovado com acesso à rota
          allow write: if isApproved(empresaId) && 
                          belongsToUserRoute(empresaId, request.resource.data.rotaId);
        }

        // ─────────────────────────────────────────────
        // SUB-SUBCOLLECTION: historico_manutencao
        // ─────────────────────────────────────────────

        match /historico_manutencao/{historicoId} {
          // Leitura: colaborador aprovado com acesso à rota
          allow read: if isApproved(empresaId) && 
                         belongsToUserRoute(empresaId, resource.data.rotaId);

          // Escrita: colaborador aprovado com acesso à rota
          allow write: if isApproved(empresaId) && 
                          belongsToUserRoute(empresaId, request.resource.data.rotaId);
        }

        // ─────────────────────────────────────────────
        // SUB-SUBCOLLECTION: panos
        // ─────────────────────────────────────────────

        match /panos/{panoId} {
          // Leitura: colaborador aprovado com acesso à rota
          allow read: if isApproved(empresaId) && 
                         belongsToUserRoute(empresaId, resource.data.rotaId);

          // Escrita: colaborador aprovado com acesso à rota
          allow write: if isApproved(empresaId) && 
                          belongsToUserRoute(empresaId, request.resource.data.rotaId);
        }

        // ─────────────────────────────────────────────
        // Outras subcollections em entidades (fallback)
        // ─────────────────────────────────────────────

        match /{anySubcollection}/{anyDoc} {
          // Leitura: colaboradores aprovados
          allow read: if isApproved(empresaId);

          // Escrita: apenas admins
          allow write: if isAdmin(empresaId);
        }
      }
    }

    // ═══════════════════════════════════════════════════════
    // FALLBACK: Negar tudo que não foi explicitamente permitido
    // ═══════════════════════════════════════════════════════

    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

**Validar sintaxe localmente (se possível):**
```powershell
# Se Firebase Emulator instalado
firebase emulators:start --only firestore

# Verificar erros de sintaxe
```

---

### Tarefa 4.2: Deploy Rules Hierárquicas

```powershell
# Deploy rules corretas
firebase deploy --only firestore:rules

# Validação
# Deve aparecer: "+ firestore: rules file firestore.rules compiled successfully"
```

**Critério de sucesso:**
- Compilação sem erros
- Deploy concluído
- Backup anterior disponível no Console

---

## 🧪 FASE 5: TESTES DE VALIDAÇÃO (30 MIN)

### Tarefa 5.1: Teste 1 - Criação de Colaborador (Primeiro Acesso)

**Cenário:** Usuário novo faz primeiro login no app

**Passos:**
1. Fazer logout no app
2. Criar novo usuário Firebase Auth: `teste@example.com` / senha123
3. Fazer login no app
4. App deve criar documento em: `empresas/empresa_001/colaboradores/{firebase_uid}`

**Validação:**
```bash
# Logcat deve mostrar
adb logcat -s FirestoreSync:D

# Firebase Console verificar
# empresas/empresa_001/colaboradores/{uid} existe?
# Campo aprovado = false
# Campo firebase_uid = {uid do Auth}
```

**Resultado esperado:**
- ✅ Documento criado em `empresas/empresa_001/colaboradores/`
- ✅ Sem erros PERMISSION_DENIED
- ✅ Campos: firebase_uid, aprovado=false, empresa_id, nome, email

---

### Tarefa 5.2: Teste 2 - Aprovação de Colaborador (Admin)

**Cenário:** Admin aprova colaborador novo

**Passos:**
1. Login com usuário admin existente
2. Navegar para tela de colaboradores pendentes
3. Aprovar usuário `teste@example.com`
4. Atribuir rota(s) permitida(s)

**Validação:**
```bash
# Firebase Console verificar
# empresas/empresa_001/colaboradores/{uid}
# Campo aprovado = true
# Campo rotasPermitidas = ["rota_001"]
```

**Resultado esperado:**
- ✅ Documento atualizado
- ✅ aprovado = true
- ✅ rotasPermitidas preenchido

---

### Tarefa 5.3: Teste 3 - Criação de Rota (Admin)

**Cenário:** Admin cria nova rota

**Passos:**
1. Login com admin
2. Navegar para tela de rotas
3. Criar nova rota: nome="Rota Teste"
4. Salvar

**Validação:**
```bash
# Firebase Console verificar
# empresas/empresa_001/entidades/rotas/{id}
# Documento existe
```

**Resultado esperado:**
- ✅ Rota criada em `empresas/empresa_001/entidades/rotas/`
- ✅ Campos: nome, ativa, data_criacao

---

### Tarefa 5.4: Teste 4 - Criação de Cliente (Colaborador Aprovado)

**Cenário:** Colaborador aprovado cria cliente em sua rota

**Passos:**
1. Login com `teste@example.com` (já aprovado)
2. Navegar para tela de clientes
3. Criar cliente: nome="Cliente Teste", rota=rota_001
4. Salvar

**Validação:**
```bash
# Firebase Console verificar
# empresas/empresa_001/entidades/clientes/{id}
# Campo usuarioCriadorId = {uid do teste@example.com}
# Campo rotaId = "rota_001"
```

**Resultado esperado:**
- ✅ Cliente criado em `empresas/empresa_001/entidades/clientes/`
- ✅ usuarioCriadorId correto
- ✅ rotaId correto

---

### Tarefa 5.5: Teste 5 - Bloqueio de Acesso a Outra Rota

**Cenário:** Colaborador tenta acessar cliente de rota SEM permissão

**Passos:**
1. Admin cria rota "rota_002"
2. Admin cria cliente em "rota_002"
3. Login com `teste@example.com` (só tem rota_001)
4. Tentar listar clientes

**Validação:**
```bash
# Logcat deve mostrar
# Cliente de rota_002 NÃO aparece na lista
# Sem PERMISSION_DENIED (query filtrada pelo app)
```

**Resultado esperado:**
- ✅ Cliente de rota_002 NÃO listado
- ✅ Cliente de rota_001 listado normalmente

---

### Tarefa 5.6: Teste 6 - Criação de Acerto

**Cenário:** Colaborador cria acerto na sua rota

**Passos:**
1. Login com colaborador aprovado
2. Navegar para tela de acertos
3. Criar acerto: cliente de rota_001, valor=100
4. Salvar

**Validação:**
```bash
# Firebase Console verificar
# empresas/empresa_001/entidades/acertos/{id}
# Campo usuarioId = {uid colaborador}
# Campo rotaId = "rota_001"
```

**Resultado esperado:**
- ✅ Acerto criado em `empresas/empresa_001/entidades/acertos/`
- ✅ usuarioId correto
- ✅ rotaId correto

---

## 📊 FASE 6: RELATÓRIO FINAL (15 MIN)

### Tarefa 6.1: Gerar Relatório de Correção

**Arquivo:** `RELATORIO_CORRECAO_SECURITY_RULES_HIERARQUICAS.md`

**Template:**
```markdown
# 📊 RELATÓRIO DE CORREÇÃO - SECURITY RULES HIERÁRQUICAS

> **Data:** 27/01/2026  
> **Executor:** Windsurf Cascade  
> **Status:** [SUCESSO/FALHA PARCIAL]

---

## 🚨 PROBLEMA ORIGINAL

**Sintoma:** Dados não sincronizavam do app para Firestore.

**Causa Raiz:** Security Rules criadas para structure flat mas app usa hierárquica.

**Estrutura Incorreta (implementada antes):**
```
clientes/ (raiz)
acertos/ (raiz)
rotas/ (raiz)
```

**Estrutura Correta (projeto real):**
```
empresas/
  └─ empresa_001/
      ├─ colaboradores/{uid}
      └─ entidades/
          ├─ rotas/{id}
          ├─ clientes/{id}
          └─ acertos/{id}
```

---

## ✅ CORREÇÕES IMPLEMENTADAS

### Fase 1: Rollback Emergencial
- [x] Rules permissivas temporárias deployadas
- [x] App restaurado funcionalmente
- [x] Tempo: 5 minutos

### Fase 2: Análise de Estrutura
- [x] Paths mapeados no código
- [x] Estrutura hierárquica confirmada
- [x] Documentação: ANALISE_ESTRUTURA_FIRESTORE.txt

**Paths Confirmados:**
```
[Colar paths exatos do código]
```

### Fase 3: Limpeza de Dados
- [x] Collections incorretas na raiz deletadas
- [x] Dados em empresas/empresa_001/* preservados
- [x] Firebase Console limpo

### Fase 4: Security Rules Hierárquicas
- [x] firestore.rules reescrito com hierarquia completa
- [x] Helpers implementados: belongsToCompany, belongsToUserRoute, isAdmin, isApproved
- [x] Rules deployadas com sucesso
- [x] Zero erros de compilação

### Fase 5: Testes de Validação

| Teste | Cenário | Resultado |
|-------|---------|-----------|
| 1 | Criação colaborador (primeiro acesso) | [✅/❌] |
| 2 | Aprovação de colaborador | [✅/❌] |
| 3 | Criação de rota (admin) | [✅/❌] |
| 4 | Criação de cliente | [✅/❌] |
| 5 | Bloqueio acesso outra rota | [✅/❌] |
| 6 | Criação de acerto | [✅/❌] |

**Detalhes dos Testes:**
[Colar logs e evidências de cada teste]

---

## 📊 MÉTRICAS

### Tempo de Execução:
- Rollback: 5 min
- Análise: 15 min
- Limpeza: 10 min
- Implementação: 30 min
- Testes: 30 min
- **Total:** ~1h30min

### Qualidade:
- Builds executados: [número]
- Erros encontrados: [número]
- Taxa de sucesso dos testes: [X/6 = Y%]

---

## ✅ STATUS FINAL

**Security Rules:** ✅ Hierárquicas funcionando  
**Multi-tenancy:** ✅ Por empresa + rota  
**App sincronização:** ✅ Criação/leitura OK  
**Controle de acesso:** ✅ Aprovação + rotas  

---

## 🚀 PRÓXIMOS PASSOS RECOMENDADOS

### Imediato (Próximas 24h):
1. Monitorar Firebase Console por violations
2. Testar com usuários reais em produção
3. Validar performance das rules (latência < 100ms)

### Curto Prazo (1 semana):
1. Implementar testes automatizados (Firebase Emulator)
2. Adicionar monitoramento de custos Firestore
3. Documentar guia de onboarding para novos colaboradores

### Médio Prazo (1 mês):
1. Expandir rules para outras subcollections futuras
2. Implementar auditoria de acessos
3. CI/CD para deploy automático de rules

---

## 📝 LIÇÕES APRENDIDAS

### ❌ Erros a Evitar:
1. **NUNCA** criar Security Rules sem mapear estrutura real do Firestore
2. **NUNCA** assumir structure flat sem verificar
3. **SEMPRE** testar rules com dados reais antes de deploy

### ✅ Boas Práticas Aplicadas:
1. Rollback imediato ao detectar problema
2. Análise de código ANTES de correção (Static Analysis)
3. Testes estruturados com cenários reais
4. Documentação completa do processo

---

## 🔗 ARQUIVOS GERADOS

1. `firestore.rules` - Rules hierárquicas corretas
2. `ANALISE_ESTRUTURA_FIRESTORE.txt` - Mapeamento de paths
3. `scripts/limpar-dados-teste-incorretos.js` - Script de limpeza
4. Este relatório

---

## 📞 SUPORTE

Se houver problemas após deploy:

**Rollback imediato:**
```powershell
# Firebase Console > Firestore > Rules > "Restore previous version"
# Ou usar backup automático
```

**Logs para análise:**
```bash
adb logcat -s FirestoreSync:D > logs_firestore.txt
```

---

*Relatório gerado automaticamente por Windsurf Cascade*  
*Baseado em AI_GUIDE_FINAL.md e FERRAMENTAS_MCP_HIERARQUIA.md*
```

---

## ⚙️ INSTRUÇÕES DE EXECUÇÃO PARA WINDSURF CASCADE

### Ordem de Execução:
1. ✅ **Fase 1 PRIMEIRO** (rollback emergencial) - App volta a funcionar
2. ✅ **Fase 2** (análise) - Confirmar paths exatos
3. ✅ **Fase 3** (limpeza) - Remover dados incorretos
4. ✅ **Fase 4** (implementação) - Deploy rules corretas
5. ✅ **Fase 5** (testes) - Validação completa
6. ✅ **Fase 6** (relatório) - Documentar tudo

### Critérios de Parada:
- ⛔ **PARE** se Fase 1 falhar → Pedir ajuda humana
- ⛔ **PARE** se Fase 2 encontrar paths diferentes dos esperados → Confirmar estrutura
- ⛔ **PARE** se Fase 4 (deploy) tiver erros de compilação → Revisar sintaxe
- ⛔ **PARE** se 4+ testes da Fase 5 falharem → Revisar rules

### Uso de Ferramentas:
- **Perplexity MCP:** Pesquisar "Firestore hierarchical security rules multi-tenancy 2026"
- **Filesystem MCP:** Editar firestore.rules, criar scripts
- **Comandos:** `rg`, `firebase deploy`, `adb logcat`
- **Máximo 2 builds** por fase

---

## 🎯 RESULTADO ESPERADO

Ao final desta execução:

✅ App sincronizando 100%  
✅ Usuários criados em `empresas/empresa_001/colaboradores/`  
✅ Rotas criadas em `empresas/empresa_001/entidades/rotas/`  
✅ Clientes criados em `empresas/empresa_001/entidades/clientes/`  
✅ Multi-tenancy funcionando por empresa + rota  
✅ Controle de acesso por aprovação + rotasPermitidas  
✅ Zero PERMISSION_DENIED para operações legítimas  
✅ Relatório completo gerado  

---

**FIM DO PROMPT** 🚀

---

*Prompt gerado em: 27/01/2026 13:32*  
*Baseado em: Análise do problema de sincronização + Estrutura hierárquica Firestore*  
*Executor: Windsurf Cascade com protocolo AI_GUIDE_FINAL.md*
