package com.flux.discord.gachi.configuration.model.listeners.impl;

import com.flux.discord.gachi.configuration.model.audio.GuildMusicManager;
import com.flux.discord.gachi.configuration.model.audio.LavaPlayerAudioSource;
import com.flux.discord.gachi.configuration.model.listeners.BotListener;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.commons.validator.routines.UrlValidator;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.*;

@Log4j2
public abstract class Listener {

    @Component
    public static class PingListener implements MessageCreateListener, BotListener {

        @Override
        public void onMessageCreate(MessageCreateEvent messageCreateEvent) {
            if (messageCreateEvent.getMessageContent().equalsIgnoreCase(getInputCommand())) {
                messageCreateEvent.getChannel().sendMessage("Pong!");
            }
        }

        @Override
        public String getInputCommand() {
            return "!ping";
        }
    }

    @Component
    public static class ReactionListener implements ReactionAddListener {

        @Override
        public void onReactionAdd(ReactionAddEvent reactionAddEvent) {
            if (reactionAddEvent.getEmoji().equalsEmoji("👎")) {
                reactionAddEvent.deleteMessage();
            }
        }
    }

    @Component
    public static class PlayListener implements MessageCreateListener, BotListener {

        private String playLink;

        private AudioPlayerManager playerManager;
        private AudioPlayer player;

        @Override
        public void onMessageCreate(MessageCreateEvent messageCreateEvent) {
            String[] s = messageCreateEvent.getMessageContent().split(" ");

            String message = s[0];

            if (s.length > 1 && s[0].equalsIgnoreCase("!play")) {
                UrlValidator urlValidator = new UrlValidator();
                playLink = urlValidator.isValid(s[1]) ? s[1] : null;
                play(messageCreateEvent);
            }

            if (Arrays.stream(commands()).anyMatch(message::equalsIgnoreCase)) {
                if (!(player == null)) {
                    switch (message) {
                        case "!play" -> player.setPaused(false);
                        case "!pause" -> player.setPaused(true);
                        case "!stop" -> player.stopTrack();
                    }
                } else messageCreateEvent.getChannel().sendMessage("Введи ссылку пидор!");
            }
        }

        private void play(MessageCreateEvent messageCreateEvent) {
            User user = messageCreateEvent.getMessage().getAuthor().asUser().isPresent() ?
                    messageCreateEvent.getMessage().getAuthor().asUser().get() : null;

            List<ServerVoiceChannel> voiceChannels = messageCreateEvent.getServer().isPresent() ?
                    messageCreateEvent.getServer().get().getVoiceChannels() : Collections.emptyList();

            long userVoiceChannelId = 0;
            for (ServerVoiceChannel chan : voiceChannels) {
                if (user != null && user.isConnected(chan)) {
                    userVoiceChannelId = chan.getId();
                }
            }

            if (userVoiceChannelId != 0) {
                ServerVoiceChannel channel = messageCreateEvent.getApi().getServerVoiceChannelById(userVoiceChannelId).isPresent() ?
                        messageCreateEvent.getApi().getServerVoiceChannelById(userVoiceChannelId).get() : null;
                if (channel != null)
                    channel.connect().thenAccept(audioConnection -> {

                        playerManager = new DefaultAudioPlayerManager();
                        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
                        player = playerManager.createPlayer();

                        AudioSource source = new LavaPlayerAudioSource(messageCreateEvent.getApi(), player);
                        audioConnection.setAudioSource(source);

                        playerManager.loadItem(playLink, new AudioLoadResultHandler() {
                            @Override
                            public void trackLoaded(AudioTrack track) {
                                player.playTrack(track);
                            }

                            @Override
                            public void playlistLoaded(AudioPlaylist playlist) {
                                for (AudioTrack track : playlist.getTracks()) {
                                    player.playTrack(track);
                                }
                            }

                            @Override
                            public void noMatches() {
                                // Notify the user that we've got nothing
                            }

                            @Override
                            public void loadFailed(FriendlyException throwable) {
                                messageCreateEvent.getChannel().sendMessage("Вставь right link, asshole!");
                            }
                        });

                    }).exceptionally(e -> {
                        // Failed to connect to voice channel (no permissions?)
                        e.printStackTrace();
                        return null;
                    });
            }
        }

        @Override
        public String getInputCommand() {
            return "!play";
        }

        @Override
        public String[] commands() {
            return new String[]{"!play", "!stop", "!pause"};
        }
    }

    @Component
    public static class MusicListener extends ListenerAdapter {

        public static final List<String> commands = List.of("!play", "!skip", "!pause", "!stop", "!volume", "!next");

        private final AudioPlayerManager playerManager;
        private final Map<Long, GuildMusicManager> musicManagers;

        public MusicListener() {
            this.musicManagers = new HashMap<>();

            this.playerManager = new DefaultAudioPlayerManager();
            AudioSourceManagers.registerRemoteSources(playerManager);
            AudioSourceManagers.registerLocalSource(playerManager);
        }

