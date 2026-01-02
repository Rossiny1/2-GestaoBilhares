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

### Ambiente de Deploy
**Recomendado**: Usar **Cursor Cloud (VM)** para builds de release, pois:
- ✅ Ambiente consistente e configurado
- ✅ Firebase CLI pré-autenticado
- ✅ Android SDK configurado corretamente
- ✅ Acesso direto ao Crashlytics via MCP

### Build e Deploy

#### Opção 1: Via Gradle (Recomendado)
```bash
# Na VM (Cursor Cloud) ou local com FIREBASE_TOKEN configurado
export FIREBASE_TOKEN="seu_token_firebase"
./gradlew clean :app:assembleRelease
./gradlew :app:appDistributionUploadRelease
```

#### Opção 2: Via Firebase CLI
```bash
# Build primeiro
./gradlew clean :app:assembleRelease

# Depois deploy via CLI
firebase appdistribution:distribute \
  app/build/outputs/apk/release/app-release.apk \
  --app 1:1089459035145:android:2d3b94222b1506a844acd8 \
  --groups "testers" \
  --release-notes "Release 1.0.1 (3) - Descrição"
```

### Configuração do Token Firebase
```bash
# Gerar token (fazer uma vez)
firebase login:ci

# Na VM, o token pode ser:
# 1. Exportado como variável de ambiente
export FIREBASE_TOKEN="token_gerado"

# 2. Ou configurado no sistema (recomendado para VM)
# Adicionar ao ~/.bashrc ou ~/.zshrc
echo 'export FIREBASE_TOKEN="token_gerado"' >> ~/.bashrc
```

### Mapping.txt
*   **Geração**: Automática em `app/build/outputs/mapping/release/mapping.txt`
*   **Upload**: Automático via task `uploadCrashlyticsMappingFileRelease` do plugin Crashlytics
*   **Uso**: Desofuscação de stack traces no Crashlytics

### Release Atual
*   **Versão**: 1.0.1 (3) - Deployado em 02/01/2026
*   **Testadores**: `rossinys@gmail.com` (configurado via Gradle)
*   **Release Notes**: "Release 1.0.1 (3) - Correções Crashlytics e Testes Unitários"
*   **Link Console**: https://console.firebase.google.com/project/gestaobilhares/appdistribution

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
