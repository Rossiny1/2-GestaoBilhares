# Guia de Teste - Diagnóstico de Login em App Vazio

## Versão do APK
- **Versão**: 1.0.1 (3)
- **Data**: $(date)
- **Correções**: Sistema de diagnóstico local + suporte para camelCase/snake_case

## O Que Foi Corrigido

### 1. Regras do Firestore
- ✅ Suporte para ambos os formatos: `firebaseUid` e `firebase_uid`
- ✅ Suporte para ambos os formatos: `nivel_acesso` e `nivelAcesso`
- ✅ Permissão de busca sem autenticação para colaboradores aprovados e ativos

### 2. AuthViewModel
- ✅ Busca por ambos os formatos de `firebaseUid` (camelCase e snake_case)
- ✅ Logs detalhados em cada etapa da busca
- ✅ Sistema de diagnóstico local integrado

### 3. Sistema de Diagnóstico Local
- ✅ `LoginDiagnostics.kt`: Diagnóstico independente do Crashlytics
- ✅ Logs em arquivo local persistente
- ✅ Logs no Logcat para debug imediato

## Como Testar

### Passo 1: Limpar Dados do App
1. Abrir Configurações do Android
2. Aplicativos > Gestão Bilhares
3. Armazenamento > Limpar dados
4. Confirmar limpeza

### Passo 2: Tentar Login com User Criado e Aprovado
1. Abrir o app (deve estar vazio)
2. Inserir email do User criado e aprovado
3. Inserir senha temporária
4. Tentar fazer login

### Passo 3: Verificar Logs Locais (NÃO DEPENDE DO CRASHLYTICS)

#### Opção A: Via ADB (Recomendado)
```bash
# Conectar dispositivo via USB
adb devices

# Ver logs em tempo real
adb logcat | grep -E "LoginDiagnostics|AuthViewModel|BUSCA_NUVEM|LOGIN_FLOW"

# Ver arquivo de log local (se disponível)
adb shell run-as com.example.gestaobilhares cat files/login_diagnostics.log
```

#### Opção B: Via Android Studio
1. Abrir Android Studio
2. Conectar dispositivo
3. Abrir Logcat
4. Filtrar por: `LoginDiagnostics` ou `AuthViewModel`

### Passo 4: Verificar Resultado do Diagnóstico

O diagnóstico vai mostrar:
- ✅ Se o colaborador foi encontrado
- ✅ Se está aprovado e ativo
- ✅ Qual caminho foi usado (collectionGroup ou busca direta)
- ✅ Estrutura dos dados encontrados
- ✅ Erros específicos (se houver)

## O Que Procurar nos Logs

### Logs de Sucesso
```
[LoginDiagnostics] ✅ Colaborador encontrado
[LoginDiagnostics] Aprovado: true
[LoginDiagnostics] Ativo: true
[LOGIN_FLOW] ✅ Colaborador encontrado na nuvem
```

### Logs de Erro
```
[LoginDiagnostics] ❌ ERRO: permission-denied
[LOGIN_FLOW] ❌ Erro na busca collectionGroup: ...
[BUSCA_NUVEM] ❌ Colaborador não encontrado na nuvem
```

### Informações Importantes
- **Email usado**: Verificar se está correto
- **Firebase Auth**: Verificar se está autenticado ou não
- **Resultado da busca**: Quantos documentos foram encontrados
- **Campos presentes**: Verificar se `nivel_acesso` ou `nivelAcesso` está presente
- **Formato dos campos**: Verificar se está em camelCase ou snake_case

## Verificar Estrutura dos Dados no Firestore

1. Acessar Firebase Console: https://console.firebase.google.com/project/gestaobilhares/firestore
2. Navegar para: `empresas > empresa_001 > entidades > colaboradores > items`
3. Encontrar o documento do User
4. Verificar:
   - ✅ Campo `aprovado` = `true`
   - ✅ Campo `ativo` = `true`
   - ✅ Campo `email` = email do User
   - ✅ Campo `nivel_acesso` OU `nivelAcesso` presente
   - ✅ Campo `firebase_uid` OU `firebaseUid` (pode ser null se ainda não fez login online)

## Possíveis Problemas e Soluções

### Problema 1: "Colaborador não encontrado"
**Causa**: Query não está encontrando o documento
**Solução**: 
- Verificar se o email está correto (case-sensitive)
- Verificar se o documento existe no Firestore
- Verificar se as regras permitem a busca

### Problema 2: "Permission denied"
**Causa**: Regras do Firestore não permitem a busca
**Solução**:
- Verificar se o colaborador está `aprovado = true` e `ativo = true`
- Verificar se o documento tem o campo `nivel_acesso` ou `nivelAcesso`
- Verificar se as regras foram deployadas corretamente

### Problema 3: "Campos não encontrados"
**Causa**: Documento não tem os campos esperados
**Solução**:
- Verificar estrutura do documento no Firestore
- Ajustar regras para verificar campos alternativos
- Atualizar documento com campos faltantes

### Problema 4: "Formato de campo incorreto"
**Causa**: Documento usa camelCase mas regras esperam snake_case (ou vice-versa)
**Solução**:
- O código agora tenta ambos os formatos automaticamente
- Se ainda falhar, verificar logs para ver qual formato está sendo usado

## Próximos Passos Após o Teste

1. **Se o login funcionar**:
   - ✅ Problema resolvido!
   - Verificar se o fluxo de primeiro acesso está funcionando
   - Verificar se a mudança de senha está funcionando

2. **Se o login ainda não funcionar**:
   - 📋 Copiar logs completos do diagnóstico
   - 📋 Verificar estrutura exata do documento no Firestore
   - 📋 Comparar com o que as regras esperam
   - 📋 Ajustar código/regras conforme necessário

## Contato para Suporte

Se o problema persistir, fornecer:
1. Logs completos do diagnóstico (arquivo local ou Logcat)
2. Screenshot da estrutura do documento no Firestore
3. Email do User que está tentando fazer login
4. Mensagem de erro exata (se houver)
