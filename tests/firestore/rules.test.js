const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const firebase = require('@firebase/app');
require('@firebase/firestore');

/**
 * 🔥 FIRESTORE SECURITY RULES TESTS
 * 
 * Testes automatizados para validar:
 * - Multi-tenancy por rota
 * - Controle de acesso
 * - Validações de negócio
 * - Proteção contra ataques
 */

// Configuração do ambiente de teste
let testEnv;

beforeAll(async () => {
  testEnv = await initializeTestEnvironment(
    firebase.firestore(),
    {
      projectId: 'gestaobilhares-test',
      firestore: {
        rules: fs.readFileSync('firestore.rules', 'utf8')
      }
    }
  );
});

afterAll(async () => {
  await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();

    // Limpar dados de teste
    const collections = ['clientes', 'acertos', 'mesas', 'colaboradores'];
    for (const collection of collections) {
      const snapshot = await db.collection(collection).get();
      for (const doc of snapshot.docs) {
        await doc.ref.delete();
      }
    }
  });
});

describe('🔥 Multi-tenancy por Rota', () => {

  test('✅ usuário pode acessar dados da própria rota', async () => {
    const db = testEnv.authenticatedContext('user123', {
      empresa_id: 'empresa001',
      rotasPermitidas: ['037-Salinas', '034-Bonito']
    }).firestore();

    // Criar cliente na rota permitida
    await assertSucceeds(
      db.collection('empresas/empresa001/entidades/clientes/items').add({
        empresa_id: 'empresa001',
        rota_id: '037-Salinas',
        nome: 'Cliente Teste',
        valorFicha: 1.0,
        comissaoFicha: 0.6
      })
    );

    // Ler dados da própria rota
    await assertSucceeds(
      db.collection('empresas/empresa001/entidades/clientes/items')
        .where('rota_id', '==', '037-Salinas')
        .get()
    );
  });

  test('❌ usuário não pode acessar outras rotas', async () => {
    const db = testEnv.authenticatedContext('user123', {
      empresa_id: 'empresa001',
      rotasPermitidas: ['037-Salinas'] // Não inclui "034-Bonito"
    }).firestore();

    // Tentar acessar rota não permitida
    await assertFails(
      db.collection('empresas/empresa001/entidades/clientes/items')
        .where('rota_id', '==', '034-Bonito')
        .get()
    );
  });

  test('❌ usuário não pode acessar dados de outra empresa', async () => {
    const db = testEnv.authenticatedContext('user456', {
      empresa_id: 'empresa002', // Empresa diferente
      rotasPermitidas: ['037-Salinas']
    }).firestore();

    // Tentar acessar dados de outra empresa
    await assertFails(
      db.collection('empresas/empresa001/entidades/clientes/items').get()
    );
  });

  test('❌ usuário não autenticado não pode acessar nada', async () => {
    const db = testEnv.unauthenticatedContext().firestore();

    // Tentar acessar sem autenticação
    await assertFails(
      db.collection('empresas/empresa001/entidades/clientes/items').get()
    );
  });
});

describe('🔒 Validações de Negócio', () => {

  test('❌ campos obrigatórios devem estar presentes', async () => {
    const db = testEnv.authenticatedContext('user123', {
      empresa_id: 'empresa001',
      rotasPermitidas: ['037-Salinas']
    }).firestore();

    // Tentar criar sem campos obrigatórios
    await assertFails(
      db.collection('empresas/empresa001/entidades/clientes/items').add({
        nome: 'Cliente Teste'
        // Falta: empresa_id, rota_id
      })
    );
  });

  test('❌ valores inválidos devem ser rejeitados', async () => {
    const db = testEnv.authenticatedContext('user123', {
      empresa_id: 'empresa001',
      rotasPermitidas: ['037-Salinas']
    }).firestore();

    // Tentar criar com valor negativo
    await assertFails(
      db.collection('empresas/empresa001/entidades/clientes/items').add({
        empresa_id: 'empresa001',
        rota_id: '037-Salinas',
        nome: 'Cliente Teste',
        valorFicha: -1.0 // Valor inválido
      })
    );
  });

  test('❌ comissão fora do range deve ser rejeitada', async () => {
    const db = testEnv.authenticatedContext('user123', {
      empresa_id: 'empresa001',
      rotasPermitidas: ['037-Salinas']
    }).firestore();

    // Tentar criar com comissão > 100%
    await assertFails(
      db.collection('empresas/empresa001/entidades/clientes/items').add({
        empresa_id: 'empresa001',
        rota_id: '037-Salinas',
        nome: 'Cliente Teste',
        valorFicha: 1.0,
        comissaoFicha: 150.0 // Fora do range (0-100)
      })
    );
  });
});

