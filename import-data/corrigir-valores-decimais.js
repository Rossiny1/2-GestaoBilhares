/**
 * 🚨 CORREÇÃO DE VALORES DECIMAIS MULTIPLICADOS POR 10
 * 
 * Problema: Importador armazenou valores multiplicados por 10
 * Solução: Dividir valor_ficha e comissao_ficha por 10
 * 
 * Execute: node corrigir-valores-decimais.js
 */

const admin = require('firebase-admin');
const serviceAccount = require('./service-account.json');

// Inicializar Firebase Admin
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    projectId: 'gestaobilhares'
});

const db = admin.firestore();
const empresaId = 'empresa_001';

/**
 * Corrige valores multiplicados por 10
 */
async function corrigirValoresDecimais() {
    console.log('🚨 INICIANDO CORREÇÃO DE VALORES DECIMAIS');
    console.log('=====================================');

    try {
        // 1. Buscar todos os clientes
        const clientesRef = db.collection(`empresas/${empresaId}/entidades/clientes/items`);
        const snapshot = await clientesRef.get();

        console.log(`📊 Encontrados ${snapshot.size} clientes para analisar`);

        let corrigidos = 0;
        let erros = 0;

        // 2. Processar cada cliente
        for (const doc of snapshot.docs) {
            try {
                const cliente = doc.data();
                const clienteId = doc.id;

                let precisaCorrecao = false;
                const updates = {};

                // 3. Verificar e corrigir valor_ficha
                if (cliente.valor_ficha && cliente.valor_ficha > 10) {
                    const valorCorrigido = cliente.valor_ficha / 10;
                    updates.valor_ficha = valorCorrigido;
                    precisaCorrecao = true;

                    console.log(`🔧 Cliente ${clienteId}: valor_ficha ${cliente.valor_ficha} → ${valorCorrigido}`);
                }

                // 4. Verificar e corrigir comissao_ficha
                if (cliente.comissao_ficha && cliente.comissao_ficha > 5) {
                    const comissaoCorrigida = cliente.comissao_ficha / 10;
                    updates.comissao_ficha = comissaoCorrigida;
                    precisaCorrecao = true;

                    console.log(`🔧 Cliente ${clienteId}: comissao_ficha ${cliente.comissao_ficha} → ${comissaoCorrigida}`);
                }

                // 5. Aplicar correção se necessário
                if (precisaCorrecao) {
                    await doc.ref.update(updates);
                    corrigidos++;
                    console.log(`✅ Cliente ${clienteId} corrigido com sucesso`);
                } else {
                    console.log(`⏭️  Cliente ${clienteId} não precisa de correção`);
                }

            } catch (error) {
                console.error(`❌ Erro ao corrigir cliente ${doc.id}:`, error);
                erros++;
            }
        }

        // 6. Resumo final
        console.log('\n📋 RESUMO DA CORREÇÃO');
        console.log('====================');
        console.log(`✅ Clientes corrigidos: ${corrigidos}`);
        console.log(`❌ Erros: ${erros}`);
        console.log(`📊 Total processados: ${snapshot.size}`);

        if (corrigidos > 0) {
            console.log('\n🎉 CORREÇÃO CONCLUÍDA COM SUCESSO!');
            console.log('Valores agora devem aparecer corretamente no app:');
            console.log('- valor_ficha: R$ 1,50 (era R$ 15,00)');
            console.log('- comissao_ficha: R$ 0,60 (era R$ 6,00)');
        } else {
            console.log('\n✅ Nenhum cliente precisou de correção');
        }

    } catch (error) {
        console.error('❌ ERRO FATAL NA CORREÇÃO:', error);
    } finally {
        // Fechar conexão
        process.exit(0);
    }
}

/**
 * Verificar valores antes da correção
 */
async function verificarValoresAtuais() {
    console.log('🔍 VERIFICANDO VALORES ATUAIS');
    console.log('============================');

    try {
        const clientesRef = db.collection(`empresas/${empresaId}/entidades/clientes/items`);
        const snapshot = await clientesRef.limit(5).get();

        console.log('📊 Amostra de valores atuais:');
        snapshot.forEach(doc => {
            const cliente = doc.data();
            console.log(`Cliente ${doc.id}:`);
            console.log(`  - valor_ficha: ${cliente.valor_ficha}`);
            console.log(`  - comissao_ficha: ${cliente.comissao_ficha}`);
            console.log('');
        });

    } catch (error) {
        console.error('❌ Erro ao verificar valores:', error);
    }
}

// Executar basedo no argumento
const comando = process.argv[2];

if (comando === 'verificar') {
    verificarValoresAtuais();
} else if (comando === 'corrigir') {
    corrigirValoresDecimais();
} else {
    console.log('📖 USO:');
    console.log('  node corrigir-valores-decimais.js verificar  # Verificar valores atuais');
    console.log('  node corrigir-valores-decimais.js corrigir   # Aplicar correção');
    process.exit(1);
}
