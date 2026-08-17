package com.cixtrowolf.protoseal.listeners;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.common.util.Snowflake;
import com.cixtrowolf.protoseal.persistence.consent.ConsentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class ConsentOwnerButtonListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentOwnerButtonListener.class);
    private static final String PREFIX = "consent-owner:";

    private final ConsentService consentService;
    private final GatewayDiscordClient client;

    public ConsentOwnerButtonListener(ConsentService consentService, GatewayDiscordClient client) {
        this.consentService = consentService;
        this.client = client;
        client.on(ButtonInteractionEvent.class, this::handle).subscribe();
    }

    private Mono<Void> handle(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith(PREFIX)) {
            return Mono.empty();
        }

        String[] parts = customId.split(":", 3);
        if (parts.length != 3 || !("accept".equals(parts[1]) || "reject".equals(parts[1]))) {
            return event.reply("Invalid owner invitation.").withEphemeral(true);
        }

        boolean accepted = "accept".equals(parts[1]);
        String token = parts[2];
        String actorId = event.getInteraction().getUser().getId().asString();

        return Mono.fromCallable(() -> consentService.respondToOwnerRequest(token, actorId, accepted))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(response -> LOGGER.info(
                        "Owner invitation response actorId={} requesterId={} result={}",
                        actorId, response.requesterUserId(), response.result()))
                .flatMap(response -> switch (response.result()) {
                    case ACCEPTED -> event.edit("✅ You accepted the owner invitation. Owner mode is now active.")
                            .withComponents()
                            .then(notifyRequester(response.requesterUserId(),
                                    "✅ Your owner invitation was accepted. Owner consent mode is now active."));
                    case REJECTED -> event.edit("❌ You rejected the owner invitation. Consent was not changed.")
                            .withComponents()
                            .then(notifyRequester(response.requesterUserId(),
                                    "❌ Your owner invitation was rejected. Your consent was not changed."));
                    case EXPIRED -> event.edit("⌛ This owner invitation has expired.").withComponents();
                    case NOT_FOUND -> event.edit("This owner invitation is no longer active.").withComponents();
                    case NOT_INVITED_USER -> event.reply("This owner invitation was not sent to you.")
                            .withEphemeral(true);
                })
                .doOnError(error -> LOGGER.error("Failed to process owner consent invitation", error));
    }

    private Mono<Void> notifyRequester(String requesterUserId, String message) {
        if (requesterUserId == null) {
            return Mono.empty();
        }
        return client.getUserById(Snowflake.of(requesterUserId))
                .flatMap(user -> user.getPrivateChannel())
                .flatMap(channel -> channel.createMessage(message))
                .then()
                .onErrorResume(error -> {
                    LOGGER.warn("Unable to notify owner consent requester userId={}", requesterUserId);
                    return Mono.empty();
                });
    }
}
