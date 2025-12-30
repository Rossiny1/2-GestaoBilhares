# 📋 Passo a Passo - Importar Mudanças no Windows

## 🎯 Objetivo
Importar todas as correções e otimizações feitas na VM para seu ambiente local Windows.

---

## ✅ Pré-requisitos

Antes de começar, verifique:

- [ ] Git instalado e configurado
- [ ] PowerShell ou Git Bash disponível
- [ ] Você está na branch correta localmente
- [ ] Conexão com internet funcionando

---

## 📝 PASSO A PASSO COMPLETO

### **PASSO 1: Abrir Terminal no Projeto**

1. Abra o **PowerShell** ou **Git Bash**
2. Navegue até a pasta do projeto:
   ```powershell
   cd C:\caminho\para\seu\projeto
   ```
   *(Substitua pelo caminho real do seu projeto)*

3. Verifique se está na pasta correta:
   ```powershell
   # Deve mostrar a pasta .git
   Test-Path .git
   
   # Deve mostrar gradlew.bat
   Test-Path gradlew.bat
   ```

---

### **PASSO 2: Verificar Branch Atual**

```powershell
# Ver qual branch você está
git branch --show-current

# Se não estiver na branch correta, mude:
git checkout cursor/cursor-build-failure-fix-efaf
```

**Se a branch não existir localmente:**
```powershell
# Buscar todas as branches remotas
git fetch origin

# Criar e mudar para a branch
git checkout -b cursor/cursor-build-failure-fix-efaf origin/cursor/cursor-build-failure-fix-efaf
```

---

### **PASSO 3: Sincronizar com GitHub**

#### **Opção A: Script Automático (Recomendado)**

```powershell
# Execute o script de sincronização
.\scripts\sync-all-changes.ps1
```

Este script faz **tudo automaticamente**:
- ✅ Busca mudanças do GitHub
- ✅ Faz pull das atualizações
- ✅ Commit mudanças locais (se houver)
- ✅ Push commits locais (se houver)

#### **Opção B: Manual**

```powershell
# 1. Buscar mudanças do GitHub
git fetch origin

# 2. Ver o que mudou
git log HEAD..origin/cursor/cursor-build-failure-fix-efaf --oneline

# 3. Fazer pull das mudanças
git pull origin cursor/cursor-build-failure-fix-efaf

# 4. Verificar status
git status
```

---

### **PASSO 4: Verificar Mudanças Importadas**

```powershell
# Ver últimos commits importados
git log --oneline -5

# Verificar arquivos modificados
git diff HEAD~5 --stat
```

**Você deve ver commits como:**
- `perf(VM): Otimizações críticas para evitar travamentos`
- `fix(Windows): Melhora scripts PowerShell...`
- `docs: Adiciona resumo completo das correções...`
- `Windows: Adiciona scripts PowerShell e Batch...`
- `feat: Add build and install automation scripts`
- `Refactor: Use Long for dates and System.currentTimeMillis()`

---

### **PASSO 5: Verificar Arquivos Importados**

```powershell
# Verificar se os scripts existem
Test-Path scripts\sync-all-changes.ps1
Test-Path scripts\auto-install-debug.ps1
Test-Path scripts\watch-and-install.ps1
Test-Path scripts\otimizar-vm.sh
Test-Path scripts\limpar-daemons.sh

# Verificar documentação
Test-Path RESUMO-CORRECOES.md
Test-Path OTIMIZACAO-VM.md
Test-Path README-AUTOMACAO-WINDOWS.md
```

**Todos devem retornar `True`**

---

### **PASSO 6: Aplicar Otimizações do Gradle**

As otimizações em `gradle.properties` já foram importadas automaticamente!

**Verificar se as configurações estão corretas:**
```powershell
# Ver configurações de memória
Select-String -Path gradle.properties -Pattern "org.gradle.jvmargs"
Select-String -Path gradle.properties -Pattern "kotlin.daemon.jvmargs"
```

