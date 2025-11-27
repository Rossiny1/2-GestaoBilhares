# 📋 Guia de Teste: Sincronização Incremental de Clientes

## 🎯 Objetivo

Validar que a sincronização incremental de clientes está funcionando corretamente, reduzindo o uso de dados móveis em 95%+ e melhorando a performance.

---

## 📱 Como Testar

### **Opção 1: Usando o Script PowerShell (Recomendado)**

1. **Conecte o dispositivo Android via USB** e habilite a depuração USB
2. **Execute o script:**
   ```powershell
   .\testar-sincronizacao-incremental-clientes.ps1
   ```
3. **No app**, vá para a tela de **Rotas** e clique no **botão de sincronização** (ícone de sincronização no canto superior direito)
4. **Observe os logs** em tempo real no terminal

### **Opção 2: Usando ADB Manualmente**

1. **Conecte o dispositivo** via USB
2. **Limpe os logs anteriores:**
   ```powershell
   adb logcat -c
   ```
3. **Monitore os logs:**
   ```powershell
   adb logcat -v time | Select-String -Pattern "SyncRepository|pullClientes|INCREMENTAL|COMPLETA|Lote"
   ```
4. **No app**, clique no botão de sincronização
5. **Observe os logs** no terminal

---

## ✅ O Que Observar nos Logs

### **1. Primeira Sincronização (Esperado)**

```
🔄 Primeira sincronização COMPLETA para clientes
📄 Lote #1 processado: 500 documentos (total: 500)
📄 Lote #2 processado: 500 documentos (total: 1000)
✅ Paginação concluída: 1500 documentos processados em 3 lote(s)
✅ Pull Clientes (COMPLETA) concluído:
   📊 1500 sincronizados, 0 pulados, 0 erros
   📥 1500 documentos processados
   ⏱️ Duração: 5000ms
```

**✅ Significa:** Primeira sincronização funcionou corretamente. Todos os clientes foram baixados.

---

### **2. Segunda Sincronização (Incremental - Esperado)**

```
🔄 Sincronização INCREMENTAL para clientes (desde 2025-01-15 10:30:00)
📄 Lote #1 processado: 5 documentos (total: 5)
✅ Paginação concluída: 5 documentos processados em 1 lote(s)
✅ Pull Clientes (INCREMENTAL) concluído:
   📊 5 sincronizados, 0 pulados, 0 erros
   📥 5 documentos processados
   ⏱️ Duração: 500ms
```

**✅ Significa:** Sincronização incremental funcionando! Apenas 5 clientes novos/atualizados foram baixados (vs 1500 na primeira vez).

**🎉 Redução de dados:** ~99.7% (5 vs 1500 documentos)

---

### **3. Sincronização Incremental Sem Novos Dados (Esperado)**

```
🔄 Sincronização INCREMENTAL para clientes (desde 2025-01-15 10:35:00)
✅ Paginação concluída: 0 documentos processados em 0 lote(s)
✅ Pull Clientes (INCREMENTAL) concluído:
   📊 0 sincronizados, 0 pulados, 0 erros
   📥 0 documentos processados
   ⏱️ Duração: 200ms
```

**✅ Significa:** Nenhum cliente foi modificado desde a última sincronização. Perfeito!

---

### **4. Erro: Índice Não Existe (Fallback Automático)**

```
⚠️ Erro ao criar query incremental: The query requires an index
⚠️ Possível causa: índice composto não existe no Firestore
⚠️ Usando sincronização completa como fallback
✅ Pull Clientes (COMPLETA - fallback) concluído:
   📊 1500 sincronizados, 0 pulados, 0 erros
```

**⚠️ Significa:** O índice composto não existe no Firestore. O sistema usou fallback automático.

**🔧 Solução:** Criar o índice no Firestore Console (veja seção "Criar Índice no Firestore" abaixo).

---

## 🔍 Validações Adicionais

### **1. Verificar Metadata de Sincronização**

Execute no terminal (com dispositivo conectado):

```powershell
adb shell "run-as com.example.gestaobilhares sqlite3 /data/data/com.example.gestaobilhares/databases/app_database.db 'SELECT * FROM sync_metadata WHERE entity_type = \"clientes\";'"
```

**Resultado esperado:**
```
clientes|1737028800000|1500|5000|0|NULL|1737028800000
```

**Campos:**
- `entity_type`: "clientes"
- `last_sync_timestamp`: Timestamp da última sincronização
- `last_sync_count`: Quantidade de registros sincronizados
- `last_sync_duration_ms`: Duração em milissegundos
- `last_sync_bytes_downloaded`: Bytes baixados (0 se não calculado)
- `last_error`: NULL se sem erros

