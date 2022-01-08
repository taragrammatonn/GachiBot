package com.flux.discord.gachi.configuration.model.audio.random;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RandomTrackScheduler extends AudioEventAdapter {
    private final AudioPlayer player;
    private Set<AudioTrack> queue;

    public RandomTrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new HashSet<>();
    }

    public void queue(AudioPlaylist playlist) {
        queue.addAll(playlist.getTracks());
    }

    public void queue(AudioTrack audioTrack) {
        if (!player.startTrack(audioTrack, true)) {
            queue.add(audioTrack);
        }
    }

    public void nextTrack(AudioTrack currentAudioTrack) {
        // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
        // giving null to startTrack, which is a valid argument and will simply stop the player.
        this.queue = queue.stream().filter(
                x -> !x.getInfo().identifier.equals(currentAudioTrack.getIdentifier())
        ).collect(Collectors.toSet());
        player.startTrack(queue.iterator().next(), false);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
        if (endReason.mayStartNext) {
            nextTrack(track);
        }
    }
}
