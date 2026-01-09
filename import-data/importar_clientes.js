/**
 * Importador de Clientes para Firebase - Script Externo
 * 
 * Uso: node importar_clientes.js
 * Requisitos: npm install firebase-admin csv-parser
 * 
 * Best Practice Firebase: Firebase Admin SDK para importações bulk
 */

const admin = require('firebase-admin');
const fs = require('fs');
const csv = require('csv-parser');

// Configurar Firebase Admin
// Você precisa ter o arquivo service-account.json
try {
    const serviceAccount = require('./service-account.json');
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        projectId: 'gestaobilhares'
    });
} catch (error) {
    console.error('❌ Erro ao configurar Firebase Admin:');
    console.error('1. Faça download do service account: https://console.firebase.google.com/project/gestaobilhares/settings/serviceaccounts/adminsdk');
    console.error('2. Salve como service-account.json na mesma pasta');
    console.error('3. Execute: npm install firebase-admin csv-parser');
    process.exit(1);
}

const db = admin.firestore();

// Mapeamento de arquivos para rotas
const arquivosParaRotas = [
    { 
        arquivo: 'anexos/Cadastro Clientes- Rota Bahia.csv', 
        rota: '037-Salinas',
        descricao: 'Rota Salinas - Importação CSV'
    },
    // Adicione os outros arquivos aqui quando tiver:
    // { arquivo: 'anexos/Cadastro Clientes- 033-Montes Claros.csv', rota: '033-Montes Claros' },
    // { arquivo: 'anexos/Cadastro Clientes- 08-Chapada Gaucha.csv', rota: '08-Chapada Gaucha' },
    // { arquivo: 'anexos/Cadastro Clientes- 035-Coração de Jesus.csv', rota: '035-Coração de Jesus' },
    // { arquivo: 'anexos/Cadastro Clientes- 034-Bonito de Minas.csv', rota: '034-Bonito de Minas' },
    // { arquivo: 'anexos/Cadastro Clientes- 03-Januária.csv', rota: '03-Januária' },
    // { arquivo: 'anexos/Cadastro Clientes- 036-Bahia.csv', rota: '036-Bahia' }
];

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
    const nome = linha[1] ? linha[1].replace(/"/g, '').trim() : '';
    const cpfCnpj = linha[2] ? linha[2].replace(/"/g, '').trim() : null;
    const endereco = linha[3] ? linha[3].replace(/"/g, '').trim() : '';
    const cidade = linha[4] ? linha[4].replace(/"/g, '').trim() : '';
    const estado = linha[5] ? linha[5].replace(/"/g, '').trim() : '';
    const telefone = linha[6] ? linha[6].replace(/"/g, '').trim() : null;
    const telefone2 = linha[7] ? linha[7].replace(/"/g, '').trim() : null;
    const dataCadastroStr = linha[9] ? linha[9].trim() : '';
    const debitoAtualStr = linha[11] ? linha[11].trim() : '';
    const observacoes = linha[12] ? linha[12].replace(/"/g, '').trim() : '';
    const valorFichaStr = linha[13] ? linha[13].trim() : '';
    
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
 * Processa um arquivo CSV completo
 */
async function processarArquivoCSV(caminhoArquivo, nomeRota, descricaoRota) {
    console.log(`\n📁 Processando arquivo: ${caminhoArquivo}`);
    console.log(`🎯 Rota destino: ${nomeRota}`);
    
    const resultados = {
        sucesso: 0,
        erros: 0,
        detalhesErros: [],
        tempoInicio: Date.now()
    };
    
    try {
        // Verificar se arquivo existe
        if (!fs.existsSync(caminhoArquivo)) {
            throw new Error(`Arquivo não encontrado: ${caminhoArquivo}`);
        }
        
        // Obter ou criar rota
        const rotaId = await obterOuCriarRota(nomeRota, descricaoRota);
        
        // Processar CSV
        const linhas = [];
        
        await new Promise((resolve, reject) => {
            fs.createReadStream(caminhoArquivo)
                .pipe(csv({ separator: ';' }))
                .on('data', (linha) => {
                    linhas.push(linha);
                })
                .on('end', resolve)
                .on('error', reject);
        });
        
        console.log(`📊 Encontradas ${linhas.length} linhas no CSV`);
        
        // Processar cada linha (pular cabeçalho se necessário)
        const linhasDados = linhas.slice(1); // Pular primeira linha (cabeçalho)
        
        for (let i = 0; i < linhasDados.length; i++) {
            const linha = linhasDados[i];
            
            // Pular linhas vazias
            if (!linha || Object.keys(linha).length === 0) continue;
            
            try {
                const cliente = mapearLinhaParaCliente(linha, rotaId);
                await db.collection('clientes').add(cliente);
                resultados.sucesso++;
                
                // Progresso a cada 10 clientes
                if ((i + 1) % 10 === 0) {
                    console.log(`⏳ Progresso: ${i + 1}/${linhasDados.length} clientes processados`);
                }
                
            } catch (error) {
                resultados.erros++;
                resultados.detalhesErros.push({
                    linha: i + 2,
                    erro: error.message,
                    dados: linha
                });
                
                // Limitar detalhes de erro para não poluir output
                if (resultados.detalhesErros.length <= 5) {
                    console.warn(`⚠️ Erro linha ${i + 2}: ${error.message}`);
                }
            }
        }
        
        const tempoTotal = Date.now() - resultados.tempoInicio;
        
        console.log(`\n✅ Importação concluída!`);
        console.log(`📊 Resultados:`);
        console.log(`   👥 Clientes importados: ${resultados.sucesso}`);
        console.log(`   ❌ Erros: ${resultados.erros}`);
        console.log(`   ⏱️  Tempo total: ${(tempoTotal / 1000).toFixed(2)}s`);
        console.log(`   🚀 Média: ${(tempoTotal / resultados.sucesso).toFixed(0)}ms/cliente`);
        
        if (resultados.detalhesErros.length > 5) {
            console.log(`   ⚠️  Mais ${resultados.detalhesErros.length - 5} erros não mostrados`);
        }
        
        return resultados;
        
    } catch (error) {
        console.error(`❌ Erro fatal ao processar ${caminhoArquivo}:`, error);
        throw error;
    }
}

/**
 * Função principal de importação
 */
async function main() {
    console.log('🚀 Iniciando Importação de Clientes - Firebase Admin SDK');
    console.log('=' .repeat(60));
    console.log(`📦 Projeto: gestaobilhares`);
    console.log(`📁 Arquivos a processar: ${arquivosParaRotas.length}`);
    console.log(`⏰ Início: ${new Date().toLocaleString('pt-BR')}`);
    
    const resultadoGeral = {
        arquivosProcessados: 0,
        totalClientes: 0,
        totalErros: 0,
        tempoInicio: Date.now()
    };
    
    try {
        for (const config of arquivosParaRotas) {
            const resultado = await processarArquivoCSV(
                config.arquivo, 
                config.rota, 
                config.descricao
            );
            
            resultadoGeral.arquivosProcessados++;
            resultadoGeral.totalClientes += resultado.sucesso;
            resultadoGeral.totalErros += resultado.erros;
        }
        
        const tempoTotal = Date.now() - resultadoGeral.tempoInicio;
        
        console.log('\n' + '='.repeat(60));
        console.log('🎉 IMPORTAÇÃO CONCLUÍDA COM SUCESSO!');
        console.log('=' .repeat(60));
        console.log(`📊 Resumo Final:`);
        console.log(`   📁 Arquivos processados: ${resultadoGeral.arquivosProcessados}`);
        console.log(`   👥 Total clientes: ${resultadoGeral.totalClientes}`);
        console.log(`   ❌ Total erros: ${resultadoGeral.totalErros}`);
        console.log(`   ⏱️  Tempo total: ${(tempoTotal / 1000).toFixed(2)}s`);
        console.log(`   🚀 Performance: ${(tempoTotal / resultadoGeral.totalClientes).toFixed(0)}ms/cliente`);
        console.log(`   ✅ Taxa de sucesso: ${((resultadoGeral.totalClientes / (resultadoGeral.totalClientes + resultadoGeral.totalErros)) * 100).toFixed(1)}%`);
        
        console.log('\n🎯 Próximos passos:');
        console.log('1. Abra o app Android');
        console.log('2. Vá em "Rotas" para verificar as novas rotas');
        console.log('3. Clique em uma rota para ver os clientes importados');
        console.log('4. Verifique se os dados estão corretos');
        
    } catch (error) {
        console.error('\n❌ ERRO FATAL NA IMPORTAÇÃO:', error);
        process.exit(1);
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

module.exports = { main, processarArquivoCSV, mapearLinhaParaCliente };
