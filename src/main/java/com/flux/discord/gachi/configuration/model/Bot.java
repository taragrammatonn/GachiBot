package com.flux.discord.gachi.configuration.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@Setter(AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "discord")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Bot {

    @Value("${discord.token}")
    String token;
    @Value("${discord.oauth2.clientId}")
    String clientId;
    @Value("${discord.oauth2.clientSecret}")
    String clientSecret;

}
