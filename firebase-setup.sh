#!/bin/bash
# Script helper para configurar e usar Firebase CLI

export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

echo "🔥 Firebase CLI Helper"
echo "===================="
echo ""

case "$1" in
  "check")
    echo "Verificando instalação..."
    firebase --version
    echo ""
    echo "Contas logadas:"
    firebase login:list
    echo ""
    echo "Projetos disponíveis:"
    firebase projects:list
    ;;
  "login")
    echo "Iniciando login..."
    firebase login --no-localhost
    ;;
  "projects")
    firebase projects:list
    ;;
  "use")
    if [ -z "$2" ]; then
      echo "Uso: ./firebase-setup.sh use <project-id>"
    else
      firebase use "$2"
    fi
    ;;
  "test")
    if [ -z "$2" ]; then
      APK="/workspace/b/outputs/apk/debug/app-debug.apk"
    else
      APK="$2"
    fi
    echo "Executando teste no Firebase Test Lab..."
    echo "APK: $APK"
    firebase test android run \
      --app "$APK" \
      --device model=Pixel2,version=28 \
      --timeout 5m
    ;;
  *)
    echo "Uso: ./firebase-setup.sh <comando>"
    echo ""
    echo "Comandos disponíveis:"
    echo "  check     - Verificar instalação e status"
    echo "  login     - Fazer login no Firebase"
    echo "  projects  - Listar projetos Firebase"
    echo "  use <id>  - Selecionar projeto"
    echo "  test [apk] - Executar teste no Test Lab"
    echo ""
    echo "Exemplos:"
    echo "  ./firebase-setup.sh check"
    echo "  ./firebase-setup.sh login"
    echo "  ./firebase-setup.sh use meu-projeto"
    echo "  ./firebase-setup.sh test"
    ;;
esac
