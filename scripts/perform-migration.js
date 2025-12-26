/**
 * Script definitivo para migração de claims
 * Executa usando o Admin SDK e tenta autenticar via ambiente
 */

const path = require('path');
// Tenta carregar o módulo do diretório de funções se não encontrar localmente
try {
    require.resolve('firebase-admin');
} catch (e) {
    const functionsNodeModules = path.join(process.cwd(), 'functions', 'node_modules');
    module.paths.push(functionsNodeModules);
}
const admin = require('firebase-admin');

// Inicialização sem argumentos tenta usar credenciais padrão do ambiente
try {
    admin.initializeApp();
    console.log('✅ Firebase Admin inicializado com sucesso');
} catch (error) {
    console.error('❌ Erro ao inicializar Firebase Admin:', error.message);
    process.exit(1);
}

const db = admin.firestore();
const auth = admin.auth();

async function getRotasAtribuidas(empresaId, colaboradorId) {
    try {
        const snapshot = await db
            .collection('empresas')
            .doc(empresaId)
            .collection('entidades')
            .doc('colaborador_rota')
            .collection('items')
            .where('colaboradorId', '==', colaboradorId)
            .get();
        
        const rotasIds = [];
        snapshot.forEach(doc => {
            const data = doc.data();
            if (data.rotaId != null) {
                rotasIds.push(data.rotaId);
            }
        });
        return rotasIds;
    } catch (error) {
        console.error(`   ⚠️ Erro ao buscar rotas para colaborador ${colaboradorId}:`, error.message);
        return [];
    }
}

async function runMigration() {
    console.log('🚀 Iniciando Migração de Claims...');
    
    try {
        // 1. Listar usuários do Auth
        const usersResult = await auth.listUsers();
        const users = usersResult.users;
        console.log(`📊 Encontrados ${users.length} usuários no Firebase Auth`);
        
        for (const user of users) {
            console.log(`\n👤 Processando: ${user.email} (${user.uid})`);
            
            // 2. Buscar colaborador no Firestore via Collection Group (mais eficiente)
            const colabSnapshot = await db.collectionGroup('items')
                .where('email', '==', user.email)
                .get();
            
            const colabDoc = colabSnapshot.docs.find(doc => doc.ref.path.includes('/colaboradores/items/'));
            
            if (!colabDoc) {
                console.log(`   ❌ Colaborador não encontrado no Firestore para o email ${user.email}`);
                continue;
            }
            
            const colabData = colabDoc.data();
            const pathSegments = colabDoc.ref.path.split('/');
            const empresaId = pathSegments[1];
            const colaboradorId = colabDoc.id;
            
            console.log(`   🏢 Empresa: ${empresaId}, ID: ${colaboradorId}`);
            
            // 3. Buscar rotas
            const rotas = await getRotasAtribuidas(empresaId, colaboradorId);
            console.log(`   🛤️ Rotas: [${rotas.join(', ')}]`);
            
            // 4. Preparar claims
            const rawRole = colabData.nivelAcesso || colabData.role || 'collaborator';
            const role = typeof rawRole === 'string' ? rawRole.toLowerCase() : 'collaborator';
            const isAdmin = role === 'admin' || user.email === 'rossinys@gmail.com';
            
            const claims = {
                companyId: empresaId,
                colaboradorId: colaboradorId,
                role: role,
                admin: isAdmin,
                approved: true
            };
            
            if (rotas.length > 0) {
                claims.rotasAtribuidas = rotas;
            }
            
            // 5. Aplicar claims
            await auth.setCustomUserClaims(user.uid, claims);
            console.log(`   ✅ Claims aplicadas: ${JSON.stringify(claims)}`);
            
            // 6. Atualizar firebaseUid no doc se necessário
            if (colabData.firebaseUid !== user.uid) {
                await colabDoc.ref.update({ firebaseUid: user.uid });
                console.log(`   📝 firebaseUid atualizado no Firestore`);
            }
        }
        
        console.log('\n✅ Migração concluída com sucesso!');
        
    } catch (error) {
        console.error('\n❌ Erro durante a migração:', error);
    }
}

runMigration().then(() => process.exit(0));

