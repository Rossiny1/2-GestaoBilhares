# 🔧 Configuração e Uso do MCP Crashlytics - GestaoBilhares

## ✅ Status: Configurado e Funcional

O servidor MCP do Firebase Crashlytics foi configurado com sucesso e está pronto para monitoramento de erros em tempo real.

## 📋 Configuração Aplicada

**Arquivo:** `C:\Users\Rossiny\.cursor\mcp.json`

```json
{
  "mcpServers": {
    "firebase-mcp-server": {
      "command": "npx",
      "args": ["-y", "firebase-tools@latest", "mcp"],
      "env": {
        "FIREBASE_PROJECT_ID": "gestaobilhares"
      }
    }
  }
}
```

## 🔍 Verificações Realizadas

1. ✅ **Firebase CLI autenticado**: `rossinys@gmail.com`
2. ✅ **Projeto Firebase**: `gestaobilhares` (ativo)
3. ✅ **Node.js instalado**: v24.11.1
4. ✅ **npm instalado**: 11.6.2

## 🛠️ Ferramentas Disponíveis no MCP

Com o MCP configurado, o assistente de IA pode acessar diretamente os dados do Crashlytics:

### 📊 Consultas de Problemas
- **`crashlytics_get_issue`** - Buscar dados detalhados de um problema específico (stack trace, ocorrências, etc.)
- **`crashlytics_get_top_issues`** - Listar os problemas mais críticos (ordenados por número de eventos)
- **`crashlytics_get_top_variants`** - Ver variantes de problemas (agrupados por stack trace similar)

### 📱 Análise por Dispositivo e Versão
- **`crashlytics_get_top_versions`** - Ver problemas agrupados por versão do app
- **`crashlytics_get_top_android_devices`** - Ver problemas por dispositivo Android específico
- **`crashlytics_get_top_apple_devices`** - Ver problemas por dispositivo iOS (se aplicável)

### 📝 Eventos e Logs
- **`crashlytics_list_events`** - Listar eventos recentes de crashes/exceções com filtros avançados
- **`crashlytics_batch_get_events`** - Buscar eventos específicos por resource name (stack traces completos)

### 📌 Anotações
- **`crashlytics_list_notes`** - Listar observações/notas adicionadas aos problemas
- **`crashlytics_create_note`** - Adicionar nota a um problema
- **`crashlytics_delete_note`** - Remover nota de um problema

### 🔄 Gerenciamento
- **`crashlytics_update_issue`** - Atualizar estado do problema (OPEN, CLOSED, MUTED)

## 🔄 Como Usar o MCP Crashlytics

### 1. Verificação Inicial

Após reiniciar o Cursor, o servidor MCP deve aparecer como ativo. Você pode verificar em:
- **Cursor Settings** → **Tools** → **Installed MCP Servers**
- Procure por `firebase-mcp-server` na lista

### 2. Monitoramento de Erros em Tempo Real

O assistente de IA pode acessar dados do Crashlytics automaticamente. Exemplos de comandos:

#### 📊 Consultar Problemas Críticos
```
"Quais são os 10 problemas mais críticos no Crashlytics?"
"Mostre os crashes mais frequentes nas últimas 24 horas"
"Quais versões do app têm mais problemas?"
```

#### 🔍 Analisar um Problema Específico
```
"Analise o problema [ISSUE_ID] no Crashlytics"
"Mostre o stack trace completo do problema [ISSUE_ID]"
"Quais dispositivos são afetados pelo problema [ISSUE_ID]?"
```

#### 📱 Análise por Dispositivo
```
"Quais dispositivos Android têm mais crashes?"
"Mostre problemas específicos do Samsung Galaxy S21"
```

#### ⏰ Análise Temporal
```
"Liste os eventos de crash das últimas 7 dias"
"Mostre crashes ocorridos entre [DATA_INICIO] e [DATA_FIM]"
```

### 3. Integração com o Código

O projeto já está configurado para enviar logs ao Crashlytics:

#### ✅ Configuração Atual
- **CrashlyticsTree**: Implementado em `app/src/main/java/com/example/gestaobilhares/CrashlyticsTree.kt`
- **Timber**: Configurado para usar CrashlyticsTree em produção
- **Logs**: ERROR, WARN e INFO são enviados automaticamente