        @Override
        public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
            String[] command = event.getMessage().getContentRaw().split(" ", 3);
            String nickname = event.getMember().getNickname() != null ? event.getMember().getNickname() : event.getMember().getUser().getName();
            log.info("Message from :" + nickname + ". Message content: " + event.getMessage().getContentRaw() + ".");

            if (command[0].contains("!") && !commands.contains(command[0]))
                event.getChannel().sendMessage("Нахуй пошёл.").queue();

            switch (command[0]) {
                case "!play" -> loadAndPlay(event.getChannel(), event.getMember().getUser().getId(), command);
                case "!skip", "!next" -> skipTrack(event.getChannel());
                case "!pause" -> pauseTrack(event.getChannel());
                case "!stop" -> stopTrack(event.getChannel());
                case "!volume" -> setVolume(event.getChannel(), command[1]);
            }
        }

        private void setVolume(final TextChannel channel, String s) {
            GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
            try {
                musicManager.player.setVolume(Integer.parseInt(s));
            } catch (Exception ex) {
                channel.sendMessage("Invalid command, asshole.").queue();
            }
        }

        private void loadAndPlay(final TextChannel channel, String userId, final String[] commands) {
            if (commands.length < 2) {
                channel.sendMessage("WE WE. What the fuck should I play, You sun of a bitch.").queue();
                return;
            }

            String trackUrl = commands.length == 3 ? commands[2] : commands[1];

            GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

            playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack audioTrack) {
                    channel.sendMessage("Adding finger to queue in your ass " + audioTrack.getInfo().title + ".").queue();
                    play(channel.getGuild(), musicManager, userId, audioTrack);
                }

                @Override
                public void playlistLoaded(AudioPlaylist audioPlaylist) {
                    AudioTrack firstTrack = audioPlaylist.getSelectedTrack();

                    if (firstTrack == null)
                        firstTrack = audioPlaylist.getTracks().get(0);

                    Runnable runnable;

                    if (audioPlaylist.getTracks().size() > 1) {
                        audioPlaylist.getTracks().remove(firstTrack);
                        List<AudioTrack> firstNElementsList = audioPlaylist.getTracks().stream().limit(60).toList();
                        AudioPlaylist playlist = new BasicAudioPlaylist(firstTrack.getIdentifier(), firstNElementsList, firstTrack, false);
                        channel.sendMessage("Found " + audioPlaylist.getTracks().size() +
                                " tracks, looks like we start GAY PARTY. :rainbow_flag: :rainbow_flag: :rainbow_flag:").queue();

                        if (commands[1].equalsIgnoreCase("!random")) {
                            runnable = () -> musicManager.randomScheduler.queue(playlist);
                        } else {
                            runnable = () -> musicManager.scheduler.queue(playlist);
                        }
                        new Thread(runnable).start();
                    }

                    channel.sendMessage("Full master added to queue " + firstTrack.getInfo().title + " (fist track for my ass " + audioPlaylist.getName() + ").").queue();

                    play(channel.getGuild(), musicManager, userId, firstTrack);
                }

                @Override
                public void noMatches() {
                    channel.sendMessage("Nothing found on this shit " + trackUrl + ". Stick your finger in my ass.").queue();
                }

                @Override
                public void loadFailed(FriendlyException e) {
                    channel.sendMessage("Suck this shit, I cant play this: " + e.getMessage()).queue();
                }
            });
        }

        private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
            long guildId = Long.parseLong(guild.getId());
            GuildMusicManager musicManager = musicManagers.get(guildId);

            if (musicManager == null) {
                musicManager = new GuildMusicManager(playerManager);
                musicManagers.put(guildId, musicManager);
            }

            guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

            return musicManager;
        }

        private void play(Guild guild, GuildMusicManager musicManager, String userId, AudioTrack track) {
            connectToFirstVoiceChannel(guild.getAudioManager(), userId);
            if (musicManager.scheduler.getQueue().isEmpty())
                musicManager.randomScheduler.queue(track);
            else musicManager.scheduler.queue(track);
        }

        private void skipTrack(TextChannel channel) {
            GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
            if (musicManager.scheduler.getQueue().isEmpty())
                musicManager.randomScheduler.nextTrack(musicManager.player.getPlayingTrack());
            else musicManager.scheduler.nextTrack();

            channel.sendMessage("Skipped to next track.").queue();
        }

        private void pauseTrack(TextChannel channel) {
            GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
            musicManager.player.setPaused(!musicManager.player.isPaused());

            channel.sendMessage(
                    musicManager.player.isPaused() ?
                            "Paused this shit." :
                            "Sticking finger..."
            ).queue();
        }

        private void stopTrack(TextChannel channel) {
            GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
            musicManager.player.stopTrack();

            channel.sendMessage("Stopped. Lets anal.").queue();
        }

        private static void connectToFirstVoiceChannel(AudioManager audioManager, String userId) {
            for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
                for (Member member : voiceChannel.getMembers()) {
                    if (member.getUser().getId().equals(userId)) {
                        audioManager.openAudioConnection(voiceChannel);
                        break;
                    }
                }
            }
        }
    }
}
