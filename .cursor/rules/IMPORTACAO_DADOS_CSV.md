# 📋 Guia de Importação de Dados CSV - Gestão Bilhares

## 🎯 **Objetivo**

Importar dados de clientes de arquivos CSV para o Firebase Firestore, garantindo compatibilidade total com o aplicativo Android Gestão Bilhares.

---

## 📁 **Estrutura da Pasta Import-Data**

A pasta `import-data/` contém apenas os arquivos essenciais:

```
import-data/
├── importar_automatico.js          # Script principal de importação
├── service-account.json             # Chave de acesso ao Firebase
├── package.json                   # Dependências Node.js
├── node_modules/                  # Dependências instaladas
└── INSTRUCAO_SERVICE_ACCOUNT.md   # Instruções da chave
```

**Nota**: `package-lock.json` pode ser deletado e regenerado com `npm install`

---

## 🚀 **Metodologia Atual**

### **1. Arquivo Principal: `importar_automatico.js`**

Script robusto que:

- ✅ **Lê CSV com codificação UTF-8** (preserva acentos)
- ✅ **Gera IDs numéricos sequenciais** (compatível com app)
- ✅ **Usa estrutura snake_case** (rota_id, cpf_cnpj, etc.)
- ✅ **Cria rotas automaticamente** se não existirem
- ✅ **Importa para caminho correto**: `empresas/empresa_001/entidades/clientes/items`

### **2. Estrutura de Dados Esperada**

```javascript
{
  id: 123456,                    // ID numérico sequencial
  nome: "JOÃO DA SILVA",          // UTF-8 com acentos
  nome_fantasia: null,             // snake_case
  cpf_cnpj: "123.456.789-01",    // snake_case
  telefone: "(11) 98765-4321",
  endereco: "RUA DAS ÁRVORES, 123",
  cidade: "SÃO PAULO",
  estado: "SP",
  rota_id: 789012,               // ID numérico da rota
  debito_atual: 150.00,           // snake_case, número
  ativo: true,                    // booleano
  data_cadastro: 1704214134000,    // timestamp numérico
  data_ultima_atualizacao: 1704214134000
}
```

### **3. Caminhos no Firestore**

- **Rotas**: `empresas/empresa_001/entidades/rotas/items`
- **Clientes**: `empresas/empresa_001/entidades/clientes/items`

---

## ⚙️ **Configuração**

### **Pré-requisitos**

1. **Node.js** instalado
2. **Chave do Firebase** em `service-account.json`
3. **Arquivo CSV** na pasta `../anexos/`

### **Instalação de Dependências**

```bash
cd import-data
npm install
```

---

## 🚀 **Execução**

### **Comando Único**

```bash
node importar_automatico.js
```

### **O que o script faz:**

1. **Conecta ao Firebase** usando a chave
2. **Lê o arquivo CSV** com codificação UTF-8
3. **Cria ou encontra a rota** especificada
4. **Importa clientes** com IDs numéricos sequenciais
5. **Preserva acentos** e caracteres especiais
6. **Mostra progresso** em tempo real

---

## 📊 **Resultados Esperados**

### **Exemplo de Saída**

```bash
✅ Firebase Admin configurado com sua chave!
🚀 IMPORTAÇÃO AUTOMÁTICA - FIREBASE ADMIN SDK
============================================================
📁 Processando arquivo: ../anexos/Cadastro Clientes- Rota Bahia.csv
🎯 Rota destino: 037-Salinas
🆕 Rota criada: 037-Salinas (ID: 500287)
📝 Arquivo lido como UTF-8 (simples)
📊 Encontradas 114 linhas no CSV
⏳ Progresso: 50/113 clientes processados
✅ Importação concluída!
📊 Resultados:
   👥 Clientes importados: 112
   ❌ Erros: 0
   ⏱️  Tempo total: 15.47s
   🚀 Média: 138ms/cliente
```

---

## 🔧 **Configuração de Arquivos**

### **Mapeamento de Arquivos**

No script `importar_automatico.js`, configure o array `arquivosParaRotas`:

```javascript
const arquivosParaRotas = [
    {
        arquivo: '../anexos/Cadastro Clientes- Rota Bahia.csv',
        rota: '037-Salinas',
        descricao: 'Rota Salinas - Importação CSV'
    },
    // Adicione outros arquivos aqui:
    // {
    //     arquivo: '../anexos/Cadastro Clientes- OutraRota.csv',
    //     rota: 'XXX-NomeRota',
    //     descricao: 'Descrição da rota'
    // }
];
```

---

## 🇧🇷 **Suporte a Caracteres**

