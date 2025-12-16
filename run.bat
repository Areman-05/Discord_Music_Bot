@echo off
echo ========================================
echo  Bot de Musica para Discord
echo ========================================
echo.
echo IMPORTANTE: Reemplaza TU_TOKEN_AQUI con tu token real de Discord
echo Obtener token: https://discord.com/developers/applications
echo.
pause

set DISCORD_TOKEN=TU_TOKEN_AQUI

if "%DISCORD_TOKEN%"=="TU_TOKEN_AQUI" (
    echo ERROR: Debes configurar tu token de Discord!
    echo Edita este archivo (run.bat) y reemplaza TU_TOKEN_AQUI con tu token real
    pause
    exit /b 1
)

java -jar target\SoundBot-1.0.0-All.jar

pause

