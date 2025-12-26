/**
 * Script para executar migração localmente usando Admin SDK
 * Simula a execução da função migrateUserClaims
 */

const admin = require('firebase-admin');

// Inicializar usando Application Default Credentials do Firebase CLI
admin.initializeApp({
    projectId: 'gestaobilhares'
});

async function getRotasAtribuidas(empresaId, colaboradorId) {
    try {
        const colaboradorRotasRef = admin.firestore()
            .collection('empresas')
            .doc(empresaId)
            .collection('entidades')
            .doc('colaborador_rota')
            .collection('items');
        
        const snapshot = await colaboradorRotasRef
            .where('colaboradorId', '==', colaboradorId)
            .get();
        
        const rotasIds = [];
        snapshot.forEach(doc => {
            const rotaId = doc.data().rotaId;
            if (rotaId != null && typeof rotaId === 'number') {
                rotasIds.push(rotaId);
            }
        });
        
        return rotasIds;
    } catch (error) {
        console.error(`Erro ao buscar rotas: ${error.message}`);
        return [];
    }
}

async function updateUserClaims(userUid, empresaId, colaboradorId, role, email) {
    const isAdmin = role === 'admin';
    const rotasAtribuidas = await getRotasAtribuidas(empresaId, colaboradorId);
    
    const claims = {
        companyId: empresaId,
        colaboradorId: colaboradorId,
        role: role,
        admin: isAdmin,
        approved: true
    };
    
    if (rotasAtribuidas.length > 0) {
        claims.rotasAtribuidas = rotasAtribuidas;
    }
    
    await admin.auth().setCustomUserClaims(userUid, claims);
    console.log(`   ✅ ${email} (${rotasAtribuidas.length} rotas, role: ${role})`);
}

async function migrateUserClaims() {
    console.log('========================================');
    console.log('  Migração de Claims de Usuários');
    console.log('========================================');
    console.log('');
    
    const results = {
        total: 0,
        success: 0,
        failed: 0,
        errors: []
    };
    
    try {
        const empresasSnapshot = await admin.firestore().collection('empresas').get();
        console.log(`📦 Encontradas ${empresasSnapshot.size} empresas`);
        console.log('');
        
        for (const empresaDoc of empresasSnapshot.docs) {
            const empresaId = empresaDoc.id;
            console.log(`🏢 Processando empresa: ${empresaId}`);
            
            const colaboradoresRef = admin.firestore()
                .collection('empresas')
                .doc(empresaId)
                .collection('entidades')
                .doc('colaboradores')
                .collection('items');
            
            const colaboradoresSnapshot = await colaboradoresRef.get();
            console.log(`   📋 Encontrados ${colaboradoresSnapshot.size} colaboradores`);
            
            for (const colaboradorDoc of colaboradoresSnapshot.docs) {
                results.total++;
                const colaboradorId = colaboradorDoc.id;
                const colaboradorData = colaboradorDoc.data();
                
                const email = colaboradorData.email;
                const firebaseUid = colaboradorData.firebaseUid;
                
                if (!email || !firebaseUid) {
                    results.failed++;
                    const errorMsg = `Colaborador ${colaboradorId} sem email ou firebaseUid`;
                    results.errors.push(errorMsg);
                    console.log(`   ⚠️  ${errorMsg}`);
                    continue;
                }
                
                try {
                    await admin.auth().getUser(firebaseUid);
                    
                    const rawRole = colaboradorData.nivelAcesso || colaboradorData.role || 'collaborator';
                    const role = typeof rawRole === 'string' ? rawRole.toLowerCase() : 'collaborator';
                    
                    await updateUserClaims(firebaseUid, empresaId, colaboradorId, role, email);
                    results.success++;
                } catch (error) {
                    results.failed++;
                    const errorMsg = `Erro ao atualizar ${email}: ${error.message}`;
                    results.errors.push(errorMsg);
                    console.log(`   ❌ ${errorMsg}`);
                }
            }
            console.log('');
        }
        
        console.log('========================================');
        console.log('  Migração Concluída!');
        console.log('========================================');
        console.log('');
        console.log('📊 Resultados:');
        console.log(`   Total: ${results.total}`);
        console.log(`   ✅ Sucesso: ${results.success}`);
        console.log(`   ❌ Falhas: ${results.failed}`);
        console.log('');
        
        if (results.errors.length > 0) {
            console.log('⚠️  Erros:');
            results.errors.slice(0, 10).forEach(e => console.log(`   - ${e}`));
            console.log('');
        }
        
        // Validar
        console.log('🔍 Validando...');
        let total = 0, withCompanyId = 0, withoutCompanyId = 0;
        let nextPageToken;
        
        do {
            const list = await admin.auth().listUsers(1000, nextPageToken);
            for (const user of list.users) {
                total++;
                if (user.customClaims?.companyId) {
                    withCompanyId++;
                } else {
                    withoutCompanyId++;
                }
            }
            nextPageToken = list.pageToken;
        } while (nextPageToken);
        
        console.log('');
        console.log('📊 Validação:');
        console.log(`   Total: ${total}`);
        console.log(`   ✅ Com companyId: ${withCompanyId}`);
        console.log(`   ❌ Sem companyId: ${withoutCompanyId}`);
        console.log('');
        
        if (withoutCompanyId === 0) {
            console.log('✅ SUCESSO! Todos têm companyId!');
        }
        
        return results;
    } catch (error) {
        console.error('❌ ERRO:', error.message);
        throw error;
    }
}

migrateUserClaims()
    .then(() => process.exit(0))
    .catch(error => {
        console.error('❌ Erro fatal:', error);
        process.exit(1);
    });

