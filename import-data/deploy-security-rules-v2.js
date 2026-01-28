/**
 * Deploy de Security Rules Firestore - Versão Simplificada
 * 
 * Usa a mesma abordagem do importador de dados (Firebase Admin SDK)
 * Deploy seguro e automatizado das regras de segurança
 */

const admin = require('firebase-admin');
const fs = require('fs');
const { execSync } = require('child_process');

// Configurar Firebase Admin com Service Account (mesmo do importador)
try {
    const serviceAccount = require('./service-account.json');
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        projectId: 'gestaobilhares'
    });
    console.log('✅ Firebase Admin configurado com Service Account!');
} catch (error) {
    console.error('❌ Erro ao configurar Firebase Admin:', error.message);
    process.exit(1);
}

/**
 * Faz deploy das Security Rules usando Firebase CLI (via Admin SDK)
 */
async function deploySecurityRules() {
    console.log('🛡️ DEPLOY SECURITY RULES - FIRESTORE ADMIN SDK V2');
    console.log('='.repeat(60));
    console.log(`📦 Projeto: gestaobilhares`);
    console.log(`🔑 Usando Service Account + Firebase CLI`);
    console.log(`⏰ Início: ${new Date().toLocaleString('pt-BR')}`);

    try {
        // 1. Verificar se arquivo de regras existe
        const rulesPath = '../firestore.rules';
        if (!fs.existsSync(rulesPath)) {
            throw new Error(`Arquivo de regras não encontrado: ${rulesPath}`);
        }

        console.log(`📁 Arquivo de regras encontrado: ${rulesPath}`);

        // 2. Ler conteúdo das regras
        const rulesContent = fs.readFileSync(rulesPath, 'utf8');
        console.log(`📝 Regras lidas (${rulesContent.length} caracteres)`);

        // 3. Fazer deploy usando Firebase CLI (mais confiável)
        console.log(`🚀 Fazendo deploy via Firebase CLI...`);
        
        try {
            // Usar Firebase CLI para deploy (mesma abordagem do script PowerShell)
            const deployCommand = `firebase deploy --only firestore:rules --project gestaobilhares`;
            console.log(`🔧 Executando: ${deployCommand}`);
            
            const output = execSync(deployCommand, { 
                encoding: 'utf8',
                cwd: '..',
                stdio: 'pipe'
            });
            
            console.log(output);
            
            if (output.includes('Deploy complete!')) {
                console.log(`✅ Deploy concluído com sucesso!`);
            } else {
                throw new Error('Deploy falhou - verifique output acima');
            }
            
        } catch (cliError) {
            console.error('❌ Erro no Firebase CLI:', cliError.message);
            throw cliError;
        }

        console.log('\n' + '='.repeat(60));
        console.log('🎉 DEPLOY DE SECURITY RULES CONCLUÍDO!');
        console.log('='.repeat(60));
        console.log(`✅ Status: Regras aplicadas com sucesso`);
        console.log(`🔗 Firebase Console: https://console.firebase.google.com/project/gestaobilhares/firestore/rules`);
        console.log(`⏰ Término: ${new Date().toLocaleString('pt-BR')}`);

        console.log('\n🎯 Próximos passos:');
        console.log('1. Teste o aplicativo Android');
        console.log('2. Verifique se as regras estão bloqueando acessos não autorizados');
        console.log('3. Confirme que usuários autenticados podem acessar suas rotas');
        console.log('4. Monitore o Firebase Console para violations');

        return { success: true, message: 'Security Rules deployadas com sucesso' };

    } catch (error) {
        console.error('\n❌ ERRO NO DAS REGRAS:', error);
        throw error;
    } finally {
        // Fechar conexão Firebase
        admin.app().delete();
    }
}

/**
 * Função para backup das regras atuais antes do deploy
 */
async function backupCurrentRules() {
    try {
        console.log('💾 Fazendo backup das regras atuais...');
        
        const backupPath = `../firestore.rules.backup.${Date.now()}`;
        
        // Tentar fazer backup via Firebase CLI
        try {
            const backupCommand = `firebase firestore:rules get ../firestore.rules.backup.${Date.now()} --project gestaobilhares`;
            execSync(backupCommand, { cwd: '..', stdio: 'pipe' });
            console.log(`✅ Backup salvo via Firebase CLI`);
        } catch (backupError) {
            // Se falhar, criar backup do arquivo local
            if (fs.existsSync('../firestore.rules')) {
                fs.copyFileSync('../firestore.rules', backupPath);
                console.log(`✅ Backup local salvo em: ${backupPath}`);
            } else {
                console.log('ℹ️ Nenhuma regra atual encontrada para backup');
            }
        }
        
        return backupPath;
        
    } catch (error) {
        console.warn('⚠️ Erro ao fazer backup:', error.message);
        return null;
    }
}

/**
 * Função principal com backup + deploy
 */
async function main() {
    try {
        // Fazer backup antes de alterar
        await backupCurrentRules();
        
        // Fazer deploy das novas regras
        const result = await deploySecurityRules();
        
        console.log('\n🎉 PROCESSO CONCLUÍDO COM SUCESSO!');
        return result;
        
    } catch (error) {
        console.error('\n❌ FALHA NO PROCESSO:', error.message);
        process.exit(1);
    }
}

// Executar se chamado diretamente
if (require.main === module) {
    main().catch(error => {
        console.error('❌ Erro não tratado:', error);
        process.exit(1);
    });
}

module.exports = { 
    deploySecurityRules, 
    backupCurrentRules, 
    main 
};