#### 📝 Exemplo de Uso no Código
```kotlin
import timber.log.Timber

// Log de erro (enviado ao Crashlytics)
try {
    // código que pode falhar
} catch (e: Exception) {
    Timber.e(e, "Erro ao processar dados do cliente")
    // Exceção é automaticamente registrada no Crashlytics
}

// Log de aviso (enviado ao Crashlytics)
if (valor < 0) {
    Timber.w("Valor negativo detectado: $valor")
}

// Log informativo (enviado ao Crashlytics em produção)
Timber.i("Sincronização iniciada para rota: $rotaId")
```

#### 🔑 Chaves Customizadas
Você pode adicionar contexto adicional aos crashes:

```kotlin
import com.google.firebase.crashlytics.FirebaseCrashlytics

val crashlytics = FirebaseCrashlytics.getInstance()

// Adicionar contexto do usuário
crashlytics.setUserId(userId)
crashlytics.setCustomKey("empresa_id", empresaId)
crashlytics.setCustomKey("rota_id", rotaId)
crashlytics.setCustomKey("versao_app", BuildConfig.VERSION_NAME)
```

### 4. Workflow de Monitoramento

#### 🔴 Quando um Crash Ocorre
1. **Crashlytics agrupa automaticamente** crashes similares em "Issues"
2. **MCP permite consultar** os problemas mais críticos
3. **Stack traces completos** estão disponíveis para análise

#### 📋 Processo de Debugging
1. **Identificar o problema**: Use `crashlytics_get_top_issues` para ver os mais críticos
2. **Analisar detalhes**: Use `crashlytics_get_issue` com o `issueId` específico
3. **Ver eventos**: Use `crashlytics_list_events` para ver crashes individuais
4. **Adicionar contexto**: Use `crashlytics_create_note` para documentar investigação
5. **Marcar como resolvido**: Use `crashlytics_update_issue` para fechar o problema

### 5. Filtros Avançados

O MCP suporta filtros avançados para análise precisa:

#### Filtros Disponíveis
- **Por versão**: `versionDisplayNames: ["1.0.0"]`
- **Por tipo de erro**: `issueErrorTypes: ["FATAL", "NON_FATAL", "ANR"]`
- **Por sinal**: `issueSignals: ["SIGNAL_EARLY", "SIGNAL_FRESH", "SIGNAL_REGRESSED"]`
- **Por dispositivo**: `deviceDisplayNames: ["Samsung Galaxy S21"]`
- **Por intervalo de tempo**: `intervalStartTime` e `intervalEndTime` (ISO 8601)

#### Exemplo de Consulta com Filtros
```
"Mostre crashes FATAL da versão 1.0.0 nas últimas 7 dias"
"Liste problemas novos (SIGNAL_FRESH) em dispositivos Samsung"
```

## 📊 Exemplos Práticos de Monitoramento

### Exemplo 1: Análise Diária de Crashes
```
"Quais são os 5 problemas mais críticos do Crashlytics hoje?"
```
O assistente irá:
1. Consultar `crashlytics_get_top_issues` com filtro de hoje
2. Retornar lista ordenada por número de eventos
3. Incluir informações sobre versões e dispositivos afetados

### Exemplo 2: Investigação de Problema Específico
```
"Analise o problema abc123def456 no Crashlytics e me mostre:
- Stack trace completo
- Dispositivos afetados
- Versões do app com o problema
- Eventos recentes"
```
O assistente irá:
1. Buscar detalhes com `crashlytics_get_issue`
2. Listar eventos com `crashlytics_list_events`
3. Analisar padrões e sugerir correções

### Exemplo 3: Análise de Regressão
```
"Mostre problemas que apareceram nas últimas 24 horas (SIGNAL_FRESH)"
```
O assistente irá:
1. Filtrar por `issueSignals: ["SIGNAL_FRESH"]`
2. Filtrar por intervalo de tempo (últimas 24h)
3. Identificar novos problemas que precisam atenção

