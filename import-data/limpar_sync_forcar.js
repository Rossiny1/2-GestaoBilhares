/**
 * Limpar metadados de sincronização e forçar app a baixar dados
 * 
 * Este script limpa os timestamps de última sincronização
 * para forçar o app Android a baixar todos os dados do Firestore
 */

const admin = require('firebase-admin');
const fs = require('fs');

// Configurar Firebase Admin
try {
    const serviceAccount = require('./service-account.json');
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        projectId: 'gestaobilhares'
    });
    console.log('✅ Firebase Admin configurado!');
} catch (error) {
    console.error('❌ Erro ao configurar Firebase Admin:', error.message);
    process.exit(1);
}

const db = admin.firestore();

/**
 * Limpa metadados de sincronização para forçar download completo
 */
async function limparMetadadosSync() {
    console.log('🧹 Limpando metadados de sincronização...');

    try {
        // Caminho onde ficam os metadados de sync
        const syncMetadataPath = 'empresas/empresa_001/sync_metadata';

        // Obter todos os documentos de metadados
        const snapshot = await db.collection(syncMetadataPath).get();

        if (snapshot.empty) {
            console.log('📝 Nenhum metadado de sync encontrado');
            return;
        }

        console.log(`📊 Encontrados ${snapshot.docs.length} metadados de sync`);

        // Deletar todos os metadados de sync
        const batch = db.batch();
        snapshot.docs.forEach(doc => {
            batch.delete(doc.ref);
        });

        await batch.commit();
        console.log(`✅ ${snapshot.docs.length} metadados de sync removidos`);

        // Listar entidades que tiveram sync removido
        const entidadesRemovidas = snapshot.docs.map(doc => doc.id);
        console.log('📋 Entidades com reset de sync:', entidadesRemovidas);

        return entidadesRemovidas;

    } catch (error) {
        console.error('❌ Erro ao limpar metadados:', error);
        throw error;
    }
}

/**
 * Verifica dados importados no Firestore
 */
async function verificarDadosImportados() {
    console.log('\n🔍 Verificando dados importados...');

    try {
        // Verificar rotas
        const rotasSnapshot = await db.collection('empresas/empresa_001/entidades/rotas/items').get();
        console.log(`📁 Rotas encontradas: ${rotasSnapshot.docs.length}`);

        rotasSnapshot.docs.forEach(doc => {
            const rota = doc.data();
            console.log(`   📍 Rota: ${rota.nome} (ID: ${doc.id})`);
        });

        // Verificar clientes
        const clientesSnapshot = await db.collection('empresas/empresa_001/entidades/clientes/items').limit(5).get();
        console.log(`👥 Clientes encontrados (primeiros 5): ${clientesSnapshot.docs.length}`);

        clientesSnapshot.docs.forEach(doc => {
            const cliente = doc.data();
            console.log(`   👤 Cliente: ${cliente.nome} (ID: ${doc.id}, Rota: ${cliente.rota_id})`);
        });

        return {
            rotas: rotasSnapshot.docs.length,
            clientes: clientesSnapshot.docs.length
        };

    } catch (error) {
        console.error('❌ Erro ao verificar dados:', error);
        throw error;
    }
}

/**
 * Função principal
 */
async function main() {
    console.log('🧹 LIMPEZA DE SYNC E VERIFICAÇÃO');
    console.log('='.repeat(50));
    console.log(`⏰ Início: ${new Date().toLocaleString('pt-BR')}`);

    try {
        // 1. Limpar metadados de sync
        const entidadesRemovidas = await limparMetadadosSync();

        // 2. Verificar dados importados
        const dadosVerificados = await verificarDadosImportados();

        console.log('\n' + '='.repeat(50));
        console.log('🎉 PROCESSO CONCLUÍDO!');
        console.log('='.repeat(50));
        console.log(`📋 Entidades com reset: ${entidadesRemovidas.length}`);
        console.log(`📁 Rotas verificadas: ${dadosVerificados.rotas}`);
        console.log(`👥 Clientes verificados: ${dadosVerificados.clientes}`);

        console.log('\n🎯 PRÓXIMOS PASSOS:');
        console.log('1. Feche e reabra o app Android');
        console.log('2. Vá em "Rotas" e aguarde a sincronização');
        console.log('3. Verifique se os clientes aparecem na rota "037-Salinas"');
        console.log('4. Se necessário, force refresh manualmente');

        console.log('\n✅ LIMPEZA CONCLUÍDA COM SUCESSO!');

    } catch (error) {
        console.error('\n❌ ERRO FATAL:', error);
        process.exit(1);
    } finally {
        // Fechar conexão Firebase
        admin.app().delete();
    }
}

// Executar script
if (require.main === module) {
    main().catch(error => {
        console.error('❌ Erro não tratado:', error);
        process.exit(1);
    });
}

module.exports = { main, limparMetadadosSync, verificarDadosImportados };
