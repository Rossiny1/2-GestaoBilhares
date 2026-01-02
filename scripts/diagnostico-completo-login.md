# Diagnóstico Completo - Problema de Login em App Vazio

## Problema Identificado
User criado e aprovado não consegue fazer login em app sem dados, mesmo estando aprovado e ativo no banco.

## Possíveis Causas

### 1. Inconsistência de Nomenclatura de Campos
- **Problema**: Regras do Firestore verificam `firebaseUid` (camelCase) mas o campo pode estar como `firebase_uid` (snake_case)
- **Localização**: `firestore.rules` linha 127
- **Impacto**: Regras podem falhar ao verificar o campo

### 2. Campos Não Presentes no Documento
- **Problema**: Regras verificam `'nivel_acesso' in resource.data` mas o documento pode não ter esse campo
- **Localização**: `firestore.rules` linha 137
- **Impacto**: Query retorna 0 documentos mesmo que o colaborador exista

### 3. Crashlytics Não Capturando Logs
- **Problema**: Logs podem não estar sendo enviados ou processados
- **Causa Possível**: CrashlyticsTree filtra logs DEBUG/VERBOSE
- **Solução**: Adicionar logs diretos via `crashlytics.log()` e `crashlytics.recordException()`

### 4. Query collectionGroup Não Funciona Sem Autenticação
- **Problema**: Mesmo com regras permitindo, a query pode estar falhando
- **Causa**: Regras do Firestore podem não estar sendo aplicadas corretamente

## Estratégia de Diagnóstico

### Passo 1: Verificar Estrutura Real dos Dados no Firestore
```bash
# Usar Firebase Console ou script de teste
# Verificar se os campos estão em snake_case ou camelCase
```

### Passo 2: Testar Regras do Firestore
```bash
# Usar Firebase Emulator ou script de teste
# Verificar se as regras permitem a query sem autenticação
```

### Passo 3: Adicionar Logs Locais (Não Depender do Crashlytics)
- Adicionar logs em arquivo local
- Adicionar logs via Logcat
- Adicionar logs via Toast/Alert no app

### Passo 4: Testar Query Diretamente
- Criar função de teste que executa a query e imprime resultado
- Testar com diferentes emails (case-sensitive)
- Testar com diferentes estruturas de dados

## Correções Necessárias

### 1. Corrigir Inconsistência de Campos
- Verificar se `firebaseUid` ou `firebase_uid` está sendo usado
- Padronizar para snake_case em todos os lugares

### 2. Melhorar Regras do Firestore
- Adicionar fallback para verificar ambos os formatos (camelCase e snake_case)
- Simplificar regras para serem mais permissivas durante diagnóstico

### 3. Adicionar Logs Locais
- Criar arquivo de log local
- Adicionar logs detalhados em cada etapa da busca

### 4. Testar com Dados Reais
- Verificar estrutura exata do documento do User no Firestore
- Comparar com o que as regras esperam
