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

## Configuración

1. Configura la variable de entorno `DISCORD_TOKEN` con tu token de bot de Discord
2. Compila el proyecto con Maven: `mvn clean package`
3. Ejecuta el JAR: `java -jar target/SoundBot-1.0.0-All.jar`

## Requisitos

- Java 11 o superior
- Token de bot de Discord (obtén uno en https://discord.com/developers/applications)