describe('👥 Controle de Acesso', () => {

  test('✅ colaborador só pode ver próprios dados', async () => {
    const db = testEnv.authenticatedContext('colab123', {
      empresa_id: 'empresa001',
      rotasPermitidas: ['037-Salinas'],
      nivel: 'colaborador'
    }).firestore();

    // Criar dados do colaborador
    await assertSucceeds(
      db.collection('empresas/empresa001/colaboradores').doc('colab123').set({
        nome: 'Colaborador Teste',
        email: 'colab@teste.com',
        nivel: 'colaborador'
      })
    );

    // Pode acessar próprios dados
    await assertSucceeds(
      db.collection('empresas/empresa001/colaboradores').doc('colab123').get()
    );

    // Não pode acessar outros colaboradores
    await assertFails(
      db.collection('empresas/empresa001/colaboradores').doc('outro-colab').get()
    );
  });

  test('✅ admin pode acessar todos os colaboradores', async () => {
    const db = testEnv.authenticatedContext('admin123', {
      empresa_id: 'empresa001',
      rotasPermitidas: ['037-Salinas', '034-Bonito'],
      nivel: 'admin'
    }).firestore();

    // Criar colaboradores
    await assertSucceeds(
      db.collection('empresas/empresa001/colaboradores').doc('colab1').set({
        nome: 'Colaborador 1',
        nivel: 'colaborador'
      })
    );

    await assertSucceeds(
      db.collection('empresas/empresa001/colaboradores').doc('colab2').set({
        nome: 'Colaborador 2',
        nivel: 'colaborador'
      })
    );

    // Admin pode listar todos
    await assertSucceeds(
      db.collection('empresas/empresa001/colaboradores').get()
    );
  });

  test('❌ colaborador não pode gerenciar outros colaboradores', async () => {
    const db = testEnv.authenticatedContext('colab123', {
      empresa_id: 'empresa001',
      rotasPermitidas: ['037-Salinas'],
      nivel: 'colaborador'
    }).firestore();

    // Tentar criar outro colaborador
    await assertFails(
      db.collection('empresas/empresa001/colaboradores').doc('novo-colab').set({
        nome: 'Novo Colaborador',
        nivel: 'colaborador'
      })
    );
  });
});

describe('⚡ Performance e Escalabilidade', () => {

  test('✅ leituras em massa devem funcionar', async () => {
    const db = testEnv.authenticatedContext('user123', {
      empresa_id: 'empresa001',
      rotasPermitidas: ['037-Salinas']
    }).firestore();

    // Criar múltiplos clientes
    const batch = db.batch();
    for (let i = 0; i < 100; i++) {
      const docRef = db.collection('empresas/empresa001/entidades/clientes/items').doc();
      batch.set(docRef, {
        empresa_id: 'empresa001',
        rota_id: '037-Salinas',
        nome: `Cliente ${i}`,
        valorFicha: 1.0,
        comissaoFicha: 0.6
      });
    }
    await assertSucceeds(batch.commit());

    // Ler todos os clientes
    await assertSucceeds(
      db.collection('empresas/empresa001/entidades/clientes/items')
        .where('rota_id', '==', '037-Salinas')
        .limit(100)
        .get()
    );
  });

  test('✅ escritas em massa devem funcionar', async () => {
    const db = testEnv.authenticatedContext('user123', {
      empresa_id: 'empresa001',
      rotasPermitidas: ['037-Salinas']
    }).firestore();

    // Criar múltiplos acertos
    const batch = db.batch();
    for (let i = 0; i < 50; i++) {
      const docRef = db.collection('empresas/empresa001/entidades/acertos/items').doc();
      batch.set(docRef, {
        empresa_id: 'empresa001',
        rota_id: '037-Salinas',
        clienteId: `cliente${i}`,
        totalMesas: 100.0,
        valorRecebido: 60.0,
        dataUltimaAtualizacao: Date.now()
      });
    }
    await assertSucceeds(batch.commit());

    // Verificar que todos foram criados
    const snapshot = await assertSucceeds(
      db.collection('empresas/empresa001/entidades/acertos/items')
        .where('rota_id', '==', '037-Salinas')
        .get()
    );

    expect(snapshot.size).toBe(50);
  });
});

