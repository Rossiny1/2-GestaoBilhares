# 📱 VISUALIZAÇÃO DE APK NO CURSOR - GESTAO BILHARES

## 🎯 **RESUMO EXECUTIVO**

**SIM, é possível visualizar telas do APK diretamente no Cursor!** Existem múltiplas abordagens para isso, desde emuladores integrados até ferramentas de captura de tela e análise de layout.

## 🚀 **OPÇÕES DISPONÍVEIS**

### 1. **📱 EMULADOR ANDROID (RECOMENDADO)**

#### **Vantagens:**

- ✅ Visualização em tempo real
- ✅ Debugging completo
- ✅ Testes de diferentes resoluções
- ✅ Integração com Android Studio
- ✅ Logs em tempo real

#### **Como usar:**

```powershell
# Executar script de visualização
.\visualizar-apk-cursor.ps1 -Emulator -Build

# Ou modo interativo
.\visualizar-apk-cursor.ps1
# Escolher opção 1: Emulador Android
```

#### **Pré-requisitos:**

1. Android SDK instalado
2. AVD (Android Virtual Device) criado
3. Emulador configurado

---

### 2. **📱 DISPOSITIVO FÍSICO**

#### **Vantagens:**

- ✅ Performance real
- ✅ Testes em hardware específico
- ✅ Comportamento real do usuário
- ✅ Testes de sensores (GPS, câmera, etc.)

#### **Como usar:**

```powershell
# Conectar dispositivo via USB
# Habilitar Depuração USB
.\visualizar-apk-cursor.ps1 -Device -Build
```

---

### 3. **📸 SCREENSHOTS AUTOMÁTICOS**

#### **Vantagens:**

- ✅ Captura rápida de telas
- ✅ Documentação visual
- ✅ Comparação de versões
- ✅ Não requer dispositivo conectado

#### **Como usar:**

```powershell
# Capturar screenshot atual
.\visualizar-apk-cursor.ps1 -Screenshot

# Screenshot com timestamp
# Arquivo salvo: screenshot-20250701-154926.png
```

---

### 4. **🔍 LAYOUT INSPECTOR**

#### **Vantagens:**

- ✅ Análise detalhada de UI
- ✅ Hierarquia de views
- ✅ Propriedades de componentes
- ✅ Debug de layout

#### **Como usar:**

```powershell
# Abrir Layout Inspector
.\visualizar-apk-cursor.ps1 -LayoutInspector

# Ou via Android Studio:
# View > Tool Windows > Layout Inspector
```

---

### 5. **📊 LOGS EM TEMPO REAL**

#### **Vantagens:**

- ✅ Debugging avançado
- ✅ Monitoramento de performance
- ✅ Detecção de crashes
- ✅ Análise de comportamento

#### **Como usar:**

```powershell
# Logs específicos do app
.\visualizar-apk-cursor.ps1
# Escolher opção 5: Logs em Tempo Real

# Ou comando direto:
C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -s "GestaoBilhares:*"
```

## 🛠️ **FERRAMENTAS INTEGRADAS**

### **Script Automatizado: `visualizar-apk-cursor.ps1`**

#### **Funcionalidades:**

- ✅ Verificação automática de pré-requisitos
- ✅ Inicialização de emulador
- ✅ Detecção de dispositivos
- ✅ Build e instalação automática
- ✅ Captura de screenshots
- ✅ Logs em tempo real
- ✅ Menu interativo

#### **Parâmetros disponíveis:**

```powershell
.\visualizar-apk-cursor.ps1 -Emulator     # Iniciar emulador
.\visualizar-apk-cursor.ps1 -Device       # Usar dispositivo físico
.\visualizar-apk-cursor.ps1 -Screenshot   # Capturar tela
.\visualizar-apk-cursor.ps1 -LayoutInspector  # Abrir Layout Inspector
.\visualizar-apk-cursor.ps1 -Build        # Construir APK
.\visualizar-apk-cursor.ps1 -Install      # Instalar APK
```

## 📋 **FLUXO DE TRABALHO RECOMENDADO**

### **Para Desenvolvimento Diário:**

1. **Preparação:**

   ```powershell
   # Verificar se tudo está configurado
   .\visualizar-apk-cursor.ps1
   # Escolher opção 6: Informações do APK
   ```

2. **Desenvolvimento:**

   ```powershell
   # Fazer alterações no código
   # Build e visualização automática
   .\visualizar-apk-cursor.ps1 -Emulator -Build
   ```

