/**
 * Script Node.js para testar as regras do Firestore diretamente
 * Usa o Firebase Admin SDK para simular queries sem autenticação
 */

const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json'); // Precisa ser criado

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function testarBuscaColaborador() {
  console.log('=== TESTE DE REGRAS FIRESTORE ===\n');
  
  const emailTeste = 'user@teste.com'; // Substituir pelo email real
  
  try {
    // 1. Testar collectionGroup sem autenticação (simulando app vazio)
    console.log('1. Testando collectionGroup("items") sem autenticação...');
    const collectionGroupQuery = db.collectionGroup('items')
      .where('email', '==', emailTeste);
    
    const snapshot = await collectionGroupQuery.get();
    console.log(`   Documentos encontrados: ${snapshot.size}`);
    
    snapshot.forEach(doc => {
      console.log(`   Path: ${doc.ref.path}`);
      console.log(`   Dados:`, doc.data());
      console.log(`   É colaborador: ${doc.ref.path.includes('/colaboradores/items/')}`);
    });
    
    // 2. Testar busca direta
    console.log('\n2. Testando busca direta em empresa_001...');
    const directQuery = db.collection('empresas')
      .doc('empresa_001')
      .collection('entidades')
      .doc('colaboradores')
      .collection('items')
      .where('email', '==', emailTeste);
    
    const directSnapshot = await directQuery.get();
    console.log(`   Documentos encontrados: ${directSnapshot.size}`);
    
    directSnapshot.forEach(doc => {
      console.log(`   Path: ${doc.ref.path}`);
      console.log(`   Dados:`, doc.data());
    });
    
    // 3. Verificar estrutura dos dados
    console.log('\n3. Verificando estrutura dos dados...');
    const allColaboradores = await db.collection('empresas')
      .doc('empresa_001')
      .collection('entidades')
      .doc('colaboradores')
      .collection('items')
      .limit(3)
      .get();
    
    allColaboradores.forEach(doc => {
      const data = doc.data();
      console.log(`\n   Colaborador: ${data.nome} (${data.email})`);
      console.log(`   Campos: ${Object.keys(data).join(', ')}`);
      console.log(`   Aprovado: ${data.aprovado}`);
      console.log(`   Ativo: ${data.ativo}`);
      console.log(`   nivel_acesso: ${data.nivel_acesso}`);
      console.log(`   nivelAcesso: ${data.nivelAcesso}`);
    });
    
  } catch (error) {
    console.error('❌ ERRO:', error.message);
    console.error('Stack:', error.stack);
  }
}

testarBuscaColaborador();
