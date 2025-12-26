# 📊 Relatório de Erros do Crashlytics

**Data de Análise:** 23 de Dezembro de 2025  
**Período:** Últimos 7 dias (16-23 Dez 2025)  
**Versão do App:** 1.0.0 (build 2)

---

## 📈 Resumo Executivo

**Total de Problemas:** 5  
**Problemas FATAL:** 3  
**Problemas NON_FATAL:** 2  
**Total de Eventos:** 51  
**Total de Usuários Impactados:** 27

---

## 🔴 Problemas FATAL (Críticos)

### 1. ⚠️ DialogAditivoEquipamentosBinding.inflate - UnsupportedOperationException

**ID do Problema:** `3a321a01a8b5fd23e8b6940164348e9e`  
**Tipo:** FATAL  
**Eventos:** 13  
**Usuários Impactados:** 11  
**Primeira Ocorrência:** 3 dias atrás  
**Status:** 🔴 **ATIVO - REQUER CORREÇÃO**

#### Descrição
Erro ao inflar o layout `dialog_aditivo_equipamentos.xml`. O `MaterialTextView` está tentando resolver um atributo de tema (`TypedValue{t=0x2/d=0x101009b a=1}`) que não está disponível no contexto do tema aplicado.

#### Stack Trace
```
android.view.InflateException: Binary XML file line #20 in 
com.example.gestaobilhares:layout/dialog_aditivo_equipamentos: 
Error inflating class com.google.android.material.textview.MaterialTextView

Caused by: java.lang.UnsupportedOperationException: 
Failed to resolve attribute at index 6: TypedValue{t=0x2/d=0x101009b a=1}

at com.example.gestaobilhares.ui.databinding.DialogAditivoEquipamentosBinding.inflate
at com.example.gestaobilhares.ui.contracts.AditivoDialog.onCreateDialog (AditivoDialog.kt:68)
```

#### Análise
- O layout `dialog_aditivo_equipamentos.xml` usa `MaterialTextView` que requer atributos específicos do Material Design 3
- O tema `AditivoDialogTheme` está definido, mas pode estar faltando alguns atributos necessários
- O erro ocorre na linha 20 do layout XML, que corresponde ao primeiro `MaterialTextView`

#### Ação Recomendada
1. ✅ **Verificar se o layout usa componentes Material corretos**
2. ✅ **Adicionar todos os atributos necessários ao tema `AditivoDialogTheme`**
3. ✅ **Considerar usar `TextView` padrão ao invés de `MaterialTextView` se o tema não suportar**
4. ✅ **Testar em dispositivos com Android 12 (onde o erro ocorreu)**

#### Status da Correção
- ⚠️ **Parcialmente Corrigido**: O tema foi criado, mas ainda há problemas com atributos específicos
- 📝 **Nota**: O usuário mencionou que o crash do diálogo para adicionar mesa já foi corrigido, mas este erro ainda persiste

---

### 2. ⚠️ AditivoDialog.onCreateDialog - IllegalArgumentException (Tema AppCompat)

**ID do Problema:** `073b01c697776336b42557c2e6818d94`  
**Tipo:** FATAL  
**Eventos:** 2  
**Usuários Impactados:** 1  
**Primeira Ocorrência:** Ontem  
**Status:** 🔴 **ATIVO - REQUER CORREÇÃO**

#### Descrição
O `MaterialAlertDialogBuilder` está reclamando que o tema da aplicação não é descendente de `Theme.AppCompat`. Isso indica que o tema base da aplicação não está configurado corretamente.

#### Stack Trace
```
java.lang.IllegalArgumentException: The style on this component requires 
your app theme to be Theme.AppCompat (or a descendant).

at com.google.android.material.internal.ThemeEnforcement.checkAppCompatTheme
at com.google.android.material.dialog.MaterialAlertDialogBuilder.<init>
at com.example.gestaobilhares.ui.contracts.AditivoDialog.onCreateDialog (AditivoDialog.kt:70)
```

