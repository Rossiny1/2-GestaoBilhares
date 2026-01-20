/**
 * Importação Automática com Service Account Existente
 * 
 * Usa a chave que você já tem
 * Importa todos os clientes automaticamente
 */

const admin = require('firebase-admin');
const fs = require('fs');
const iconv = require('iconv-lite');

// Configurar Firebase Admin com sua chave
try {
    const serviceAccount = require('./service-account.json');
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        projectId: 'gestaobilhares'
    });
    console.log('✅ Firebase Admin configurado com sua chave!');
} catch (error) {
    console.error('❌ Erro ao configurar Firebase Admin:', error.message);
    process.exit(1);
}

const db = admin.firestore();

/**
 * Converte string de valor monetário para número
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
 * Converte string de data para timestamp
 */
function converterData(dataStr) {
    if (!dataStr) return admin.firestore.FieldValue.serverTimestamp();

    try {
        const [dia, mes, ano] = dataStr.split('/');
        if (dia && mes && ano) {
            const data = new Date(`${ano}-${mes}-${dia}`);
            return admin.firestore.Timestamp.fromDate(data);
        }
        return admin.firestore.FieldValue.serverTimestamp();
    } catch (error) {
        console.warn(`⚠️ Data inválida: ${dataStr}`);
        return admin.firestore.FieldValue.serverTimestamp();
    }
}

/**
 * Normaliza texto mantendo acentos do português brasileiro
 */
