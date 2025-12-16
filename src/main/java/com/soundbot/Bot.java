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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.soundbot.audio.AloneInVoiceHandler;
import com.soundbot.audio.AudioHandler;
import com.soundbot.audio.NowplayingHandler;
import com.soundbot.audio.PlayerManager;
import com.soundbot.playlist.PlaylistLoader;
import com.soundbot.settings.SettingsManager;
import java.util.Objects;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;

/**
 * Clase principal que gestiona las componentes del bot de música.
 * Contiene referencias a los managers de configuración, audio, playlists y handlers.
 * 
 * @author SoundBot Contributors
 */
public class Bot
{
    private final EventWaiter waiter;
    private final ScheduledExecutorService threadpool;
    private final BotConfig config;
    private final SettingsManager settings;
    private final PlayerManager players;
    private final PlaylistLoader playlists;
    private final NowplayingHandler nowplaying;
    private final AloneInVoiceHandler aloneInVoiceHandler;
    
    private boolean shuttingDown = false;
    private JDA jda;
    
    /**
     * Constructor principal del bot.
     * Inicializa todos los componentes necesarios: playlists, player manager, 
     * nowplaying handler y alone in voice handler.
     * 
     * @param waiter EventWaiter para manejar eventos asíncronos
     * @param config Configuración del bot
     * @param settings Manager de configuraciones por servidor
     */
    public Bot(EventWaiter waiter, BotConfig config, SettingsManager settings)
    {
        this.waiter = waiter;
        this.config = config;
        this.settings = settings;
        this.playlists = new PlaylistLoader(config);
        this.threadpool = Executors.newSingleThreadScheduledExecutor();
        this.players = new PlayerManager(this);
        this.players.init();
        this.nowplaying = new NowplayingHandler(this);
        this.nowplaying.init();
        this.aloneInVoiceHandler = new AloneInVoiceHandler(this);
        this.aloneInVoiceHandler.init();
    }
    
    /**
     * Obtiene la configuración del bot.
     * @return BotConfig con todas las configuraciones
     */
    public BotConfig getConfig()
    {
        return config;
    }
    
    /**
     * Obtiene el manager de configuraciones por servidor.
     * @return SettingsManager para gestionar configuraciones
     */
    public SettingsManager getSettingsManager()
    {
        return settings;
    }
    
    /**
     * Obtiene el EventWaiter para manejar eventos asíncronos.
     * @return EventWaiter instance
     */
    public EventWaiter getWaiter()
    {
        return waiter;
    }
    
    /**
     * Obtiene el thread pool para tareas programadas.
     * @return ScheduledExecutorService para ejecutar tareas
     */
    public ScheduledExecutorService getThreadpool()
    {
        return threadpool;
    }
    
    /**
     * Obtiene el manager de reproductores de audio.
     * @return PlayerManager para gestionar reproducción de audio
     */
    public PlayerManager getPlayerManager()
    {
        return players;
    }
    
    /**
     * Obtiene el loader de playlists.
     * @return PlaylistLoader para cargar playlists
     */
    public PlaylistLoader getPlaylistLoader()
    {
        return playlists;
    }
    
    /**
     * Obtiene el handler de "now playing".
     * @return NowplayingHandler para mostrar qué se está reproduciendo
     */
    public NowplayingHandler getNowplayingHandler()
    {
        return nowplaying;
    }

    /**
     * Obtiene el handler de "alone in voice".
     * @return AloneInVoiceHandler para gestionar cuando el bot está solo
     */
    public AloneInVoiceHandler getAloneInVoiceHandler()
    {
        return aloneInVoiceHandler;
    }
    
    /**
     * Obtiene la instancia de JDA.
     * @return JDA instance
     */
    public JDA getJDA()
    {
        return jda;
    }
    
    /**
     * Cierra la conexión de audio para un servidor específico.
     * @param guildId ID del servidor de Discord
     */
    public void closeAudioConnection(long guildId)
    {
        if(jda == null) return;
        Guild guild = jda.getGuildById(guildId);
        if(guild!=null)
            threadpool.submit(() -> guild.getAudioManager().closeAudioConnection());
    }
    
    /**
     * Resetea la actividad del bot según la configuración.
     * Si la configuración tiene una actividad definida, la establece.
     */
    public void resetGame()
    {
        if(jda == null || config == null) return;
        Activity game = null;
        if(config.getGame() != null && !config.getGame().getName().equalsIgnoreCase("none"))
            game = config.getGame();
        if(jda.getPresence() != null && !Objects.equals(jda.getPresence().getActivity(), game))
            jda.getPresence().setActivity(game);
    }

    /**
     * Apaga el bot de forma segura.
     * Cierra todas las conexiones de audio, detiene los reproductores
     * y termina todos los threads antes de salir.
     */
    public void shutdown()
    {
        if(shuttingDown)
            return;
        shuttingDown = true;
        if(threadpool != null)
            threadpool.shutdownNow();
        if(jda != null && jda.getStatus()!=JDA.Status.SHUTTING_DOWN)
        {
            jda.getGuilds().stream().forEach(g -> 
            {
                if(g != null && g.getAudioManager() != null)
                {
                    g.getAudioManager().closeAudioConnection();
                    AudioHandler ah = (AudioHandler)g.getAudioManager().getSendingHandler();
                    if(ah!=null)
                    {
                        ah.stopAndClear();
                        if(ah.getPlayer() != null)
                            ah.getPlayer().destroy();
                    }
                }
            });
            jda.shutdown();
        }
        System.exit(0);
    }

    /**
     * Establece la instancia de JDA del bot.
     * @param jda Instancia de JDA a establecer
     */
    public void setJDA(JDA jda)
    {
        this.jda = jda;
    }
}

