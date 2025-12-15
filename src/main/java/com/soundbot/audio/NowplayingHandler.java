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

import com.soundbot.Bot;
import com.soundbot.entities.Pair;
import com.soundbot.settings.Settings;
import com.soundbot.utils.FormatUtil;
import com.soundbot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;

/**
 *
 * @author SoundBot Contributors
 */
public class NowplayingHandler
{
    private final Bot bot;
    private final HashMap<Long,Pair<Long,Long>> lastNP; // guild -> channel,message
    
    public NowplayingHandler(Bot bot)
    {
        this.bot = bot;
        this.lastNP = new HashMap<>();
    }
    
    public void init()
    {
        if(!bot.getConfig().useNPImages())
            bot.getThreadpool().scheduleWithFixedDelay(() -> updateAll(), 0, 5, TimeUnit.SECONDS);
    }
    
    public void setLastNPMessage(Message m)
    {
        if(m != null && m.getGuild() != null && m.getTextChannel() != null)
            lastNP.put(m.getGuild().getIdLong(), new Pair<>(m.getTextChannel().getIdLong(), m.getIdLong()));
    }
    
    public void clearLastNPMessage(Guild guild)
    {
        lastNP.remove(guild.getIdLong());
    }
    
    private void updateAll()
    {
        if(bot.getJDA() == null) return;
        Set<Long> toRemove = new HashSet<>();
        for(long guildId: lastNP.keySet())
        {
            Guild guild = bot.getJDA().getGuildById(guildId);
            if(guild==null)
            {
                toRemove.add(guildId);
                continue;
            }
            Pair<Long,Long> pair = lastNP.get(guildId);
            if(pair == null)
            {
                toRemove.add(guildId);
                continue;
            }
            TextChannel tc = guild.getTextChannelById(pair.getKey());
            if(tc==null)
            {
                toRemove.add(guildId);
                continue;
            }
            AudioHandler handler = (AudioHandler)guild.getAudioManager().getSendingHandler();
            if(handler == null)
            {
                toRemove.add(guildId);
                continue;
            }
            Message msg = handler.getNowPlaying(bot.getJDA());
            if(msg==null)
            {
                msg = handler.getNoMusicPlaying(bot.getJDA());
                toRemove.add(guildId);
            }
            try 
            {
                tc.editMessageById(pair.getValue(), msg).queue(m->{}, t -> lastNP.remove(guildId));
            } 
            catch(Exception e) 
            {
                toRemove.add(guildId);
            }
        }
        toRemove.forEach(id -> lastNP.remove(id));
    }
    
    public void onMessageDelete(Guild guild, long messageId)
    {
        if(guild == null) return;
        Pair<Long,Long> pair = lastNP.get(guild.getIdLong());
        if(pair==null)
            return;
        if(pair.getValue() == messageId)
            lastNP.remove(guild.getIdLong());
    }
    
    public void onTrackUpdate(AudioTrack track)
    {
        if(bot.getConfig().getSongInStatus() && bot.getJDA() != null)
        {
            if(track!=null && track.getInfo() != null && track.getInfo().title != null && bot.getJDA().getGuilds().stream()
                .filter(g -> g != null && g.getSelfMember() != null && g.getSelfMember().getVoiceState() != null && g.getSelfMember().getVoiceState().inVoiceChannel())
                .count()<=1)
            {
                if(bot.getJDA().getPresence() != null)
                    bot.getJDA().getPresence().setActivity(Activity.listening(track.getInfo().title));
            }
            else
                bot.resetGame();
        }
    }
    
    public void updateTopic(long guildId, AudioTrack track, boolean paused)
    {
        if(bot.getJDA() == null || track == null || track.getInfo() == null)
            return;
        Guild guild = bot.getJDA().getGuildById(guildId);
        if(guild==null || guild.getSelfMember() == null || bot.getSettingsManager() == null)
            return;
        Settings settings = bot.getSettingsManager().getSettings(guild);
        if(settings == null) return;
        TextChannel tc = settings.getTextChannel(guild);
        if(tc==null || !guild.getSelfMember().hasPermission(tc, Permission.MANAGE_CHANNEL))
            return;
        String topic = tc.getTopic();
        if(topic==null || topic.isEmpty())
            topic = "\u200B";
        long duration = track.getDuration();
        if(duration == Long.MAX_VALUE || duration <= 0) return;
        String playing = (paused ? "\u23F8" : "\u25B6") + " " + FormatUtil.progressBar((double)track.getPosition()/duration) + " `[" 
                + TimeUtil.formatTime(track.getPosition()) + "/" + TimeUtil.formatTime(duration) + "]` " 
                + FormatUtil.filter(track.getInfo().title);
        if(topic.equals(playing))
            return;
        int index = playing.length() - 250;
        String shorter = index >= playing.length() - 20 ? playing : playing.substring(0, index) + "... " + FormatUtil.filter(track.getInfo().title);
        try 
        {
            tc.getManager().setTopic(shorter).queue();
        } 
        catch(PermissionException ignored) {}
    }
}

