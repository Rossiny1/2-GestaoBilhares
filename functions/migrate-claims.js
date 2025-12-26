/**
 * Script para migrar claims de usuários existentes
 * Executa diretamente na pasta functions onde firebase-admin está instalado
 */

const admin = require('firebase-admin');

// Inicializar Firebase Admin
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
        console.error(`Erro ao buscar rotas do colaborador ${colaboradorId}:`, error.message);
        return [];
    }
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
        // Buscar todas as empresas
        const empresasSnapshot = await admin.firestore().collection('empresas').get();
        console.log(`📦 Encontradas ${empresasSnapshot.size} empresas`);
        console.log('');
        
        for (const empresaDoc of empresasSnapshot.docs) {
            const empresaId = empresaDoc.id;
            console.log(`🏢 Processando empresa: ${empresaId}`);
            
            // Buscar todos os colaboradores da empresa
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
                
                if (!email) {
                    results.failed++;
                    const errorMsg = `Colaborador ${colaboradorId} sem email`;
                    results.errors.push(errorMsg);
                    console.log(`   ⚠️  ${errorMsg}`);
                    continue;
                }
                
                if (!firebaseUid) {
                    results.failed++;
                    const errorMsg = `Colaborador ${colaboradorId} (${email}) sem firebaseUid`;
                    results.errors.push(errorMsg);
                    console.log(`   ⚠️  ${errorMsg}`);
                    continue;
                }
                
                try {
                    // Verificar se usuário existe no Auth
                    const userRecord = await admin.auth().getUser(firebaseUid);
                    
                    // Buscar rotas atribuídas
                    const rotasIds = await getRotasAtribuidas(empresaId, colaboradorId);
                    
                    // Buscar role
                    const rawRole = colaboradorData.nivelAcesso || colaboradorData.role || 'collaborator';
                    const role = typeof rawRole === 'string' ? rawRole.toLowerCase() : 'collaborator';
                    const isAdmin = role === 'admin';
                    
                    // Configurar claims
                    const claims = {
                        companyId: empresaId,
                        colaboradorId: colaboradorId,
                        role: role,
                        admin: isAdmin,
                        approved: true
                    };
                    
                    if (rotasIds.length > 0) {
                        claims.rotasAtribuidas = rotasIds;
                    }
                    
                    // Atualizar claims
                    await admin.auth().setCustomUserClaims(firebaseUid, claims);
                    
                    results.success++;
                    console.log(`   ✅ ${email} (${rotasIds.length} rotas, role: ${role})`);
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
        console.log(`   Total processado: ${results.total}`);
        console.log(`   ✅ Sucesso: ${results.success}`);
        console.log(`   ❌ Falhas: ${results.failed}`);
        console.log('');
        
        if (results.errors.length > 0) {
            console.log('⚠️  Erros encontrados:');
            results.errors.slice(0, 10).forEach(error => {
                console.log(`   - ${error}`);
            });
            if (results.errors.length > 10) {
                console.log(`   ... e mais ${results.errors.length - 10} erros`);
            }
            console.log('');
        }
        
        // Validar após migração
        console.log('🔍 Validando claims após migração...');
        await validateUserClaims();
        
        return results;
    } catch (error) {
        console.error('');
        console.error('❌ ERRO na migração:', error.message);
        throw error;
    }
}

async function validateUserClaims() {
    const results = {
        total: 0,
        withCompanyId: 0,
        withoutCompanyId: 0,
        withoutClaims: 0,
        details: []
    };
    
    try {
        let nextPageToken;
        
        do {
            const listUsersResult = await admin.auth().listUsers(1000, nextPageToken);
            
            for (const user of listUsersResult.users) {
                results.total++;
                
                const claims = user.customClaims || {};
                const hasCompanyId = !!claims.companyId;
                
                if (!hasCompanyId) {
                    results.withoutCompanyId++;
                    results.details.push({
                        uid: user.uid,
                        email: user.email,
                        reason: !claims.role ? 'Sem claims' : 'Sem companyId'
                    });
                    
                    if (!claims.role) {
                        results.withoutClaims++;
                    }
                } else {
                    results.withCompanyId++;
                }
            }
            
            nextPageToken = listUsersResult.pageToken;
        } while (nextPageToken);
        
        console.log('');
        console.log('📊 Validação de Claims:');
        console.log(`   Total de usuários: ${results.total}`);
        console.log(`   ✅ Com companyId: ${results.withCompanyId}`);
        console.log(`   ❌ Sem companyId: ${results.withoutCompanyId}`);
        console.log(`   ⚠️  Sem claims: ${results.withoutClaims}`);
        console.log('');
        
        if (results.withoutCompanyId === 0) {
            console.log('✅ SUCESSO! Todos os usuários têm companyId nas claims!');
            console.log('');
            console.log('🎯 Próximo passo: Você pode agora remover os fallbacks das Firestore Rules');
        } else {
            console.log('⚠️  Ainda há usuários sem companyId:');
            results.details.slice(0, 10).forEach(detail => {
                console.log(`   - ${detail.email || detail.uid}: ${detail.reason}`);
            });
            if (results.details.length > 10) {
                console.log(`   ... e mais ${results.details.length - 10} usuários`);
            }
        }
        
        console.log('');
        return results;
    } catch (error) {
        console.error('❌ Erro na validação:', error.message);
        throw error;
    }
}

// Executar migração
migrateUserClaims()
    .then(() => {
        console.log('✅ Script concluído com sucesso!');
        process.exit(0);
    })
    .catch(error => {
        console.error('❌ Erro fatal:', error);
        process.exit(1);
    });

