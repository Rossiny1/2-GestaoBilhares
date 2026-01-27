/**
 * Gerar JSON Local para Importação
 * Cria arquivo JSON com dados formatados para importação manual
 */

const fs = require('fs');
const iconv = require('iconv-lite');

function gerarJSONLocal() {
    console.log('🚀 Gerando JSON local para importação...');

    try {
        // Ler CSV com encoding correto
        const filePath = './clientes_rota_bahia.csv';
        console.log(`📁 Lendo arquivo: ${filePath}`);

        const buffer = fs.readFileSync(filePath);
        const content = iconv.decode(buffer, 'win1252');
        const lines = content.split('\n').filter(line => line.trim());

        console.log(`📊 Processando ${lines.length} registros...`);

        const clientes = [];
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
                id: Number(id) || (i + 1),
                nome: nome,
                cpf: cpf,
                endereco: endereco,
                cidade: cidade,
                estado: estado,
                telefone1: telefone1,
                telefone2: telefone2,
                dataCadastro: dataCadastro || new Date().toISOString().split('T')[0],
                valorUltimoAcerto: valor,
                observacoes: observacoes,
                ativo: true,
                rota_id: 1, // Rota padrão "Bahia"
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString()
            };

            clientes.push(cliente);
            sucesso++;

            console.log(`✅ Processado ${i + 1}/${lines.length}: ${nome}`);
        }

        // Gerar arquivo JSON
        const outputFile = './clientes_bahia_import.json';
        fs.writeFileSync(outputFile, JSON.stringify(clientes, null, 2));

        console.log('\n🎉 JSON GERADO COM SUCESSO!');
        console.log(`📊 Resumo:`);
        console.log(`   ✅ Processados: ${sucesso}`);
        console.log(`   ❌ Erros: ${erros}`);
        console.log(`   📊 Total: ${lines.length}`);
        console.log(`   📁 Arquivo: ${outputFile}`);
        console.log(`   📏 Tamanho: ${(fs.statSync(outputFile).size / 1024).toFixed(2)} KB`);

        // Gerar script de importação Firebase CLI
        const importScript = `
# Script para importar via Firebase CLI
# Execute linha por linha ou em batch pequeno

firebase firestore:delete clientes --confirm

# Importar clientes (em batch de 10)
`;

        for (let i = 0; i < clientes.length; i += 10) {
            const batch = clientes.slice(i, i + 10);
            importScript += `\n# Batch ${Math.floor(i / 10) + 1}\n`;
            batch.forEach(cliente => {
                importScript += `firebase firestore:create clientes/${cliente.id} --data '${JSON.stringify(cliente).replace(/'/g, "\\'")}'\n`;
            });
        }

        fs.writeFileSync('./importar_via_cli.sh', importScript);
        console.log(`   📜 Script CLI: ./importar_via_cli.sh`);

    } catch (error) {
        console.error('❌ Erro fatal:', error.message);
    }
}

// Executar
gerarJSONLocal();
