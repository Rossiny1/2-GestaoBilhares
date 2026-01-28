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