/**
 * Script para executar migração de claims de usuários existentes
 * Requer Firebase Admin SDK configurado
 */

const admin = require('firebase-admin');
const https = require('https');

// Inicializar Firebase Admin
try {
    // Tentar usar credenciais padrão (GOOGLE_APPLICATION_CREDENTIALS ou default)
    admin.initializeApp();
    console.log('✅ Firebase Admin inicializado');
} catch (error) {
    console.error('❌ Erro ao inicializar Firebase Admin:', error.message);
    console.error('Certifique-se de que GOOGLE_APPLICATION_CREDENTIALS está configurado');
    process.exit(1);
}

/**
 * Chamar função callable do Firebase
 */
async function callFunction(functionName, data = {}) {
    return new Promise((resolve, reject) => {
        const projectId = 'gestaobilhares';
        const region = 'us-central1';
        const url = `https://${region}-${projectId}.cloudfunctions.net/${functionName}`;
        
        // Obter token de autenticação
        admin.auth().createCustomToken(admin.auth().getUserByEmail('rossinys@gmail.com').then(user => user.uid))
            .then(token => {
                const postData = JSON.stringify({ data });
                
                const options = {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Content-Length': Buffer.byteLength(postData),
                        'Authorization': `Bearer ${token}`
                    }
                };
                
                const req = https.request(url, options, (res) => {
                    let responseData = '';
                    
                    res.on('data', (chunk) => {
                        responseData += chunk;
                    });
                    
                    res.on('end', () => {
                        try {
                            const result = JSON.parse(responseData);
                            if (res.statusCode === 200) {
                                resolve(result.result);
                            } else {
                                reject(new Error(`HTTP ${res.statusCode}: ${result.error?.message || responseData}`));
                            }
                        } catch (e) {
                            reject(new Error(`Erro ao parsear resposta: ${e.message}`));
                        }
                    });
                });
                
                req.on('error', (error) => {
                    reject(error);
                });
                
                req.write(postData);
                req.end();
            })
            .catch(reject);
    });
}

/**
 * Executar migração
 */
async function main() {
    console.log('========================================');
    console.log('  Migração de Claims de Usuários');
    console.log('========================================');
    console.log('');
    
    try {
        // Primeiro, validar estado atual
        console.log('[1/2] Validando claims atuais...');
        const validation = await callFunction('validateUserClaims');
        
        console.log('');
        console.log('📊 Status Atual:');
        console.log(`   Total de usuários: ${validation.total}`);
        console.log(`   Com companyId: ${validation.withCompanyId}`);
        console.log(`   Sem companyId: ${validation.withoutCompanyId}`);
        console.log(`   Sem claims: ${validation.withoutClaims}`);
        console.log('');
        
        if (validation.withoutCompanyId > 0) {
            console.log('⚠️  Usuários sem companyId encontrados:');
            validation.details.slice(0, 10).forEach(detail => {
                console.log(`   - ${detail.email || detail.uid}: ${detail.reason}`);
            });
            if (validation.details.length > 10) {
                console.log(`   ... e mais ${validation.details.length - 10} usuários`);
            }
            console.log('');
        }
        
        // Executar migração
        console.log('[2/2] Executando migração de claims...');
        console.log('Isso pode levar alguns minutos...');
        console.log('');
        
        const migrationResult = await callFunction('migrateUserClaims');
        
        console.log('');
        console.log('========================================');
        console.log('  Migração Concluída!');
        console.log('========================================');
        console.log('');
        console.log('📊 Resultados:');
        console.log(`   Total processado: ${migrationResult.total}`);
        console.log(`   ✅ Sucesso: ${migrationResult.success}`);
        console.log(`   ❌ Falhas: ${migrationResult.failed}`);
        console.log('');
        
        if (migrationResult.errors && migrationResult.errors.length > 0) {
            console.log('⚠️  Erros encontrados:');
            migrationResult.errors.slice(0, 10).forEach(error => {
                console.log(`   - ${error}`);
            });
            if (migrationResult.errors.length > 10) {
                console.log(`   ... e mais ${migrationResult.errors.length - 10} erros`);
            }
            console.log('');
        }
        
        // Validar novamente após migração
        console.log('Validando claims após migração...');
        const validationAfter = await callFunction('validateUserClaims');
        
        console.log('');
        console.log('📊 Status Após Migração:');
        console.log(`   Total de usuários: ${validationAfter.total}`);
        console.log(`   Com companyId: ${validationAfter.withCompanyId}`);
        console.log(`   Sem companyId: ${validationAfter.withoutCompanyId}`);
        console.log('');
        
        if (validationAfter.withoutCompanyId === 0) {
            console.log('✅ SUCESSO! Todos os usuários têm companyId nas claims!');
            console.log('');
            console.log('🎯 Próximo passo: Você pode agora remover os fallbacks das Firestore Rules');
            console.log('   Execute: .\\scripts\\deploy-regras-firestore.ps1');
        } else {
            console.log('⚠️  Ainda há usuários sem companyId. Verifique os erros acima.');
        }
        
    } catch (error) {
        console.error('');
        console.error('❌ ERRO na migração:', error.message);
        console.error('');
        process.exit(1);
    }
}

main();

