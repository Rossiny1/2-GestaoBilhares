# 🚀 Setup Rápido - Importação CSV com Firebase CLI

## ✅ Status Atual

- **Firebase CLI**: ✅ v14.26.0 instalado
- **Projeto**: ✅ gestaobilhares configurado
- **Arquivos**: ✅ 1 CSV disponível (037-Salinas)

## 🎯 Melhor Alternativa: Firebase CLI + Script Node.js

Como você já tem Firebase CLI, vamos usar a abordagem **recomendada oficialmente**:

### **Vantagens:**

- ✅ **Best Practice Firebase** (Admin SDK)
- ✅ **Independente do app** (não afeta production)
- ✅ **Execução imediata** (sem build)
- ✅ **Setup mínimo** (já tem Firebase CLI)

---

## 🛠️ Setup (5 minutos)

### 1. **Verificar Node.js**

```bash
node --version
# Se não tiver, instale de: https://nodejs.org
```

### 2. **Instalar Dependências**

```bash
cd import-data
npm install
```

### 3. **Baixar Service Account**

1. Vá: <https://console.firebase.google.com/project/gestaobilhares/settings/serviceaccounts/adminsdk>
2. Clique: **"Gerar nova chave privada"**
3. Salve como: `service-account.json` (na pasta import-data)

---

## 🚀 Execução Imediata

### **Teste com arquivo atual:**

```bash
cd import-data
npm start
```

### **Resultado esperado:**

```
🚀 Iniciando Importação de Clientes - Firebase Admin SDK
============================================================
📦 Projeto: gestaobilhares
📁 Arquivos a processar: 1
⏰ Início: 09/01/2026 12:15:00

📁 Processando arquivo: anexos/Cadastro Clientes- Rota Bahia.csv
🎯 Rota destino: 037-Salinas
✅ Rota criada: 037-Salinas (ID: abc123)
📊 Encontradas 114 linhas no CSV
⏳ Progresso: 10/114 clientes processados
...
✅ Importação concluída!
📊 Resultados:
   👥 Clientes importados: 114
   ❌ Erros: 0
   ⏱️  Tempo total: 3.45s
```

---

## 📊 Arquivos Mantidos

Apenas os essenciais:

```
import-data/
├── README_IMPORTACAO.md     # Guia completo
├── csv_analysis.md          # Análise do CSV
├── importar_clientes.js     # Script principal
├── package.json             # Dependências
└── service-account.json     # Firebase credentials (criar)
```

---

## 🎯 Próximos Passos

1. **Setup agora** (5 min)
2. **Teste com arquivo atual** (2 min)
3. **Adicionar outros 6 arquivos** quando disponíveis
4. **Importação completa** (~5 min total)

**Pronto para começar? Execute os comandos acima!**
