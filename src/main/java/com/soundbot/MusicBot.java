package com.soundbot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import javax.annotation.Nonnull;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * Bot de música simple para Discord.
 * Maneja comandos básicos de reproducción de música.
 */
public class MusicBot extends ListenerAdapter {
    private static final String PREFIX = "!";
    private static final int MAX_QUEUE_DISPLAY = 10;
    
    private static final String MSG_NOT_IN_VOICE = "Debes estar en un canal de voz!";
    private static final String MSG_USAGE_PLAY = "Uso: !play <URL o búsqueda>";
    private static final String MSG_QUEUE_EMPTY = "La cola está vacía.";
    private static final String MSG_NO_MUSIC = "No hay música reproduciéndose.";
    private static final String MSG_STOPPED = "Música detenida y cola limpiada.";
    private static final String MSG_SKIPPED = "Canción saltada.";
    
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;
    
    public MusicBot() {
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        this.musicManagers = new HashMap<>();
    }
    
    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        
        String message = event.getMessage().getContentRaw();
        if (!message.startsWith(PREFIX)) return;
        
        String[] parts = message.substring(PREFIX.length()).trim().split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        
        switch (command) {
            case "play":
                playMusic(event, args);
                break;
            case "stop":
                stopMusic(event);
                break;
            case "skip":
                skipMusic(event);
                break;
            case "queue":
                showQueue(event);
                break;
            case "help":
                sendHelp(event);
                break;
        }
    }
    
    private void playMusic(MessageReceivedEvent event, String query) {
        if (query.isEmpty()) {
            event.getChannel().sendMessage(MSG_USAGE_PLAY).queue();
            return;
        }
        
        Member member = event.getMember();
        if (member == null) {
            return;
        }
        
        var voiceState = member.getVoiceState();
        if (voiceState == null || !voiceState.inVoiceChannel()) {
            event.getChannel().sendMessage(MSG_NOT_IN_VOICE).queue();
            return;
        }
        
        VoiceChannel voiceChannel = voiceState.getChannel();
        if (voiceChannel == null) return;
        
        Guild guild = event.getGuild();
        GuildMusicManager musicManager = getGuildMusicManager(guild);
        
        connectToVoiceChannel(guild, voiceChannel, musicManager);
        
        playerManager.loadItemOrdered(musicManager, query, new TrackLoadHandler(event, musicManager.scheduler, query));
    }
    
    private void stopMusic(MessageReceivedEvent event) {
        GuildMusicManager musicManager = getGuildMusicManager(event.getGuild());
        musicManager.player.stopTrack();
        musicManager.scheduler.clearQueue();
        event.getChannel().sendMessage(MSG_STOPPED).queue();
    }
    
    private void skipMusic(MessageReceivedEvent event) {
        GuildMusicManager musicManager = getGuildMusicManager(event.getGuild());
        if (musicManager.player.getPlayingTrack() == null) {
            event.getChannel().sendMessage(MSG_NO_MUSIC).queue();
            return;
        }
        
        musicManager.scheduler.nextTrack();
        event.getChannel().sendMessage(MSG_SKIPPED).queue();
    }
    
    private void showQueue(MessageReceivedEvent event) {
        GuildMusicManager musicManager = getGuildMusicManager(event.getGuild());
        Queue<AudioTrack> queue = musicManager.scheduler.getQueue();
        
        if (queue.isEmpty() && musicManager.player.getPlayingTrack() == null) {
            event.getChannel().sendMessage(MSG_QUEUE_EMPTY).queue();
            return;
        }
        
        StringBuilder sb = new StringBuilder("**Cola:**\n");
        AudioTrack playing = musicManager.player.getPlayingTrack();
        if (playing != null) {
            sb.append("▶️ ").append(playing.getInfo().title).append("\n");
        }
        
        int count = 0;
        for (AudioTrack track : queue) {
            if (++count > MAX_QUEUE_DISPLAY) break;
            sb.append(count).append(". ").append(track.getInfo().title).append("\n");
        }
        
        if (queue.size() > MAX_QUEUE_DISPLAY) {
            sb.append("... y ").append(queue.size() - MAX_QUEUE_DISPLAY).append(" más");
        }
        
        String queueMessage = sb.toString();
        if (queueMessage != null) {
            event.getChannel().sendMessage(queueMessage).queue();
        }
    }
    
    private void sendHelp(MessageReceivedEvent event) {
        String help = "**Comandos:**\n" +
            "`!play <URL/búsqueda>` - Reproduce música\n" +
            "`!stop` - Detiene y limpia la cola\n" +
            "`!skip` - Salta canción\n" +
            "`!queue` - Muestra la cola\n" +
            "`!help` - Esta ayuda";
        event.getChannel().sendMessage(help).queue();
    }
    
    private void connectToVoiceChannel(Guild guild, VoiceChannel channel, GuildMusicManager musicManager) {
        AudioManager audioManager = guild.getAudioManager();
        if (!audioManager.isConnected()) {
            audioManager.setSendingHandler(musicManager.sendHandler);
            audioManager.openAudioConnection(channel);
        }
    }
    
    private GuildMusicManager getGuildMusicManager(Guild guild) {
        return musicManagers.computeIfAbsent(guild.getIdLong(), id -> {
            AudioPlayer player = playerManager.createPlayer();
            TrackScheduler scheduler = new TrackScheduler(player);
            player.addListener(scheduler);
            return new GuildMusicManager(player, scheduler);
        });
    }
    
    /**
     * Clase simple para gestionar la música de un servidor.
     */
    static class GuildMusicManager {
        final AudioPlayer player;
        final TrackScheduler scheduler;
        final AudioPlayerSendHandler sendHandler;
        
        GuildMusicManager(AudioPlayer player, TrackScheduler scheduler) {
            this.player = player;
            this.scheduler = scheduler;
            this.sendHandler = new AudioPlayerSendHandler(player);
        }
    }
}

