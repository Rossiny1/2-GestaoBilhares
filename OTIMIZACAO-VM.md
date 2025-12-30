# 🚀 Guia de Otimização da VM

## 🔍 Problemas Identificados

### 1. **Memória Insuficiente**
- **Gradle daemon**: 8GB (muito alto)
- **Kotlin daemon**: 6GB (muito alto)
- **Total**: 14GB só para build tools em uma VM com 15GB
- **Resultado**: Sistema fica sem memória e trava

### 2. **Sem Swap Configurado**
- Swap: 0B (zero!)
- Quando a memória acaba, o sistema trava completamente
- Sem swap, não há "válvula de escape"

### 3. **Processos Zombie**
- 4 processos Java zombie não limpos
- Consomem recursos do sistema

### 4. **Configurações Sub-ótimas**
- Swappiness: 60 (muito alto, usa swap demais)
- Workers: 8 (muito para 4 CPUs)

## ✅ Otimizações Aplicadas

### 1. Redução de Memória dos Daemons
- **Gradle**: 8GB → **4GB** (redução de 50%)
- **Kotlin**: 6GB → **3GB** (redução de 50%)
- **Total**: 14GB → **7GB** (libera 7GB para o sistema)

### 2. Configuração de Swap
- Criado swap de **4GB**
- Swappiness reduzido para **10** (usa swap apenas quando necessário)

### 3. Otimizações de Sistema
- VFS cache pressure otimizado
- Dirty ratio ajustado
- Workers reduzidos para 4 (igual ao número de CPUs)

## 🛠️ Como Aplicar as Otimizações

### Opção 1: Script Automático (Recomendado)

```bash
# Execute com sudo para aplicar todas as otimizações:
sudo bash scripts/otimizar-vm.sh
```

### Opção 2: Manual

#### 1. Limpar daemons antigos:
```bash
bash scripts/limpar-daemons.sh
```

#### 2. Criar swap (requer sudo):
```bash
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo "/swapfile none swap sw 0 0" | sudo tee -a /etc/fstab
```

#### 3. Otimizar swappiness:
```bash
echo "vm.swappiness=10" | sudo tee -a /etc/sysctl.conf
sudo sysctl vm.swappiness=10
```

#### 4. Aplicar novas configurações do Gradle:
```bash
# As mudanças em gradle.properties já foram aplicadas
# Apenas limpe os daemons para reiniciar com novas configurações:
gradle --stop
```

## 📊 Monitoramento

### Verificar uso de memória:
```bash
free -h
watch -n 1 free -h  # Monitoramento contínuo
```

### Verificar swap:
```bash
swapon --show
```

### Verificar processos Java:
```bash
ps aux | grep java
```

### Verificar daemons Gradle:
```bash
./gradlew --status
```

## 🎯 Resultados Esperados

### Antes:
- ❌ Memória: 14GB usado por build tools
- ❌ Swap: 0B (sem proteção)
- ❌ Travamentos frequentes
- ❌ Builds lentos por falta de memória

### Depois:
- ✅ Memória: 7GB usado por build tools (50% menos)
- ✅ Swap: 4GB disponível (proteção contra travamentos)
- ✅ Sistema mais estável
- ✅ Builds mais rápidos (menos swapping)

## 🔧 Manutenção

### Limpar daemons regularmente:
```bash
# Quando a VM começar a ficar lenta:
bash scripts/limpar-daemons.sh
```

### Verificar saúde do sistema:
```bash
# Verificar memória:
free -h

# Verificar processos:
top

# Verificar daemons Gradle:
./gradlew --status
```

## ⚠️ Troubleshooting

### Se ainda travar após otimizações:

1. **Aumentar swap** (se necessário):
```bash
sudo swapoff /swapfile
sudo fallocate -l 8G /swapfile  # Aumentar para 8GB
sudo mkswap /swapfile
sudo swapon /swapfile
```

2. **Reduzir ainda mais a memória** (editar `gradle.properties`):
```properties
org.gradle.jvmargs=-Xmx3g ...  # Reduzir para 3GB
kotlin.daemon.jvmargs=-Xmx2g ...  # Reduzir para 2GB
```

3. **Desabilitar parallel execution** (último recurso):
```properties
org.gradle.parallel=false
org.gradle.workers.max=2
```

## 📝 Notas

- As otimizações em `gradle.properties` já foram aplicadas
- Execute `scripts/otimizar-vm.sh` para aplicar otimizações de sistema
- Execute `scripts/limpar-daemons.sh` regularmente para manter performance
- Monitore o uso de memória após builds grandes
