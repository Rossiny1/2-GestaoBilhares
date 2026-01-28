# 🚱 EXECUTAR TESTES DO APP REAL - INSTRUÇÕES COMPLETAS

## 📋 **SITUAÇÃO ATUAL**

❌ **PROBLEMA CRÍTICO:** App Android real NÃO sincroniza com Firestore  
✅ **Testes Service Account:** Passaram (mas IGNORAM Security Rules)  
❌ **App Real:** Bloqueado por PERMISSION_DENIED  

**Causa:** Security Rules incompatíveis com paths/fields do app real.

---

## 🎯 **OBJETIVO**

Capturar logs REAIS do app durante operações bloqueadas para corrigir Security Rules baseado em erros EXATOS.

---

## 📱 **PASSO 1: PREPARAR DISPOSITIVO**

### **Conectar o Android:**
1. Conectar o celular via USB
2. Habilitar "Depuração USB" nas opções do desenvolvedor
3. Autorizar o computador no celular

### **Verificar conexão:**
```powershell
& "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe" devices
```

**Resultado esperado:**
```
List of devices attached
XXXXXXXXXXXX    device
```

---

## 📡 **PASSO 2: INICIAR CAPTURA DE LOGS**

### **Opção A: Script Automático**
```powershell
.\scripts\iniciar_captura_logs.ps1
```

### **Opção B: Manual**
```powershell
& "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe" logcat -c
& "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe" logcat -s FirebaseFirestore:D FirebaseAuth:D GestaoBilhares:D *:E > logs_app_real.txt
```

**⚠️ IMPORTANTE:** Mantenha o terminal aberto durante todos os testes!

---

## 🧪 **PASSO 3: EXECUTAR TESTES NO APP**

**⚠️ CRÍTICO:** Execute os testes EXATAMENTE na ordem abaixo!

### **Teste 1: Criar Rota**
1. Abrir app **Gestão Bilhares** no celular
2. Login com: **rossipys@gmail.com**
3. Navegar: **Configurações → Rotas**
4. Clicar: **"Adicionar Rota" (+)**
5. Preencher:
   - Nome: **"Rota Log Teste 1"**
   - Ativa: **SIM**
6. Clicar: **"Salvar"**
7. **AGUARDAR 5 segundos** (contar: 1001, 1002, 1003, 1004, 1005)
8. **Observar:** Provavelmente mostrará erro ou não salvará

### **Teste 2: Criar Cliente**
1. Navegar: **Clientes**
2. Clicar: **"Adicionar Cliente" (+)**
3. Preencher:
   - Nome: **"Cliente Log Teste 1"**
   - Rota: (selecionar primeira rota disponível)
   - Telefone: **(11) 99999-9999**
4. Clicar: **"Salvar"**
5. **AGUARDAR 5 segundos**

### **Teste 3: Criar Mesa**
1. Navegar: **Mesas**
2. Clicar: **"Adicionar Mesa" (+)**
3. Preencher:
   - Número: **999**
   - Rota: (selecionar primeira rota)
   - Status: **Ativa**
4. Clicar: **"Salvar"**
5. **AGUARDAR 5 segundos**

### **Teste 4: Criar Acerto**
1. Navegar: **Acertos**
2. Clicar: **"Adicionar Acerto" (+)**
3. Preencher:
   - Cliente: (selecionar "Cliente Log Teste 1")
   - Valor: **R$ 100,00**
   - Data: **Hoje**
4. Clicar: **"Salvar"**
5. **AGUARDAR 5 segundos**

### **Teste 5: Criar Despesa**
1. Navegar: **Despesas**
2. Clicar: **"Adicionar Despesa" (+)**
3. Preencher:
   - Descrição: **"Despesa Teste Log"**
   - Valor: **R$ 50,00**
   - Rota: (selecionar primeira rota)
4. Clicar: **"Salvar"**
5. **AGUARDAR 5 segundos**

---

## 🛑 **PASSO 4: PARAR CAPTURA**

Após TODOS os 5 testes:

1. **Parar logcat:** Pressione `Ctrl+C` no terminal
2. **Verificar arquivo:** `logs_app_real.txt` deve ter conteúdo

---

## 🔍 **PASSO 5: ANALISAR LOGS**

### **Executar análise:**
```powershell
.\scripts\analisar_logs_app.ps1
```

### **Verificar resultados:**
- `erros_permission_denied.txt` - Erros filtrados
- `document_paths.txt` - Paths tentados
- Análise no terminal

### **Logs esperados:**
```
❌ PERMISSION_DENIED at /empresas/empresa_001/entidades/rotas/items/abc123
❌ Missing or insufficient permissions
❌ Firestore write failed: PERMISSION_DENIED
```

---

## 📊 **PASSO 6: COMPARTILHAR RESULTADOS**

### **Arquivos para enviar:**
1. `logs_app_real.txt` (logs completos)
2. `erros_permission_denied.txt` (erros filtrados)
3. `document_paths.txt` (paths tentados)

### **Informações adicionais:**
- Qual teste falhou? (todos devem falhar exceto colaboradores)
- Alguma operação funcionou?
- Screenshots dos erros no app (se possível)

---

## ⚠️ **CRÍTICO - NÃO ESQUECER**

- ✅ **MANTER logcat rodando** durante todos os testes
- ✅ **AGUARDAR 5 segundos** após cada operação
- ✅ **USAR app REAL** (não emulador se possível)
- ✅ **LOGIN com rossipys@gmail.com** (super user)
- ✅ **EXECUTAR todos os 5 testes** na ordem
- ✅ **PARAR captura** apenas após o último teste

---

## 🎯 **RESULTADO ESPERADO**

Ao final, teremos:
- ✅ **Logs REAIS** com PERMISSION_DENIED exatos
- ✅ **Paths EXATOS** que o app tenta acessar
- ✅ **Base para corrigir** Security Rules adequadamente
- ✅ **Evidências concretas** do problema

---

**Próximo passo:** Com os logs capturados, vou analisar os erros PERMISSION_DENIED e corrigir as Security Rules para fazer o app REAL funcionar! 🚀
