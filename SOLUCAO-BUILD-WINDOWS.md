# 🔧 Solução: Build Passa na VM mas Falha no Windows

## 🎯 Problema

Build passa na VM (Linux), mas falha no seu Windows local após merge do PR.

---

## ✅ Solução Rápida (Windows)

### **PASSO 1: Criar `local.properties`**

O arquivo `local.properties` **NÃO** é commitado (está no .gitignore).

**Você precisa criar na raiz do projeto:**

1. **Encontrar caminho do Android SDK no Windows:**
   - Abra Android Studio
   - Vá em: **File → Settings → Appearance & Behavior → System Settings → Android SDK**
   - Copie o caminho mostrado (geralmente: `C:\Users\SeuUsuario\AppData\Local\Android\Sdk`)

2. **Criar arquivo `local.properties` na raiz do projeto:**
   ```properties
   sdk.dir=C:\\Users\\SeuUsuario\\AppData\\Local\\Android\\Sdk
   ```
   
   **⚠️ IMPORTANTE:** Use `\\` (duas barras) no Windows!

3. **Salvar o arquivo** na mesma pasta onde está `gradlew.bat`

---

### **PASSO 2: Verificar Java**

```powershell
# Verificar se Java está instalado
java -version
```

**Deve mostrar Java 11 ou superior.**

**Se não tiver:**
- Instale Java 11+ do site oficial
- Ou use o Java que vem com Android Studio

---

### **PASSO 3: Executar Diagnóstico**

```powershell
# Execute o diagnóstico
.\scripts\diagnostico-build-local.ps1
```

Isso vai mostrar **exatamente** o que está faltando.

---

### **PASSO 4: Testar Build**

```powershell
# Limpar cache primeiro
.\gradlew.bat clean

# Testar compilação
.\gradlew.bat compileDebugKotlin
```

---

## 📋 Checklist Windows

Execute e verifique:

- [ ] **`local.properties` existe?**
  ```powershell
  Test-Path local.properties
  ```
  Deve retornar `True`

- [ ] **Caminho do SDK está correto?**
  ```powershell
  Get-Content local.properties
  ```
  Deve mostrar algo como: `sdk.dir=C:\\Users\\...\\Android\\Sdk`

- [ ] **Java instalado?**
  ```powershell
  java -version
  ```

- [ ] **Android SDK existe no caminho?**
  ```powershell
  $sdkPath = (Get-Content local.properties | Select-String "sdk.dir").ToString().Split("=")[1]
  Test-Path $sdkPath
  ```

---

## 🐛 Problemas Comuns no Windows

### **1. Erro: "SDK location not found"**

**Solução:**
- Verifique se `local.properties` existe
- Verifique se o caminho está correto (use `\\` no Windows)
- Verifique se o Android SDK está instalado nesse caminho

### **2. Erro: "Java not found"**

**Solução:**
```powershell
# Verificar JAVA_HOME
$env:JAVA_HOME

# Se vazio, configurar (ajuste o caminho):
$env:JAVA_HOME = "C:\Program Files\Java\jdk-11"
```

### **3. Erro: "Gradle daemon failed"**

**Solução:**
```powershell
# Parar daemons
.\gradlew.bat --stop

# Limpar cache
Remove-Item -Recurse -Force .gradle -ErrorAction SilentlyContinue
```

### **4. Erro: "Path too long" (Windows)**

**Solução:**
- Ativar suporte a caminhos longos no Windows
- Ou mover projeto para caminho mais curto (ex: `C:\dev\projeto`)

---

## 🎯 Comandos Rápidos Windows

```powershell
# 1. Criar local.properties (ajuste o caminho)
@"
sdk.dir=C:\\Users\\SeuUsuario\\AppData\\Local\\Android\\Sdk
"@ | Out-File -FilePath local.properties -Encoding UTF8

# 2. Verificar se foi criado
Get-Content local.properties

# 3. Testar build
.\gradlew.bat compileDebugKotlin

# 4. Se falhar, ver erros
.\gradlew.bat compileDebugKotlin --console=plain 2>&1 | Select-String -Pattern "error:" | Select-Object -First 10
```

---

## 📞 Se Ainda Não Funcionar

1. **Execute diagnóstico completo:**
   ```powershell
   .\scripts\diagnostico-build-local.ps1
   ```

2. **Copie os erros:**
   ```powershell
   .\gradlew.bat compileDebugKotlin --console=plain 2>&1 | Out-File erros-build.txt
   ```

3. **Me envie:**
   - Saída do `diagnostico-build-local.ps1`
   - Conteúdo de `erros-build.txt`
   - Conteúdo do seu `local.properties` (sem senhas)

---

## 💡 Dica Importante

**Sempre que fizer merge de um PR no Windows:**

1. ✅ Verificar se `local.properties` existe
2. ✅ Verificar se caminho do SDK está correto (Windows usa `\\`)
3. ✅ Executar `.\gradlew.bat clean` se necessário
4. ✅ Testar com `.\gradlew.bat compileDebugKotlin`

---

## 🚀 Solução Mais Provável (99% dos casos)

**Criar `local.properties` com caminho correto do Windows:**

```properties
sdk.dir=C:\\Users\\SeuUsuario\\AppData\\Local\\Android\\Sdk
```

**Lembre-se:**
- Use `\\` (duas barras) no Windows
- Ajuste `SeuUsuario` para seu usuário
- Verifique se o caminho existe

---

**Execute o diagnóstico e me mostre o resultado! 🔍**
