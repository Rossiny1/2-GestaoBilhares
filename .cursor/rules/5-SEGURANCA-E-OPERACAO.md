# 5️⃣ SEGURANÇA E OPERAÇÃO

> **Propósito**: Regras de proteção de dados, segurança de nuvem e processo de release.  
> **Última Atualização**: Dezembro 2025  
> **Versão**: 3.0 (Segurança Crítica Resolvida)

---

## 🔐 SEGURANÇA FIREBASE (Firestore Rules)
Todas as coleções seguem o princípio de privilégio mínimo e isolamento multi-tenant:
1.  **Regra de Ouro**: Acesso permitido apenas se `request.auth.token.companyId == resource.data.empresaId`.
2.  **Fallbacks**: Não são permitidos fallbacks de leitura/escrita global em coleções operacionais.
3.  **Deploy**: As regras são mantidas no arquivo `firestore.rules` e devem ser validadas antes de cada deploy.

---

## 🛡️ AUTHENTICATION & CUSTOM CLAIMS
*   **companyId**: Claim obrigatória no token JWT para identificar a empresa do usuário.
*   **rotasAtribuidas**: Lista de IDs de rotas as quais o colaborador tem acesso.
*   **Automação**: Firebase Functions (`functions/index.js`) automatizam a injeção dessas claims em:
    - Criação de novo usuário (`onUserCreated`).
    - Atualização de colaborador (`onCollaboratorUpdated`).
    - Mudança de escala de rotas (`onColaboradorRotaUpdated`).

---

## 📁 DADOS SENSÍVEIS (Local)
*   **EncryptedSharedPreferences**: Usado para armazenar tokens de acesso e dados de sessão via `SecurePreferencesHelper`.
*   **Criptografia**: AES256_GCM para chaves e valores.

---

## 📦 PROCESSO DE RELEASE (Firebase App Distribution)
O app é de uso interno e não é publicado na Play Store.
1.  **Build**: `./gradlew assembleRelease`.
2.  **Distribuição**: `firebase appdistribution:distribute [APK_PATH] --groups testers`.
3.  **Logs de Produção**: `CrashlyticsTree` de Timber envia apenas erros críticos e stack traces para o console.

---

## 📡 MONITORAMENTO MCP
O assistente de IA possui permissões de leitura no Crashlytics e Firestore via MCP, permitindo:
*   Análise de bugs em tempo real.
*   Correção proativa baseada em logs de erro reais.
