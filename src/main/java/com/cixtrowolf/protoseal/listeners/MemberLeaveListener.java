package com.cixtrowolf.protoseal.listeners;

import com.cixtrowolf.protoseal.persistence.privacy.UserDataCleanupService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class MemberLeaveListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemberLeaveListener.class);
    private final UserDataCleanupService cleanupService;

    public MemberLeaveListener(GatewayDiscordClient client, UserDataCleanupService cleanupService) {
        this.cleanupService = cleanupService;
        client.on(MemberLeaveEvent.class, this::handle).subscribe();
    }

    Mono<Void> handle(MemberLeaveEvent event) {
        String guildId = event.getGuildId().asString();
        String userId = event.getUser().getId().asString();

        return Mono.fromRunnable(() -> cleanupService.deleteForMember(guildId, userId))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(ignored -> LOGGER.info(
                        "Deleted departing member data guildId={} userId={}", guildId, userId))
                .doOnError(error -> LOGGER.error(
                        "Failed to delete departing member data guildId={} userId={}", guildId, userId, error))
                .then();
    }
}
