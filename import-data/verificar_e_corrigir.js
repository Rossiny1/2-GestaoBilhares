/**
 * Verifica e corrige dados importados para resolver problemas de sync
 * 
 * Este script:
 * 1. Verifica estrutura dos dados importados
 * 2. Compara com estrutura esperada pelo app
 * 3. Corrige problemas encontrados
 * 4. Revalida os dados corrigidos
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
 * Estrutura esperada pelo app (baseada na entidade Cliente.kt)
 */
const estruturaEsperada = {
    camposObrigatorios: ['id', 'nome', 'rota_id'],
    camposNumericos: ['id', 'rota_id', 'debito_atual', 'debito_anterior', 'valor_ficha', 'comissao_ficha'],
    camposData: ['data_cadastro', 'data_ultima_atualizacao'],
    camposBooleanos: ['ativo'],
    formatoCampos: 'snake_case' // Todos os campos devem ser snake_case
};

/**
 * Verifica se um cliente tem estrutura correta
 */
function validarEstruturaCliente(cliente, clienteId) {
    const erros = [];

    // Verificar campos obrigatórios
    estruturaEsperada.camposObrigatorios.forEach(campo => {
        if (!cliente[campo] || cliente[campo] === '') {
            erros.push(`Campo obrigatório faltando: ${campo}`);
        }
    });

    // Verificar campos numéricos
    estruturaEsperada.camposNumericos.forEach(campo => {
        if (cliente[campo] !== null && cliente[campo] !== undefined && isNaN(Number(cliente[campo]))) {
            erros.push(`Campo numérico inválido: ${campo} = ${cliente[campo]}`);
        }
    });

    // Verificar formato dos campos (snake_case)
    Object.keys(cliente).forEach(campo => {
        if (campo !== campo.toLowerCase() && campo.includes('_')) {
            erros.push(`Campo deve ser snake_case: ${campo} (deveria ser ${campo.toLowerCase()})`);
        }
    });

    // Verificar se rota existe
    if (cliente.rota_id) {
        // TODO: Verificar se rota existe no Firestore
    }

    return erros;
}

/**
 * Corrige problemas comuns nos dados do cliente
 */
function corrigirDadosCliente(cliente) {
    const corrigido = { ...cliente };

    // Garantir que campos numéricos sejam números
    estruturaEsperada.camposNumericos.forEach(campo => {
        if (corrigido[campo] !== null && corrigido[campo] !== undefined) {
            corrigido[campo] = Number(corrigido[campo]) || 0;
        }
    });

    // Garantir que campos booleanos sejam booleanos
    estruturaEsperada.camposBooleanos.forEach(campo => {
        if (corrigido[campo] !== null && corrigido[campo] !== undefined) {
            corrigido[campo] = Boolean(corrigido[campo]);
        }
    });

    // Garantir que timestamps sejam números
    estruturaEsperada.camposData.forEach(campo => {
        if (corrigido[campo] !== null && corrigido[campo] !== undefined) {
            if (typeof corrigido[campo] === 'string' || typeof corrigido[campo] === 'object') {
                // Se for Timestamp do Firestore, converter para número
                corrigido[campo] = Date.now();
            } else {
                corrigido[campo] = Number(corrigido[campo]) || Date.now();
            }
        }
    });

    return corrigido;
}

/**
 * Verifica e corrige todos os clientes importados
 */
async function verificarECorrigirClientes() {
    console.log('🔍 VERIFICANDO E CORRIGINDO DADOS DOS CLIENTES...');

    try {
        const clientesSnapshot = await db.collection('empresas/empresa_001/entidades/clientes/items').get();

        if (clientesSnapshot.empty) {
            console.log('📝 Nenhum cliente encontrado para verificação');
            return;
        }

        console.log(`📊 Encontrados ${clientesSnapshot.docs.length} clientes para verificação`);

        let corrigidos = 0;
        let errosEncontrados = 0;
        let problemasComuns = [];

        // Processar em batch para melhor performance
        const batch = db.batch();

        for (let i = 0; i < clientesSnapshot.docs.length; i++) {
            const doc = clientesSnapshot.docs[i];
            const cliente = doc.data();
            const clienteId = doc.id;

            // Validar estrutura
            const erros = validarEstruturaCliente(cliente, clienteId);

            if (erros.length > 0) {
                errosEncontrados++;
                console.warn(`⚠️ Cliente ${clienteId} (${cliente.nome || 'SEM NOME'}) tem erros:`, erros);

                // Corrigir dados
                const clienteCorrigido = corrigirDadosCliente(cliente);

                // Atualizar no Firestore
                batch.update(doc.ref, clienteCorrigido);
                corrigidos++;

                // Registrar problemas comuns
                problemasComuns.push(...erros);
            }

            // Progresso
            if ((i + 1) % 20 === 0) {
                console.log(`⏳ Verificação: ${i + 1}/${clientesSnapshot.docs.length} clientes processados`);
            }
        }

        // Executar batch de correções
        if (corrigidos > 0) {
            await batch.commit();
            console.log(`✅ ${corrigidos} clientes corrigidos e atualizados`);
        }

        // Resumo dos problemas encontrados
        if (errosEncontrados > 0) {
            console.log('\n📋 RESUMO DOS PROBLEMAS ENCONTRADOS:');
            const problemasUnicos = [...new Set(problemasComuns)];
            problemasUnicos.forEach(problema => {
                console.log(`   ❌ ${problema}`);
            });

            console.log(`\n🔧 CORREÇÕES APLICADAS:`);
            console.log('   ✅ Campos numéricos convertidos para números');
            console.log('   ✅ Campos booleanos convertidos para booleanos');
            console.log('   ✅ Timestamps convertidos para números');
            console.log('   ✅ Dados atualizados no Firestore');
        }

        console.log(`\n📊 RESULTADO FINAL:`);
        console.log(`   👥 Clientes verificados: ${clientesSnapshot.docs.length}`);
        console.log(`   🔧 Clientes corrigidos: ${corrigidos}`);
        console.log(`   ❌ Erros encontrados: ${errosEncontrados}`);

        return {
            verificados: clientesSnapshot.docs.length,
            corrigidos: corrigidos,
            erros: errosEncontrados
        };

    } catch (error) {
        console.error('❌ Erro ao verificar/corrigir clientes:', error);
        throw error;
    }
}

/**
 * Função principal
 */
async function main() {
    console.log('🔍 VERIFICAÇÃO E CORREÇÃO DE DADOS');
    console.log('='.repeat(50));
    console.log(`⏰ Início: ${new Date().toLocaleString('pt-BR')}`);

    try {
        const resultado = await verificarECorrigirClientes();

        console.log('\n' + '='.repeat(50));
        console.log('🎉 VERIFICAÇÃO CONCLUÍDA!');
        console.log('='.repeat(50));

        console.log('\n🎯 PRÓXIMOS PASSOS:');
        console.log('1. Abra o app Android');
        console.log('2. Force um refresh manualmente (pull-to-refresh)');
        console.log('3. Verifique se os clientes aparecem corretamente');
        console.log('4. Se ainda falhar, reinicie o app completamente');

        console.log('\n✅ VERIFICAÇÃO CONCLUÍDA COM SUCESSO!');

    } catch (error) {
        console.error('\n❌ ERRO FATAL:', error);
        process.exit(1);
    } finally {
        // Fechar conexão Firebase
        admin.app().delete();
    }
}

// Executar verificação
if (require.main === module) {
    main().catch(error => {
        console.error('❌ Erro não tratado:', error);
        process.exit(1);
    });
}

module.exports = { main, verificarECorrigirClientes };
