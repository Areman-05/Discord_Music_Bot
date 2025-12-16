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
package com.soundbot.utils;

import com.soundbot.audio.RequestMetadata.UserInfo;
import java.util.List;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;

/**
 * Utilidades para formatear texto y datos para mostrar al usuario.
 * Incluye formateo de nombres de usuario, barras de progreso,
 * iconos de volumen y listas de canales/roles.
 * 
 * @author SoundBot Contributors
 */
public class FormatUtil {

    /**
     * Formatea un nombre de usuario con su discriminador.
     * @param username Nombre de usuario
     * @param discrim Discriminador (formato antiguo de Discord)
     * @return Nombre formateado (username#discrim o solo username)
     */
    public static String formatUsername(String username, String discrim)
    {
        if(username == null)
            username = "Usuario desconocido";
        if(discrim == null || "0000".equals(discrim))
        {
            return username;
        }
        else
        {
            return username + "#" + discrim;
        }
    }

    public static String formatUsername(UserInfo userinfo)
    {
        if(userinfo == null)
            return "Usuario desconocido";
        return formatUsername(userinfo.username, userinfo.discrim);
    }

    public static String formatUsername(User user)
    {
        if(user == null)
            return "Usuario desconocido";
        return formatUsername(user.getName(), user.getDiscriminator());
    }

    /**
     * Genera una barra de progreso visual usando emojis.
     * @param percent Porcentaje de progreso (0.0 a 1.0)
     * @return String con la barra de progreso
     */
    public static String progressBar(double percent)
    {
        if(percent < 0)
            percent = 0;
        if(percent > 1)
            percent = 1;
        StringBuilder str = new StringBuilder();
        for(int i = 0; i < 12; i++)
            if(i == (int)(percent * 12))
                str.append("\uD83D\uDD18"); // 🔘
            else
                str.append("▬");
        return str.toString();
    }
    
    /**
     * Devuelve un emoji según el nivel de volumen.
     * @param volume Nivel de volumen (0-100)
     * @return Emoji representando el volumen
     */
    public static String volumeIcon(int volume)
    {
        if(volume == 0)
            return "\uD83D\uDD07"; // 🔇
        if(volume < 30)
            return "\uD83D\uDD08"; // 🔈
        if(volume < 70)
            return "\uD83D\uDD09"; // 🔉
        return "\uD83D\uDD0A";     // 🔊
    }
    
    public static String listOfTChannels(List<TextChannel> list, String query)
    {
        if(list == null || query == null)
            return "";
        String out = " Se encontraron multiples canales de texto que coinciden con \""+query+"\":";
        int size = Math.min(6, list.size());
        for(int i=0; i<size; i++)
        {
            TextChannel tc = list.get(i);
            if(tc != null)
                out+="\n - "+tc.getName()+" (<#"+tc.getId()+">)";
        }
        if(list.size()>6)
            out+="\n**Y "+(list.size()-6)+" mas...**";
        return out;
    }
    
    public static String listOfVChannels(List<VoiceChannel> list, String query)
    {
        if(list == null || query == null)
            return "";
        String out = " Se encontraron multiples canales de voz que coinciden con \""+query+"\":";
        int size = Math.min(6, list.size());
        for(int i=0; i<size; i++)
        {
            VoiceChannel vc = list.get(i);
            if(vc != null)
                out+="\n - "+vc.getAsMention()+" (ID:"+vc.getId()+")";
        }
        if(list.size()>6)
            out+="\n**Y "+(list.size()-6)+" mas...**";
        return out;
    }
    
    public static String listOfRoles(List<Role> list, String query)
    {
        if(list == null || query == null)
            return "";
        String out = " Se encontraron multiples roles que coinciden con \""+query+"\":";
        int size = Math.min(6, list.size());
        for(int i=0; i<size; i++)
        {
            Role role = list.get(i);
            if(role != null)
                out+="\n - "+role.getName()+" (ID:"+role.getId()+")";
        }
        if(list.size()>6)
            out+="\n**Y "+(list.size()-6)+" mas...**";
        return out;
    }
    
    public static String filter(String input)
    {
        if(input == null)
            return "";
        return input.replace("\u202E","")
                .replace("@everyone", "@\u0435veryone") // cyrillic letter e
                .replace("@here", "@h\u0435re") // cyrillic letter e
                .trim();
    }
}

