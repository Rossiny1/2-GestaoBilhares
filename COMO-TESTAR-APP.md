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
