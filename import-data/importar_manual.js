/**
 * Importação Manual via Firebase Console
 * 
 * Gera instruções detalhadas para importação manual
 */

const fs = require('fs');

function main() {
    console.log('📋 INSTRUÇÕES PARA IMPORTAÇÃO MANUAL');
    console.log('='.repeat(60));

    // Verificar se o arquivo JSON existe
    if (!fs.existsSync('../dados_teste_3_clientes.json')) {
        console.log('❌ Arquivo dados_teste_3_clientes.json não encontrado!');
        console.log('Execute primeiro: node teste_simples.js');
        return;
    }

    console.log('✅ Arquivo JSON encontrado: dados_teste_3_clientes.json');

    // Ler e mostrar resumo dos dados
    const dados = JSON.parse(fs.readFileSync('../dados_teste_3_clientes.json', 'utf8'));

    console.log('\n📊 RESUMO DOS DADOS:');
    console.log(`📁 Rotas: ${dados.rotas ? dados.rotas.length : 0}`);
    console.log(`👥 Clientes: ${dados.clientes ? dados.clientes.length : 0}`);

    if (dados.rotas && dados.rotas.length > 0) {
        console.log('\n🎯 ROTA A SER CRIADA:');
        const rota = dados.rotas[0];
        console.log(`   Nome: ${rota.nome}`);
        console.log(`   Descrição: ${rota.descricao}`);
        console.log(`   Ativa: ${rota.ativa ? 'Sim' : 'Não'}`);
    }

    if (dados.clientes && dados.clientes.length > 0) {
        console.log('\n👥 CLIENTES A SEREM IMPORTADOS:');
        dados.clientes.forEach((cliente, index) => {
            console.log(`   ${index + 1}. ${cliente.nome}`);
            console.log(`      CPF: ${cliente.cpfCnpj || 'N/A'}`);
            console.log(`      Cidade: ${cliente.cidade}`);
            console.log(`      Débito: R$ ${cliente.debitoAtual.toFixed(2)}`);
            console.log(`      Ativo: ${cliente.ativo ? 'Sim' : 'Não'}`);
            console.log('');
        });
    }

    console.log('🚀 PASSOS PARA IMPORTAÇÃO:');
    console.log('');
    console.log('1️⃣ ABRIR FIREBASE CONSOLE:');
    console.log('   📎 Link: https://console.firebase.google.com/project/gestaobilhares/firestore');
    console.log('   👤 Login: rossinys@gmail.com');
    console.log('');

    console.log('2️⃣ IMPORTAR DADOS:');
    console.log('   📁 Clique em "Importar documento" (botão no topo)');
    console.log('   📄 Selecione o arquivo: dados_teste_3_clientes.json');
    console.log('   ✅ Mantenha as opções padrão');
    console.log('   🚀 Clique em "Importar"');
    console.log('');

    console.log('3️⃣ VERIFICAR RESULTADO:');
    console.log('   👀 No Firebase Console, você deve ver:');
    console.log('      📁 Collection: rotas (1 documento)');
    console.log('      📁 Collection: clientes (3 documentos)');
    console.log('');

    console.log('4️⃣ VALIDAR NO APP:');
    console.log('   📱 Abra o app Android Gestão Bilhares');
    console.log('   🗺️ Vá para a tela de "Rotas"');
    console.log('   🔍 Procure por "037-Salinas"');
    console.log('   👥 Clique na rota para ver os 3 clientes');
    console.log('');

    console.log('📋 ESTRUTURA ESPERADA:');
    console.log('```');
    console.log('Firestore Database');
    console.log('├── rotas/');
    console.log('│   └── 037-Salinas (documento)');
    console.log('└── clientes/');
    console.log('    ├── Angela Ramos Cruz (documento)');
    console.log('    ├── Mauro Luiz Batista (documento)');
    console.log('    └── Sinvaldo Ribeiro da Silva (documento)');
    console.log('```');
    console.log('');

    console.log('⚠️ OBSERVAÇÕES IMPORTANTES:');
    console.log('• O arquivo JSON já está no formato correto para Firestore');
    console.log('• Não é necessário fazer nenhuma conversão');
    console.log('• A importação criará automaticamente as collections');
    console.log('• Os IDs dos documentos serão gerados automaticamente');
    console.log('');

    console.log('🔍 SE DER ERRO:');
    console.log('• Verifique se está logado corretamente');
    console.log('• Confirme se o projeto é "gestaobilhares"');
    console.log('• Tente recarregar a página do Firebase Console');
    console.log('• Verifique se o arquivo JSON não está corrompido');
    console.log('');

    console.log('📞 SUPORTE:');
    console.log('• Documentação completa: .cursor/rules/IMPORTACAO_DADOS_CSV.md');
    console.log('• Script de teste: import-data/teste_simples.js');
    console.log('• Firebase Console: https://console.firebase.google.com/project/gestaobilhares');
    console.log('');

    console.log('🎯 PRÓXIMOS PASSOS:');
    console.log('1. ✅ Importe os 3 clientes de teste');
    console.log('2. ✅ Valide no app Android');
    console.log('3. ✅ Se funcionar, adicione os outros 7 arquivos CSV');
    console.log('4. ✅ Execute a importação completa');
    console.log('');

    console.log('🎉 ESTÁ PRONTO PARA IMPORTAR!');
    console.log('Siga os passos acima para enviar os dados ao Firebase.');
}

// Executar
if (require.main === module) {
    main();
}

module.exports = { main };
