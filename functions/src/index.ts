import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

admin.initializeApp();

/**
 * ✅ FUNÇÃO AUXILIAR: Busca rotas atribuídas a um colaborador
 * Retorna array de IDs de rotas (rotasAtribuidas) para incluir nas Custom Claims
 */
async function getRotasAtribuidas(empresaId: string, colaboradorId: string): Promise<number[]> {
    try {
        const colaboradorRotasRef = admin.firestore()
            .collection("empresas")
            .doc(empresaId)
            .collection("entidades")
            .doc("colaborador_rota")
            .collection("items");
        
        // Buscar todas as rotas onde este colaborador está vinculado
        const snapshot = await colaboradorRotasRef
            .where("colaboradorId", "==", colaboradorId)
            .get();
        
        const rotasIds: number[] = [];
        snapshot.forEach((doc) => {
            const data = doc.data();
            const rotaId = data.rotaId;
            if (rotaId != null && typeof rotaId === "number") {
                rotasIds.push(rotaId);
            }
        });
        
        console.log(`Colaborador ${colaboradorId} tem ${rotasIds.length} rotas atribuídas: [${rotasIds.join(", ")}]`);
        return rotasIds;
    } catch (error) {
        console.error(`Erro ao buscar rotas do colaborador ${colaboradorId}:`, error);
        // ✅ FALLBACK SEGURO: Retorna array vazio em caso de erro (não bloqueia criação de claims)
        return [];
    }
}

/**
 * Trigger: onCreate do Firebase Auth
 * Executado quando um novo usuário se cadastra/loga pela primeira vez.
 * Busca o colaborador correspondente no Firestore e define as Custom Claims.
 * ✅ ATUALIZADO: Agora inclui rotasAtribuidas nas claims
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
    const colaboradorId = doc.id;

    // Normalizar role para minúsculo para evitar problemas de case-sensitivity nas Security Rules
    const rawRole = data.nivelAcesso || data.role || "collaborator";
    const role = typeof rawRole === "string" ? rawRole.toLowerCase() : "collaborator";
    const isAdmin = role === "admin";

    // ✅ NOVO: Buscar rotas atribuídas ao colaborador
    const rotasAtribuidas = await getRotasAtribuidas(empresaId, colaboradorId);

    // Configura as claims que serão usadas nas Security Rules
    const claims: any = {
        companyId: empresaId,
        colaboradorId: colaboradorId,
        role: role,
        admin: isAdmin, // Claim explícita para facilitar regras
        approved: true
    };

    // ✅ NOVO: Adicionar rotasAtribuidas apenas se houver rotas (evita claim vazia desnecessária)
    if (rotasAtribuidas.length > 0) {
        claims.rotasAtribuidas = rotasAtribuidas;
    }

    console.log(`Atribuindo claims para ${email}:`, JSON.stringify(claims));
    await admin.auth().setCustomUserClaims(user.uid, claims);

    // Atualizar o documento do colaborador com o UID do Auth
    await doc.ref.update({ firebaseUid: user.uid });
});

/**
 * ✅ FUNÇÃO AUXILIAR: Atualiza claims de um usuário específico
 * Usada por múltiplas funções para evitar duplicação de código
 */
async function updateUserClaims(
    userUid: string,
    empresaId: string,
    colaboradorId: string,
    role: string,
    email: string
): Promise<void> {
    const isAdmin = role === "admin";
    
    // ✅ NOVO: Buscar rotas atribuídas ao colaborador
    const rotasAtribuidas = await getRotasAtribuidas(empresaId, colaboradorId);
    
    const claims: any = {
        companyId: empresaId,
        colaboradorId: colaboradorId,
        role: role,
        admin: isAdmin,
        approved: true
    };
    
    // ✅ NOVO: Adicionar rotasAtribuidas apenas se houver rotas
    if (rotasAtribuidas.length > 0) {
        claims.rotasAtribuidas = rotasAtribuidas;
    }
    
    await admin.auth().setCustomUserClaims(userUid, claims);
    console.log(`Claims atualizadas para ${email}:`, JSON.stringify(claims));
}

