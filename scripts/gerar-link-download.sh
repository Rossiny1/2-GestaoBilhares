#!/bin/bash
# Script para gerar link de download do APK
# Uso: ./scripts/gerar-link-download.sh

echo "🔗 Gerando link de download do APK..."
echo ""

# Encontrar APK
APK_PATH=""
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
elif [ -f "b/outputs/apk/debug/app-debug.apk" ]; then
    APK_PATH="b/outputs/apk/debug/app-debug.apk"
else
    echo "❌ APK não encontrado!"
    echo "💡 Execute: ./gradlew :app:assembleDebug"
    exit 1
fi

APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo "📦 APK encontrado: $APK_PATH ($APK_SIZE)"
echo ""

# Tentar upload via transfer.sh
echo "📤 Fazendo upload para transfer.sh..."
LINK=$(curl --progress-bar --upload-file "$APK_PATH" "https://transfer.sh/app-debug.apk" 2>&1)

if [ $? -eq 0 ] && [ ! -z "$LINK" ]; then
    echo ""
    echo "✅ Link gerado com sucesso!"
    echo ""
    echo "════════════════════════════════════════"
    echo "🔗 LINK DE DOWNLOAD:"
    echo "════════════════════════════════════════"
    echo "$LINK"
    echo "════════════════════════════════════════"
    echo ""
    echo "💡 Este link é válido por 14 dias"
    echo "📱 Você pode compartilhar este link para download"
    echo ""
else
    echo "❌ Erro ao gerar link"
    echo ""
    echo "💡 Alternativas:"
    echo "   1. Baixar via Cursor Explorer"
    echo "   2. Usar GitHub Releases"
    echo "   3. Compartilhar via Google Drive/Dropbox"
    exit 1
fi
