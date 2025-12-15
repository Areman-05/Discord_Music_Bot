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
package com.soundbot.audio;

import com.soundbot.playlist.PlaylistLoader.Playlist;
import com.soundbot.queue.AbstractQueue;
import com.soundbot.settings.QueueType;
import com.soundbot.utils.TimeUtil;
import com.soundbot.settings.RepeatMode;
import com.soundbot.utils.FormatUtil;
import com.soundbot.settings.Settings;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.nio.ByteBuffer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.LoggerFactory;

/**
 *
 * @author SoundBot Contributors
 */
public class AudioHandler extends AudioEventAdapter implements AudioSendHandler 
{
    public final static String PLAY_EMOJI  = "\u25B6"; // ▶
    public final static String PAUSE_EMOJI = "\u23F8"; // ⏸
    public final static String STOP_EMOJI  = "\u23F9"; // ⏹


    private final List<AudioTrack> defaultQueue = new LinkedList<>();
    private final Set<String> votes = new HashSet<>();
    
    private final PlayerManager manager;
    private final AudioPlayer audioPlayer;
    private final long guildId;
    
    private AudioFrame lastFrame;
    private AbstractQueue<QueuedTrack> queue;

    protected AudioHandler(PlayerManager manager, Guild guild, AudioPlayer player)
    {
        this.manager = manager;
        this.audioPlayer = player;
        this.guildId = guild.getIdLong();

        this.setQueueType(manager.getBot().getSettingsManager().getSettings(guildId).getQueueType());
    }

    public void setQueueType(QueueType type)
    {
        queue = type.createInstance(queue);
    }

    public int addTrackToFront(QueuedTrack qtrack)
    {
        if(audioPlayer.getPlayingTrack()==null)
        {
            audioPlayer.playTrack(qtrack.getTrack());
            return -1;
        }
        else
        {
            queue.addAt(0, qtrack);
            return 0;
        }
    }
    
    public int addTrack(QueuedTrack qtrack)
    {
        if(audioPlayer.getPlayingTrack()==null)
        {
            audioPlayer.playTrack(qtrack.getTrack());
            return -1;
        }
        else
            return queue.add(qtrack);
    }
    
    public AbstractQueue<QueuedTrack> getQueue()
    {
        return queue;
    }
    
    public void stopAndClear()
    {
        queue.clear();
        defaultQueue.clear();
        audioPlayer.stopTrack();
    }
    
    public boolean isMusicPlaying(JDA jda)
    {
        Guild g = guild(jda);
        if(g == null || g.getSelfMember() == null || g.getSelfMember().getVoiceState() == null)
            return false;
        return g.getSelfMember().getVoiceState().inVoiceChannel() && audioPlayer.getPlayingTrack()!=null;
    }
    
    public Set<String> getVotes()
    {
        return votes;
    }
    
    public AudioPlayer getPlayer()
    {
        return audioPlayer;
    }
    
    public RequestMetadata getRequestMetadata()
    {
        if(audioPlayer.getPlayingTrack() == null)
            return RequestMetadata.EMPTY;
        RequestMetadata rm = audioPlayer.getPlayingTrack().getUserData(RequestMetadata.class);
        return rm == null ? RequestMetadata.EMPTY : rm;
    }
    
    public boolean playFromDefault()
    {
        if(!defaultQueue.isEmpty())
        {
            AudioTrack track = defaultQueue.remove(0);
            if(track != null)
                audioPlayer.playTrack(track);
            return true;
        }
        if(manager == null || manager.getBot() == null || manager.getBot().getSettingsManager() == null)
            return false;
        Settings settings = manager.getBot().getSettingsManager().getSettings(guildId);
        if(settings==null || settings.getDefaultPlaylist()==null)
            return false;
        
        if(manager.getBot().getPlaylistLoader() == null)
            return false;
        Playlist pl = manager.getBot().getPlaylistLoader().getPlaylist(settings.getDefaultPlaylist());
        if(pl==null || pl.getItems()==null || pl.getItems().isEmpty())
            return false;
        pl.loadTracks(manager, (at) -> 
        {
            if(at != null)
            {
                if(audioPlayer.getPlayingTrack()==null)
                    audioPlayer.playTrack(at);
                else
                    defaultQueue.add(at);
            }
        }, () -> 
        {
            if(pl.getTracks()==null || (pl.getTracks().isEmpty() && manager.getBot().getConfig() != null && !manager.getBot().getConfig().getStay()))
                manager.getBot().closeAudioConnection(guildId);
        });
        return true;
    }
    