### Exemplo 4: Análise por Versão
```
"Compare os crashes da versão 1.0.0 com a versão anterior"
```
O assistente irá:
1. Consultar `crashlytics_get_top_versions` para ambas versões
2. Comparar estatísticas
3. Identificar problemas novos ou resolvidos

## ⚠️ Solução de Problemas

### Se o MCP não estiver funcionando:

1. **Verificar autenticação Firebase:**
   ```powershell
   firebase login:list
   ```
   Se não estiver autenticado:
   ```powershell
   firebase login
   ```

2. **Verificar projeto ativo:**
   ```powershell
   firebase use gestaobilhares
   firebase projects:list
   ```

3. **Verificar configuração do MCP:**
   - Arquivo: `C:\Users\Rossiny\.cursor\mcp.json`
   - Verificar se `FIREBASE_PROJECT_ID` está correto: `"gestaobilhares"`

4. **Testar servidor MCP manualmente:**
   ```powershell
   npx -y firebase-tools@latest mcp
   ```

5. **Verificar logs do Cursor:**
   - Abra o Cursor Settings → Tools → Installed MCP Servers
   - Clique em "Show Output" para ver os logs de erro
   - Procure por erros de autenticação ou conexão

6. **Reiniciar o Cursor:**
   - Feche completamente o Cursor
   - Abra novamente para recarregar a configuração do MCP

### Erros Comuns

#### ❌ "Firebase project not found"
- Verificar se o projeto `gestaobilhares` existe no Firebase Console
- Verificar se você tem permissões no projeto

#### ❌ "Authentication required"
- Executar `firebase login` novamente
- Verificar se o email `rossinys@gmail.com` tem acesso ao projeto

#### ❌ "MCP server not responding"
- Verificar se Node.js está instalado: `node --version`
- Verificar se npm está funcionando: `npm --version`
- Tentar reinstalar: `npm install -g firebase-tools`

## 🎯 Melhores Práticas

### 1. Monitoramento Proativo
- **Diariamente**: Consultar os 10 problemas mais críticos
- **Semanalmente**: Analisar tendências e regressões
- **Após cada release**: Verificar novos problemas (SIGNAL_FRESH)

### 2. Contexto nos Logs
Sempre adicione contexto relevante aos logs:
```kotlin
// ✅ BOM: Contexto completo
Timber.e(e, "Erro ao salvar acerto. Cliente: $clienteId, Mesa: $mesaId")

// ❌ RUIM: Sem contexto
Timber.e(e, "Erro ao salvar")
```

### 3. Chaves Customizadas Estratégicas
Use chaves customizadas para facilitar análise:
```kotlin
crashlytics.setCustomKey("tela_atual", "SettlementFragment")
crashlytics.setCustomKey("acao_usuario", "calcular_acerto")
crashlytics.setCustomKey("dados_entrada", jsonString)
```

### 4. Não Expor Dados Sensíveis
⚠️ **NUNCA** logue dados sensíveis:
```kotlin
// ❌ ERRADO: Expor CPF
Timber.e("Erro ao processar cliente: $cpf")

// ✅ CORRETO: Usar ID
Timber.e("Erro ao processar cliente: $clienteId")
```

### 5. Agrupamento de Problemas
O Crashlytics agrupa automaticamente crashes similares. Para facilitar:
- Use mensagens de erro consistentes
- Adicione contexto via chaves customizadas
- Não inclua valores dinâmicos na mensagem principal

## 📚 Referências

- [Documentação Firebase MCP](https://firebase.google.com/docs/crashlytics/ai-assistance-mcp)
- [Firebase CLI Documentation](https://firebase.google.com/docs/cli)
- [Crashlytics Android SDK](https://firebase.google.com/docs/crashlytics/get-started?platform=android)
- [Console Firebase Crashlytics](https://console.firebase.google.com/project/gestaobilhares/crashlytics)

## 🔗 Links Úteis

- **Console Crashlytics**: https://console.firebase.google.com/project/gestaobilhares/crashlytics
- **Firebase Console**: https://console.firebase.google.com/project/gestaobilhares
- **Performance Monitoring**: https://console.firebase.google.com/project/gestaobilhares/performance

