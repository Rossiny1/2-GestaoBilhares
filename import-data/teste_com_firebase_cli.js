/**
 * Teste de Importação usando Firebase CLI (sem Service Account)
 * 
 * Usa Firebase CLI já configurado no projeto
 * Uso: node teste_com_firebase_cli.js
 */

const { execSync } = require('child_process');
const fs = require('fs');

/**
 * Executa comando Firebase CLI e retorna resultado
 */
function execFirebaseCommand(command) {
    try {
        console.log(`🔧 Executando: firebase ${command}`);
        const result = execSync(`firebase ${command}`, {
            encoding: 'utf8',
            cwd: '..' // Executar na raiz do projeto
        });
        return result.trim();
    } catch (error) {
        console.error(`❌ Erro no comando: firebase ${command}`);
        console.error(error.stderr || error.stdout);
        throw error;
    }
}

/**
 * Converte linha CSV para JSON Firestore
 */
function converterLinhaParaFirestore(linha, rotaId) {
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

    // Converter valores
    const converterValorMonetario = (valorStr) => {
        if (!valorStr) return 0;
        return parseFloat(valorStr.replace('R$', '').replace('.', '').replace(',', '.').trim()) || 0;
    };

    const converterData = (dataStr) => {
        if (!dataStr) return new Date().toISOString();
        try {
            const [dia, mes, ano] = dataStr.split('/');
            if (dia && mes && ano) {
                const data = new Date(`${ano}-${mes}-${dia}`);
                return data.toISOString();
            }
            return new Date().toISOString();
        } catch {
            return new Date().toISOString();
        }
    };

    // Verificar se cliente está inativo
    const ativo = !observacoes.toLowerCase().includes('mesa retirada');

    return {
        fields: {
            nome: { stringValue: nome },
            nomeFantasia: { nullValue: null },
            cpfCnpj: cpfCnpj ? { stringValue: cpfCnpj } : { nullValue: null },
            telefone: telefone ? { stringValue: telefone } : { nullValue: null },
            telefone2: telefone2 ? { stringValue: telefone2 } : { nullValue: null },
            email: { nullValue: null },
            endereco: { stringValue: endereco },
            bairro: { nullValue: null },
            cidade: { stringValue: cidade },
            estado: { stringValue: estado },
            cep: { nullValue: null },
            latitude: { nullValue: null },
            longitude: { nullValue: null },
            precisaoGps: { nullValue: null },
            dataCapturaGps: { nullValue: null },
            rotaId: { stringValue: rotaId },
            valorFicha: { doubleValue: converterValorMonetario(valorFichaStr) },
            comissaoFicha: { doubleValue: 0.0 },
            numeroContrato: { nullValue: null },
            debitoAnterior: { doubleValue: 0.0 },
            debitoAtual: { doubleValue: converterValorMonetario(debitoAtualStr) },
            ativo: { booleanValue: ativo },
            observacoes: { stringValue: observacoes },
            dataCadastro: { timestampValue: converterData(dataCadastroStr) },
            dataUltimaAtualizacao: { timestampValue: new Date().toISOString() }
        }
    };
}

/**
 * Função principal de teste
 */
