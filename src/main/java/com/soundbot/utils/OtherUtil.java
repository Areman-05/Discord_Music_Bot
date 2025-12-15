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
package com.soundbot.utils;

import com.soundbot.SoundBot;
import com.soundbot.entities.Prompt;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.User;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 *
 * @author SoundBot Contributors
 */
public class OtherUtil
{
    public final static String NEW_VERSION_AVAILABLE = "Hay una nueva version de SoundBot disponible!\n"
                    + "Version actual: %s\n"
                    + "Nueva Version: %s\n\n"
                    + "Por favor visita la pagina de releases para obtener la ultima version.";
    private final static String WINDOWS_INVALID_PATH = "c:\\windows\\system32\\";
    
    /**
     * gets a Path from a String
     * also fixes the windows tendency to try to start in system32
     * any time the bot tries to access this path, it will instead start in the location of the jar file
     * 
     * @param path the string path
     * @return the Path object
     */
    public static Path getPath(String path)
    {
        if(path == null || path.isEmpty())
            return Paths.get(".");
        Path result = Paths.get(path);
        // special logic to prevent trying to access system32
        try
        {
            String absPath = result.toAbsolutePath().toString().toLowerCase();
            if(absPath.startsWith(WINDOWS_INVALID_PATH))
            {
                try
                {
                    File jarFile = new File(SoundBot.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                    File parentDir = jarFile.getParentFile();
                    if(parentDir != null)
                        result = Paths.get(parentDir.getPath() + File.separator + path);
                }
                catch(URISyntaxException | NullPointerException ignored) {}
            }
        }
        catch(Exception ignored) {}
        return result;
    }
    
    /**
     * Loads a resource from the jar as a string
     * 
     * @param clazz class base object
     * @param name name of resource
     * @return string containing the contents of the resource
     */
    public static String loadResource(Object clazz, String name)
    {
        if(clazz == null || name == null || name.isEmpty())
            return null;
        try
        {
            InputStream stream = clazz.getClass().getResourceAsStream(name);
            if(stream == null)
                return null;
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream)))
            {
                StringBuilder sb = new StringBuilder();
                reader.lines().forEach(line -> sb.append("\r\n").append(line));
                return sb.toString().trim();
            }
        }
        catch(IOException | NullPointerException ignored)
        {
            return null;
        }
    }
    
    /**
     * Loads image data from a URL
     * 
     * @param url url of image
     * @return inputstream of url
     */
    @SuppressWarnings("deprecation")
    public static InputStream imageFromUrl(String url)
    {
        if(url==null)
            return null;
        try 
        {
            URL u = new URL(url);
            URLConnection urlConnection = u.openConnection();
            urlConnection.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36");
            return urlConnection.getInputStream();
        }
        catch(IOException | IllegalArgumentException ignore) {}
        return null;
    }
    
    /**
     * Parses an activity from a string
     * 
     * @param game the game, including the action such as 'playing' or 'watching'
     * @return the parsed activity
     */
    public static Activity parseGame(String game)
    {
        if(game==null || game.trim().isEmpty() || game.trim().equalsIgnoreCase("default"))
            return null;
        String trimmed = game.trim();
        String lower = trimmed.toLowerCase();
        if(lower.startsWith("playing"))
        {
            String content = trimmed.length() > 7 ? trimmed.substring(7).trim() : "";
            return Activity.playing(makeNonEmpty(content));
        }
        if(lower.startsWith("listening to"))
        {
            String content = trimmed.length() > 12 ? trimmed.substring(12).trim() : "";
            return Activity.listening(makeNonEmpty(content));
        }
        if(lower.startsWith("listening"))
        {
            String content = trimmed.length() > 9 ? trimmed.substring(9).trim() : "";
            return Activity.listening(makeNonEmpty(content));
        }
        if(lower.startsWith("watching"))
        {
            String content = trimmed.length() > 8 ? trimmed.substring(8).trim() : "";
            return Activity.watching(makeNonEmpty(content));
        }
        if(lower.startsWith("streaming"))
        {
            String content = trimmed.length() > 9 ? trimmed.substring(9).trim() : "";
            if(content.isEmpty())
                return Activity.streaming("\u200B", "https://twitch.tv/");
            String[] parts = content.split("\\s+", 2);
            if(parts.length == 2 && parts[0] != null && parts[1] != null)
            {
                return Activity.streaming(makeNonEmpty(parts[1]), "https://twitch.tv/"+parts[0]);
            }
        }
        return Activity.playing(makeNonEmpty(trimmed));
    }
   
    public static String makeNonEmpty(String str)
    {
        return str == null || str.isEmpty() ? "\u200B" : str;
    }
    
    public static OnlineStatus parseStatus(String status)
    {
        if(status==null || status.trim().isEmpty())
            return OnlineStatus.ONLINE;
        OnlineStatus st = OnlineStatus.fromKey(status);
        return st == null ? OnlineStatus.ONLINE : st;
    }
    
    public static void checkJavaVersion(Prompt prompt)
    {
        String vmName = System.getProperty("java.vm.name");
        if(vmName == null || !vmName.contains("64"))
            prompt.alert(Prompt.Level.WARNING, "Version de Java", 
                    "Parece que no estas usando una version de Java soportada. Por favor usa Java de 64 bits.");
    }
    
    public static void checkVersion(Prompt prompt)
    {
        // Get current version number
        String version = getCurrentVersion();
        
        // Check for new version
        String latestVersion = getLatestVersion();
        
        if(latestVersion!=null && !latestVersion.equals(version) && prompt != null)
        {
            prompt.alert(Prompt.Level.WARNING, "Version de SoundBot", String.format(NEW_VERSION_AVAILABLE, version, latestVersion));
        }
    }
    
    public static String getCurrentVersion()
    {
        if(SoundBot.class.getPackage()!=null && SoundBot.class.getPackage().getImplementationVersion()!=null)
            return SoundBot.class.getPackage().getImplementationVersion();
        else
            return "UNKNOWN";
    }
    
    public static String getLatestVersion()
    {
        try
        {
            OkHttpClient client = new OkHttpClient.Builder().build();
            Request request = new Request.Builder()
                    .get()
                    .url("https://api.github.com/repos/soundbot/SoundBot/releases/latest")
                    .build();
            try(Response response = client.newCall(request).execute())
            {
                ResponseBody body = response.body();
                if(body != null)
                {
                    try(Reader reader = body.charStream())
                    {
                        JSONObject obj = new JSONObject(new JSONTokener(reader));
                        return obj.optString("tag_name", null);
                    }
                    catch(JSONException ex)
                    {
                        return null;
                    }
                }
                else
                {
                    return null;
                }
            }
        }
        catch(IOException | NullPointerException ex)
        {
            return null;
        }
    }

    /**
     * Checks if the bot SoundBot is being run on is supported & returns the reason if it is not.
     * @return A string with the reason, or null if it is supported.
     */
    public static String getUnsupportedBotReason(JDA jda) 
    {
        if(jda == null || jda.getSelfUser() == null)
            return null;
        if (jda.getSelfUser().getFlags().contains(User.UserFlag.VERIFIED_BOT))
            return "El bot esta verificado. Usar SoundBot en un bot verificado no esta soportado.";

        try
        {
            ApplicationInfo info = jda.retrieveApplicationInfo().complete();
            if (info != null && info.isBotPublic())
                return "\"Public Bot\" esta habilitado. Usar SoundBot como bot publico no esta soportado. Por favor deshabitalo en el "
                        + "Panel de Desarrollador en https://discord.com/developers/applications/" + jda.getSelfUser().getId() + "/bot ."
                        + "Tambien puede que necesites deshabilitar todos los Contextos de Instalacion en https://discord.com/developers/applications/" 
                        + jda.getSelfUser().getId() + "/installation .";
        }
        catch(Exception ex)
        {
            return null;
        }

        return null;
    }
}

