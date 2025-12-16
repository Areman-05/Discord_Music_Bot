/*
 * Copyright 2016 SoundBot Contributors
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

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.soundbot.entities.Prompt;
import com.soundbot.settings.SettingsManager;
import com.soundbot.utils.OtherUtil;
import java.util.Arrays;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;

/**
 *
 * @author SoundBot Contributors
 */
public class SoundBot 
{
    // Constantes para configuración
    private static final String CONFIG_GENERATE_COMMAND = "generate-config";
    private static final String MENTION_PREFIX = "@mention";
    private static final String LOADING_ACTIVITY = "loading...";
    private static final int LINKED_CACHE_SIZE = 200;
    private static final int SHUTDOWN_WAIT_TIME_MS = 5000;
    
    public final static Logger LOG = LoggerFactory.getLogger(SoundBot.class);
    public final static Permission[] RECOMMENDED_PERMS = {Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ADD_REACTION,
                                Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_MANAGE, Permission.MESSAGE_EXT_EMOJI,
                                Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.NICKNAME_CHANGE};
    public final static GatewayIntent[] INTENTS = {GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES};
    
    /**
     * Punto de entrada principal de la aplicación.
     * 
     * @param args Argumentos de línea de comandos. Si contiene "generate-config", 
     *             genera un archivo de configuración por defecto.
     */
    public static void main(String[] args)
    {
        if(args.length > 0)
            switch(args[0].toLowerCase())
            {
                case CONFIG_GENERATE_COMMAND:
                    BotConfig.writeDefaultConfig();
                    return;
                default:
            }
        startBot();
    }
    
    /**
     * Inicializa y arranca el bot de Discord.
     * Realiza las validaciones necesarias, carga la configuración
     * y establece la conexión con Discord.
     */
    private static void startBot()
    {
        // Crear prompt para manejar el inicio
        Prompt prompt = new Prompt("SoundBot");
        
        // Verificaciones de inicio
        OtherUtil.checkVersion(prompt);
        OtherUtil.checkJavaVersion(prompt);
        
        // Cargar configuración
        BotConfig config = new BotConfig(prompt);
        config.load();
        if(!config.isValid())
            return;
        LOG.info("Loaded config from " + config.getConfigLocation());

        // Establecer nivel de log desde la configuración
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(
                Level.toLevel(config.getLogLevel(), Level.INFO));
        
        // Configurar listeners
        EventWaiter waiter = new EventWaiter();
        SettingsManager settings = new SettingsManager();
        Bot bot = new Bot(waiter, config, settings);
        CommandClient client = createCommandClient(config, settings, bot);
        
        // Intentar iniciar sesión y arrancar
        try
        {
            JDA jda = JDABuilder.create(config.getToken(), Arrays.asList(INTENTS))
                    .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                    .disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.EMOTE, CacheFlag.ONLINE_STATUS)
                    .setActivity(config.isGameNone() ? null : Activity.playing(LOADING_ACTIVITY))
                    .setStatus(config.getStatus()==OnlineStatus.INVISIBLE || config.getStatus()==OnlineStatus.OFFLINE 
                            ? OnlineStatus.INVISIBLE : OnlineStatus.DO_NOT_DISTURB)
                    .addEventListeners(client, waiter, new Listener(bot))
                    .setBulkDeleteSplittingEnabled(true)
                    .build();
            bot.setJDA(jda);

            // check if something about the current startup is not supported
            String unsupportedReason = OtherUtil.getUnsupportedBotReason(jda);
            if (unsupportedReason != null)
            {
                prompt.alert(Prompt.Level.ERROR, "SoundBot", "SoundBot cannot be run on this Discord bot: " + unsupportedReason);
                try{ Thread.sleep(SHUTDOWN_WAIT_TIME_MS);}catch(InterruptedException ignored){}
                jda.shutdown();
                System.exit(1);
            }
            
            if(!MENTION_PREFIX.equals(config.getPrefix()))
            {
                LOG.info("Tienes un prefijo personalizado configurado. "
                        + "Si tu prefijo no funciona, asegurate de que el 'MESSAGE CONTENT INTENT' este habilitado "
                        + "en https://discord.com/developers/applications/" + jda.getSelfUser().getId() + "/bot");
            }
            LOG.info("SoundBot iniciado correctamente!");
        }
        catch (LoginException ex)
        {
            prompt.alert(Prompt.Level.ERROR, "SoundBot", ex + "\nPor favor asegurate de estar "
                    + "editando el archivo config.txt correcto, y que hayas usado el "
                    + "token correcto (no el 'secret'!)\nUbicacion del config: " + config.getConfigLocation());
            System.exit(1);
        }
        catch(IllegalArgumentException ex)
        {
            prompt.alert(Prompt.Level.ERROR, "SoundBot", "Algun aspecto de la configuracion es "
                    + "invalido: " + ex + "\nUbicacion del config: " + config.getConfigLocation());
            System.exit(1);
        }
        catch(ErrorResponseException ex)
        {
            prompt.alert(Prompt.Level.ERROR, "SoundBot", ex + "\nRespuesta invalida al intentar "
                    + "conectar, asegurate de estar conectado a internet");
            System.exit(1);
        }
    }
    
    /**
     * Crea y configura el cliente de comandos de Discord.
     * 
     * @param config Configuración del bot
     * @param settings Manager de configuraciones por servidor
     * @param bot Instancia del bot
     * @return CommandClient configurado
     */
    private static CommandClient createCommandClient(BotConfig config, SettingsManager settings, Bot bot)
    {
        // Configurar el cliente de comandos
        CommandClientBuilder cb = new CommandClientBuilder()
                .setPrefix(config.getPrefix())
                .setAlternativePrefix(config.getAltPrefix())
                .setOwnerId(Long.toString(config.getOwnerId()))
                .setEmojis(config.getSuccess(), config.getWarning(), config.getError())
                .setHelpWord(config.getHelp())
                .setLinkedCacheSize(LINKED_CACHE_SIZE)
                .setGuildSettingsManager(settings);
        
        // Establecer estado si está configurado
        if(config.getStatus() != OnlineStatus.UNKNOWN)
            cb.setStatus(config.getStatus());
        
        // Establecer actividad/juego
        if(config.getGame() == null)
            cb.useDefaultGame();
        else if(config.isGameNone())
            cb.setActivity(null);
        else
            cb.setActivity(config.getGame());
        
        return cb.build();
    }
}

