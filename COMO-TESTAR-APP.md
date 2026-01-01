# Como Testar o App Android

## Situação Atual

O emulador Android requer aceleração de hardware (KVM), mas a VM não possui suporte a virtualização aninhada. Portanto, não é possível rodar o emulador diretamente nesta VM.

## ✅ O que foi feito

1. ✅ Android SDK instalado em `~/android-sdk`
2. ✅ Componentes instalados:
   - Platform Tools (adb)
   - Android SDK Platform 34
   - Build Tools 34.0.0
   - Emulator
   - System Image (Android 34, Google APIs, x86_64)
3. ✅ AVD criado: `test_avd`
4. ✅ APK compilado com sucesso

## 📱 Opções para Testar o App

### Opção 1: Dispositivo Físico (Recomendado)

#### Via USB:
```bash
# 1. Conecte seu dispositivo Android via USB
# 2. Ative "Depuração USB" nas opções de desenvolvedor
# 3. Verifique conexão:
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
adb devices

# 4. Instale o app:
cd /workspace
./gradlew installDebug
```

#### Via Rede (ADB over Network):
```bash
# No dispositivo Android:
# 1. Conecte via USB primeiro
# 2. Execute: adb tcpip 5555
# 3. Desconecte USB
# 4. Conecte via rede:
adb connect <IP_DO_DISPOSITIVO>:5555

# 5. Instale o app:
cd /workspace
./gradlew installDebug
```

### Opção 2: Emulador em Outro Ambiente

Se você tiver acesso a uma máquina com suporte a KVM:

```bash
# 1. Configure o Android SDK (já feito aqui)
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator

# 2. Inicie o emulador:
$ANDROID_HOME/emulator/emulator -avd test_avd &

# 3. Aguarde inicialização:
adb wait-for-device
adb shell getprop sys.boot_completed

# 4. Instale o app:
cd /workspace
./gradlew installDebug
```

### Opção 3: Serviços de Emulação em Nuvem

- **Firebase Test Lab**: https://firebase.google.com/docs/test-lab
- **AWS Device Farm**: https://aws.amazon.com/device-farm/
- **BrowserStack**: https://www.browserstack.com/app-automate

### Opção 4: Usar o APK Compilado

O APK já foi compilado com sucesso. Você pode:

1. **Transferir o APK para seu dispositivo:**
```bash
# Localizar o APK:
ls -lh /workspace/app/build/outputs/apk/debug/*.apk

# Transferir via scp, email, ou serviço de nuvem
```

2. **Instalar manualmente no dispositivo:**
   - Transfira o APK para o dispositivo
   - Ative "Fontes desconhecidas" nas configurações
   - Toque no arquivo APK para instalar

## 🔧 Configuração do Ambiente

O Android SDK está configurado em:
- **Localização**: `~/android-sdk`
- **Variáveis de ambiente**: Adicionadas ao `~/.bashrc`

Para usar em uma nova sessão:
```bash
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator
```

## 📝 Comandos Úteis

```bash
# Listar dispositivos conectados
adb devices

# Ver logs do app
adb logcat | grep -i "gestaobilhares"

# Desinstalar app
adb uninstall com.example.gestaobilhares

# Reiniciar adb
adb kill-server && adb start-server

# Ver informações do dispositivo
adb shell getprop ro.build.version.release
adb shell getprop ro.product.model
```

## 🚀 Próximos Passos

1. **Teste em dispositivo físico** (mais rápido e confiável)
2. **Configure CI/CD** para testes automatizados
3. **Use Firebase Test Lab** para testes em múltiplos dispositivos

## ⚠️ Nota Importante

A VM atual não suporta virtualização aninhada, então o emulador não pode rodar aqui. Para desenvolvimento local com emulador, use:
- Uma máquina física com Linux
- Uma VM com suporte a KVM habilitado
- WSL2 no Windows (com algumas limitações)

## 🌐 Alternativas Online para Teste de Frontend/Android

### Serviços de Emulação em Nuvem (Gratuitos/Pagos)

#### 1. **Firebase Test Lab** (Google) ⭐ Recomendado
- **Gratuito**: 5 testes físicos + 10 testes virtuais por dia
- **URL**: https://firebase.google.com/docs/test-lab
- **Como usar**:
  ```bash
  # Instalar Firebase CLI
  npm install -g firebase-tools
  
  # Fazer login
  firebase login
  
  # Executar testes
  firebase test android run \
    --app app-debug.apk \
    --device model=Pixel2,version=28 \
    --device model=NexusLowRes,version=25
  ```