**Você deve ver:**
- `org.gradle.jvmargs=-Xmx4g` (não mais 8g)
- `kotlin.daemon.jvmargs=-Xmx3g` (não mais 6g)

---

### **PASSO 7: Limpar Daemons Antigos (Opcional mas Recomendado)**

```powershell
# Parar daemons Gradle antigos para usar novas configurações
.\gradlew.bat --stop
```

Isso garante que os próximos builds usem as novas configurações (menos memória).

---

### **PASSO 8: Testar Build (Opcional)**

```powershell
# Testar se o build funciona com as novas configurações
.\gradlew.bat assembleDebug
```

**Ou apenas compilar:**
```powershell
.\gradlew.bat compileDebugKotlin
```

---

## 🎉 PRONTO!

Agora você tem todas as mudanças importadas:

✅ **Correções de Build** (119+ erros corrigidos)  
✅ **Scripts de Automação** (PowerShell e Batch)  
✅ **Otimizações de Performance** (50% menos memória)  
✅ **Documentação Completa**

---

## 📱 Próximos Passos (Opcional)

### **Instalar App no Celular:**

1. **Conectar celular via USB**
2. **Ativar Depuração USB** no celular
3. **Verificar se está conectado:**
   ```powershell
   adb devices
   ```

4. **Instalar app:**
   ```powershell
   # Instalação sob demanda
   .\scripts\auto-install-debug.ps1
   
   # Ou monitoramento contínuo (deixe rodando)
   .\scripts\watch-and-install.ps1
   ```

---

## 🐛 Troubleshooting

### **Erro: "execution of scripts is disabled"**

```powershell
# Execute no PowerShell como Administrador:
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### **Erro: "Não é um repositório Git"**

Certifique-se de estar na **raiz do projeto** (pasta com `.git`):
```powershell
cd C:\caminho\correto\do\projeto
Test-Path .git  # Deve retornar True
```

### **Erro: "Branch não encontrada"**

```powershell
# Buscar branches remotas
git fetch origin

# Ver todas as branches disponíveis
git branch -a

# Criar branch local a partir da remota
git checkout -b cursor/cursor-build-failure-fix-efaf origin/cursor/cursor-build-failure-fix-efaf
```

### **Erro: "Conflitos de merge"**

```powershell
# Ver conflitos
git status

# Se houver conflitos, você pode:
# Opção 1: Aceitar mudanças remotas (cuidado - perde mudanças locais)
git reset --hard origin/cursor/cursor-build-failure-fix-efaf

# Opção 2: Fazer merge manual
git merge origin/cursor/cursor-build-failure-fix-efaf
# (Depois resolver conflitos manualmente)
```

### **Erro: "git pull falhou"**

```powershell
# Tentar novamente com mais informações
git pull origin cursor/cursor-build-failure-fix-efaf --verbose

# Ou fazer fetch + merge manual
git fetch origin
git merge origin/cursor/cursor-build-failure-fix-efaf
```

---

## 📊 Checklist Final

Após seguir os passos, verifique:

- [ ] Branch correta: `cursor/cursor-build-failure-fix-efaf`
- [ ] Último commit: `perf(VM): Otimizações críticas...`
- [ ] Scripts PowerShell existem em `scripts\`
- [ ] `gradle.properties` tem `-Xmx4g` e `-Xmx3g`
- [ ] Build funciona (opcional, mas recomendado)

---

## 💡 Dicas

1. **Use o script de sincronização** (`sync-all-changes.ps1`) sempre que quiser atualizar
2. **Monitore o uso de memória** durante builds grandes
3. **Limpe daemons** periodicamente se a VM ficar lenta
4. **Consulte a documentação** em `OTIMIZACAO-VM.md` e `README-AUTOMACAO-WINDOWS.md`

---

## 📞 Precisa de Ajuda?

Se algo não funcionar:
1. Copie a mensagem de erro completa
2. Verifique em qual passo você está
3. Consulte a seção de Troubleshooting acima

---

**Boa sorte! 🚀**
