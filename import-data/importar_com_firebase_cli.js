/**
 * Importação usando Firebase CLI (sem Service Account)
 * 
 * Usa o login existente no Firebase CLI
 * Cria script para importar via gcloud
 */

const { execSync } = require('child_process');
const fs = require('fs');

/**
 * Executa comando e retorna resultado
 */
function execCommand(command) {
    try {
        console.log(`🔧 Executando: ${command}`);
        const result = execSync(command, {
            encoding: 'utf8',
            cwd: '..',
            stdio: ['pipe', 'pipe', 'pipe']
        });
        return { success: true, output: result.trim() };
    } catch (error) {
        return {
            success: false,
            error: error.stderr || error.stdout || error.message
        };
    }
}

/**
 * Converte linha CSV para JSON
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
 * Função principal
 */
function main() {
    console.log('🚀 IMPORTAÇÃO VIA FIREBASE CLI');
    console.log('='.repeat(50));

    try {
        // 1. Verificar configuração
        console.log('📋 Verificando configuração...');

        const versionResult = execCommand('firebase --version');
        if (!versionResult.success) {
            throw new Error('Firebase CLI não encontrado');
        }
        console.log(`✅ Firebase CLI: ${versionResult.output}`);

        const loginResult = execCommand('firebase login:list');
        if (!loginResult.success || !loginResult.output.includes('rossinys@gmail.com')) {
            throw new Error('Não está logado como rossinys@gmail.com');
        }
        console.log('✅ Login OK');

        // 2. Ler CSV e converter para JSON
        console.log('\n📁 Processando CSV...');

        const caminhoArquivo = '../anexos/Cadastro Clientes- Rota Bahia.csv';

        if (!fs.existsSync(caminhoArquivo)) {
            throw new Error(`Arquivo não encontrado: ${caminhoArquivo}`);
        }

        const conteudo = fs.readFileSync(caminhoArquivo, 'utf8');
        const linhas = conteudo.split('\n');

        console.log(`📊 Encontradas ${linhas.length} linhas no CSV`);

        // 3. Criar estrutura JSON para importação
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
            clientes: []
        };

        // Processar clientes (pular cabeçalho)
        const linhasDados = linhas.slice(1, 4); // Pegar 3 primeiros clientes

        for (let i = 0; i < linhasDados.length; i++) {
            const linha = linhasDados[i];

            if (!linha.trim()) continue;

            try {
                const cliente = converterLinhaParaJSON(linha, '037-Salinas');
                dadosImportacao.clientes.push(cliente);

                console.log(`✅ Cliente ${i + 1}: ${cliente.nome}`);

            } catch (error) {
                console.error(`❌ Erro linha ${i + 2}: ${error.message}`);
            }
        }

        // 4. Salvar arquivo JSON
        const arquivoSaida = '../dados_importacao_completa.json';
        fs.writeFileSync(arquivoSaida, JSON.stringify(dadosImportacao, null, 2));

        console.log(`\n✅ Arquivo JSON criado: ${arquivoSaida}`);
        console.log(`📊 Estrutura:`);
        console.log(`   📁 Rotas: ${dadosImportacao.rotas.length}`);
        console.log(`   👥 Clientes: ${dadosImportacao.clientes.length}`);

        // 5. Gerar script de importação
        const scriptImportacao = `# Script de Importação para Firebase
# Execute este script no PowerShell ou CMD

# 1. Selecionar projeto
firebase use gestaobilhares

# 2. Importar dados
# NOTA: Firebase CLI não tem comando direto de importação
# Use o Firebase Console para importar o arquivo JSON

echo "📁 Arquivo para importar: ${arquivoSaida}"
echo "🌐 Abra: https://console.firebase.google.com/project/gestaobilhares/firestore"
echo "📥 Importe o arquivo manualmente"
`;

        fs.writeFileSync('../importar_dados.ps1', scriptImportacao);

        console.log('\n' + '='.repeat(50));
        console.log('🎉 PREPARAÇÃO CONCLUÍDA!');
        console.log('='.repeat(50));
        console.log('📋 Arquivos gerados:');
        console.log(`   📄 JSON: ${arquivoSaida}`);
        console.log('   🔧 Script: importar_dados.ps1');

        console.log('\n🚀 Próximos passos:');
        console.log('1. Abra o Firebase Console');
        console.log('2. Importe o arquivo JSON manualmente');
        console.log('3. Valide no app Android');

        console.log('\n🎯 Vantagens desta abordagem:');
        console.log('✅ Usa login existente');
        console.log('✅ Não precisa de Service Account');
        console.log('✅ Arquivo JSON pronto para importar');
        console.log('✅ Pode importar manualmente em 2 minutos');

    } catch (error) {
        console.error('\n❌ ERRO FATAL:', error.message);
        process.exit(1);
    }
}

// Executar
if (require.main === module) {
    main();
}

module.exports = { main };
