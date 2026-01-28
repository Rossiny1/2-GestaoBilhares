// functions/backup-firestore.js
/**
 * 🔥 BACKUP AUTOMÁTICO DO FIRESTORE
 * 
 * Funções Cloud para backup diário e limpeza automática
 * Executa todos os dias às 3h AM (America/Sao_Paulo)
 * Mantém últimos 30 dias de backups
 */

const functions = require('firebase-functions');
const firestore = require('@google-cloud/firestore');
const storage = require('@google-cloud/storage');

/**
 * Backup diário do Firestore
 * Agenda: 0 3 * * * (3h AM diariamente)
 * TimeZone: America/Sao_Paulo
 */
exports.backupFirestore = functions.pubsub
  .schedule('0 3 * * *') // 3h AM diariamente
  .timeZone('America/Sao_Paulo')
  .onRun(async (context) => {
    console.log('🔥 INICIANDO BACKUP DIÁRIO DO FIRESTORE');
    
    try {
      const client = new firestore.v1.FirestoreAdminClient();
      const projectId = 'gestaobilhares';
      const databaseName = client.databasePath(projectId, '(default)');

      // Gerar timestamp para nome do backup
      const timestamp = new Date().toISOString().split('T')[0]; // YYYY-MM-DD
      const bucket = `gs://gestaobilhares-backups/backup-${timestamp}`;

      console.log(`📦 Criando backup: ${bucket}`);

      // Executar exportação
      await client.exportDocuments({
        name: databaseName,
        outputUriPrefix: bucket,
        collectionIds: [] // Todas as collections
      });

      console.log(`✅ Backup criado com sucesso: ${bucket}`);
      
      // Opcional: Notificar sobre sucesso
      await notificarSucesso(timestamp);
      
      return null;
    } catch (error) {
      console.error('❌ ERRO NO BACKUP:', error);
      
      // Notificar sobre falha
      await notificarFalha(error);
      
      throw error;
    }
  });

/**
 * Limpeza de backups antigos
 * Agenda: 0 4 * * * (4h AM diariamente)
 * Mantém apenas últimos 30 dias
 */
exports.cleanOldBackups = functions.pubsub
  .schedule('0 4 * * *') // 4h AM diariamente
  .timeZone('America/Sao_Paulo')
  .onRun(async (context) => {
    console.log('🧹 INICIANDO LIMPEZA DE BACKUPS ANTIGOS');
    
    try {
      const storageClient = storage();
      const bucket = storageClient.bucket('gestaobilhares-backups');

      // Listar todos os arquivos
      const [files] = await bucket.getFiles();
      
      // Calcular data limite (30 dias atrás)
      const thirtyDaysAgo = new Date(Date.now() - (30 * 24 * 60 * 60 * 1000));
      
      let deletados = 0;
      
      for (const file of files) {
        const [metadata] = await file.getMetadata();
        const created = new Date(metadata.timeCreated);
        
        // Se arquivo for mais antigo que 30 dias, deletar
        if (created < thirtyDaysAgo) {
          await file.delete();
          console.log(`🗑️ Backup antigo deletado: ${file.name} (criado em ${created.toISOString()})`);
          deletados++;
        }
      }
      
      console.log(`✅ Limpeza concluída. ${deletados} backups deletados.`);
      
      return deletados;
    } catch (error) {
      console.error('❌ ERRO NA LIMPEZA:', error);
      throw error;
    }
  });

/**
 * Backup manual sob demanda
 * Pode ser chamado via HTTP trigger para backups emergenciais
 */
exports.backupManual = functions.https.onRequest(async (req, res) => {
  try {
    console.log('🔥 INICIANDO BACKUP MANUAL');
    
    const client = new firestore.v1.FirestoreAdminClient();
    const projectId = 'gestaobilhares';
    const databaseName = client.databasePath(projectId, '(default)');

    // Timestamp com data e hora para backup manual
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const bucket = `gs://gestaobilhares-backups/backup-manual-${timestamp}`;

    await client.exportDocuments({
      name: databaseName,
      outputUriPrefix: bucket,
      collectionIds: []
    });

    console.log(`✅ Backup manual criado: ${bucket}`);
    
    res.status(200).json({
      success: true,
      message: 'Backup manual criado com sucesso',
      bucket: bucket,
      timestamp: timestamp
    });
    
  } catch (error) {
    console.error('❌ ERRO NO BACKUP MANUAL:', error);
    
    res.status(500).json({
      success: false,
      message: 'Erro ao criar backup manual',
      error: error.message
    });
  }
});

/**
 * Verificar status dos backups
 * Retorna informação sobre backups disponíveis
 */
exports.verificarBackups = functions.https.onRequest(async (req, res) => {
  try {
    const storageClient = storage();
    const bucket = storageClient.bucket('gestaobilhares-backups');
    const [files] = await bucket.getFiles();
    
    // Organizar backups por data
    const backups = files
      .filter(file => file.name.startsWith('backup-'))
      .map(file => {
        const [metadata] = file.getMetadataSync();
        return {
          nome: file.name,
          tamanho: metadata.size,
          criadoEm: metadata.timeCreated,
          tipo: file.name.includes('manual') ? 'manual' : 'automático'
        };
      })
      .sort((a, b) => new Date(b.criadoEm) - new Date(a.criadoEm));
    
    res.status(200).json({
      total: backups.length,
      backups: backups,
      ultimoBackup: backups[0] || null
    });
    
  } catch (error) {
    console.error('❌ ERRO AO VERIFICAR BACKUPS:', error);
    
    res.status(500).json({
      success: false,
      message: 'Erro ao verificar backups',
      error: error.message
    });
  }
});

/**
 * Funções auxiliares de notificação
 */

// Notificar sucesso do backup
async function notificarSucesso(timestamp) {
  try {
    // Opcional: Enviar notificação para Slack, email, etc.
    console.log(`📧 Notificação: Backup ${timestamp} concluído com sucesso`);
    
    // Exemplo: Poderia integrar com SendGrid, Slack API, etc.
    // await sendEmail({
    //   to: 'admin@gestaobilhares.com',
    //   subject: '✅ Backup Firestore Concluído',
    //   text: `Backup diário ${timestamp} criado com sucesso.`
    // });
    
  } catch (error) {
    console.warn('⚠️ Erro ao notificar sucesso:', error.message);
  }
}

// Notificar falha no backup
async function notificarFalha(error) {
  try {
    console.error(`📧 Notificação: Falha no backup - ${error.message}`);
    
    // Exemplo: Notificação de emergência
    // await sendEmergencyAlert({
    //   message: `❌ Falha crítica no backup Firestore: ${error.message}`,
    //   priority: 'high'
    // });
    
  } catch (notificationError) {
    console.warn('⚠️ Erro ao notificar falha:', notificationError.message);
  }
}

/**
 * Configurações de ambiente necessárias:
 * 
 * 1. Firebase Functions habilitadas:
 *    firebase functions:config:get
 * 
 * 2. Bucket do Cloud Storage criado:
 *    gsutil mb gs://gestaobilhares-backups
 * 
 * 3. Permissões configuradas:
 *    - Firestore Admin API
 *    - Cloud Storage Admin
 *    - Cloud Scheduler API
 * 
 * 4. Billing ativado (Blaze plan):
 *    Necessário para Cloud Functions agendadas
 * 
 * 5. Deploy das funções:
 *    firebase deploy --only functions
 * 
 * 6. Teste manual:
 *    curl https://us-central1-gestaobilhares.cloudfunctions.net/backupManual
 *    curl https://us-central1-gestaobilhares.cloudfunctions.net/verificarBackups
 */
