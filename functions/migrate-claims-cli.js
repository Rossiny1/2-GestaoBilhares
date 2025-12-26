/**
 * Script para migrar claims usando Firebase CLI credentials
 * Executa: firebase login:ci primeiro para gerar token
 */

const { execSync } = require('child_process');
const https = require('https');

// Obter token do Firebase CLI
function getFirebaseToken() {
    try {
        // Tentar obter token via firebase login:ci
        const token = execSync('firebase login:ci --project gestaobilhares', { 
            encoding: 'utf-8',
            stdio: ['inherit', 'pipe', 'pipe']
        }).trim();
        return token;
    } catch (error) {
        console.error('❌ Erro ao obter token. Execute: firebase login:ci');
        process.exit(1);
    }
}

// Chamar função callable
function callFunction(functionName, data = {}, token) {
    return new Promise((resolve, reject) => {
        const projectId = 'gestaobilhares';
        const region = 'us-central1';
        const url = `https://${region}-${projectId}.cloudfunctions.net/${functionName}`;
        
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
    });
}

async function main() {
    console.log('========================================');
    console.log('  Migração de Claims via Firebase CLI');
    console.log('========================================');
    console.log('');
    
    console.log('Obtendo token de autenticação...');
    const token = getFirebaseToken();
    console.log('✅ Token obtido');
    console.log('');
    
    try {
        // Primeiro validar
        console.log('[1/2] Validando claims atuais...');
        const validation = await callFunction('validateUserClaims', {}, token);
        
        console.log('');
        console.log('📊 Status Atual:');
        console.log(`   Total de usuários: ${validation.total}`);
        console.log(`   Com companyId: ${validation.withCompanyId}`);
        console.log(`   Sem companyId: ${validation.withoutCompanyId}`);
        console.log('');
        
        if (validation.withoutCompanyId > 0) {
            console.log('⚠️  Usuários sem companyId encontrados:');
            validation.details.slice(0, 10).forEach(detail => {
                console.log(`   - ${detail.email || detail.uid}: ${detail.reason}`);
            });
            console.log('');
        }
        
        // Executar migração
        console.log('[2/2] Executando migração...');
        console.log('Isso pode levar alguns minutos...');
        console.log('');
        
        const migrationResult = await callFunction('migrateUserClaims', {}, token);
        
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
            console.log('');
        }
        
        // Validar novamente
        console.log('Validando após migração...');
        const validationAfter = await callFunction('validateUserClaims', {}, token);
        
        console.log('');
        console.log('📊 Status Após Migração:');
        console.log(`   Total de usuários: ${validationAfter.total}`);
        console.log(`   Com companyId: ${validationAfter.withCompanyId}`);
        console.log(`   Sem companyId: ${validationAfter.withoutCompanyId}`);
        console.log('');
        
        if (validationAfter.withoutCompanyId === 0) {
            console.log('✅ SUCESSO! Todos os usuários têm companyId nas claims!');
        }
        
    } catch (error) {
        console.error('');
        console.error('❌ ERRO:', error.message);
        process.exit(1);
    }
}

main();

