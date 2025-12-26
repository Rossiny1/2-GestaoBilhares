# 🎯 Plano de Ação: Correção dos Problemas do Crashlytics

**Data:** 23 de Dezembro de 2025  
**Prioridade:** 🔴 ALTA - Problemas FATAL afetando usuários

---

## 📊 Resumo Executivo

**Total de Problemas:** 5  
**FATAL (Críticos):** 3  
**NON_FATAL:** 2  
**Usuários Impactados:** 27

---

## 🎯 Estratégia Recomendada

### ✅ Abordagem: **Correção Incremental e Testada**

1. **Fase 1 (URGENTE)**: Corrigir problemas FATAL que causam crashes
2. **Fase 2 (IMPORTANTE)**: Melhorar configuração ProGuard e mapeamento
3. **Fase 3 (MONITORAR)**: Investigar problemas NON_FATAL

---

## 🔴 FASE 1: Problemas FATAL (URGENTE)

### Problema 1: DialogAditivoEquipamentosBinding.inflate
**Impacto:** 13 eventos, 11 usuários  
**Complexidade:** 🟢 BAIXA  
**Tempo Estimado:** 30-60 minutos

#### ✅ Solução Recomendada (MAIS RÁPIDA)

**Opção A: Substituir MaterialTextView por TextView padrão** ⭐ **RECOMENDADO**

O layout já usa `TextView` padrão, mas o erro menciona `MaterialTextView`. Isso pode ser:
- Cache do binding gerado
- Algum componente ainda usando MaterialTextView

**Ação:**
1. Limpar build e regenerar bindings
2. Verificar se há algum `MaterialTextView` escondido no layout
3. Garantir que todos os TextViews sejam padrão

**Código:**
```xml
<!-- ✅ JÁ ESTÁ CORRETO - Usar TextView padrão -->
<TextView
    android:id="@+id/tvDialogTitle"
    ...
    android:textColor="@color/text_primary" />
```

**Opção B: Adicionar atributos faltantes ao tema** (se Opção A não resolver)

Adicionar ao `AditivoDialogTheme`:
```xml
<!-- Atributos adicionais para MaterialTextView -->
<item name="android:textAppearance">@style/TextAppearance.Material3.BodyLarge</item>
<item name="textAppearanceHeadline1">@style/TextAppearance.Material3.Headline1</item>
<item name="textAppearanceHeadline2">@style/TextAppearance.Material3.Headline2</item>
```

---

### Problema 2: AditivoDialog.onCreateDialog (Tema AppCompat)
**Impacto:** 2 eventos, 1 usuário  
**Complexidade:** 🟡 MÉDIA  
**Tempo Estimado:** 1-2 horas

#### ✅ Solução Recomendada

**Opção A: Usar AlertDialog padrão ao invés de MaterialAlertDialogBuilder** ⭐ **RECOMENDADO**

Mais simples e compatível com qualquer tema:

```kotlin
// ❌ ANTES (MaterialAlertDialogBuilder)
return MaterialAlertDialogBuilder(requireContext(), R.style.AditivoDialogTheme)
    .setView(binding.root)
    .setCancelable(false)
    .create()

// ✅ DEPOIS (AlertDialog padrão)
return AlertDialog.Builder(requireContext(), R.style.AditivoDialogTheme)
    .setView(binding.root)
    .setCancelable(false)
    .create()
```

**Opção B: Ajustar tema para ser compatível com AppCompat** (se quiser manter Material)

1. Verificar tema base no `AndroidManifest.xml`
2. Garantir que seja descendente de `Theme.AppCompat`
3. Ajustar `AditivoDialogTheme` para usar parent compatível

---

### Problema 3: SyncRepository.mapType (TypeToken/ProGuard)
**Impacto:** 4 eventos, 1 usuário  
**Complexidade:** 🟡 MÉDIA  
**Tempo Estimado:** 1-2 horas

#### ✅ Solução Recomendada

**Verificar e reforçar regras ProGuard:**

1. **Verificar se as regras estão sendo aplicadas:**
   - Confirmar que `proguard-rules.pro` está sendo usado no build
   - Verificar se `minifyEnabled = true` está ativado

2. **Adicionar regra mais específica:**
```proguard
# ✅ REGRA ESPECÍFICA PARA SYNCREPOSITORY
-keep class com.example.gestaobilhares.sync.SyncRepository$Companion$MapTypeToken {
    <init>();
}
-keep class com.example.gestaobilhares.sync.SyncRepository$Companion {
    *;
}
```

3. **Garantir que Signature está preservado:**
```proguard
# ✅ JÁ DEVE ESTAR, MAS CONFIRMAR
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
```

4. **Testar em build de release:**
   - Fazer build de release
   - Testar sincronização manual
   - Verificar se o erro ainda ocorre

---

## 🟡 FASE 2: Configuração e Melhorias (IMPORTANTE)

### Problema 4: s6.f0 (Código Ofuscado)
**Impacto:** 10 eventos, 2 usuários  
**Complexidade:** 🟢 BAIXA (configuração)  
**Tempo Estimado:** 30 minutos

