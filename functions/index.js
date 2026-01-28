const functions = require("firebase-functions");
const admin = require("firebase-admin");

// Inicializar Firebase Admin
admin.initializeApp();

// Função para backup automático do Firestore
exports.backupFirestore = functions.https.onRequest(async (req, res) => {
  try {
    console.log("Iniciando backup do Firestore...");
    
    const db = admin.firestore();
    const backup = {};
    
    // Coleções principais para backup
    const collections = [
      'clientes',
      'acertos', 
      'mesas',
      'colaboradores',
      'rotas'
    ];
    
    // Backup de cada coleção
    for (const collectionName of collections) {
      console.log(`Fazendo backup da coleção: ${collectionName}`);
      
      const snapshot = await db.collection(collectionName).get();
      const documents = [];
      
      snapshot.forEach(doc => {
        documents.push({
          id: doc.id,
          data: doc.data()
        });
      });
      
      backup[collectionName] = documents;
      console.log(`${documents.length} documentos backupados de ${collectionName}`);
    }
    
    // Salvar backup em uma coleção separada
    const backupRef = db.collection('backups').doc(new Date().toISOString());
    await backupRef.set({
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      collections: backup,
      totalDocuments: Object.values(backup).reduce((sum, docs) => sum + docs.length, 0)
    });
    
    console.log("Backup concluído com sucesso!");
    
    res.status(200).json({
      success: true,
      message: "Backup realizado com sucesso",
      timestamp: new Date().toISOString(),
      collections: Object.keys(backup).length,
      totalDocuments: Object.values(backup).reduce((sum, docs) => sum + docs.length, 0)
    });
    
  } catch (error) {
    console.error("Erro no backup:", error);
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

// Função agendada para backup diário
exports.backupDiario = functions.pubsub
  .schedule('0 2 * * *')  // Todos os dias às 2h da manhã
  .timeZone('America/Sao_Paulo')
  .onRun(async (context) => {
    console.log("Iniciando backup diário automático...");
    
    try {
      const db = admin.firestore();
      const backup = {};
      
      const collections = ['clientes', 'acertos', 'mesas', 'colaboradores', 'rotas'];
      
      for (const collectionName of collections) {
        const snapshot = await db.collection(collectionName).get();
        const documents = [];
        
        snapshot.forEach(doc => {
          documents.push({
            id: doc.id,
            data: doc.data()
          });
        });
        
        backup[collectionName] = documents;
      }
      
      const backupRef = db.collection('backups').doc(`diario_${new Date().toISOString().split('T')[0]}`);
      await backupRef.set({
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        type: 'daily',
        collections: backup,
        totalDocuments: Object.values(backup).reduce((sum, docs) => sum + docs.length, 0)
      });
      
      console.log(`Backup diário concluído: ${Object.values(backup).reduce((sum, docs) => sum + docs.length, 0)} documentos`);
      
    } catch (error) {
      console.error("Erro no backup diário:", error);
    }
  });
