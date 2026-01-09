/**
 * Teste de Importação - 3 Clientes Apenas
 * 
 * Uso: node teste_3_clientes.js
 * Objetivo: Testar importação com amostra pequena antes de processar todos
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
    console.log('✅ Firebase Admin configurado com sucesso');
} catch (error) {
    console.error('❌ Erro ao configurar Firebase Admin:');
    console.error('1. Faça download do service account: https://console.firebase.google.com/project/gestaobilhares/settings/serviceaccounts/adminsdk');
    console.error('2. Salve como service-account.json na mesma pasta');
    console.error('3. Execute: npm install firebase-admin');
    process.exit(1);
}

const db = admin.firestore();

/**
 * Converte string de valor monetário (R$ 130,00) para número
 */
function converterValorMonetario(valorStr) {
    if (!valorStr) return 0;
    return parseFloat(valorStr
        .replace('R$', '')
        .replace('.', '')
        .replace(',', '.')
        .trim()) || 0;
}

/**
 * Converte string de data (DD/MM/YYYY) para timestamp
 */
function converterData(dataStr) {
    if (!dataStr) return admin.firestore.FieldValue.serverTimestamp();

    try {
        // Tentar formato com hora
        const dataComHora = new Date(dataStr);
        if (!isNaN(dataComHora.getTime())) {
            return admin.firestore.Timestamp.fromDate(dataComHora);
        }

        // Tentar formato só data
        const [dia, mes, ano] = dataStr.split('/');
        if (dia && mes && ano) {
            const dataApenas = new Date(`${ano}-${mes}-${dia}`);
            return admin.firestore.Timestamp.fromDate(dataApenas);
        }

        return admin.firestore.FieldValue.serverTimestamp();
    } catch (error) {
        console.warn(`⚠️ Data inválida: ${dataStr}`);
        return admin.firestore.FieldValue.serverTimestamp();
    }
}

/**
 * Mapeia linha do CSV para documento Firebase
 */
