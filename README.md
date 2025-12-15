# SoundBot

Un bot de musica para Discord multiplataforma con una interfaz limpia y facil de configurar y ejecutar.

## Caracteristicas
  * Facil de ejecutar (solo asegurate de tener Java instalado y ejecutalo!)
  * Carga rapida de canciones
  * No se necesitan claves externas (solo un token de bot de Discord)
  * Reproduccion fluida
  * Configuracion especifica por servidor para el rol "DJ" que puede moderar la musica
  * Menus limpios y bonitos
  * Soporta muchos sitios, incluyendo Youtube, Soundcloud y mas
  * Soporta muchas radios/streams en linea
  * Soporta archivos locales
  * Soporte para listas de reproduccion (tanto web/youtube como locales)

## Fuentes y formatos soportados
SoundBot soporta todas las fuentes y formatos soportados por [lavaplayer](https://github.com/sedmelluq/lavaplayer#supported-formats):
### Fuentes
  * YouTube
  * SoundCloud
  * Bandcamp
  * Vimeo
  * Streams de Twitch
  * Archivos locales
  * URLs HTTP
### Formatos
  * MP3
  * FLAC
  * WAV
  * Matroska/WebM (codecs AAC, Opus o Vorbis)
  * MP4/M4A (codec AAC)
  * Streams OGG (codecs Opus, Vorbis y FLAC)
  * Streams AAC
  * Listas de reproduccion de streams (M3U y PLS)

## Configuracion
Por favor consulta la pagina de configuracion para ejecutar este bot tu mismo!

## Requisitos
  * Java 11 o superior
  * Token de bot de Discord
  * Permisos necesarios en el servidor de Discord

## Instalacion
1. Descarga el proyecto
2. Configura el archivo `config.txt` con tu token de bot
3. Compila el proyecto con Maven: `mvn clean package`
4. Ejecuta el bot: `java -jar target/SoundBot-All.jar`

## Preguntas/Sugerencias/Reportes de Bugs
Si tienes una pregunta, necesitas ayuda para solucionar problemas, o quieres proponer una nueva caracteristica, por favor inicia una Discusion. Si te gustaria sugerir una caracteristica o reportar un bug reproducible, por favor abre un Issue en este repositorio.

## Licencia
Este proyecto esta licenciado bajo la Licencia Apache 2.0 - ver el archivo LICENSE para mas detalles.