#### Análise
- O `MaterialAlertDialogBuilder` requer que o tema base da aplicação seja descendente de `Theme.AppCompat`
- O tema `AditivoDialogTheme` está usando `Theme.Material3.DayNight.Dialog.Alert` como parent
- Pode haver conflito entre Material 3 e AppCompat

#### Ação Recomendada
1. ✅ **Verificar o tema base da aplicação no `AndroidManifest.xml`**
2. ✅ **Garantir que o tema base seja descendente de `Theme.AppCompat`**
3. ✅ **Ajustar o `AditivoDialogTheme` para ser compatível com AppCompat**
4. ✅ **Considerar usar `AlertDialog.Builder` ao invés de `MaterialAlertDialogBuilder` se necessário**

#### Status da Correção
- 🔴 **NÃO CORRIGIDO**: Este é um problema diferente do anterior, relacionado ao tema base da aplicação

---

### 3. ⚠️ SyncRepository.mapType - TypeToken/ProGuard

**ID do Problema:** `f6a5e50fbf4e8bf45e14cc1e848afe80`  
**Tipo:** FATAL  
**Eventos:** 4  
**Usuários Impactados:** 1  
**Primeira Ocorrência:** 6 dias atrás  
**Status:** 🟡 **PARCIALMENTE CORRIGIDO**

#### Descrição
O R8/ProGuard está removendo as assinaturas genéricas do `TypeToken`, causando `IllegalStateException` quando o Gson tenta usar o tipo.

#### Stack Trace
```
java.lang.IllegalStateException: TypeToken must be created with a type argument: 
new TypeToken<...>() {}; When using code shrinkers (ProGuard, R8, ...) 
make sure that generic signatures are preserved.

at com.example.gestaobilhares.sync.SyncRepository$Companion$mapType$1.<init>
```

#### Análise
- O código já foi refatorado para usar uma classe estática interna `MapTypeToken` ao invés de classe anônima
- As regras ProGuard já incluem preservação de assinaturas genéricas
- O problema pode estar relacionado à forma como o `mapTypeTokenInstance` está sendo inicializado

#### Ação Recomendada
1. ✅ **Verificar se as regras ProGuard estão sendo aplicadas corretamente**
2. ✅ **Adicionar regra específica para `SyncRepository$Companion$MapTypeToken`**
3. ✅ **Garantir que `-keepattributes Signature` está presente**
4. ✅ **Testar build de release com R8 ativado**

#### Status da Correção
- 🟡 **PARCIALMENTE CORRIGIDO**: O código foi refatorado, mas o problema ainda ocorre em builds de release
- 📝 **Nota**: As regras ProGuard parecem estar corretas, mas podem não estar sendo aplicadas

---

## 🟡 Problemas NON_FATAL (Importantes)

### 4. ⚠️ kotlinx.coroutines.JobCancellationException

**ID do Problema:** `d677c91f7b5a867cdcbbcd5c7d26f844`  
**Tipo:** NON_FATAL  
**Eventos:** 22  
**Usuários Impactados:** 12  
**Status:** 🟡 **MONITORAR**

#### Descrição
Jobs de corrotinas estão sendo cancelados, possivelmente devido a:
- Navegação entre telas
- Cancelamento de operações assíncronas
- Timeout de operações

#### Ação Recomendada
1. ✅ **Verificar se os jobs estão sendo cancelados corretamente (não é necessariamente um erro)**
2. ✅ **Adicionar tratamento adequado para `CancellationException`**
3. ✅ **Garantir que operações críticas não sejam canceladas inadvertidamente**

#### Status
- 🟡 **MONITORAR**: Pode ser comportamento esperado, mas deve ser investigado se estiver afetando funcionalidades

---

### 5. ⚠️ s6.f0 (Código Ofuscado)

