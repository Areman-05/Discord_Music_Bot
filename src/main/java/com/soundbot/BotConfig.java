/*
 * Copyright 2018 SoundBot Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.soundbot;

import com.soundbot.entities.Prompt;
import com.soundbot.utils.OtherUtil;
import com.soundbot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.typesafe.config.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

/**
 * Clase que gestiona la configuración del bot.
 * Carga la configuración desde archivos, valida los valores
 * y proporciona acceso a todas las opciones de configuración.
 * 
 * @author SoundBot Contributors
 */
public class BotConfig
{
    private final Prompt prompt;
    private final static String CONTEXT = "Config";
    private final static String START_TOKEN = "/// START OF SOUNDBOT CONFIG ///";
    private final static String END_TOKEN = "/// END OF SOUNDBOT CONFIG ///";
    
    private Path path = null;
    private String token, prefix, altprefix, helpWord, playlistsFolder, logLevel,
            successEmoji, warningEmoji, errorEmoji, loadingEmoji, searchingEmoji,
            evalEngine;
    private boolean stayInChannel, songInGame, npImages, updatealerts, useEval, dbots;
    private long owner, maxSeconds, aloneTimeUntilStop;
    private int maxYTPlaylistPages;
    private double skipratio;
    private OnlineStatus status;
    private Activity game;
    private Config aliases, transforms;

    private boolean valid = false;
    
    /**
     * Constructor de BotConfig.
     * 
     * @param prompt Instancia de Prompt para interactuar con el usuario
     */
    public BotConfig(Prompt prompt)
    {
        this.prompt = prompt;
    }
    
    /**
     * Carga la configuración desde el archivo de configuración.
     * Valida el token y el ID del propietario, solicitándolos si faltan.
     */
    public void load()
    {
        valid = false;
        
        // read config from file
        try 
        {
            // get the path to the config, default config.txt
            path = getConfigPath();
            
            // load in the config file, plus the default values
            Config config = ConfigFactory.load();
            
            // set values
            token = config.getString("token");
            prefix = config.getString("prefix");
            altprefix = config.getString("altprefix");
            helpWord = config.getString("help");
            owner = config.getLong("owner");
            successEmoji = config.getString("success");
            warningEmoji = config.getString("warning");
            errorEmoji = config.getString("error");
            loadingEmoji = config.getString("loading");
            searchingEmoji = config.getString("searching");
            game = OtherUtil.parseGame(config.getString("game"));
            status = OtherUtil.parseStatus(config.getString("status"));
            stayInChannel = config.getBoolean("stayinchannel");
            songInGame = config.getBoolean("songinstatus");
            npImages = config.getBoolean("npimages");
            updatealerts = config.getBoolean("updatealerts");
            logLevel = config.getString("loglevel");
            useEval = config.getBoolean("eval");
            evalEngine = config.getString("evalengine");
            maxSeconds = config.getLong("maxtime");
            maxYTPlaylistPages = config.getInt("maxytplaylistpages");
            aloneTimeUntilStop = config.getLong("alonetimeuntilstop");
            playlistsFolder = config.getString("playlistsfolder");
            aliases = config.getConfig("aliases");
            transforms = config.getConfig("transforms");
            skipratio = config.getDouble("skipratio");
            dbots = false;
            
            // we may need to write a new config file
            boolean write = false;

            // Validar token del bot
            if(token == null || token.isEmpty() || token.equalsIgnoreCase("BOT_TOKEN_HERE"))
            {
                token = prompt.prompt("Por favor proporciona un token de bot."
                        + "\nLas instrucciones para obtener un token se pueden encontrar aqui:"
                        + "\nhttps://github.com/soundbot/SoundBot/wiki/Getting-a-Bot-Token."
                        + "\nToken del bot: ");
                if(token == null || token.isEmpty())
                {
                    prompt.alert(Prompt.Level.WARNING, CONTEXT, "No se proporciono token! Saliendo.\n\nUbicacion del config: " + path.toAbsolutePath().toString());
                    return;
                }
                else
                {
                    write = true;
                }
            }
            
            // Validar propietario del bot
            if(owner <= 0)
            {
                try
                {
                    String ownerInput = prompt.prompt("El ID del propietario falta, o el ID proporcionado no es valido."
                        + "\nPor favor proporciona el ID de usuario del propietario del bot."
                        + "\nLas instrucciones para obtener tu ID de usuario se pueden encontrar aqui:"
                        + "\nhttps://github.com/soundbot/SoundBot/wiki/Finding-Your-User-ID"
                        + "\nID de usuario del propietario: ");
                    if(ownerInput != null && !ownerInput.isEmpty())
                        owner = Long.parseLong(ownerInput);
                }
                catch(NumberFormatException | NullPointerException ex)
                {
                    owner = 0;
                }
                if(owner <= 0)
                {
                    prompt.alert(Prompt.Level.ERROR, CONTEXT, "ID de usuario invalido! Saliendo.\n\nUbicacion del config: " + path.toAbsolutePath().toString());
                    return;
                }
                else
                {
                    write = true;
                }
            }
            
            if(write)
                writeToFile();
            
            // if we get through the whole config, it's good to go
            valid = true;
        }
        catch (ConfigException ex)
        {
            prompt.alert(Prompt.Level.ERROR, CONTEXT, ex + ": " + ex.getMessage() + "\n\nUbicacion del config: " + path.toAbsolutePath().toString());
        }
    }
    
