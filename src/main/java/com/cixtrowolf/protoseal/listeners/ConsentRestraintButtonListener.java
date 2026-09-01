package com.cixtrowolf.protoseal.listeners;

import com.cixtrowolf.protoseal.model.restraint.RestraintDefinition;
import com.cixtrowolf.protoseal.persistence.consent.ConsentService;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintStateService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.common.util.Snowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class ConsentRestraintButtonListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentRestraintButtonListener.class);
    private static final String PREFIX = "consent-restraint:";
    private final ConsentService consentService;
    private final RestraintStateService restraintStateService;
    private final GatewayDiscordClient client;

    public ConsentRestraintButtonListener(ConsentService consentService,
                                          RestraintStateService restraintStateService,
                                          GatewayDiscordClient client) {
        this.consentService = consentService;
        this.restraintStateService = restraintStateService;
        this.client = client;
        client.on(ButtonInteractionEvent.class, this::handle).subscribe();
    }

    Mono<Void> handle(ButtonInteractionEvent event) {
        if (!event.getCustomId().startsWith(PREFIX)) return Mono.empty();
        String[] parts = event.getCustomId().split(":", 3);
        if (parts.length != 3 || !("accept".equals(parts[1]) || "reject".equals(parts[1]))) {
            return event.reply("Invalid restraint request.").withEphemeral(true);
        }
        boolean accepted = "accept".equals(parts[1]);
        String clickingUserId = event.getInteraction().getUser().getId().asString();
        return Mono.fromCallable(() -> consentService.respondToRestraintRequest(parts[2], clickingUserId, accepted))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(response -> switch (response.result()) {
                    case ACCEPTED -> applyAccepted(event, response.request());
                    case REJECTED -> event.edit("Request rejected. No restraints were changed.").withComponents()
                            .then(notifyRequester(response.request(), "Your restraint request was rejected."));
                    case EXPIRED -> event.edit("This restraint request has expired.").withComponents();
                    case NOT_FOUND -> event.edit("This restraint request is no longer active.").withComponents();
                    case CONSENT_CHANGED -> event.edit("Consent settings changed, so this request was cancelled.")
                            .withComponents();
                    case NOT_TARGET_USER -> event.reply("Only the requested user can accept or reject this action.")
                            .withEphemeral(true);
                })
                .doOnError(error -> LOGGER.error("Failed to process restraint consent request", error));
    }

    private Mono<Void> applyAccepted(ButtonInteractionEvent event,
                                     com.cixtrowolf.protoseal.persistence.consent.ConsentRestraintRequest request) {
        return Mono.fromCallable(() -> restraintStateService.saveStateApproved(
                        request.getGuildId(), request.getTargetUserId(), request.getZone(), request.getLevel(),
                        request.getActorUserId(), request.getName()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> {
                    String message = switch (result) {
                        case UPDATED -> acceptedMessage(request);
                        case LOCKED -> "Request accepted, but the restraint is now locked and could not be changed.";
                        case MITTS_ACTIVE -> "Request accepted, but active mitts now prevent this change.";
                        case CONSENT_DENIED -> "The approved request could not be applied.";
                    };
                    return event.edit(message).withComponents()
                            .then(notifyRequester(request, message));
                });
    }

    private Mono<Void> notifyRequester(
            com.cixtrowolf.protoseal.persistence.consent.ConsentRestraintRequest request, String message) {
        if (request == null) return Mono.empty();
        return client.getUserById(Snowflake.of(request.getActorUserId()))
                .flatMap(user -> user.getPrivateChannel())
                .flatMap(channel -> channel.createMessage(message))
                .then()
                .onErrorResume(error -> {
                    LOGGER.warn("Unable to notify restraint requester userId={}", request.getActorUserId());
                    return Mono.empty();
                });
    }

    private String acceptedMessage(com.cixtrowolf.protoseal.persistence.consent.ConsentRestraintRequest request) {
        String command = java.util.Arrays.stream(RestraintDefinition.values())
                .filter(definition -> definition.getZone() == request.getZone())
                .map(RestraintDefinition::getCommandName)
                .findFirst().orElse(request.getZone().name().toLowerCase());
        return "Request accepted: <@" + request.getActorUserId() + "> set <@" + request.getTargetUserId()
                + ">'s **" + command + "** to **" + request.getName() + "**.";
    }
}
