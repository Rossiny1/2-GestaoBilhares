# 🛡️ PROMPT: IMPLEMENTAÇÃO SEGURA DE SECURITY RULES FIREBASE

## 📋 CONTEXTO

Projeto Android de gestão de bilhares com Firebase Firestore. Preciso implementar Security Rules de forma **segura e reversível**, testando TUDO localmente antes de qualquer deploy em produção.

**Anexos obrigatórios:**
- `AI_GUIDE_FINAL.md` (protocolo de trabalho)
- `PROJECT_CONTEXT_FULL.md` (contexto do projeto)
- `RELATORIO_ANALISE_FIRESTORE_2026.md` (melhorias recomendadas)

---

## 🎯 OBJETIVO

Implementar Security Rules com **ZERO risco de travar produção**, seguindo este roadmap:

### Fase 1: Setup Emulator Local (2h)
1. Configurar Firebase Emulator Suite
2. Criar testes automatizados de Security Rules
3. Validar 100% localmente

### Fase 2: Rules Temporárias Safe Mode (5 min)
Deploy rules permissivas temporárias em produção (apenas autenticados)

### Fase 3: Rules Definitivas (1 semana)
Após validação local completa, deploy gradual em produção

---

## 🚀 FASE 1: SETUP EMULATOR + TESTES (EXECUTE AGORA)

### Tarefa 1.1: Instalar Firebase Emulator

**Comando Windows (PowerShell):**
```powershell
# Verificar Node.js instalado
node --version  # Deve ser v16+

# Instalar Firebase CLI globalmente
npm install -g firebase-tools

# Login Firebase
firebase login

# Inicializar emulators no projeto
cd C:\Users\Rossiny\Desktop\2-GestaoBilhares
firebase init emulators
```

**Configuração interativa:**
- Selecione: `Firestore`, `Authentication`
- Porta Firestore: `8080` (padrão)
- Porta Auth: `9099` (padrão)
- Porta UI: `4000` (padrão)
- Download emulators: `Yes`

**Validação:**
```powershell
firebase emulators:start
# Deve abrir http://localhost:4000
```

---

### Tarefa 1.2: Criar Security Rules Base (Local)

**Arquivo:** `firestore.rules` (criar na raiz do projeto)

