# 🚀 CHECKLIST CRÍTICO PARA PRODUÇÃO

**Data**: Dezembro 2025  
**Status**: ⚠️ **AÇÃO IMEDIATA NECESSÁRIA**

---

## ⚠️ **CRÍTICO - FAZER ANTES DE PUBLICAR**

### **1. KEYSTORE DE PRODUÇÃO** 🔐 **CRÍTICO**

#### ✅ **Status Atual**: ✅ **RESOLVIDO**

**Problema Identificado**:
- O `build.gradle.kts` tem fallback para debug keystore se `keystore.properties` não existir
- Isso significa que o APK de release pode estar sendo assinado com debug key (INSEGURO!)

**Ação Imediata**:

1. **Criar Keystore de Produção**:
```bash
keytool -genkey -v -keystore gestaobilhares-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias gestaobilhares
```

2. **Criar arquivo `keystore.properties` na raiz do projeto**:
```properties
storePassword=SUA_SENHA_FORTE_AQUI
keyPassword=SUA_SENHA_FORTE_AQUI
keyAlias=gestaobilhares
storeFile=C:/caminho/para/gestaobilhares-release.jks
```

3. **Adicionar ao `.gitignore`**:
```
keystore.properties
*.jks
*.keystore
```

4. **Backup Seguro do Keystore**:
   - ⚠️ **GUARDE EM LUGAR SEGURO** (cofre, backup criptografado)
   - ⚠️ **SEM O KEYSTORE, VOCÊ NÃO PODE ATUALIZAR O APP NO PLAY STORE**
   - ⚠️ **NUNCA COMMITE O KEYSTORE NO GIT**

**Verificação**:
```bash
# Verificar se o APK está assinado corretamente
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

---

### **2. REMOVER LOGS DE DEBUG** 🗑️ **CRÍTICO**

#### ✅ **Status Atual**: ✅ **RESOLVIDO**

**Problema Identificado**:
- Encontrados `Log.d()`, `Log.e()`, `Log.w()` no código
- Alguns logs podem conter informações sensíveis (PII)

**Ação Imediata**:

1. **Substituir todos os `Log.*` por `Timber.*`**:
```kotlin
// ❌ REMOVER
Log.d("TAG", "Mensagem")
Log.e("TAG", "Erro", exception)

// ✅ USAR
Timber.d("Mensagem")
Timber.e(exception, "Erro")
```

2. **Verificar se Timber está configurado corretamente** (já está ✅):
```kotlin
// GestaoBilharesApplication.kt - JÁ ESTÁ CORRETO
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree()) // Debug: logs completos
} else {
    Timber.plant(CrashlyticsTree()) // Produção: apenas WARN/ERROR
}
```

3. **Buscar e remover logs problemáticos**:
```bash
# Buscar todos os Log.d, Log.e, etc.
grep -r "Log\." app/src/main/
grep -r "Log\." ui/src/main/
grep -r "Log\." data/src/main/
grep -r "Log\." sync/src/main/
```

**Arquivos Encontrados com Logs**:
- `app/src/main/java/com/example/gestaobilhares/MainActivity.kt`
- `app/src/main/java/com/example/gestaobilhares/notification/NotificationService.kt`
- Múltiplos arquivos em `ui/src/main/`

**Ação**: Substituir todos por Timber antes do build de release.

---

### **3. PROGUARD/R8 - VERIFICAR REGRAS** 🛡️ **CRÍTICO**

#### ✅ **Status Atual**: ✅ **CONFIGURADO, MAS VERIFICAR**

**Verificações Necessárias**:

1. **Testar Build de Release**:
```bash
./gradlew assembleRelease
```

2. **Verificar se o app funciona após minificação**:
   - Instalar APK de release em dispositivo
   - Testar fluxos críticos:
     - Login
     - Sincronização
     - Criação de acerto
     - Geração de relatório

3. **Adicionar regras ProGuard se necessário**:
```proguard
# Se houver crashes após minificação, adicionar:
-keep class com.example.gestaobilhares.** { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
```

**Arquivo**: `app/proguard-rules.pro` (já existe ✅)

---

### **4. FIREBASE - CONFIGURAÇÕES DE PRODUÇÃO** 🔥 **CRÍTICO**

#### ✅ **Status Atual**: ✅ **CONFIGURADO**

**Verificações Necessárias**:

1. **Firebase Console - Verificar**:
   - ✅ Crashlytics ativo
   - ✅ Analytics ativo
   - ✅ Performance Monitoring ativo
   - ✅ Remote Config configurado

2. **Firestore Rules - Verificar Segurança**:
   - Verificar `firestore.rules` no projeto
   - Garantir que regras de segurança estão corretas
   - Testar acesso não autorizado

3. **Firebase Storage Rules**:
   - Verificar regras de acesso ao Storage
   - Garantir que uploads são seguros

4. **Remote Config - Valores de Produção**:
   - Verificar valores padrão em `GestaoBilharesApplication.kt`
   - Configurar valores no Firebase Console

---

### **5. VERSIONAMENTO** 📱 **IMPORTANTE**

#### ✅ **Status Atual**: ✅ **ATUALIZADO (v2 / 1.0.0)**

**Ação Imediata**:

1. **Atualizar `versionCode` e `versionName`**:
```kotlin
// app/build.gradle.kts
defaultConfig {
    versionCode = 1  // ⚠️ Incrementar a cada release
    versionName = "1.0.0"  // ⚠️ Semântico (MAJOR.MINOR.PATCH)
}
```

2. **Estratégia de Versionamento**:
   - `versionCode`: Incrementar sempre (1, 2, 3, ...)
   - `versionName`: Semântico (1.0.0, 1.0.1, 1.1.0, ...)

---

### **6. TESTES CRÍTICOS ANTES DE PUBLICAR** 🧪 **CRÍTICO**

#### ✅ **Status Atual**: ⚠️ **EXECUTAR TESTES**

**Checklist de Testes**:

1. **Testes Funcionais Críticos**:
   - [ ] Login/Logout
   - [ ] Sincronização (Pull + Push)
   - [ ] Criação de acerto
   - [ ] Geração de relatório PDF
   - [ ] Backup de emergência
   - [ ] Navegação entre telas principais

2. **Testes de Performance**:
   - [ ] App abre em < 3 segundos (cold start)
   - [ ] Sincronização completa em < 2 minutos
   - [ ] Sem memory leaks (usar LeakCanary em debug)

3. **Testes de Segurança**:
   - [ ] Dados sensíveis não aparecem em logs
   - [ ] Keystore configurado corretamente
   - [ ] ProGuard não quebra funcionalidades

4. **Testes em Dispositivos Reais**:
   - [ ] Android 7.0 (API 24) - mínimo
   - [ ] Android 14 (API 34) - target
   - [ ] Diferentes tamanhos de tela
   - [ ] Com e sem internet

---

### **7. CONFIGURAÇÕES DE BUILD DE RELEASE** 🏗️ **IMPORTANTE**

#### ✅ **Status Atual**: ✅ **CONFIGURADO**

**Verificações**:

1. **Build Type Release**:
```kotlin
// ✅ JÁ ESTÁ CORRETO
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = true  // ✅
        isShrinkResources = true  // ✅
        proguardFiles(...)  // ✅
    }
}
```

2. **Verificar APK Final**:
```bash
# Tamanho do APK
ls -lh app/build/outputs/apk/release/app-release.apk