function mapearLinhaParaCliente(linha, rotaId) {
    // Extrair campos do CSV (baseado na análise anterior)
    const campos = linha.split(';');

    const nome = campos[1] ? campos[1].replace(/"/g, '').trim() : '';
    const cpfCnpj = campos[2] ? campos[2].replace(/"/g, '').trim() : null;
    const endereco = campos[3] ? campos[3].replace(/"/g, '').trim() : '';
    const cidade = campos[4] ? campos[4].replace(/"/g, '').trim() : '';
    const estado = campos[5] ? campos[5].replace(/"/g, '').trim() : '';
    const telefone = campos[6] ? campos[6].replace(/"/g, '').trim() : null;
    const telefone2 = campos[7] ? campos[7].replace(/"/g, '').trim() : null;
    const dataCadastroStr = campos[9] ? campos[9].trim() : '';
    const debitoAtualStr = campos[11] ? campos[11].trim() : '';
    const observacoes = campos[12] ? campos[12].replace(/"/g, '').trim() : '';
    const valorFichaStr = campos[13] ? campos[13].trim() : '';

    // Validações básicas
    if (!nome) {
        throw new Error('Nome do cliente é obrigatório');
    }

    // Verificar se cliente está inativo
    const ativo = !observacoes.toLowerCase().includes('mesa retirada');

    return {
        nome: nome,
        nomeFantasia: null,
        cpfCnpj: cpfCnpj || null,
        telefone: telefone || null,
        telefone2: telefone2 || null,
        email: null,
        endereco: endereco,
        bairro: null,
        cidade: cidade,
        estado: estado,
        cep: null,
        latitude: null,
        longitude: null,
        precisaoGps: null,
        dataCapturaGps: null,
        rotaId: rotaId,
        valorFicha: converterValorMonetario(valorFichaStr),
        comissaoFicha: 0.0,
        numeroContrato: null,
        debitoAnterior: 0.0,
        debitoAtual: converterValorMonetario(debitoAtualStr),
        ativo: ativo,
        observacoes: observacoes,
        dataCadastro: converterData(dataCadastroStr),
        dataUltimaAtualizacao: admin.firestore.FieldValue.serverTimestamp()
    };
}

/**
 * Cria ou obtém rota no Firestore
 */
async function obterOuCriarRota(nomeRota, descricao = '') {
    try {
        // Buscar rota existente
        const rotaSnapshot = await db.collection('rotas')
            .where('nome', '==', nomeRota)
            .limit(1)
            .get();

        if (!rotaSnapshot.empty) {
            const rotaDoc = rotaSnapshot.docs[0];
            console.log(`✅ Rota encontrada: ${nomeRota} (ID: ${rotaDoc.id})`);
            return rotaDoc.id;
        }

        // Criar nova rota
        const novaRota = {
            nome: nomeRota,
            descricao: descricao || `Rota importada via CSV em ${new Date().toISOString()}`,
            colaboradorResponsavel: 'Sistema',
            cidades: 'Importação CSV',
            ativa: true,
            cor: '#6200EA',
            dataCriacao: admin.firestore.FieldValue.serverTimestamp(),
            dataAtualizacao: admin.firestore.FieldValue.serverTimestamp(),
            statusAtual: 'PAUSADA',
            cicloAcertoAtual: 1,
            anoCiclo: new Date().getFullYear()
        };

        const rotaRef = await db.collection('rotas').add(novaRota);
        console.log(`🆕 Rota criada: ${nomeRota} (ID: ${rotaRef.id})`);
        return rotaRef.id;

    } catch (error) {
        console.error(`❌ Erro ao criar/obter rota ${nomeRota}:`, error);
        throw error;
    }
}

/**
 * Função principal de teste com 3 clientes
 */
async function main() {
    console.log('🧪 TESTE DE IMPORTAÇÃO - 3 CLIENTES');
    console.log('='.repeat(50));

    try {
        // 1. Ler arquivo CSV
        const caminhoArquivo = '../anexos/Cadastro Clientes- Rota Bahia.csv';

        if (!fs.existsSync(caminhoArquivo)) {
            throw new Error(`Arquivo não encontrado: ${caminhoArquivo}`);
        }

        console.log(`📁 Lendo arquivo: ${caminhoArquivo}`);

        const conteudo = fs.readFileSync(caminhoArquivo, 'utf8');
        const linhas = conteudo.split('\n');

        // Pegar as 3 primeiras linhas de dados (pular cabeçalho)
        const linhasTeste = linhas.slice(1, 4); // Linhas 2, 3, 4

        console.log(`📊 Selecionadas ${linhasTeste.length} linhas para teste`);

        // 2. Criar rota 037-Salinas
        const rotaId = await obterOuCriarRota('037-Salinas', 'Rota de teste - 3 clientes');

        // 3. Processar cada linha
        const resultados = [];

        for (let i = 0; i < linhasTeste.length; i++) {
            const linha = linhasTeste[i];

            if (!linha.trim()) continue;

            try {
                console.log(`\n🔄 Processando linha ${i + 2}:`);
                console.log(`   Dados: ${linha.substring(0, 100)}...`);

                const cliente = mapearLinhaParaCliente(linha, rotaId);

                // Exibir dados mapeados
                console.log(`   ✅ Nome: ${cliente.nome}`);
                console.log(`   ✅ CPF: ${cliente.cpfCnpj || 'N/A'}`);
                console.log(`   ✅ Cidade: ${cliente.cidade}`);
                console.log(`   ✅ Débito: R$ ${cliente.debitoAtual.toFixed(2)}`);
                console.log(`   ✅ Ativo: ${cliente.ativo ? 'Sim' : 'Não'}`);

                // Inserir no Firestore
                const docRef = await db.collection('clientes').add(cliente);
                console.log(`   🆔 ID: ${docRef.id}`);

                resultados.push({ sucesso: true, id: docRef.id, nome: cliente.nome });

            } catch (error) {
                console.error(`   ❌ Erro: ${error.message}`);
                resultados.push({ sucesso: false, erro: error.message, linha: i + 2 });
            }
        }

        // 4. Resumo final
        console.log('\n' + '='.repeat(50));
        console.log('📊 RESUMO DO TESTE');
        console.log('='.repeat(50));

        const sucesso = resultados.filter(r => r.sucesso).length;
        const erros = resultados.filter(r => !r.sucesso).length;

        console.log(`✅ Clientes importados: ${sucesso}`);
        console.log(`❌ Erros: ${erros}`);
        console.log(`📈 Taxa de sucesso: ${((sucesso / resultados.length) * 100).toFixed(1)}%`);

        if (sucesso > 0) {
            console.log('\n🎯 Clientes importados:');
            resultados.filter(r => r.sucesso).forEach(r => {
                console.log(`   - ${r.nome} (ID: ${r.id})`);
            });
        }

        if (erros > 0) {
            console.log('\n⚠️ Erros encontrados:');
            resultados.filter(r => !r.sucesso).forEach(r => {
                console.log(`   - Linha ${r.linha}: ${r.erro}`);
            });
        }

        console.log('\n🔍 Para validar:');
        console.log('1. Vá ao Firebase Console: https://console.firebase.google.com/project/gestaobilhares/firestore');
        console.log('2. Verifique as collections:');
        console.log('   - rotas → deve ter "037-Salinas"');
        console.log('   - clientes → deve ter 3 novos documentos');
        console.log('3. Abra o app Android e verifique se os clientes aparecem');

        if (sucesso === resultados.length) {
            console.log('\n🎉 TESTE CONCLUÍDO COM SUCESSO!');
            console.log('✅ Pronto para importar todos os clientes com: npm start');
        } else {
            console.log('\n⚠️ TESTE CONCLUÍDO COM ERROS');
            console.log('🔧 Verifique os erros acima antes de prosseguir');
        }

    } catch (error) {
        console.error('\n❌ ERRO FATAL NO TESTE:', error);
        process.exit(1);
    } finally {
        // Fechar conexão Firebase
        admin.app().delete();
    }
}

// Executar teste
if (require.main === module) {
    main().catch(error => {
        console.error('❌ Erro não tratado:', error);
        process.exit(1);
    });
}

module.exports = { main, mapearLinhaParaCliente };