**Conteúdo (baseado no RELATORIO_ANALISE_FIRESTORE_2026.md):**

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

    function isOwner(userId) {
      return request.auth.uid == userId;
    }

    function belongsToUserRoute(rotaId) {
      // Verificar se rota está em rotasPermitidas do usuário
      return isAuthenticated() && 
             get(/databases/$(database)/documents/usuarios/$(request.auth.uid)).data.rotasPermitidas.hasAny([rotaId]);
    }

    // ═══════════════════════════════════════════════════════
    // COLLECTION: clientes
    // ═══════════════════════════════════════════════════════

    match /clientes/{clienteId} {
      // Leitura: apenas se cliente pertence à rota permitida do usuário
      allow read: if isAuthenticated() && 
                     belongsToUserRoute(resource.data.rotaId);

      // Escrita: apenas se usuário tem acesso à rota E é o criador
      allow create: if isAuthenticated() && 
                       belongsToUserRoute(request.resource.data.rotaId) &&
                       request.resource.data.usuarioCriadorId == request.auth.uid;

      allow update: if isAuthenticated() && 
                       belongsToUserRoute(resource.data.rotaId) &&
                       resource.data.usuarioCriadorId == request.auth.uid;

      allow delete: if isAuthenticated() && 
                       belongsToUserRoute(resource.data.rotaId) &&
                       resource.data.usuarioCriadorId == request.auth.uid;
    }

    // ═══════════════════════════════════════════════════════
    // COLLECTION: acertos
    // ═══════════════════════════════════════════════════════

    match /acertos/{acertoId} {
      allow read: if isAuthenticated() && 
                     belongsToUserRoute(resource.data.rotaId);

      allow create: if isAuthenticated() && 
                       belongsToUserRoute(request.resource.data.rotaId) &&
                       request.resource.data.usuarioId == request.auth.uid;

      allow update: if isAuthenticated() && 
                       belongsToUserRoute(resource.data.rotaId) &&
                       resource.data.usuarioId == request.auth.uid;

      allow delete: if isAuthenticated() && 
                       belongsToUserRoute(resource.data.rotaId) &&
                       resource.data.usuarioId == request.auth.uid;
    }

    // ═══════════════════════════════════════════════════════
    // COLLECTION: mesas
    // ═══════════════════════════════════════════════════════

    match /mesas/{mesaId} {
      allow read: if isAuthenticated() && 
                     belongsToUserRoute(resource.data.rotaId);

      allow write: if isAuthenticated() && 
                      belongsToUserRoute(request.resource.data.rotaId);
    }

    // ═══════════════════════════════════════════════════════
    // COLLECTION: rotas
    // ═══════════════════════════════════════════════════════

    match /rotas/{rotaId} {
      allow read: if isAuthenticated() && 
                     belongsToUserRoute(rotaId);

      // Apenas admin pode criar/modificar rotas
      allow write: if isAuthenticated() && 
                      get(/databases/$(database)/documents/usuarios/$(request.auth.uid)).data.isAdmin == true;
    }

    // ═══════════════════════════════════════════════════════
    // COLLECTION: usuarios (leitura própria apenas)
    // ═══════════════════════════════════════════════════════

    match /usuarios/{userId} {
      allow read: if isAuthenticated() && request.auth.uid == userId;
      allow write: if false; // Usuários gerenciados via Admin SDK
    }

    // ═══════════════════════════════════════════════════════
    // COLLECTION: historico_manutencao
    // ═══════════════════════════════════════════════════════

    match /historico_manutencao/{historicoId} {
      allow read: if isAuthenticated() && 
                     belongsToUserRoute(resource.data.rotaId);

      allow write: if isAuthenticated() && 
                      belongsToUserRoute(request.resource.data.rotaId);
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

---

### Tarefa 1.3: Criar Testes Automatizados

**Arquivo:** `firestore.rules.test.js` (criar em `/tests/`)

**Conteúdo:**

```javascript
const { assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const { initializeTestEnvironment, RulesTestEnvironment } = require('@firebase/rules-unit-testing');
const fs = require('fs');

let testEnv;

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: 'demo-gestao-bilhares',
    firestore: {
      rules: fs.readFileSync('firestore.rules', 'utf8'),
      host: 'localhost',
      port: 8080,
    },
  });
});

afterAll(async () => {
  await testEnv.cleanup();
});

afterEach(async () => {
  await testEnv.clearFirestore();
});

// ═══════════════════════════════════════════════════════
// TESTES: clientes
// ═══════════════════════════════════════════════════════

describe('Clientes Collection', () => {
  test('Usuário autenticado pode ler cliente da própria rota', async () => {
    const alice = testEnv.authenticatedContext('alice', { rotasPermitidas: ['rota1'] });

    // Seed data
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection('clientes').doc('cliente1').set({
        nome: 'Cliente Teste',
        rotaId: 'rota1',
        usuarioCriadorId: 'alice',
      });

      await context.firestore().collection('usuarios').doc('alice').set({
        rotasPermitidas: ['rota1'],
      });
    });

    // Test
    await assertSucceeds(alice.firestore().collection('clientes').doc('cliente1').get());
  });

  test('Usuário NÃO pode ler cliente de outra rota', async () => {
    const bob = testEnv.authenticatedContext('bob', { rotasPermitidas: ['rota2'] });

    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection('clientes').doc('cliente1').set({
        nome: 'Cliente Teste',
        rotaId: 'rota1',
        usuarioCriadorId: 'alice',
      });

      await context.firestore().collection('usuarios').doc('bob').set({
        rotasPermitidas: ['rota2'],
      });
    });

    await assertFails(bob.firestore().collection('clientes').doc('cliente1').get());
  });

  test('Usuário anônimo NÃO pode ler clientes', async () => {
    const unauthed = testEnv.unauthenticatedContext();

    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection('clientes').doc('cliente1').set({
        nome: 'Cliente Teste',
        rotaId: 'rota1',
      });
    });

    await assertFails(unauthed.firestore().collection('clientes').doc('cliente1').get());
  });
});

// ═══════════════════════════════════════════════════════
// TESTES: acertos
// ═══════════════════════════════════════════════════════

describe('Acertos Collection', () => {
  test('Usuário pode criar acerto na própria rota', async () => {
    const alice = testEnv.authenticatedContext('alice');

    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection('usuarios').doc('alice').set({
        rotasPermitidas: ['rota1'],
      });
    });

    await assertSucceeds(
      alice.firestore().collection('acertos').add({
        rotaId: 'rota1',
        usuarioId: 'alice',
        valor: 100,
      })
    );
  });

  test('Usuário NÃO pode criar acerto em rota sem permissão', async () => {
    const bob = testEnv.authenticatedContext('bob');

    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection('usuarios').doc('bob').set({
        rotasPermitidas: ['rota2'],
      });
    });

    await assertFails(
      bob.firestore().collection('acertos').add({
        rotaId: 'rota1', // Rota não permitida
        usuarioId: 'bob',
        valor: 100,
      })
    );
  });
});

// ═══════════════════════════════════════════════════════
// TESTES: mesas
// ═══════════════════════════════════════════════════════

describe('Mesas Collection', () => {
  test('Usuário pode ler/escrever mesas da própria rota', async () => {
    const alice = testEnv.authenticatedContext('alice');

    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection('usuarios').doc('alice').set({
        rotasPermitidas: ['rota1'],
      });

      await context.firestore().collection('mesas').doc('mesa1').set({
        numero: 100,
        rotaId: 'rota1',
      });
    });

    await assertSucceeds(alice.firestore().collection('mesas').doc('mesa1').get());
    await assertSucceeds(alice.firestore().collection('mesas').doc('mesa1').update({ numero: 101 }));
  });
});

console.log('✅ Todos os testes configurados. Execute: npm test');
```

---

### Tarefa 1.4: Instalar Dependências de Teste

```powershell
# Criar package.json se não existir
npm init -y

# Instalar dependências de teste
npm install --save-dev @firebase/rules-unit-testing jest

# Adicionar script de teste ao package.json
```

**Edite `package.json` (adicione):**
```json
{
  "scripts": {
    "test": "jest firestore.rules.test.js"
  }
}
```

---

### Tarefa 1.5: Executar Testes Localmente

```powershell
# Terminal 1: Iniciar emulator
firebase emulators:start

# Terminal 2: Rodar testes
npm test
```

**Resultado esperado:**
```
✅ Usuário autenticado pode ler cliente da própria rota
✅ Usuário NÃO pode ler cliente de outra rota
✅ Usuário anônimo NÃO pode ler clientes
✅ Usuário pode criar acerto na própria rota
✅ Usuário NÃO pode criar acerto em rota sem permissão
✅ Usuário pode ler/escrever mesas da própria rota

Test Suites: 1 passed, 1 total
Tests:       6 passed, 6 total
```

---

## 🛡️ FASE 2: DEPLOY SAFE MODE EM PRODUÇÃO (APÓS TESTES 100%)

**⚠️ EXECUTAR APENAS SE TODOS OS TESTES PASSARAM**

### Tarefa 2.1: Backup Rules Atuais

```powershell
# Firebase Console > Firestore > Rules > Copiar tudo
# Salvar em: firestore.rules.backup.txt
```

### Tarefa 2.2: Deploy Rules Safe Mode Temporárias

**Criar arquivo:** `firestore.rules.safe` (temporário 1-2 semanas)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Safe Mode: Apenas usuários autenticados (permissivo temporário)
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

**Deploy:**
```powershell
# Copiar safe mode para production
cp firestore.rules.safe firestore.rules

# Deploy apenas rules (NÃO deploya app)
firebase deploy --only firestore:rules

# Confirmar no Console: Rules atualizadas
```

**Validação:**
- App funciona 100% normal
- Usuários autenticados acessam tudo
- Usuários anônimos bloqueados

---

## 🎯 FASE 3: DEPLOY RULES DEFINITIVAS (1 SEMANA DEPOIS)

**⏰ Executar apenas após:**
- [ ] 7 dias com Safe Mode em produção sem erros
- [ ] Todos os testes locais passando (6/6)
- [ ] Zero reports de usuários bloqueados

### Tarefa 3.1: Deploy Gradual (Collection por Collection)

**Semana 1:** Apenas `clientes`
```javascript
match /clientes/{clienteId} {
  // Rules completas (do Tarefa 1.2)
}
match /{document=**} {
  allow read, write: if request.auth != null; // Resto permissivo
}
```

**Semana 2:** + `acertos`  
**Semana 3:** + `mesas`  
**Semana 4:** Rules completas para todas collections

---

## ✅ CRITÉRIOS DE SUCESSO

### Fase 1 (Emulator):
- [ ] Firebase Emulator rodando (http://localhost:4000)
- [ ] 6/6 testes passando
- [ ] Rules locais validadas 100%
- [ ] Zero erros de sintaxe

### Fase 2 (Safe Mode):
- [ ] Deploy rules permissivas OK
- [ ] App funciona normalmente
- [ ] Zero downtime
- [ ] Backup rules antigas salvo

### Fase 3 (Definitivo):
- [ ] Deploy gradual (1 collection/semana)
- [ ] Monitoramento Firebase Console (zero violations)
- [ ] Rollback instantâneo disponível

---

## 🛑 SE ALGO DER ERRADO

### Rollback Imediato (10 segundos):

```powershell
# Restaurar backup
cp firestore.rules.backup.txt firestore.rules
firebase deploy --only firestore:rules
```

**Ou via Console:**
Firebase Console > Firestore > Rules > "Restore previous version"

---

## 📊 ENTREGÁVEIS

Ao finalizar, gere relatório:

```markdown
## 🛡️ RELATÓRIO: IMPLEMENTAÇÃO SECURITY RULES

### Fase 1: Emulator + Testes
- [x] Emulator configurado
- [x] 6/6 testes passando
- [x] Tempo: 2h
- [x] Zero erros

### Fase 2: Safe Mode Produção
- [x] Deploy rules permissivas
- [x] App funcionando
- [x] Backup salvo: firestore.rules.backup.txt
- [x] Data: [DD/MM/YYYY]

### Fase 3: Rules Definitivas (Planejado)
- [ ] Semana 1: clientes
- [ ] Semana 2: acertos
- [ ] Semana 3: mesas
- [ ] Semana 4: completo

### Métricas
- Downtime: 0 segundos
- Testes locais: 6/6 ✅
- Rollback disponível: SIM
- Risco produção: ZERO
```

---

## 🎓 NOTAS IMPORTANTES

1. **NUNCA pule os testes locais** - É o único jeito seguro.
2. **Safe Mode é temporário** - 1-2 semanas apenas.
3. **Deploy gradual é obrigatório** - 1 collection por semana.
4. **Backup sempre disponível** - Rollback em < 1 min.
5. **Use MCP Perplexity** para dúvidas sobre syntax rules.
6. **Use MCP Filesystem** apenas para editar arquivos após validação.
7. **Siga AI_GUIDE_FINAL.md** - Protocolo de trabalho obrigatório.
8. **Máximo 2 builds** - Se ultrapassar, volte ao diagnóstico.

---

## 🔧 INTEGRAÇÃO COM FERRAMENTAS MCP

### Quando Usar MCP Perplexity:
- ✅ Pesquisar "Firebase Security Rules best practices 2026"
- ✅ Pesquisar "Firestore rules hasAny syntax examples"
- ✅ Pesquisar "Firebase Emulator Suite setup Windows"
- ❌ NÃO usar para contexto do projeto (usar PROJECT_CONTEXT_FULL.md)

### Quando Usar MCP Filesystem:
- ✅ Editar `firestore.rules` após validação local
- ✅ Criar `firestore.rules.test.js` com testes
- ✅ Criar `package.json` com scripts
- ❌ NÃO usar para diagnóstico (consultar docs primeiro)

### Quando Usar MCP GitKraken:
- ✅ Commit após cada fase concluída
- ✅ Branch `feature/security-rules` para desenvolvimento
- ✅ Merge após testes 100% passando

---

## 📦 ESTRUTURA DE ARQUIVOS FINAL

```
C:\Users\Rossiny\Desktop\2-GestaoBilhares\
├── firestore.rules                    # Rules definitivas
├── firestore.rules.safe               # Rules Safe Mode temporárias
├── firestore.rules.backup.txt         # Backup rules antigas
├── firebase.json                      # Config Firebase
├── package.json                       # Scripts npm
├── tests/
│   └── firestore.rules.test.js       # Testes automatizados
└── documentation/
    └── PROMPT_SECURITY_RULES.md      # Este documento
```

---

## 🚀 COMANDOS RÁPIDOS

### Setup Inicial:
```powershell
npm install -g firebase-tools
firebase login
firebase init emulators
```

### Desenvolvimento:
```powershell
# Terminal 1
firebase emulators:start

# Terminal 2
npm test
```

### Deploy Safe Mode:
```powershell
cp firestore.rules.safe firestore.rules
firebase deploy --only firestore:rules
```

### Rollback:
```powershell
cp firestore.rules.backup.txt firestore.rules
firebase deploy --only firestore:rules
```

---

**FIM DO PROMPT** 🚀

---

*Gerado em: 27/01/2026*  
*Versão: 1.0*  
*Baseado em: RELATORIO_ANALISE_FIRESTORE_2026.md + AI_GUIDE_FINAL.md*
