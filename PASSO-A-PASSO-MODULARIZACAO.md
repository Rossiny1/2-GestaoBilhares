# 📋 PASSO A PASSO - MODULARIZAÇÃO COMPLETA

## ⚠️ IMPORTANTE
- **FECHE O ANDROID STUDIO/IDE** antes de executar os scripts
- Execute os scripts **na ordem apresentada**
- Após cada script, verifique se houve erros
- **NÃO execute o build** até completar todos os passos

---

## 📝 PASSO 1: Migrar Módulo :sync

Execute o script para migrar arquivos de sincronização e workers:

```powershell
.\migrate-sync-module.ps1
```

**O que faz:**
- Copia `app/src/main/java/.../sync/` → `sync/src/main/java/.../sync/`
- Copia `app/src/main/java/.../workers/` → `sync/src/main/java/.../workers/`

**Verificação:**
- Verifique se apareceu "✅ Migração :sync concluída"
- Confirme o número de arquivos copiados

---

## 📝 PASSO 2: Migrar Módulo :ui

Execute o script para migrar arquivos de UI:

```powershell
.\migrate-ui-module.ps1
```

**O que faz:**
- Copia `app/src/main/java/.../ui/` → `ui/src/main/java/.../ui/`
- Mantém toda a estrutura de diretórios

**Verificação:**
- Verifique se apareceu "✅ Migração :ui concluída"
- Confirme o número de arquivos copiados

---

## 📝 PASSO 3: Corrigir Imports

Execute o script para corrigir imports nos arquivos migrados:

```powershell
.\fix-imports-after-migration.ps1
```

**O que faz:**
- Corrige imports de `utils` → `core.utils` no módulo :ui
- Corrige imports de `workers` → `sync.workers` no módulo :app
- Atualiza referências de módulos migrados

**Verificação:**
- Verifique se apareceu "✅ Correção de imports concluída"
- Confirme quantos arquivos foram corrigidos

---

## 📝 PASSO 4: Limpar Build Antigo

Execute para limpar builds antigos e evitar conflitos:

```powershell
.\fix-file-lock.ps1
```

**O que faz:**
- Para o Gradle daemon
- Para processos Java/Gradle
- Remove diretórios `build/` dos módulos
- Limpa o build

**Verificação:**
- Verifique se não houve erros
- Confirme que os diretórios `build/` foram removidos

---

## 📝 PASSO 5: Verificar Estrutura

Verifique se os arquivos foram migrados corretamente:

```powershell
# Verificar módulo :sync
Get-ChildItem -Path "sync\src\main\java" -Recurse -File -Filter "*.kt" | Measure-Object | Select-Object -ExpandProperty Count

# Verificar módulo :ui
Get-ChildItem -Path "ui\src\main\java" -Recurse -File -Filter "*.kt" | Measure-Object | Select-Object -ExpandProperty Count
```

**Verificação:**
- Módulo :sync deve ter pelo menos 3 arquivos (SyncManagerV2, SyncWorker, CleanupWorker)
- Módulo :ui deve ter muitos arquivos (fragments, ViewModels, adapters)

---

## 📝 PASSO 6: Atualizar Dependências (JÁ FEITO)

✅ Os arquivos `build.gradle.kts` já foram atualizados:
- `app/build.gradle.kts` - inclui módulos :sync e :ui
- `sync/build.gradle.kts` - dependências corretas
- `ui/build.gradle.kts` - dependências corretas

**Não precisa fazer nada neste passo.**

---

## 📝 PASSO 7: Executar Build

Agora você pode executar o build:

```powershell
.\gradlew assembleDebug
```

**O que esperar:**
- O build pode demorar mais na primeira vez (compilação completa)
- Builds incrementais serão mais rápidos
- Se houver erros, anote-os e me informe

---

## 🔧 RESOLUÇÃO DE PROBLEMAS

### Erro: "Unresolved reference"
- Verifique se os imports foram corrigidos (PASSO 3)
- Verifique se os módulos estão no `settings.gradle.kts`

### Erro: "File lock" ou "Access denied"
- Execute `.\fix-file-lock.ps1` novamente
- Feche o Android Studio/IDE
- Execute como administrador se necessário

### Erro: "Circular dependency"
- Verifique se não há dependências circulares entre módulos
- Módulos devem depender apenas de :core e :data
- :app pode depender de todos os módulos

### Erro: "Build failed"
- Verifique os logs completos
- Procure por erros específicos de compilação
- Me informe os erros para correção

---

## ✅ CHECKLIST FINAL

Antes de considerar a modularização completa:

- [ ] Módulo :sync migrado e funcionando
- [ ] Módulo :ui migrado e funcionando
- [ ] Imports corrigidos
- [ ] Build executado com sucesso
- [ ] App compila sem erros
- [ ] Nenhum arquivo duplicado entre módulos

---

## 📞 PRÓXIMOS PASSOS

Após o build bem-sucedido:

1. **Testar o app** - Verificar se todas as funcionalidades funcionam
2. **Remover arquivos duplicados** - Deletar pastas antigas do módulo :app
3. **Commit** - Fazer commit das mudanças
4. **Continuar desenvolvimento** - Prosseguir com outras funcionalidades

---

**Boa sorte! 🚀**

