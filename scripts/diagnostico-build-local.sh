#!/bin/bash
# Script para diagnosticar problemas de build local
# Uso: bash scripts/diagnostico-build-local.sh

set -e

echo "🔍 DIAGNÓSTICO DE BUILD LOCAL"
echo "════════════════════════════════════════"
echo ""

# 1. Verificar Java
echo "1️⃣ Verificando Java..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -1)
    echo "   ✅ Java encontrado: $JAVA_VERSION"
else
    echo "   ❌ Java não encontrado!"
    echo "   💡 Instale Java 11 ou superior"
fi

# 2. Verificar Gradle
echo ""
echo "2️⃣ Verificando Gradle..."
if [ -f "gradlew" ]; then
    GRADLE_VERSION=$(./gradlew --version 2>&1 | grep "Gradle" | head -1)
    echo "   ✅ Gradle encontrado: $GRADLE_VERSION"
else
    echo "   ❌ gradlew não encontrado!"
fi

# 3. Verificar Android SDK
echo ""
echo "3️⃣ Verificando Android SDK..."
if [ -f "local.properties" ]; then
    SDK_DIR=$(grep "sdk.dir" local.properties | cut -d'=' -f2)
    if [ -d "$SDK_DIR" ]; then
        echo "   ✅ Android SDK encontrado: $SDK_DIR"
    else
        echo "   ❌ Android SDK não encontrado em: $SDK_DIR"
        echo "   💡 Configure o caminho correto em local.properties"
    fi
else
    echo "   ❌ local.properties não existe!"
    echo "   💡 Crie local.properties com: sdk.dir=/caminho/para/android-sdk"
fi

# 4. Verificar gradle.properties
echo ""
echo "4️⃣ Verificando gradle.properties..."
if [ -f "gradle.properties" ]; then
    echo "   ✅ gradle.properties encontrado"
    echo "   Gradle JVM: $(grep 'org.gradle.jvmargs' gradle.properties | head -1)"
    echo "   Kotlin JVM: $(grep 'kotlin.daemon.jvmargs' gradle.properties | head -1)"
else
    echo "   ❌ gradle.properties não encontrado!"
fi

# 5. Tentar build de teste
echo ""
echo "6️⃣ Testando compilação..."
echo "   Executando: ./gradlew compileDebugKotlin"
if ./gradlew compileDebugKotlin --console=plain 2>&1 | tee /tmp/build-output.log; then
    echo "   ✅ Build passou!"
else
    echo "   ❌ Build falhou!"
    echo ""
    echo "   📋 Erros encontrados:"
    grep -E "error:|FAILED|Exception" /tmp/build-output.log | head -10
fi

echo ""
echo "════════════════════════════════════════"
echo "📊 RESUMO"
echo ""
echo "💡 Próximos passos:"
echo "   1. Verifique se local.properties existe e está correto"
echo "   2. Verifique se Java está instalado"
echo "   3. Compare erros acima com os da VM"
echo ""
