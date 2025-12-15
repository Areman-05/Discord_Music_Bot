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

import com.soundbot.utils.OtherUtil;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author SoundBot Contributors
 */
public class Listener extends ListenerAdapter
{
    private final Bot bot;
    
    public Listener(Bot bot)
    {
        this.bot = bot;
    }
    
    @Override
    public void onReady(@NotNull ReadyEvent event) 
    {
        if(event.getJDA().getGuildCache().isEmpty())
        {
            Logger log = LoggerFactory.getLogger("SoundBot");
            log.warn("Este bot no esta en ningun servidor! Usa el siguiente enlace para agregar el bot a tus servidores!");
            String inviteUrl = event.getJDA().getInviteUrl(SoundBot.RECOMMENDED_PERMS);
            if(inviteUrl != null)
                log.warn(inviteUrl);
        }
        credit(event.getJDA());
        event.getJDA().getGuilds().forEach((guild) -> 
        {
            try
            {
                String defpl = bot.getSettingsManager().getSettings(guild).getDefaultPlaylist();
                VoiceChannel vc = bot.getSettingsManager().getSettings(guild).getVoiceChannel(guild);
                if(defpl!=null && vc!=null)
                {
                    if(bot.getPlayerManager().setUpHandler(guild).playFromDefault())
                    {
                        guild.getAudioManager().openAudioConnection(vc);
                    }
                }
            }
            catch(Exception ignore) {}
        });
        if(bot.getConfig().useUpdateAlerts())
        {
            bot.getThreadpool().scheduleWithFixedDelay(() -> 
            {
                try
                {
                    if(bot.getJDA() != null)
                    {
                        User owner = bot.getJDA().retrieveUserById(bot.getConfig().getOwnerId()).complete();
                        if(owner != null)
                        {
                            String currentVersion = OtherUtil.getCurrentVersion();
                            String latestVersion = OtherUtil.getLatestVersion();
                            if(latestVersion!=null && !currentVersion.equalsIgnoreCase(latestVersion))
                            {
                                String msg = String.format(OtherUtil.NEW_VERSION_AVAILABLE, currentVersion, latestVersion);
                                owner.openPrivateChannel().queue(pc -> pc.sendMessage(msg).queue());
                            }
                        }
                    }
                }
                catch(Exception ignored) {} // ignored
            }, 0, 24, TimeUnit.HOURS);
        }
    }
    
    @Override
    public void onGuildMessageDelete(@Nonnull GuildMessageDeleteEvent event) 
    {
        bot.getNowplayingHandler().onMessageDelete(event.getGuild(), event.getMessageIdLong());
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event)
    {
        bot.getAloneInVoiceHandler().onVoiceUpdate(event);
    }
    
    @Override
    public void onGuildJoin(@Nonnull GuildJoinEvent event)
    {
        credit(event.getJDA());
    }
    
    @Override
    public void onShutdown(@Nonnull ShutdownEvent event)
    {
        bot.shutdown();
    }
    
    private void credit(JDA jda)
    {
        if(jda == null) return;
        Logger log = LoggerFactory.getLogger("SoundBot");
        log.info("SoundBot esta ejecutandose en " + jda.getGuilds().size() + " servidores!");
    }
}

