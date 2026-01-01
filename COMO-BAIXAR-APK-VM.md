# 📥 Como Baixar APK da VM para Instalar no Celular

## 🎯 Objetivo

Baixar o APK gerado na VM e instalar no seu celular via Cursor/Windows.

---

## 📋 Métodos Disponíveis

### **Método 1: Via Cursor (Mais Fácil) ⭐**

#### **Passo a Passo:**

1. **Gerar APK na VM:**
   - Eu executo: `./gradlew assembleDebug`
   - APK é gerado em: `app/build/outputs/apk/debug/app-debug.apk`

2. **No Cursor, abra o arquivo:**
   - Navegue até: `app/build/outputs/apk/debug/app-debug.apk`
   - Clique com botão direito no arquivo
   - Selecione: **"Download"** ou **"Save As"**

3. **Ou use o terminal do Cursor:**
   - `Ctrl+`` (abrir terminal)
   - Execute:
   ```powershell
   # Se estiver conectado via SSH/Remote
   scp usuario@vm:/workspace/app/build/outputs/apk/debug/app-debug.apk ./
   ```

4. **Instalar no celular:**
   - Transfira o APK para o celular (USB, email, etc.)
   - No celular, ative "Instalar apps de fontes desconhecidas"
   - Toque no APK para instalar

---

### **Método 2: Via GitHub (Recomendado)**

#### **Passo a Passo:**

1. **Eu faço commit do APK (temporário):**
   - Gero o APK
   - Faço commit e push
   - Você faz pull

2. **Você baixa localmente:**
   ```powershell
   git pull origin cursor/cursor-build-failure-fix-efaf
   ```

3. **APK estará em:**
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Instalar:**
   - Conecte celular via USB
   - Execute: `adb install app/build/outputs/apk/debug/app-debug.apk`

---

### **Método 3: Via Script Automático**

#### **Criar script para baixar APK:**

```powershell
# scripts/baixar-apk-vm.ps1
# (Precisa configurar conexão SSH com a VM)
```

---

## 🚀 Solução Mais Prática

### **Opção A: Gerar APK Localmente (Mais Rápido)**

Se o build funcionar localmente:

```powershell
# Gerar APK
.\gradlew.bat assembleDebug

# APK será gerado em:
# app\build\outputs\apk\debug\app-debug.apk

# Instalar diretamente
adb install app\build\outputs\apk\debug\app-debug.apk
```

---

### **Opção B: Eu Gero e Você Baixa**

1. **Eu gero o APK na VM:**
   ```bash
   ./gradlew assembleDebug
   ```

2. **Eu faço commit temporário:**
   ```bash
   git add app/build/outputs/apk/debug/app-debug.apk
   git commit -m "temp: APK para download"
   git push
   ```

3. **Você baixa:**
   ```powershell
   git pull
   # APK estará em: app\build\outputs\apk\debug\app-debug.apk
   ```

4. **Instalar:**
   ```powershell
   adb install app\build\outputs\apk\debug\app-debug.apk
   ```

---

## 📱 Instalar APK no Celular

### **Via ADB (Recomendado):**

```powershell
# 1. Conectar celular via USB
adb devices

# 2. Instalar APK
adb install app\build\outputs\apk\debug\app-debug.apk

# 3. Se já tiver instalado, usar -r para reinstalar
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### **Via Transferência Manual:**

1. Copie `app-debug.apk` para o celular (USB, email, etc.)
2. No celular, ative "Instalar apps de fontes desconhecidas"
3. Toque no APK para instalar

---

## 💡 Recomendação

**Melhor opção:** Gerar APK localmente (se build funcionar):

```powershell
.\gradlew.bat assembleDebug
adb install app\build\outputs\apk\debug\app-debug.apk
```

**Se build não funcionar localmente:** Eu gero na VM e você baixa via git pull.

---

## 🔧 Script Automático (Futuro)

Posso criar um script que:
1. Gera APK na VM
2. Faz commit temporário
3. Você faz pull
4. Instala automaticamente

**Quer que eu crie esse script?**

---

**Qual método você prefere? 🚀**
