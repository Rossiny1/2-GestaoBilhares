#!/bin/bash
# Script para otimizar a VM e melhorar performance
# Uso: sudo bash scripts/otimizar-vm.sh

set -e

echo "🔧 Otimizando configuração da VM para máxima performance..."
echo ""

# 1. Limpar processos zombie e daemons antigos do Gradle
echo "🧹 Limpando processos zombie e daemons antigos..."
# Matar processos Java zombie (já estão mortos, apenas limpar referências)
pkill -9 -f "kotlin-compiler-embeddable" 2>/dev/null || true
pkill -9 -f "gradle-daemon" 2>/dev/null || true

# Parar todos os daemons Gradle para reiniciar com novas configurações
if command -v gradle &> /dev/null; then
    gradle --stop 2>/dev/null || true
fi

# Limpar cache do Kotlin daemon
rm -rf ~/.kotlin/daemon/* 2>/dev/null || true

echo "✅ Processos limpos"
echo ""

# 2. Configurar swap (se não existir)
echo "💾 Verificando configuração de swap..."
if [ ! -f /swapfile ]; then
    echo "📦 Criando arquivo de swap (4GB)..."
    sudo fallocate -l 4G /swapfile || sudo dd if=/dev/zero of=/swapfile bs=1M count=4096
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
    sudo swapon /swapfile
    
    # Tornar permanente
    if ! grep -q "/swapfile" /etc/fstab; then
        echo "/swapfile none swap sw 0 0" | sudo tee -a /etc/fstab
    fi
    
    echo "✅ Swap criado e ativado"
else
    echo "✅ Swap já existe"
    sudo swapon --show
fi
echo ""

# 3. Otimizar swappiness (reduzir de 60 para 10 - menos uso de swap)
echo "⚙️  Otimizando swappiness..."
if [ "$(cat /proc/sys/vm/swappiness)" != "10" ]; then
    echo "vm.swappiness=10" | sudo tee -a /etc/sysctl.conf
    sudo sysctl vm.swappiness=10
    echo "✅ Swappiness otimizado para 10"
else
    echo "✅ Swappiness já está otimizado"
fi
echo ""

# 4. Otimizar cache de memória
echo "⚙️  Otimizando cache de memória..."
if ! grep -q "vm.vfs_cache_pressure" /etc/sysctl.conf; then
    echo "vm.vfs_cache_pressure=50" | sudo tee -a /etc/sysctl.conf
    sudo sysctl vm.vfs_cache_pressure=50
fi
if ! grep -q "vm.dirty_ratio" /etc/sysctl.conf; then
    echo "vm.dirty_ratio=15" | sudo tee -a /etc/sysctl.conf
    echo "vm.dirty_background_ratio=5" | sudo tee -a /etc/sysctl.conf
    sudo sysctl vm.dirty_ratio=15
    sudo sysctl vm.dirty_background_ratio=5
fi
echo "✅ Cache de memória otimizado"
echo ""

# 5. Limpar caches do sistema
echo "🧹 Limpando caches do sistema..."
sudo sync
echo 3 | sudo tee /proc/sys/vm/drop_caches > /dev/null 2>&1 || true
echo "✅ Caches limpos"
echo ""

# 6. Resumo final
echo "════════════════════════════════════════"
echo "✅ Otimizações aplicadas!"
echo ""
echo "📊 Status atual:"
free -h
echo ""
echo "💾 Swap:"
swapon --show
echo ""
echo "⚙️  Configurações:"
echo "  Swappiness: $(cat /proc/sys/vm/swappiness)"
echo "  VFS Cache Pressure: $(cat /proc/sys/vm/vfs_cache_pressure 2>/dev/null || echo 'N/A')"
echo ""
echo "💡 Próximos passos:"
echo "  1. Reinicie os daemons Gradle: gradle --stop && ./gradlew build"
echo "  2. Monitore o uso de memória: watch -n 1 free -h"
echo ""