/**
 * Trigger: onWrite no Firestore (Colaboradores)
 * Executado quando um administrador cria ou edita um colaborador.
 * Atualiza as claims do usuário Auth correspondente em tempo real.
 * ✅ ATUALIZADO: Agora inclui rotasAtribuidas nas claims
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

            // Normalizar role para minúsculo
            const rawRole = data.nivelAcesso || data.role || "collaborator";
            const role = typeof rawRole === "string" ? rawRole.toLowerCase() : "collaborator";

            // ✅ ATUALIZADO: Usar função auxiliar que inclui rotasAtribuidas
            await updateUserClaims(
                userRecord.uid,
                context.params.empresaId,
                context.params.docId,
                role,
                email
            );

            // Se o firebaseUid ainda não estiver no doc, salva agora
            if (!data.firebaseUid) {
                await change.after.ref.update({ firebaseUid: userRecord.uid });
            }

        } catch (error) {
            console.log(`Usuário Auth não encontrado para email ${email}.`);
        }
    });

/**
 * ✅ NOVO: Trigger quando rotas de um colaborador são atribuídas/removidas
 * Atualiza automaticamente as claims do usuário quando suas rotas mudam
 */
export const onColaboradorRotaUpdated = functions.firestore
    .document("empresas/{empresaId}/entidades/colaborador_rota/items/{docId}")
    .onWrite(async (change: functions.Change<functions.firestore.DocumentSnapshot>, context: functions.EventContext) => {
        const empresaId = context.params.empresaId;
        const data = change.after.exists ? change.after.data() : null;
        
        // Se deletado, precisamos atualizar o colaborador removendo a rota das claims
        if (!data) {
            const oldData = change.before.exists ? change.before.data() : null;
            if (oldData && oldData.colaboradorId) {
                await updateColaboradorClaimsFromRotas(empresaId, oldData.colaboradorId);
            }
            return;
        }
        
        const colaboradorId = data.colaboradorId;
        if (!colaboradorId) {
            console.log("Documento colaborador_rota sem colaboradorId");
            return;
        }
        
        // Atualizar claims do colaborador com todas as rotas atualizadas
        await updateColaboradorClaimsFromRotas(empresaId, colaboradorId);
    });

/**
 * ✅ FUNÇÃO AUXILIAR: Atualiza claims de um colaborador baseado em suas rotas
 * Busca todas as rotas do colaborador e atualiza as claims
 */
async function updateColaboradorClaimsFromRotas(empresaId: string, colaboradorId: string): Promise<void> {
    try {
        // Buscar documento do colaborador
        const colaboradorRef = admin.firestore()
            .collection("empresas")
            .doc(empresaId)
            .collection("entidades")
            .doc("colaboradores")
            .collection("items")
            .doc(colaboradorId);
        
        const colaboradorDoc = await colaboradorRef.get();
        if (!colaboradorDoc.exists) {
            console.log(`Colaborador ${colaboradorId} não encontrado`);
            return;
        }
        
        const colaboradorData = colaboradorDoc.data();
        if (!colaboradorData) {
            return;
        }
        
        const email = colaboradorData.email;
        const firebaseUid = colaboradorData.firebaseUid;
        
        if (!email || !firebaseUid) {
            console.log(`Colaborador ${colaboradorId} sem email ou firebaseUid`);
            return;
        }
        
        // Buscar role do colaborador
        const rawRole = colaboradorData.nivelAcesso || colaboradorData.role || "collaborator";
        const role = typeof rawRole === "string" ? rawRole.toLowerCase() : "collaborator";
        
        // Atualizar claims (incluindo rotasAtribuidas)
        await updateUserClaims(firebaseUid, empresaId, colaboradorId, role, email);
        
        console.log(`Claims atualizadas para colaborador ${colaboradorId} após mudança de rotas`);
    } catch (error) {
        console.error(`Erro ao atualizar claims do colaborador ${colaboradorId}:`, error);
    }
}

