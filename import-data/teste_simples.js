/**
 * Teste Simples - Ler CSV e Gerar JSON
 * 
 * Não usa Firebase CLI ou Admin SDK
 * Apenas processa os dados e gera JSON para importação manual
 */

const fs = require('fs');

/**
 * Converte linha CSV para objeto JSON simples
 */
function converterLinhaParaJSON(linha, rotaId) {
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
        nome: nome,
        nomeFantasia: null,
        cpfCnpj: cpfCnpj,
        telefone: telefone,
        telefone2: telefone2,
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
        dataUltimaAtualizacao: new Date().toISOString()
    };
}

/**
 * Função principal de teste
 */
function main() {
    console.log('🧪 TESTE SIMPLES - CSV para JSON');
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

        console.log(`📊 Total de linhas: ${linhas.length}`);

        // Pegar as 3 primeiras linhas de dados (pular cabeçalho)
        const linhasTeste = linhas.slice(1, 4); // Linhas 2, 3, 4

        console.log(`📊 Selecionadas ${linhasTeste.length} linhas para teste`);

        // 2. Processar cada linha
        const clientes = [];
        const rotaId = '037-Salinas';

        for (let i = 0; i < linhasTeste.length; i++) {
            const linha = linhasTeste[i];

            if (!linha.trim()) continue;

            try {
                console.log(`\n🔄 Processando linha ${i + 2}:`);
                console.log(`   Dados: ${linha.substring(0, 100)}...`);

                const cliente = converterLinhaParaJSON(linha, rotaId);

                // Exibir dados mapeados
                console.log(`   ✅ Nome: ${cliente.nome}`);
                console.log(`   ✅ CPF: ${cliente.cpfCnpj || 'N/A'}`);
                console.log(`   ✅ Cidade: ${cliente.cidade}`);
                console.log(`   ✅ Débito: R$ ${cliente.debitoAtual.toFixed(2)}`);
                console.log(`   ✅ Ativo: ${cliente.ativo ? 'Sim' : 'Não'}`);

                clientes.push(cliente);

            } catch (error) {
                console.error(`   ❌ Erro: ${error.message}`);
            }
        }

        // 3. Criar estrutura completa para importação
        const dadosImportacao = {
            rotas: [
                {
                    nome: '037-Salinas',
                    descricao: 'Rota de teste - 3 clientes',
                    colaboradorResponsavel: 'Sistema',
                    cidades: 'Importação CSV',
                    ativa: true,
                    cor: '#6200EA',
                    dataCriacao: new Date().toISOString(),
                    dataAtualizacao: new Date().toISOString(),
                    statusAtual: 'PAUSADA',
                    cicloAcertoAtual: 1,
                    anoCiclo: new Date().getFullYear()
                }
            ],
            clientes: clientes
        };

        // 4. Salvar arquivo JSON
        const arquivoSaida = '../dados_teste_3_clientes.json';
        fs.writeFileSync(arquivoSaida, JSON.stringify(dadosImportacao, null, 2));

        console.log('\n' + '='.repeat(50));
        console.log('📊 RESUMO DO TESTE');
        console.log('='.repeat(50));

        console.log(`✅ Clientes processados: ${clientes.length}`);
        console.log(`✅ Rotas criadas: 1`);
        console.log(`📁 Arquivo gerado: ${arquivoSaida}`);

        console.log('\n📋 Estrutura do JSON:');
        console.log('```json');
        console.log('{');
        console.log('  "rotas": [');
        console.log('    { "nome": "037-Salinas", ... }');
        console.log('  ],');
        console.log('  "clientes": [');
        console.log('    { "nome": "Cliente 1", ... },');
        console.log('    { "nome": "Cliente 2", ... },');
        console.log('    { "nome": "Cliente 3", ... }');
        console.log('  ]');
        console.log('}');
        console.log('```');

        console.log('\n🔍 Para importar no Firebase:');
        console.log('1. Abra: https://console.firebase.google.com/project/gestaobilhares/firestore');
        console.log('2. Clique em "Importar documento"');
        console.log('3. Selecione o arquivo: dados_teste_3_clientes.json');
        console.log('4. Confirme a importação');

        console.log('\n🎯 Clientes gerados:');
        clientes.forEach((cliente, index) => {
            console.log(`   ${index + 1}. ${cliente.nome} - ${cliente.cidade}`);
        });

        console.log('\n🎉 TESTE CONCLUÍDO COM SUCESSO!');
        console.log('✅ Arquivo JSON pronto para importação manual');

    } catch (error) {
        console.error('\n❌ ERRO FATAL NO TESTE:', error.message);
        process.exit(1);
    }
}

// Executar teste
if (require.main === module) {
    main();
}

module.exports = { main, converterLinhaParaJSON };
