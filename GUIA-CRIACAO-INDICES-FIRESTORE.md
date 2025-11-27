# 🔧 Guia: Criação de Índices Compostos no Firestore

## 📋 Resumo

Os índices compostos do Firestore **NÃO podem ser criados via código**. Eles devem ser criados manualmente no Firebase Console ou via arquivo de configuração `firestore.indexes.json`.

---

## 🎯 Opção 1: Criação Automática via Firebase CLI (RECOMENDADO)

### Pré-requisitos
1. Firebase CLI instalado: `npm install -g firebase-tools`
2. Projeto Firebase inicializado: `firebase init firestore`

### Passos

1. **Criar arquivo de configuração** (já criado: `firestore.indexes.json`)
   - O arquivo já está na raiz do projeto com todos os índices necessários

2. **Fazer login no Firebase:**
   ```bash
   firebase login
   ```

3. **Inicializar Firestore (se ainda não fez):**
   ```bash
   firebase init firestore
   ```
   - Selecione seu projeto
   - Escolha usar o arquivo `firestore.indexes.json` existente

4. **Deploy dos índices:**
   ```bash
   firebase deploy --only firestore:indexes
   ```

5. **Aguardar criação:**
   - Os índices podem levar alguns minutos para serem criados
   - Verifique o status no Firebase Console: https://console.firebase.google.com/project/gestaobilhares/firestore/indexes

---

## 🎯 Opção 2: Criação Manual no Firebase Console

### Passos

1. **Acesse o Firebase Console:**
   - https://console.firebase.google.com/project/gestaobilhares/firestore/indexes

2. **Clique em "Criar Índice"**

3. **Para cada índice, configure:**

   **Índice 1: Acertos por clienteId + dataAcerto**
   - Collection Group: `items` (aplica a todas as subcoleções `items` de todas as entidades)
   - Campo 1: `clienteId` (Ascending)
   - Campo 2: `dataAcerto` (Descending)
   - Query scope: Collection Group

   **Índice 2: Acertos por cliente_id + dataAcerto**
   - Collection Group: `items`
   - Campo 1: `cliente_id` (Ascending)
   - Campo 2: `dataAcerto` (Descending)
   - Query scope: Collection Group

   **Índice 3: Acertos por clienteID + dataAcerto**
   - Collection Group: `items`
   - Campo 1: `clienteID` (Ascending)
   - Campo 2: `dataAcerto` (Descending)
   - Query scope: Collection Group

   **Índice 4: Sincronização Incremental (lastModified)**
   - Collection Group: `items`
   - Campo 1: `lastModified` (Ascending)
   - Query scope: Collection Group
   - **Nota:** Este índice é necessário para todas as coleções que usam sincronização incremental (clientes, acertos, despesas, mesas)

4. **Aguardar criação:**
   - Os índices aparecem como "Building" inicialmente
   - Podem levar 5-15 minutos para ficarem prontos
   - Você receberá um email quando estiverem prontos

---

## 🎯 Opção 3: Usar Links dos Logs (MAIS RÁPIDO)

Quando o app tentar fazer uma query que requer índice, o Firestore retorna um erro com um link direto para criar o índice:

1. **Execute o app e tente buscar mais de 3 acertos**
2. **Verifique os logs do logcat**
3. **Procure por mensagens como:**
   ```
   ⚠️ Campo 'clienteId' sem índice para consulta: FAILED_PRECONDITION
   You can create it here: https://console.firebase.google.com/...
   ```
4. **Clique no link** (ou copie e cole no navegador)
5. **Clique em "Criar Índice"** na página que abrir
6. **Aguarde a criação**

---

## 📊 Índices Necessários

### Para Busca de Acertos por Cliente

| Índice | Campos | Uso |
|--------|--------|-----|
| `items_clienteId_dataAcerto` | `clienteId` (ASC) + `dataAcerto` (DESC) | Buscar últimos N acertos de um cliente |
| `items_cliente_id_dataAcerto` | `cliente_id` (ASC) + `dataAcerto` (DESC) | Fallback para formato antigo |
| `items_clienteID_dataAcerto` | `clienteID` (ASC) + `dataAcerto` (DESC) | Fallback para formato alternativo |

### Para Sincronização Incremental

| Índice | Campos | Uso |
|--------|--------|-----|
| `items_lastModified` | `lastModified` (ASC) | Sincronização incremental de todas as entidades |

**Nota:** O índice `lastModified` é necessário para:
- ✅ Clientes (já implementado)
- ✅ Acertos (já implementado)
- ✅ Despesas (já implementado)
- ✅ Mesas (já implementado)

---

## 🔍 Verificar Índices Existentes

1. Acesse: https://console.firebase.google.com/project/gestaobilhares/firestore/indexes
2. Veja a lista de índices criados
3. Status pode ser:
   - ✅ **Enabled**: Índice pronto e funcionando
   - 🔄 **Building**: Índice sendo criado (aguarde)
   - ❌ **Error**: Erro na criação (verifique os campos)

---

## ⚠️ Importante

1. **Estrutura do Firestore:**
   - Caminho completo: `empresas/empresa_001/entidades/{collectionName}/items`
   - Collection Group nos índices: `items` (aplica a todas as subcoleções `items` de todas as entidades)
   - Exemplo: O índice `items_clienteId_dataAcerto` funciona para:
     - `empresas/empresa_001/entidades/acertos/items`
     - `empresas/empresa_001/entidades/clientes/items`
     - Qualquer outra entidade que tenha subcoleção `items`

2. **Query Scope:**
   - Use **"Collection Group"** para aplicar o índice a todas as subcoleções `items`
   - Como a estrutura é `empresas/empresa_001/entidades/{entidade}/items`, o `collectionGroup: "items"` aplica o índice a todas as entidades

3. **Tempo de Criação:**
   - Índices pequenos: 2-5 minutos
   - Índices grandes: 10-30 minutos
   - Você receberá um email quando estiverem prontos

4. **Custo:**
   - Índices compostos são **gratuitos** no Firestore
   - Não há custo adicional

---

## 🚀 Recomendação

**Use a Opção 1 (Firebase CLI)** se:
- ✅ Você tem Firebase CLI instalado
- ✅ Quer automatizar a criação
- ✅ Quer versionar os índices no Git

**Use a Opção 3 (Links dos Logs)** se:
- ✅ Quer criar rapidamente apenas os índices necessários
- ✅ Não quer instalar Firebase CLI
- ✅ Prefere criar manualmente conforme necessário

---

## 📝 Arquivo Criado

O arquivo `firestore.indexes.json` foi criado na raiz do projeto com todos os índices necessários. Você pode:

1. **Deploy via CLI:**
   ```bash
   firebase deploy --only firestore:indexes
   ```

2. **Ou copiar manualmente** para o Firebase Console se preferir

---

## ✅ Após Criar os Índices

1. Aguarde a criação completar (verifique no Console)
2. Teste o app novamente
3. O botão "Mais" deve funcionar sem erros de índice
4. A sincronização incremental será mais rápida

---

## 🔗 Links Úteis

- Firebase Console: https://console.firebase.google.com/project/gestaobilhares
- Firestore Indexes: https://console.firebase.google.com/project/gestaobilhares/firestore/indexes
- Documentação Oficial: https://firebase.google.com/docs/firestore/query-data/indexing