**ID do Problema:** `f07e31a9d9886adad05d48c640e27e9e`  
**Tipo:** NON_FATAL  
**Eventos:** 10  
**Usuários Impactados:** 2  
**Status:** 🟡 **INVESTIGAR**

#### Descrição
Erro com código ofuscado pelo R8/ProGuard. O nome `s6.f0` não permite identificar a origem do problema.

#### Ação Recomendada
1. ✅ **Adicionar mapeamento de símbolos (mapping.txt) ao Crashlytics**
2. ✅ **Usar `-keepnames` para preservar nomes de classes importantes**
3. ✅ **Adicionar regras ProGuard para preservar stack traces legíveis**

#### Status
- 🟡 **REQUER INVESTIGAÇÃO**: Sem mapeamento de símbolos, é difícil identificar a origem

---

## 📋 Priorização de Correções

### 🔴 Prioridade ALTA (Bloqueadores)
1. **DialogAditivoEquipamentosBinding.inflate** - 13 eventos, 11 usuários
2. **AditivoDialog.onCreateDialog (Tema AppCompat)** - 2 eventos, 1 usuário

### 🟡 Prioridade MÉDIA
3. **SyncRepository.mapType (TypeToken)** - 4 eventos, 1 usuário (já parcialmente corrigido)

### 🟢 Prioridade BAIXA (Monitorar)
4. **JobCancellationException** - Pode ser comportamento esperado
5. **s6.f0 (Ofuscado)** - Requer mapeamento de símbolos

---

## 🔧 Ações Imediatas Recomendadas

### 1. Corrigir Problema do DialogAditivoEquipamentosBinding
- [ ] Verificar se o layout `dialog_aditivo_equipamentos.xml` está usando componentes compatíveis
- [ ] Adicionar todos os atributos necessários ao tema `AditivoDialogTheme`
- [ ] Testar em dispositivo Android 12 (Samsung Galaxy A31)

### 2. Corrigir Problema do Tema AppCompat
- [ ] Verificar tema base da aplicação no `AndroidManifest.xml`
- [ ] Garantir compatibilidade entre Material 3 e AppCompat
- [ ] Ajustar `AditivoDialogTheme` se necessário

### 3. Melhorar Regras ProGuard
- [ ] Verificar se `-keepattributes Signature` está sendo aplicado
- [ ] Adicionar regra específica para `SyncRepository$Companion$MapTypeToken`
- [ ] Configurar upload de mapping.txt para Crashlytics

### 4. Configurar Mapeamento de Símbolos
- [ ] Habilitar upload automático de mapping.txt no build
- [ ] Configurar ProGuard para preservar nomes de classes críticas
- [ ] Testar se os erros ofuscados ficam legíveis após configuração

---

## 📊 Métricas por Dispositivo

**Dispositivo Mais Afetado:**
- Samsung Galaxy A31 (SM-A315G) - Android 12
- Todos os erros FATAL ocorreram neste dispositivo

**Versões Afetadas:**
- 1.0.0 (build 2) - Todos os erros
- 1.0 (build 1) - Erro TypeToken

---

## 📝 Notas Adicionais

- O usuário mencionou que o crash do diálogo para adicionar mesa já foi corrigido, mas os erros relacionados ao `AditivoDialog` ainda persistem
- Os erros estão concentrados em um único dispositivo (Samsung Galaxy A31), o que pode indicar problema específico de compatibilidade
- A maioria dos erros ocorre durante a criação de diálogos, sugerindo problema sistemático com temas/diálogos

---

## 🔗 Links Úteis

- [Console Firebase Crashlytics](https://console.firebase.google.com/project/gestaobilhares/crashlytics/app/android:com.example.gestaobilhares/issues)
- [Documentação Material Design 3](https://m3.material.io/)
- [Guia ProGuard para Android](https://developer.android.com/studio/build/shrink-code)

---

**Última Atualização:** 23 de Dezembro de 2025