    private void writeToFile()
    {
        byte[] bytes = loadDefaultConfig().replace("BOT_TOKEN_HERE", token)
                .replace("0 // OWNER ID", Long.toString(owner))
                .trim().getBytes();
        try 
        {
            Files.write(path, bytes);
        }
        catch(IOException ex) 
        {
            prompt.alert(Prompt.Level.WARNING, CONTEXT, "Error al escribir nuevas opciones de configuracion en config.txt: "+ex
                + "\nPor favor asegurate de que los archivos no esten en tu escritorio o en otra area restringida.\n\nUbicacion del config: " 
                + path.toAbsolutePath().toString());
        }
    }
    
    private static String loadDefaultConfig()
    {
        String original = OtherUtil.loadResource(new SoundBot(), "/reference.conf");
        return original==null 
                ? "token = BOT_TOKEN_HERE\r\nowner = 0 // OWNER ID" 
                : original.substring(original.indexOf(START_TOKEN)+START_TOKEN.length(), original.indexOf(END_TOKEN)).trim();
    }
    
    private static Path getConfigPath()
    {
        Path path = OtherUtil.getPath(System.getProperty("config.file", System.getProperty("config", "config.txt")));
        if(path.toFile().exists())
        {
            if(System.getProperty("config.file") == null)
                System.setProperty("config.file", System.getProperty("config", path.toAbsolutePath().toString()));
            ConfigFactory.invalidateCaches();
        }
        return path;
    }
    
    public static void writeDefaultConfig()
    {
        Prompt prompt = new Prompt(null, null, true, true);
        prompt.alert(Prompt.Level.INFO, "SoundBot Config", "Generando archivo de configuracion por defecto");
        Path path = BotConfig.getConfigPath();
        byte[] bytes = BotConfig.loadDefaultConfig().getBytes();
        try
        {
            prompt.alert(Prompt.Level.INFO, "SoundBot Config", "Escribiendo archivo de configuracion por defecto en " + path.toAbsolutePath().toString());
            Files.write(path, bytes);
        }
        catch(Exception ex)
        {
            prompt.alert(Prompt.Level.ERROR, "SoundBot Config", "Ocurrio un error al escribir el archivo de configuracion por defecto: " + ex.getMessage());
        }
    }
    
    /**
     * Verifica si la configuración es válida.
     * @return true si la configuración se cargó correctamente
     */
    public boolean isValid()
    {
        return valid;
    }
    
    /**
     * Obtiene la ubicación del archivo de configuración.
     * @return Ruta absoluta del archivo de configuración
     */
    public String getConfigLocation()
    {
        return path != null ? path.toFile().getAbsolutePath() : "N/A";
    }
    
    /**
     * Obtiene el prefijo de comandos.
     * @return Prefijo configurado
     */
    public String getPrefix()
    {
        return prefix;
    }
    
    /**
     * Obtiene el prefijo alternativo de comandos.
     * @return Prefijo alternativo, o null si está deshabilitado
     */
    public String getAltPrefix()
    {
        return "NONE".equalsIgnoreCase(altprefix) ? null : altprefix;
    }
    
    /**
     * Obtiene el token del bot de Discord.
     * @return Token del bot
     */
    public String getToken()
    {
        return token;
    }
    
    /**
     * Obtiene la ratio de skip requerida.
     * @return Ratio de skip (0.0 a 1.0)
     */
    public double getSkipRatio()
    {
        return skipratio;
    }
    
    /**
     * Obtiene el ID del propietario del bot.
     * @return ID de usuario de Discord del propietario
     */
    public long getOwnerId()
    {
        return owner;
    }
    
    public String getSuccess()
    {
        return successEmoji;
    }
    
    public String getWarning()
    {
        return warningEmoji;
    }
    
    public String getError()
    {
        return errorEmoji;
    }
    
    public String getLoading()
    {
        return loadingEmoji;
    }
    
    public String getSearching()
    {
        return searchingEmoji;
    }
    
    public Activity getGame()
    {
        return game;
    }
    
    public boolean isGameNone()
    {
        return game != null && game.getName().equalsIgnoreCase("none");
    }
    
    public OnlineStatus getStatus()
    {
        return status;
    }
    
    public String getHelp()
    {
        return helpWord;
    }
    
    public boolean getStay()
    {
        return stayInChannel;
    }
    
    public boolean getSongInStatus()
    {
        return songInGame;
    }
    
    public String getPlaylistsFolder()
    {
        return playlistsFolder;
    }
    
    public boolean getDBots()
    {
        return dbots;
    }
    
    public boolean useUpdateAlerts()
    {
        return updatealerts;
    }

    public String getLogLevel()
    {
        return logLevel;
    }

    public boolean useEval()
    {
        return useEval;
    }
    
    public String getEvalEngine()
    {
        return evalEngine;
    }
    
    public boolean useNPImages()
    {
        return npImages;
    }
    
    public long getMaxSeconds()
    {
        return maxSeconds;
    }
    
    public int getMaxYTPlaylistPages()
    {
        return maxYTPlaylistPages;
    }
    
    public String getMaxTime()
    {
        return TimeUtil.formatTime(maxSeconds * 1000);
    }

    public long getAloneTimeUntilStop()
    {
        return aloneTimeUntilStop;
    }
    
    public boolean isTooLong(AudioTrack track)
    {
        if(maxSeconds<=0 || track == null)
            return false;
        long duration = track.getDuration();
        if(duration == Long.MAX_VALUE)
            return false;
        return Math.round(duration/1000.0) > maxSeconds;
    }

    public String[] getAliases(String command)
    {
        try
        {
            return aliases.getStringList(command).toArray(new String[0]);
        }
        catch(NullPointerException | ConfigException.Missing e)
        {
            return new String[0];
        }
    }
    
    public Config getTransforms()
    {
        return transforms;
    }
}

