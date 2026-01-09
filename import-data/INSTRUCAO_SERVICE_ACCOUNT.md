# 🔑 Criar Service Account - Firebase

## 📋 Passo a Passo

### 1. **Acessar Firebase Console**

Vá para: <https://console.firebase.google.com/project/gestaobilhares/settings/serviceaccounts/adminsdk>

### 2. **Gerar Nova Chave**

1. Clique em **"Gerar nova chave privada"**
2. Selecione **JSON** (já deve estar selecionado)
3. Clique em **"GERAR CHAVE"**

### 3. **Salvar Arquivo**

1. O arquivo será baixado automaticamente
2. Renomeie para: `service-account.json`
3. Mova para a pasta: `import-data/`

### 4. **Verificar Instalação**

```bash
cd import-data
ls service-account.json
# Deve mostrar o arquivo
```

## 🚀 Após Criar Service Account

Execute o teste:

```bash
node teste_3_clientes.js
```

## ⚠️ Importante

- **Não compartilhe** este arquivo (contém credenciais admin)
- **Não commit** para o Git
- **Guarde em local seguro**

## 🔍 Se Tiver Problemas

1. **Verifique se o projeto está correto**: `gestaobilhares`
2. **Verifique se o arquivo está no lugar certo**: `import-data/service-account.json`
3. **Verifique se o Node.js está funcionando**: `node --version`

---

**Pronto para criar o service account?**
