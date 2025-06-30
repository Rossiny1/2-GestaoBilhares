# 📱 INSTALAÇÃO APK - GESTAO BILHARES

## 🚀 Scripts de Instalação Automática

Este projeto inclui scripts PowerShell para facilitar a instalação do APK via ADB.

## 📋 Pré-requisitos

1. **Android SDK instalado** (via Android Studio)
2. **Dispositivo Android conectado** via USB
3. **Depuração USB habilitada** no dispositivo
4. **PowerShell** (já incluído no Windows)

## 🔧 Scripts Disponíveis

### 1. `install-apk.ps1` - Instalador Completo

**Funcionalidades:**

- ✅ Verifica se ADB está disponível
- ✅ Detecta dispositivos conectados
- ✅ Constrói APK automaticamente (opcional)
- ✅ Desinstala versão anterior
- ✅ Instala nova versão
- ✅ Abre o app automaticamente
- ✅ Mostra informações detalhadas

**Uso:**

```powershell
# Instalar APK existente
.\install-apk.ps1

# Construir e instalar (se APK não existir)
.\install-apk.ps1 -Build

# Limpar, construir e instalar
.\install-apk.ps1 -Build -Clean

# Forçar reconstrução
.\install-apk.ps1 -Build -Force
```

### 2. `quick-install.ps1` - Instalação Rápida

**Funcionalidades:**

- ✅ Instalação simples e rápida
- ✅ Verifica se APK existe
- ✅ Abre o app automaticamente

**Uso:**

```powershell
# Instalação rápida
.\quick-install.ps1
```

## 📱 Como Usar

### Passo 1: Preparar o Dispositivo

1. Conecte o dispositivo Android via USB
2. Habilite "Depuração USB" nas opções de desenvolvedor
3. Aceite a autorização de depuração no dispositivo

### Passo 2: Executar o Script

```powershell
# Abra o PowerShell na pasta do projeto
cd C:\Users\Rossiny\Desktop\2-GestaoBilhares

# Execute o instalador completo
.\install-apk.ps1
```

### Passo 3: Verificar Instalação

- O script mostrará o progresso da instalação
- O app será aberto automaticamente no dispositivo
- Verifique se o app está funcionando corretamente

## 🔍 Solução de Problemas

### Erro: "ADB não encontrado"

```powershell
# Verifique se o Android SDK está instalado
# Caminho padrão: C:\Users\[USERNAME]\AppData\Local\Android\Sdk\platform-tools\adb.exe
```

### Erro: "Nenhum dispositivo conectado"

1. Verifique se o cabo USB está conectado
2. Habilite a depuração USB no dispositivo
3. Aceite a autorização de depuração

### Erro: "APK não encontrado"

```powershell
# Construa o APK primeiro
.\gradlew assembleDebug

# Ou use o script com build automático
.\install-apk.ps1 -Build
```

### Erro: "Falha na instalação"

1. Desinstale a versão anterior manualmente
2. Verifique se há espaço suficiente no dispositivo
3. Tente reiniciar o dispositivo

## 📊 Comandos Úteis

```powershell
# Ver logs do app
adb logcat -s GestaoBilhares

# Desinstalar app
adb uninstall com.example.gestaobilhares

# Abrir app
adb shell am start -n com.example.gestaobilhares/.MainActivity

# Listar dispositivos
adb devices

# Reiniciar servidor ADB
adb kill-server
adb start-server
```

## 🎯 Fluxo de Desenvolvimento

1. **Desenvolver** → Fazer alterações no código
2. **Construir** → `.\gradlew assembleDebug`
3. **Instalar** → `.\quick-install.ps1`
4. **Testar** → Verificar funcionalidades no dispositivo
5. **Repetir** → Voltar ao passo 1

## 📈 Benefícios dos Scripts

- ✅ **Automação completa** do processo de instalação
- ✅ **Verificações automáticas** de pré-requisitos
- ✅ **Feedback visual** com cores e símbolos
- ✅ **Tratamento de erros** com mensagens claras
- ✅ **Comandos úteis** para debugging
- ✅ **Flexibilidade** com diferentes opções

---

**📱 Status:** Scripts prontos para uso
**🔄 Última atualização:** 30/06/2025
**✅ Compatibilidade:** Windows 10/11 + PowerShell