/**
 * ✅ FUNÇÃO ADMINISTRATIVA: Migra claims de todos os usuários existentes
 * Esta função deve ser chamada manualmente via HTTP callable para atualizar usuários antigos
 * 
 * Uso: Chamar via HTTP POST para /migrateUserClaims
 * Requer autenticação de admin
 */
export const migrateUserClaims = functions.https.onCall(async (data, context) => {
    // ✅ SEGURANÇA: Verificar se é admin
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Usuário não autenticado");
    }
    
    // Simplificar isAdmin para facilitar execução via shell/emulador
    const isAdmin = context.auth.token.email === "rossinys@gmail.com" || 
                   context.auth.token.admin === true ||
                   context.auth.token.role === "admin";
    
    if (!isAdmin) {
        throw new functions.https.HttpsError("permission-denied", "Apenas administradores podem executar esta função");
    }
    
    console.log(`Migração iniciada por: ${context.auth.token.email}`);
    console.log("Iniciando migração de claims de usuários existentes...");
    
    const results = {
        total: 0,
        success: 0,
        failed: 0,
        errors: [] as string[]
    };
    
    try {
        // Buscar todas as empresas
        const empresasSnapshot = await admin.firestore().collection("empresas").get();
        
        for (const empresaDoc of empresasSnapshot.docs) {
            const empresaId = empresaDoc.id;
            console.log(`Processando empresa: ${empresaId}`);
            
            // Buscar todos os colaboradores da empresa
            const colaboradoresRef = admin.firestore()
                .collection("empresas")
                .doc(empresaId)
                .collection("entidades")
                .doc("colaboradores")
                .collection("items");
            
            const colaboradoresSnapshot = await colaboradoresRef.get();
            
            for (const colaboradorDoc of colaboradoresSnapshot.docs) {
                results.total++;
                const colaboradorId = colaboradorDoc.id;
                const colaboradorData = colaboradorDoc.data();
                
                const email = colaboradorData.email;
                const firebaseUid = colaboradorData.firebaseUid;
                
                if (!email || !firebaseUid) {
                    results.failed++;
                    results.errors.push(`Colaborador ${colaboradorId} sem email ou firebaseUid`);
                    continue;
                }
                
                try {
                    // Verificar se usuário existe no Auth (validação)
                    await admin.auth().getUser(firebaseUid);
                    
                    // Buscar role
                    const rawRole = colaboradorData.nivelAcesso || colaboradorData.role || "collaborator";
                    const role = typeof rawRole === "string" ? rawRole.toLowerCase() : "collaborator";
                    
                    // Atualizar claims
                    await updateUserClaims(firebaseUid, empresaId, colaboradorId, role, email);
                    
                    results.success++;
                    console.log(`✅ Claims atualizadas para ${email}`);
                } catch (error: any) {
                    results.failed++;
                    const errorMsg = `Erro ao atualizar ${email}: ${error.message}`;
                    results.errors.push(errorMsg);
                    console.error(`❌ ${errorMsg}`);
                }
            }
        }
        
        console.log(`Migração concluída: ${results.success}/${results.total} sucesso, ${results.failed} falhas`);
        return results;
    } catch (error: any) {
        console.error("Erro na migração:", error);
        throw new functions.https.HttpsError("internal", `Erro na migração: ${error.message}`);
    }
});

/**
 * ✅ FUNÇÃO DE VALIDAÇÃO: Verifica se todos os usuários têm claims configuradas
 * Útil para validar antes de remover fallbacks das Security Rules
 * 
 * Uso: Chamar via HTTP POST para /validateUserClaims
 * Requer autenticação de admin
 */
