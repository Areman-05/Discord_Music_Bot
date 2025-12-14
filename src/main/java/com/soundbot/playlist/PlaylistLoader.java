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
package com.soundbot.playlist;

import com.soundbot.BotConfig;
import com.soundbot.utils.OtherUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author SoundBot Contributors
 */
public class PlaylistLoader
{
    private final BotConfig config;
    
    public PlaylistLoader(BotConfig config)
    {
        this.config = config;
    }
    
    public List<String> getPlaylistNames()
    {
        if(folderExists())
        {
            File folder = new File(OtherUtil.getPath(config.getPlaylistsFolder()).toString());
            return Arrays.asList(folder.listFiles((pathname) -> pathname.getName().endsWith(".txt")))
                    .stream().map(f -> f.getName().substring(0,f.getName().length()-4)).collect(Collectors.toList());
        }
        else
        {
            createFolder();
            return Collections.emptyList();
        }
    }
    
    public void createFolder()
    {
        try
        {
            Files.createDirectory(OtherUtil.getPath(config.getPlaylistsFolder()));
        } 
        catch (IOException ignore) {}
    }
    
    public boolean folderExists()
    {
        return Files.exists(OtherUtil.getPath(config.getPlaylistsFolder()));
    }
    
    public void createPlaylist(String name) throws IOException
    {
        Files.createFile(OtherUtil.getPath(config.getPlaylistsFolder()+File.separator+name+".txt"));
    }
    
    public void deletePlaylist(String name) throws IOException
    {
        Files.delete(OtherUtil.getPath(config.getPlaylistsFolder()+File.separator+name+".txt"));
    }
    
    public void writePlaylist(String name, String text) throws IOException
    {
        Files.write(OtherUtil.getPath(config.getPlaylistsFolder()+File.separator+name+".txt"), text.trim().getBytes());
    }
    
    public Playlist getPlaylist(String name)
    {
        if(!getPlaylistNames().contains(name))
            return null;
        try
        {
            if(folderExists())
            {
                boolean[] shuffle = {false};
                List<String> list = new ArrayList<>();
                Files.readAllLines(OtherUtil.getPath(config.getPlaylistsFolder()+File.separator+name+".txt")).forEach(str -> 
                {
                    String s = str.trim();
                    if(s.isEmpty())
                        return;
                    if(s.startsWith("#") || s.startsWith("//"))
                    {
                        s = s.replaceAll("\\s+", "");
                        if(s.equalsIgnoreCase("#shuffle") || s.equalsIgnoreCase("//shuffle"))
                            shuffle[0] = true;
                    }
                    else
                        list.add(s);
                });
                if(shuffle[0])
                    Collections.shuffle(list);
                return new Playlist(name, list, shuffle[0]);
            }
            else
                return null;
        }
        catch(IOException e)
        {
            return null;
        }
    }
    
    public static class Playlist
    {
        public final String name;
        public final List<String> items;
        public final boolean shuffle;
        private List<com.sedmelluq.discord.lavaplayer.track.AudioTrack> tracks;
        
        private Playlist(String name, List<String> items, boolean shuffle)
        {
            this.name = name;
            this.items = items;
            this.shuffle = shuffle;
            this.tracks = new java.util.ArrayList<>();
        }
        
        public List<String> getItems()
        {
            return items;
        }
        
        public List<com.sedmelluq.discord.lavaplayer.track.AudioTrack> getTracks()
        {
            return tracks;
        }
        
        public void loadTracks(com.soundbot.audio.PlayerManager manager, java.util.function.Consumer<com.sedmelluq.discord.lavaplayer.track.AudioTrack> consumer, Runnable callback)
        {
            if(items.isEmpty())
            {
                callback.run();
                return;
            }
            manager.loadItemOrdered(manager, items.get(0), new com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler() {
                private int index = 0;
                @Override
                public void trackLoaded(com.sedmelluq.discord.lavaplayer.track.AudioTrack track) {
                    tracks.add(track);
                    consumer.accept(track);
                    if(++index < items.size())
                        manager.loadItemOrdered(manager, items.get(index), this);
                    else
                        callback.run();
                }
                @Override
                public void playlistLoaded(com.sedmelluq.discord.lavaplayer.track.AudioPlaylist playlist) {
                    if(playlist.getSelectedTrack() != null)
                        trackLoaded(playlist.getSelectedTrack());
                    else if(!playlist.getTracks().isEmpty())
                        playlist.getTracks().forEach(track -> {
                            tracks.add(track);
                            consumer.accept(track);
                        });
                    if(++index < items.size())
                        manager.loadItemOrdered(manager, items.get(index), this);
                    else
                        callback.run();
                }
                @Override
                public void noMatches() {
                    if(++index < items.size())
                        manager.loadItemOrdered(manager, items.get(index), this);
                    else
                        callback.run();
                }
                @Override
                public void loadFailed(com.sedmelluq.discord.lavaplayer.tools.FriendlyException exception) {
                    if(++index < items.size())
                        manager.loadItemOrdered(manager, items.get(index), this);
                    else
                        callback.run();
                }
            });
        }
    }
}