    // Audio Events
    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) 
    {
        if(player == null || track == null || manager == null || manager.getBot() == null)
            return;
        if(manager.getBot().getSettingsManager() == null)
            return;
        RepeatMode repeatMode = manager.getBot().getSettingsManager().getSettings(guildId).getRepeatMode();
        if(endReason==AudioTrackEndReason.FINISHED && repeatMode != RepeatMode.OFF && queue != null)
        {
            try
            {
                AudioTrack cloned = track.makeClone();
                if(cloned != null)
                {
                    QueuedTrack clone = new QueuedTrack(cloned, track.getUserData(RequestMetadata.class));
                    if(repeatMode == RepeatMode.ALL)
                        queue.add(clone);
                    else
                        queue.addAt(0, clone);
                }
            }
            catch(Exception ex)
            {
                // Ignore clone errors
            }
        }
        
        if(queue == null || queue.isEmpty())
        {
            if(!playFromDefault())
            {
                if(manager.getBot().getNowplayingHandler() != null)
                    manager.getBot().getNowplayingHandler().onTrackUpdate(null);
                if(manager.getBot().getConfig() != null && !manager.getBot().getConfig().getStay())
                    manager.getBot().closeAudioConnection(guildId);
                player.setPaused(false);
            }
        }
        else
        {
            QueuedTrack qt = queue.pull();
            if(qt != null && qt.getTrack() != null)
                player.playTrack(qt.getTrack());
        }
    }
    
    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) 
    {
        if(votes != null)
            votes.clear();
        if(manager != null && manager.getBot() != null && manager.getBot().getNowplayingHandler() != null)
            manager.getBot().getNowplayingHandler().onTrackUpdate(track);
    }
    
    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) 
    {
        String msg = exception != null ? exception.getMessage() : "Excepcion desconocida";
        LoggerFactory.getLogger(getClass()).warn("Excepcion en track: " + msg);
    }
    
    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) 
    {
        String title = track != null && track.getInfo() != null ? track.getInfo().title : "track desconocido";
        LoggerFactory.getLogger(getClass()).warn("Track atascado: " + title);
    }
    
    // AudioSendHandler implementation
    @Override
    public boolean canProvide() 
    {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Override
    public ByteBuffer provide20MsAudio() 
    {
        if(lastFrame == null || lastFrame.getData() == null)
            return null;
        return ByteBuffer.wrap(lastFrame.getData());
    }

    @Override
    public boolean isOpus() 
    {
        return true;
    }
    
    // Helper methods
    private Guild guild(JDA jda)
    {
        return jda.getGuildById(guildId);
    }
    
    public Message getNowPlaying(JDA jda)
    {
        if(isMusicPlaying(jda))
        {
            Guild guild = guild(jda);
            if(guild == null || guild.getSelfMember() == null)
                return getNoMusicPlaying(jda);
            AudioTrack track = audioPlayer.getPlayingTrack();
            if(track == null)
                return getNoMusicPlaying(jda);
            MessageBuilder mb = new MessageBuilder();
            String channelName = "canal desconocido";
            if(guild.getSelfMember().getVoiceState() != null 
                && guild.getSelfMember().getVoiceState().getChannel() != null)
            {
                channelName = guild.getSelfMember().getVoiceState().getChannel().getName();
            }
            String successEmoji = manager.getBot().getConfig() != null ? manager.getBot().getConfig().getSuccess() : "🎶";
            mb.append(FormatUtil.filter(successEmoji+" **Reproduciendo en "+channelName+"...**"));
            EmbedBuilder eb = new EmbedBuilder();
            if(guild.getSelfMember().getColor() != null)
                eb.setColor(guild.getSelfMember().getColor());
            RequestMetadata rm = getRequestMetadata();
            if(rm != null && rm.getOwner() != 0 && rm.user != null)
            {
                User u = guild.getJDA().getUserById(rm.user.id);
                if(u == null)
                    eb.setAuthor(rm.user.username + "#" + rm.user.discrim, null, rm.user.avatar);
                else
                    eb.setAuthor(u.getName() + "#" + u.getDiscriminator(), null, u.getEffectiveAvatarUrl());
            }

            try 
            {
                if(track.getInfo() != null)
                {
                    if(track.getInfo().uri != null)
                        eb.setTitle(track.getInfo().title, track.getInfo().uri);
                    else
                        eb.setTitle(track.getInfo().title);
                }
            }
            catch(Exception e) 
            {
                if(track.getInfo() != null)
                    eb.setTitle(track.getInfo().title);
            }

            if(track.getInfo() != null && track.getInfo().length != Long.MAX_VALUE)
            {
                String time = TimeUtil.formatTime(track.getPosition()) + " / " + TimeUtil.formatTime(track.getDuration());
                eb.setFooter(time, null);
            }

            return mb.setEmbed(eb.build()).build();
        }
        else return getNoMusicPlaying(jda);
    }
    
    public Message getNoMusicPlaying(JDA jda)
    {
            Guild guild = guild(jda);
            String successEmoji = manager != null && manager.getBot() != null && manager.getBot().getConfig() != null ? manager.getBot().getConfig().getSuccess() : "🎶";
            if(guild == null || guild.getSelfMember() == null)
                return new MessageBuilder()
                    .setContent(FormatUtil.filter(successEmoji+" **No hay musica reproduciendose**"))
                    .build();
            int volume = 100;
            if(manager != null && manager.getBot() != null && manager.getBot().getSettingsManager() != null)
            {
                Settings s = manager.getBot().getSettingsManager().getSettings(guild);
                if(s != null)
                    volume = s.getVolume();
            }
            EmbedBuilder eb = new EmbedBuilder()
                .setTitle("No hay musica reproduciendose")
                .setDescription(FormatUtil.progressBar(-1) + " " + FormatUtil.volumeIcon(volume));
            if(guild.getSelfMember().getColor() != null)
                eb.setColor(guild.getSelfMember().getColor());
            return new MessageBuilder()
            .setContent(FormatUtil.filter(successEmoji+" **No hay musica reproduciendose**"))
            .setEmbed(eb.build())
            .build();
    }
}

