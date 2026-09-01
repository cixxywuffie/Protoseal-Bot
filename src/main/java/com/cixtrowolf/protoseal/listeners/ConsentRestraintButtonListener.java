package com.cixtrowolf.protoseal.listeners;

import com.cixtrowolf.protoseal.model.restraint.RestraintDefinition;
import com.cixtrowolf.protoseal.persistence.consent.ConsentService;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintStateService;
import com.cixtrowolf.protoseal.model.restraint.RestraintLockType;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.MessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

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
                    case REJECTED -> event.edit("Request rejected. No restraints were changed.").withComponents();
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
        return switch (request.getRequestType()) {
            case RESTRAINT -> applyAcceptedRestraint(event, request);
            case LOCK -> applyAcceptedLock(event, request);
            case TIMELOCK -> applyAcceptedTimelock(event, request);
        };
    }

    private Mono<Void> applyAcceptedRestraint(ButtonInteractionEvent event,
                                     com.cixtrowolf.protoseal.persistence.consent.ConsentRestraintRequest request) {
        return Mono.fromCallable(() -> restraintStateService.saveStateApproved(
                        request.getGuildId(), request.getTargetUserId(), request.getZone(), request.getLevel(),
                        request.getActorUserId(), request.getName()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> {
                    String message = switch (result) {
                        case UPDATED -> "Request accepted. The restraint was applied.";
                        case LOCKED -> "Request accepted, but the restraint is now locked and could not be changed.";
                        case MITTS_ACTIVE -> "Request accepted, but active mitts now prevent this change.";
                        case CONSENT_DENIED -> "The approved request could not be applied.";
                    };
                    Mono<Void> editRequest = event.edit(message).withComponents();
                    if (result != RestraintStateService.StateUpdateResult.UPDATED) return editRequest;
                    return editRequest.then(postAcceptedAction(request));
                });
    }

    private Mono<Void> applyAcceptedLock(ButtonInteractionEvent event,
            com.cixtrowolf.protoseal.persistence.consent.ConsentRestraintRequest request) {
        return Mono.fromCallable(() -> restraintStateService.updateLocksApproved(request.getGuildId(),
                        request.getTargetUserId(), request.getLockType(), request.getActorUserId()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> {
                    String message = switch (result) {
                        case APPLIED, REMOVED -> "Request accepted. The lock change was applied.";
                        case NO_ACTIVE_RESTRAINT -> "Request accepted, but there are no active restraints.";
                        case NOT_LOCKED -> "Request accepted, but the restraints are not locked.";
                        case PERMALOCKED -> "Request accepted, but the restraints are already permanently locked.";
                        case TIMELOCKED -> "Request accepted, but the restraints are already timelocked.";
                        case LOCKED_BY_ANOTHER_USER -> "Request accepted, but another user owns the active lock.";
                        case CONSENT_DENIED -> "The approved request could not be applied.";
                    };
                    Mono<Void> editRequest = event.edit(message).withComponents();
                    if (result != RestraintStateService.LockResult.APPLIED
                            && result != RestraintStateService.LockResult.REMOVED) return editRequest;
                    return editRequest.then(postAcceptedAction(request));
                });
    }

    private Mono<Void> applyAcceptedTimelock(ButtonInteractionEvent event,
            com.cixtrowolf.protoseal.persistence.consent.ConsentRestraintRequest request) {
        return Mono.fromCallable(() -> restraintStateService.applyTimelockApproved(request.getGuildId(),
                        request.getTargetUserId(), request.getActorUserId(),
                        Duration.ofMinutes(request.getDurationMinutes())))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> {
                    String message = switch (result) {
                        case APPLIED -> "Request accepted. The timelock was applied.";
                        case NO_ACTIVE_RESTRAINT -> "Request accepted, but there are no active restraints.";
                        case ALREADY_LOCKED -> "Request accepted, but the restraints are already locked.";
                        case PERMALOCKED -> "Request accepted, but the restraints are permanently locked.";
                        case INVALID_DURATION -> "The approved timelock duration is no longer valid.";
                        case CONSENT_DENIED -> "The approved request could not be applied.";
                    };
                    Mono<Void> editRequest = event.edit(message).withComponents();
                    if (result != RestraintStateService.TimelockResult.APPLIED) return editRequest;
                    return editRequest.then(postAcceptedAction(request));
                });
    }

    private Mono<Void> postAcceptedAction(
            com.cixtrowolf.protoseal.persistence.consent.ConsentRestraintRequest request) {
        if (request.getChannelId() == null) return Mono.empty();
        return client.getChannelById(Snowflake.of(request.getChannelId()))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(acceptedMessage(request)))
                .then()
                .onErrorResume(error -> {
                    LOGGER.warn("Unable to post accepted restraint request channelId={} actorId={} targetId={}",
                            request.getChannelId(), request.getActorUserId(), request.getTargetUserId(), error);
                    return Mono.empty();
                });
    }

    private String acceptedMessage(com.cixtrowolf.protoseal.persistence.consent.ConsentRestraintRequest request) {
        if (request.getRequestType() == com.cixtrowolf.protoseal.persistence.consent.ConsentRestraintRequest.RequestType.LOCK) {
            if (request.getLockType() == null) {
                return "<@" + request.getActorUserId() + "> removed the locks from all of <@"
                        + request.getTargetUserId() + ">'s active restraints.";
            }
            if (request.getLockType() == RestraintLockType.PERMALOCK) {
                return "🔐 <@" + request.getActorUserId() + "> applied **Permalock** to all of <@"
                        + request.getTargetUserId() + ">'s active restraints. Only `/safeword` can clear it.";
            }
            return "<@" + request.getActorUserId() + "> applied " + request.getLockType().getEmoji()
                    + " **" + request.getLockType().getDisplayName() + "** to all of <@"
                    + request.getTargetUserId() + ">'s active restraints.";
        }
        if (request.getRequestType() == com.cixtrowolf.protoseal.persistence.consent.ConsentRestraintRequest.RequestType.TIMELOCK) {
            return "<@" + request.getActorUserId() + "> timelocks all of <@" + request.getTargetUserId()
                    + ">'s active restraints for **" + request.getDurationMinutes() + " minutes**.";
        }
        var definition = java.util.Arrays.stream(RestraintDefinition.values())
                .filter(candidate -> candidate.getZone() == request.getZone())
                .findFirst().orElse(null);
        if (definition == null) {
            return "<@" + request.getActorUserId() + "> changed <@" + request.getTargetUserId()
                    + ">'s restraint to **" + request.getName() + "**.";
        }
        return String.format(definition.getLevel(request.getLevel()).message(),
                "<@" + request.getActorUserId() + ">", "<@" + request.getTargetUserId() + ">");
    }
}
