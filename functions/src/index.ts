import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

admin.initializeApp();

/**
 * Trigger: onCreate do Firebase Auth
 * Executado quando um novo usuário se cadastra/loga pela primeira vez.
 * Busca o colaborador correspondente no Firestore e define as Custom Claims.
 */
export const onUserCreated = functions.auth.user().onCreate(async (user) => {
    const email = user.email;
    if (!email) {
        console.log("Usuário criado sem email:", user.uid);
        return;
    }

    console.log(`Novo usuário criado: ${email}. Buscando perfil de colaborador...`);

    // Group Query por 'email' na coleção 'colaboradores'
    // IMPORTANTE: Requer índice no Firestore (colaboradores / email)
    const snapshot = await admin.firestore().collectionGroup("colaboradores")
        .where("email", "==", email)
        .limit(1)
        .get();

    if (snapshot.empty) {
        console.log(`Nenhum colaborador encontrado para o email ${email}`);
        // Define claim 'pending' para indicar que não tem acesso ainda
        await admin.auth().setCustomUserClaims(user.uid, { 
            role: "pending",
            approved: false 
        });
        return;
    }

    const doc = snapshot.docs[0];
    const data = doc.data();
    
    // Extrair empresaId do caminho: empresas/{empresaId}/entidades/colaboradores/{id}
    // doc.ref.path ex: empresas/empresa_001/entidades/colaboradores/items/colab_123
    const pathSegments = doc.ref.path.split("/");
    const empresaId = pathSegments[1]; 
    
    // Configura as claims que serão usadas nas Security Rules
    const claims = {
        companyId: empresaId,
        colaboradorId: doc.id,
        role: data.role || "collaborator", // admin, gerente, etc
        approved: true
    };

    console.log(`Atribuindo claims para ${email}:`, claims);
    await admin.auth().setCustomUserClaims(user.uid, claims);
    
    // Opcional: Atualizar o documento do colaborador com o UID do Auth para referência futura
    await doc.ref.update({ authUid: user.uid });
});

/**
 * Trigger: onWrite no Firestore (Colaboradores)
 * Executado quando um administrador cria ou edita um colaborador.
 * Atualiza as claims do usuário Auth correspondente em tempo real.
 */
export const onCollaboratorUpdated = functions.firestore
    .document("empresas/{empresaId}/entidades/colaboradores/items/{docId}")
    .onWrite(async (change, context) => {
        const data = change.after.exists ? change.after.data() : null;
        
        // Se deletado
        if (!data) {
            console.log("Colaborador deletado. A revogação automática de acesso requer busca pelo email antigo.");
            return; 
        }

        const email = data.email;
        if (!email) {
            console.log("Colaborador sem email no cadastro.");
            return;
        }

        try {
            // Buscar usuário no Auth pelo email
            const userRecord = await admin.auth().getUserByEmail(email);
            
            const claims = {
                companyId: context.params.empresaId,
                colaboradorId: context.params.docId,
                role: data.role || "collaborator",
                approved: true
            };

            await admin.auth().setCustomUserClaims(userRecord.uid, claims);
            console.log(`Claims atualizadas com sucesso para ${email} (UID: ${userRecord.uid})`);
            
            // Se o authUid ainda não estiver no doc, salva agora
            if (!data.authUid) {
                 await change.after.ref.update({ authUid: userRecord.uid });
            }

        } catch (error) {
            console.log(`Usuário Auth não encontrado para email ${email}. As claims serão definidas quando ele se cadastrar (via onUserCreated).`);
        }
    });
