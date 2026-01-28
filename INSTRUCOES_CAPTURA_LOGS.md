# 📱 INSTRUÇÕES PARA CAPTURA DE LOGS REAIS

## 🎯 **OBJETIVO**

Capturar logs EXATOS do app Android durante operações bloqueadas para identificar PERMISSION_DENIED reais.

## 📋 **PREPARO CONCLUÍDO**

✅ ADB limpo e pronto  
✅ Comando de captura preparado  
✅ Arquivo de saída configurado  

## 🚀 **EXECUÇÃO - PASSO A PASSO**

### **PASSO 1: Iniciar Captura de Logs**

Execute este comando (deixe rodando em background):

```powershell
& "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe" logcat -s FirebaseFirestore:D FirebaseAuth:D GestaoBilhares:D *:E > logs_app_real.txt
```

**OU** use o script pronto:
```powershell
.\scripts\iniciar_captura_logs.ps1
```

### **PASSO 2: Testar no App Android**

**⚠️ IMPORTANTE:** Use o app REAL, não scripts de Service Account!

#### **Teste 1: Criar Rota**
1. Abrir app Android
2. Login com: **rossipys@gmail.com** (super user)
3. Navegar: Configurações → Rotas
4. Clicar: "Adicionar Rota" (+)
5. Preencher:
   - Nome: "Rota Log Teste 1"
   - Ativa: SIM
6. Salvar
7. **AGUARDAR 5 segundos**

#### **Teste 2: Criar Cliente**
1. Navegar: Clientes
2. Clicar: "Adicionar Cliente" (+)
3. Preencher:
   - Nome: "Cliente Log Teste 1"
   - Rota: (selecionar existente)
   - Telefone: (11) 99999-9999
4. Salvar
5. **AGUARDAR 5 segundos**

#### **Teste 3: Criar Mesa**
1. Navegar: Mesas
2. Clicar: "Adicionar Mesa" (+)
3. Preencher:
   - Número: 999
   - Rota: (selecionar existente)
   - Status: Ativa
4. Salvar
5. **AGUARDAR 5 segundos**

#### **Teste 4: Criar Acerto**
1. Navegar: Acertos
2. Clicar: "Adicionar Acerto" (+)
3. Preencher:
   - Cliente: (selecionar existente)
   - Valor: R$ 100,00
   - Data: Hoje
4. Salvar
5. **AGUARDAR 5 segundos**

#### **Teste 5: Criar Despesa**
1. Navegar: Despesas
2. Clicar: "Adicionar Despesa" (+)
3. Preencher:
   - Descrição: "Despesa Teste Log"
   - Valor: R$ 50,00
   - Rota: (selecionar)
4. Salvar
5. **AGUARDAR 5 segundos**

### **PASSO 3: Parar Captura**

Após todos os testes, pare a captura:
- **PowerShell:** Pressione `Ctrl+C` no terminal
- **OU** feche a janela do terminal

### **PASSO 4: Verificar Logs**

Execute:
```powershell
# Ver logs capturados
Get-Content logs_app_real.txt | Select-String -Pattern "PERMISSION_DENIED|Missing|insufficient|Error" -Context 3

# Salvar erros específicos
Get-Content logs_app_real.txt | Select-String -Pattern "PERMISSION_DENIED|Missing|insufficient" -Context 5 > erros_permission_denied.txt
```

## 📊 **O QUE ESPERAR**

**Logs de ERRO esperados:**
```
FirebaseFirestore/D: PERMISSION_DENIED at /empresas/empresa_001/entidades/rotas/items/abc123
FirebaseFirestore/D: Missing or insufficient permissions
FirebaseFirestore/E: Firestore write failed: PERMISSION_DENIED
```

**Logs de SUCESSO (colaboradores):**
```
FirebaseFirestore/D: Document written at /empresas/empresa_001/colaboradores/uid123
```

## ⚠️ **CRÍTICO**

- **NÃO feche o app** durante os testes
- **AGUARDE 5 segundos** após cada operação
- **Execute UM teste por vez**
- **Mantenha o logcat rodando** durante todos os testes

## 🎯 **RESULTADO ESPERADO**

Ao final, teremos:
- ✅ `logs_app_real.txt` (logs completos)
- ✅ `erros_permission_denied.txt` (erros filtrados)
- ✅ Paths EXATOS que o app tenta acessar
- ✅ Mensagens PERMISSION_DENIED reais

---

**Próximo passo:** Analisar os logs capturados e corrigir as Security Rules baseado nos erros REAIS.