export const validateUserClaims = functions.https.onCall(async (data, context) => {
    // ✅ SEGURANÇA: Verificar se é admin
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Usuário não autenticado");
    }
    
    const userRecord = await admin.auth().getUser(context.auth.uid);
    const isAdmin = userRecord.customClaims?.admin === true || 
                   userRecord.customClaims?.role === "admin" ||
                   userRecord.email === "rossinys@gmail.com";
    
    if (!isAdmin) {
        throw new functions.https.HttpsError("permission-denied", "Apenas administradores podem executar esta função");
    }
    
    console.log("Iniciando validação de claims de usuários...");
    
    const results = {
        total: 0,
        withCompanyId: 0,
        withoutCompanyId: 0,
        withoutClaims: 0,
        details: [] as any[]
    };
    
    try {
        // Listar todos os usuários do Auth
        let nextPageToken: string | undefined;
        
        do {
            const listUsersResult = await admin.auth().listUsers(1000, nextPageToken);
            
            for (const user of listUsersResult.users) {
                results.total++;
                
                const claims = user.customClaims || {};
                const hasCompanyId = !!claims.companyId;
                
                if (!hasCompanyId) {
                    results.withoutCompanyId++;
                    results.details.push({
                        uid: user.uid,
                        email: user.email,
                        reason: !claims.role ? "Sem claims" : "Sem companyId"
                    });
                    
                    if (!claims.role) {
                        results.withoutClaims++;
                    }
                } else {
                    results.withCompanyId++;
                }
            }
            
            nextPageToken = listUsersResult.pageToken;
        } while (nextPageToken);
        
        console.log(`Validação concluída: ${results.withCompanyId}/${results.total} com companyId`);
        return results;
    } catch (error: any) {
        console.error("Erro na validação:", error);
        throw new functions.https.HttpsError("internal", `Erro na validação: ${error.message}`);
    }
});

/**
 * ⚠️ FUNÇÃO TEMPORÁRIA DE MIGRAÇÃO (HTTP)
 * Permite executar a migração via URL direta para contornar problemas de shell
 * Protegida por uma chave simples passada via query param
 */
export const executeMigrationHttp = functions.https.onRequest(async (req, res) => {
    const key = req.query.key;
    if (key !== "migrate_2025_gestao") {
        res.status(403).send("Acesso negado");
        return;
    }

    console.log("Iniciando migração via HTTP...");
    
    try {
        const empresasSnapshot = await admin.firestore().collection("empresas").get();
        console.log(`Empresas encontradas: ${empresasSnapshot.size}`);
        
        const results = { total: 0, success: 0, failed: 0, logs: [] as string[], errors: [] as string[] };
        
        for (const empresaDoc of empresasSnapshot.docs) {
            const empresaId = empresaDoc.id;
            results.logs.push(`Processando empresa: ${empresaId}`);
            
            const entidadesRef = admin.firestore()
                .collection("empresas")
                .doc(empresaId)
                .collection("entidades");
            
            const entidadesSnapshot = await entidadesRef.get();
            results.logs.push(`Entidades encontradas na empresa ${empresaId}: ${entidadesSnapshot.size}`);
            
            const colaboradoresRef = entidadesRef
                .doc("colaboradores")
                .collection("items");
            
            const colaboradoresSnapshot = await colaboradoresRef.get();
            results.logs.push(`Colaboradores encontrados em ${empresaId}: ${colaboradoresSnapshot.size}`);
            
            for (const colaboradorDoc of colaboradoresSnapshot.docs) {
                results.total++;
                const data = colaboradorDoc.data();
                const email = data.email;
                const firebaseUid = data.firebaseUid;
                
                if (!email || !firebaseUid) {
                    results.failed++;
                    results.logs.push(`Colaborador ${colaboradorDoc.id} ignorado (sem email ou UID)`);
                    continue;
                }
                
                try {
                    const rawRole = data.nivelAcesso || data.role || "collaborator";
                    const role = typeof rawRole === "string" ? rawRole.toLowerCase() : "collaborator";
                    
                    await updateUserClaims(firebaseUid, empresaId, colaboradorDoc.id, role, email);
                    results.success++;
                    results.logs.push(`Claims atualizadas para ${email}`);
                } catch (err: any) {
                    results.failed++;
                    results.errors.push(`${email}: ${err.message}`);
                }
            }
        }
        
        res.status(200).json(results);
    } catch (error: any) {
        res.status(500).send(error.message);
    }
});
