package com.soundbot;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Handler para manejar los resultados de carga de audio.
 */
public class TrackLoadHandler implements AudioLoadResultHandler {
    private final MessageReceivedEvent event;
    private final TrackScheduler scheduler;
    private final String query;
    
    public TrackLoadHandler(MessageReceivedEvent event, TrackScheduler scheduler, String query) {
        this.event = event;
        this.scheduler = scheduler;
        this.query = query;
    }
    
    @Override
    public void trackLoaded(AudioTrack track) {
        scheduler.queue(track);
        event.getChannel().sendMessage("Agregado a la cola: **" + track.getInfo().title + "**").queue();
    }
    
    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        if (playlist.getTracks().isEmpty()) {
            event.getChannel().sendMessage("No se encontraron canciones.").queue();
            return;
        }
        
        for (AudioTrack track : playlist.getTracks()) {
            scheduler.queue(track);
        }
        
        event.getChannel().sendMessage("Agregadas " + playlist.getTracks().size() + " canciones a la cola.").queue();
    }
    
    @Override
    public void noMatches() {
        event.getChannel().sendMessage("No se encontró nada para: " + query).queue();
    }
    
    @Override
    public void loadFailed(FriendlyException exception) {
        event.getChannel().sendMessage("Error al cargar: " + exception.getMessage()).queue();
    }
}