function normalizarTexto(texto) {
    if (!texto) return texto;

    return texto
        // Manter caracteres UTF-8 do português
        .replace(/["""''''``]/g, '"') // Normalizar aspas
        .replace(/[–—]/g, '-') // Normalizar travessões
        .replace(/[…]/g, '...') // Normalizar reticências
        .replace(/\r\n/g, '\n') // Normalizar quebras de linha
        .trim();
}

/**
 * Mapeia linha do CSV para documento Firebase
 */
function mapearLinhaParaCliente(linha, rotaId, clienteId) {
    const campos = linha.split(';');

    const nome = normalizarTexto(campos[1] ? campos[1].replace(/"/g, '').trim() : '');
    const cpfCnpj = normalizarTexto(campos[2] ? campos[2].replace(/"/g, '').trim() : '');
    const endereco = normalizarTexto(campos[3] ? campos[3].replace(/"/g, '').trim() : '');
    const cidade = normalizarTexto(campos[4] ? campos[4].replace(/"/g, '').trim() : '');
    const estado = normalizarTexto(campos[5] ? campos[5].replace(/"/g, '').trim() : '');
    const telefone = normalizarTexto(campos[6] ? campos[6].replace(/"/g, '').trim() : '');
    const telefone2 = normalizarTexto(campos[7] ? campos[7].replace(/"/g, '').trim() : '');
    const dataCadastroStr = normalizarTexto(campos[9] ? campos[9].trim() : '');
    const debitoAtualStr = normalizarTexto(campos[11] ? campos[11].trim() : '');
    const observacoes = normalizarTexto(campos[12] ? campos[12].replace(/"/g, '').trim() : '');
    const valorFichaStr = normalizarTexto(campos[13] ? campos[13].trim() : '');

    if (!nome) {
        throw new Error('Nome do cliente é obrigatório');
    }

    const ativo = true;

    return {
        id: parseInt(clienteId), // ID numérico como o app usa
        nome: nome,
        nome_fantasia: null,
        cpf_cnpj: cpfCnpj || null,
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
        precisao_gps: null,
        data_captura_gps: null,
        rota_id: Number(rotaId), // ID numérico da rota
        valor_ficha: converterValorMonetario(valorFichaStr),
        comissao_ficha: 0.0,
        numero_contrato: null,
        debito_anterior: 0.0,
        debito_atual: converterValorMonetario(debitoAtualStr),
        ativo: ativo,
        observacoes: observacoes,
        data_cadastro: Date.now(), // Timestamp numérico como o app usa
        data_ultima_atualizacao: Date.now()
    };
}

/**
 * Gera próximo ID numérico para uma collection (versão simplificada)
 */
async function getNextId(collectionPath) {
    try {
        // Para evitar problemas com índice, vamos usar timestamp + random
        const timestamp = Date.now();
        const random = Math.floor(Math.random() * 1000);
        const nextId = timestamp + random;

        // Tentar usar um ID numérico simples baseado no timestamp
        return Date.now() % 1000000;
    } catch (error) {
        console.warn(`⚠️ Erro ao obter próximo ID: ${error.message}`);
        return Date.now();
    }
}

/**
 * Cria ou obtém rota no Firestore
 */
async function obterOuCriarRota(nomeRota, descricao = '') {
    try {
        const collectionPath = 'empresas/empresa_001/entidades/rotas/items';
        const rotaSnapshot = await db.collection(collectionPath)
            .where('nome', '==', nomeRota)
            .limit(1)
            .get();

        if (rotaSnapshot.empty) {
            // Gerar ID numérico para a rota
            const rotaId = await getNextId(collectionPath);

            // Criar nova rota com ID numérico
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

            await db.collection(collectionPath)
                .doc(String(rotaId))
                .set(novaRota);
            console.log(`🆕 Rota criada: ${nomeRota} (ID: ${rotaId})`);
            return rotaId;
        } else {
            // Rota já existe
            const rotaDoc = rotaSnapshot.docs[0];
            console.log(`✅ Rota encontrada: ${nomeRota} (ID: ${rotaDoc.id})`);
            const rotaId = Number(rotaDoc.id);
            if (Number.isNaN(rotaId)) {
                console.warn(`⚠️ ID da rota não numérico (${rotaDoc.id}) - usando 0`);
                return 0;
            }
            return rotaId;
        }

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
        if (!fs.existsSync(caminhoArquivo)) {
            throw new Error(`Arquivo não encontrado: ${caminhoArquivo}`);
        }

        const rotaId = await obterOuCriarRota(nomeRota, descricaoRota);
        const clientesCollectionPath = 'empresas/empresa_001/entidades/clientes/items';

        // Obter próximo ID para clientes
        let proximoClienteId = await getNextId(clientesCollectionPath);

        // Ler arquivo em Windows-1252 e converter para UTF-8
        const buffer = fs.readFileSync(caminhoArquivo);
        const conteudo = iconv.decode(buffer, 'win1252');
        console.log('📝 Arquivo lido como Windows-1252 e convertido para UTF-8');
        const linhas = conteudo.split('\n');

        console.log(`📊 Encontradas ${linhas.length} linhas no CSV`);
        console.log(`🔢 Iniciando com ID: ${proximoClienteId}`);

        const linhasDados = linhas.slice(1); // Pular primeira linha (cabeçalho)

        // Processar em batch de 10 para melhor performance
        for (let i = 0; i < linhasDados.length; i++) {
            const linha = linhasDados[i];

            if (!linha || Object.keys(linha).length === 0) continue;

            try {
                const cliente = mapearLinhaParaCliente(linha, rotaId, proximoClienteId);
                const clienteId = String(proximoClienteId);

                // Validar que o ID não está vazio
                if (!clienteId || clienteId.trim() === '') {
                    throw new Error('ID do cliente está vazio');
                }

                // Usar ID numérico sequencial como o app
                await db.collection(clientesCollectionPath)
                    .doc(clienteId)
                    .set(cliente);

                resultados.sucesso++;
                proximoClienteId++;

                // Progresso a cada 10 clientes
                if ((i + 1) % 10 === 0) {
                    console.log(`⏳ Progresso: ${i + 1}/${linhasDados.length} clientes processados (ID atual: ${proximoClienteId})`);
                }

            } catch (error) {
                resultados.erros++;
                resultados.detalhesErros.push({
                    linha: i + 2,
                    erro: error.message,
                    dados: linha.substring(0, 50) + '...'
                });

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
        console.log(`   🔢 Último ID usado: ${proximoClienteId - 1}`);

        return resultados;

    } catch (error) {
        console.error(`❌ Erro fatal ao processar ${caminhoArquivo}:`, error);
        throw error;
    }
}

/**
 * Função principal de importação automática
 */
async function main() {
    console.log('🚀 IMPORTAÇÃO AUTOMÁTICA - FIREBASE ADMIN SDK');
    console.log('='.repeat(60));
    console.log(`📦 Projeto: gestaobilhares`);
    console.log(`🔑 Usando sua chave existente`);
    console.log(`⏰ Início: ${new Date().toLocaleString('pt-BR')}`);

    // Mapeamento de arquivos para rotas
    const arquivosParaRotas = [
        {
            arquivo: '../anexos/Cadastro Clientes- Rota Bahia.csv',
            rota: '037-Salinas',
            descricao: 'Rota Salinas - Importação CSV'
        }
        // Adicione os outros arquivos aqui quando tiver:
        // { arquivo: '../anexos/Cadastro Clientes- 033-Montes Claros.csv', rota: '033-Montes Claros', descricao: 'Rota Montes Claros - Importação CSV' },
        // { arquivo: '../anexos/Cadastro Clientes- 08-Chapada Gaucha.csv', rota: '08-Chapada Gaucha', descricao: 'Rota Chapada Gaucha - Importação CSV' },
        // { arquivo: '../anexos/Cadastro Clientes- 035-Coração de Jesus.csv', rota: '035-Coração de Jesus', descricao: 'Rota Coração de Jesus - Importação CSV' },
        // { arquivo: '../anexos/Cadastro Clientes- 034-Bonito de Minas.csv', rota: '034-Bonito de Minas', descricao: 'Rota Bonito de Minas - Importação CSV' },
        // { arquivo: '../anexos/Cadastro Clientes- 03-Januária.csv', rota: '03-Januária', descricao: 'Rota Januária - Importação CSV' },
        // { arquivo: '../anexos/Cadastro Clientes- 036-Bahia.csv', rota: '036-Bahia', descricao: 'Rota Bahia - Importação CSV' }
    ];

    const resultadoGeral = {
        arquivosProcessados: 0,
        totalClientes: 0,
        totalErros: 0,
        tempoInicio: Date.now()
    };

    try {
        for (const config of arquivosParaRotas) {
            try {
                const resultado = await processarArquivoCSV(
                    config.arquivo,
                    config.rota,
                    config.descricao
                );

                resultadoGeral.arquivosProcessados++;
                resultadoGeral.totalClientes += resultado.sucesso;
                resultadoGeral.totalErros += resultado.erros;

            } catch (error) {
                console.error(`❌ Erro ao processar ${config.arquivo}:`, error.message);
                resultadoGeral.totalErros++;
            }
        }

        const tempoTotal = Date.now() - resultadoGeral.tempoInicio;

        console.log('\n' + '='.repeat(60));
        console.log('🎉 IMPORTAÇÃO AUTOMÁTICA CONCLUÍDA!');
        console.log('='.repeat(60));
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

        console.log('\n🎉 IMPORTAÇÃO CONCLUÍDA COM SUCESSO!');

    } catch (error) {
        console.error('\n❌ ERRO FATAL NA IMPORTAÇÃO:', error);
        process.exit(1);
    } finally {
        // Fechar conexão Firebase
        admin.app().delete();
    }
}

// Executar importação automática
if (require.main === module) {
    main().catch(error => {
        console.error('❌ Erro não tratado:', error);
        process.exit(1);
    });
}

module.exports = { main, processarArquivoCSV };
