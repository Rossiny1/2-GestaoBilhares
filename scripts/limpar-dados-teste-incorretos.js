/**
 * Script para limpar dados de teste incorretos criados na raiz do Firestore
 * Remove collections que foram criadas erroneamente durante implementação
 * Preserva dados em empresas/empresa_001/* (se existirem)
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Configurar Firebase Admin com Service Account
try {
    const serviceAccountPath = path.join(__dirname, '..', 'import-data', 'service-account.json');
    const serviceAccount = require(serviceAccountPath);
    
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        projectId: 'gestaobilhares'
    });
    console.log('✅ Firebase Admin configurado com Service Account!');
} catch (error) {
    console.error('❌ Erro ao configurar Firebase Admin:', error.message);
    process.exit(1);
}

const db = admin.firestore();

/**
 * Deleta uma collection inteira da raiz do Firestore
 */
async function deletarCollectionRaiz(collectionName) {
    const collectionRef = db.collection(collectionName);
    const snapshot = await collectionRef.get();

    if (snapshot.empty) {
        console.log(`✅ Collection ${collectionName} não existe ou já está vazia`);
        return 0;
    }

    console.log(`🗑️ Deletando ${snapshot.size} documentos de ${collectionName}...`);

    // Firestore limita batch operations a 500 documentos
    const batchSize = 500;
    let totalDeletados = 0;

    while (true) {
        const batch = db.batch();
        let docsInBatch = 0;

        for (const doc of snapshot.docs) {
            if (docsInBatch >= batchSize) break;
            
            batch.delete(doc.ref);
            docsInBatch++;
        }

        if (docsInBatch === 0) break;

        await batch.commit();
        totalDeletados += docsInBatch;
        
        console.log(`   📦 Batch deletado: ${docsInBatch} documentos`);
        
        // Se ainda houver documentos, buscar próximo lote
        if (snapshot.size > batchSize) {
            const nextSnapshot = await collectionRef.limit(batchSize).get();
            if (nextSnapshot.empty) break;
        } else {
            break;
        }
    }

    console.log(`✅ Collection ${collectionName}: ${totalDeletados} documentos deletados`);
    return totalDeletados;
}

/**
 * Verifica se dados em empresas/empresa_001 existem
 */
async function verificarDadosEmpresas() {
    try {
        console.log('\n🔍 Verificando dados em empresas/empresa_001/*...');
        
        // Verificar colaboradores
        const colaboradoresSnapshot = await db
            .collection('empresas')
            .document('empresa_001')
            .collection('colaboradores')
            .limit(1)
            .get();
        
        console.log(`   👥 Colaboradores em empresa_001: ${colaboradoresSnapshot.size} documentos`);

        // Verificar entidades
        const entidadesCollections = ['rotas', 'clientes', 'acertos', 'mesas'];
        
        for (const entityName of entidadesCollections) {
            const entitySnapshot = await db
                .collection('empresas')
                .document('empresa_001')
                .collection('entidades')
                .document(entityName)
                .collection('items')
                .limit(1)
                .get();
            
            console.log(`   📁 ${entityName} em empresa_001: ${entitySnapshot.size} documentos`);
        }

        return true;
    } catch (error) {
        console.log(`   ℹ️ empresas/empresa_001 não existe ou erro de acesso: ${error.message}`);
        return false;
    }
}

/**
 * Função principal de limpeza
 */
async function limparDadosIncorretos() {
    console.log('🧹 INICIANDO LIMPEZA DE DADOS INCORRETOS');
    console.log('='.repeat(60));
    console.log(`📦 Projeto: gestaobilhares`);
    console.log(`⏰ Início: ${new Date().toLocaleString('pt-BR')}`);

    // Collections que foram criadas incorretamente na raiz
    const collectionsParaDeletar = [
        'clientes',
        'acertos', 
        'mesas',
        'rotas',
        'usuarios',
        'historico_manutencao'
    ];

    let totalDeletados = 0;
    const resultados = [];

    try {
        // Verificar dados existentes em empresas/empresa_001
        await verificarDadosEmpresas();

        console.log('\n🗑️ INICIANDO DELEÇÃO DE COLLECTIONS NA RAIZ...');

        // Deletar cada collection incorreta
        for (const collection of collectionsParaDeletar) {
            console.log(`\n--- Processando: ${collection} ---`);
            const deletados = await deletarCollectionRaiz(collection);
            totalDeletados += deletados;
            resultados.push({ collection, deletados });
        }

        // Resumo final
        console.log('\n' + '='.repeat(60));
        console.log('🎉 LIMPEZA CONCLUÍDA!');
        console.log('='.repeat(60));
        console.log(`📊 Total geral de documentos deletados: ${totalDeletados}`);
        
        console.log('\n📋 Detalhes por collection:');
        for (const resultado of resultados) {
            console.log(`   ${resultado.collection}: ${resultado.deletados} documentos`);
        }

        console.log('\n✅ Dados em empresas/empresa_001/* foram PRESERVADOS');
        console.log('⚠️ Collections na raiz foram removidas');

        console.log('\n🎯 Próximo passo: Implementar Security Rules hierárquicas');

    } catch (error) {
        console.error('\n❌ ERRO NA LIMPEZA:', error);
        throw error;
    } finally {
        // Fechar conexão Firebase
        admin.app().delete();
    }
}

// Executar se chamado diretamente
if (require.main === module) {
    limparDadosIncorretos().catch(error => {
        console.error('❌ Erro não tratado:', error);
        process.exit(1);
    });
}

module.exports = { 
    deletarCollectionRaiz,
    verificarDadosEmpresas,
    limparDadosIncorretos
};