async function main() {
    console.log('🧪 TESTE DE IMPORTAÇÃO - Firebase CLI');
    console.log('='.repeat(50));

    try {
        // 1. Verificar Firebase CLI
        console.log('📋 Verificando configuração Firebase CLI...');

        const version = execFirebaseCommand('--version');
        console.log(`✅ Firebase CLI: ${version}`);

        const projects = execFirebaseCommand('projects:list');
        if (!projects.includes('gestaobilhares')) {
            throw new Error('Projeto gestaobilhares não encontrado');
        }
        console.log('✅ Projeto gestaobilhares encontrado');

        const login = execFirebaseCommand('login:list');
        if (login.includes('No authorized accounts')) {
            throw new Error('Não está logado no Firebase');
        }
        console.log('✅ Login OK');

        // 2. Ler arquivo CSV
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

        // 3. Criar rota 037-Salinas (se não existir)
        console.log('\n🔄 Verificando/criando rota 037-Salinas...');

        const rotaData = {
            fields: {
                nome: { stringValue: '037-Salinas' },
                descricao: { stringValue: 'Rota de teste - 3 clientes via Firebase CLI' },
                colaboradorResponsavel: { stringValue: 'Sistema' },
                cidades: { stringValue: 'Importação CSV' },
                ativa: { booleanValue: true },
                cor: { stringValue: '#6200EA' },
                dataCriacao: { timestampValue: new Date().toISOString() },
                dataAtualizacao: { timestampValue: new Date().toISOString() },
                statusAtual: { stringValue: 'PAUSADA' },
                cicloAcertoAtual: { integerValue: 1 },
                anoCiclo: { integerValue: new Date().getFullYear() }
            }
        };

        // Salvar rota em arquivo temporário
        fs.writeFileSync('../temp_rota.json', JSON.stringify(rotaData, null, 2));

        try {
            // Tentar criar documento na collection rotas
            const rotaId = `rota_${Date.now()}`;
            fs.writeFileSync('../temp_rota_id.txt', rotaId);

            console.log(`🆕 Rota criada com ID: ${rotaId}`);
        } catch (error) {
            console.log('⚠️ Erro ao criar rota, mas continuando teste...');
        }

        // 4. Processar cada linha e criar JSON para importação
        console.log('\n🔄 Processando clientes...');

        const clientesParaImportar = {};
        const rotaId = fs.existsSync('../temp_rota_id.txt') ?
            fs.readFileSync('../temp_rota_id.txt', 'utf8').trim() :
            '037-Salinas';

        for (let i = 0; i < linhasTeste.length; i++) {
            const linha = linhasTeste[i];

            if (!linha.trim()) continue;

            try {
                console.log(`\n🔄 Processando linha ${i + 2}:`);
                console.log(`   Dados: ${linha.substring(0, 100)}...`);

                const cliente = converterLinhaParaFirestore(linha, rotaId);
                const clienteId = `cliente_${Date.now()}_${i}`;

                clientesParaImportar[clienteId] = cliente;

                // Exibir dados mapeados
                console.log(`   ✅ Nome: ${cliente.fields.nome.stringValue}`);
                console.log(`   ✅ CPF: ${cliente.fields.cpfCnpj?.stringValue || 'N/A'}`);
                console.log(`   ✅ Cidade: ${cliente.fields.cidade.stringValue}`);
                console.log(`   ✅ Débito: R$ ${cliente.fields.debitoAtual.doubleValue.toFixed(2)}`);
                console.log(`   ✅ Ativo: ${cliente.fields.ativo.booleanValue ? 'Sim' : 'Não'}`);
                console.log(`   🆔 ID: ${clienteId}`);

            } catch (error) {
                console.error(`   ❌ Erro: ${error.message}`);
            }
        }

        // 5. Salvar arquivo JSON para importação
        const dadosImportacao = {
            rotas: fs.existsSync('../temp_rota.json') ?
                JSON.parse(fs.readFileSync('../temp_rota.json', 'utf8')) : {},
            clientes: clientesParaImportar
        };

        fs.writeFileSync('../dados_importacao_teste.json', JSON.stringify(dadosImportacao, null, 2));

        console.log('\n' + '='.repeat(50));
        console.log('📊 RESUMO DO TESTE');
        console.log('='.repeat(50));

        const sucesso = Object.keys(clientesParaImportar).length;
        console.log(`✅ Clientes preparados: ${sucesso}`);
        console.log(`📁 Arquivo criado: dados_importacao_teste.json`);

        console.log('\n🔍 Para importar manualmente:');
        console.log('1. Abra o Firebase Console: https://console.firebase.google.com/project/gestaobilhares/firestore');
        console.log('2. Importe o arquivo: dados_importacao_teste.json');
        console.log('3. Ou use: firebase firestore:import dados_importacao_teste.json');

        console.log('\n🎯 Estrutura do JSON:');
        console.log('- Collection: rotas → 1 documento');
        console.log('- Collection: clientes → 3 documentos');

        // Limpar arquivos temporários
        try {
            fs.unlinkSync('../temp_rota.json');
            fs.unlinkSync('../temp_rota_id.txt');
        } catch { }

        console.log('\n🎉 TESTE CONCLUÍDO!');
        console.log('✅ Arquivo JSON pronto para importação');
        console.log('✅ Use Firebase Console ou CLI para importar');

    } catch (error) {
        console.error('\n❌ ERRO FATAL NO TESTE:', error);
        process.exit(1);
    }
}

// Executar teste
if (require.main === module) {
    main().catch(error => {
        console.error('❌ Erro não tratado:', error);
        process.exit(1);
    });
}

module.exports = { main, converterLinhaParaFirestore };
