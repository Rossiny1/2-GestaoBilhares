/**
 * Importação via Firebase CLI
 * Usa firebase cli para enviar dados diretamente
 */

const fs = require('fs');
const iconv = require('iconv-lite');
const { exec } = require('child_process');
const util = require('util');
const execPromise = util.promisify(exec);

async function importarViaFirebaseCLI() {
    console.log('🚀 Iniciando importação via Firebase CLI...');

    try {
        // Ler CSV com encoding correto
        const filePath = './clientes_rota_bahia.csv';
        console.log(`📁 Lendo arquivo: ${filePath}`);

        const buffer = fs.readFileSync(filePath);
        const content = iconv.decode(buffer, 'win1252');
        const lines = content.split('\n').filter(line => line.trim());

        console.log(`📊 Encontrados ${lines.length} registros`);

        // Processar cada linha e criar documento Firestore
        let sucesso = 0;
        let erros = 0;

        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            const parts = line.split(';');

            if (parts.length < 12) {
                console.log(`⚠️ Linha ${i + 1} formato inválido, pulando...`);
                erros++;
                continue;
            }

            // Extrair dados
            const id = parts[0]?.replace(/"/g, '').trim() || String(i + 1);
            const nome = parts[1]?.replace(/"/g, '').trim() || '';
            const cpf = parts[2]?.replace(/"/g, '').trim() || '';
            const endereco = parts[3]?.replace(/"/g, '').trim() || '';
            const cidade = parts[4]?.replace(/"/g, '').trim() || '';
            const estado = parts[5]?.replace(/"/g, '').trim() || '';
            const telefone1 = parts[6]?.replace(/"/g, '').trim() || '';
            const telefone2 = parts[7]?.replace(/"/g, '').trim() || '';
            const dataCadastro = parts[9]?.replace(/"/g, '').trim() || '';
            const valorStr = parts[11]?.replace(/"/g, '').trim() || '0';
            const observacoes = parts[12]?.replace(/"/g, '').trim() || '';

            // Converter valor monetário
            const valor = parseFloat(valorStr
                .replace('R$', '')
                .replace('.', '')
                .replace(',', '.')
                .trim()) || 0;

            // Criar documento cliente
            const cliente = {
                nome: nome,
                cpf: cpf,
                endereco: endereco,
                cidade: cidade,
                estado: estado,
                telefone1: telefone1,
                telefone2: telefone2,
                dataCadastro: dataCadastro || new Date().toISOString(),
                valorUltimoAcerto: valor,
                observacoes: observacoes,
                ativo: true,
                rota_id: 1, // Rota padrão "Bahia"
            };

            // Gerar JSON temporário
            const tempFile = `./temp_cliente_${i}.json`;
            fs.writeFileSync(tempFile, JSON.stringify(cliente, null, 2));

            try {
                // Usar Firebase CLI para importar
                const command = `firebase firestore:import ${tempFile} --collection clientes`;
                console.log(`📤 Importando cliente ${i + 1}/${lines.length}: ${nome}`);

                await execPromise(command);
                sucesso++;
                console.log(`✅ Cliente ${nome} importado com sucesso`);

                // Remover arquivo temporário
                fs.unlinkSync(tempFile);

            } catch (error) {
                erros++;
                console.error(`❌ Erro ao importar cliente ${nome}:`, error.message);

                // Tentar remover arquivo temporário
                try {
                    fs.unlinkSync(tempFile);
                } catch (e) { }
            }

            // Pequena pausa para não sobrecarregar
            await new Promise(resolve => setTimeout(resolve, 100));
        }

        console.log('\n🎉 IMPORTAÇÃO CONCLUÍDA!');
        console.log(`📊 Resumo:`);
        console.log(`   ✅ Sucessos: ${sucesso}`);
        console.log(`   ❌ Erros: ${erros}`);
        console.log(`   📊 Total: ${lines.length}`);
        console.log(`   📈 Taxa de sucesso: ${((sucesso / lines.length) * 100).toFixed(1)}%`);

    } catch (error) {
        console.error('❌ Erro fatal:', error.message);
    }
}

// Executar
importarViaFirebaseCLI();
