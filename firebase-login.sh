#!/bin/bash
# Script para fazer login no Firebase CLI
# Execute este script em um terminal interativo

export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

echo "🔥 Iniciando login no Firebase..."
echo ""
echo "Este processo irá:"
echo "1. Abrir uma URL no seu navegador"
echo "2. Você fará login com sua conta Google"
echo "3. Você receberá um código de autorização"
echo "4. Cole o código aqui quando solicitado"
echo ""
echo "Pressione Enter para continuar..."
read

firebase login --no-localhost

echo ""
echo "✅ Login concluído!"
echo ""
echo "Verificando login..."
firebase login:list