# Verificar assinatura
jarsigner -verify -verbose app/build/outputs/apk/release/app-release.apk
```

---

### **8. MONITORAMENTO PÓS-LANÇAMENTO** 📊 **IMPORTANTE**

#### ✅ **Status Atual**: ✅ **CONFIGURADO**

**Configurar Alertas no Firebase**:

1. **Crashlytics**:
   - Configurar alertas para crashes críticos
   - Monitorar taxa de crash (meta: < 1%)

2. **Analytics**:
   - Configurar eventos customizados importantes
   - Monitorar uso do app

3. **Performance**:
   - Monitorar tempo de inicialização
   - Monitorar performance de rede

---

## 📋 CHECKLIST RESUMIDO

### **ANTES DO BUILD DE RELEASE**:

- [ ] **1. Keystore de produção criado e configurado**
- [ ] **2. Todos os `Log.*` substituídos por `Timber.*`**
- [ ] **3. ProGuard testado e funcionando**
- [ ] **4. Firebase configurado para produção**
- [ ] **5. Versionamento atualizado**
- [ ] **6. Testes críticos executados e passando**
- [ ] **7. Build de release testado em dispositivo real**
- [ ] **8. Monitoramento configurado**

### **APÓS O BUILD**:

- [ ] **9. APK assinado corretamente (verificar com jarsigner)**
- [ ] **10. Testar APK em dispositivo limpo (sem dados anteriores)**
- [ ] **11. Verificar tamanho do APK (< 50MB recomendado)**
- [ ] **12. Upload para Firebase App Distribution (testers)**
- [ ] **13. Testes com testers beta**
- [ ] **14. Upload para Play Store (quando aprovado)**

---

## 🚨 AÇÕES IMEDIATAS (FAZER AGORA)

### **PRIORIDADE MÁXIMA**:

1. **Criar Keystore de Produção** (15 minutos)
   - ⚠️ **SEM ISSO, NÃO PUBLIQUE!**

2. **Remover Logs de Debug** (1-2 horas)
   - Substituir todos os `Log.*` por `Timber.*`
   - Verificar que não há PII em logs

3. **Testar Build de Release** (30 minutos)
   - Build completo
   - Testar em dispositivo real
   - Verificar que tudo funciona

### **PRIORIDADE ALTA**:

4. **Executar Testes Críticos** (2-3 horas)
   - Fluxos principais
   - Sincronização
   - Geração de relatórios

5. **Configurar Monitoramento** (30 minutos)
   - Alertas no Firebase
   - Dashboards

---

## 📝 NOTAS IMPORTANTES

### **⚠️ NUNCA FAÇA**:

- ❌ **NUNCA** publique APK assinado com debug keystore
- ❌ **NUNCA** commite keystore ou senhas no Git
- ❌ **NUNCA** deixe logs de debug em produção
- ❌ **NUNCA** publique sem testar build de release
- ❌ **NUNCA** publique sem monitoramento configurado

### **✅ SEMPRE FAÇA**:

- ✅ **SEMPRE** teste build de release antes de publicar
- ✅ **SEMPRE** faça backup do keystore em local seguro
- ✅ **SEMPRE** use Timber em vez de Log
- ✅ **SEMPRE** verifique ProGuard não quebrou nada
- ✅ **SEMPRE** monitore crashes após publicação

---

## 🔗 RECURSOS ÚTEIS

- [Android App Signing](https://developer.android.com/studio/publish/app-signing)
- [ProGuard Rules](https://developer.android.com/studio/build/shrink-code)
- [Firebase Console](https://console.firebase.google.com)
- [Play Store Console](https://play.google.com/console)

---

**Última Atualização**: Dezembro 2025  
**Próxima Revisão**: Após primeira publicação

