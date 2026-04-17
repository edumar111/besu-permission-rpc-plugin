#!/bin/bash
set -e

echo "🚀 Compilando Besu Permission RPC Plugin v2.0.0"
echo ""

# Verificar Java
java -version

echo ""
echo "Usando gradle desde gradlew..."

# Intentar compilar
chmod +x gradlew
./gradlew --version

echo ""
echo "Iniciando compilación..."
./gradlew clean
./gradlew build -x test

echo ""
echo "✅ Compilación completada"
ls -lh build/libs/
