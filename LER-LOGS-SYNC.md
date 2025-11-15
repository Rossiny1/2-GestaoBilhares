# 📋 GUIA DE USO - LEITURA DE LOGS DE SINCRONIZAÇÃO

## 🚀 SCRIPTS DISPONÍVEIS

### 1. **ler-logs-sync.ps1** (Recomendado)
Script completo com opções avançadas.

**Uso básico:**
```powershell
.\scripts\ler-logs-sync.ps1
```

**Opções:**
```powershell
# Limpar logs antes de iniciar
.\scripts\ler-logs-sync.ps1 -Clear

# Mostrar últimas 50 linhas antes de monitorar
.\scripts\ler-logs-sync.ps1 -Lines 50

# Combinar opções
.\scripts\ler-logs-sync.ps1 -Clear -Lines 100
```

**Características:**
- ✅ Filtra logs por tags relevantes (SyncRepository, RoutesFragment, etc)
- ✅ Cores para diferentes tipos de log (erro, sucesso, aviso)
- ✅ Mostra últimas linhas antes de monitorar
- ✅ Opção de limpar logs anteriores

---

### 2. **ler-logs-sync-simples.ps1** (Mais rápido)
Versão simplificada e mais rápida.

**Uso:**
```powershell
.\scripts\ler-logs-sync-simples.ps1
```

**Características:**
- ✅ Mais leve e rápido
- ✅ Filtra apenas logs relevantes
- ✅ Cores básicas
- ✅ Ideal para uso rápido

---

### 3. **ler-logs-sync-completo.ps1** (Com salvamento)
Versão completa com opção de salvar logs em arquivo.

**Uso básico:**
```powershell
.\scripts\ler-logs-sync-completo.ps1
```

**Salvar logs em arquivo:**
```powershell
# Salvar com nome automático (timestamp)
.\scripts\ler-logs-sync-completo.ps1 -SaveToFile

# Salvar com nome específico
.\scripts\ler-logs-sync-completo.ps1 -SaveToFile -OutputFile "meus_logs.txt"

# Limpar logs e salvar
.\scripts\ler-logs-sync-completo.ps1 -Clear -SaveToFile
```

**Características:**
- ✅ Todas as funcionalidades do script completo
- ✅ Salva logs em arquivo texto
- ✅ Útil para análise posterior
- ✅ Nome automático com timestamp

---

## 📱 PRÉ-REQUISITOS

1. **Dispositivo Android conectado via USB**
2. **USB Debugging habilitado**
3. **ADB instalado** (Android SDK Platform Tools)
   - Caminho padrão: `C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe`
   - Se estiver em outro local, edite o script e altere `$adbPath`

---

## 🔍 TAGS MONITORADAS

Os scripts filtram logs das seguintes tags:

- `SyncRepository` - Logs do repositório de sincronização
- `RoutesFragment` - Logs da tela de rotas
- `SyncWorker` - Logs do worker de sincronização
- `SyncManager` - Logs do gerenciador de sincronização
- `AppRepository` - Logs do repositório principal
- `RoutesViewModel` - Logs do ViewModel de rotas

---

## 🎨 CORES DOS LOGS

- 🔴 **Vermelho**: Erros (ERROR, ❌, Falhou)
- 🟡 **Amarelo**: Avisos (WARN, ⚠️, Pulando)
- 🟢 **Verde**: Sucessos (SUCCESS, ✅, INSERIDO, ATUALIZADO)
- 🔵 **Ciano**: Informações (INFO, 🔵, 📥, 📄, 🔄, 📊)
- ⚪ **Branco/Cinza**: Outros logs

---

## 📝 EXEMPLO DE USO

### Cenário 1: Testar sincronização pela primeira vez

```powershell
# 1. Limpar logs anteriores
.\scripts\ler-logs-sync.ps1 -Clear

# 2. Iniciar monitoramento
.\scripts\ler-logs-sync.ps1

# 3. No app, executar sincronização
# 4. Observar logs em tempo real
```

### Cenário 2: Salvar logs para análise

```powershell
# 1. Iniciar monitoramento salvando em arquivo
.\scripts\ler-logs-sync-completo.ps1 -SaveToFile

# 2. Executar sincronização no app
# 3. Parar monitoramento (Ctrl+C)
# 4. Analisar arquivo gerado: logs_sync_YYYYMMDD_HHMMSS.txt
```

### Cenário 3: Ver últimas linhas e continuar monitorando

```powershell
# Mostrar últimas 100 linhas e continuar monitorando
.\scripts\ler-logs-sync.ps1 -Lines 100
```

---

## 🐛 TROUBLESHOOTING

### Erro: "ADB não encontrado"
- Verifique se o Android SDK está instalado
- Edite o script e altere `$adbPath` para o caminho correto

### Erro: "Nenhum dispositivo conectado"
- Conecte o dispositivo via USB
- Habilite USB Debugging nas opções de desenvolvedor
- Execute: `adb devices` para verificar

### Logs não aparecem
- Verifique se o app está rodando
- Verifique se a sincronização foi executada
- Tente limpar logs: `adb logcat -c`

### Script muito lento
- Use `ler-logs-sync-simples.ps1` para versão mais rápida
- Reduza o número de tags monitoradas

---

## 📊 LOGS ESPERADOS (SUCESSO)

Quando a sincronização funciona corretamente, você verá:

```
🔄 ========== INICIANDO SINCRONIZAÇÃO PULL ==========
✅ Dispositivo online - prosseguindo com sincronização
📡 Conectando ao Firestore...
🔵 Iniciando pull de clientes...
📥 Total de documentos recebidos do Firestore: 5
📄 Processando cliente ID: 1, Nome: Cliente 1
✅ Cliente INSERIDO: Cliente 1 (ID: 1)
📄 Processando cliente ID: 2, Nome: Cliente 2
✅ Cliente INSERIDO: Cliente 2 (ID: 2)
✅ Pull Clientes concluído: 5 sincronizados, 0 pulados, 0 erros
✅ ========== SINCRONIZAÇÃO PULL CONCLUÍDA ==========
📊 Total sincronizado: 5 itens
```

---

## 💡 DICAS

1. **Execute o script ANTES de iniciar a sincronização** para capturar todos os logs
2. **Use `-Clear`** para limpar logs antigos e focar apenas na sincronização atual
3. **Use `-SaveToFile`** para salvar logs e analisar depois
4. **Combine com Database Inspector** do Android Studio para verificar se dados foram salvos
5. **Pressione Ctrl+C** para parar o monitoramento a qualquer momento

---

## 🔗 RELACIONADO

- `DIAGNOSTICO-SINCRONIZACAO.md` - Guia completo de diagnóstico
- Documentação do projeto em `.cursor/rules/`

