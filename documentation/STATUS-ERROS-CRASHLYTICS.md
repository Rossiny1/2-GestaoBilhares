# 📊 Status dos Erros do Crashlytics

**Data de Atualização:** 02 de Janeiro de 2026  
**Última Verificação:** Relatório de 23 de Dezembro de 2025

---

## 🔴 Erros FATAL (Críticos) - Ainda Não Corrigidos

### 1. ⚠️ DialogAditivoEquipamentosBinding.inflate - UnsupportedOperationException

**ID:** `3a321a01a8b5fd23e8b6940164348e9e`  
**Eventos:** 13  
**Usuários:** 11  
**Status:** 🔴 **NÃO CORRIGIDO**

**Problema:**
- Erro ao inflar layout `dialog_aditivo_equipamentos.xml`
- `MaterialTextView` tentando resolver atributo de tema não disponível
- Ocorre em Android 12 (Samsung Galaxy A31)

**Ação Necessária:**
- [ ] Verificar layout `dialog_aditivo_equipamentos.xml`
- [ ] Verificar tema `AditivoDialogTheme`
- [ ] Considerar usar `TextView` padrão ao invés de `MaterialTextView`
- [ ] Testar em Android 12

---

### 2. ⚠️ AditivoDialog.onCreateDialog - IllegalArgumentException (Tema AppCompat)

**ID:** `073b01c697776336b42557c2e6818d94`  
**Eventos:** 2  
**Usuários:** 1  
**Status:** 🔴 **NÃO CORRIGIDO**

**Problema:**
- `MaterialAlertDialogBuilder` requer tema descendente de `Theme.AppCompat`
- Tema base da aplicação pode não estar configurado corretamente

**Ação Necessária:**
- [ ] Verificar tema base no `AndroidManifest.xml`
- [ ] Garantir compatibilidade entre Material 3 e AppCompat
- [ ] Ajustar `AditivoDialogTheme` se necessário

---

### 3. ⚠️ SyncRepository.mapType - TypeToken/ProGuard

**ID:** `f6a5e50fbf4e8bf45e14cc1e848afe80`  
**Eventos:** 4  
**Usuários:** 1  
**Status:** 🟡 **PARCIALMENTE CORRIGIDO**

**Problema:**
- R8/ProGuard removendo assinaturas genéricas do `TypeToken`
- Regras ProGuard já existem, mas podem não estar sendo aplicadas

**Ação Necessária:**
- [x] Verificar regras ProGuard (já existem)
- [ ] Verificar se `-keepattributes Signature` está sendo aplicado
- [ ] Testar build de release com R8 ativado
- [ ] Verificar se `MapTypeToken` está sendo preservado

**Regras Atuais no proguard-rules.pro:**
```proguard
-keepattributes Signature
-keep class com.example.gestaobilhares.sync.SyncRepository$Companion$MapTypeToken {
    <init>();
    *;
}
```

---

## 🟡 Erros NON_FATAL - Status

### 4. ⚠️ kotlinx.coroutines.JobCancellationException

**ID:** `d677c91f7b5a867cdcbbcd5c7d26f844`  
**Eventos:** 22  
**Usuários:** 12  
**Status:** ✅ **CORRIGIDO PARCIALMENTE**

**Problema:**
- Jobs de corrotinas sendo cancelados
- Tratamento inadequado de `CancellationException`

**Correções Implementadas:**
- ✅ Adicionado tratamento específico em `BaseSyncHandler.executePaginatedQuery`
- ✅ Adicionado tratamento em `ClienteSyncHandler`
- [ ] Aplicar correção em todos os handlers restantes

**Ver:** `documentation/ANALISE-JOBCANCELLATIONEXCEPTION.md`

---

### 5. ⚠️ s6.f0 (Código Ofuscado)

**ID:** `f07e31a9d9886adad05d48c640e27e9e`  
**Eventos:** 10  
**Usuários:** 2  
**Status:** 🟡 **REQUER CONFIGURAÇÃO**

**Problema:**
- Erro ofuscado pelo R8/ProGuard
- Sem mapeamento de símbolos, não é possível identificar origem

**Ação Necessária:**
- [ ] Configurar upload automático de `mapping.txt` para Crashlytics
- [ ] Verificar se `mapping.txt` está sendo gerado no build de release
- [ ] Configurar Firebase CLI para fazer upload do mapping

**Configuração Necessária:**
```gradle
android {
    buildTypes {
        release {
            // mapping.txt é gerado automaticamente
            // Firebase CLI deve fazer upload
        }
    }
}
```

---

## 📋 Priorização de Correções

### 🔴 URGENTE (Fazer Agora)
1. **DialogAditivoEquipamentosBinding** - 13 eventos, 11 usuários
2. **AditivoDialog.onCreateDialog** - 2 eventos, 1 usuário

### 🟡 IMPORTANTE (Esta Semana)
3. **SyncRepository.mapType** - Verificar se regras ProGuard estão funcionando
4. **JobCancellationException** - Aplicar correção em todos os handlers

### 🟢 MELHORIAS (Próxima Semana)
5. **s6.f0 (Ofuscado)** - Configurar upload de mapping.txt

---

## 🔧 Próximos Passos

1. **Corrigir erros FATAL:**
   - [ ] Investigar e corrigir `DialogAditivoEquipamentosBinding`
   - [ ] Corrigir tema AppCompat em `AditivoDialog`

2. **Melhorar tratamento de exceções:**
   - [ ] Aplicar tratamento de `CancellationException` em todos os handlers
   - [ ] Adicionar logs mais detalhados para debug

3. **Configurar Crashlytics:**
   - [ ] Configurar upload automático de `mapping.txt`
   - [ ] Verificar se stack traces estão legíveis

---

## 📊 Métricas

**Total de Erros Ativos:** 5  
**Erros FATAL:** 3  
**Erros NON_FATAL:** 2  
**Total de Eventos:** 51  
**Total de Usuários Impactados:** 27

---

**Última Atualização:** 02 de Janeiro de 2026
