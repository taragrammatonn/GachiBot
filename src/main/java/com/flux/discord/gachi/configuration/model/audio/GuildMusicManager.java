package com.flux.discord.gachi.configuration.model.audio;

import com.flux.discord.gachi.configuration.model.audio.queue.TrackScheduler;
import com.flux.discord.gachi.configuration.model.audio.random.RandomTrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;


public class GuildMusicManager {

    public final AudioPlayer player;

    public final TrackScheduler scheduler;
    public final RandomTrackScheduler randomScheduler;

    public GuildMusicManager(AudioPlayerManager manager) {
        player = manager.createPlayer();
        scheduler = new TrackScheduler(player);
        randomScheduler = new RandomTrackScheduler(player);
        player.addListener(scheduler);
        player.addListener(randomScheduler);
    }

    public AudioPlayerSendHandler getSendHandler() {
        return new AudioPlayerSendHandler(player);
    }
}