describe('🛡️ Proteção Contra Ataques', () => {

  test('❌ injeção de SQL deve ser bloqueada', async () => {
    const db = testEnv.authenticatedContext('user123', {
      empresa_id: 'empresa001',
      rotasPermitidas: ['037-Salinas']
    }).firestore();

    // Tentar injeção SQL (não deve funcionar no Firestore, mas testa validações)
    await assertFails(
      db.collection('empresas/empresa001/entidades/clientes/items')
        .where('nome', '==', "'; DROP TABLE clientes; --")
        .get()
    );
  });

  test('❌ acesso a campos sensíveis deve ser controlado', async () => {
    const db = testEnv.authenticatedContext('user123', {
      empresa_id: 'empresa001',
      rotasPermitidas: ['037-Salinas']
    }).firestore();

    // Tentar acessar campo sensível não autorizado
    await assertFails(
      db.collection('empresas/empresa001/entidades/clientes/items')
        .where('senha', '==', 'qualquer')
        .get()
    );
  });

  test('❌ tentativa de escalonamento privilegiado deve ser bloqueada', async () => {
    // Simular usuário tentando acessar dados de admin
    const db = testEnv.authenticatedContext('user123', {
      empresa_id: 'empresa001',
      rotasPermitidas: ['037-Salinas'],
      nivel: 'colaborador' // Nível baixo
    }).firestore();

    // Tentar acessar dados administrativos
    await assertFails(
      db.collection('empresas/empresa001/admin').get()
    );
  });
});

describe('📊 Relatórios e Análise', () => {

  test('✅ pode gerar relatórios de próprios dados', async () => {
    const db = testEnv.authenticatedContext('user123', {
      empresa_id: 'empresa001',
      rotasPermitidas: ['037-Salinas']
    }).firestore();

    // Criar dados de teste
    await assertSucceeds(
      db.collection('empresas/empresa001/entidades/acertos/items').add({
        empresa_id: 'empresa001',
        rota_id: '037-Salinas',
        clienteId: 'cliente1',
        totalMesas: 100.0,
        valorRecebido: 60.0,
        dataUltimaAtualizacao: Date.now()
      })
    );

    // Gerar relatório (simulado)
    const acertos = await assertSucceeds(
      db.collection('empresas/empresa001/entidades/acertos/items')
        .where('rota_id', '==', '037-Salinas')
        .get()
    );

    expect(acertos.size).toBeGreaterThan(0);
    expect(acertos.docs[0].data().empresa_id).toBe('empresa001');
    expect(acertos.docs[0].data().rota_id).toBe('037-Salinas');
  });

  test('❌ não pode acessar relatórios de outras rotas', async () => {
    const db = testEnv.authenticatedContext('user123', {
      empresa_id: 'empresa001',
      rotasPermitidas: ['037-Salinas']
    }).firestore();

    // Tentar acessar relatório de outra rota
    await assertFails(
      db.collection('empresas/empresa001/entidades/acertos/items')
        .where('rota_id', '==', '034-Bonito')
        .get()
    );
  });
});

// Helper functions
function fs() {
  // Implementação simplificada para leitura de arquivo
  // Em produção, usar fs real do Node.js
  return `
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper functions
    function belongsToUserRoute(empresaId, rotaId) {
      return request.auth != null &&
             request.auth.token.empresa_id == empresaId &&
             request.auth.token.rotasPermitidas.contains(rotaId);
    }
    
    function isAdmin() {
      return request.auth != null &&
             request.auth.token.nivel == 'admin';
    }
    
    function isCollaboradorOwnData(userId) {
      return request.auth != null &&
             request.auth.uid == userId &&
             request.auth.token.nivel == 'colaborador';
    }
    
    // Entidades com multi-tenancy
    match /empresas/{empresaId}/entidades/{collection}/items/{itemId} {
      allow read, write: if belongsToUserRoute(empresaId, resource.data.rota_id);
      allow create: if belongsToUserRoute(empresaId, request.resource.data.rota_id);
    }
    
    // Colaboradores
    match /empresas/{empresaId}/colaboradores/{uid} {
      allow read, write: if request.auth != null &&
                           request.auth.token.empresa_id == empresaId &&
                           (isAdmin() || isCollaboradorOwnData(uid));
      allow create: if request.auth != null &&
                       request.auth.token.empresa_id == empresaId &&
                       isAdmin();
    }
    
    // Dados administrativos
    match /empresas/{empresaId}/admin/{document} {
      allow read, write: if isAdmin() && request.auth.token.empresa_id == empresaId;
    }
    
    // Negar tudo mais
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
  `;
}
