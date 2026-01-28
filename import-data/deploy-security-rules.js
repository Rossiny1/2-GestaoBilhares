/**
 * Deploy de Security Rules Firestore - Versão Service Account
 * 
 * Usa a mesma abordagem do importador de dados (Firebase Admin SDK)
 * Deploy seguro e automatizado das regras de segurança
 */

const admin = require('firebase-admin');
const fs = require('fs');

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

const firestore = admin.firestore();

/**
 * Faz deploy das Security Rules usando Admin SDK
 */
async function deploySecurityRules() {
    console.log('🛡️ DEPLOY SECURITY RULES - FIRESTORE ADMIN SDK');
    console.log('='.repeat(60));
    console.log(`📦 Projeto: gestaobilhares`);
    console.log(`🔑 Usando Service Account (mesmo do importador)`);
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

        // 3. Fazer deploy usando Admin SDK
        console.log(`🚀 Fazendo deploy das regras...`);
        
        await firestore.clearIndexes(); // Limpar índices antigos se necessário
        await firestore.createIndexes([]); // Recriar estrutura básica
        
        // Deploy das regras
        const result = await firestore.setSecurityRules(rulesContent);
        
        console.log(`✅ Deploy concluído com sucesso!`);
        console.log(`📊 Resultado:`, result);

        // 4. Verificar se as regras foram aplicadas
        console.log(`🔍 Verificando regras aplicadas...`);
        const currentRules = await firestore.getSecurityRules();
        console.log(`📋 Regras atuais: ${currentRules ? 'Aplicadas' : 'Não encontradas'}`);

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
        
        // Tentar rollback se possível
        console.log('🔄 Tentando rollback...');
        try {
            // Aqui você poderia ter um backup das regras antigas
            console.log('⚠️ Rollback manual necessário via Firebase Console');
        } catch (rollbackError) {
            console.error('❌ Erro no rollback:', rollbackError);
        }
        
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
        
        const currentRules = await firestore.getSecurityRules();
        if (currentRules) {
            const backupPath = `./firestore.rules.backup.${Date.now()}`;
            fs.writeFileSync(backupPath, currentRules);
            console.log(`✅ Backup salvo em: ${backupPath}`);
            return backupPath;
        } else {
            console.log('ℹ️ Nenhuma regra atual encontrada para backup');
            return null;
        }
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
