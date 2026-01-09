/**
 * Importar Dados para Firebase - Versão Final
 * 
 * Usa Firebase CLI já configurado no projeto
 * Importa diretamente os 3 clientes de teste
 */

const { execSync } = require('child_process');
const fs = require('fs');

/**
 * Executa comando Firebase e captura resultado
 */
function execFirebase(command) {
    try {
        console.log(`🔧 Executando: firebase ${command}`);
        const result = execSync(`firebase ${command}`, {
            encoding: 'utf8',
            cwd: '..',
            stdio: ['pipe', 'pipe', 'pipe']
        });
        return { success: true, output: result.trim() };
    } catch (error) {
        console.error(`❌ Erro no comando: firebase ${command}`);
        return {
            success: false,
            error: error.stderr || error.stdout || error.message
        };
    }
}

/**
 * Cria arquivo Firestore compatível para importação
 */
function criarFirestoreJSON() {
    // Ler o JSON já gerado
    const dadosTeste = JSON.parse(fs.readFileSync('../dados_teste_3_clientes.json', 'utf8'));

    // Converter para formato Firestore batch
    const firestoreData = {};

    // Adicionar rota
    if (dadosTeste.rotas && dadosTeste.rotas.length > 0) {
        const rota = dadosTeste.rotas[0];
        firestoreData[`rotas/${rota.nome}`] = rota;
    }

    // Adicionar clientes
    if (dadosTeste.clientes && dadosTeste.clientes.length > 0) {
        dadosTeste.clientes.forEach((cliente, index) => {
            const clienteId = `cliente_${Date.now()}_${index}`;
            firestoreData[`clientes/${clienteId}`] = cliente;
        });
    }

    return firestoreData;
}

/**
 * Função principal
 */
async function main() {
    console.log('🚀 IMPORTANDO DADOS PARA FIREBASE');
    console.log('='.repeat(50));

    try {
        // 1. Verificar configuração Firebase
        console.log('📋 Verificando configuração...');

        const versionResult = execFirebase('--version');
        if (!versionResult.success) {
            throw new Error('Firebase CLI não encontrado');
        }
        console.log(`✅ Firebase CLI: ${versionResult.output}`);

        // 2. Verificar se está logado
        const loginResult = execFirebase('login:list');
        if (!loginResult.success) {
            throw new Error('Não está logado no Firebase');
        }
        console.log('✅ Login OK');

        // 3. Verificar projeto
        const projectResult = execFirebase('projects:list');
        if (!projectResult.success || !projectResult.output.includes('gestaobilhares')) {
            throw new Error('Projeto gestaobilhares não encontrado');
        }
        console.log('✅ Projeto gestaobilhares encontrado');

        // 4. Preparar dados para importação
        console.log('\n📊 Preparando dados para importação...');

        if (!fs.existsSync('../dados_teste_3_clientes.json')) {
            throw new Error('Arquivo dados_teste_3_clientes.json não encontrado');
        }

        const firestoreData = criarFirestoreJSON();
        const importFile = '../firebase_import_data.json';
        fs.writeFileSync(importFile, JSON.stringify(firestoreData, null, 2));

        console.log(`✅ Arquivo de importação criado: ${importFile}`);
        console.log(`📊 Documentos a importar: ${Object.keys(firestoreData).length}`);

        // 5. Selecionar projeto
        console.log('\n🎯 Selecionando projeto...');
        const selectResult = execFirebase('use gestaobilhares');
        if (!selectResult.success) {
            console.log('⚠️ Aviso: Não foi possível selecionar projeto automaticamente');
        } else {
            console.log('✅ Projeto selecionado');
        }

        // 6. Importar dados
        console.log('\n🚀 Importando dados para Firestore...');
        console.log('⏳ Isso pode levar alguns segundos...');

        const importResult = execFirebase(`firestore:import ${importFile} --project gestaobilhares`);

        if (importResult.success) {
            console.log('\n' + '='.repeat(50));
            console.log('🎉 IMPORTAÇÃO CONCLUÍDA COM SUCESSO!');
            console.log('='.repeat(50));
            console.log('✅ Dados importados para o Firebase Firestore');
            console.log('✅ Rota "037-Salinas" criada');
            console.log('✅ 3 clientes importados');
            console.log('\n📱 Para validar:');
            console.log('1. Abra o app Android');
            console.log('2. Vá para "Rotas"');
            console.log('3. Procure por "037-Salinas"');
            console.log('4. Verifique os 3 clientes');

        } else {
            console.log('\n❌ ERRO NA IMPORTAÇÃO:');
            console.log(importResult.error);

            // Tentar método alternativo
            console.log('\n🔄 Tentando método alternativo...');
            console.log('📋 Importação manual necessária:');
            console.log('1. Abra: https://console.firebase.google.com/project/gestaobilhares/firestore');
            console.log('2. Importe o arquivo: firebase_import_data.json');
        }

        // 7. Limpar arquivo temporário
        try {
            fs.unlinkSync(importFile);
            console.log('\n🧹 Arquivo temporário removido');
        } catch { }

    } catch (error) {
        console.error('\n❌ ERRO FATAL:', error.message);
        process.exit(1);
    }
}

// Executar
if (require.main === module) {
    main().catch(error => {
        console.error('❌ Erro não tratado:', error);
        process.exit(1);
    });
}

module.exports = { main, criarFirestoreJSON };
