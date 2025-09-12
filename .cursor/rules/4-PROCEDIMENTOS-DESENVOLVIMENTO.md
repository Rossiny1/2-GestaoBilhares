# 4. PROCEDIMENTOS DE DESENVOLVIMENTO

## 🚀 REGRAS FUNDAMENTAIS

### **Preservação do Progresso**

- **NUNCA** comprometer funcionalidades já implementadas
- **SEMPRE** verificar funcionalidades existentes antes de implementar
- **SEMPRE** fazer builds intermediários para validação
- **SEMPRE** trabalhar em paralelo para otimização

### **Responsabilidades do Usuário**

- **Builds**: Usuário executa todos os builds e geração de APK
- **Testes**: Usuário realiza testes manuais
- **Validação**: Usuário confirma funcionamento antes de prosseguir

## 🔧 COMANDOS E FERRAMENTAS

### **Comandos de Build (Auto-aprovados)**

```bash
gradlew tasks
gradlew clean
gradlew build
gradlew compileDebugKotlin
gradlew assembleDebug
```

### **Comandos de Sistema (Auto-aprovados)**

```bash
dir / ls
Get-ChildItem
tasklist
Select-String
```

### **Comandos de Desenvolvimento (Auto-aprovados)**

- Criar, editar, excluir arquivos `.kt`, `.xml`, `.gradle`
- Comentar/descomentar imports
- Remover dependências problemáticas
- Criar implementações mock
- Operações de limpeza de cache

## 🐛 RESOLUÇÃO DE PROBLEMAS

### **Build Failures**

1. **Diagnóstico**: Usar `--stacktrace` para identificar erros
2. **Limpeza**: `gradlew clean` antes de rebuild
3. **Recovery**: Parar daemons se necessário
4. **Validação**: Build intermediário após correções

### **Recovery de Daemon Kotlin**

```bash
./gradlew --stop
taskkill /f /im java.exe
./gradlew clean --no-daemon
```

### **Logs e Debug**

- **Logcat**: Usar caminho específico do ADB
- **Logs Detalhados**: Adicionar em componentes críticos
- **Análise**: Capturar logs durante testes

## 📱 TESTES E VALIDAÇÃO

### **Fluxo de Testes**

1. **Build**: Gerar APK de debug
2. **Instalação**: Transferir para dispositivo
3. **Teste Manual**: Validar funcionalidades
4. **Logs**: Capturar logs se necessário
5. **Correção**: Ajustar baseado nos resultados

### **Validações Críticas**

- **Login**: Autenticação funcionando
- **Navegação**: Fluxo entre telas
- **Dados**: Persistência no banco
- **Contratos**: Geração e assinatura
- **Relatórios**: PDF e impressão

## 🔄 METODOLOGIA DE TRABALHO

### **Abordagem Sistemática**

- **Análise Profunda**: Entender código existente
- **Implementação Incremental**: Pequenas mudanças
- **Validação Contínua**: Testes após cada alteração
- **Documentação**: Atualizar regras quando necessário

### **Comunicação**

- **Explicações Detalhadas**: Para desenvolvedor iniciante
- **Código Comentado**: Facilitar compreensão
- **Logs Claros**: Sem jargão técnico
- **Visualização**: Explicações fáceis de visualizar

## ⚠️ CUIDADOS ESPECIAIS

### **Evitar Loops**

- **Não repetir** verificações desnecessárias
- **Focar** no problema principal
- **Usar** ferramentas de diagnóstico adequadas

### **Preservar Funcionalidades**

- **Verificar** dependências antes de remover
- **Manter** compatibilidade com código existente
- **Testar** funcionalidades relacionadas

### **Eficiência**

- **Trabalhar em paralelo** quando possível
- **Usar** ferramentas apropriadas para cada tarefa
- **Otimizar** tempo de desenvolvimento
