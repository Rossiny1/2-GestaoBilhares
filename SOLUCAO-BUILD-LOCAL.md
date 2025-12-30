# 🔧 Solução: Build Passa na VM mas Falha Localmente

## 🔍 Problema Identificado

**Sintoma:** Build passa na VM, mas falha após merge do PR localmente.

**Causas Comuns:**
1. ❌ `local.properties` não existe localmente (está no .gitignore)
2. ❌ Java diferente ou não configurado
3. ❌ Android SDK não configurado
4. ❌ Dependências não baixadas
5. ❌ Cache do Gradle corrompido

---

## 🛠️ Solução Passo a Passo

### **PASSO 1: Executar Diagnóstico**

Execute o script de diagnóstico:

**Windows:**
```powershell
.\scripts\diagnostico-build-local.ps1
```

**Linux/Mac:**
```bash
bash scripts/diagnostico-build-local.sh
```

Isso vai mostrar **exatamente** o que está faltando.

---

### **PASSO 2: Criar local.properties (Mais Comum)**

O `local.properties` **NÃO** é commitado (está no .gitignore).

**Você precisa criar localmente:**

1. **Encontrar caminho do Android SDK:**
   - Se usa Android Studio: geralmente em `C:\Users\SeuUsuario\AppData\Local\Android\Sdk`
   - Ou verifique em: Android Studio → Settings → Appearance & Behavior → System Settings → Android SDK

2. **Criar arquivo `local.properties` na raiz do projeto:**
   ```properties
   sdk.dir=C:\\Users\\SeuUsuario\\AppData\\Local\\Android\\Sdk
   ```
   *(Ajuste o caminho para o seu sistema)*

3. **Verificar se funcionou:**
   ```powershell
   .\gradlew.bat compileDebugKotlin
   ```

---

### **PASSO 3: Verificar Java**

```powershell
# Verificar versão
java -version

# Deve ser Java 11 ou superior
```

**Se não tiver Java:**
- Instale Java 11 ou superior
- Ou configure `JAVA_HOME` no Windows

---

### **PASSO 4: Limpar Cache do Gradle**

```powershell
# Limpar cache
.\gradlew.bat clean

# Limpar cache do Gradle
Remove-Item -Recurse -Force .gradle -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force ~\.gradle\caches -ErrorAction SilentlyContinue

# Rebuild
.\gradlew.bat build
```

---

### **PASSO 5: Verificar Erros Específicos**

Execute o build e copie os erros:

```powershell
.\gradlew.bat compileDebugKotlin --console=plain 2>&1 | Select-String -Pattern "error:" | Select-Object -First 10
```

**Envie os erros** para eu corrigir.

---

## 📋 Checklist de Verificação

Execute e verifique cada item:

- [ ] `local.properties` existe e tem `sdk.dir` correto
- [ ] Java instalado (`java -version`)
- [ ] Android SDK instalado no caminho especificado
- [ ] `gradle.properties` existe (deve estar commitado)
- [ ] Cache do Gradle limpo
- [ ] Dependências baixadas (`.\gradlew.bat --refresh-dependencies`)

---

## 🎯 Solução Rápida (Mais Provável)

**99% dos casos é falta de `local.properties`:**

1. **Criar `local.properties`:**
   ```properties
   sdk.dir=C:\\caminho\\para\\seu\\android-sdk
   ```

2. **Testar:**
   ```powershell
   .\gradlew.bat compileDebugKotlin
   ```

---

## 📞 Se Ainda Não Funcionar

1. **Execute o diagnóstico:**
   ```powershell
   .\scripts\diagnostico-build-local.ps1
   ```

2. **Copie os erros do build:**
   ```powershell
   .\gradlew.bat compileDebugKotlin --console=plain 2>&1 | Out-File erros-build.txt
   ```

3. **Envie:**
   - Saída do diagnóstico
   - Erros do build
   - Conteúdo do `local.properties` (sem senhas)

---

## 💡 Dica

**Sempre que fizer merge de um PR:**
1. Verifique se `local.properties` existe
2. Execute `.\gradlew.bat clean` se necessário
3. Teste com `.\gradlew.bat compileDebugKotlin`

---

**Execute o diagnóstico primeiro e me mostre o resultado! 🔍**
