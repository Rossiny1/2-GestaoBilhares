/**
 * Script para ser executado via firebase functions:shell
 * Atualiza as claims de todos os usuários
 */

const admin = require('firebase-admin');

async function migrate() {
    console.log('--- INICIANDO MIGRAÇÃO VIA SHELL ---');
    const db = admin.firestore();
    const auth = admin.auth();
    
    const users = [
        'rossinys@gmail.com',
        'mel@gmail.com',
        'ceci@gmail.com',
        'leo@gmail.com',
        'lia@gmail.com'
    ];
    
    for (const email of users) {
        try {
            console.log(`Processando: ${email}`);
            
            // Buscar colaborador no Firestore (Collection Group)
            const snapshot = await db.collectionGroup('items')
                .where('email', '==', email)
                .get();
            
            const doc = snapshot.docs.find(d => d.ref.path.includes('/colaboradores/items/'));
            
            if (!doc) {
                console.log(`  ❌ Colaborador não encontrado no Firestore para ${email}`);
                continue;
            }
            
            const data = doc.data();
            const pathSegments = doc.ref.path.split('/');
            const empresaId = pathSegments[1];
            const colaboradorId = doc.id;
            
            // Buscar usuário no Auth
            const userRecord = await auth.getUserByEmail(email);
            
            // Buscar rotas
            const rotasSnapshot = await db.collection('empresas')
                .doc(empresaId)
                .collection('entidades')
                .doc('colaborador_rota')
                .collection('items')
                .where('colaboradorId', '==', colaboradorId)
                .get();
            
            const rotasIds = [];
            rotasSnapshot.forEach(d => {
                const rId = d.data().rotaId;
                if (rId != null && typeof rId === 'number') rotasIds.push(rId);
            });
            
            const rawRole = data.nivelAcesso || data.role || 'collaborator';
            const role = typeof rawRole === 'string' ? rawRole.toLowerCase() : 'collaborator';
            
            const claims = {
                companyId: empresaId,
                colaboradorId: colaboradorId,
                role: role,
                admin: role === 'admin',
                approved: true
            };
            
            if (rotasIds.length > 0) {
                claims.rotasAtribuidas = rotasIds;
            }
            
            await auth.setCustomUserClaims(userRecord.uid, claims);
            console.log(`  ✅ Claims atualizadas: ${JSON.stringify(claims)}`);
            
            // Sincronizar firebaseUid se necessário
            if (data.firebaseUid !== userRecord.uid) {
                await doc.ref.update({ firebaseUid: userRecord.uid });
                console.log(`  ✅ UID sincronizado no Firestore`);
            }
            
        } catch (e) {
            console.error(`  ❌ Erro ao processar ${email}: ${e.message}`);
        }
    }
    console.log('--- MIGRAÇÃO CONCLUÍDA ---');
}

migrate().then(() => process.exit(0)).catch(e => { console.error(e); process.exit(1); });

