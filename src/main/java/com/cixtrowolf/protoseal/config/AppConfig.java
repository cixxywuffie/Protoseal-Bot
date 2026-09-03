package com.cixtrowolf.protoseal.config;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.RestClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    static final IntentSet ENABLED_INTENTS = IntentSet.nonPrivileged()
            .or(IntentSet.of(Intent.GUILD_MEMBERS));

    private static String tokenHolder;

    @Value("${bot.token}")
    public void setTokenHolder(String tokenHolder) {
        AppConfig.tokenHolder = tokenHolder;
    }

    @Bean
    public GatewayDiscordClient gatewayDiscordClient() {
        return DiscordClientBuilder.create(tokenHolder).build()
                .gateway()
                .setEnabledIntents(ENABLED_INTENTS)
                .setInitialPresence(ignore -> ClientPresence.online(ClientActivity.listening("to /commands")))
                .login()
                .block();
    }

    @Bean
    public RestClient discordRestClient(GatewayDiscordClient client) {
        return client.getRestClient();
    }
}