### **Codificação**

- **Leitura**: UTF-8 (preserva acentos brasileiros)
- **Caracteres suportados**: á, é, í, ó, ú, ã, õ, ç, ñ, ü, etc.
- **Sem conversões forçadas** (evita caracteres especiais)

### **Se caracteres aparecerem errados:**

1. Abra o CSV em um editor
2. **Salve como UTF-8** explicitamente
3. Execute a importação novamente

---

## 📱 **Validação no App**

### **Passos para Verificar:**

1. **Abra o app** Gestão Bilhares
2. **Vá em "Rotas"**
3. **Procure a rota** importada (ex: "037-Salinas")
4. **Clique na rota** para ver clientes
5. **Verifique se:**
   - ✅ Nomes aparecem com acentos corretos
   - ✅ Quantidade de clientes corresponde
   - ✅ Dados estão completos

---

## 🚨 **Solução de Problemas**

### **Problema: Clientes não aparecem no app**

**Causa**: Dados no Firestore mas app não sincroniza

**Solução**:

1. **Limpe cache do app** (configurações > armazenamento > limpar cache)
2. **Force sincronização** (pull-to-refresh na tela de rotas)
3. **Reinicie o app** completamente

### **Problema: Caracteres especiais**

**Causa**: Codificação incorreta do CSV

**Solução**:

1. Abra o CSV no Excel/Google Sheets
2. **Salve como CSV UTF-8**
3. Execute importação novamente

### **Problema: Erro de importação**

**Causa**: Arquivo não encontrado ou permissões

**Solução**:

1. Verifique se o arquivo existe em `../anexos/`
2. Confirme a chave `service-account.json` está correta
3. Execute com `node importar_automatico.js` na pasta `import-data/`

---

## 📋 **Checklist Final**

Antes de executar:

- [ ] Node.js instalado
- [ ] Chave `service-account.json` configurada
- [ ] Arquivo CSV na pasta `../anexos/`
- [ ] Dependências instaladas (`npm install`)

Após executar:

- [ ] Importação concluída sem erros
- [ ] Rota criada/encontrada
- [ ] Clientes importados com IDs numéricos
- [ ] Acentos preservados corretamente
- [ ] Dados visíveis no app Android

---

## 🎯 **Resumo**

**Metodologia atual:**

- ✅ **Um script principal** (`importar_automatico.js`)
- ✅ **Leitura UTF-8 simples** (preserva acentos)
- ✅ **Estrutura compatível** com app Android
- ✅ **IDs numéricos** sequenciais
- ✅ **Caminhos corretos** no Firestore
- ✅ **Zero dependências desnecessárias**

**Resultado:** Importação 100% funcional e compatível! 🎉

- **Documentos por segundo**: 10,000
- **Tamanho documento**: 1MB
- **Batch writes**: 500 operações

## 🎯 **Próximos Passos**

### **Após Teste Bem-Sucedido:**

#### **1. Importar Todos os Arquivos**

1. Adicione os outros 7 arquivos CSV na pasta `anexos/`
2. Execute: `node importar_clientes.js`
3. Monitore o progresso no console

#### **2. Validação Completa**

1. Verifique todas as 8 rotas no app
2. Confirme contagem de clientes
3. Teste sincronização

#### **3. Backup**

1. Exporte dados do Firebase Console
2. Salve backup seguro
3. Documente processo

---

## 📞 **Suporte**

### **Se Precisar Ajuda:**

1. **Verifique logs** no console do script
2. **Confirme estrutura** do JSON gerado
3. **Teste com 1 cliente** antes de todos
4. **Use Firebase Console** para debug

### **Contatos:**

- **Firebase Console**: <https://console.firebase.google.com/project/gestaobilhares>
- **Documentação**: `import-data/README_IMPORTACAO.md`
- **Script teste**: `import-data/teste_simples.js`

---

## ✅ **Checklist Final**

### **Antes de Importar:**

- [ ] Arquivo CSV na pasta `anexos/`
- [ ] Script de teste executado com sucesso
- [ ] JSON gerado validado
- [ ] Firebase Console acessível

### **Após Importar:**

- [ ] Dados visíveis no Firebase Console
- [ ] Rotas aparecem no app Android
- [ ] Clientes listados corretamente
- [ ] Sincronização funcionando

---

## 🎉 **Conclusão**

**A importação de dados CSV para o Gestão Bilhares está funcional e testada!**

**Método recomendado:** Importação manual via Firebase Console para testes, script automatizado para produção.

**Status:** ✅ Pronto para uso em produção

---

*Última atualização: 09/01/2026*  
*Versão: 1.0*
