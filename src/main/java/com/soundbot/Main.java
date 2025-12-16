package com.soundbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.Arrays;
import java.util.List;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        String token = System.getenv("DISCORD_TOKEN");
        if (token == null || token.isEmpty()) {
            log.error("No se encontró el token de Discord. Configura la variable de entorno DISCORD_TOKEN");
            System.exit(1);
        }
        
        try {
            List<GatewayIntent> intents = Arrays.asList(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_VOICE_STATES
            );
            
            JDA jda = JDABuilder.createDefault(token)
                .enableIntents(intents)
                .addEventListeners(new MusicBot())
                .build();
                
            jda.awaitReady();
            log.info("Bot iniciado correctamente!");
        } catch (LoginException e) {
            log.error("Error al iniciar sesión: ", e);
        } catch (InterruptedException e) {
            log.error("Interrupción durante el inicio: ", e);
        }
    }
}

