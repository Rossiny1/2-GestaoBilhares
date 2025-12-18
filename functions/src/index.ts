import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

admin.initializeApp();

/**
 * Trigger: onCreate do Firebase Auth
 * Executado quando um novo usuário se cadastra/loga pela primeira vez.
 * Busca o colaborador correspondente no Firestore e define as Custom Claims.
 */
export const onUserCreated = functions.auth.user().onCreate(async (user: admin.auth.UserRecord) => {
    const email = user.email;
    if (!email) {
        console.log("Usuário criado sem email:", user.uid);
        return;
    }

    console.log(`Novo usuário criado: ${email}. Buscando perfil de colaborador...`);

    // ✅ CORREÇÃO: A estrutura agora é empresas/{id}/entidades/colaboradores/items/{itemId}
    // O collectionGroup deve ser 'items' e filtramos pelo path
    const snapshot = await admin.firestore().collectionGroup("items")
        .where("email", "==", email)
        .get();

    const doc = snapshot.docs.find((d: admin.firestore.QueryDocumentSnapshot) => d.ref.path.includes("/colaboradores/items/"));

    if (!doc) {
        console.log(`Nenhum colaborador encontrado para o email ${email}`);
        // Define claim 'pending' para indicar que não tem acesso ainda
        await admin.auth().setCustomUserClaims(user.uid, {
            role: "pending",
            approved: false
        });
        return;
    }

    const data = doc.data();

    // Extrair empresaId do caminho: empresas/{empresaId}/entidades/colaboradores/items/{id}
    const pathSegments = doc.ref.path.split("/");
    const empresaId = pathSegments[1];

    // Configura as claims que serão usadas nas Security Rules
    const claims = {
        companyId: empresaId,
        colaboradorId: doc.id,
        role: data.nivelAcesso || data.role || "collaborator", // Ajustado para nivelAcesso (Kotlin enum)
        approved: true
    };

    console.log(`Atribuindo claims para ${email}:`, claims);
    await admin.auth().setCustomUserClaims(user.uid, claims);

    // Atualizar o documento do colaborador com o UID do Auth
    await doc.ref.update({ firebaseUid: user.uid });
});

/**
 * Trigger: onWrite no Firestore (Colaboradores)
 * Executado quando um administrador cria ou edita um colaborador.
 * Atualiza as claims do usuário Auth correspondente em tempo real.
 */
export const onCollaboratorUpdated = functions.firestore
    .document("empresas/{empresaId}/entidades/colaboradores/items/{docId}")
    .onWrite(async (change: functions.Change<functions.firestore.DocumentSnapshot>, context: functions.EventContext) => {
        const data = change.after.exists ? change.after.data() : null;

        // Se deletado
        if (!data) {
            console.log("Colaborador deletado.");
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
                role: data.nivelAcesso || data.role || "collaborator",
                approved: true
            };

            await admin.auth().setCustomUserClaims(userRecord.uid, claims);
            console.log(`Claims atualizadas com sucesso para ${email} (UID: ${userRecord.uid})`);

            // Se o firebaseUid ainda não estiver no doc, salva agora
            if (!data.firebaseUid) {
                await change.after.ref.update({ firebaseUid: userRecord.uid });
            }

        } catch (error) {
            console.log(`Usuário Auth não encontrado para email ${email}.`);
        }
    });
