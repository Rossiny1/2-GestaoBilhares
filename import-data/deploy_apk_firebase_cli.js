/**
 * Deploy do APK via Firebase CLI
 * Usa firebase cli para fazer upload do APK para App Distribution
 */

const { exec } = require('child_process');
const util = require('util');
const execPromise = util.promisify(exec);

async function deployParaFirebaseAppDistribution() {
    console.log('🚀 Iniciando deploy para Firebase App Distribution...');

    try {
        // Caminho do APK release
        const apkPath = '../b/outputs/apk/release/app-release.apk';
        console.log(`📱 APK: ${apkPath}`);

        // App ID do Firebase (precisa ser configurado)
        const appId = '1:1089459035145:android:2d3b94222b1506a844acd8'; // ID real do app Android

        // Comando de deploy
        const command = `firebase appdistribution:distribute ${apkPath} --app "${appId}" --release-notes "Correções V6 Windows - 3 regressões corrigidas, testes 100% passando" --testers "rossinys@gmail.com"`;
        console.log(`📤 Executando: ${command}`);

        const { stdout, stderr } = await execPromise(command);

        console.log('✅ Deploy realizado com sucesso!');
        console.log('📊 Saída:', stdout);

        if (stderr) {
            console.log('⚠️ Avisos:', stderr);
        }

        console.log('\n🎉 APK disponível para testes na Firebase App Distribution!');
        console.log('📱 Testadores já podem baixar a nova versão');

    } catch (error) {
        console.error('❌ Erro no deploy:', error.message);

        // Tentar ajudar com diagnóstico
        if (error.message.includes('undefined')) {
            console.log('\n💡 Dica: O ID do app parece estar undefined.');
            console.log('   Execute: firebase projects:list');
            console.log('   E use o ID correto do projeto');
        }

        if (error.message.includes('HTTP Error: 400')) {
            console.log('\n💡 Dica: Erro de argumento inválido.');
            console.log('   Verifique se o APK existe e se o ID do app está correto');
        }

        process.exit(1);
    }
}

// Executar
deployParaFirebaseAppDistribution();
