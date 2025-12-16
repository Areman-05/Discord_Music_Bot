#!/bin/bash

echo "========================================"
echo " Bot de Musica para Discord"
echo "========================================"
echo ""
echo "IMPORTANTE: Reemplaza TU_TOKEN_AQUI con tu token real de Discord"
echo "Obtener token: https://discord.com/developers/applications"
echo ""

# Reemplaza esto con tu token real
export DISCORD_TOKEN="TU_TOKEN_AQUI"

if [ "$DISCORD_TOKEN" = "TU_TOKEN_AQUI" ]; then
    echo "ERROR: Debes configurar tu token de Discord!"
    echo "Edita este archivo (run.sh) y reemplaza TU_TOKEN_AQUI con tu token real"
    exit 1
fi

java -jar target/SoundBot-1.0.0-All.jar