3. **Testes:**

   ```powershell
   # Capturar screenshots para documentação
   .\visualizar-apk-cursor.ps1 -Screenshot
   
   # Analisar layout se necessário
   .\visualizar-apk-cursor.ps1 -LayoutInspector
   ```

4. **Debugging:**

   ```powershell
   # Logs em tempo real
   .\visualizar-apk-cursor.ps1
   # Escolher opção 5: Logs em Tempo Real
   ```

## 🔧 **CONFIGURAÇÃO AVANÇADA**

### **Criar AVD (Android Virtual Device):**

1. **Via Android Studio:**
   - Tools > AVD Manager
   - Create Virtual Device
   - Escolher dispositivo (ex: Pixel 4)
   - Escolher sistema (ex: API 34)
   - Finalizar criação

2. **Via Linha de Comando:**

   ```bash
   # Listar AVDs disponíveis
   C:\Users\Rossiny\AppData\Local\Android\Sdk\emulator\emulator.exe -list-avds
   
   # Criar novo AVD
   C:\Users\Rossiny\AppData\Local\Android\Sdk\cmdline-tools\latest\bin\avdmanager.bat create avd -n "Pixel4_API34" -k "system-images;android-34;google_apis;x86_64"
   ```

### **Configurar Dispositivo Físico:**

1. **Habilitar Opções de Desenvolvedor:**
   - Configurações > Sobre o telefone
   - Tocar 7 vezes em "Número da versão"

2. **Habilitar Depuração USB:**
   - Configurações > Opções do desenvolvedor
   - Ativar "Depuração USB"

3. **Conectar via USB:**
   - Aceitar autorização no dispositivo
   - Verificar com `adb devices`

## 📊 **COMPARAÇÃO DE MÉTODOS**

| Método | Velocidade | Realismo | Debugging | Facilidade |
|--------|------------|----------|-----------|------------|
| **Emulador** | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Dispositivo Físico** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Screenshots** | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐ | ⭐⭐⭐⭐⭐ |
| **Layout Inspector** | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Logs** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |

## 🚨 **SOLUÇÃO DE PROBLEMAS**

### **Emulador não inicia:**

```powershell
# Verificar AVDs disponíveis
C:\Users\Rossiny\AppData\Local\Android\Sdk\emulator\emulator.exe -list-avds

# Verificar recursos do sistema
# Habilitar Virtualização no BIOS
# Instalar Intel HAXM ou AMD Hypervisor
```

### **Dispositivo não detectado:**

```powershell
# Reiniciar servidor ADB
C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe kill-server
C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe start-server

# Verificar drivers USB
# Testar cabo USB diferente
```

### **APK não instala:**

```powershell
# Desinstalar versão anterior
C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe uninstall com.example.gestaobilhares

# Verificar assinatura
# Limpar cache do dispositivo
```

## 🎯 **MELHORES PRÁTICAS**

### **Para Desenvolvimento Rápido:**

1. Use emulador para desenvolvimento diário
2. Use dispositivo físico para testes finais
3. Capture screenshots para documentação
4. Monitore logs para debugging

### **Para Debugging:**

1. Use Layout Inspector para problemas de UI
2. Use logs em tempo real para crashes
3. Use dispositivo físico para problemas específicos
4. Capture screenshots de erros

### **Para Documentação:**

1. Capture screenshots de todas as telas
2. Use diferentes resoluções de emulador
3. Documente fluxos de navegação
4. Mantenha histórico de versões

## 📈 **PRÓXIMOS PASSOS**

### **Melhorias Futuras:**

- [ ] Integração com Android Studio
- [ ] Captura automática de vídeos
- [ ] Testes automatizados de UI
- [ ] Análise de performance
- [ ] Relatórios automáticos

### **Ferramentas Adicionais:**

- [ ] Firebase Test Lab
- [ ] Appium para testes automatizados
- [ ] Fastlane para CI/CD
- [ ] SonarQube para análise de código

---

## ✅ **CONCLUSÃO**

**O Cursor oferece excelente suporte para visualização de APKs Android** através de múltiplas ferramentas integradas. O script `visualizar-apk-cursor.ps1` automatiza todo o processo, tornando o desenvolvimento mais eficiente e produtivo.

**Recomendação:** Use o emulador para desenvolvimento diário e dispositivo físico para testes finais, sempre monitorando logs e capturando screenshots para documentação.

---

**📱 Status:** Scripts prontos para uso
**🔄 Última atualização:** 01/07/2025
**✅ Compatibilidade:** Windows 10/11 + PowerShell + Android SDK
