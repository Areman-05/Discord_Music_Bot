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
package com.soundbot.audio;

import com.dunctebot.sourcemanagers.DuncteBotSources;
import com.soundbot.Bot;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.nico.NicoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.entities.Guild;

/**
 *
 * @author SoundBot Contributors
 */
public class PlayerManager extends DefaultAudioPlayerManager
{
    private final Bot bot;
    
    public PlayerManager(Bot bot)
    {
        this.bot = bot;
    }
    
    public void init()
    {
        if(bot == null || bot.getConfig() == null)
            return;
        try
        {
            TransformativeAudioSourceManager.createTransforms(bot.getConfig().getTransforms()).forEach(t -> {
                if(t != null) registerSourceManager(t);
            });

            YoutubeAudioSourceManager yt = new YoutubeAudioSourceManager(true);
            yt.setPlaylistPageCount(bot.getConfig().getMaxYTPlaylistPages());
            registerSourceManager(yt);

            registerSourceManager(SoundCloudAudioSourceManager.createDefault());
            registerSourceManager(new BandcampAudioSourceManager());
            registerSourceManager(new VimeoAudioSourceManager());
            registerSourceManager(new TwitchStreamAudioSourceManager());
            registerSourceManager(new BeamAudioSourceManager());
            registerSourceManager(new GetyarnAudioSourceManager());
            registerSourceManager(new NicoAudioSourceManager());
            registerSourceManager(new HttpAudioSourceManager(MediaContainerRegistry.DEFAULT_REGISTRY));

            AudioSourceManagers.registerLocalSource(this);

            DuncteBotSources.registerAll(this, "en-US");
        }
        catch(Exception ex)
        {
            // Log error but continue
        }
    }
    
    public Bot getBot()
    {
        return bot;
    }
    
    public boolean hasHandler(Guild guild)
    {
        return guild != null && guild.getAudioManager() != null && guild.getAudioManager().getSendingHandler()!=null;
    }
    
    public AudioHandler setUpHandler(Guild guild)
    {
        if(guild == null || guild.getAudioManager() == null)
            return null;
        AudioHandler handler;
        if(guild.getAudioManager().getSendingHandler()==null)
        {
            AudioPlayer player = createPlayer();
            if(player != null)
            {
                player.setVolume(bot.getSettingsManager().getSettings(guild).getVolume());
                handler = new AudioHandler(this, guild, player);
                player.addListener(handler);
                guild.getAudioManager().setSendingHandler(handler);
            }
            else
            {
                return null;
            }
        }
        else
            handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
        return handler;
    }
}