- **Vantagens**: Integração com Firebase, múltiplos dispositivos, relatórios detalhados

#### 2. **BrowserStack App Live** ⭐ Melhor para Testes Interativos
- **Gratuito**: Trial de 100 minutos
- **URL**: https://www.browserstack.com/app-live
- **Como usar**:
  1. Criar conta em browserstack.com
  2. Fazer upload do APK
  3. Testar em dispositivos reais na nuvem
- **Vantagens**: Dispositivos reais, não emuladores, interface web interativa

#### 3. **AWS Device Farm**
- **Gratuito**: 250 minutos/mês
- **URL**: https://aws.amazon.com/device-farm/
- **Como usar**: Via console AWS ou CLI
- **Vantagens**: Integração com AWS, testes automatizados

#### 4. **Sauce Labs**
- **Gratuito**: Trial limitado
- **URL**: https://saucelabs.com/
- **Vantagens**: Suporte a múltiplas plataformas

#### 5. **Genymotion Cloud** (Pago, mas tem trial)
- **URL**: https://www.genymotion.com/cloud/
- **Vantagens**: Emuladores rápidos, múltiplas versões Android

### Alternativas para Emulação Sem KVM

#### 1. **Android-x86 em VirtualBox/VMware**
- Rodar Android-x86 como sistema operacional em uma VM
- **Limitação**: Não é um emulador Android completo, mas permite testar apps
- **URL**: https://www.android-x86.org/

#### 2. **Anbox** (Android in a Box)
- Container Linux que executa Android
- **Instalação**:
  ```bash
  sudo snap install --devmode --edge anbox
  ```
- **Limitação**: Requer suporte a kernel modules, pode não funcionar em todas as VMs

#### 3. **Scrcpy** (Espelhamento de Tela)
- Não é emulador, mas permite controlar dispositivo físico via USB/WiFi
- **Instalação**:
  ```bash
  sudo apt install scrcpy
  ```
- **Uso**: Conecte dispositivo físico e espelhe na VM
- **Vantagem**: Funciona sem KVM, usa dispositivo real

### Teste de Frontend Web (Se o app tiver versão web)

#### 1. **BrowserStack** (Web Testing)
- Teste em múltiplos navegadores e dispositivos
- **Gratuito**: Trial disponível

#### 2. **LambdaTest**
- Teste cross-browser
- **Gratuito**: 100 minutos/mês

#### 3. **Sauce Labs** (Web)
- Teste automatizado de frontend
- **Gratuito**: Trial disponível

### Recomendações Práticas

#### Para Desenvolvimento Rápido:
1. **Use dispositivo físico** via USB ou WiFi ADB (mais rápido e confiável)
2. **Firebase Test Lab** para testes automatizados em múltiplos dispositivos

#### Para Testes em Produção:
1. **BrowserStack App Live** para testes interativos em dispositivos reais
2. **Firebase Test Lab** para testes automatizados e CI/CD

#### Para Desenvolvimento Local (sem KVM):
1. **Scrcpy** para espelhar dispositivo físico
2. **Anbox** (se suportado pela VM)

### Scripts Úteis

#### Conectar Dispositivo via WiFi:
```bash
# No dispositivo (via USB primeiro):
adb tcpip 5555

# Depois desconecte USB e conecte via WiFi:
adb connect <IP_DO_DISPOSITIVO>:5555

# Verificar:
adb devices
```

#### Upload APK para Firebase Test Lab:
```bash
# Instalar Firebase CLI
npm install -g firebase-tools

# Configurar projeto
firebase init

# Executar teste
firebase test android run \
  --app /workspace/b/outputs/apk/debug/app-debug.apk \
  --type instrumentation \
  --timeout 5m
```

### Conclusão

**Para a VM do Cursor especificamente:**
- ❌ Emulador Android tradicional não funciona (sem KVM)
- ✅ **Melhor opção**: Dispositivo físico via ADB (USB ou WiFi)
- ✅ **Alternativa online**: Firebase Test Lab ou BrowserStack
- ✅ **Para desenvolvimento**: Scrcpy para espelhar dispositivo físico

**Próximos Passos:**
1. Se tiver dispositivo Android: Configure ADB over WiFi
2. Se não tiver: Use Firebase Test Lab (gratuito) ou BrowserStack (trial)
3. Para CI/CD: Integre Firebase Test Lab no pipeline