---

### **2. Comparar Tempos de Sincronização**

| Tipo | Primeira Sync | Segunda Sync (Incremental) | Redução |
|------|---------------|----------------------------|---------|
| **Tempo** | ~5000ms | ~500ms | **90%** |
| **Documentos** | 1500 | 5 | **99.7%** |
| **Dados** | ~7MB | ~100KB | **98.6%** |

---

### **3. Testar Cenários**

#### **Cenário A: Primeira Sincronização**
1. Limpe os dados do app (desinstale e reinstale, ou limpe os dados)
2. Execute a sincronização
3. **Esperado:** "Primeira sincronização COMPLETA"

#### **Cenário B: Sincronização Incremental**
1. Após a primeira sincronização, aguarde alguns segundos
2. Execute a sincronização novamente
3. **Esperado:** "Sincronização INCREMENTAL" com poucos documentos

#### **Cenário C: Atualizar Cliente no Firestore**
1. No Firestore Console, atualize um cliente (ex: mude o nome)
2. Execute a sincronização
3. **Esperado:** Apenas 1 cliente sincronizado (o atualizado)

#### **Cenário D: Criar Novo Cliente no Firestore**
1. No Firestore Console, crie um novo cliente
2. Execute a sincronização
3. **Esperado:** Apenas 1 cliente sincronizado (o novo)

---

## 🔧 Criar Índice no Firestore (Se Necessário)

Se você ver o erro "The query requires an index", siga estes passos:

1. **Acesse o Firestore Console:** https://console.firebase.google.com
2. **Selecione seu projeto**
3. **Vá para Firestore Database → Indexes**
4. **Clique em "Create Index"**
5. **Configure o índice:**
   - **Collection ID:** `clientes` (ou o nome da sua coleção)
   - **Fields to index:**
     - Campo: `lastModified` (ou `dataUltimaAtualizacao`)
     - Ordenação: `Ascending`
   - **Query scope:** `Collection`
6. **Clique em "Create"**
7. **Aguarde o índice ser criado** (pode levar alguns minutos)

**Alternativa:** O Firestore pode gerar um link automático no log. Clique no link para criar o índice automaticamente.

---

## 📊 Métricas de Sucesso

### ✅ **Sincronização Incremental Funcionando:**
- [ ] Log mostra "Sincronização INCREMENTAL"
- [ ] Apenas documentos novos/atualizados são baixados
- [ ] Tempo de sincronização reduzido em 90%+
- [ ] Metadata de sincronização salva corretamente
- [ ] Paginação funcionando (lotes de 500 documentos)

### ⚠️ **Problemas Comuns:**

| Problema | Causa | Solução |
|----------|-------|---------|
| Sempre "Primeira sincronização" | Metadata não está sendo salva | Verificar `saveSyncMetadata()` |
| Erro "índice não existe" | Índice não criado no Firestore | Criar índice (veja seção acima) |
| Sincronização lenta | Paginação não funcionando | Verificar `executePaginatedQuery()` |
| Nenhum log aparece | Filtros muito restritivos | Ajustar filtros no script |

---

## 🐛 Debug

Se algo não estiver funcionando, execute:

```powershell
# Ver todos os logs do SyncRepository
adb logcat -v time | Select-String -Pattern "SyncRepository"

# Ver logs específicos de clientes
adb logcat -v time | Select-String -Pattern "pullClientes|clientes"

# Ver erros
adb logcat -v time *:E | Select-String -Pattern "SyncRepository|pullClientes"
```

---

## 📝 Checklist de Teste

- [ ] Primeira sincronização completa funcionou
- [ ] Segunda sincronização foi incremental
- [ ] Apenas documentos novos/atualizados foram baixados
- [ ] Tempo de sincronização reduziu significativamente
- [ ] Metadata de sincronização foi salva
- [ ] Paginação funcionou (múltiplos lotes se necessário)
- [ ] Fallback para sincronização completa funcionou (se índice não existe)
- [ ] Nenhum erro crítico nos logs

---

## 🎉 Resultado Esperado

Após implementar a sincronização incremental, você deve ver:

- **Redução de 95%+ no uso de dados móveis**
- **Sincronização 10x mais rápida**
- **Menor consumo de bateria**
- **Melhor experiência do usuário**

---

## 📞 Suporte

Se encontrar problemas, verifique:
1. Logs do script de teste
2. Logs do ADB
3. Metadata de sincronização no banco de dados
4. Índices no Firestore Console

