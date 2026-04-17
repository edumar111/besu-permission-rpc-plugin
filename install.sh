#!/bin/bash

set -e

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     Besu Permission RPC Plugin - Installation Script          ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Verificar que Besu está instalado
if ! command -v besu &> /dev/null; then
    echo -e "${RED}[ERROR]${NC} Besu no está instalado o no está en el PATH"
    exit 1
fi

echo -e "${GREEN}[✓]${NC} Besu encontrado"

# Crear directorios necesarios
echo ""
echo "Creando directorios..."
mkdir -p /opt/besu/plugins
mkdir -p /var/log/besu
chmod 755 /var/log/besu

echo -e "${GREEN}[✓]${NC} Directorios creados"

# Compilar el proyecto
echo ""
echo "Compilando el proyecto..."
./gradlew clean build -q

echo -e "${GREEN}[✓]${NC} Proyecto compilado"

# Copiar el JAR
echo ""
echo "Instalando plugin..."
cp build/libs/besu-permission-rpc-plugin-1.0.0-fat.jar /opt/besu/plugins/

echo -e "${GREEN}[✓]${NC} Plugin instalado en /opt/besu/plugins/"

# Mostrar información
echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                  ¡Instalación completada!                      ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo -e "${YELLOW}Próximos pasos:${NC}"
echo ""
echo "1. Inicia Besu con el plugin:"
echo ""
echo "   besu \\"
echo "     --data-path=/data \\"
echo "     --p2p-port=30303 \\"
echo "     --rpc-http-enabled \\"
echo "     --rpc-http-port=8545 \\"
echo "     --rpc-http-api=ETH,NET,WEB3,PERM \\"
echo "     --plugin-dir=/opt/besu/plugins"
echo ""
echo "2. Verifica que el plugin se cargó:"
echo ""
echo "   tail -f /opt/besu/logs/besu.log | grep PermissionInterceptor"
echo ""
echo "3. Prueba el plugin:"
echo ""
echo "   curl -X POST \\"
echo "     --data '{\"jsonrpc\":\"2.0\",\"method\":\"perm_addAccountsToAllowlist\",\"params\":[[\"0xb9b81ee349c3807e46bc71aa2632203c5b462032\"]], \"id\":1}' \\"
echo "     http://127.0.0.1:8545/ \\"
echo "     -H \"Content-Type: application/json\""
echo ""
echo "4. Ver los logs:"
echo ""
echo "   tail -f /var/log/besu/permission-events.log"
echo ""
