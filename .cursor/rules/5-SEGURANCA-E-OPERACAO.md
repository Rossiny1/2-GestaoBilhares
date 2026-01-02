# 5️⃣ SEGURANÇA E OPERAÇÃO

> **Propósito**: Regras de proteção de dados, segurança de nuvem e processo de release.  
> **Última Atualização**: 02 de Janeiro de 2026  
> **Versão**: 4.0 (Release 1.0.1 Deployado)

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

### Build e Deploy
1.  **Build**: `./gradlew clean :app:assembleRelease`
2.  **Deploy**: `./gradlew :app:appDistributionUploadRelease` (usa token `FIREBASE_TOKEN`)
3.  **Mapping.txt**: Gerado automaticamente em `app/build/outputs/mapping/release/mapping.txt` e enviado ao Crashlytics via task `uploadCrashlyticsMappingFileRelease`

### Release Atual
*   **Versão**: 1.0.1 (3) - Deployado em 02/01/2026
*   **Testadores**: `rossinys@gmail.com` (configurado via Gradle)
*   **Release Notes**: "Release 1.0.1 (3) - Correções Crashlytics e Testes Unitários"

### Logs de Produção
*   `CrashlyticsTree` de Timber envia apenas erros críticos e stack traces para o console.
*   ✅ Mapping.txt disponível para desofuscação de erros em produção.

---

## 📡 MONITORAMENTO MCP
O assistente de IA possui permissões de leitura no Crashlytics e Firestore via MCP, permitindo:
*   Análise de bugs em tempo real.
*   Correção proativa baseada em logs de erro reais.

### Status Crashlytics (02/01/2026)
*   ✅ **4 erros corrigidos**: DialogAditivoEquipamentosBinding, AditivoDialog, SyncRepository.mapType, JobCancellationException
*   🟡 **1 erro pendente**: s6.f0 (ofuscado) - será resolvido quando mapping.txt for processado pelo Crashlytics
*   📊 **Monitoramento**: Após deploy da release 1.0.1, verificar se erros corrigidos pararam de ocorrer
