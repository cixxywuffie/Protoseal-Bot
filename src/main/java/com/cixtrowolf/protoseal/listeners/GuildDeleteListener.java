package com.cixtrowolf.protoseal.listeners;

import com.cixtrowolf.protoseal.persistence.privacy.GuildDataCleanupService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class GuildDeleteListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuildDeleteListener.class);
    private final GuildDataCleanupService cleanupService;

    @Autowired
    public GuildDeleteListener(GatewayDiscordClient client, GuildDataCleanupService cleanupService) {
        this(cleanupService);
        client.on(GuildDeleteEvent.class, this::handle).subscribe();
    }

    GuildDeleteListener(GuildDataCleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    Mono<Void> handle(GuildDeleteEvent event) {
        String guildId = event.getGuildId().asString();
        if (event.isUnavailable()) {
            LOGGER.info("Guild temporarily unavailable; preserving data guildId={}", guildId);
            return Mono.empty();
        }

        return Mono.fromRunnable(() -> cleanupService.deleteForGuild(guildId))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(ignored -> LOGGER.info("Deleted removed guild data guildId={}", guildId))
                .doOnError(error -> LOGGER.error("Failed to delete removed guild data guildId={}", guildId, error))
                .then();
    }
}
