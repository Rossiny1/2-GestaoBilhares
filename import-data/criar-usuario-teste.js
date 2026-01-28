/**
 * Script para criar usuário de teste no Firebase Auth
 * e validar criação de colaborador no Firestore
 */

const admin = require('firebase-admin');
const path = require('path');

// Configurar Firebase Admin com Service Account
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

const auth = admin.auth();
const db = admin.firestore();

/**
 * Cria usuário no Firebase Auth
 */
async function criarUsuarioAuth(email, senha, nome) {
    try {
        console.log(`👤 Criando usuário Auth: ${email}`);
        
        const userRecord = await auth.createUser({
            email: email,
            password: senha,
            displayName: nome,
            emailVerified: false
        });
        
        console.log(`✅ Usuário Auth criado: ${userRecord.uid}`);
        console.log(`   📧 Email: ${userRecord.email}`);
        console.log(`   📛 Nome: ${userRecord.displayName}`);
        
        return userRecord;
    } catch (error) {
        if (error.code === 'auth/email-already-exists') {
            console.log(`ℹ️ Usuário ${email} já existe no Auth`);
            // Buscar usuário existente
            const userRecord = await auth.getUserByEmail(email);
            return userRecord;
        } else {
            throw error;
        }
    }
}

/**
 * Cria documento de colaborador no Firestore
 */
async function criarColaboradorFirestore(userRecord) {
    try {
        console.log(`📝 Criando colaborador no Firestore...`);
        
        const colaboradorData = {
            firebase_uid: userRecord.uid,
            nome: userRecord.displayName || 'Usuário Teste',
            email: userRecord.email,
            empresa_id: 'empresa_001',
            nivel_acesso: 'COLABORADOR',
            aprovado: false,  // Inicia não aprovado
            rotasPermitidas: [],  // Sem rotas inicialmente
            data_cadastro: admin.firestore.FieldValue.serverTimestamp(),
            data_ultima_atualizacao: admin.firestore.FieldValue.serverTimestamp(),
            ativo: true
        };
        
        const docRef = db
            .collection('empresas')
            .doc('empresa_001')
            .collection('colaboradores')
            .doc(userRecord.uid);
        
        await docRef.set(colaboradorData);
        
        console.log(`✅ Colaborador criado no Firestore!`);
        console.log(`   📍 Path: empresas/empresa_001/colaboradores/${userRecord.uid}`);
        console.log(`   👤 Nome: ${colaboradorData.nome}`);
        console.log(`   🏢 Empresa: ${colaboradorData.empresa_id}`);
        console.log(`   ✅ Aprovado: ${colaboradorData.aprovado}`);
        console.log(`   🛣️ Rotas: ${colaboradorData.rotasPermitidas.length} rotas`);
        
        return docRef;
    } catch (error) {
        console.error('❌ Erro ao criar colaborador no Firestore:', error);
        throw error;
    }
}

/**
 * Verifica se colaborador foi criado corretamente
 */
async function verificarColaborador(uid) {
    try {
        console.log(`🔍 Verificando colaborador criado...`);
        
        const docRef = db
            .collection('empresas')
            .doc('empresa_001')
            .collection('colaboradores')
            .doc(uid);
        
        const docSnapshot = await docRef.get();
        
        if (docSnapshot.exists) {
            const data = docSnapshot.data();
            console.log(`✅ Colaborador encontrado e verificado!`);
            console.log(`   📋 Campos: ${Object.keys(data).join(', ')}`);
            console.log(`   📅 Data cadastro: ${data.data_cadastro?.toDate?.() || data.data_cadastro}`);
            console.log(`   🔄 Data atualização: ${data.data_ultima_atualizacao?.toDate?.() || data.data_ultima_atualizacao}`);
            
            return true;
        } else {
            console.log(`❌ Colaborador não encontrado no Firestore`);
            return false;
        }
    } catch (error) {
        console.error('❌ Erro ao verificar colaborador:', error);
        return false;
    }
}

/**
 * Função principal
 */
async function main() {
    console.log('🧪 TESTE 1: CRIAÇÃO DE COLABORADOR (PRIMEIRO ACESSO)');
    console.log('='.repeat(60));
    console.log(`📦 Projeto: gestaobilhares`);
    console.log(`⏰ Início: ${new Date().toLocaleString('pt-BR')}`);

    // Dados do usuário de teste
    const email = 'teste@example.com';
    const senha = 'senha123';
    const nome = 'Usuário Teste Security Rules';

    try {
        // 1. Criar usuário no Firebase Auth
        const userRecord = await criarUsuarioAuth(email, senha, nome);
        
        // 2. Criar colaborador no Firestore
        await criarColaboradorFirestore(userRecord);
        
        // 3. Verificar se foi criado corretamente
        const sucesso = await verificarColaborador(userRecord.uid);
        
        // Resumo
        console.log('\n' + '='.repeat(60));
        console.log('🎉 TESTE 1 CONCLUÍDO!');
        console.log('='.repeat(60));
        
        if (sucesso) {
            console.log('✅ SUCESSO: Colaborador criado corretamente');
            console.log('📱 Próximo passo: Fazer login no app com este usuário');
            console.log(`👤 Login: ${email} / ${senha}`);
            console.log('⚠️ Usuário criado com aprovado=false (aguardando aprovação admin)');
        } else {
            console.log('❌ FALHA: Problema na criação do colaborador');
        }

        console.log('\n🎯 Instruções para o app:');
        console.log('1. Abra o app Android');
        console.log(`2. Faça login com: ${email}`);
        console.log('3. Verifique se o documento é criado em empresas/empresa_001/colaboradores/');
        console.log('4. App deve mostrar "aguardando aprovação"');

    } catch (error) {
        console.error('\n❌ ERRO NO TESTE:', error);
        throw error;
    } finally {
        // Fechar conexão Firebase
        admin.app().delete();
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
    criarUsuarioAuth,
    criarColaboradorFirestore,
    verificarColaborador,
    main
};