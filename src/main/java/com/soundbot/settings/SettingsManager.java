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
package com.soundbot.settings;

import com.jagrosh.jdautilities.command.GuildSettingsManager;
import com.soundbot.utils.OtherUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author SoundBot Contributors
 */
public class SettingsManager implements GuildSettingsManager<Settings>
{
    private final static Logger LOG = LoggerFactory.getLogger("Settings");
    private final static String SETTINGS_FILE = "serversettings.json";
    private final HashMap<Long,Settings> settings;

    public SettingsManager()
    {
        this.settings = new HashMap<>();

        try {
            Path settingsPath = OtherUtil.getPath(SETTINGS_FILE);
            if(!Files.exists(settingsPath))
            {
                LOG.info("serversettings.json se creara en " + settingsPath.toAbsolutePath());
                Files.write(settingsPath, new JSONObject().toString(4).getBytes());
                return;
            }
            byte[] fileBytes = Files.readAllBytes(settingsPath);
            if(fileBytes == null || fileBytes.length == 0)
            {
                Files.write(settingsPath, new JSONObject().toString(4).getBytes());
                return;
            }
            JSONObject loadedSettings = new JSONObject(new String(fileBytes));
            loadedSettings.keySet().forEach((id) -> {
                try
                {
                    JSONObject o = loadedSettings.getJSONObject(id);
                    if(o == null) return;

                    // Legacy version support: On versions 0.3.3 and older, the repeat mode was represented as a boolean.
                    if (!o.has("repeat_mode") && o.has("repeat") && o.getBoolean("repeat"))
                        o.put("repeat_mode", RepeatMode.ALL);

                    settings.put(Long.parseLong(id), new Settings(this,
                            o.has("text_channel_id") ? o.getString("text_channel_id")            : null,
                            o.has("voice_channel_id")? o.getString("voice_channel_id")           : null,
                            o.has("dj_role_id")      ? o.getString("dj_role_id")                 : null,
                            o.has("volume")          ? o.getInt("volume")                        : 100,
                            o.has("default_playlist")? o.getString("default_playlist")           : null,
                            o.has("repeat_mode")     ? o.getEnum(RepeatMode.class, "repeat_mode"): RepeatMode.OFF,
                            o.has("prefix")          ? o.getString("prefix")                     : null,
                            o.has("skip_ratio")      ? o.getDouble("skip_ratio")                 : -1,
                            o.has("queue_type")      ? o.getEnum(QueueType.class, "queue_type")  : QueueType.FAIR));
                }
                catch(Exception ex)
                {
                    LOG.warn("Error al cargar configuracion para servidor " + id + ": " + ex.getMessage());
                }
            });
        } catch (NoSuchFileException e) {
            // create an empty json file
            try {
                LOG.info("serversettings.json se creara en " + OtherUtil.getPath("serversettings.json").toAbsolutePath());
                Files.write(OtherUtil.getPath("serversettings.json"), new JSONObject().toString(4).getBytes());
            } catch(IOException ex) {
                LOG.warn("Error al crear nuevo archivo de configuracion: "+ex);
            }
            return;
        } catch(IOException | JSONException e) {
            LOG.warn("Error al cargar configuraciones del servidor: "+e);
        }

        LOG.info("serversettings.json cargado desde " + OtherUtil.getPath("serversettings.json").toAbsolutePath());
    }

    /**
     * Gets non-null settings for a Guild
     *
     * @param guild the guild to get settings for
     * @return the existing settings, or new settings for that guild
     */
    @Override
    public Settings getSettings(Guild guild)
    {
        if(guild == null)
            return createDefaultSettings();
        return getSettings(guild.getIdLong());
    }

    public Settings getSettings(long guildId)
    {
        return settings.computeIfAbsent(guildId, id -> createDefaultSettings());
    }

    private Settings createDefaultSettings()
    {
        return new Settings(this, 0, 0, 0, 100, null, RepeatMode.OFF, null, -1, QueueType.FAIR);
    }

    protected void writeSettings()
    {
        JSONObject obj = new JSONObject();
        settings.keySet().stream().forEach(key -> {
            Settings s = settings.get(key);
            if(s == null) return;
            JSONObject o = new JSONObject();
            if(s.textId!=0)
                o.put("text_channel_id", Long.toString(s.textId));
            if(s.voiceId!=0)
                o.put("voice_channel_id", Long.toString(s.voiceId));
            if(s.roleId!=0)
                o.put("dj_role_id", Long.toString(s.roleId));
            if(s.getVolume()!=100)
                o.put("volume",s.getVolume());
            if(s.getDefaultPlaylist() != null)
                o.put("default_playlist", s.getDefaultPlaylist());
            if(s.getRepeatMode()!=RepeatMode.OFF)
                o.put("repeat_mode", s.getRepeatMode());
            if(s.getPrefix() != null)
                o.put("prefix", s.getPrefix());
            if(s.getSkipRatio() != -1)
                o.put("skip_ratio", s.getSkipRatio());
            if(s.getQueueType() != QueueType.FAIR)
                o.put("queue_type", s.getQueueType().name());
            obj.put(Long.toString(key), o);
        });
        try {
            Files.write(OtherUtil.getPath(SETTINGS_FILE), obj.toString(4).getBytes());
        } catch(IOException ex){
            LOG.warn("Error al escribir en el archivo: "+ex);
        }
    }
}

