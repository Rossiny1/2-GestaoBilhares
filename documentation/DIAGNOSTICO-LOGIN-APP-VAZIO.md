# Diagnóstico Completo - Login em App Vazio

## Problema
User criado e aprovado não consegue fazer login em app sem dados, mesmo estando aprovado e ativo no banco.

## Análise Profunda

### Problemas Identificados

#### 1. **Inconsistência de Nomenclatura de Campos**
- **Problema**: Regras do Firestore verificam `firebaseUid` (camelCase) mas o campo pode estar como `firebase_uid` (snake_case)
- **Localização**: 
  - `firestore.rules` linha 127: verifica `resource.data.firebaseUid`
  - `AuthViewModel.kt` linha 1525: busca por `firebaseUid` (camelCase)
- **Impacto**: Regras podem falhar ao verificar o campo, queries podem não encontrar documentos
- **Solução**: Verificar ambos os formatos (camelCase e snake_case)

#### 2. **Campos Não Presentes no Documento**
- **Problema**: Regras verificam `'nivel_acesso' in resource.data` mas o documento pode não ter esse campo
- **Localização**: `firestore.rules` linha 137
- **Impacto**: Query retorna 0 documentos mesmo que o colaborador exista
- **Solução**: Verificar ambos os formatos (`nivel_acesso` e `nivelAcesso`)

#### 3. **Crashlytics Não Capturando Logs**
- **Problema**: Logs podem não estar sendo enviados ou processados
- **Causa Possível**: 
  - CrashlyticsTree filtra logs DEBUG/VERBOSE
  - Logs podem não estar sendo enviados imediatamente
  - Problemas de conectividade
- **Solução**: Criar sistema de diagnóstico local que não depende do Crashlytics

#### 4. **Query collectionGroup Não Funciona Sem Autenticação**
- **Problema**: Mesmo com regras permitindo, a query pode estar falhando
- **Causa**: Regras do Firestore podem não estar sendo aplicadas corretamente
- **Solução**: Simplificar regras e adicionar fallback para busca direta

## Correções Implementadas

### 1. Regras do Firestore Atualizadas
- ✅ Verificar ambos os formatos de `firebaseUid` (`firebaseUid` e `firebase_uid`)
- ✅ Verificar ambos os formatos de `nivel_acesso` (`nivel_acesso` e `nivelAcesso`)
- ✅ Permitir busca sem autenticação para colaboradores aprovados e ativos

### 2. AuthViewModel Melhorado
- ✅ Tentar busca por `firebaseUid` (camelCase) e `firebase_uid` (snake_case)
- ✅ Adicionar logs detalhados em cada etapa da busca
- ✅ Integrar sistema de diagnóstico local

### 3. Sistema de Diagnóstico Local
- ✅ Criar `LoginDiagnostics.kt` para diagnóstico independente do Crashlytics
- ✅ Logs em arquivo local (`/data/data/com.example.gestaobilhares/files/login_diagnostics.log`)
- ✅ Logs no Logcat para debug imediato
- ✅ Teste completo de busca com resultado detalhado

## Como Usar o Diagnóstico

### 1. Verificar Logs Locais
```bash
# No dispositivo Android (via adb)
adb shell run-as com.example.gestaobilhares cat files/login_diagnostics.log
```

### 2. Verificar Logcat
```bash
# Filtrar logs do diagnóstico
adb logcat | grep LoginDiagnostics
```

### 3. Verificar Crashlytics
- Acessar Firebase Console > Crashlytics
- Procurar por logs com tag `[LOGIN_FLOW]` ou `[BUSCA_NUVEM]`
- Verificar custom keys: `diagnostico_colaborador_encontrado`, `diagnostico_aprovado`, `diagnostico_ativo`

## Próximos Passos

1. **Testar com App Limpo**
   - Limpar dados do app
   - Tentar fazer login com User criado e aprovado
   - Verificar logs locais e Crashlytics

2. **Verificar Estrutura dos Dados no Firestore**
   - Acessar Firebase Console > Firestore
   - Verificar estrutura exata do documento do User
   - Comparar com o que as regras esperam

3. **Testar Regras do Firestore**
   - Usar Firebase Emulator para testar regras
   - Verificar se as regras permitem a query sem autenticação

4. **Ajustar Conforme Necessário**
   - Se os dados estão em camelCase, ajustar regras
   - Se os dados estão em snake_case, garantir consistência
   - Se há campos faltando, adicionar aos documentos

## Estrutura Esperada do Documento

```json
{
  "id": 123,
  "nome": "Nome do User",
  "email": "user@teste.com",
  "nivel_acesso": "USER",  // ou "nivelAcesso"
  "aprovado": true,
  "ativo": true,
  "firebase_uid": "abc123",  // ou "firebaseUid"
  "senha_temporaria": "senha123",
  "primeiro_acesso": true
}
```

## Notas Importantes

- O diagnóstico local não depende do Crashlytics, então sempre funcionará
- Os logs locais são persistentes e podem ser consultados mesmo após o app fechar
- O sistema tenta ambos os formatos (camelCase e snake_case) para máxima compatibilidade
- As regras do Firestore foram simplificadas para serem mais permissivas durante diagnóstico
