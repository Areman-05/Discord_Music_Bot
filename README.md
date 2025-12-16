# Bot de Música Simple para Discord

Un bot de música simple y fácil de usar para Discord usando Java, JDA y LavaPlayer.

## Características

- Reproducir música desde YouTube y otras fuentes
- Cola de reproducción simple
- Comandos básicos: play, stop, skip, queue, help

## Comandos

- `!play <URL o búsqueda>` - Reproduce música
- `!stop` - Detiene la música y limpia la cola
- `!skip` - Salta a la siguiente canción
- `!queue` - Muestra la cola de reproducción
- `!help` - Muestra la ayuda

## Configuración Paso a Paso

### 1. Crear un Bot en Discord

1. Ve a https://discord.com/developers/applications
2. Haz clic en **"New Application"** y dale un nombre
3. Ve a la pestaña **"Bot"** en el menú lateral
4. Haz clic en **"Add Bot"** y confirma
5. En la sección **"Token"**, haz clic en **"Reset Token"** y copia el token (guárdalo bien, lo necesitarás)
6. Desactiva **"Public Bot"** si no quieres que otros lo usen
7. Activa estos permisos en **"Privileged Gateway Intents"**:
   - ✅ **MESSAGE CONTENT INTENT** (necesario para leer comandos)

### 2. Invitar el Bot a tu Servidor

1. Ve a la pestaña **"OAuth2" > "URL Generator"**
2. Selecciona los **scopes**:
   - ✅ `bot`
   - ✅ `applications.commands` (opcional, para slash commands)
3. Selecciona los **permisos** necesarios:
   - ✅ **Connect** (conectarse a canales de voz)
   - ✅ **Speak** (reproducir audio)
   - ✅ **Use Voice Activity** (usar actividad de voz)
   - ✅ **Send Messages** (enviar mensajes)
   - ✅ **Read Message History** (leer historial de mensajes)
   - ✅ **Add Reactions** (agregar reacciones)
4. Copia la URL generada y ábrela en tu navegador
5. Selecciona tu servidor y autoriza

### 3. Configurar el Proyecto

1. **Compila el proyecto:**
   ```bash
   mvn clean package
   ```
   
   Esto creará un archivo JAR en: `target/SoundBot-1.0.0-All.jar`

2. **Configura el token:**
   
   **Opción A - Variable de entorno (Recomendado):**
   ```bash
   # Windows (PowerShell)
   $env:DISCORD_TOKEN="TU_TOKEN_AQUI"
   
   # Windows (CMD)
   set DISCORD_TOKEN=TU_TOKEN_AQUI
   
   # Linux/Mac
   export DISCORD_TOKEN="TU_TOKEN_AQUI"
   ```
   
   **Opción B - Crear un archivo .bat/.sh:**
   
   Crea un archivo `run.bat` (Windows) o `run.sh` (Linux/Mac):
   
   ```batch
   @echo off
   set DISCORD_TOKEN=TU_TOKEN_AQUI
   java -jar target/SoundBot-1.0.0-All.jar
   ```
   
   O para Linux/Mac:
   ```bash
   #!/bin/bash
   export DISCORD_TOKEN="TU_TOKEN_AQUI"
   java -jar target/SoundBot-1.0.0-All.jar
   ```

### 4. Ejecutar el Bot

1. Asegúrate de tener Java 21 instalado:
   ```bash
   java -version
   ```

2. Ejecuta el JAR:
   ```bash
   java -jar target/SoundBot-1.0.0-All.jar
   ```

3. Si todo va bien, verás: `Bot iniciado correctamente!`

### 5. Usar el Bot

1. Ve a un canal de voz en tu servidor
2. En un canal de texto, escribe: `!play <URL de YouTube o búsqueda>`
3. El bot se conectará al canal de voz y empezará a reproducir música

## Requisitos

- Java 21 o superior
- Token de bot de Discord (obtén uno en https://discord.com/developers/applications)
- Maven (para compilar)

## Ejemplos de Uso

```
!play https://www.youtube.com/watch?v=dQw4w9WgXcQ
!play never gonna give you up
!queue
!skip
!stop
```

## Solución de Problemas

**El bot no responde a comandos:**
- Asegúrate de que el bot tenga el permiso "MESSAGE CONTENT INTENT" activado
- Verifica que el bot esté en línea (verde en la lista de miembros)

**El bot no se conecta al canal de voz:**
- Asegúrate de estar en un canal de voz antes de usar !play
- Verifica que el bot tenga permisos para conectarse y hablar en el canal

**Error al iniciar:**
- Verifica que el token esté correcto
- Asegúrate de tener Java 21 instalado
- Revisa que el JAR se haya compilado correctamente
