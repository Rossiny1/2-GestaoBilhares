# 📱 Como Baixar o APK da VM Agora

## ⚠️ Situação Atual

O APK foi gerado com sucesso na **VM (ambiente remoto)**, mas você está vendo a pasta vazia **localmente no Windows** porque:

- ✅ O build roda na VM (remoto)
- ✅ O APK é gerado na VM
- ❌ O APK **NÃO** está sincronizado com seu computador local

---

## 🚀 Soluções para Baixar o APK

### **Método 1: Via Cursor Explorer (Mais Fácil)**

1. **No Cursor**, abra o **Explorer** (painel lateral esquerdo)
2. **Navegue até**: `app/build/outputs/apk/debug/`
3. **Clique com botão direito** no arquivo `app-debug.apk`
4. **Selecione**: `Download` ou `Save As...`
5. **Escolha** onde salvar no seu computador

**Se não aparecer o arquivo:**
- O Cursor pode não estar mostrando arquivos da VM
- Use o Método 2 ou 3 abaixo

---

### **Método 2: Via Git (Commit Temporário)**

1. **Na VM**, o APK já está gerado em: `app/build/outputs/apk/debug/app-debug.apk`

2. **Commit e push** (temporário):
   ```bash
   git add app/build/outputs/apk/debug/app-debug.apk
   git commit -m "APK debug para download"
   git push
   ```

3. **No seu computador local**:
   ```bash
   git pull
   ```

4. **Baixe o APK** que agora está na pasta local

5. **Remova do Git** (importante!):
   ```bash
   # Adicione ao .gitignore se ainda não estiver
   echo "*.apk" >> app/build/outputs/apk/debug/.gitignore
   
   # Remova do Git (mas mantenha localmente)
   git rm --cached app/build/outputs/apk/debug/app-debug.apk
   git commit -m "Remove APK do controle de versão"
   git push
   ```

---

### **Método 3: Via Terminal/SCP (Avançado)**

Se você tem acesso SSH à VM:

```bash
# No Windows (PowerShell)
scp ubuntu@vm-ip:/workspace/app/build/outputs/apk/debug/app-debug.apk ./
```

---

### **Método 4: Gerar APK Localmente**

Se preferir gerar o APK no seu computador:

1. **Sincronize o código** (via Git pull)
2. **Execute localmente**:
   ```bash
   .\gradlew.bat :app:assembleDebug
   ```
3. **O APK será gerado em**: `app\build\outputs\apk\debug\app-debug.apk`

---

## 📲 Instalar no Celular

Após baixar o APK:

### **Android:**
1. **Ative** "Fontes desconhecidas" nas configurações
2. **Transfira** o APK para o celular (USB, email, Google Drive, etc.)
3. **Abra** o arquivo no celular
4. **Toque** em "Instalar"

### **Via ADB (se conectado):**
```bash
adb install app-debug.apk
```

---

## ✅ Resumo

- ✅ APK gerado na VM: `app/build/outputs/apk/debug/app-debug.apk` (25MB)
- ⚠️ Precisa baixar/sincronizar para ver localmente
- 🚀 Método mais fácil: Cursor Explorer ou Git (Método 2)

---

## 🆘 Problemas?

Se ainda não conseguir:
1. Verifique se o arquivo existe na VM: `ls -lh app/build/outputs/apk/debug/app-debug.apk`
2. Tente gerar localmente: `.\gradlew.bat :app:assembleDebug`
3. Use o Método 2 (Git) como alternativa mais confiável
