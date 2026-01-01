# 🤖 Automação de Build e Instalação

Este projeto possui scripts automatizados para facilitar o desenvolvimento e instalação do app.

## 📋 Scripts Disponíveis

### 1. `scripts/auto-commit-on-build-success.sh`
**O que faz:** Automaticamente faz commit e push das mudanças quando o build passa.

**Quando roda:** Automaticamente após `installDebug` ou `assembleDebug` bem-sucedido.

**Como funciona:**
- Verifica se há mudanças não commitadas
- Faz commit automático com mensagem timestampada
- Faz push para o repositório remoto

### 2. `scripts/auto-install-debug.sh`
**O que faz:** Verifica mudanças remotas e instala o app automaticamente.

**Uso:**
```bash
./scripts/auto-install-debug.sh
```

**Como funciona:**
- Verifica se há atualizações no repositório remoto
- Se houver, faz pull automaticamente
- Compila e instala o app no dispositivo conectado

### 3. `scripts/watch-and-install.sh`
**O que faz:** Monitora mudanças remotas continuamente e instala automaticamente.

**Uso:**
```bash
./scripts/watch-and-install.sh
```

**Como funciona:**
- Roda em loop verificando mudanças a cada 30 segundos
- Quando detecta mudanças, faz pull e instala automaticamente
- Pressione `Ctrl+C` para parar

## 🚀 Configuração Inicial

### No seu ambiente local:

1. **Torne os scripts executáveis** (se ainda não estiverem):
```bash
chmod +x scripts/*.sh
```

2. **Configure o Git** (se ainda não tiver):
```bash
git config --global user.name "Seu Nome"
git config --global user.email "seu@email.com"
```

3. **Teste a instalação automática:**
```bash
./scripts/auto-install-debug.sh
```

## 🔄 Fluxo de Trabalho Automatizado

### Cenário 1: Eu faço mudanças e o build passa
1. ✅ Eu faço as correções no código
2. ✅ Build passa automaticamente
3. ✅ Script `auto-commit-on-build-success.sh` roda automaticamente
4. ✅ Mudanças são commitadas e enviadas para o repositório
5. 🔄 **Você roda localmente:** `./scripts/auto-install-debug.sh`
6. ✅ App é instalado automaticamente no seu celular

### Cenário 2: Monitoramento contínuo
1. 🔄 **Você roda localmente:** `./scripts/watch-and-install.sh`
2. ✅ Script monitora mudanças remotas continuamente
3. ✅ Quando eu fizer mudanças e commitar, o script detecta
4. ✅ Pull e instalação acontecem automaticamente
5. ✅ Seu app sempre atualizado!

## 📱 Requisitos

- Dispositivo Android conectado via USB
- Depuração USB ativada no dispositivo
- Git configurado com credenciais
- Acesso ao repositório remoto (push/pull)

## ⚙️ Personalização

### Alterar intervalo de verificação (watch-and-install.sh):
Edite a linha `sleep 30` para o intervalo desejado (em segundos).

### Desabilitar commit automático:
Comente ou remova as linhas no final de `app/build.gradle.kts`:
```kotlin
// tasks.named("installDebug") {
//     finalizedBy("autoCommitOnSuccess")
// }
```

## 🐛 Troubleshooting

### Script não executa:
```bash
chmod +x scripts/auto-install-debug.sh
```

### Erro de permissão Git:
Verifique suas credenciais:
```bash
git config --list
```

### Dispositivo não detectado:
```bash
adb devices
```

### Build falha mas script tenta commitar:
O script só roda se o build passar. Se o build falhar, nada é commitado.

## 📝 Notas

- O commit automático usa mensagens genéricas com timestamp
- Para commits mais descritivos, faça manualmente antes do build
- O script de watch consome recursos - use apenas durante desenvolvimento ativo
- Recomendado usar `auto-install-debug.sh` quando precisar, ou `watch-and-install.sh` para monitoramento contínuo