#### ✅ Solução: Configurar Upload de Mapping

**1. Habilitar upload automático no `build.gradle.kts`:**

```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // ✅ ADICIONAR: Upload automático de mapping
            firebaseCrashlytics {
                nativeSymbolUploadEnabled = true
                unstrippedNativeLibsDir = "build/intermediates/merged_native_libs/release/out/lib"
            }
        }
    }
}
```

**2. Adicionar plugin do Crashlytics (se não tiver):**

```kotlin
plugins {
    id("com.google.firebase.crashlytics")
}
```

**3. Verificar se o mapping está sendo gerado:**
- Build de release deve gerar `app/build/outputs/mapping/release/mapping.txt`
- O Firebase CLI deve fazer upload automático

---

### Problema 5: JobCancellationException
**Impacto:** 22 eventos, 12 usuários  
**Complexidade:** 🟡 MÉDIA (análise)  
**Tempo Estimado:** 2-4 horas (investigação)

#### ✅ Solução: Investigação e Tratamento

**1. Verificar se é comportamento esperado:**
- Jobs cancelados durante navegação são normais
- Verificar se está afetando funcionalidades

**2. Adicionar tratamento adequado:**
```kotlin
try {
    // operação assíncrona
} catch (e: CancellationException) {
    // ✅ Tratamento adequado - não é erro
    Timber.d("Operação cancelada normalmente")
    throw e // Re-throw para propagar cancelamento
} catch (e: Exception) {
    // Erro real
    Timber.e(e, "Erro na operação")
}
```

**3. Garantir que operações críticas não sejam canceladas:**
```kotlin
// Para operações críticas, usar NonCancellable
withContext(NonCancellable) {
    // Operação que não deve ser cancelada
}
```

---

## 📋 Plano de Execução Recomendado

### ✅ Semana 1: Correções Críticas

**Dia 1-2: Problemas FATAL**
- [ ] **Problema 1**: Limpar build e verificar layout (30 min)
- [ ] **Problema 2**: Trocar para AlertDialog padrão (1h)
- [ ] **Problema 3**: Reforçar regras ProGuard (1h)
- [ ] **Teste**: Build de release e teste em dispositivo real

**Dia 3: Configuração**
- [ ] **Problema 4**: Configurar upload de mapping (30 min)
- [ ] **Teste**: Build de release e verificar upload

**Dia 4-5: Monitoramento**
- [ ] Deploy da versão corrigida
- [ ] Monitorar Crashlytics por 2-3 dias
- [ ] Verificar se os erros diminuíram

### ✅ Semana 2: Melhorias e Investigação

**Dia 1-2: Investigação**
- [ ] **Problema 5**: Analisar logs de JobCancellationException
- [ ] Identificar padrões e causas
- [ ] Implementar correções se necessário

**Dia 3-5: Validação**
- [ ] Testes completos
- [ ] Validação com usuários
- [ ] Documentação das correções

---

## 🎯 Priorização Final (Ordem de Execução)

### 🔴 URGENTE (Fazer Agora)
1. ✅ **Problema 1** - DialogAditivoEquipamentosBinding (30 min)
2. ✅ **Problema 2** - Tema AppCompat (1h)
3. ✅ **Problema 3** - TypeToken/ProGuard (1h)

### 🟡 IMPORTANTE (Esta Semana)
4. ✅ **Problema 4** - Configurar mapping (30 min)

### 🟢 MONITORAR (Próxima Semana)
5. ⚠️ **Problema 5** - JobCancellationException (investigar)

---

## 🧪 Checklist de Testes

Após cada correção, testar:

- [ ] Build de debug funciona
- [ ] Build de release funciona
- [ ] Diálogo de aditivo abre sem crash
- [ ] Sincronização manual funciona
- [ ] App não crasha em Android 12 (Samsung Galaxy A31)
- [ ] Verificar Crashlytics após deploy

---

## 📊 Métricas de Sucesso

**Meta:** Reduzir crashes FATAL em 90% em 1 semana

- **Antes:** 19 eventos FATAL
- **Meta:** < 2 eventos FATAL
- **Monitorar:** Dashboard do Crashlytics diariamente

---

## 💡 Dicas Importantes

1. **Testar em dispositivo real**: Todos os erros ocorreram em Samsung Galaxy A31 (Android 12)
2. **Build de release**: Muitos erros só aparecem em release (ProGuard/R8)
3. **Limpar build**: Sempre limpar build após mudanças em temas/layouts
4. **Incremental**: Corrigir um problema por vez e testar antes do próximo

---

## 🔗 Recursos Úteis

- [Documentação Material Design 3](https://m3.material.io/)
- [Guia ProGuard Android](https://developer.android.com/studio/build/shrink-code)
- [Firebase Crashlytics - Upload de Mapping](https://firebase.google.com/docs/crashlytics/get-deobfuscated-crash-reports)

---

**Última Atualização:** 23 de Dezembro de 2025

