package com.flux.discord.gachi;

import com.flux.discord.gachi.configuration.model.Bot;
import com.flux.discord.gachi.configuration.model.listeners.impl.Listener;
import com.sun.tools.javac.Main;
import net.dv8tion.jda.api.JDABuilder;
import org.apache.commons.validator.routines.UrlValidator;
import org.javacord.api.DiscordApiBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.security.auth.login.LoginException;

import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGES;
import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_VOICE_STATES;

@SpringBootApplication
@EnableConfigurationProperties(Bot.class)
public class GachiBotMain {

    @Autowired private Bot bot;
    @Autowired private Listener.PingListener pingListener;
    @Autowired private Listener.ReactionListener reactionListener;
    @Autowired private Listener.PlayListener playListener;
    @Autowired private Listener.MusicListener musicListener;

    public static void main(String[] args) {
        SpringApplication.run(GachiBotMain.class, args);
    }
//    Javacord API
//    @Bean
//    @Deprecated
//    public void DiscordApi() {
//        new DiscordApiBuilder()
//                .setToken(bot.getToken())
//                .addMessageCreateListener(pingListener)
//                .addMessageCreateListener(playListener)
//                .addReactionAddListener(reactionListener)
//                .login()
//                .join();
//    }

//  JDA API
    @Bean
    public void DiscordApi() throws LoginException {
        JDABuilder.create(bot.getToken(), GUILD_MESSAGES, GUILD_VOICE_STATES)
                .addEventListeners(musicListener)
                .build();
    }
}
