# 🔥 Execute Este Comando no Terminal Bash da VM

## ⚠️ Limitação

Não consigo executar comandos interativos automaticamente. O Firebase CLI precisa de interação humana.

## ✅ Solução: Você Executa no Terminal

### Passo 1: Abrir Terminal Bash da VM

1. **Command Palette**: `Ctrl+Shift+P`
2. **Digite**: `Terminal: Select Default Profile`
3. **Escolha**: `Git Bash` ou `WSL`
4. **Abra terminal**: `Ctrl+Shift+``

### Passo 2: Verificar que Está na VM

```bash
pwd
```

Deve mostrar: `/workspace`

### Passo 3: Executar Este Comando

**Copie e cole este comando completo no terminal:**

```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin && firebase login --no-localhost 2>&1 | tee /workspace/firebase-url-gerada.txt
```

### Passo 4: Ver a URL Completa

Depois que o comando executar, veja a URL:

```bash
cat /workspace/firebase-url-gerada.txt | grep "https://"
```

Ou abra o arquivo `firebase-url-gerada.txt` no Cursor e copie a URL completa.

## 🎯 Ou Use o Script

Execute:

```bash
./gerar-url-firebase.sh
```

E siga as instruções na tela.

## 📋 O Que Vai Acontecer

1. O comando vai perguntar sobre Gemini (digite Y ou N)
2. Vai mostrar a URL (pode aparecer truncada no terminal)
3. **MAS a URL completa estará no arquivo** `firebase-url-gerada.txt`
4. Abra o arquivo no Cursor e copie a URL completa
5. Cole no navegador e faça login
6. Copie o código e cole no terminal

## ✅ Resumo

**Execute no terminal bash da VM:**

```bash
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin
firebase login --no-localhost 2>&1 | tee /workspace/firebase-url-gerada.txt
```

**Depois veja a URL:**

```bash
cat /workspace/firebase-url-gerada.txt
```

**Ou abra o arquivo `firebase-url-gerada.txt` no Cursor!**
