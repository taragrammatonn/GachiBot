package com.flux.discord.gachi.configuration.model.listeners;

import org.springframework.beans.factory.annotation.Autowired;

public interface BotListener {

    @Autowired
    default String[] commands() {
        return new String[]{getInputCommand()};
    }

    String getInputCommand();
}
