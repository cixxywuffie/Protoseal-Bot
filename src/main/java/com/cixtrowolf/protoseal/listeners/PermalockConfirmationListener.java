package com.cixtrowolf.protoseal.listeners;

import com.cixtrowolf.protoseal.commands.restraint.PermalockConfirmation;
import com.cixtrowolf.protoseal.model.restraint.RestraintLockType;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintStateService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class PermalockConfirmationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermalockConfirmationListener.class);
    private final RestraintStateService restraintStateService;

    public PermalockConfirmationListener(RestraintStateService restraintStateService,
                                         GatewayDiscordClient client) {
        this.restraintStateService = restraintStateService;
        client.on(ButtonInteractionEvent.class, this::handle).subscribe();
    }

    Mono<Void> handle(ButtonInteractionEvent event) {
        var confirmation = PermalockConfirmation.parse(event.getCustomId());
        if (confirmation.isEmpty()) {
            return Mono.empty();
        }

        var request = confirmation.get();
        String clickingUserId = event.getInteraction().getUser().getId().asString();
        if (!request.actorUserId().equals(clickingUserId)) {
            LOGGER.warn("Permalock confirmation rejected guildId={} requesterId={} clickingUserId={} targetId={}",
                    request.guildId(), request.actorUserId(), clickingUserId, request.targetUserId());
            return event.reply("Only the user who requested this permalock can confirm or cancel it.")
                    .withEphemeral(true);
        }

        if (request.action() == PermalockConfirmation.Action.CANCEL) {
            LOGGER.info("Permalock confirmation cancelled guildId={} actorId={} targetId={}",
                    request.guildId(), request.actorUserId(), request.targetUserId());
            return event.edit("Permalock cancelled. No restraints were changed.")
                    .withComponents();
        }

        return Mono.fromCallable(() -> restraintStateService.updateLocks(
                        request.guildId(), request.targetUserId(), RestraintLockType.PERMALOCK,
                        request.actorUserId()))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(result -> LOGGER.info(
                        "Permalock confirmation processed guildId={} actorId={} targetId={} result={}",
                        request.guildId(), request.actorUserId(), request.targetUserId(), result))
                .flatMap(result -> editForResult(event, request, result))
                .doOnError(error -> LOGGER.error(
                        "Permalock confirmation failed guildId={} actorId={} targetId={}",
                        request.guildId(), request.actorUserId(), request.targetUserId(), error));
    }

    private Mono<Void> editForResult(ButtonInteractionEvent event, PermalockConfirmation request,
                                     RestraintStateService.LockResult result) {
        if (result == RestraintStateService.LockResult.APPLIED) {
            boolean selfTarget = request.actorUserId().equals(request.targetUserId());
            String publicMessage = selfTarget
                    ? "🔐 <@" + request.actorUserId()
                            + "> surrendered to their own **Permalock**, sealing every active restraint beyond any "
                            + "ordinary release. No key and no change of mind will unlock them—only special tools "
                            + "can break the lock and set them free (`/safeword`)."
                    : "🔐 <@" + request.actorUserId() + "> claimed <@" + request.targetUserId()
                            + "> in a **Permalock**, sealing every active restraint beyond any ordinary release. "
                            + "No key and no command will unlock them—only special tools can break the lock and "
                            + "set them free (`/safeword`).";
            return event.edit("Permalock confirmed. The result has been posted in the channel.")
                    .withComponents()
                    .then(event.createFollowup(publicMessage).then());
        }

        String message = switch (result) {
            case NO_ACTIVE_RESTRAINT -> "Permalock was not applied because that user has no active restraints.";
            case PERMALOCKED -> "That user's restraints are already permanently locked.";
            case TIMELOCKED -> "That user's restraints are currently timelocked.";
            case LOCKED_BY_ANOTHER_USER -> "Permalock was not applied because another user owns the active lock.";
            case CONSENT_DENIED -> "Permalock was not applied because the user's consent settings no longer allow it.";
            case NOT_LOCKED, REMOVED -> "Permalock could not be applied because the restraint state changed.";
            case APPLIED -> throw new IllegalStateException("Applied permalock handled before result switch");
        };
        return event.edit(message).withComponents();
    }
}
